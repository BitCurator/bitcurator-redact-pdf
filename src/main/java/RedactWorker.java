import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.SwingWorker;

import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IPdfTextLocation;
import com.itextpdf.kernel.pdf.canvas.parser.listener.RegexBasedLocationExtractionStrategy;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;

public class RedactWorker extends SwingWorker<List<RedactLocation>, RedactLocation> {
	PdfDocument pdfDoc = null;
	Collection<String> entities = null;

	public RedactWorker(PdfDocument pdfDoc, Collection<String> entities) {
		this.pdfDoc = pdfDoc;
		this.entities = entities;
	}

	@Override
	protected List<RedactLocation> doInBackground() throws Exception {
		List<RedactLocation> cleanUps = new ArrayList<RedactLocation>();
		int pages = pdfDoc.getNumberOfPages();
		for (int i = 1; i < pages; i++) {
			PdfPage page = pdfDoc.getPage(i /* FIXME */);
			List<RedactLocation> pagechunk = new ArrayList<RedactLocation>();
			for (String entity : entities) {
				String entityRegex = Pattern.quote(entity);
				RegexBasedLocationExtractionStrategy extractionStrategy = new RegexBasedLocationExtractionStrategy(
						entityRegex);
				new PdfCanvasProcessor(extractionStrategy).processPageContent(page);
				for (IPdfTextLocation location : extractionStrategy.getResultantLocations()) {
					PdfCleanUpLocation loc = new PdfCleanUpLocation(i, location.getRectangle(), DeviceGray.BLACK);
					RedactLocation myloc = new RedactLocation();
					myloc.entity = entity;
					myloc.page = i;
					myloc.loc = loc;
					cleanUps.add(myloc);
					pagechunk.add(myloc);
				}
			}
			publish(pagechunk.toArray(new RedactLocation[pagechunk.size()-1]));
		}
		return cleanUps;
	}

}
