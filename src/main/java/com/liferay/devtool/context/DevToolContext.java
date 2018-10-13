package com.liferay.devtool.context;

import com.liferay.devtool.eventlog.EventLogger;
import com.liferay.devtool.utils.SysEnv;

public class DevToolContext {
	private static volatile DevToolContext defaultContext;
	private SysEnv sysEnv;
	private EventLogger logger;
	
	public SysEnv getSysEnv() {
		return sysEnv;
	}
	
	public void setSysEnv(SysEnv sysEnv) {
		this.sysEnv = sysEnv;
	}
	
	public EventLogger getLogger() {
		return logger;
	}
	
	public void setLogger(EventLogger logger) {
		this.logger = logger;
	}

	public static synchronized DevToolContext getDefault() {
		if (defaultContext == null) {
			defaultContext = new DevToolContext();
			defaultContext.setSysEnv(new SysEnv());
			defaultContext.setLogger(new EventLogger());
		}
		return defaultContext;
	}
}
