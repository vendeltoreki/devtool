package com.liferay.devtool.bundles;

import java.util.List;

import com.liferay.devtool.bundlemonitor.BundleMonitor;
import com.liferay.devtool.bundles.model.BundleModel;
import com.liferay.devtool.bundles.reader.BundleDetailsReader;
import com.liferay.devtool.bundles.reader.BundleJarReader;
import com.liferay.devtool.bundles.reader.DbSchemaReader;
import com.liferay.devtool.bundles.reader.PatchingToolReader;
import com.liferay.devtool.context.DevToolContext;
import com.liferay.devtool.process.BundleRunner;
import com.liferay.devtool.utils.DbUtil;
import com.liferay.devtool.utils.TempDirUtil;

public class BundleManager implements FileSystemScanEventListener {
	private DevToolContext context;
	private BundleEventListener bundleEventListener;
	private BundleMonitor bundleMonitor = new BundleMonitor();
	private BundleModel bundleModel = new BundleModel();
	
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
		
		if (bundleModel.isEmpty()) {
			tryLoadFromConfig();
		}
		
		if (context.getSysEnv().isWindows()) {
			if (!bundleMonitor.isStarted()) {
				bundleMonitor.setBundleModel(bundleModel);
				bundleMonitor.start();
			}
		}
		readBundleDetails();
		readBundleVersions();
		readPatchingToolVersions();
		readGitRepoDetails();
		readBundleDbs();
	}

	private void saveConfig() {
		BundlePathConfig bundlePathConfig = new BundlePathConfig();
		bundlePathConfig.saveConfig(bundleModel);
	}

	private void tryLoadFromConfig() {
		BundlePathConfig bundlePathConfig = new BundlePathConfig();
		bundlePathConfig.tryLoadFromConfig();

		for (String path : bundlePathConfig.getBundlePaths()) {
			onFoundBundle(path);
		}
		
		for (String path : bundlePathConfig.getGitPaths()) {
			onFoundGitRepo(path);
		}
	}

	private void readBundleDbs() {
		for (BundleEntry bundle : bundleModel.getBundles()) {
			DbSchemaReader dbSchemaReader = new DbSchemaReader();
			dbSchemaReader.setSysEnv(context.getSysEnv());
			dbSchemaReader.setBundleEntry(bundle);
			dbSchemaReader.readDetails();
			
			sendUpdate(bundle);
		}
	}
	
	private void readBundleVersions() {
		for (BundleEntry bundle : bundleModel.getBundles()) {
			BundleJarReader bundleJarReader = new BundleJarReader();
			bundleJarReader.setSysEnv(context.getSysEnv());
			bundleJarReader.setBundleEntry(bundle);
			bundleJarReader.readDetails();
			
			sendUpdate(bundle);
		}
	}

	private void readPatchingToolVersions() {
		for (BundleEntry bundle : bundleModel.getBundles()) {
			PatchingToolReader ptJarReader = new PatchingToolReader();
			ptJarReader.setSysEnv(context.getSysEnv());
			ptJarReader.setBundleEntry(bundle);
			ptJarReader.readDetails();
			
			sendUpdate(bundle);
		}
	}
	
	private void readGitRepoDetails() {
		for (GitRepoEntry gitRepo : bundleModel.getGitRepos()) {
			GitRepoDetailsReader gitRepoDetailsReader = new GitRepoDetailsReader();
			gitRepoDetailsReader.setSysEnv(context.getSysEnv());
			gitRepoDetailsReader.setGitRepoEntry(gitRepo);
			gitRepoDetailsReader.readDetails();
			
			BundleEntry foundBundle = bundleModel.updateGitRepoEntry(gitRepo);
			if (foundBundle != null) {
				sendUpdate(foundBundle);
			}
		}
	}

	private void readBundleDetails() {
		for (BundleEntry bundle : bundleModel.getBundles()) {
			BundleDetailsReader bundleDetailsReader = new BundleDetailsReader();
			bundleDetailsReader.setSysEnv(context.getSysEnv());
			bundleDetailsReader.setBundleEntry(bundle);
			bundleDetailsReader.readDetails();
			
			bundleModel.updateBundleDetails(bundle);
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
		return bundleModel.getBundles();
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
		BundleEntry bundleEntry = bundleModel.addBundle(absolutePath);
		if (bundleEntry != null) {
			context.getLogger().log("Bundle found: "+absolutePath);
			sendUpdate(bundleEntry);
		}
	}

	@Override
	public void onFoundGitRepo(String absolutePath) {
		bundleModel.addGitRepo(absolutePath);
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

	public void startAndWaitBundle(BundleEntry bundleEntry) {
		
	}
	
	public void stopAndWaitBundle(BundleEntry bundleEntry) {
	}

	public void restartBundle(BundleEntry bundleEntry) {
		stopAndWaitBundle(bundleEntry);
		startAndWaitBundle(bundleEntry);
	}
	
	public void stop() {
		bundleMonitor.stop();
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
