package com.liferay.devtool.window;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import com.liferay.devtool.bundles.BundleDetector;
import com.liferay.devtool.bundles.BundleEntry;

public class BundlesPanel extends JPanel {
	private static final long serialVersionUID = -3140427339966486122L;
	private BundleDetector bundleDetector = new BundleDetector();
	private List<BundlePanel> bundlePanelList = new ArrayList<>();
	private JPanel bundleLister;

	public BundleDetector getBundleDetector() {
		return bundleDetector;
	}

	public void setBundleDetector(BundleDetector bundleDetector) {
		this.bundleDetector = bundleDetector;
	}

	public void init() {
		this.setLayout(new BorderLayout());

		JButton refreshButton = new JButton("Refresh");
		this.add(refreshButton, BorderLayout.NORTH);

		bundleLister = new JPanel();
		BoxLayout layout = new BoxLayout(bundleLister, BoxLayout.Y_AXIS);
		bundleLister.setLayout(layout);

		SwingWorker<List<BundleEntry>, Void> worker = new SwingWorker<List<BundleEntry>, Void>() {
			@Override
			public List<BundleEntry> doInBackground() {
				System.out.println("T:" + Thread.currentThread().getName() + " -- do in background");
				bundleDetector.scan();

				return bundleDetector.getEntries();
			}

			@Override
			public void done() {
				System.out.println("T:" + Thread.currentThread().getName() + " -- DONE");

				try {
					List<BundleEntry> result = get();

					bundlePanelList.clear();
					for (BundleEntry entry : result) {
						BundlePanel p = createBundlePanel(entry);
						bundlePanelList.add(p);
						bundleLister.add(p);
					}

					for (BundlePanel p : bundlePanelList) {
						p.refreshEntry();
					}
					bundleLister.revalidate();
				} catch (InterruptedException ignore) {
				} catch (java.util.concurrent.ExecutionException e) {
					String why = null;
					Throwable cause = e.getCause();
					if (cause != null) {
						why = cause.getMessage();
					} else {
						why = e.getMessage();
					}
					System.err.println("Error retrieving file: " + why);
				}
			}
		};

		refreshButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				bundleLister.removeAll();
				bundleLister.revalidate();
				worker.execute();
			}
		});

		JScrollPane listScrollPane = new JScrollPane(bundleLister);
		listScrollPane.getVerticalScrollBar().setUnitIncrement(5);

		this.add(listScrollPane, BorderLayout.CENTER);
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
		private JLabel textArea;

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

			refreshEntry();

			/*
			 * JButton button = new JButton("Button"); res.add(button);
			 */
		}

		private String createLabelText() {
			return entry.getName();
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
			}

			if (entry != null) {
				if (textArea == null) {
					// textArea = new JTextArea(createDescription());
					// textArea.setEditable(false);

					textArea = new JLabel(createDescription());
					textArea.setHorizontalAlignment(JLabel.LEFT);
					textArea.setBackground(Color.WHITE);
					textArea.setOpaque(true);

					this.add(textArea, BorderLayout.SOUTH);
				} else {
					textArea.setText(createDescription());
				}
			}
		}

		private String createDescription() {
			StringBuffer sb = new StringBuffer();
			sb.append("<html>");
			sb.append("root dir: " + entry.getRootDir().toString() + "<br>\n");
			sb.append("memory: xmx=" + formatLimitInt(entry.getMemoryXmx(), 4000) + ", perm="
					+ formatLimitInt(entry.getMemoryPermSize(), 512) + "<br>\n");
			sb.append("tomcat version: " + entry.getTomcatVersion() + "<br>\n");
			sb.append("DB driver: " + formatNotNull(entry.getDbDriverClass()) + "<br>\n");
			sb.append("DB URL: " + formatNotNull(entry.getDbUrl()) + "<br>\n");
			sb.append("DB user: " + formatNotNull(entry.getDbUsername()) + ", password="
					+ formatNotNull(entry.getDbPassword()) + "\n");
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
	}

}
