import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.JTextComponent;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JComponent;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Vector;

public class TabbedPane extends JPanel {
	private JTabbedPane tabbedPane;
	public static JPanel panelMain = new JPanel(new GridBagLayout());
	public static JPanel panelHist = new JPanel(new GridBagLayout());
	public static JComboBox filter;
	public static JComboBox value;
	public static JComboBox filterHist;
	public static JTextField valueHist;
	
	public static String[] histFilter = {"", "Make", "Model", "Item type", "Bought date", "Warranty", "Asset number",
			"BIOS", "Manufacturing year", "OS installation date", "OS", "Processor", "Hostname", "HDD", "RAM",
			"Owner", "Location", "Date from", "Date to"};

	public TabbedPane() {
		super(new GridLayout(1, 1));

		tabbedPane = new JTabbedPane();
		ImageIcon icon = new ImageIcon();// images/middle.gif");

		makeMainPanel();
		addFilter();
		tabbedPane.addTab("Main table", icon, panelMain, "Main tabula");
		tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);
		
		makeHistPanel();
		addFilterHist();
		tabbedPane.addTab("History table", icon, panelHist, "History tabula");
		tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);
		// Add the tabbed pane to this panel.
		add(tabbedPane);

		// The following line enables to use scrolling tabs.
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
	}

	public static void makeMainPanel() {

		GridBagConstraints c = new GridBagConstraints();
		final JTable filler = new Tables.TableMain();

		JScrollPane jScrollPane2 = new JScrollPane(filler, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollPane2.setViewportView(filler);

		c.fill = GridBagConstraints.BOTH;
		c.ipady = 1000;
		c.weightx = 5;
		c.weighty = 5;
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 0;
		c.gridy = 20;
		c.gridwidth = 6;
		c.gridheight = 8;
		panelMain.add(jScrollPane2, c);

		JButton btLabot = new JButton("Modify selected");
		c.gridx = 5;
		c.gridy = 0;
		c.weightx = 0.5;
		c.weighty = 0.5;
		c.gridheight = 14;
		c.gridwidth = 1;
		c.ipady = 30;
		c.fill = GridBagConstraints.NONE;
		panelMain.add(btLabot, c);
		btLabot.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				try {
					Noliktava.interOverTable();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
	public static void makeHistPanel() {

		GridBagConstraints c = new GridBagConstraints();
		final JTable filler = new Tables.TableHist();

		JScrollPane jScrollPane2 = new JScrollPane(filler, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		jScrollPane2.setViewportView(filler);

		c.fill = GridBagConstraints.BOTH;
		c.ipady = 1000;
		c.weightx = 5;
		c.weighty = 5;
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 0;
		c.gridy = 20;
		c.gridwidth = 6;
		c.gridheight = 8;
		panelHist.add(jScrollPane2, c);

		JButton btLabot = new JButton("Search");
		c.gridx = 5;
		c.gridy = 0;
		c.weightx = 0.5;
		c.weighty = 0.5;
		c.gridheight = 14;
		c.gridwidth = 1;
		c.ipady = 30;
		c.fill = GridBagConstraints.NONE;
		panelHist.add(btLabot, c);
		btLabot.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				Noliktava.filteringHist();
			}
		});
	}


	public static void addFilter() {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.weighty = 0.5;
		c.gridy = Noliktava.listOfFilters.size();
		c.gridheight = 1;
		c.gridwidth = 1;
		c.ipady = 30;
		c.gridx = 0;
		JLabel lbFilter = new JLabel("Filter: ");
		panelMain.add(lbFilter, c);
		c.gridx = 1;
		filter = new JComboBox(Tables.TableMain.mainHeader);
		panelMain.add(filter, c);
		Noliktava.listOfFilters.add(filter);
		filter.addActionListener(addFilterOnCahnge());

		c.gridx = 2;
		JLabel lbvalue = new JLabel("Value: ");
		panelMain.add(lbvalue, c);
		c.gridx = 3;
		value = new JComboBox();
		value.setEditable(true);
		panelMain.add(value, c);
		// value.addActionListener(filterMain());
		Noliktava.listOfValues.add(value);
	}
	
	public static void addFilterHist() {
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0.5;
		c.weighty = 0.5;
		c.gridy = Noliktava.listOfFiltersHist.size();
		c.gridheight = 1;
		c.gridwidth = 1;
		c.ipady = 30;
		c.gridx = 0;
		JLabel lbFilter = new JLabel("Filter: ");
		panelHist.add(lbFilter, c);
		c.gridx = 1;
		filterHist = new JComboBox(histFilter);		
		panelHist.add(filterHist, c);
		Noliktava.listOfFiltersHist.add(filterHist);
		filterHist.addActionListener(addFilterOnCahngeHist());

		c.gridx = 2;
		JLabel lbvalue = new JLabel("Value: ");
		panelHist.add(lbvalue, c);
		c.gridx = 3;
		valueHist = new JTextField(500);
		valueHist.setEditable(true);
		c.fill = GridBagConstraints.BOTH;
		panelHist.add(valueHist, c);
		// value.addActionListener(filterMain());
		Noliktava.listOfValuesHist.add(valueHist);
		panelHist.setVisible(false);
		panelHist.setVisible(true);
	}

	public static ActionListener addFilterOnCahnge() {
		ActionListener a = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				CheckFilters();
			}
		};
		return a;
	}
	
	public static ActionListener addFilterOnCahngeHist() {
		ActionListener a = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub
				CheckFiltersHist();
			}
		};
		return a;
	}

	public static ActionListener filterMain() {
		ActionListener a = new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				// TODO Auto-generated method stub				
				Noliktava.filtering();
			}
		};
		return a;
	}

	public static void CheckFilters() {
		for (int i = 0; i < Noliktava.listOfFilters.size(); i++) {
			if (Noliktava.listOfFilters.get(i).getSelectedIndex() != 0) {
				DefaultComboBoxModel model = new DefaultComboBoxModel(Noliktava.listOfMaps
						.get(Noliktava.listOfFilters.get(i).getSelectedIndex() - 1).keySet().toArray());
				model.insertElementAt("", 0);
				int tempVal = Noliktava.listOfValues.get(i).getSelectedIndex();
				Noliktava.listOfValues.get(i).setModel(model);
				Noliktava.listOfValues.get(i).setSelectedIndex(tempVal);
				Noliktava.listOfValues.get(i).addActionListener(filterMain());
				if (i == Noliktava.listOfFilters.size() - 1) {
					addListener(Noliktava.listOfValues.get(i));
					addFilter();
					Noliktava.listOfValues.get(i).setSelectedIndex(0);
				}
			}
		}
	}
	
	public static void CheckFiltersHist() {
		for (int i = 0; i < Noliktava.listOfFiltersHist.size(); i++) {
			if (Noliktava.listOfFiltersHist.get(i).getSelectedIndex() != 0) {
				if (i == Noliktava.listOfFiltersHist.size() - 1) {					
					addFilterHist();					
				}
			}
		}
	}

	public static void addListener(JComboBox box) {
		JTextComponent editor = (JTextComponent) box.getEditor().getEditorComponent();
		editor.addKeyListener(new KeyAdapter() {
			public void keyReleased(KeyEvent evt) {
				// your code
				System.out.println("Before: " + box.getSelectedItem().toString());
				box.setSelectedItem(box.getEditor().getItem());
				System.out.println("After: " + box.getSelectedItem().toString());
			}
		});
	}
}
