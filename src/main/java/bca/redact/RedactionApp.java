package bca.redact;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.HashSetValuedHashMap;
import org.apache.log4j.Logger;
import org.docopt.Docopt;

public class RedactionApp {
	private static final Logger log = Logger.getLogger(RedactionApp.class);
	private static final File[] EMPTY_FILE_ARRAY = new File[] {};
	protected static final Color ANALYZED_BG_COLOR = Color.lightGray;
	private Action defaultAction = Action.Ignore;
	
	private JFrame frmBitcuratorPdfRedact;
	private final JToolBar toolBar = new JToolBar();
	private JTable table_entities;
	private JTable table_expressions;
	private JFileChooser fileChooser_pdfs;

	private List<File> pdfFiles = new ArrayList<>();
	private Map<File, PDFAnalysis> file_analyses = new HashMap<>();
	private MultiValuedMap<String, File> pattern_files = new HashSetValuedHashMap<>();
	private Map<String, EntityPattern> entities = new HashMap<String, EntityPattern>();
	private List<String> entityOrder = new ArrayList<String>();
	private Map<String, ExpressionPattern> expressions = new HashMap<>();
	private List<String> expressionOrder = new ArrayList<String>();
	
	Object file_columnNames[] = { "Filename", "Path", "Redacted File" };

	AbstractTableModel tableModel_pdfs = new AbstractTableModel() {
		private static final long serialVersionUID = 1L;

		public String getColumnName(int column) {
			return file_columnNames[column].toString();
		}

		public int getRowCount() {
			return pdfFiles.size();
		}

		public int getColumnCount() {
			return file_columnNames.length;
		}

		public Object getValueAt(int row, int col) {
			switch (col) {
			case 0:
				return pdfFiles.get(row).getName();
			case 1:
				return pdfFiles.get(row).getParentFile().getPath();
			case 2:
				return getOutputFile(pdfFiles.get(row)).getPath();
			default:
				return "n/a";
			}
		}
	};

