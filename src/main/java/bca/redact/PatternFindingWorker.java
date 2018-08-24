package bca.redact;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IPdfTextLocation;
import com.itextpdf.kernel.pdf.canvas.parser.listener.RegexBasedLocationExtractionStrategy;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;

public class PatternFindingWorker extends SwingWorker<List<RedactLocation>, RedactLocation> {
	Logger log = LoggerFactory.getLogger(PatternFindingWorker.class);
	PdfDocument pdfDoc = null;
	List<TextPattern> patterns = null;

	public PatternFindingWorker(PdfDocument pdfDoc, List<TextPattern> patterns) {
		this.pdfDoc = pdfDoc;
		this.patterns = patterns;
	}

	@Override
	protected List<RedactLocation> doInBackground() throws Exception {
		log.info("Starting redaction scan..");
		List<RedactLocation> locations = new ArrayList<RedactLocation>();
		int pages = pdfDoc.getNumberOfPages();
		for (int i = 1; i <= pages; i++) {
			PdfPage page = pdfDoc.getPage(i /* FIXME */);
			for (TextPattern pattern : patterns) {
				log.info("Working on entity: {}", pattern.getRegex());
				String entityRegex = pattern.getRegex();
				RegexBasedLocationExtractionStrategy extractionStrategy = new RegexBasedLocationExtractionStrategy(
						entityRegex);
				new PdfCanvasProcessor(extractionStrategy).processPageContent(page);
				for (IPdfTextLocation location : extractionStrategy.getResultantLocations()) {
					PdfCleanUpLocation loc = new PdfCleanUpLocation(i, location.getRectangle(), DeviceGray.BLACK);
					RedactLocation myloc = new RedactLocation();
					myloc.pattern = pattern;
					myloc.page = i;
					myloc.loc = loc;
					myloc.action = pattern.policy;
					publish(myloc);
					locations.add(myloc);
				}
			}
		}
		return locations;
	}

}
