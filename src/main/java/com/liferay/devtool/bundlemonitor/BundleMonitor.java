package com.liferay.devtool.bundlemonitor;

import java.util.Timer;
import java.util.TimerTask;

import com.liferay.devtool.DevToolContext;

public class BundleMonitor {
	private DevToolContext context;
	private BundleMonitorEventListener eventListener;
	private Timer timer;
	
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
		
	}
	
	public void stop() {
		if (timer != null) {
			timer.cancel();
		}
	}
}
