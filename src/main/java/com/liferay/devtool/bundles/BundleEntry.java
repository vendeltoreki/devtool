package com.liferay.devtool.bundles;

import java.io.File;
import java.util.List;

public class BundleEntry {
	private String rootDirPath;
	private File rootDir;
	private File tomcatDir;
	private String name;
	private String tomcatVersion;
	private int memoryXmx;
	private int memoryPermSize;
	private String dbDriverClass;
	private String dbUrl;
	private String dbUsername;
	private String dbPassword;
	private List<GitRepoEntry> gitRepos;
	private List<TempDirEntry> tempDirs;

	public String getRootDirPath() {
		return rootDirPath;
	}

	public void setRootDirPath(String rootDirPath) {
		this.rootDirPath = rootDirPath;
	}

	public String getDbDriverClass() {
		return dbDriverClass;
	}

	public void setDbDriverClass(String dbDriverClass) {
		this.dbDriverClass = dbDriverClass;
	}

	public String getDbUrl() {
		return dbUrl;
	}

	public void setDbUrl(String dbUrl) {
		this.dbUrl = dbUrl;
	}

	public String getDbUsername() {
		return dbUsername;
	}

	public void setDbUsername(String dbUsername) {
		this.dbUsername = dbUsername;
	}

	public String getDbPassword() {
		return dbPassword;
	}

	public void setDbPassword(String dbPassword) {
		this.dbPassword = dbPassword;
	}

	public File getRootDir() {
		return rootDir;
	}

	public void setRootDir(File rootDir) {
		this.rootDir = rootDir;
	}

	public File getTomcatDir() {
		return tomcatDir;
	}

	public void setTomcatDir(File tomcatDir) {
		this.tomcatDir = tomcatDir;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getTomcatVersion() {
		return tomcatVersion;
	}

	public void setTomcatVersion(String tomcatVersion) {
		this.tomcatVersion = tomcatVersion;
	}

	public int getMemoryXmx() {
		return memoryXmx;
	}

	public void setMemoryXmx(int memoryXmx) {
		this.memoryXmx = memoryXmx;
	}

	public int getMemoryPermSize() {
		return memoryPermSize;
	}

	public void setMemoryPermSize(int memoryPermSize) {
		this.memoryPermSize = memoryPermSize;
	}

	public List<GitRepoEntry> getGitRepos() {
		return gitRepos;
	}

	public void setGitRepos(List<GitRepoEntry> gitRepos) {
		this.gitRepos = gitRepos;
	}

	public List<TempDirEntry> getTempDirs() {
		return tempDirs;
	}

	public void setTempDirs(List<TempDirEntry> tempDirs) {
		this.tempDirs = tempDirs;
	}

	@Override
	public String toString() {
		return "BundleEntry [rootDir=" + rootDir + ", tomcatDir=" + tomcatDir + ", name=" + name + ", tomcatVersion="
				+ tomcatVersion + ", memoryXmx=" + memoryXmx + ", memoryPermSize=" + memoryPermSize + "]";
	}

}
