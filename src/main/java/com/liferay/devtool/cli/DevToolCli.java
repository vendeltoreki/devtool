package com.liferay.devtool.cli;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.BundleEventListener;
import com.liferay.devtool.bundles.BundleManager;
import com.liferay.devtool.devenv.DevEnvChecker;
import com.liferay.devtool.devenv.DevEnvEventListener;
import com.liferay.devtool.devenv.checks.BaseDevEnvCheckEntry;
import com.liferay.devtool.utils.SysEnv;

public class DevToolCli implements DevEnvEventListener, BundleEventListener {

	public void run(String[] args) {
		if (args.length >= 1) {
			if (args[0].trim().toLowerCase().equals("check")) {
				runCheck();
			} else if (args[0].trim().toLowerCase().equals("bundles")) {
				runBundleScan();
			} else {
				printHelp();
			}
		} else {
			printHelp();
		}
	}

	private void runCheck() {
		long t = System.currentTimeMillis();
		
		SysEnv sysEnv = new SysEnv();
		
		DevEnvChecker devEnvChecker = new DevEnvChecker();
		devEnvChecker.setSysEnv(sysEnv);
		devEnvChecker.setListener(this);
		devEnvChecker.addChecks();
		devEnvChecker.runChecks();

		println("Finished in " + (System.currentTimeMillis() - t) + " ms");
		
		if (devEnvChecker.isFailed()) {
			println("\nCheck FAILED.");
			sysEnv.exitWithCode(1);
		} else {
			println("\nCheck SUCCESSFUL.");
		}
		
	}

	private void runBundleScan() {
		println("not implemented yet");
		
		BundleManager bundleManager = new BundleManager();
		bundleManager.setBundleEventListener(this);
		bundleManager.scanFileSystem();
	}

	private void println(String line) {
		System.out.println(line);
	}
	
	private void printHelp() {
		println("DevTool\n"
				+ "Usage: devtool <command>\n"
				+ "\n"
				+ "Command:\n"
				+ "\tcheck - check development environment\n"
				+ "\tbundles - scan bundles\n"
			);
	}
	
	@Override
	public void onUpdate(BaseDevEnvCheckEntry entry) {
		println(entry.getStatus().name()+" -- "+entry.getTitle());
		if (entry.getMessage() != null) {
			println("\t"+entry.getMessage());
		}
		if (entry.getDescription() != null) {
			println("\t"+entry.getDescription().replaceAll("\n", "\n\t"));
		}
		println("");
	}

	@Override
	public void onUpdate(BundleEntry entry) {
		println(entry.toString());
	}

}
