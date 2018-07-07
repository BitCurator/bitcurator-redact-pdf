import com.itextpdf.pdfcleanup.PdfCleanUpLocation;

public class RedactLocation {
	public int page = -1;
	public String entity = null;
	public PdfCleanUpLocation loc = null;
	public boolean redact = true;

	RedactLocation() {
	}
}
