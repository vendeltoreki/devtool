package com.liferay.devtool.window;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;

import com.liferay.devtool.bundles.BundleManager;
import com.liferay.devtool.devenv.DevEnvChecker;
import com.liferay.devtool.utils.SysEnv;

public class DevToolWindow {
	private SysEnv sysEnv;
	private DevEnvChecker devEnvChecker = new DevEnvChecker();
	private BundleManager bundleManager = new BundleManager();

	public void createAndShowGUI() {
		devEnvChecker.setSysEnv(sysEnv);
		devEnvChecker.addChecks();
		
		bundleManager.setSysEnv(sysEnv);

		JFrame frame = new JFrame("Developer Tool");
		frame.setSize(600, 400);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setIconImage(Icons.IMG_APP);

		JLabel emptyLabel = new JLabel("Test");
		// emptyLabel.setPreferredSize(new Dimension(175, 100));
		frame.getContentPane().add(emptyLabel, BorderLayout.NORTH);

		JTabbedPane tabbedPane = new JTabbedPane();
		JComponent envPanel = createEnvPanel();
		JComponent bundlesPanel = createBundlesPanel();

		tabbedPane.add("Environment", envPanel);
		tabbedPane.add("Bundles", bundlesPanel);
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
		return panel;
	}

	public void runWindowApp() {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				createAndShowGUI();
			}
		});
	}

	public void setSysEnv(SysEnv sysEnv) {
		this.sysEnv = sysEnv;
	}

}
