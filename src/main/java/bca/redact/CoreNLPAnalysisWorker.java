package bca.redact;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreEntityMention;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class CoreNLPAnalysisWorker extends AnalysisWorker {
	private static final Logger log = Logger.getLogger(CoreNLPAnalysisWorker.class);
	private static Properties props = new Properties();
	private static final String[][] empty = new String[][] {};
	File[] pdfs;
	StanfordCoreNLP pipeline;
	private Set<String> entityTypesIncluded = new HashSet<>();

	static {
		props.setProperty("annotators", "tokenize,ssplit,pos,ner");
		// set a property for an annotator, in this case the coref annotator is being
		// set to use the neural algorithm
		props.setProperty("ner.model", "edu/stanford/nlp/models/ner/english.all.3class.distsim.crf.ser.gz");
		props.setProperty("ner.useSUTime", "false");
		props.setProperty("ner.applyNumericClassifiers", "false");
		props.setProperty("ner.applyFineGrained", "false");
	}

	CoreNLPAnalysisWorker(File[] pdfs, boolean includePerson, boolean includeLocation, boolean includeOrganization) {
		this.pdfs = pdfs;
		if(includeOrganization) entityTypesIncluded.add("ORGANIZATION");
		if(includePerson) entityTypesIncluded.add("PERSON");
		if(includeLocation) entityTypesIncluded.add("LOCATION");
	}

	@Override
	protected Set<PDFAnalysis> doInBackground() {
		Set<PDFAnalysis> analyses = new HashSet<>();
		if (pipeline == null) {
			pipeline = new StanfordCoreNLP(props);
		}
		int errors = 0;
		for (File f : pdfs) {
			PdfDocument pdfDoc = null;
			try {
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
				try {
					pipeline.annotate(doc);
				} catch (Throwable e) {
					throw new AnalysisException(f, e);
				}
				List<String[]> pdfentities = new ArrayList<>();
				for (CoreEntityMention cem : doc.entityMentions()) {
					if(entityTypesIncluded.contains(cem.entityType())) {
						pdfentities.add(new String[] {cem.text(), cem.entityType(), cem.sentence().text()});
					}
				}
				PDFAnalysis analysis = new PDFAnalysis(f, pdfentities.toArray(empty));
				analyses.add(analysis);
				publish(analysis);
			} catch (AnalysisException e) {
				PDFAnalysis analysis = new PDFAnalysis(f, e);
				analyses.add(analysis);
				publish(analysis);
			} finally {
				if(pdfDoc != null) {
					pdfDoc.close();
				}
			}
		}
		if(errors > 0) {
			log.error("There were PDF Entity Analysis errors: "+errors);
		}
		return analyses;
	}

}
