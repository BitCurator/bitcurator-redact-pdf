package bca.redact;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.BevelBorder;

import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.SimpleRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;
import com.itextpdf.pdfcleanup.PdfCleanUpTool;

import bca.redact.RedactionApp.Entity;

public class RedactDialog extends JDialog {
	Logger log = LoggerFactory.getLogger(RedactDialog.class);
	private static final long serialVersionUID = 2027484467240740791L;

	// private PreviewPanel previewPanel;

	List<Entity> entities = null;
	File pdfFile = null;
	File outFile = null;

	List<RedactLocation> redactionLocations = new ArrayList<>();

	// user policies from prompts
	Set<String> entitiesAccepted = new HashSet<>();
	Set<String> entitiesRejected = new HashSet<>();

	// Ghostscript rendering variables for preview
	PreviewPanel previewPanel;
	PDFDocument doc = null;
	Image pageImage = null;

	private PdfDocument itextPDF;

	private int currentPage = -1;
	private Document layoutDoc;

	@SuppressWarnings("unchecked")
	public RedactDialog() {
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setModal(false);
		setTitle("Redact Document");
		setAlwaysOnTop(true);
		//setModalityType(ModalityType.DOCUMENT_MODAL);

		JPanel panel = new JPanel(new BorderLayout());
		//panel.setMinimumSize(new Dimension(600, 500));
		getContentPane().add(panel, BorderLayout.CENTER);

		previewPanel = new PreviewPanel();
		//previewPanel.setMinimumSize(new Dimension(600, 400));
		previewPanel.setBackground(new java.awt.Color(255, 255, 255));
		previewPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		previewPanel.addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
	        	log.info("mouse pressed at: {}",  e.getPoint());
			}
		});
		JScrollPane scrolls = new JScrollPane(previewPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		panel.add(scrolls, BorderLayout.CENTER);
		pack();
	}

	public void markPageLocation(RedactLocation loc) {
		log.info("Marking redact location for \"{}\"", loc.entity.text);
		if (loc.page != this.currentPage) {
			setPreviewPage(loc.page);
		}
		previewPanel.setPDFLocation(loc.loc, this.layoutDoc);
	}

	public void redactButtonHandler() throws IOException {
		List<PdfCleanUpLocation> cleanUps = this.redactionLocations.stream().filter(x -> x.redact).map(x -> x.loc)
				.collect(Collectors.toList());
		PdfCleanUpTool cleaner = new PdfCleanUpTool(this.itextPDF, cleanUps);
		cleaner.cleanUp();
		closeDoc();
	}

	public void closeDoc() {
		this.itextPDF.close();
		this.itextPDF = null;
		this.setVisible(false);
		this.doc = null;
		this.pageImage = null;
		this.currentPage = -1;
	}

	public void startDoc(File pdfFile, File outputFile, List<Entity> fileEntities)
			throws FileNotFoundException, IOException {
		this.entities = fileEntities;
		this.pdfFile = pdfFile;
		this.outFile = outputFile;
		this.outFile.getParentFile().mkdirs();
		PDFDocument mydoc = new PDFDocument();
		mydoc.load(pdfFile);
		this.doc = mydoc;
		this.currentPage = -1;
		boolean firstLocMarked = false;
		try {
			this.itextPDF = new PdfDocument(new PdfReader(pdfFile), new PdfWriter(outFile));
			this.layoutDoc = new Document(this.itextPDF);
			RedactWorker worker = new RedactWorker(itextPDF, this.entities) {
				@Override
				protected void done() {
					// ?? all locations loaded
				}

				@Override
				protected void process(List<RedactLocation> newLocations) {
					log.info("Found {} redaction locations.", newLocations.size());
					redactionLocations.addAll(newLocations);
					if (currentPage == -1) {
						RedactLocation l = newLocations.get(0);
						markPageLocation(l);
					}
				}
			};
			worker.execute();
		} catch (IOException e) {
			throw new Error(e);
		}
		setPreviewPage(1);
		pack();
	}

	private void setPreviewPage(int i) {
		try {
			SimpleRenderer renderer = new SimpleRenderer();
			renderer.setResolution(72);
			BufferedImage pageImage = (BufferedImage) renderer.render(doc, i - 1, i - 1).get(0);
			previewPanel.setPageImage(pageImage);
			//previewPanel.setMinimumSize(new Dimension(pageImage.getWidth(), pageImage.getHeight()));
		} catch (Exception e) {
			throw new Error(e);
		}
	}

}