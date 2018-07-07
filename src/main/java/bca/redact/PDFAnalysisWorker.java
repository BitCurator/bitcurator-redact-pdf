package bca.redact;

import java.io.File;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.SwingWorker;

import org.apache.log4j.Logger;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class PDFAnalysisWorker extends SwingWorker<Set<String>, String> {
	private static final Logger log = Logger.getLogger(PDFAnalysisWorker.class);
	private static Properties props = new Properties();
	String[] empty = new String[] {};
	File[] pdfs;
	StanfordCoreNLP pipeline;
	
	public class AnalysisException extends Exception {
		File file;
		Throwable cause;
		AnalysisException(File f, Throwable e) {
			file=f;
			cause=e;
		}
	}

	static {
		props.setProperty("annotators", "tokenize,ssplit,pos,ner");
		// set a property for an annotator, in this case the coref annotator is being
		// set to use the neural algorithm
		props.setProperty("ner.model", "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz");
		props.setProperty("ner.useSUTime", "false");
		props.setProperty("ner.applyNumericClassifiers", "false");
		props.setProperty("ner.applyFineGrained", "false");
	}

	PDFAnalysisWorker(File[] pdfs) {
		this.pdfs = pdfs;
	}

	@Override
	protected Set<String> doInBackground() {
		log.info("Processing PDFs: " + this.pdfs.length);
		Set<String> entities = new HashSet<>();
		if (pipeline == null) {
			pipeline = new StanfordCoreNLP(props);
			log.info("loaded SNLP pipeline");
		}
		int errors = 0;
		int successes = 0;
		fileloop: for (File f : pdfs) {
			PdfDocument pdfDoc = null;
			try {
				log.info("analyzing: " + f.getAbsolutePath());
				try {
					pdfDoc = new PdfDocument(new PdfReader(f.getAbsolutePath()));
				} catch (Throwable e) {
					throw new AnalysisException(f, e);
				}
				LocationTextExtractionStrategy strategy = new LocationTextExtractionStrategy();
				PdfCanvasProcessor parser = new PdfCanvasProcessor(strategy);
				for (int i = 1; i <= pdfDoc.getNumberOfPages(); i++) {
					try {
						parser.processPageContent(pdfDoc.getPage(i));
					} catch (Throwable e) {
						throw new AnalysisException(f, e);
					} finally {
						parser.reset();
					}
				}
				String str = strategy.getResultantText();
				CoreDocument doc = new CoreDocument(str);
				log.info("created CoreDocument");
				try {
					pipeline.annotate(doc);
				} catch (Throwable e) {
					throw new AnalysisException(f, e);
				}
				log.info("annotated document");
				Set<String> docEntities = new HashSet<String>();
				for (CoreEntityMention cem : doc.entityMentions()) {
					String text = cem.text();
					String entity = cem.entity();
					docEntities.add(text);
					// log.info("got entity: " + entity + " text: "+ text);
				}
				log.info("finished gathering tags for: " + f.getName());
				publish(docEntities.toArray(empty));
				log.info("new entities: " + String.join(", ", docEntities));
				entities.addAll(docEntities);
				successes++;
			} catch (AnalysisException e) {
				log.error("ANALYSIS ERROR: " + e.cause.getLocalizedMessage());
				errors++;
			} finally {
				if(pdfDoc != null) {
					pdfDoc.close();
				}
			}
		}
		log.error("There were errors: "+errors);
		log.info("There were successes: "+successes);
		return entities;
	}

}
