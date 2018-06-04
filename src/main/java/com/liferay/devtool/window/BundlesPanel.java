package com.liferay.devtool.window;

import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.text.html.HTMLEditorKit;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.BundleEventListener;
import com.liferay.devtool.bundles.BundleManager2;
import com.liferay.devtool.bundles.GitRepoEntry;
import com.liferay.devtool.bundles.TempDirEntry;

public class BundlesPanel extends JPanel implements MouseWheelListener, BundleEventListener {
	private static final long serialVersionUID = -3140427339966486122L;
	private BundleManager2 bundleManager = null;
	private List<BundlePanel> bundlePanelList = new ArrayList<>();
	private Map<String,BundlePanel> bundlePanelMap = new HashMap<>();
	private JPanel bundleLister;
	private int fontSize = 12;
	private Font labelFont = new Font("Dialog", Font.BOLD, fontSize);
	private Font textFont = new Font("Dialog", Font.PLAIN, fontSize);

	public BundleManager2 getBundleDetector() {
		return bundleManager;
	}

	public void setBundleManager(BundleManager2 bundleManager) {
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

		this.add(listScrollPane, BorderLayout.CENTER);
		
		listScrollPane.addMouseWheelListener(this);
	}

	private SwingWorker<List<BundleEntry>, Void> createWorker() {
		return new SwingWorker<List<BundleEntry>, Void>() {
			@Override
			public List<BundleEntry> doInBackground() {
				bundleManager.scanFileSystem();
				bundleManager.readDetails();

				return bundleManager.getEntries();
			}

			@Override
			public void done() {
			}
		};
	}

	private SwingWorker<List<BundleEntry>, Void> createRefreshWorker() {
		return new SwingWorker<List<BundleEntry>, Void>() {
			@Override
			public List<BundleEntry> doInBackground() {
				bundleManager.readDetails();

				return bundleManager.getEntries();
			}

			@Override
			public void done() {
			}
		};
	}
	
	private BundlePanel createBundlePanel(BundleEntry entry) {
		BundlePanel res = new BundlePanel(entry);
		res.init();
		return res;
	}

	class BundlePanel extends JPanel {
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
			MouseListener popupListener = new PopupListener(popup);

			label.addMouseListener(popupListener);

			refreshEntry();
		}
		
		private String createLabelText() {
			return entry.getName() + 
					(entry.getPortalVersion() != null ? (" - "+entry.getPortalVersion()) : "") +
					(entry.getPortalPatches() != null ? (" - "+entry.getPortalPatches()) : "");
		}

		public BundleEntry getEntry() {
			return entry;
		}

		public void setEntry(BundleEntry entry) {
			this.entry = entry;
		}

		public void refreshEntry() {
			if (label != null) {
				label.setText(createLabelText());
				label.setFont(labelFont);
			}

			if (entry != null) {
				if (textArea == null) {
					// textArea = new JTextArea(createDescription());
					// textArea.setEditable(false);

					/*textArea = new JTextPane();
					textArea.setContentType("text/html");
					textArea.setText(createDescription());
					textArea.setEditable(false);
					textArea.setBackground(null);
					textArea.setBorder(null);
					//textArea.setHorizontalAlignment(JLabel.LEFT);
					textArea.setBackground(Color.WHITE);
					textArea.setOpaque(true);
					textArea.setFont(textFont);*/

					
			        textArea = new JEditorPane(new HTMLEditorKit().getContentType(),createDescription());
			        textArea.setText(createDescription());
			        
			        /*Font font = UIManager.getFont("Label.font");
			        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
			                "font-size: " + fontSize + "pt; }";
			        ((HTMLDocument)textArea.getDocument()).getStyleSheet().addRule(bodyRule);*/
			        
			        textArea.setOpaque(true);
			        textArea.setBorder(null);
			        textArea.setEditable(false);
			        
			        textArea.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
			        textArea.setFont(textFont);			        
			        
					this.add(textArea, BorderLayout.SOUTH);
				} else {
					textArea.setText(createDescription());
					textArea.setFont(textFont);
				}
			}
		}

		private String createDescription() {
			StringBuffer sb = new StringBuffer();
			sb.append("<html>");
			sb.append("root dir: " + entry.getRootDir() + "<br>\n");
			sb.append("memory: xmx=" + formatLimitInt(entry.getMemoryXmx(), 4000) + ", perm="
					+ formatLimitInt(entry.getMemoryPermSize(), 512) + "<br>\n");
			sb.append("tomcat version: " + entry.getTomcatVersion() + "<br>\n");
			sb.append("DB driver: " + formatNotNull(entry.getDbDriverClass()) + "<br>\n");
			sb.append("DB URL: " + formatNotNull(entry.getDbUrl()) + "<br>\n");
			sb.append("DB user: " + formatNotNull(entry.getDbUsername()) + ", password="
					+ formatNotNull(entry.getDbPassword()) + "<br>\n");

			if (entry.getGitRepos() != null && !entry.getGitRepos().isEmpty()) {
				sb.append("<br>Git Repos:<br>");
				for (GitRepoEntry repo : entry.getGitRepos()) {
					sb.append(repo.toString()+"<br>\n");
				}
			}
			
			sb.append("<br>Temp dirs:<br>");
			if (entry.getTempDirs() != null && !entry.getTempDirs().isEmpty()) {
				for (TempDirEntry tempDir : entry.getTempDirs()) {
					sb.append(tempDir.getRelativePath()+" -- "+formatMaxLimitLong(tempDir.getTotalSize(), 0) +"<br>\n");
				}
			} else {
				sb.append("none<br>");
			}
			
			return sb.toString();
		}

		private String formatNotNull(String value) {
			if (value != null) {
				return "" + value;
			} else {
				return "<font color=red>" + value + "</font>";
			}
		}

		private String formatLimitInt(int value, int minLimit) {
			if (value >= minLimit) {
				return "" + value;
			} else {
				return "<font color=red>" + value + "</font>";
			}
		}
		
		private String formatMaxLimitLong(long value, long maxLimit) {
			if (value <= maxLimit) {
				return "" + value;
			} else {
				return "<font color=red>" + value + "</font>";
			}
		}

		private void createPopupMenu() {
			JMenuItem menuItem;

			popup = new JPopupMenu();
			
			menuItem = new JMenuItem("Actions");
			menuItem.setEnabled(false);
			popup.add(menuItem);
			popup.addSeparator();
			
			addPopupMenuItem("Clean temp dirs", new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Clean temp dirs is called on: "+entry.getName());
				}
			});
			
			addPopupMenuItem("Clean DB", new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					System.out.println("Clean DB is called on: "+entry.getName());
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

		PopupListener(JPopupMenu popupMenu) {
			popup = popupMenu;
		}

		public void mousePressed(MouseEvent e) {
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e) {
			maybeShowPopup(e);
		}

		private void maybeShowPopup(MouseEvent e) {
			if (e.isPopupTrigger()) {
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
				System.out.println("Entry event: "+entry);
				
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

}
