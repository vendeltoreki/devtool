package com.liferay.devtool.bundles;

import java.util.HashSet;
import java.util.Set;

public class DbSchemaEntry {
	private String schemaName;
	private int tableCount;
	private String schemaVersion;
	private Set<String> deployedApps = new HashSet<>();
	private Double sizeInMb;
	
	public String getSchemaName() {
		return schemaName;
	}
	
	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}
	
	public int getTableCount() {
		return tableCount;
	}
	
	public void setTableCount(int tableCount) {
		this.tableCount = tableCount;
	}
	
	public String getSchemaVersion() {
		return schemaVersion;
	}
	
	public void setSchemaVersion(String schemaVersion) {
		this.schemaVersion = schemaVersion;
	}

	public void addDeployedApp(String appName) {
		deployedApps.add(appName);
	}

	public Set<String> getDeployedApps() {
		return deployedApps;
	}

	public boolean hasDeployedApps() {
		return deployedApps != null && !deployedApps.isEmpty();
	}

	public Double getSizeInMb() {
		return sizeInMb;
	}

	public void setSizeInMb(Double sizeInMb) {
		this.sizeInMb = sizeInMb;
	}
}
