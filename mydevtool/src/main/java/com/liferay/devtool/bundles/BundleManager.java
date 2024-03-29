package com.liferay.devtool.bundles;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.devtool.DevToolContext;
import com.liferay.devtool.bundles.reader.BundleDetailsReader;
import com.liferay.devtool.bundles.reader.BundleJarReader;
import com.liferay.devtool.bundles.reader.DbSchemaReader;
import com.liferay.devtool.bundles.reader.PatchingToolReader;
import com.liferay.devtool.process.BundleRunner;
import com.liferay.devtool.process.ProcessEntry;
import com.liferay.devtool.process.WindowsProcessTool;
import com.liferay.devtool.utils.ConfigStorage;
import com.liferay.devtool.utils.DbUtil;
import com.liferay.devtool.utils.TempDirUtil;

public class BundleManager implements FileSystemScanEventListener {
	private static final String PROPS_NAME = "bundle_paths";
	private static final String PROPS_BUNDLE = "bundle.root.dir";
	private static final String PROPS_GIT = "git.root.dir";

	private DevToolContext context;
	private BundleEventListener bundleEventListener;
	private List<BundleEntry> bundles = new ArrayList<>();
	private Map<String,BundleEntry> bundleMap = new HashMap<>();
	private List<GitRepoEntry> gitRepos = new ArrayList<>();
	private Map<String,GitRepoEntry> gitRepoMap = new HashMap<>();
	
	public void scanFileSystem() {
		context.getLogger().log("Scanning file system");
		EventBasedFileSystemScanner scanner = new EventBasedFileSystemScanner();
		scanner.setContext(context);
		scanner.setFileSystemScanEventListener(this);
		scanner.scanByConfig();
		
		saveConfig();
	}

	public void readDetails() {
		context.getLogger().log("Reading bundle details");
		
		if (bundles == null || bundles.isEmpty()) {
			tryLoadFromConfig();
		}
		
		if (context.getSysEnv().isWindows()) {
			queryProcessEntries();
		}
		readBundleDetails();
		readBundleVersions();
		readPatchingToolVersions();
		readGitRepoDetails();
		readBundleDbs();
	}

	private void saveConfig() {
		ConfigStorage configStorage = new ConfigStorage();
		configStorage.setContext(context);
		configStorage.setName(PROPS_NAME);
		configStorage.setComment("Temporary storage for detected bundle paths");
		
		for (BundleEntry bundleEntry : bundles) {
			configStorage.addToList(PROPS_BUNDLE, bundleEntry.getRootDirPath());
		}

		for (GitRepoEntry gitRepoEntry : gitRepos) {
			configStorage.addToList(PROPS_GIT, gitRepoEntry.getRootDir().getAbsolutePath());
		}
		
		configStorage.save();
	}

	private void tryLoadFromConfig() {
		try {
			ConfigStorage configStorage = new ConfigStorage();
			configStorage.setContext(context);
			configStorage.setName(PROPS_NAME);
			configStorage.load();
			
			List<String> bundlePaths = configStorage.getList(PROPS_BUNDLE);
			for (String path : bundlePaths) {
				onFoundBundle(path);
			}
			
			List<String> gitPaths = configStorage.getList(PROPS_GIT);
			for (String path : gitPaths) {
				onFoundGitRepo(path);
			}
		} catch (Exception e) {
			context.getLogger().log(e);
		}
	}

	private void queryProcessEntries() {
		WindowsProcessTool wp = new WindowsProcessTool();
		wp.setSysEnv(context.getSysEnv());
		wp.refresh();

		for (BundleEntry bundle : bundles) {
			bundle.setRunningProcess(null);
			bundle.setBundleStatus(BundleStatus.STOPPED);
		}

		for (ProcessEntry process : wp.getProcessEntries()) {
			if (process.getBundlePath() != null) {
				if (bundleMap.containsKey(getBundleKey(process.getBundlePath()))) {
					BundleEntry bundle = bundleMap.get(getBundleKey(process.getBundlePath()));
					bundle.setRunningProcess(process);
					bundle.setBundleStatus(BundleStatus.RUNNING);
				}
			}
		}
	}

	private void readBundleDbs() {
		for (BundleEntry bundle : bundles) {
			DbSchemaReader dbSchemaReader = new DbSchemaReader();
			dbSchemaReader.setSysEnv(context.getSysEnv());
			dbSchemaReader.setBundleEntry(bundle);
			dbSchemaReader.readDetails();
			
			sendUpdate(bundle);
		}
	}
	
	private void readBundleVersions() {
		for (BundleEntry bundle : bundles) {
			BundleJarReader bundleJarReader = new BundleJarReader();
			bundleJarReader.setSysEnv(context.getSysEnv());
			bundleJarReader.setBundleEntry(bundle);
			bundleJarReader.readDetails();
			
			sendUpdate(bundle);
		}
	}

