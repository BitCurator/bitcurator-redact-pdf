package bca.redact;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

public class RedactionApp {
	private static final Logger log = Logger.getLogger(RedactionApp.class);
	private static final File[] EMPTY_FILE_ARRAY = new File[] {};

	private JFrame frmBitcuratorPdfRedact;
	private final JToolBar toolBar = new JToolBar();
	private JTable table_entities;
	private JTable table_pdfs;
	private JTable table_1;
	private JFileChooser fileChooser_pdfs;

	private List<File> pdfFiles = new ArrayList<>();
	private Set<File> analyzed = new HashSet<File>();
	private List<String> entities = new ArrayList<>();
	private Map<String, Action> entityAction = new HashMap<String, Action>();
	private Action defaultAction = Action.AskMe;

	enum Action {
		Redact, Ignore, AskMe
	}

	AbstractTableModel tableModel_pdfs = new AbstractTableModel() {
		private static final long serialVersionUID = 1L;
		Object columnNames[] = { "File", "Redacted Location" };

		public String getColumnName(int column) {
			return columnNames[column].toString();
		}

		public int getRowCount() {
			return pdfFiles.size();
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public Object getValueAt(int row, int col) {
			switch (col) {
			case 0:
				return pdfFiles.get(row).getAbsolutePath();
			case 1:
				return "TODO: output paths";
			default:
				return "n/a";
			}
		}
	};

	AbstractTableModel tableModel_entities = new AbstractTableModel() {
		private static final long serialVersionUID = 1L;
		Object columnNames[] = { "Entity", "Action" };

		public String getColumnName(int column) {
			return columnNames[column].toString();
		}

		public int getRowCount() {
			return entities.size();
		}

		public int getColumnCount() {
			return columnNames.length;
		}

		public Object getValueAt(int row, int col) {
			String s = entities.get(row);
			switch (col) {
			case 0:
				return s;
			case 1:
				if (entityAction.containsKey(s)) {
					return entityAction.get(s).name();
				} else {
					return defaultAction.name();
				}
			default:
				return "n/a";
			}
		}
	};

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					RedactionApp window = new RedactionApp();
					window.frmBitcuratorPdfRedact.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public RedactionApp() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frmBitcuratorPdfRedact = new JFrame();
		frmBitcuratorPdfRedact.setTitle("BitCurator PDF Redact");
		frmBitcuratorPdfRedact.setBounds(50, 50, 800, 600);
		frmBitcuratorPdfRedact.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frmBitcuratorPdfRedact.getContentPane().add(toolBar, BorderLayout.NORTH);

		JButton btnAddFiles = new JButton("Add PDFs");
		btnAddFiles.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				int retVal = fileChooser_pdfs.showOpenDialog(frmBitcuratorPdfRedact);
				if (retVal == JFileChooser.APPROVE_OPTION) {
					File[] files = fileChooser_pdfs.getSelectedFiles();
					addPDFPaths(files);
				}
			}
		});
		toolBar.add(btnAddFiles);

		JCheckBox chckbxDetectEntities = new JCheckBox("detect entities");
		chckbxDetectEntities.setSelected(true);
		toolBar.add(chckbxDetectEntities);

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		toolBar.add(separator);

		JButton btnBeginRedaction = new JButton("Begin Redaction");
		toolBar.add(btnBeginRedaction);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setDividerLocation(150);
		frmBitcuratorPdfRedact.getContentPane().add(splitPane, BorderLayout.CENTER);

		Dimension minimumSize = new Dimension(500, 400);

		JTabbedPane tabbedPane_1 = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane_1.setMinimumSize(minimumSize);
		splitPane.setRightComponent(tabbedPane_1);

		JPanel panel_Entities = new JPanel();
		panel_Entities.setBorder(new EmptyBorder(10, 10, 10, 10));
		tabbedPane_1.addTab("Found Entities", null, panel_Entities, null);
		panel_Entities.setLayout(new BorderLayout(0, 0));

		JTextArea txtrThisToolUses = new JTextArea();
		txtrThisToolUses.setBackground(UIManager.getColor("Panel.background"));
		txtrThisToolUses.setEditable(false);
		txtrThisToolUses.setWrapStyleWord(true);
		txtrThisToolUses.setLineWrap(true);
		txtrThisToolUses
				.setText("Named entities are people, places, and organizations detected in the text of PDF files.");
		panel_Entities.add(txtrThisToolUses, BorderLayout.NORTH);

		table_entities = new JTable();
		table_entities.setModel(tableModel_entities);
		table_entities.setFillsViewportHeight(true);
		table_entities.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table_entities.setPreferredSize(new Dimension(400, 300));
		panel_Entities.add(table_entities, BorderLayout.CENTER);

		JPanel panel_Patterns = new JPanel();
		panel_Patterns.setBorder(new EmptyBorder(10, 10, 10, 10));
		tabbedPane_1.addTab("Text Patterns", null, panel_Patterns, null);
		panel_Patterns.setLayout(new BorderLayout(0, 0));

		JTextArea textArea = new JTextArea();
		textArea.setWrapStyleWord(true);
		textArea.setText(
				"Patterns are regular expressions used to redact matching text in PDFs. Add new patterns by clicking in the empty first row.");
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setBackground(UIManager.getColor("Button.background"));
		panel_Patterns.add(textArea, BorderLayout.NORTH);

		table_1 = new JTable();
		table_1.setPreferredSize(new Dimension(400, 300));
		table_1.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		panel_Patterns.add(table_1, BorderLayout.CENTER);

		JPanel panel_Files = new JPanel();
		panel_Files.setMinimumSize(minimumSize);
		splitPane.setLeftComponent(panel_Files);
		panel_Files.setLayout(new BorderLayout(0, 0));

		table_pdfs = new JTable();
		table_pdfs.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table_pdfs.setModel(tableModel_pdfs);
		panel_Files.add(table_pdfs);

		JLabel lblStatus = new JLabel("output folder: none");
		frmBitcuratorPdfRedact.getContentPane().add(lblStatus, BorderLayout.SOUTH);
		frmBitcuratorPdfRedact.pack();

		fileChooser_pdfs = new JFileChooser();
		fileChooser_pdfs.setDialogTitle("Add PDF Files or Folders");
		fileChooser_pdfs.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fileChooser_pdfs.setAcceptAllFileFilterUsed(false);
		fileChooser_pdfs.setMultiSelectionEnabled(true);
		fileChooser_pdfs.setFileFilter(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				if (arg0.isDirectory())
					return true;
				if (arg0.getName().endsWith(".PDF"))
					return true;
				if (arg0.getName().endsWith(".pdf"))
					return true;
				return false;
			}

			@Override
			public String getDescription() {
				return "Folders and PDF Files";
			}

		});
	}

	private void addPDFPaths(final File[] pdfPaths) throws Error {
		SwingWorker<List<File>, File> sw = new SwingWorker<List<File>, File>() {
			@Override
			protected void process(List<File> chunks) {
				tableModel_pdfs.fireTableDataChanged();
			}

			File[] empty = new File[] {};

			@Override
			protected List<File> doInBackground() throws Exception {
				doList(pdfPaths);
				return pdfFiles;
			}

			private void doList(File[] paths) {
				List<File> chunks = new ArrayList<>();
				for (File file : paths) {
					if (!file.isDirectory()) {
						if (pdfFiles.contains(file))
							continue;
						pdfFiles.add(file);
						chunks.add(file);
					}
				}
				publish(chunks.toArray(empty));
				for (File file : paths) {
					if (file.isDirectory()) {
						File[] dirFiles = file.listFiles(new java.io.FileFilter() {
							@Override
							public boolean accept(File f) {
								if (f.isDirectory())
									return true;
								if (f.getName().endsWith(".PDF"))
									return true;
								if (f.getName().endsWith(".pdf"))
									return true;
								return false;
							}
						});
						doList(dirFiles);
					}
				}
			}

		};
		sw.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				switch (event.getPropertyName()) {
				case "progress":
					break;
				case "state":
					switch ((StateValue) event.getNewValue()) {
					case DONE:
						System.out.println("HERE SYSTEM");
						log.info("HERE");
						startEntityAnalysisWorker();
					}
				}
			}
		});
		sw.execute();
	}

	private void startEntityAnalysisWorker() {
		log.info("starting analysis");
		PDFAnalysisWorker pdfAnalysisWorker = new PDFAnalysisWorker(pdfFiles.toArray(EMPTY_FILE_ARRAY));
		pdfAnalysisWorker.addPropertyChangeListener(new PropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent evt) {
				StateValue s = (StateValue) evt.getNewValue();
				log.info("state: "+ s.name());
				if (s == StateValue.DONE) {
					try {
						Set<String> result = pdfAnalysisWorker.get();
						result.removeAll(entities);
						entities.addAll(result);
						Collections.sort(entities);
						tableModel_entities.fireTableDataChanged();
					} catch (InterruptedException e) {
						throw new Error(e);
					} catch (ExecutionException e) {
						throw new Error(e);
					}
				}
			}
		});
		pdfAnalysisWorker.execute();
	}
}
