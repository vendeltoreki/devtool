package com.liferay.devtool.bundlemonitor;

import com.liferay.devtool.bundles.BundleEntry;

public interface BundleMonitorEventListener {
	void startDetected(BundleEntry bundleEntry);
	void stopDetected(BundleEntry bundleEntry);
}
