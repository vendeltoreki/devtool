package com.liferay.devtool.eventlog;

import java.util.Date;

public class EventLogger {
	private LogEventListener logEventListener;
	
	public void log(String message) {
		log(message, null);
	}
	
	public void log(Throwable exception) {
		log("Error", exception);
	}
	
	public void log(String message, Throwable exception) {
		LogEntry logEntry = new LogEntry();
		logEntry.setTime(new Date());
		logEntry.setMessage(message);
		logEntry.setException(exception);
		logEntry.setThread(Thread.currentThread().getName());
		
		if (logEventListener != null) {
			logEventListener.onLogEventReceived(logEntry);
		}		
	}

	public LogEventListener getLogEventListener() {
		return logEventListener;
	}

	public void setLogEventListener(LogEventListener logEventListener) {
		this.logEventListener = logEventListener;
	}
}
