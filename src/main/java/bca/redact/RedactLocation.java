package bca.redact;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;

import bca.redact.RedactionApp.Entity;

public class RedactLocation {
	public int page = -1;
	public Entity entity = null;
	public PdfCleanUpLocation loc = null;
	public boolean redact = true;

	RedactLocation() {
	}
}
