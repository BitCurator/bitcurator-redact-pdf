package bca.redact;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.List;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Document;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;

class PreviewPanel extends JPanel {
	private static final Logger log = LoggerFactory.getLogger(PreviewPanel.class);

	private static final long serialVersionUID = 8082940778123734607L;
	BufferedImage pageImage = null;
	int page = -1;
	Rectangle currentLocation = null;
	List<RedactLocation> locations = null;
	//private boolean paintedPage;
	
	public Color redactColor = new Color(0, 0, 0, .4f);
	public Color askColor = new Color(1, 0, 0, .4f);

	private com.itextpdf.kernel.geom.Rectangle pageSize;

	PreviewPanel() {
		log.debug("Created PreviewPanel");
		// set a preferred size for the custom panel.
		//setPreferredSize(new Dimension(420, 420));
		this.locations = locations;
	}
	
	public void setRedactLocations(List<RedactLocation> locations) {
		this.locations = locations;
	}

	public void setPage(BufferedImage image, int page, com.itextpdf.kernel.geom.Rectangle pageSize) {
		this.pageImage = image;
		this.page = page;
		this.pageSize = pageSize;
		//this.paintedPage = false;
	}

	
	@Override
	public Dimension getPreferredSize() {
		if(this.pageImage != null) {
			int h = this.pageImage.getHeight();
			int w = this.pageImage.getWidth();
			Dimension size = new Dimension(w, h);
			return size;
		} else {
			return new Dimension(612, 792);
		}
	}

	public void setPDFLocation(PdfCleanUpLocation loc, Document doc) {
		com.itextpdf.kernel.geom.Rectangle r = loc.getRegion();
		PdfPage page = doc.getPdfDocument().getPage(loc.getPage());
		com.itextpdf.kernel.geom.Rectangle pageSize = page.getPageSize();
		this.currentLocation = translateLocation(r, pageSize);
		repaint(500);
	}
	
	public void clearPDFLocation() {
		this.currentLocation = null;
		repaint(500);
	}
	
	private Rectangle translateLocation(com.itextpdf.kernel.geom.Rectangle location, com.itextpdf.kernel.geom.Rectangle pageSize) {
		int awtX = (int)(location.getX());
		int awtY = (int)(pageSize.getHeight()-location.getY()-location.getHeight());
		return new Rectangle(awtX, awtY, (int)location.getWidth(), (int)location.getHeight());
	}

	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		if(pageImage != null) {
			Graphics2D g2d = (Graphics2D) g.create();
			g2d.drawImage(pageImage, 0, 0, this);
			g2d.dispose();
		}
		//this.paintedPage = true;
		// g.drawString("BLAH", 20, 20);
		Color prev = g.getColor();
		//.getSize().g.setComposite(AlphaComposite.SrcOver.derive(0.8f));
		for(RedactLocation rl : this.locations) {
			if(rl.page == page) {
				Rectangle r = translateLocation(rl.loc.getRegion(), pageSize);
				switch(rl.action) {
				case Redact:
					g.setColor(redactColor);
					g.fillRect(r.x-1, r.y-1, r.width+2, r.height+2);
					break;
				case Ignore:
					g.setColor(Color.GRAY);
					g.drawRect(r.x, r.y, r.width, r.height);
					g.drawRect(r.x-1, r.y-1, r.width+2, r.height+2);
					break;
				case Ask:
					g.setColor(askColor);
					g.fillRect(r.x-1, r.y-1, r.width+2, r.height+2);
					break;
				}
			}
		}
		if (currentLocation != null) {
			g.setColor(Color.red);
			g.drawRect(currentLocation.x, currentLocation.y, currentLocation.width, currentLocation.height);
			g.drawRect(currentLocation.x-1, currentLocation.y-1, currentLocation.width+2, currentLocation.height+2);
			this.scrollRectToVisible(currentLocation);
		}
		g.setColor(prev);
	}
	
}