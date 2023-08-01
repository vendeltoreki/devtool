package com.liferay.devtool.bundles;

public interface FileSystemScanEventListener {
	public void onFoundBundle(String absolutePath);
	public void onFoundGitRepo(String absolutePath);
}
