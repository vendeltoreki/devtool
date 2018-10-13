package com.liferay.devtool.bundles.model;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.BundleStatus;
import com.liferay.devtool.bundles.GitRepoEntry;
import com.liferay.devtool.context.DevToolContext;
import com.liferay.devtool.process.ProcessEntry;

public class BundleModel {
	private DevToolContext context;
	
	private List<BundleEntry> bundles = new ArrayList<>();
	private Map<String,BundleEntry> bundleMap = new HashMap<>();
	private List<GitRepoEntry> gitRepos = new ArrayList<>();
	private Map<String,GitRepoEntry> gitRepoMap = new HashMap<>();

	public List<BundleEntry> getBundles() {
		return bundles;
	}

	public List<GitRepoEntry> getGitRepos() {
		return gitRepos;
	}

	public void setGitRepos(List<GitRepoEntry> gitRepos) {
		this.gitRepos = gitRepos;
	}

	public void updateWithProcessEntries(List<ProcessEntry> processEntries) {
		for (BundleEntry bundle : bundles) {
			bundle.setRunningProcess(null);
			bundle.setBundleStatus(BundleStatus.STOPPED);
		}

		for (ProcessEntry process : processEntries) {
			if (process.getBundlePath() != null) {
				if (bundleMap.containsKey(getBundleKey(process.getBundlePath()))) {
					BundleEntry bundle = bundleMap.get(getBundleKey(process.getBundlePath()));
					bundle.setRunningProcess(process);
					bundle.setBundleStatus(BundleStatus.RUNNING);
				}
			}
		}
	}

	public BundleEntry addBundle(String absolutePath) {
		if (!bundleMap.containsKey(getBundleKey(absolutePath))) {
			context.getLogger().log("Bundle found: "+absolutePath);
			
			BundleEntry bundleEntry = new BundleEntry();
			bundleEntry.setRootDirPath(absolutePath);
			bundleMap.put(getBundleKey(absolutePath), bundleEntry);
			bundles.add(bundleEntry);
			return bundleEntry;
		}
		
		return null;
	}

	private String getBundleKey(String bundlePath) {
		return bundlePath.toLowerCase();
	}

	public GitRepoEntry addGitRepo(String absolutePath) {
		if (!gitRepoMap.containsKey(absolutePath)) {
			context.getLogger().log("GIT repo found: "+absolutePath);
			
			GitRepoEntry gitRepoEntry = new GitRepoEntry();
			gitRepoEntry.setRootDir(new File(absolutePath));
			gitRepoMap.put(absolutePath, gitRepoEntry);
			gitRepos.add(gitRepoEntry);
			
			return gitRepoEntry;
		}
		
		return null;
	}

	public BundleEntry updateGitRepoEntry(GitRepoEntry gitRepo) {
		if (gitRepo.getBuildTargetDir() != null && bundleMap.containsKey(getBundleKey(gitRepo.getBuildTargetDir()))) {
			BundleEntry bundle = bundleMap.get(getBundleKey(gitRepo.getBuildTargetDir()));
			if (bundle.getGitRepos() == null) {
				bundle.setGitRepos(new ArrayList<>());
			}
			
			if (!bundle.getGitRepos().contains(gitRepo)) {
				bundle.getGitRepos().add(gitRepo);
				return bundle;
			}
		}
		
		return null;
	}

	public boolean isEmpty() {
		return bundles.isEmpty();
	}
	
}
