package com.liferay.devtool;

import com.liferay.devtool.eventlog.EventLogger;
import com.liferay.devtool.utils.SysEnv;

public class DevToolContext {
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

	public static DevToolContext getDefault() {
		DevToolContext context = new DevToolContext();
		context.setSysEnv(new SysEnv());
		context.setLogger(new EventLogger());
		return context;
	}
}
