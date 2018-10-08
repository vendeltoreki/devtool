package com.liferay.devtool.window;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;
import javax.swing.text.DefaultCaret;
import javax.swing.text.html.HTMLEditorKit;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.BundleEventListener;
import com.liferay.devtool.bundles.BundleManager;
import com.liferay.devtool.bundles.BundleStatus;
import com.liferay.devtool.utils.HtmlDescriptionRenderer;
import com.liferay.devtool.utils.StringUtils;

public class BundlesPanel extends JPanel implements MouseWheelListener, BundleEventListener {
	private static final long serialVersionUID = -3140427339966486122L;
	private BundleManager bundleManager = null;
	private List<BundlePanel> bundlePanelList = new ArrayList<>();
	private Map<String,BundlePanel> bundlePanelMap = new HashMap<>();
	private JPanel bundleLister;
	private int fontSize = 12;
	private Font labelFont = new Font("Dialog", Font.BOLD, fontSize);
	private Font textFont = new Font("Dialog", Font.PLAIN, fontSize);
	private JLabel statusLabel;

	public void setBundleManager(BundleManager bundleManager) {
		this.bundleManager = bundleManager;
	}

	public void init() {
		this.setLayout(new BorderLayout());

		JButton scanButton = new JButton("Scan");
		JButton refreshButton = new JButton("Refresh");

		JPanel topPanel = new JPanel();
		topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));
		
		topPanel.add(scanButton);
		topPanel.add(refreshButton);
		this.add(topPanel, BorderLayout.NORTH);

		bundleLister = new JPanel();
		BoxLayout layout = new BoxLayout(bundleLister, BoxLayout.Y_AXIS);
		bundleLister.setLayout(layout);

		bundleManager.setBundleEventListener(this);

		scanButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingWorker<List<BundleEntry>, Void> worker = createWorker();
				worker.execute();
			}
		});

		refreshButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				SwingWorker<List<BundleEntry>, Void> worker = createRefreshWorker();
				worker.execute();
			}
		});

		JScrollPane listScrollPane = new JScrollPane(bundleLister);
		listScrollPane.getVerticalScrollBar().setUnitIncrement(5);
		listScrollPane.getVerticalScrollBar().getModel().getValue();

		this.add(listScrollPane, BorderLayout.CENTER);
		
		listScrollPane.addMouseWheelListener(this);
	}

	private SwingWorker<List<BundleEntry>, Void> createWorker() {
		return new SwingWorker<List<BundleEntry>, Void>() {
			@Override
			public List<BundleEntry> doInBackground() {
				updateStatus("Scanning bundles...");
				bundleManager.scanFileSystem();
				bundleManager.readDetails();

				return bundleManager.getEntries();
			}

			@Override
			public void done() {
				updateStatus("Bundle scanning done.");
			}
		};
	}

	private SwingWorker<List<BundleEntry>, Void> createRefreshWorker() {
		return new SwingWorker<List<BundleEntry>, Void>() {
			@Override
			public List<BundleEntry> doInBackground() {
				updateStatus("Reading bundles...");
				bundleManager.readDetails();

				return bundleManager.getEntries();
			}

			@Override
			public void done() {
				updateStatus("Reading bundles done.");
			}
		};
	}
	
	private BundlePanel createBundlePanel(BundleEntry entry) {
		BundlePanel res = new BundlePanel(entry);
		res.init();
		return res;
	}

	class BundlePanel extends JPanel {
		public static final String MENU_STOP_BUNDLE = "Stop Bundle";
		public static final String MENU_START_BUNDLE = "Start Bundle";
		public static final String MENU_CLEAN_DB = "Clean DB";
		public static final String MENU_CLEAN_TEMP_DIRS = "Clean temp dirs";
		private static final long serialVersionUID = -4063016827691757109L;
		private BundleEntry entry;
		private JLabel label;
		// private JTextArea textArea;
		//private JTextPane textArea;
		private JEditorPane textArea;
		private JPopupMenu popup;

		public BundlePanel(BundleEntry entry) {
			super();
			this.entry = entry;
		}

		public void init() {
			this.setBorder(BorderFactory.createLineBorder(Color.black));
			BorderLayout layout = new BorderLayout();
			this.setLayout(layout);

			label = new JLabel(createLabelText());
			label.setHorizontalAlignment(JLabel.LEFT);
			this.add(label, BorderLayout.CENTER);
			
			createPopupMenu();
			MouseListener popupListener = new PopupListener(popup, entry);

			label.addMouseListener(popupListener);

			refreshEntry();
		}
		
		private String createLabelText() {
			if (entry.getName() != null) {
				return StringUtils.truncateString(
						entry.getName() + 
						(entry.getPortalVersion() != null ? (" - "+entry.getPortalVersion()) : "") +
						(entry.getPortalPatches() != null ? (" - "+entry.getPortalPatches()) : ""),
						100);
			} else {
				return entry.getRootDirPath();
			}
		}
		
		public BundleEntry getEntry() {
			return entry;
		}

		public void setEntry(BundleEntry entry) {
			this.entry = entry;
		}

		public void refreshEntry() {
			if (label != null) {
				label.setFont(labelFont);
				if (entry != null && entry.getBundleStatus() == BundleStatus.RUNNING) {
					label.setText(entry.getBundleStatus().name() + " - " + createLabelText());
					label.setOpaque(true);
					label.setBackground(Color.green);
				} else if (entry != null && entry.getBundleStatus() == BundleStatus.STARTING) {
					label.setText(entry.getBundleStatus().name() + " - " + createLabelText());
					label.setOpaque(true);
					label.setBackground(Color.yellow);
				} else if (entry != null && entry.getBundleStatus() == BundleStatus.STOPPING) {
					label.setText(entry.getBundleStatus().name() + " - " + createLabelText());
					label.setOpaque(true);
					label.setBackground(Color.yellow);
				} else {
					label.setText(createLabelText());
					label.setOpaque(false);
					label.setBackground(Color.lightGray);
				}
			}

			if (entry != null) {
				HtmlDescriptionRenderer desc = new HtmlDescriptionRenderer();
				desc.setEntry(entry);
				
				if (textArea == null) {
			        textArea = new JEditorPane(new HTMLEditorKit().getContentType(), desc.createDescription());
			        
			        DefaultCaret caret = (DefaultCaret) textArea.getCaret();
			        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
			        
			        textArea.setOpaque(true);
			        textArea.setBorder(null);
			        textArea.setEditable(false);
			        
			        textArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			        textArea.setFont(textFont);			        
			        
					this.add(textArea, BorderLayout.SOUTH);
				} else {
					
					textArea.setText(desc.createDescription());
					textArea.setFont(textFont);
				}
			}
		}

		private void createPopupMenu() {
			JMenuItem menuItem;

			popup = new JPopupMenu();
			
			menuItem = new JMenuItem("Actions");
			menuItem.setEnabled(false);
			popup.add(menuItem);
			popup.addSeparator();
			
			addPopupMenuItem(MENU_CLEAN_TEMP_DIRS, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					bundleManager.cleanTempDirs(entry);
				}
			});
			
			addPopupMenuItem(MENU_CLEAN_DB, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					bundleManager.cleanDb(entry);
				}
			});
			
			addPopupMenuItem(MENU_START_BUNDLE, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					bundleManager.startBundle(entry);
				}
			});

			addPopupMenuItem(MENU_STOP_BUNDLE, new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					bundleManager.stopBundle(entry);
				}
			});
			
		}

		private void addPopupMenuItem(String name, ActionListener actionListener) {
			JMenuItem menuItem = new JMenuItem(name);
			menuItem.addActionListener(actionListener);
			popup.add(menuItem);
		}
	}

	class PopupListener extends MouseAdapter {
		JPopupMenu popup;
		BundleEntry entry;

		PopupListener(JPopupMenu popupMenu, BundleEntry bundleEntry) {
			popup = popupMenu;
			entry = bundleEntry;
		}

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
				for (Component component : popup.getComponents()) {
					//System.out.println("menu item component: "+component);
					if (component instanceof JMenuItem) {
						JMenuItem menuItem = (JMenuItem)component;
						
						if (menuItem.getText().equals(BundlePanel.MENU_CLEAN_TEMP_DIRS)) {
							menuItem.setEnabled(!entry.isRunning());
						} else if (menuItem.getText().equals(BundlePanel.MENU_CLEAN_DB)) {
							menuItem.setEnabled(!entry.isRunning());
						} else if (menuItem.getText().equals(BundlePanel.MENU_START_BUNDLE)) {
							menuItem.setEnabled(bundleManager.isBundleStartable(entry));							
						} else if (menuItem.getText().equals(BundlePanel.MENU_STOP_BUNDLE)) {
							menuItem.setEnabled(bundleManager.isBundleStoppable(entry));							
						}
					}
				}
				popup.show(e.getComponent(), e.getX(), e.getY());
			}
		}
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		boolean controlDown = (e.getModifiers() & InputEvent.CTRL_MASK) != 0;
		
		if (controlDown) {
			fontSize -= e.getWheelRotation();
			if (fontSize < 12) {
				fontSize = 12;
			}
			
			if (fontSize > 100) {
				fontSize = 100;
			}
			
			updateFontSize();
		}
	}

	private void updateFontSize() {
		labelFont = new Font(labelFont.getName(), labelFont.getStyle(), fontSize);
		textFont = new Font(textFont.getName(), textFont.getStyle(), fontSize);
		
		for (BundlePanel p : bundlePanelList) {
			p.refreshEntry();
		}
		bundleLister.revalidate();
	}

	@Override
	public void onUpdate(BundleEntry entry) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				//System.out.println("Entry event: "+entry);
				
				refreshEntry(entry);
				/*bundleLister.revalidate();
				for (BundlePanel p : bundlePanelList) {
					p.repaint();
				}
				bundleLister.revalidate();*/
			}

		});		
	}	

	private void refreshEntry(BundleEntry entry) {
		if (!bundlePanelMap.containsKey(entry.getRootDirPath())) {
			BundlePanel bundlePanel = createBundlePanel(entry);
			bundlePanelMap.put(entry.getRootDirPath(), bundlePanel);
			bundlePanelList.add(bundlePanel);
			bundleLister.add(bundlePanel);
			
			bundlePanel.refreshEntry();
		} else {
			BundlePanel bundlePanel = bundlePanelMap.get(entry.getRootDirPath());
			bundlePanel.refreshEntry();
		}
	}
	
	private void updateStatus(String statusMessage) {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (statusLabel != null) {
					statusLabel.setText(statusMessage);
				}
			}

		});			
	}

	public void setStatusLabel(JLabel statusLabel) {
		this.statusLabel = statusLabel;
	}

}
