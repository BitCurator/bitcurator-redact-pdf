package bca.redact;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.NotImplementedException;
import org.apache.log4j.Logger;

public class SpaceyAnalysisWorker extends AnalysisWorker {
	private static final Logger log = Logger.getLogger(SpaceyAnalysisWorker.class);
	File[] pdfs;

	SpaceyAnalysisWorker(File[] pdfs) {
		this.pdfs = pdfs;
	}

	@Override
	protected Set<PDFAnalysis> doInBackground() {
		Set<PDFAnalysis> analyses = new HashSet<>();
		int errors = 0;
		
		ProcessBuilder processBuilder = new ProcessBuilder("D:/test.bat", "ABC", "XYZ");
		processBuilder.directory(new File("D:/"));
		processBuilder.redirectErrorStream(true);
		//processBuilder.
		Process p;
		try {
			p = processBuilder.start();
			p.waitFor();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (File f : pdfs) {
			try {
				throw new AnalysisException(f, new NotImplementedException("Not yet implemented"));
				//List<String[]> pdfentities = new ArrayList<>();
				//for (CoreEntityMention cem : doc.entityMentions()) {
				//	pdfentities.add(new String[] {cem.text(), cem.entityType()});
				//}
				//PDFAnalysis analysis = new PDFAnalysis(f, pdfentities.toArray(empty));
				//analyses.add(analysis);
				//publish(analysis);
			} catch (AnalysisException e) {
				PDFAnalysis analysis = new PDFAnalysis(f, e);
				analyses.add(analysis);
				publish(analysis);
			} finally {
			}
		}
		
		log.error("There were PDF Entity Analysis errors: "+errors);
		return analyses;
	}

}
