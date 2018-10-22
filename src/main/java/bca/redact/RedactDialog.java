package bca.redact;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

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
import com.lowagie.text.Font;


public class RedactDialog extends JDialog {
	public class RedactionsTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		public final String[] cols = new String[] { "Page", "Text", "Type", "Action" };
		Color defaultBackground = null;

		@Override
		public String getColumnName(int column) {
			return cols[column];
		}

		@Override
		public int getColumnCount() {
			return 4;
		}

		@Override
		public int getRowCount() {
			return redactLocations.size();
		}

		@Override
		public Object getValueAt(int row, int col) {
			RedactLocation rl = redactLocations.get(row);
			switch (col) {
			case 0:
				return rl.page;
			case 1:
				return rl.pattern.getLabel();
			case 2:
				return rl.pattern.getType();
			case 3:
				return rl.action;
			}
			return null;
		}
		
		public Action getAction(int row) {
			return redactLocations.get(row).action;
		}

		public boolean isCellEditable(int row, int col) {
			return col == 3;
		}

		@Override
		public void setValueAt(Object value, int row, int col) {
			RedactLocation rl = redactLocations.get(row);
			if (col == 3) {
				rl.action = (Action) value;
				this.fireTableRowsUpdated(row, row);
				previewPanel.repaint(500);
			}
			fireTableCellUpdated(row, col);
		}
	}

	Logger log = LoggerFactory.getLogger(RedactDialog.class);
	private static final long serialVersionUID = 2027484467240740791L;

	// private PreviewPanel previewPanel;

	List<TextPattern> patterns = null;
	File pdfFile = null;
	File outFile = null;

	List<RedactLocation> redactLocations = new ArrayList<>();
	RedactionsTableModel tableModel = new RedactionsTableModel();

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
	private JTable table;
	Color backgroundColor = null;
	
	public RedactDialog() {
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setModal(false);
		setTitle("Redact Document");
		setAlwaysOnTop(true);
		
		JToolBar toolBar = new JToolBar();
		getContentPane().add(toolBar, BorderLayout.SOUTH);
		
		Button button = new Button("Redact");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				redactButtonHandler();
			}
		});
		toolBar.add(button);
		
		Button Close = new Button("Close");
		Close.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				close();
			}
		});
		toolBar.add(Close);
		//setModalityType(ModalityType.DOCUMENT_MODAL);

		JPanel panel = new JPanel(new BorderLayout());
		//panel.setMinimumSize(new Dimension(600, 500));
		getContentPane().add(panel, BorderLayout.CENTER);

		previewPanel = new PreviewPanel();
		//previewPanel.setMinimumSize(new Dimension(600, 400));
		previewPanel.setBackground(new java.awt.Color(255, 255, 255));
		previewPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		
		JScrollPane scrolls = new JScrollPane(previewPanel, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		panel.add(scrolls, BorderLayout.CENTER);
		
		JComboBox<Action> actionComboBox = new JComboBox<>();
        actionComboBox.addItem(Action.Ignore);
        actionComboBox.addItem(Action.Ask);
        actionComboBox.addItem(Action.Redact);
		
		table = new JTable();
		table.setFillsViewportHeight(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//table.setPreferredSize(new Dimension(400, 300));
		table.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table.setModel(new RedactionsTableModel());
		TableColumn pageColumn = table.getColumn(tableModel.getColumnName(0));
		pageColumn.setPreferredWidth(10);
		TableColumn actionColumn = table.getColumn(tableModel.getColumnName(3));
		actionColumn.setCellEditor(new DefaultCellEditor(actionComboBox));
		// Single click jumps to show redact location
		table.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent mouseEvent) {
		        int row = table.rowAtPoint(mouseEvent.getPoint());
		        if(0 <= row && row < redactLocations.size()) {
		        	markPageLocation(redactLocations.get(row));
		        }
		    }
		});
		table.getColumn(tableModel.getColumnName(3)).setCellRenderer(
	            new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;

				@Override
	               public Component getTableCellRendererComponent(JTable table,
	                     Object value, boolean isSelected, boolean hasFocus,
	                     int row, int column) {
	                  JLabel superRenderer = (JLabel)super.getTableCellRendererComponent(table, 
	                        value, isSelected, hasFocus, row, column);
	                  if(backgroundColor == null) backgroundColor = getBackground();
	                  RedactLocation rl = redactLocations.get(row);
	                  java.awt.Font f = superRenderer.getFont();
	                  if(!rl.pattern.policy.equals(Action.Ask) && !rl.pattern.policy.equals(rl.action)) {
	                	  if(Action.Redact.equals(rl.action)) {
	                		  superRenderer.setFont(f.deriveFont(Font.BOLDITALIC));
	                	  } else {
	                		  superRenderer.setFont(f.deriveFont(Font.ITALIC));
	                	  }
	                	  superRenderer.setToolTipText("Chosen action differs from this entity's policy of "+rl.pattern.policy.name());
	                  } else {
	                	  if(Action.Redact.equals(rl.action)) {
	                		  superRenderer.setFont(f.deriveFont(Font.BOLD));
	                	  }
	                	  superRenderer.setToolTipText(null);
	                  }
	                  if(!isSelected) {
		                  switch(rl.action) {
		                  case Ask:
		                   	  superRenderer.setBackground(previewPanel.askColor);
		                   	  break;
		                  case Redact:
		                	  superRenderer.setBackground(previewPanel.redactColor);
		                	  break;
		                  case Ignore:
		                	  superRenderer.setBackground(backgroundColor);
		                	  break;
		                  }
	                  }
	                  return superRenderer;
	               }
	            });
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		panel.add(scrollPane, BorderLayout.EAST);
		pack();
	}

	public void markPageLocation(RedactLocation loc) {
		if (loc.page != this.currentPage) {
			setPreviewPage(loc.page);
		}
		previewPanel.setPDFLocation(loc.loc, this.layoutDoc);
	}

	public void redactButtonHandler() {
		long asks = this.redactLocations.stream().filter(l -> Action.Ask.equals(l.action)).count();
		if(asks > 0) {
			JOptionPane.showMessageDialog(this.getContentPane(), "Please choose \"Redact\" or \"Ignore\" actions for the entities highlighted in yellow and marked \"Ask\".", "Redact Choices Required", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		List<PdfCleanUpLocation> cleanUps = this.redactLocations.stream().filter(x -> Action.Redact.equals(x.action)).map(x -> x.loc)
				.collect(Collectors.toList());
		PdfCleanUpTool cleaner = new PdfCleanUpTool(this.itextPDF, cleanUps);
		try {
			cleaner.cleanUp();
		} catch (IOException e) {
			e.printStackTrace();
		}
		closeDoc();
	}

	public void closeDoc() {
		this.itextPDF.close();
		this.itextPDF = null;
		this.setVisible(false);
		this.doc = null;
		this.pageImage = null;
		this.currentPage = -1;
		this.redactLocations.clear();
		tableModel.fireTableDataChanged();
	}
	
	public void close() {
		this.setVisible(false);
		previewPanel.clearPDFLocation();
		closeDoc();
	}

	public void startDoc(File pdfFile, File outputFile, List<TextPattern> filePatterns)
			throws FileNotFoundException, IOException {
		log.debug("Starting redaction dialog for {}, outputting to {} with {} patterns", pdfFile.getPath(), outputFile.getPath(), filePatterns.size()); 
		this.redactLocations = new ArrayList<>();
		this.patterns = filePatterns;
		this.pdfFile = pdfFile;
		this.outFile = outputFile;
		this.outFile.getParentFile().mkdirs();
		PDFDocument mydoc = new PDFDocument();
		mydoc.load(pdfFile);
		this.doc = mydoc;
		this.currentPage = -1;
		try {
			this.itextPDF = new PdfDocument(new PdfReader(pdfFile), new PdfWriter(outFile));
			this.layoutDoc = new Document(this.itextPDF);
			PatternFindingWorker worker = new PatternFindingWorker(itextPDF, this.patterns) {
				@Override
				protected void process(List<RedactLocation> newLocations) {
					log.info("Found {} redaction locations.", newLocations.size());
					redactLocations.addAll(newLocations);
					java.util.Collections.sort(redactLocations);
					if (currentPage == -1 && !newLocations.isEmpty()) {
						RedactLocation l = newLocations.get(0);
						markPageLocation(l);
					}
					tableModel.fireTableDataChanged();
				}
			};
			worker.execute();
		} catch (IOException e) {
			throw new Error(e);
		}
		this.previewPanel.clearPDFLocation();
		this.previewPanel.setRedactLocations(redactLocations);
		tableModel.fireTableDataChanged();
		if(this.currentPage == -1)
			setPreviewPage(1);
		pack();
	}

	private void setPreviewPage(int i) {
		try {
			SimpleRenderer renderer = new SimpleRenderer();
			renderer.setResolution(72);
			BufferedImage pageImage = (BufferedImage) renderer.render(doc, i - 1, i - 1).get(0);
			previewPanel.setPage(pageImage, i, itextPDF.getPage(i).getPageSize());
			this.currentPage = i;
		} catch (Exception e) {
			throw new Error(e);
		}
	}

}