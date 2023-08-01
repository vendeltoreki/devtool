package com.liferay.devtool.bundles;

import java.io.File;

public class GitRepoEntry {
	private File rootDir;
	private String name;
	private String originUrl;
	private String upstreamUrl;
	private String currentBranch;
	private String buildTargetDir;

	public File getRootDir() {
		return rootDir;
	}

	public void setRootDir(File rootDir) {
		this.rootDir = rootDir;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getOriginUrl() {
		return originUrl;
	}

	public void setOriginUrl(String originUrl) {
		this.originUrl = originUrl;
	}

	public String getUpstreamUrl() {
		return upstreamUrl;
	}

	public void setUpstreamUrl(String upstreamUrl) {
		this.upstreamUrl = upstreamUrl;
	}

	public String getCurrentBranch() {
		return currentBranch;
	}

	public void setCurrentBranch(String currentBranch) {
		this.currentBranch = currentBranch;
	}

	public String getBuildTargetDir() {
		return buildTargetDir;
	}

	public void setBuildTargetDir(String buildTargetDir) {
		this.buildTargetDir = buildTargetDir;
	}

	@Override
	public String toString() {
		return "GitRepoEntry [rootDir=" + rootDir + ", name=" + name + ", originUrl=" + originUrl + ", upstreamUrl="
				+ upstreamUrl + ", currentBranch=" + currentBranch+ ", buildTargetDir=" + buildTargetDir + "]";
	}

}
