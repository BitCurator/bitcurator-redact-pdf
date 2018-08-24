package bca.redact;
import java.util.Comparator;

import com.itextpdf.pdfcleanup.PdfCleanUpLocation;

import bca.redact.TextPattern;

public class RedactLocation implements Comparable<RedactLocation> {
	public int page = -1;
	public TextPattern pattern = null;
	public PdfCleanUpLocation loc = null;
	public Action action = null;

	RedactLocation() {
	}
	
	public int getPage() {
		return this.loc.getPage();
	}
	
	public float getTop() {
		return this.loc.getRegion().getTop();
	}
	
	public float getLeft() {
		return this.loc.getRegion().getLeft();
	}

	@Override
	public int compareTo(RedactLocation loc0) {
		return Comparator.comparingInt(RedactLocation::getPage)
	              .thenComparing(Comparator.comparingDouble(RedactLocation::getTop).reversed())
	              .thenComparingDouble(RedactLocation::getLeft)
	               .compare(this, loc0);
	}
}
