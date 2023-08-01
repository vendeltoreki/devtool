package com.liferay.devtool.bundles.reader;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.utils.SysEnv;

public class BundleDetailsReader {
	private SysEnv sysEnv;
	private BundleEntry bundleEntry;
	
	public SysEnv getSysEnv() {
		return sysEnv;
	}

	public void setSysEnv(SysEnv sysEnv) {
		this.sysEnv = sysEnv;
	}

	public BundleEntry getBundleEntry() {
		return bundleEntry;
	}

	public void setBundleEntry(BundleEntry bundleEntry) {
		this.bundleEntry = bundleEntry;
	}

	public void readDetails() {
		String rootPath = bundleEntry.getRootDirPath();
		
		BundleEntryFactory bundleEntryFactory = new BundleEntryFactory();
		BundleEntry newEntry = bundleEntryFactory.create(sysEnv.createFile(rootPath));
		
		if (newEntry != null) {
			bundleEntry.setName(newEntry.getName());
			bundleEntry.setRootDir(newEntry.getRootDir());
			bundleEntry.setWebServerDir(newEntry.getWebServerDir());
			bundleEntry.setWebServerType(newEntry.getWebServerType());
			bundleEntry.setMultipleWebServers(newEntry.isMultipleWebServers());
			bundleEntry.setTempDirs(newEntry.getTempDirs());
			bundleEntry.setMemoryPermSize(newEntry.getMemoryPermSize());
			bundleEntry.setMemoryXmx(newEntry.getMemoryXmx());
			bundleEntry.setDbDriverClass(newEntry.getDbDriverClass());
			bundleEntry.setDbUrl(newEntry.getDbUrl());
			bundleEntry.setDbUsername(newEntry.getDbUsername());
			bundleEntry.setDbPassword(newEntry.getDbPassword());
			bundleEntry.setTomcatVersion(newEntry.getTomcatVersion());
			bundleEntry.setConfiguredServerPorts(newEntry.getConfiguredServerPorts());
			bundleEntry.setDeleted(false);
		} else {
			bundleEntry.setDeleted(true);
		}
	}
	
}
