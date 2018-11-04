package com.liferay.devtool.bundles.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.BundleStatus;
import com.liferay.devtool.bundles.GitRepoEntry;
import com.liferay.devtool.context.ContextBase;
import com.liferay.devtool.process.ProcessEntry;

public class BundleModel extends ContextBase {
	private List<BundleEntry> bundles = new ArrayList<>();
	private Map<String,BundleEntry> bundleMap = new HashMap<>();
	private List<GitRepoEntry> gitRepos = new ArrayList<>();
	private Map<String,GitRepoEntry> gitRepoMap = new HashMap<>();
	private List<BundleModelListener> listeners = new ArrayList<>();
	
	private Object lock = new Object();
	
	public List<BundleEntry> getBundles() {
		List<BundleEntry> res = null;
		synchronized (lock) {
			res = new ArrayList<>(bundles.size());
			for (BundleEntry bundle : bundles) {
				res.add(new BundleEntry(bundle));
			}
		}
		
		return Collections.unmodifiableList(res);
	}

	public List<BundleEntry> getBundlesByStatus(BundleStatus bundleStatu) {
		List<BundleEntry> res = null;
		synchronized (lock) {
			res = new ArrayList<>(bundles.size());
			for (BundleEntry bundle : bundles) {
				if (bundle.getBundleStatus() == bundleStatu) {
					res.add(new BundleEntry(bundle));
				}
			}
		}
		
		return Collections.unmodifiableList(res);
	}
	
	public List<GitRepoEntry> getGitRepos() {
		List<GitRepoEntry> res = null;
		synchronized (lock) {
			res = new ArrayList<>(gitRepos.size());
			for (GitRepoEntry entry : gitRepos) {
				res.add(new GitRepoEntry(entry));
			}
		}
		
		return Collections.unmodifiableList(res);
	}

	public void updateWithProcessEntries(List<ProcessEntry> processEntries) {
		synchronized (lock) {
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
	}

	public BundleEntry addBundle(String absolutePath) {
		BundleEntry res = null;
		
		synchronized (lock) {
			if (!bundleMap.containsKey(getBundleKey(absolutePath))) {
				getContext().getLogger().log("Bundle found: "+absolutePath);
				
				BundleEntry bundleEntry = new BundleEntry();
				bundleEntry.setRootDirPath(absolutePath);
				bundleMap.put(getBundleKey(absolutePath), bundleEntry);
				bundles.add(bundleEntry);
				
				Collections.sort(bundles, new Comparator<BundleEntry>() {
	
					@Override
					public int compare(BundleEntry o1, BundleEntry o2) {
						return o1.getRootDirPath().compareTo(o2.getRootDirPath());
					}
				});
				
				res = bundleEntry;
			}
		}
		
		return res;
	}

	private String getBundleKey(String bundlePath) {
		return bundlePath.toLowerCase();
	}

	public void addGitRepo(String absolutePath) {
		synchronized (lock) {
			if (!gitRepoMap.containsKey(absolutePath)) {
				getContext().getLogger().log("GIT repo found: "+absolutePath);
				
				GitRepoEntry gitRepoEntry = new GitRepoEntry();
				gitRepoEntry.setRootDir(new File(absolutePath));
				gitRepoMap.put(absolutePath, gitRepoEntry);
				gitRepos.add(gitRepoEntry);
			}
		}
	}

	public BundleEntry updateGitRepoEntry(GitRepoEntry gitRepo) {
		BundleEntry res = null;
		
		synchronized (lock) {
			if (gitRepo.getBuildTargetDir() != null && bundleMap.containsKey(getBundleKey(gitRepo.getBuildTargetDir()))) {
				BundleEntry bundle = bundleMap.get(getBundleKey(gitRepo.getBuildTargetDir()));
				if (bundle.getGitRepos() == null) {
					bundle.setGitRepos(new ArrayList<>());
				}
				
				if (!bundle.getGitRepos().contains(gitRepo)) {
					bundle.getGitRepos().add(gitRepo);
					res = new BundleEntry(bundle);
				}
			}
		}
		
		return res;
	}

	public void updateStartTimestamp(BundleEntry bundle, Date latestStartup) {
		ModelEvent event = null;
		synchronized (lock) {
			BundleEntry foundBundle = findBundle(bundle);
			if (isBundleFullyStarted(latestStartup, foundBundle)) {
				foundBundle.setBundleStatus(BundleStatus.RUNNING);
				event = new ModelEvent(EventType.STATUS_UPDATE, new BundleEntry(bundle));
			}
		}
		
		if (event != null) {
			sendEvent(event);
		}
	}

	private boolean isBundleFullyStarted(Date latestStartup, BundleEntry foundBundle) {
		return foundBundle != null
				&& latestStartup != null
				&& foundBundle.getBundleStatus() == BundleStatus.PROCESS_STARTED
				&& foundBundle.getRunningProcess() != null
				&& foundBundle.getRunningProcess().getProcessStartTime() != null
				&& latestStartup.after(foundBundle.getRunningProcess().getProcessStartTime());
	}

	private BundleEntry findBundle(BundleEntry bundle) {
		return bundleMap.get(getBundleKey(bundle.getRootDirPath()));
	}

	public boolean isEmpty() {
		boolean res = true;
		synchronized (lock) {
			res = bundles.isEmpty();
		}
		return res;
	}
	
	public void addListener(BundleModelListener listener) {
		listeners.add(listener);
	}

	private void sendEvent(ModelEvent event) {
		for (BundleModelListener listener : listeners) {
			listener.bundleUpdated(event);
		}
	}

	public void updateBundleDetails(BundleEntry bundle) {
		ModelEvent event = null;
		synchronized (lock) {
			BundleEntry foundBundle = findBundle(bundle);
			foundBundle.setDbDriverClass(bundle.getDbDriverClass());
			foundBundle.setDbUrl(bundle.getDbUrl());
			foundBundle.setDbUsername(bundle.getDbUsername());
			foundBundle.setDbUsername(bundle.getDbPassword());
			event = new ModelEvent(EventType.DETAILS_LOADED, bundle);
		}
		
		if (event != null) {
			sendEvent(event);
		}
	}
}
