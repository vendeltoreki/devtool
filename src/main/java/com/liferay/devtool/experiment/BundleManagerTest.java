package com.liferay.devtool.experiment;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.BundleEventListener;
import com.liferay.devtool.bundles.BundleManager2;
import com.liferay.devtool.utils.SysEnv;

public class BundleManagerTest {

	public static void main(String[] args) {
		BundleManager2 bundleManager = new BundleManager2();
		bundleManager.setSysEnv(new SysEnv());
		
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
