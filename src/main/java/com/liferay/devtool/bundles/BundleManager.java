package com.liferay.devtool.bundles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BundleManager {
	private BundleEventListener bundleEventListener;
	private List<BundleEntry> bundles = new ArrayList<>();
	private List<GitRepoEntry> gitRepos = new ArrayList<>();
	
	public void scanFileSystem() {
		FileSystemScanner scanner = new FileSystemScanner();
		scanner.scan("C:\\");
		
		updateBundlesList(scanner.getFoundBundles());
		
		gitRepos.clear();
		gitRepos.addAll(scanner.getFoundGitRepos());
		
		connectBundlesWithSources();
		
		for (BundleEntry bundle : bundles) {
			sendUpdate(bundle);
		}
	}

	private String getKey(BundleEntry entry) {
		return entry.getRootDir().getAbsolutePath();
	}
	
	private void updateBundlesList(List<BundleEntry> foundBundles) {
		Map<String, BundleEntry> bundleMap = createBundleMap(bundles);

		for (BundleEntry foundBundle : foundBundles) {
			if (!bundleMap.containsKey(getKey(foundBundle))) {
				bundles.add(foundBundle);
				bundleMap.put(getKey(foundBundle), foundBundle);
			}
		}

		Map<String, BundleEntry> foundBundleMap = createBundleMap(foundBundles);
		
		
		
		bundles.clear();
		bundles.addAll(foundBundles);
	}

	private Map<String, BundleEntry> createBundleMap(List<BundleEntry> bundleList) {
		Map<String,BundleEntry> bundleMap = new HashMap<>();
		for (BundleEntry bundle : bundleList) {
			bundleMap.put(getKey(bundle), bundle);
		}
		return bundleMap;
	}

	private void connectBundlesWithSources() {
		Map<String, BundleEntry> bundleMap = createBundleMap(bundles);
		
		for (GitRepoEntry repo : gitRepos) {
			if (repo.getBuildTargetDir() != null && bundleMap.containsKey(repo.getBuildTargetDir())) {
				BundleEntry b = bundleMap.get(repo.getBuildTargetDir());
				if (b.getGitRepos() == null) {
					b.setGitRepos(new ArrayList<>());
				}
				b.getGitRepos().add(repo);
			}
		}
	}

	public void reScanEntries() {
		
	}
	
	private void sendUpdate(BundleEntry bundle) {
		if (bundleEventListener != null) {
			bundleEventListener.onUpdate(bundle);
		}
	}
	
	public void setBundleEventListener(BundleEventListener bundleEventListener) {
		this.bundleEventListener = bundleEventListener;
	}

	public List<BundleEntry> getEntries() {
		return bundles;
	}
}
