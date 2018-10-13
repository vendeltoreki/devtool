package com.liferay.devtool.experiment;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.BundleEventListener;
import com.liferay.devtool.bundles.BundleManager;
import com.liferay.devtool.context.DevToolContext;

public class BundleManagerTest {

	public static void main(String[] args) {
		BundleManager bundleManager = new BundleManager();
		bundleManager.setContext(DevToolContext.getDefault());
		
		bundleManager.setBundleEventListener(new BundleEventListener() {
			
			@Override
			public void onUpdate(BundleEntry entry) {
				System.out.println("updateEntry "+entry);
			}
		});
		
		bundleManager.scanFileSystem();
		bundleManager.readDetails();
	}

}
