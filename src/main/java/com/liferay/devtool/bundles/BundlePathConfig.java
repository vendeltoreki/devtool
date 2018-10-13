package com.liferay.devtool.bundles;

import java.util.List;

import com.liferay.devtool.bundles.model.BundleModel;
import com.liferay.devtool.context.DevToolContext;
import com.liferay.devtool.utils.ConfigStorage;

public class BundlePathConfig {
	private static final String PROPS_NAME = "bundle_paths";
	private static final String PROPS_BUNDLE = "bundle.root.dir";
	private static final String PROPS_GIT = "git.root.dir";

	private DevToolContext context;
	private List<String> bundlePaths;
	private List<String> gitPaths;

	public void saveConfig(BundleModel bundleModel) {
		ConfigStorage configStorage = new ConfigStorage();
		configStorage.setContext(context);
		configStorage.setName(PROPS_NAME);
		configStorage.setComment("Temporary storage for detected bundle paths");
		
		for (BundleEntry bundleEntry : bundleModel.getBundles()) {
			configStorage.addToList(PROPS_BUNDLE, bundleEntry.getRootDirPath());
		}

		for (GitRepoEntry gitRepoEntry : bundleModel.getGitRepos()) {
			configStorage.addToList(PROPS_GIT, gitRepoEntry.getRootDir().getAbsolutePath());
		}
		
		configStorage.save();
	}
	
	public void tryLoadFromConfig() {
		try {
			ConfigStorage configStorage = new ConfigStorage();
			configStorage.setContext(context);
			configStorage.setName(PROPS_NAME);
			configStorage.load();
			
			bundlePaths = configStorage.getList(PROPS_BUNDLE);
			gitPaths = configStorage.getList(PROPS_GIT);
		} catch (Exception e) {
			context.getLogger().log(e);
		}
	}

	public List<String> getBundlePaths() {
		return bundlePaths;
	}

	public List<String> getGitPaths() {
		return gitPaths;
	}
	
}
