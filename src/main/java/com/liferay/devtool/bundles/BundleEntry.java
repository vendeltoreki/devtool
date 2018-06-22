package com.liferay.devtool.bundles;

import java.io.File;
import java.util.List;

import com.liferay.devtool.process.ProcessEntry;

public class BundleEntry {
	private String rootDirPath;
	private File rootDir;
	private File webServerDir;
	private WebServerType webServerType;
	private boolean multipleWebServers = false;
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
	private String portalVersion;
	private String portalPatches;
	private DbSchemaEntry dbSchemaEntry;
	private ProcessEntry runningProcess;
	private boolean deleted = false;

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

	public File getWebServerDir() {
		return webServerDir;
	}

	public void setWebServerDir(File webServerDir) {
		this.webServerDir = webServerDir;
	}

	public WebServerType getWebServerType() {
		return webServerType;
	}

	public void setWebServerType(WebServerType webServerType) {
		this.webServerType = webServerType;
	}

	public boolean isMultipleWebServers() {
		return multipleWebServers;
	}

	public void setMultipleWebServers(boolean multipleWebServers) {
		this.multipleWebServers = multipleWebServers;
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

	public String getPortalVersion() {
		return portalVersion;
	}

	public void setPortalVersion(String portalVersion) {
		this.portalVersion = portalVersion;
	}

	public String getPortalPatches() {
		return portalPatches;
	}

	public void setPortalPatches(String portalPatches) {
		this.portalPatches = portalPatches;
	}

	public DbSchemaEntry getDbSchemaEntry() {
		return dbSchemaEntry;
	}

	public void setDbSchemaEntry(DbSchemaEntry dbSchemaEntry) {
		this.dbSchemaEntry = dbSchemaEntry;
	}

	public ProcessEntry getRunningProcess() {
		return runningProcess;
	}

	public void setRunningProcess(ProcessEntry runningProcess) {
		this.runningProcess = runningProcess;
	}

	public boolean isDeleted() {
		return deleted;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	@Override
	public String toString() {
		return "BundleEntry [rootDir=" + rootDirPath + ", tomcatDir=" + webServerDir + ", name=" + name + ", tomcatVersion="
				+ tomcatVersion + ", memoryXmx=" + memoryXmx + ", memoryPermSize=" + memoryPermSize + "]";
	}

}
