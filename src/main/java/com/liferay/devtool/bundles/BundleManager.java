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
		
		bundles.clear();
		bundles.addAll(scanner.getFoundBundles());
		
		gitRepos.clear();
		gitRepos.addAll(scanner.getFoundGitRepos());
		
		connectBundlesWithSources();
		
		for (BundleEntry bundle : bundles) {
			sendUpdate(bundle);
		}
	}

	private void connectBundlesWithSources() {
		Map<String,BundleEntry> bundleMap = new HashMap<>();
		for (BundleEntry bundle : bundles) {
			bundleMap.put(bundle.getRootDir().getAbsolutePath(), bundle);
		}
		
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
}
