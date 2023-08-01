package com.liferay.devtool.eventlog;

public interface LogEventListener {
	public void onLogEventReceived(LogEntry logEntry);
}
