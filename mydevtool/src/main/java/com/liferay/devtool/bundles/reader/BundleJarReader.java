package com.liferay.devtool.bundles.reader;

import java.io.File;
import java.util.Map;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.WebServerType;
import com.liferay.devtool.utils.JarManifestReader;
import com.liferay.devtool.utils.SysEnv;

public class BundleJarReader {
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
		String jarPath = null;
		
		if (bundleEntry.getWebServerType() == WebServerType.TOMCAT) {
			jarPath = bundleEntry.getWebServerDir()+"\\webapps\\ROOT\\WEB-INF\\lib\\portal-impl.jar";
		} else if (bundleEntry.getWebServerType() == WebServerType.WILDFLY) {
			jarPath = bundleEntry.getWebServerDir()+"\\standalone\\deployments\\ROOT.war\\WEB-INF\\lib\\portal-impl.jar";
		} else if (bundleEntry.getWebServerType() == WebServerType.JBOSS) {
			jarPath = bundleEntry.getWebServerDir()+"\\standalone\\deployments\\ROOT.war\\WEB-INF\\lib\\portal-impl.jar";
		}
		
		File jarFile = sysEnv.createFile(jarPath);
		if (jarFile.exists() && jarFile.isFile()) {
			readJarFile(jarFile);
		}
	}

	private void readJarFile(File file) {
		JarManifestReader jarManifestReader = new JarManifestReader(); 

		/*
Liferay-Portal-Build-Number: 7010
Liferay-Portal-Installed-Patches: de-40-7010
Liferay-Portal-Parent-Build-Number: 7010
Liferay-Portal-Release-Info: Liferay DXP Digital Enterprise 7.0.10 GA1
  (Wilberforce / Build 7010 / June 15, 2016)
Liferay-Portal-Server-Info: Liferay DXP Digital Enterprise / 7.0.10
Liferay-Portal-Version: 7.0.10
		 */
		jarManifestReader.addKeys(
			"Liferay-Portal-Build-Number",
			"Liferay-Portal-Installed-Patches",
			"Liferay-Portal-Parent-Build-Number",
			"Liferay-Portal-Release-Info",
			"Liferay-Portal-Server-Info",
			"Liferay-Portal-Version");
		
		Map<String,String> res = jarManifestReader.readJarFile(file); 
		
		if (res.containsKey("Liferay-Portal-Version")) {
			bundleEntry.setPortalVersion(res.get("Liferay-Portal-Version"));
		}
		
		if (res.containsKey("Liferay-Portal-Installed-Patches")) {
			bundleEntry.setPortalPatches(res.get("Liferay-Portal-Installed-Patches"));
		}
		
	}
	
}
