package com.liferay.devtool.bundles;

import com.liferay.devtool.utils.SysEnv;

public class GitRepoDetailsReader {
	private SysEnv sysEnv;
	private GitRepoEntry gitRepoEntry;
	
	public SysEnv getSysEnv() {
		return sysEnv;
	}

	public void setSysEnv(SysEnv sysEnv) {
		this.sysEnv = sysEnv;
	}

	public GitRepoEntry getGitRepoEntry() {
		return gitRepoEntry;
	}

	public void setGitRepoEntry(GitRepoEntry gitRepoEntry) {
		this.gitRepoEntry = gitRepoEntry;
	}

	public void readDetails() {
		GitRepoEntryFactory bundleEntryFactory = new GitRepoEntryFactory();
		GitRepoEntry newEntry = bundleEntryFactory.create(gitRepoEntry.getRootDir());
		
		if (newEntry != null) {
			gitRepoEntry.setName(newEntry.getName());
			gitRepoEntry.setBuildTargetDir(newEntry.getBuildTargetDir());
			gitRepoEntry.setCurrentBranch(newEntry.getCurrentBranch());
			gitRepoEntry.setOriginUrl(newEntry.getOriginUrl());
			gitRepoEntry.setUpstreamUrl(newEntry.getUpstreamUrl());
		}	
	}

}
