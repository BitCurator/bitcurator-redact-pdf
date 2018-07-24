package bca.redact;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import javax.swing.JPanel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.layout.Document;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;

class PreviewPanel extends JPanel {
	private static final Logger log = LoggerFactory.getLogger(PreviewPanel.class);

	private static final long serialVersionUID = 8082940778123734607L;
	BufferedImage pageImage = null;
	Rectangle location = null;
	private boolean paintedPage;

	PreviewPanel() {
		// set a preferred size for the custom panel.
		//setPreferredSize(new Dimension(420, 420));
	}

	public void setPageImage(BufferedImage image) {
		this.pageImage = image;
		this.paintedPage = false;
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
		
		// Pdf Region => lower left point
		log.info("Rect {} x/y: {}/{}", r, r.getX(), r.getY());
		log.info("Margins T:{} B:{} L:{} R:{}", doc.getTopMargin(), doc.getBottomMargin(), doc.getLeftMargin(), doc.getRightMargin());
		
		// AWT Rectangle => upper left point
		int awtX = (int)(r.getX());
		int awtY = (int)(pageSize.getHeight()-r.getY()-r.getHeight()/*+doc.getTopMargin()*/);
		location = new Rectangle(awtX, awtY, (int)r.getWidth(), (int)r.getHeight());
		log.info("preview rectangle: {}", location);
		log.info("Art box :{}", page.getArtBox());
		log.info("Bleed box :{}", page.getBleedBox());
		log.info("Crop box :{}", page.getCropBox());
		log.info("Media box :{}", page.getMediaBox());
		log.info("Trim box :{}", page.getTrimBox());
		log.info("pagesize: {}", pageSize);
		com.itextpdf.kernel.geom.Rectangle pr = doc.getPageEffectiveArea(new PageSize(pageSize));
		log.info("Effective pagesize: {}", pr);
		log.info("image size: {}", getPreferredSize());
		//location.translate
		repaint(500);
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
		if (location != null) {
			Color prev = g.getColor();
			g.setColor(Color.red);
			g.drawRect(location.x, location.y, location.width, location.height);
			g.drawRect(location.x-1, location.y-1, location.width+2, location.height+2);
			g.setColor(prev);
			this.scrollRectToVisible(location);
		}
	}
	
}