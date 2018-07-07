import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.BevelBorder;

import org.apache.commons.io.IOUtils;
import org.docopt.Docopt;
import org.ghost4j.document.PDFDocument;
import org.ghost4j.renderer.SimpleRenderer;

import com.itextpdf.kernel.colors.DeviceGray;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.IPdfTextLocation;
import com.itextpdf.kernel.pdf.canvas.parser.listener.RegexBasedLocationExtractionStrategy;
import com.itextpdf.pdfcleanup.PdfCleanUpLocation;
import com.itextpdf.pdfcleanup.PdfCleanUpTool;

public class App extends JFrame {

	private static final long serialVersionUID = 2027484467240740791L;

	private PreviewPanel jPanel;

	List<String> entities = null;
	List<File> pdfFiles = new ArrayList<>();
	String outPath = null;

	// user policies from prompts
	Set<String> entitiesAccepted = new HashSet<>();
	Set<String> entitiesRejected = new HashSet<>();

	// Ghostscript rendering variables for preview
	PDFDocument doc = null;
	Image pageImage = null;

	@SuppressWarnings("unchecked")
	public App(Map<String, Object> opts) {
		initComponents();

		// Load entity strings from file
		String entityFileStr = (String) opts.get("<entity-file>");
		setEntityFile(entityFileStr);

		// Validate output path and prompt for overwrite.
		Boolean overwrite = (Boolean) opts.get("-O");
		String outPathStr = (String) opts.get("--output-dir");
		setOutputPath(Paths.get(outPathStr).toFile(), overwrite);

		// Build the list of PDFs
		List<File> pdfFilePaths = new ArrayList<>();
		for (String f : (List<String>) opts.get("FILE")) {
			File file = Paths.get(f).toFile();
			pdfFilePaths.add(file);
		}
		setPDFPaths(pdfFilePaths);
	}

	private void initComponents() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException ex) {
			ex.printStackTrace();
		}
		this.setLocationRelativeTo(null);
		this.setSize(400, 600);

		jPanel = new PreviewPanel();
		jPanel.setBackground(new java.awt.Color(255, 255, 255));
		jPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		this.setContentPane(jPanel);
		// be nice to testers..
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		pack();
	}

	public void gotoPage(int i) {

	}

	public void markPageLocation(RedactLocation loc) {

	}

	public void redactButtonHandler() {
		File f = this.pdfFiles.get(0);
		try {
			File outFile = Paths.get(outPath, f.getName()).toFile();
			PdfDocument pdf = new PdfDocument(new PdfReader(f), new PdfWriter(outFile));
			int pageCount = pdf.getNumberOfPages();
			RedactWorker worker = new RedactWorker(pdf, this.entities);
			worker.addPropertyChangeListener(new PropertyChangeListener() {

				@Override
				public void propertyChange(final PropertyChangeEvent event) {
					event.getNewValue();
				}

			});
			worker.execute();
		} catch (IOException e) {
			throw new Error(e);
		}

		//
		// // load Ghostscript preview doc
		// setPreviewFile(src);
		// setPreviewPage(i);
		// this.jPanel.setPDFLocation(location);
		// if (!this.entitiesAccepted.contains(entity)) {
		// // TODO prompt user, record any "All" answers
		// System.out.println(location.getRectangle().toString()+"\t"+location.getText());
		// }
		// PdfCleanUpTool cleaner = new PdfCleanUpTool(pdfDoc, cleanUps);
		// cleaner.cleanUp();
		// pdfDoc.close();
	}

	private void setPDFPaths(List<File> pdfPaths) throws Error {
		pdfFiles.clear();
		for (File file : pdfPaths) {
			if (!file.exists()) {
				throw new Error("PDF path does not exist: " + file.getPath());
			}
			if (file.isDirectory()) {
				File[] dirFiles = file.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File dir, String name) {
						return name.endsWith(".PDF") || name.endsWith(".pdf");
					}

				});
				for (File dirFile : dirFiles) {
					pdfFiles.add(dirFile);
				}
			} else {
				pdfFiles.add(file);
			}
		}
		System.out.println(pdfFiles.size() + " PDFs are queued for redaction.");
	}

	private void setOutputPath(File newOutPath, Boolean overwrite) {
		if (!newOutPath.exists())
			newOutPath.mkdirs();
		if (newOutPath.listFiles().length > 0 && !overwrite) {
			// TODO replace with a dialog
			Scanner scan = new Scanner(System.in);
			while (true) {
				System.out.println("The output path has file in it. Do you want to overwrite them? [Y/n]");
				String ans = scan.nextLine();
				if ("n".equals(ans)) {
					scan.close();
					return;
				} else if ("Y".equals(ans) || "y".equals(ans)) {
					scan.close();
					break;
				}
			}
		}
		this.outPath = newOutPath.getAbsolutePath();
	}

	private void setEntityFile(String entityFileStr) throws Error {
		File entityFile = Paths.get(entityFileStr).toFile();
		if (!entityFile.exists()) {
			throw new Error("The entity-file does not exist: " + entityFileStr);
		}
		try (InputStream in = new FileInputStream(entityFile);) {
			entities = IOUtils.readLines(in, "utf-8");
		} catch (IOException e) {
			throw new Error("Problem reading entity file: " + entityFileStr, e);
		}
	}

	private void setPreviewFile(File src) throws IOException {
		PDFDocument mydoc = new PDFDocument();
		mydoc.load(src);
		this.doc = mydoc;
	}

	private void setPreviewPage(int i) {
		try {
			SimpleRenderer renderer = new SimpleRenderer();
			renderer.setResolution(100);
			BufferedImage pageImage = (BufferedImage) renderer.render(doc, i, i).get(0);
			jPanel.setPageImage(pageImage);
		} catch (Exception e) {
			throw new Error(e);
		}
	}

	public static void main(String[] args) throws IOException {
		InputStream doc = App.class.getClassLoader().getResourceAsStream("docopt.txt");
		final Map<String, Object> opts = new Docopt(doc).withVersion("BitCurator PDF Redactor 0.1.0").parse(args);

		EventQueue.invokeLater(new Runnable() {
			public void run() {
				App foo = new App(opts);
				foo.setVisible(true);
				if (foo.entities.size() > 0 && foo.pdfFiles.size() > 0) { // ready to run
					// push the button to process

				}
			}
		});
	}

	class PreviewPanel extends JPanel {

		private static final long serialVersionUID = 8082940778123734607L;
		BufferedImage pageImage = null;
		IPdfTextLocation location = null;
		private boolean paintedPage;

		PreviewPanel() {
			// set a preferred size for the custom panel.
			setPreferredSize(new Dimension(420, 420));
		}

		public void setPageImage(BufferedImage image) {
			this.pageImage = image;
			this.paintedPage = false;
		}

		public void setPDFLocation(IPdfTextLocation location) {
			this.location = location;
			repaint(500);
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (this.pageImage != null && !paintedPage) {
				Graphics2D g2d = (Graphics2D) g.create();
				g2d.drawImage(pageImage, 0, 0, this);
				g2d.dispose();
				this.paintedPage = true;
			}
			// g.drawString("BLAH", 20, 20);
			if (location != null) {
				float Y = location.getRectangle().getTop();
				float X = location.getRectangle().getLeft();
			}
			// TODO translate floats into panel coordinates..

			// g.drawRect(200, 200, 200, 200);
		}
	}

}