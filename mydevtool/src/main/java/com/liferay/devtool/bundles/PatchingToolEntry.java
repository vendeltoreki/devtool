package com.liferay.devtool.bundles;

public class PatchingToolEntry {
	private String rootDirPath;
	private String version;
	private String build;
	private boolean internal;
	private String sourcePath;
	
	public String getRootDirPath() {
		return rootDirPath;
	}
	
	public void setRootDirPath(String rootDirPath) {
		this.rootDirPath = rootDirPath;
	}
	
	public String getVersion() {
		return version;
	}
	
	public void setVersion(String version) {
		this.version = version;
	}
	
	public String getBuild() {
		return build;
	}
	
	public void setBuild(String build) {
		this.build = build;
	}
	
	public boolean isInternal() {
		return internal;
	}
	
	public void setInternal(boolean internal) {
		this.internal = internal;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}
}
