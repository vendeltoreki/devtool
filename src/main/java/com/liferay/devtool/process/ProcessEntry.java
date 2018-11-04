package com.liferay.devtool.process;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.liferay.devtool.bundles.model.CloneUtil;

public class ProcessEntry {
	private String pid;
	private ProcessEntry parentProcess;
	private String commandLine;
	private String bundlePath;
	private String windowTitle;
	private String execName;
	private List<String> listeningPorts;
	private String timezone;
	private Date processStartTime;
	
	public ProcessEntry() {
	}
	
	public ProcessEntry(ProcessEntry original) {
		this.pid = original.pid;
		this.parentProcess = null;
		this.commandLine = original.commandLine;
		this.bundlePath = original.bundlePath;
		this.windowTitle = original.windowTitle;
		this.execName = original.execName;
		this.listeningPorts = CloneUtil.cloneStringList(original.listeningPorts);
		this.timezone = original.timezone;
		this.processStartTime = original.processStartTime;
	}

	public String getPid() {
		return pid;
	}
	
	public void setPid(String pid) {
		this.pid = pid;
	}
	
	public ProcessEntry getParentProcess() {
		return parentProcess;
	}

	public void setParentProcess(ProcessEntry parentProcess) {
		this.parentProcess = parentProcess;
	}

	public String getCommandLine() {
		return commandLine;
	}
	
	public void setCommandLine(String commandLine) {
		this.commandLine = commandLine;
	}

	public String getBundlePath() {
		return bundlePath;
	}

	public void setBundlePath(String bundlePath) {
		this.bundlePath = bundlePath;
	}

	public String getWindowTitle() {
		return windowTitle;
	}

	public void setWindowTitle(String windowTitle) {
		this.windowTitle = windowTitle;
	}

	public String getExecName() {
		return execName;
	}

	public void setExecName(String execName) {
		this.execName = execName;
	}

	public List<String> getListeningPorts() {
		return listeningPorts;
	}

	public void setListeningPorts(List<String> listeningPorts) {
		this.listeningPorts = listeningPorts;
	}

	public void addPort(String port) {
		if (listeningPorts == null) {
			listeningPorts = new ArrayList<>();
		}
		
		if (!listeningPorts.contains(port)) {
			listeningPorts.add(port);
		}
	}

	public String getTimezone() {
		return timezone;
	}

	public void setTimezone(String timezone) {
		this.timezone = timezone;
	}

	public Date getProcessStartTime() {
		return processStartTime;
	}

	public void setProcessStartTime(Date processStartTime) {
		this.processStartTime = processStartTime;
	}

	@Override
	public String toString() {
		return "ProcessEntry [pid=" + pid + ", "
				+ "commandLine=" + commandLine + ", "
				+ "bundlePath=" + bundlePath + ", "
				+ "windowTitle=" + windowTitle + ", "
				+ "execName=" + execName + ", "
				+ "listeningPorts=" + listeningPorts + "]";
	}

	public String getShortName() {
		return windowTitle != null && !windowTitle.equalsIgnoreCase("N/A") ? windowTitle : execName;
	}
	
	
}
