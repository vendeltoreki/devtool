package com.liferay.devtool.bundlemonitor;

import java.util.Timer;
import java.util.TimerTask;

import com.liferay.devtool.bundles.model.BundleModel;
import com.liferay.devtool.context.ContextBase;
import com.liferay.devtool.process.WindowsProcessTool;

public class BundleMonitor extends ContextBase {
	private BundleMonitorEventListener eventListener;
	private Timer timer;
	private BundleModel bundleModel;
	
	public void start() {
		timer = new Timer();
		timer.schedule(new TimerTask() {
			
			@Override
			public void run() {
				runMonitor();
			}
		}, 1000, 10000);
	}
	
	private void runMonitor() {
		WindowsProcessTool wp = new WindowsProcessTool();
		wp.setSysEnv(getContext().getSysEnv());
		wp.refresh();

		bundleModel.updateWithProcessEntries(wp.getProcessEntries());
	}
	
	public void stop() {
		if (timer != null) {
			timer.cancel();
		}
	}
}
