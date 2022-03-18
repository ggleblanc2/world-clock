package com.ggl.testing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class WorldClockGUI implements Runnable {

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new WorldClockGUI());
	}
	
	private DateTimeFormatter formatter;
	
	private DisplayPanel[] displayPanel;
	
	private JFrame frame;
	
	private JPanel mainPanel;
	
	private final WorldClockModel model;
	
	public WorldClockGUI() {
		this.model = new WorldClockModel();
		this.formatter = DateTimeFormatter.ofPattern("h:mm a");
		this.mainPanel = createMainPanel();
	}

	@Override
	public void run() {
		frame = new JFrame("World Clock");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(createMenuBar());
		
		frame.add(mainPanel, BorderLayout.CENTER);
		
		frame.pack();
		frame.setLocationByPlatform(true);
		frame.setVisible(true);
		
		Timer timer = new Timer(20_000, event -> {
			model.setCurrentTime();
			updateMainPanel();
		});
		timer.start();
	}
	
	private JMenuBar createMenuBar() {
		JMenuBar menuBar = new JMenuBar();
		
		JMenu selectMenu = new JMenu("Select");
		menuBar.add(selectMenu);
		
		JMenuItem timezoneItem =  new JMenuItem("Select time zones...");
		timezoneItem.addActionListener(event -> new TimezoneDialog(this, model));
		selectMenu.add(timezoneItem);
		
		return menuBar;
	}
	
	public void recreateMainPanel() {
		frame.setVisible(false);
		frame.remove(mainPanel);
		mainPanel = createMainPanel();
		frame.add(mainPanel, BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}
	
	private JPanel createMainPanel() {
		JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		List<ZoneId> timezones = model.getTimezones();
		int length = timezones.size();
		displayPanel = new DisplayPanel[length];
		for (int index = 0; index < length; index++) {
			displayPanel[index] = new DisplayPanel();
			ZoneId zone = timezones.get(index);
			String name = zone.getId();
			ZonedDateTime time = model.getCurrentTime(zone);
			displayPanel[index].setDisplayField(time.format(formatter));
			displayPanel[index].setTimezoneLabel(name);
			panel.add(displayPanel[index].getPanel());
		}
		
		return panel;
	}
	
	public void updateMainPanel() {
		List<ZoneId> timezones = model.getTimezones();
		for (int index = 0; index < timezones.size(); index++) {
			ZoneId zone = timezones.get(index);
			ZonedDateTime time = model.getCurrentTime(zone);
			displayPanel[index].setDisplayField(time.format(formatter));
		}
	}

	
	public JFrame getFrame() {
		return frame;
	}

	public class DisplayPanel {
		
		private final JPanel panel;
		
		private JLabel timezoneLabel;
		
		private JTextField displayField;
		
		public DisplayPanel() {
			this.panel = createDisplayPanel();
		}
		
		private JPanel createDisplayPanel() {
			JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 40, 5));
			panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			Font displayFont = panel.getFont().deriveFont(Font.BOLD, 36f);
			Font timezoneFont = panel.getFont().deriveFont(Font.BOLD, 24f);
			
			displayField = new JTextField(6);
			displayField.setEditable(false);
			displayField.setFont(displayFont);
			displayField.setHorizontalAlignment(JTextField.TRAILING);
			panel.add(displayField);
			
			timezoneLabel = new JLabel();
			timezoneLabel.setFont(timezoneFont);
			panel.add(timezoneLabel);
			
			return panel;
		}
		
		public JPanel getPanel() {
			return panel;
		}

		public void setTimezoneLabel(String timezone) {
			this.timezoneLabel.setText(timezone);
		}

		public void setDisplayField(String display) {
			this.displayField.setText(display);
		}
		
	}
	
	public class TimezoneDialog extends JDialog {

		private static final long serialVersionUID = 1L;
		
		private DefaultListModel<String> allTimezonesListModel;
		private DefaultListModel<ZoneId> displayTimezonesListModel;
		
		private JList<String> allTimezonesList;
		private JList<ZoneId> displayTimezonesList;
		
		private final WorldClockGUI view;
		
		private final WorldClockModel model;

		public TimezoneDialog(WorldClockGUI view, WorldClockModel model) {
			super(view.getFrame(), "Select time zones to display");
			this.view = view;
			this.model = model;
			
			add(createMainPanel(), BorderLayout.CENTER);
			add(createButtonPanel(), BorderLayout.SOUTH);
			
			pack();
			setLocationRelativeTo(view.getFrame());
			setVisible(true);
		}
		
		private JPanel createMainPanel() {
			JPanel panel = new JPanel(new FlowLayout());
			panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			Font font = panel.getFont().deriveFont(16f);
			
			allTimezonesListModel = createAllTimezonesModel();
			allTimezonesList = new JList<String>(allTimezonesListModel);
			allTimezonesList.setFont(font);
			JScrollPane scrollPane = new JScrollPane(allTimezonesList);
			panel.add(scrollPane);
			
			JButton allDeleteButton = new JButton("<<");
			allDeleteButton.setFont(font);
			allDeleteButton.addActionListener(event -> {
				List<ZoneId> values = getDisplayTimezones();
				for (ZoneId zone : values) {
					displayTimezonesListModel.removeElement(zone);
					allTimezonesListModel.addElement(zone.getId());
				}
				sortAllTimezonesListModel();
			});
			panel.add(allDeleteButton);
			
			JButton selectDeleteButton = new JButton("<");
			selectDeleteButton.setFont(font);
			selectDeleteButton.addActionListener(event -> {
				List<ZoneId> values = displayTimezonesList.getSelectedValuesList();
				for (ZoneId zone : values) {
					displayTimezonesListModel.removeElement(zone);
					allTimezonesListModel.addElement(zone.getId());
				}
				sortAllTimezonesListModel();
			});
			panel.add(selectDeleteButton);
			
			JButton selectAddButton = new JButton(">");
			selectAddButton.setFont(font);
			selectAddButton.addActionListener(event -> {
				List<String> values = allTimezonesList.getSelectedValuesList();
				for (String s : values) {
					allTimezonesListModel.removeElement(s);
					displayTimezonesListModel.addElement(ZoneId.of(s));
				}
			});
			panel.add(selectAddButton);
			
			displayTimezonesListModel = createDisplayTimezonesModel();
			displayTimezonesList = new JList<ZoneId>(displayTimezonesListModel);
			displayTimezonesList.setFont(font);
			JScrollPane scrollPane2 = new JScrollPane(displayTimezonesList);
			scrollPane2.setPreferredSize(scrollPane.getPreferredSize());
			scrollPane2.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
			panel.add(scrollPane2);
			
			return panel;
		}
		
		public List<ZoneId> getDisplayTimezones() {
			List<ZoneId> timezoneList = new ArrayList<>();
			
			for (int index = 0; index < displayTimezonesListModel.size(); index++) {
				timezoneList.add(displayTimezonesListModel.getElementAt(index));
			}
			
			return timezoneList;
		}
		
		public void sortAllTimezonesListModel() {
			List<String> allTimezones = new ArrayList<>();
			for (int index = 0; index < allTimezonesListModel.size(); index++) {
				allTimezones.add(allTimezonesListModel.getElementAt(index));
			}
			
			Collections.sort(allTimezones);
			allTimezonesListModel.removeAllElements();
			
			for (String s : allTimezones) {
				allTimezonesListModel.addElement(s);
			}
		}
		
		private JPanel createButtonPanel() {
			JPanel panel = new JPanel(new FlowLayout());
			panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			Font font = panel.getFont().deriveFont(16f);
			
			JButton okButton = new JButton("OK");
			okButton.addActionListener(new TimezoneDialogListener(view, model, this));
			okButton.setFont(font);
			panel.add(okButton);
			
			JButton cancelButton = new JButton("Cancel");
			cancelButton.addActionListener(event -> dispose());
			cancelButton.setFont(font);
			panel.add(cancelButton);
			
			okButton.setPreferredSize(cancelButton.getPreferredSize());
			
			return panel;
		}
		
		private DefaultListModel<String> createAllTimezonesModel() {
			DefaultListModel<String> listModel = new DefaultListModel<>();
			List<ZoneId> timezones = model.getTimezones();
			List<String> allTimezones = model.getAllTimezones();
			
			for (String s : allTimezones) {
				listModel.addElement(s);
			}
			
			for (ZoneId zone : timezones) {
				listModel.removeElement(zone.getId());
			}
			
			return listModel;
		}
		
		private DefaultListModel<ZoneId> createDisplayTimezonesModel() {
			DefaultListModel<ZoneId> listModel = new DefaultListModel<>();
			List<ZoneId> timeZones = model.getTimezones();
			
			for (ZoneId zone : timeZones) {
				listModel.addElement(zone);
			}
			
			return listModel;
		}

		public DefaultListModel<ZoneId> getDisplayTimezonesListModel() {
			return displayTimezonesListModel;
		}
		
	}
	
	public class TimezoneDialogListener implements ActionListener {
		
		private final TimezoneDialog dialog;
		
		private final WorldClockGUI view;
		
		private final WorldClockModel model;

		public TimezoneDialogListener(WorldClockGUI view, WorldClockModel model, 
				TimezoneDialog dialog) {
			this.view = view;
			this.model = model;
			this.dialog = dialog;
		}

		@Override
		public void actionPerformed(ActionEvent event) {
			DefaultListModel<ZoneId> listModel = dialog.getDisplayTimezonesListModel();
			if (listModel.size() < 1) {
				JOptionPane.showMessageDialog(view.getFrame(), 
						"You must select one or more time zones.", 
						"Display Time Zone Error", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			List<ZoneId> timezones = new ArrayList<>();
			for (int index = 0; index < listModel.size(); index++) {
				timezones.add(listModel.getElementAt(index));
			}
			model.setTimezones(timezones);
			view.recreateMainPanel();
			dialog.dispose();
		}
		
	}
	
	public class WorldClockModel {
		
		private final List<String> allTimezones;
		private List<ZoneId> timezones;
				
		private ZonedDateTime currentTime;
		
		public WorldClockModel() {
			this.currentTime = ZonedDateTime.now();
			this.allTimezones = getAvailableTimezones();
			this.timezones = createDisplayTimezones();
		}

		private List<String> getAvailableTimezones() {
			List<String> allTimezones = new ArrayList<>();
			
			for (String id : ZoneId.getAvailableZoneIds()) {
				if (id.contains("/") && !id.startsWith("Etc") 
						&& !id.startsWith("System")) {
					allTimezones.add(id);
				}
			}
			
			Collections.sort(allTimezones);
			
			return allTimezones;
		}
		
		private List<ZoneId> createDisplayTimezones() {
			List<ZoneId> timezones = new ArrayList<>();
			
			timezones.add(ZoneId.of("US/Pacific"));
			timezones.add(ZoneId.of("US/Mountain"));
			timezones.add(ZoneId.of("US/Central"));
			timezones.add(ZoneId.of("US/Eastern"));
			
			return timezones;
		}
		
		public void setTimezones(List<ZoneId> timezones) {
			this.timezones = timezones;
		}

		public List<ZoneId> getTimezones() {
			return timezones;
		}

		public List<String> getAllTimezones() {
			return allTimezones;
		}

		public void setCurrentTime() {
			this.currentTime = ZonedDateTime.now();
		}
		
		public ZonedDateTime getCurrentTime(ZoneId zone) {
			return currentTime.withZoneSameInstant(zone);
		}
		
	}

}
