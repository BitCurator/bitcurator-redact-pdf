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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.SwingWorker.StateValue;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.apache.commons.collections4.ListValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.apache.log4j.Logger;
import org.docopt.Docopt;

public class RedactionApp {
	private static final Logger log = Logger.getLogger(RedactionApp.class);
	private static final File[] EMPTY_FILE_ARRAY = new File[] {};
	private static final String DEFAULT_PATTERNS_PATH = "bca_redact-pdf/default_patterns.txt";
	protected static final Color ANALYZED_BG_COLOR = Color.lightGray;
	private Action defaultAction = Action.Ignore;
	
	private JFrame frmBitcuratorPdfRedact;
	private JTable table_entities;
	private JTable table_expressions;
	private JFileChooser fileChooser_pdfs;
	private JFileChooser fileChooser_textpattern;
	private ButtonGroup nlpToolGroup;
	private JCheckBoxMenuItem chckbxmntmDetectPlaces;
	private JCheckBoxMenuItem chckbxmntmDetectPersons;
	private JCheckBoxMenuItem chckbxmntmDetectOrganizations;
	
	
	private List<File> pdfFiles = new ArrayList<>();
	private Map<File, PDFAnalysis> file_analyses = new HashMap<>();
	private ListValuedMap<String, File> pattern_files = new ArrayListValuedHashMap<>();
	private Map<String, EntityPattern> entities = new HashMap<String, EntityPattern>();
	private List<String> entityOrder = new ArrayList<String>();
	private List<ExpressionPattern> expressions = new ArrayList<>();
	private boolean useSpacy = false;
	Color backgroundColor = null;
	