	private void readPatchingToolVersions() {
		for (BundleEntry bundle : bundles) {
			PatchingToolReader ptJarReader = new PatchingToolReader();
			ptJarReader.setSysEnv(context.getSysEnv());
			ptJarReader.setBundleEntry(bundle);
			ptJarReader.readDetails();
			
			sendUpdate(bundle);
		}
	}
	
	private void readGitRepoDetails() {
		for (GitRepoEntry gitRepo : gitRepos) {
			GitRepoDetailsReader gitRepoDetailsReader = new GitRepoDetailsReader();
			gitRepoDetailsReader.setSysEnv(context.getSysEnv());
			gitRepoDetailsReader.setGitRepoEntry(gitRepo);
			gitRepoDetailsReader.readDetails();
			
			if (gitRepo.getBuildTargetDir() != null && bundleMap.containsKey(getBundleKey(gitRepo.getBuildTargetDir()))) {
				BundleEntry bundle = bundleMap.get(getBundleKey(gitRepo.getBuildTargetDir()));
				if (bundle.getGitRepos() == null) {
					bundle.setGitRepos(new ArrayList<>());
				}
				
				if (!bundle.getGitRepos().contains(gitRepo)) {
					bundle.getGitRepos().add(gitRepo);
					sendUpdate(bundle);
				}
			}
		}
	}

	private void readBundleDetails() {
		for (BundleEntry bundle : bundles) {
			BundleDetailsReader bundleDetailsReader = new BundleDetailsReader();
			bundleDetailsReader.setSysEnv(context.getSysEnv());
			bundleDetailsReader.setBundleEntry(bundle);
			bundleDetailsReader.readDetails();
			
			sendUpdate(bundle);
		}
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

	public void cleanTempDirs(BundleEntry entry) {
		TempDirUtil tempDirUtil = new TempDirUtil();
		tempDirUtil.extractPathsFromBundleEntry(entry);
		tempDirUtil.clean();
	}
	
	public void cleanDb(BundleEntry entry) {
		DbUtil dbUtil = new DbUtil();
		dbUtil.setBundleEntry(entry);
		dbUtil.cleanDb();
	}
	
	@Override
	public void onFoundBundle(String absolutePath) {
		if (!bundleMap.containsKey(getBundleKey(absolutePath))) {
			context.getLogger().log("Bundle found: "+absolutePath);
			
			BundleEntry bundleEntry = new BundleEntry();
			bundleEntry.setRootDirPath(absolutePath);
			bundleMap.put(getBundleKey(absolutePath), bundleEntry);
			bundles.add(bundleEntry);
			
			sendUpdate(bundleEntry);
		}
	}

	private String getBundleKey(String bundlePath) {
		return bundlePath.toLowerCase();
	}

	@Override
	public void onFoundGitRepo(String absolutePath) {
		if (!gitRepoMap.containsKey(absolutePath)) {
			context.getLogger().log("GIT repo found: "+absolutePath);
			
			GitRepoEntry gitRepoEntry = new GitRepoEntry();
			gitRepoEntry.setRootDir(new File(absolutePath));
			gitRepoMap.put(absolutePath, gitRepoEntry);
			gitRepos.add(gitRepoEntry);
		}
	}

	public void startBundle(BundleEntry bundleEntry) {
		if (isBundleStartable(bundleEntry)) {
			BundleRunner bundleRunner = new BundleRunner();
			bundleRunner.setContext(context);
			bundleRunner.setBundleEntry(bundleEntry);
			bundleRunner.start();
			
			bundleEntry.setBundleStatus(BundleStatus.STARTING);
			
			sendUpdate(bundleEntry);
		}
	}

	public void stopBundle(BundleEntry bundleEntry) {
		if (isBundleStoppable(bundleEntry)) {
			BundleRunner bundleRunner = new BundleRunner();
			bundleRunner.setContext(context);
			bundleRunner.setBundleEntry(bundleEntry);
			bundleRunner.stop();
			
			bundleEntry.setBundleStatus(BundleStatus.STOPPING);
			
			sendUpdate(bundleEntry);
		}
	}
	
	public boolean isBundleStartable(BundleEntry bundleEntry) {
		return bundleEntry != null && bundleEntry.getBundleStatus() == BundleStatus.STOPPED;
	}

	public boolean isBundleStoppable(BundleEntry bundleEntry) {
		return bundleEntry != null && bundleEntry.getBundleStatus() == BundleStatus.RUNNING;
	}

	public void setContext(DevToolContext context) {
		this.context = context;
	}
}
