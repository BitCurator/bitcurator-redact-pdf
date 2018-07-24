package bca.redact;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
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
import org.apache.commons.collections4.list.SetUniqueList;
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
	private JTable table_patterns;
	private JFileChooser fileChooser_pdfs;

	private List<File> pdfFiles = new ArrayList<>();
	private Map<File, PDFAnalysis> file_analyses = new HashMap<>();
	private MultiValuedMap<String, File> entity_files = new HashSetValuedHashMap<>();
	private Map<String, Entity> entities = new HashMap<String, Entity>();
	private List<String> entityOrder = new ArrayList<String>();
	private List<Object[]> patterns = new ArrayList<>();
	
	Object file_columnNames[] = { "Filename", "Path", "Redacted File" };
	Object pattern_columnNames[] = { "Name", "Regular Expression", "Policy", "Notes" };
	Object entity_columnNames[] = { "Entity Text", "Type", "Count", "Policy", "Notes" };

	class Entity {
		public Entity(String text, String type) {
			this.text = text;
			this.type = type;
			this.policy = defaultAction;
			this.count = 1;
			this.notes = "";
		}
		public String text;
		public String type;
		public Action policy;
		public String notes;
		public int count;
		public void incr() {
			this.count++;
		}
	}
	
	
	enum Action {
		Redact, Ignore, Ask
	}

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
			default:
				return "n/a";
			}
		}
	};

	AbstractTableModel tableModel_patterns = new AbstractTableModel() {
		private static final long serialVersionUID = 1L;

		public String getColumnName(int column) {
			return pattern_columnNames[column].toString();
		}

		public int getRowCount() {
			return patterns.size();
		}

		public int getColumnCount() {
			return pattern_columnNames.length;
		}
		
        public boolean isCellEditable(int row, int col) {
            return true;
        }

		@Override
		public void setValueAt(Object value, int row, int col) {
			Object[] pattern = patterns.get(row);
			pattern[col] = value;
		}

		public Object getValueAt(int row, int col) {
			Object[] pattern = patterns.get(row);
			return pattern[col];
		}
	};
	
	
	AbstractTableModel tableModel_entities = new AbstractTableModel() {
		private static final long serialVersionUID = 1L;

		public String getColumnName(int column) {
			return entity_columnNames[column].toString();
		}

		public int getRowCount() {
			return entities.size();
		}

		public int getColumnCount() {
			return entity_columnNames.length;
		}
		
        public boolean isCellEditable(int row, int col) {
            return col == 3 || col == 4;
        }

		@Override
		public void setValueAt(Object value, int row, int col) {
			String name = entityOrder.get(row);
			Entity e = entities.get(name);
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
		}

		public Object getValueAt(int row, int col) {
			String name = entityOrder.get(row);
			Entity e = entities.get(name);
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
		loadPattens();
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
		TableColumn policyColumn = table_entities.getColumn(entity_columnNames[2]);
		policyColumn.setCellEditor(new DefaultCellEditor(patternAction_comboBox));
		table_entities.setFillsViewportHeight(true);
		table_entities.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table_entities.setPreferredSize(new Dimension(400, 300));
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

		table_patterns = new JTable();
		table_patterns.setFillsViewportHeight(true);
		table_patterns.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table_patterns.setPreferredSize(new Dimension(400, 300));
		table_patterns.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table_patterns.setModel(tableModel_patterns);
		TableColumn patternPolicyColumn = table_patterns.getColumn(pattern_columnNames[2]);
		patternPolicyColumn.setCellEditor(new DefaultCellEditor(patternAction_comboBox));
		JScrollPane patterns_scrollPane = new JScrollPane(table_patterns);
		patterns_scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		panel_Patterns.add(patterns_scrollPane, BorderLayout.CENTER);

		JPanel panel_Files = new JPanel();
		panel_Files.setMinimumSize(minimumSize);
		splitPane.setLeftComponent(panel_Files);
		panel_Files.setLayout(new BorderLayout(0, 0));
				
		table = new JTable();
		table.setFillsViewportHeight(true);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setPreferredSize(new Dimension(400, 300));
		table.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		table.setModel(tableModel_pdfs);
		table.getColumn(file_columnNames[0]).setCellRenderer(
	            new DefaultTableCellRenderer() {
	               @Override
	               public Component getTableCellRendererComponent(JTable table,
	                     Object value, boolean isSelected, boolean hasFocus,
	                     int row, int column) {
	                  JLabel superRenderer = (JLabel)super.getTableCellRendererComponent(table, 
	                        value, isSelected, hasFocus, row, column);
	                  Color backgroundColor = getBackground();
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
		        	if(table.getSelectedRow() != -1) {
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

	private void loadPattens() {
		// name, regular expression, policy, notes
		patterns.add(new Object[] {"Social Security Number", "\\d{3}-\\d{2}-\\d{4}", Action.Redact, ""});
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
					if(a.entities != null) {
						Arrays.stream(a.entities)
							.forEach( x -> {
								if(!entities.containsKey(x[0])) {
									entities.put(x[0], new Entity(x[0], x[1]));
								} else {
									entities.get(x[0]).incr();
								}
							});
						Arrays.stream(a.entities).forEach( x -> {
							entity_files.put(x[0], a.file);
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
		List<Entity> fileEntities = new ArrayList<>();
		entity_files.entries().parallelStream().forEach( x -> {
			if(file.equals(x.getValue())) {
				fileEntities.add(entities.get(x.getKey()));
			}
		});
		File outFile = Paths.get(outPath, file.getAbsolutePath()).toFile();
		Collections.sort(fileEntities, Comparator.comparing(x -> { return ((Entity)x).text;})); 
		try {
			this.redactDialog.startDoc(file, outFile, fileEntities);
			this.redactDialog.setVisible(true);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
}
