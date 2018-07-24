package bca.redact;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IPdfTextLocation;
import com.itextpdf.kernel.pdf.canvas.parser.listener.RegexBasedLocationExtractionStrategy;
import com.itextpdf.layout.Document;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;

import bca.redact.RedactionApp.Entity;

public class RedactWorker extends SwingWorker<List<RedactLocation>, RedactLocation> {
	Logger log = LoggerFactory.getLogger(RedactWorker.class);
	PdfDocument pdfDoc = null;
	List<Entity> entities = null;

	public RedactWorker(PdfDocument pdfDoc, List<Entity> entities2) {
		this.pdfDoc = pdfDoc;
		Document doc = new Document(pdfDoc);
		log.info("Margins (t,b,l,r) ({},{},{},{})",doc.getTopMargin(), doc.getBottomMargin(), doc.getLeftMargin(), doc.getRightMargin());
		this.entities = entities2;
	}

	@Override
	protected List<RedactLocation> doInBackground() throws Exception {
		log.info("Starting redaction scan..");
		List<RedactLocation> locations = new ArrayList<RedactLocation>();
		int pages = pdfDoc.getNumberOfPages();
		for (int i = 1; i <= pages; i++) {
			PdfPage page = pdfDoc.getPage(i /* FIXME */);
			for (Entity entity : entities) {
				log.info("Working on entity: {}", entity.text);
				String entityRegex = Pattern.quote(entity.text);
				RegexBasedLocationExtractionStrategy extractionStrategy = new RegexBasedLocationExtractionStrategy(
						entityRegex);
				new PdfCanvasProcessor(extractionStrategy).processPageContent(page);
				for (IPdfTextLocation location : extractionStrategy.getResultantLocations()) {
					PdfCleanUpLocation loc = new PdfCleanUpLocation(i, location.getRectangle(), DeviceGray.BLACK);
					RedactLocation myloc = new RedactLocation();
					myloc.entity = entity;
					myloc.page = i;
					myloc.loc = loc;
					publish(myloc);
					locations.add(myloc);
				}
			}
		}
		return locations;
	}

}
