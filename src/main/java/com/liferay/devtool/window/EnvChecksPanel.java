package com.liferay.devtool.window;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.border.Border;

import com.liferay.devtool.devenv.CheckStatus;
import com.liferay.devtool.devenv.DevEnvChecker;
import com.liferay.devtool.devenv.DevEnvEventListener;
import com.liferay.devtool.devenv.checks.BaseDevEnvCheckEntry;

public class EnvChecksPanel extends JPanel implements DevEnvEventListener {
	private static final long serialVersionUID = 3611565541554629442L;
	private DevEnvChecker devEnvChecker;
	private List<CheckPanel> checkPanelList = new ArrayList<>();
	private JPanel envLister = new JPanel();

	public DevEnvChecker getDevEnvChecker() {
		return devEnvChecker;
	}

	public void setDevEnvChecker(DevEnvChecker devEnvChecker) {
		this.devEnvChecker = devEnvChecker;
	}

	public void init() {
		this.setLayout(new BorderLayout());

		JButton refreshButton = new JButton("Refresh");
		this.add(refreshButton, BorderLayout.NORTH);

		envLister = new JPanel();
		BoxLayout layout = new BoxLayout(envLister, BoxLayout.Y_AXIS);
		envLister.setLayout(layout);

		devEnvChecker.setListener(this);

		for (BaseDevEnvCheckEntry entry : devEnvChecker.getChecks()) {
			CheckPanel p = createCheckPanel(entry);
			checkPanelList.add(p);
			envLister.add(p);
		}

		SwingWorker<List<BaseDevEnvCheckEntry>, Void> worker = new SwingWorker<List<BaseDevEnvCheckEntry>, Void>() {
			@Override
			public List<BaseDevEnvCheckEntry> doInBackground() {
				System.out.println("T:" + Thread.currentThread().getName() + " -- do in background");
				devEnvChecker.runChecks();
				return devEnvChecker.getChecks();
			}

			@Override
			public void done() {
				System.out.println("T:" + Thread.currentThread().getName() + " -- DONE");

				try {
					List<BaseDevEnvCheckEntry> result = get();
					for (CheckPanel p : checkPanelList) {
						p.refreshEntry();
					}
					envLister.revalidate();
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
				System.out.println("Action called: " + e);
				worker.execute();
				/*
				 * devEnvChecker.runChecks();
				 * 
				 * for (CheckPanel p : checkPanelList) { p.refreshEntry(); }
				 * lister.revalidate();
				 */
			}
		});

		JScrollPane listScrollPane = new JScrollPane(envLister);
		listScrollPane.getVerticalScrollBar().setUnitIncrement(5);

		this.add(listScrollPane, BorderLayout.CENTER);
	}

	private CheckPanel createCheckPanel(BaseDevEnvCheckEntry entry) {
		CheckPanel res = new CheckPanel(entry);
		res.init();
		return res;
	}

	@Override
	public void onUpdate(BaseDevEnvCheckEntry entry) {
		System.out.println("T:" + Thread.currentThread().getName() + " -- event called " + entry);

		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				System.out.println("T:" + Thread.currentThread().getName() + " -- run in queue");
				for (CheckPanel p : checkPanelList) {
					p.refreshEntry();
				}
				envLister.revalidate();
				for (CheckPanel p : checkPanelList) {
					p.repaint();
				}
				envLister.revalidate();
			}
		});
	}

	class CheckPanel extends JPanel {
		private static final long serialVersionUID = 5665610630101377537L;
		private BaseDevEnvCheckEntry entry;
		private JLabel iconLabel;
		private ImageIcon icon;
		private JLabel label;
		private JTextArea textArea;

		public CheckPanel(BaseDevEnvCheckEntry entry) {
			super();
			this.entry = entry;
		}

		public void init() {
			Border border = BorderFactory.createLineBorder(Color.black);
			Border margin = BorderFactory.createEmptyBorder(1, 1, 1, 1);
			this.setBorder(BorderFactory.createCompoundBorder(margin, border));

			// this.setBorder(BorderFactory.createLineBorder(Color.black));

			icon = new ImageIcon(Icons.IMG_PENDING);
			iconLabel = new JLabel(icon);
			iconLabel.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));

			// iconLabel.setForeground(Color.YELLOW);
			// iconLabel.setBackground(Color.GRAY);
			// iconLabel.setFont(new Font("Courier New", Font.BOLD, 12));
			iconLabel.setHorizontalAlignment(JLabel.LEFT);
			iconLabel.setVerticalAlignment(JLabel.TOP);
			// iconLabel.setOpaque(true);
			// iconLabel.setPreferredSize(new Dimension(50, 10));
			// iconLabel.setMaximumSize(new Dimension(50, 20));

			label = new JLabel(createLabelText());
			label.setHorizontalAlignment(JLabel.LEFT);
			label.setVerticalAlignment(JLabel.TOP);

			// label.setBorder(new EmptyBorder(10,10,10,10));

			refreshEntry();

			BorderLayout layout = new BorderLayout();
			// layout.addLayoutComponent(iconLabel, );
			// layout.setHgap(10);
			// layout.setVgap(10);
			this.setLayout(layout);
			this.add(iconLabel, BorderLayout.WEST);
			this.add(label, BorderLayout.CENTER);

			/*
			 * JButton button = new JButton("Button"); res.add(button);
			 */
		}

		private String createLabelText() {
			return entry.getTitle() + (entry.getMessage() != null ? ": " + entry.getMessage() : "");
		}

		public BaseDevEnvCheckEntry getEntry() {
			return entry;
		}

		public void setEntry(BaseDevEnvCheckEntry entry) {
			this.entry = entry;
		}

		public void refreshEntry() {
			if (iconLabel != null) {
				if (entry.getStatus() == CheckStatus.SUCCESS) {
					icon.setImage(Icons.IMG_OK);
				} else if (entry.getStatus() == CheckStatus.FAIL) {
					icon.setImage(Icons.IMG_ERROR);
				} else if (entry.getStatus() == CheckStatus.UNKNOWN) {
					icon.setImage(Icons.IMG_PENDING);
				}
				iconLabel.setToolTipText(entry.getStatus().name());
			}

			if (label != null) {
				label.setText(createLabelText());
			}

			if (entry.getDescription() != null) {
				if (textArea == null) {
					textArea = new JTextArea(entry.getDescription());
					textArea.setEditable(false);
					this.add(textArea, BorderLayout.SOUTH);
				} else {
					textArea.setText(entry.getDescription());
				}
			}
		}
	}

}
