package com.liferay.devtool.bundlemonitor;

import java.io.File;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.BundleStatus;
import com.liferay.devtool.bundles.WebServerType;
import com.liferay.devtool.bundles.model.BundleModel;
import com.liferay.devtool.context.ContextBase;
import com.liferay.devtool.process.WindowsProcessTool;
import com.liferay.devtool.utils.LogReader;

public class BundleMonitor extends ContextBase {
	private BundleMonitorEventListener eventListener;
	private Timer timer;
	private BundleModel bundleModel;
	private volatile boolean started = false;
	
	public void start() {
		getContext().getLogger().log("Starting bundle monitor");
		if (!started) {
			timer = new Timer();
			timer.schedule(new TimerTask() {
				
				@Override
				public void run() {
					runMonitor();
				}
			}, 1000, 10000);
			started = true;
		}
	}
	
	private void runMonitor() {
		getContext().getLogger().log("Bundle monitor event");
		
		WindowsProcessTool wp = new WindowsProcessTool();
		wp.setSysEnv(getContext().getSysEnv());
		wp.refresh();

		bundleModel.updateWithProcessEntries(wp.getProcessEntries());
		
		List<BundleEntry> startedBundles = bundleModel.getBundlesByStatus(BundleStatus.PROCESS_STARTED);
		
		for (BundleEntry bundle : startedBundles) {
			readLogsForBundle(bundle);
		}
	}
	
	private void readLogsForBundle(BundleEntry bundle) {
		LogReader logReader = new LogReader();
		logReader.setContext(getContext());
		
		if (bundle.getRunningProcess() != null && bundle.getRunningProcess().getTimezone() != null) {
			logReader.setTimeZone(bundle.getRunningProcess().getTimezone());
		}
		
		String logDirPath = findLogDirPath(bundle);
		if (logDirPath != null) {
			logReader.readDir(logDirPath);
		}
		
		if (logReader.getLatestStartup() != null) {
			bundleModel.updateStartTimestamp(bundle, logReader.getLatestStartup());
		}
	}

	private String findLogDirPath(BundleEntry bundle) {
		if (bundle.getWebServerType() == WebServerType.TOMCAT) {
			return bundle.getWebServerDir().getAbsolutePath()+File.separator+"logs";
		} else {
			return null;
		}
	}

	public void stop() {
		getContext().getLogger().log("Stopping bundle monitor");
		if (timer != null && started) {
			timer.cancel();
			started = false;
		}
	}

	public BundleModel getBundleModel() {
		return bundleModel;
	}

	public void setBundleModel(BundleModel bundleModel) {
		this.bundleModel = bundleModel;
	}

	public boolean isStarted() {
		return started;
	}
	
}