	AbstractTableModel tableModel_patterns = new AbstractTableModel() {
		public Object columnNames[] = { "Name", "Expression", "Default Action", "Notes" };
		private static final long serialVersionUID = 1L;

		public String getColumnName(int column) {
			return columnNames[column].toString();
		}

		public int getRowCount() {
			return expressions.size();
		}

		public int getColumnCount() {
			return columnNames.length;
		}
		
        public boolean isCellEditable(int row, int col) {
            return true;
        }

		@Override
		public void setValueAt(Object value, int row, int col) {
			String key = expressionOrder.get(row);
			ExpressionPattern p = expressions.get(key);
			switch(col) {
			case 0:
				p.label = (String)value;
				return;
			case 1:
				// TODO check valid expression
				p.regex = (String)value;
				return;
			case 2:
				p.policy = (Action)value;
				return;
			case 3:
				p.notes = (String)value;
				return;
			}
			fireTableCellUpdated(row, col);
		}

		public Object getValueAt(int row, int col) {
			String key = expressionOrder.get(row);
			ExpressionPattern p = expressions.get(key);
			switch(col) {
			case 0:
				return p.label;
			case 1:
				return p.regex;
			case 2:
				return p.policy;
			case 3:
				return p.notes;
			default:
				return "";
			}
		}
	};
	
	
	AbstractTableModel tableModel_entities = new AbstractTableModel() {
		private static final long serialVersionUID = 1L;
		Object columnNames[] = { "Entity Text", "Type", "Count", "Default Action", "Notes" };

		public String getColumnName(int column) {
			return columnNames[column].toString();
		}

		public int getRowCount() {
			return entities.size();
		}

		public int getColumnCount() {
			return columnNames.length;
		}
		
        public boolean isCellEditable(int row, int col) {
            return col == 3 || col == 4;
        }

		@Override
		public void setValueAt(Object value, int row, int col) {
			String name = entityOrder.get(row);
			EntityPattern e = entities.get(name);
			switch(col) {
			case 0:
				e.text = (String)value;
				return;
			case 1:
				e.type = (String)value;
				return;
			case 3:
				e.policy = (Action)value;
				return;
			case 4:
				e.notes = (String)value;
				return;
			}
			fireTableCellUpdated(row, col);
		}

		public Object getValueAt(int row, int col) {
			String name = entityOrder.get(row);
			EntityPattern e = entities.get(name);
			switch(col) {
			case 0:
				return e.text;
			case 1:
				return e.type;
			case 2:
				return e.count;
			case 3:
				return e.policy;
			case 4:
				return e.notes;
			}
			return "?";
		}
	};
	private JTable table;
	private String outPath;
	private RedactDialog redactDialog;
	private final javax.swing.Action actionAddPDFs = new AbstractAction("Add PDFs") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			int retVal = fileChooser_pdfs.showOpenDialog(frmBitcuratorPdfRedact);
			if (retVal == JFileChooser.APPROVE_OPTION) {
				File[] files = fileChooser_pdfs.getSelectedFiles();
				addPDFPaths(files);
			}
		}
	};
	private final javax.swing.Action actionClearPDFs = new AbstractAction("Clear PDFs") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			entities.clear();
			entityOrder.clear();
			tableModel_entities.fireTableDataChanged();
			clearPDFPaths();
		}
		@Override
		public boolean isEnabled() {
			return pdfFiles != null && pdfFiles.size() > 0;
		}
	};
	private javax.swing.Action actionRunEntityAnalysis = new AbstractAction("Detect Entities") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			entities.clear();
			entityOrder.clear();
			tableModel_entities.fireTableDataChanged();
			startEntityAnalysisWorker();
		}
		@Override
		public boolean isEnabled() {
			return pdfFiles != null && pdfFiles.size() > 0;
		}
	};
	

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		InputStream doc = RedactionApp.class.getClassLoader().getResourceAsStream("docopt.txt");
		final Map<String, Object> opts = new Docopt(doc).withVersion("BitCurator PDF Redactor 0.1.0").parse(args);

		// Validate output path.
		String outPathArg = (String) opts.get("--output-dir");
		final String outPathAbsolute = Paths.get(outPathArg).toFile().getAbsolutePath();

		// Build the list of PDFs
		@SuppressWarnings("unchecked")
		List<Object> infiles = (List<Object>)opts.get("FILE");
		File[] files = infiles.stream()
				.map(a -> new File((String) a))
				.collect(Collectors.toCollection(ArrayList::new)).toArray(EMPTY_FILE_ARRAY);
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					RedactionApp window = new RedactionApp();
					window.setOutPath(outPathAbsolute);
					window.frmBitcuratorPdfRedact.setVisible(true);
					window.addPDFPaths(files);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	protected void setOutPath(String outPath) {
		this.outPath = outPath;
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
		loadExpressions();
		frmBitcuratorPdfRedact = new JFrame();
		frmBitcuratorPdfRedact.setTitle("BitCurator PDF Redact");
		frmBitcuratorPdfRedact.setBounds(50, 50, 800, 600);
		frmBitcuratorPdfRedact.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		frmBitcuratorPdfRedact.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		mnFile.setMnemonic('F');
		
		JMenuItem mntmAddPdfs = new JMenuItem("Add PDFs");
		mntmAddPdfs.setAction(actionAddPDFs);
		mnFile.add(mntmAddPdfs);
		
		JMenuItem mntmClearPdfs = new JMenuItem("Clear PDFs");
		mntmClearPdfs.setAction(actionClearPDFs);
		mnFile.add(mntmClearPdfs);
		
		JMenu mnNewMenu = new JMenu("Named Entities");
		mnNewMenu.setMnemonic('N');
		menuBar.add(mnNewMenu);
		
		JMenu mnRecognitionTool = new JMenu("Recognition Tool");
		mnNewMenu.add(mnRecognitionTool);
		
		JRadioButtonMenuItem rdbtnmntmCorenlp = new JRadioButtonMenuItem("CoreNLP");
		rdbtnmntmCorenlp.setSelected(true);
		mnRecognitionTool.add(rdbtnmntmCorenlp);
		
		JRadioButtonMenuItem rdbtnmntmSpacey = new JRadioButtonMenuItem("Spacey");
		mnRecognitionTool.add(rdbtnmntmSpacey);
		
		JCheckBoxMenuItem chckbxmntmNewCheckItem = new JCheckBoxMenuItem("Detect People");
		chckbxmntmNewCheckItem.setSelected(true);
		mnNewMenu.add(chckbxmntmNewCheckItem);
		
		JCheckBoxMenuItem chckbxmntmDetectOrganizations = new JCheckBoxMenuItem("Detect Organizations");
		chckbxmntmDetectOrganizations.setSelected(true);
		mnNewMenu.add(chckbxmntmDetectOrganizations);
		
		JCheckBoxMenuItem chckbxmntmDetectPlaces = new JCheckBoxMenuItem("Detect Places");
		mnNewMenu.add(chckbxmntmDetectPlaces);
		
		JCheckBoxMenuItem chckbxmntmNewCheckItem_1 = new JCheckBoxMenuItem("Detect ??");
		mnNewMenu.add(chckbxmntmNewCheckItem_1);
		
		JMenuItem mntmClearNamedEntities = new JMenuItem("Detect Entities");
		mntmClearNamedEntities.setAction(actionRunEntityAnalysis);
		mnNewMenu.add(mntmClearNamedEntities);
		
		JMenu mnHelp = new JMenu("Help");
		mnHelp.setHorizontalAlignment(SwingConstants.RIGHT);
		mnHelp.setMnemonic('H');
		menuBar.add(mnHelp);
		
		JMenuItem mntmOverview = new JMenuItem("Overview");
		mnHelp.add(mntmOverview);
		
		JMenuItem mntmAbout = new JMenuItem("About");
		mnHelp.add(mntmAbout);
		frmBitcuratorPdfRedact.getContentPane().add(toolBar, BorderLayout.NORTH);

		JButton btnAddFiles = new JButton("Add PDFs");
		btnAddFiles.setAction(actionAddPDFs);
		toolBar.add(btnAddFiles);
		
		JButton btnClearPdfs = new JButton("Clear PDFs");
		toolBar.add(btnClearPdfs);
		
		JButton btnClearEntities = new JButton("Clear Entities");
		toolBar.add(btnClearEntities);

		JSeparator separator = new JSeparator();
		separator.setOrientation(SwingConstants.VERTICAL);
		toolBar.add(separator);

		JButton btnBeginRedaction = new JButton("Begin Redaction");
		toolBar.add(btnBeginRedaction);

		JSplitPane splitPane = new JSplitPane();
		splitPane.setDividerLocation(250);
		frmBitcuratorPdfRedact.getContentPane().add(splitPane, BorderLayout.CENTER);

		Dimension minimumSize = new Dimension(500, 400);

		JTabbedPane tabbedPane_1 = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane_1.setMinimumSize(minimumSize);
		splitPane.setRightComponent(tabbedPane_1);

		JPanel panel_Entities = new JPanel();
		panel_Entities.setBorder(new EmptyBorder(10, 10, 10, 10));
		tabbedPane_1.addTab("Named Entities", null, panel_Entities, null);
		panel_Entities.setLayout(new BorderLayout(0, 0));

		JTextArea txtrThisToolUses = new JTextArea();
		txtrThisToolUses.setBackground(UIManager.getColor("Panel.background"));
		txtrThisToolUses.setEditable(false);
		txtrThisToolUses.setWrapStyleWord(true);
		txtrThisToolUses.setLineWrap(true);
		txtrThisToolUses
				.setText("Named entities are people, places, and organizations detected in the text of PDF files you have added.");
		panel_Entities.add(txtrThisToolUses, BorderLayout.NORTH);

		//Set up the editor for the sport cells.
        JComboBox<Action> patternAction_comboBox = new JComboBox<>();
        patternAction_comboBox.addItem(Action.Ignore);
        patternAction_comboBox.addItem(Action.Ask);
        patternAction_comboBox.addItem(Action.Redact);
		
		table_entities = new JTable();
		table_entities.setModel(tableModel_entities);
		TableColumn policyColumn = table_entities.getColumn(tableModel_entities.getColumnName(3));
		policyColumn.setCellEditor(new DefaultCellEditor(patternAction_comboBox));
		//table_entities.setFillsViewportHeight(true);
		table_entities.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		//table_entities.setPreferredSize(new Dimension(400, 300));
		JScrollPane entities_scrollPane = new JScrollPane(table_entities);
		entities_scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panel_Entities.add(entities_scrollPane, BorderLayout.CENTER);

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

		table_expressions = new JTable();
		//table_expressions.setFillsViewportHeight(true);
		table_expressions.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//table_expressions.setPreferredSize(new Dimension(400, 300));
		table_expressions.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table_expressions.setModel(tableModel_patterns);
		TableColumn patternPolicyColumn = table_expressions.getColumn(tableModel_patterns.getColumnName(2));
		patternPolicyColumn.setCellEditor(new DefaultCellEditor(patternAction_comboBox));
		JScrollPane patterns_scrollPane = new JScrollPane(table_expressions);
		patterns_scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panel_Patterns.add(patterns_scrollPane, BorderLayout.CENTER);

		JPanel panel_Files = new JPanel();
		panel_Files.setMinimumSize(minimumSize);
		splitPane.setLeftComponent(panel_Files);
		panel_Files.setLayout(new BorderLayout(0, 0));
				
		table = new JTable();
		//table.setFillsViewportHeight(true);
		table.setPreferredScrollableViewportSize(new Dimension(200, 600));
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table.setModel(tableModel_pdfs);
		table.getColumn(file_columnNames[0]).setCellRenderer(
	            new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;

				@Override
	               public Component getTableCellRendererComponent(JTable table,
	                     Object value, boolean isSelected, boolean hasFocus,
	                     int row, int column) {
	                  JLabel superRenderer = (JLabel)super.getTableCellRendererComponent(table, 
	                        value, isSelected, hasFocus, row, column);
	                  //Color backgroundColor = getBackground();
	                  if(file_analyses.containsKey(pdfFiles.get(row))) {
	                	  PDFAnalysis a = file_analyses.get(pdfFiles.get(row));
	                	  if(a.error != null) {
	                		  superRenderer.setForeground(Color.red);
	                		  if(a.error.getCause() != null) {
	                			  superRenderer.setToolTipText(a.error.getCause().getMessage());
	                		  }
	                	  } else {
	                		  superRenderer.setForeground(Color.black);
	                		  superRenderer.setToolTipText(a.entities.length + " named entities");
	                	  }
	                  } else {
	                     superRenderer.setForeground(Color.gray);
	                     superRenderer.setToolTipText("not analyzed");
	                  }
	                  return superRenderer;
	               }
	            });
		table.addMouseListener(new MouseAdapter() {
		    public void mousePressed(MouseEvent mouseEvent) {
		        if (mouseEvent.getClickCount() == 2) {
		        	int row = table.rowAtPoint(mouseEvent.getPoint());
		        	if(0 <= row && row < pdfFiles.size()) {
		        		redact(pdfFiles.get(row));
		            }
		        }
		    }
		});
		JScrollPane scrollPane = new JScrollPane(table);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panel_Files.add(scrollPane, BorderLayout.CENTER);

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
		this.redactDialog = new RedactDialog();
	}

	private void loadExpressions() {
		// name, regular expression, policy, notes
		ExpressionPattern p = new ExpressionPattern("Social Security Number", "\\d{3}-\\d{2}-\\d{4}", Action.Redact);
		expressionOrder.add(p.getRegex());
		expressions.put(p.getRegex(), p);
	}
	
	private void clearPDFPaths() {
		pdfFiles.clear();
		tableModel_pdfs.fireTableDataChanged();
	}

	private void addPDFPaths(final File[] pdfPaths) throws Error {
		SwingWorker<List<File>, File> sw = new SwingWorker<List<File>, File>() {
			@Override
			protected void process(List<File> chunks) {
				Collections.sort(pdfFiles);
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
						startEntityAnalysisWorker();
					default:
					}
				}
			}
		});
		sw.execute();
	}

	private void startEntityAnalysisWorker() {
		log.info("starting analysis");
		PDFAnalysisWorker pdfAnalysisWorker = new PDFAnalysisWorker(pdfFiles.toArray(EMPTY_FILE_ARRAY)) {

			@Override
			protected void process(List<PDFAnalysis> chunks) {
				for(PDFAnalysis a : chunks) {
					if(a.entities != null && a.entities.length > 0) {
						Arrays.stream(a.entities)
							.forEach( x -> {
								EntityPattern p = new EntityPattern(x[0], x[1], defaultAction);
								if(!entities.containsKey(p.getLabel())) {
									entities.put(p.getLabel(), p);
								} else {
									entities.get(p.getLabel()).incr();
								}
								pattern_files.put(p.getLabel(), a.file);
							});
					}
					file_analyses.put(a.file, a);
				}
				entityOrder.clear();
				entityOrder.addAll(entities.keySet());
				Collections.sort(entityOrder);
				tableModel_entities.fireTableDataChanged();
				table.repaint();
			}
			
		};
		pdfAnalysisWorker.execute();
	}
	
	private void redact(File file) {
		// TODO Alert if redacted file already exists..
		List<TextPattern> filePatterns = new ArrayList<>();
		pattern_files.entries().parallelStream().forEach( x -> {
			if(file.equals(x.getValue())) {
				TextPattern p = entities.get(x.getKey());
				if(p != null) filePatterns.add(p);
			}
		});
		filePatterns.stream().filter( x -> x == null).forEach( x -> System.out.println(x));
		//log.error("nulls: "+count);
		File outFile = getOutputFile(file);
		Collections.sort(filePatterns, Comparator.comparing(x -> { return ((TextPattern)x).getLabel();}));
		filePatterns.addAll(this.expressions.values());
		try {
			this.redactDialog.startDoc(file, outFile, filePatterns);
			this.redactDialog.setVisible(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private File getOutputFile(File inputFile) {
		File outFile = Paths.get(outPath, inputFile.getAbsolutePath()).toFile();
		return outFile;
	}
}
