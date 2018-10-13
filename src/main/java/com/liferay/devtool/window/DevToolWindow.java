package com.liferay.devtool.window;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import com.liferay.devtool.bundles.BundleManager;
import com.liferay.devtool.context.DevToolContext;
import com.liferay.devtool.devenv.DevEnvChecker;

public class DevToolWindow {
	private DevToolContext context;
	private DevEnvChecker devEnvChecker = new DevEnvChecker();
	private BundleManager bundleManager = new BundleManager();
	private JLabel statusLabel;

	public void createAndShowGUI() {
		devEnvChecker.setSysEnv(context.getSysEnv());
		devEnvChecker.addChecks();
		
		bundleManager.setContext(context);

		JFrame frame = new JFrame("Developer Tool");
		frame.setSize(600, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.addWindowListener(new java.awt.event.WindowAdapter() {
		    @Override
		    public void windowClosing(java.awt.event.WindowEvent windowEvent) {
		    	if (bundleManager != null) {
		    		bundleManager.stop();
		    	}
		    }
		});		
		frame.setIconImage(Icons.IMG_APP);

		statusLabel = new JLabel("Ready");
		// emptyLabel.setPreferredSize(new Dimension(175, 100));
		frame.getContentPane().add(statusLabel, BorderLayout.NORTH);

		JTabbedPane tabbedPane = new JTabbedPane();
		JComponent envPanel = createEnvPanel();
		JComponent bundlesPanel = createBundlesPanel();
		JComponent logsPanel = createLogsPanel();

		tabbedPane.add("Environment", envPanel);
		tabbedPane.add("Bundles", bundlesPanel);
		tabbedPane.add("Logs", logsPanel);
		tabbedPane.setDoubleBuffered(true);

		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);

		frame.pack();
		frame.setVisible(true);
	}

	protected JComponent createEnvPanel() {
		EnvChecksPanel panel = new EnvChecksPanel();
		panel.setDevEnvChecker(devEnvChecker);
		panel.init();
		return panel;
	}

	protected JComponent createBundlesPanel() {
		BundlesPanel panel = new BundlesPanel();
		panel.setBundleManager(bundleManager);
		panel.init();
		panel.setStatusLabel(statusLabel);
		return panel;
	}

	protected JComponent createLogsPanel() {
		LogsPanel panel = new LogsPanel();
		panel.init();
		
		context.getLogger().setLogEventListener(panel);
		
		return panel;
	}
	
	public void runWindowApp() {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	public void setContext(DevToolContext context) {
		this.context = context;
	}
}