	Object file_columnNames[] = { "Filename", "Path", "Output" };

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
				File output = getOutputFile(pdfFiles.get(row));
				if(output.exists()) {
					return output.getPath();
				} else {
					return "";
				}
			default:
				return "n/a";
			}
		}
	};

	AbstractTableModel tableModel_patterns = new AbstractTableModel() {
		public Object columnNames[] = { "Name", "Expression", "Action" };
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
			ExpressionPattern p = expressions.get(row);
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
			}
			fireTableCellUpdated(row, col);
		}

		public Object getValueAt(int row, int col) {
			ExpressionPattern p = expressions.get(row);
			switch(col) {
			case 0:
				return p.label;
			case 1:
				return p.regex;
			case 2:
				return p.policy;
			default:
				return "";
			}
		}
	};
	
	
	AbstractTableModel tableModel_entities = new AbstractTableModel() {
		private static final long serialVersionUID = 1L;
		Object columnNames[] = { "Entity Text", "Type", "#", "Files", "Action"};

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
            return col == 4;
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
			case 4:
				e.policy = (Action)value;
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
				return pattern_files.get(e.getLabel()).stream().distinct().count();
			case 4:
				return e.policy;
			}
			return "?";
		}
	};
	private JTable table;
	private String outPath;
	private RedactDialog redactDialog;
	private final javax.swing.Action actionAddPDFs = new AbstractAction("Open File(s)..") {
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
	private final javax.swing.Action actionClearPDFs = new AbstractAction("Clear All") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			entities.clear();
			entityOrder.clear();
			pattern_files.clear();
			tableModel_entities.fireTableDataChanged();
			clearPDFPaths();
		}
	};
	private javax.swing.Action actionRunEntityAnalysis = new AbstractAction("Run Recognition") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			entities.clear();
			entityOrder.clear();
			pattern_files.clear();
			tableModel_entities.fireTableDataChanged();
			startEntityAnalysisWorker();
		}
	};
	
	private javax.swing.Action actionImportPatterns = new AbstractAction("Open File(s)..") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			fileChooser_textpattern.setDialogTitle("Open text patterns file(s)");
			int retVal = fileChooser_textpattern.showOpenDialog(frmBitcuratorPdfRedact);
			if (retVal == JFileChooser.APPROVE_OPTION) {
				File[] files = fileChooser_textpattern.getSelectedFiles();
				for(File f : files) {
					TextPatternUtil.loadExpressionActionList(f).stream()
					.forEach(p -> {
						log.info(p.label);
						expressions.add(p);
					});
				}
				tableModel_patterns.fireTableDataChanged();
			}
		}
	};
	
	private javax.swing.Action actionImportBEPatterns = new AbstractAction("Import Bulk Extractor features..") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			fileChooser_textpattern.setDialogTitle("Import Bulk Extractor features..");
			int retVal = fileChooser_textpattern.showOpenDialog(frmBitcuratorPdfRedact);
			if (retVal == JFileChooser.APPROVE_OPTION) {
				File[] files = fileChooser_textpattern.getSelectedFiles();
				for(File f : files) {
					TextPatternUtil.loadBEFeatures(f).stream()
					.forEach(p -> {
						expressions.add(p);
					});
				}
			}
			tableModel_patterns.fireTableDataChanged();
		}
	};
	
	private ActionListener analysisToolListener = new ActionListener() {
		@Override
		public void actionPerformed(ActionEvent e) {
			useSpacy = "spaCy".equals(e.getActionCommand());
		}
	};
	
	private javax.swing.Action actionClearEntities = new AbstractAction("Clear All") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			entities = Collections.emptyMap();
			entityOrder = Collections.emptyList();
			tableModel_entities.fireTableDataChanged();
		}
	};
	
	private javax.swing.Action actionSaveAsPatterns = new AbstractAction("Save As..") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser jfc = new JFileChooser();
			jfc.setDialogType(JFileChooser.SAVE_DIALOG);
			jfc.setDialogTitle("Save Text Patterns to File");
			int retVal = jfc.showOpenDialog(frmBitcuratorPdfRedact);
			if(retVal != JFileChooser.CANCEL_OPTION) {
				File f = jfc.getSelectedFile();
				TextPatternUtil.saveExpressionPatterns(f, expressions);
			}
		}
	};
	
	private javax.swing.Action actionResetDefaultPatterns = new AbstractAction("Reset to Defaults") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			expressions.clear();
			TextPatternUtil.loadExpressionActionList(new File(DEFAULT_PATTERNS_PATH))
				.stream()
				.forEach(p -> {
					expressions.add(p);
				});
			tableModel_patterns.fireTableDataChanged();
		}			
	};
	
	private javax.swing.Action actionSaveDefaultPatterns = new AbstractAction("Save as Defaults") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			TextPatternUtil.saveExpressionPatterns(new File(DEFAULT_PATTERNS_PATH), expressions);
		}
	};
	
	private javax.swing.Action actionClearPatterns = new AbstractAction("Clear All") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent e) {
			expressions.clear();
			tableModel_patterns.fireTableDataChanged();
		}
	};
	private javax.swing.Action actionAddPattern = new AbstractAction("New Pattern") {
		private static final long serialVersionUID = 1L;
		@Override
		public void actionPerformed(ActionEvent arg0) {
			expressions.add(new ExpressionPattern("Add a label", "", Action.Ask));
			tableModel_patterns.fireTableDataChanged();
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
		
		JMenu mnFile = new JMenu("PDF Files");
		menuBar.add(mnFile);
		mnFile.setMnemonic('F');
		
		JMenuItem mntmAddPdfs = new JMenuItem("Open File(s)..");
		mntmAddPdfs.setAction(actionAddPDFs);
		mnFile.add(mntmAddPdfs);
		
		JMenuItem mntmClearPdfs = new JMenuItem("Clear All");
		mntmClearPdfs.setAction(actionClearPDFs);
		mnFile.add(mntmClearPdfs);
		
		JMenu mnNewMenu = new JMenu("Entity Recognition");
		mnNewMenu.setMnemonic('E');
		menuBar.add(mnNewMenu);
		
		JMenu mnRecognitionTool = new JMenu("Recognition Tool");
		mnNewMenu.add(mnRecognitionTool);
		
		JRadioButtonMenuItem rdbtnmntmCorenlp = new JRadioButtonMenuItem("CoreNLP");
		rdbtnmntmCorenlp.setSelected(true);
		rdbtnmntmCorenlp.setActionCommand("CoreNLP");
		rdbtnmntmCorenlp.addActionListener(analysisToolListener);
		mnRecognitionTool.add(rdbtnmntmCorenlp);
		
		JRadioButtonMenuItem rdbtnmntmSpacy = new JRadioButtonMenuItem("spaCy");
		rdbtnmntmSpacy.setActionCommand("spaCy");
		rdbtnmntmSpacy.addActionListener(analysisToolListener);
		mnRecognitionTool.add(rdbtnmntmSpacy);
		nlpToolGroup = new ButtonGroup();
		nlpToolGroup.add(rdbtnmntmSpacy);
		nlpToolGroup.add(rdbtnmntmCorenlp);
		
		JMenuItem mntmRunEntities = new JMenuItem("Run Entity Recognition");
		mntmRunEntities.setAction(actionRunEntityAnalysis);
		mnNewMenu.add(mntmRunEntities);
		
		chckbxmntmDetectPersons = new JCheckBoxMenuItem("Include Persons");
		chckbxmntmDetectPersons.setSelected(true);
		mnNewMenu.add(chckbxmntmDetectPersons);
		
		chckbxmntmDetectPlaces = new JCheckBoxMenuItem("Include Locations");
		chckbxmntmDetectPlaces.setSelected(true);
		mnNewMenu.add(chckbxmntmDetectPlaces);
		
		chckbxmntmDetectOrganizations = new JCheckBoxMenuItem("Include Organizations");
		chckbxmntmDetectOrganizations.setSelected(true);
		mnNewMenu.add(chckbxmntmDetectOrganizations);
		
		JMenuItem mntmClearNamedEntities = new JMenuItem("Clear All");
		mntmClearNamedEntities.setAction(actionClearEntities);
		mnNewMenu.add(mntmClearNamedEntities);
		
		JMenu mnTextPatterns = new JMenu("Text Patterns");
		menuBar.add(mnTextPatterns);
		
		JMenuItem mntmNewPattern = new JMenuItem("Add Pattern");
		mntmNewPattern.setAction(actionAddPattern);
		mnTextPatterns.add(mntmNewPattern);
		
		JMenuItem mntmImportPatterns = new JMenuItem("Open File(s)..");
		mntmImportPatterns.setAction(actionImportPatterns);
		mnTextPatterns.add(mntmImportPatterns);
		
		JMenuItem mntmSaveAsPatterns = new JMenuItem("Save as..");
		mntmSaveAsPatterns.setAction(actionSaveAsPatterns);
		mnTextPatterns.add(mntmSaveAsPatterns);
		
		JMenuItem mntmResetDefaultPatterns = new JMenuItem("Reset to Defaults");
		mntmResetDefaultPatterns.setAction(actionResetDefaultPatterns);
		mnTextPatterns.add(mntmResetDefaultPatterns);
		
		JMenuItem mntmSaveDefaultPatterns = new JMenuItem("Save as Defaults");
		mntmSaveDefaultPatterns.setAction(actionSaveDefaultPatterns);
		mnTextPatterns.add(mntmSaveDefaultPatterns);
		
		JMenuItem mntmClearPatterns = new JMenuItem("Clear All");
		mntmClearPatterns.setAction(actionClearPatterns);
		mnTextPatterns.add(mntmClearPatterns);
		
		JMenuItem mntmImportBEFeatures = new JMenuItem("Import Bulk Extractor features..");
		mntmImportBEFeatures.setAction(actionImportBEPatterns);
		mnTextPatterns.add(mntmImportBEFeatures);
		
		JMenu mnHelp = new JMenu("Help");
		mnHelp.setHorizontalAlignment(SwingConstants.RIGHT);
		mnHelp.setMnemonic('H');
		menuBar.add(mnHelp);
		
		JMenuItem mntmOverview = new JMenuItem("Overview");
		mnHelp.add(mntmOverview);
		
		JMenuItem mntmAbout = new JMenuItem("About");
		mnHelp.add(mntmAbout);

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
		table_entities.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table_entities.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table_entities.setModel(tableModel_entities);
		TableCellRenderer rend = table_entities.getTableHeader().getDefaultRenderer();
		TableColumn tc0 = table_entities.getColumn(tableModel_entities.getColumnName(0));
		tc0.setHeaderRenderer(rend);
		tc0.setPreferredWidth(250);
		TableColumn tc1 = table_entities.getColumn(tableModel_entities.getColumnName(1));
		tc1.setHeaderRenderer(rend);
		tc1.setPreferredWidth(150);
		TableColumn tc2 = table_entities.getColumn(tableModel_entities.getColumnName(2));
		tc2.setHeaderRenderer(rend);
		//tc2.sizeWidthToFit();
		tc2.setPreferredWidth(35);
		TableColumn tc3 = table_entities.getColumn(tableModel_entities.getColumnName(3));
		tc3.setHeaderRenderer(rend);
		//tc3.sizeWidthToFit();
		tc3.setPreferredWidth(35);
		TableColumn policyColumn = table_entities.getColumn(tableModel_entities.getColumnName(4));
		policyColumn.setHeaderRenderer(rend);
		policyColumn.setPreferredWidth(100);
		policyColumn.setCellEditor(new DefaultCellEditor(patternAction_comboBox));
		//table_entities.setFillsViewportHeight(true);
		table_entities.setBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		//table_entities.setPreferredSize(new Dimension(400, 300));
		table_entities.getColumn(tableModel_entities.getColumnName(0)).setCellRenderer(
	            new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;

				@Override
	               public Component getTableCellRendererComponent(JTable table,
	                     Object value, boolean isSelected, boolean hasFocus,
	                     int row, int column) {
	                  JLabel superRenderer = (JLabel)super.getTableCellRendererComponent(table, 
	                        value, isSelected, hasFocus, row, column);
	                  String name = entityOrder.get(row);
	      			  EntityPattern e = entities.get(name);
	                  superRenderer.setToolTipText(e.getExampleSentence());
	                  return superRenderer;
	               }
	            });
		table_entities.getColumn(tableModel_entities.getColumnName(4)).setCellRenderer(
	            new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;

				@Override
	               public Component getTableCellRendererComponent(JTable table,
	                     Object value, boolean isSelected, boolean hasFocus,
	                     int row, int column) {
	                  JLabel superRenderer = (JLabel)super.getTableCellRendererComponent(table, 
	                        value, isSelected, hasFocus, row, column);
	                  if(backgroundColor == null) backgroundColor = getBackground();
	                  Action action = (Action)tableModel_entities.getValueAt(row, column);
	                  if(!isSelected) {
		                  switch(action) {
		                  case Ask:
		                   	  superRenderer.setBackground(PreviewPanel.askColor);
		                   	  break;
		                  case Redact:
		                	  superRenderer.setBackground(PreviewPanel.redactColor);
		                	  break;
		                  case Ignore:
		                	  superRenderer.setBackground(backgroundColor);
		                	  break;
		                  }
	                  }
	                  return superRenderer;
	               }
	            });
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
		JPopupMenu popupMenu = new JPopupMenu();
		popupMenu.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        int rowAtPoint = table.rowAtPoint(SwingUtilities.convertPoint(popupMenu, new Point(0, 0), table));
                        if (rowAtPoint > -1) {
                            table.setRowSelectionInterval(rowAtPoint, rowAtPoint);
                        }
                    }
                });
            }
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // TODO Auto-generated method stub

            }
            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // TODO Auto-generated method stub
            }
        });
		JMenuItem mntmDelete = new JMenuItem("Delete");
		mntmDelete.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int i = table_expressions.getSelectedRow();
				expressions.remove(i);
				tableModel_patterns.fireTableDataChanged();
			}
		});
		popupMenu.add(mntmDelete);

		JMenuItem mntmAdd = new JMenuItem("Add");
		mntmAdd.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				int i = table_expressions.getSelectedRow();
				expressions.add(i+1, new ExpressionPattern("", "", Action.Ask));
				tableModel_patterns.fireTableDataChanged();
			}
		});
		popupMenu.add(mntmAdd);
		
		table_expressions.setComponentPopupMenu(popupMenu);
		
		TableColumn patternPolicyColumn = table_expressions.getColumn(tableModel_patterns.getColumnName(2));
		patternPolicyColumn.setCellEditor(new DefaultCellEditor(patternAction_comboBox));
		table_expressions.getColumn(tableModel_patterns.getColumnName(2)).setCellRenderer(
	            new DefaultTableCellRenderer() {
					private static final long serialVersionUID = 1L;

				@Override
	               public Component getTableCellRendererComponent(JTable table,
	                     Object value, boolean isSelected, boolean hasFocus,
	                     int row, int column) {
	                  JLabel superRenderer = (JLabel)super.getTableCellRendererComponent(table, 
	                        value, isSelected, hasFocus, row, column);
	                  if(backgroundColor == null) backgroundColor = getBackground();
	                  Action action = (Action)tableModel_patterns.getValueAt(row, column);
	                  if(!isSelected) {
		                  switch(action) {
		                  case Ask:
		                   	  superRenderer.setBackground(PreviewPanel.askColor);
		                   	  break;
		                  case Redact:
		                	  superRenderer.setBackground(PreviewPanel.redactColor);
		                	  break;
		                  case Ignore:
		                	  superRenderer.setBackground(backgroundColor);
		                	  break;
		                  }
	                  }
	                  return superRenderer;
	               }
	            });
		
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
		
		fileChooser_textpattern = new JFileChooser();
		fileChooser_textpattern.setDialogTitle("Import Text Patterns");
		fileChooser_textpattern.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fileChooser_textpattern.setAcceptAllFileFilterUsed(false);
		fileChooser_textpattern.setMultiSelectionEnabled(true);
		fileChooser_textpattern.setFileFilter(new FileFilter() {
			public boolean accept(File arg0) {
				if(arg0.isDirectory()) 
					return true;
				if (arg0.getName().endsWith(".TXT"))
					return true;
				if (arg0.getName().endsWith(".txt"))
					return true;
				return false;
			}
			public String getDescription() {
				return "Text files";
			}
		});
		
		this.redactDialog = new RedactDialog();
	}

	private void loadExpressions() {
		if(!new File(DEFAULT_PATTERNS_PATH).exists()) {
			new File(DEFAULT_PATTERNS_PATH).getParentFile().mkdirs();
			ExpressionPattern p = new ExpressionPattern("Social Security Number", "\\d{3}-\\d{2}-\\d{4}", Action.Redact);
			TextPatternUtil.saveExpressionPatterns(new File(DEFAULT_PATTERNS_PATH), Collections.singletonList(p));
		}
		// name, regular expression, policy, notes
		List<ExpressionPattern> list = TextPatternUtil.loadExpressionActionList(new File(DEFAULT_PATTERNS_PATH));
		expressions.addAll(list);
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
		AnalysisWorker pdfAnalysisWorker = null;
		if(useSpacy) {
			log.info("starting PDF analysis with spaCy");
			pdfAnalysisWorker = new SpaceyAnalysisWorker(pdfFiles.toArray(EMPTY_FILE_ARRAY)) {
				@Override
				protected void process(List<PDFAnalysis> chunks) {
					processAnalysisChunks(chunks);
				}
			};
		} else {
			log.info("starting PDF analysis with CoreNLP");
			pdfAnalysisWorker = new CoreNLPAnalysisWorker(pdfFiles.toArray(EMPTY_FILE_ARRAY),
					chckbxmntmDetectPersons.isSelected(),
					chckbxmntmDetectPlaces.isSelected(),
					chckbxmntmDetectOrganizations.isSelected()) {
				@Override
				protected void process(List<PDFAnalysis> chunks) {
					processAnalysisChunks(chunks);
				}
			};
		}
		pdfAnalysisWorker.execute();
	}

	private void processAnalysisChunks(List<PDFAnalysis> chunks) {
		for(PDFAnalysis a : chunks) {
			if(a.entities != null && a.entities.length > 0) {
				Arrays.stream(a.entities)
					.forEach( x -> {
						EntityPattern p = new EntityPattern(x[0], x[1], defaultAction);
						p.setExampleSentence(x[2]);
						if(!entities.containsKey(p.getLabel())) {
							entities.put(p.getLabel(), p);
						} else {
							EntityPattern e = entities.get(p.getLabel());
							if(x[2].length() < 200 && x[2].length() > e.getExampleSentence().length()) {
								e.setExampleSentence(x[2]);
							}
							e.incr();
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
	
	private void redact(File file) {
		// TODO Alert if redacted file already exists..
		List<TextPattern> filePatterns = new ArrayList<>();
		for(String key : pattern_files.keySet()) {
			if(pattern_files.get(key).contains(file)) {
				EntityPattern e = entities.get(key);
				if(!Action.Ignore.equals(e.policy)) filePatterns.add(e);
			}
		}
		File outFile = getOutputFile(file);
		filePatterns = filePatterns.stream().filter(p -> p != null).collect(Collectors.toList());
		Collections.sort(filePatterns, Comparator.comparing(x -> { return ((TextPattern)x).getLabel();}));
		filePatterns.addAll(this.expressions.stream()
				.filter(p -> !p.policy.equals(Action.Ignore)).collect(Collectors.toList()));
		try {
			this.redactDialog.startDoc(file, outFile, filePatterns);
			this.redactDialog.setVisible(true);
			this.redactDialog.tableModel.fireTableDataChanged();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	private File getOutputFile(File inputFile) {
		File outFile = Paths.get(outPath, inputFile.getAbsolutePath()).toFile();
		return outFile;
	}
}
