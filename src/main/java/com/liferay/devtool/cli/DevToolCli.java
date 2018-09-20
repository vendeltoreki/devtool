package com.liferay.devtool.cli;

import java.util.List;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.BundleEventListener;
import com.liferay.devtool.bundles.BundleManager;
import com.liferay.devtool.bundles.GitRepoEntry;
import com.liferay.devtool.bundles.TempDirEntry;
import com.liferay.devtool.devenv.DevEnvChecker;
import com.liferay.devtool.devenv.DevEnvEventListener;
import com.liferay.devtool.devenv.checks.BaseDevEnvCheckEntry;
import com.liferay.devtool.utils.StringUtils;
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
		long t = System.currentTimeMillis();
		SysEnv sysEnv = new SysEnv();

		BundleManager bundleManager = new BundleManager();
		bundleManager.setSysEnv(sysEnv);
		bundleManager.setBundleEventListener(this);
		bundleManager.scanFileSystem();
		
		println("");
		println("Reading details..");
		
		bundleManager.readDetails();
		
		println("");
		printBundleDetails(bundleManager.getEntries());

		println("Finished in " + (System.currentTimeMillis() - t) + " ms");
	}

	private void printBundleDetails(List<BundleEntry> entries) {
		if (entries == null) {
			return;
		}
		
		println("Found bundles ("+entries.size()+"): ");
		
		for (BundleEntry bundle : entries) {
			println("------------------------------------------------------");
			if (bundle.getRunningProcess() != null) {
				println(">> RUNNING - "+bundle.getName());
			} else {
				println("-- "+bundle.getName());
			}
			println("");
			
			println("root dir: "+bundle.getRootDir().getAbsolutePath());
			println("version: "+bundle.getPortalVersion() +", patches: "+bundle.getPortalPatches());
			println("memory: Xmx="+bundle.getMemoryXmx() +", perm="+bundle.getMemoryPermSize());
			println("");
			println("DB driver: "+bundle.getDbDriverClass());
			println("DB URL: "+bundle.getDbUrl());
			println("DB User: "+bundle.getDbUsername()+", pass="+bundle.getDbPassword());
			
			if (bundle.getRunningProcess() != null) {
				println("");
				println("Running process");
				println("\tpid: "+bundle.getRunningProcess().getPid());
				println("\texec name: "+bundle.getRunningProcess().getExecName());
				println("\tports: " + StringUtils.join(bundle.getRunningProcess().getListeningPorts(), ","));
				println("\tcommand: "+bundle.getRunningProcess().getCommandLine());
			}
			
			if (bundle.getDbSchemaEntry() != null) {
				println("");
				println("DB schema");
				println("\tname: "+bundle.getDbSchemaEntry().getSchemaName());
				println("\tversion: "+bundle.getDbSchemaEntry().getSchemaVersion());
				println("\ttable count: "+bundle.getDbSchemaEntry().getTableCount());
				if (bundle.getDbSchemaEntry().hasDeployedApps()) {
					println("\tDeployed apps: "+StringUtils.join(bundle.getDbSchemaEntry().getDeployedApps(),", "));
				}
				if (bundle.getDbSchemaEntry().getSizeInMb() != null) {
					println("\tSize in MB: "+bundle.getDbSchemaEntry().getSizeInMb());
				}
			}

			if (bundle.getTempDirs() != null && !bundle.getTempDirs().isEmpty()) {
				println("");
				println("Temp dirs");
				for (TempDirEntry tempDir : bundle.getTempDirs()) {
					println("\t"+tempDir.getRelativePath()+": size="+tempDir.getTotalSize()+", dirs="+tempDir.getNumberOfDirs()+", files="+tempDir.getNumberOfFiles());
				}
			}
			
			if (bundle.getGitRepos() != null && !bundle.getGitRepos().isEmpty()) {
				println("");
				println("GIT repos");
				for (GitRepoEntry gitRepo : bundle.getGitRepos()) {
					println("\t"+gitRepo);
				}
			}
			
			println("");
		}
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
		if (!detailsScanned(entry)) {
			println(entry.getRootDirPath());
		}
	}

	private boolean detailsScanned(BundleEntry entry) {
		return entry.getRootDir() != null;
	}

}
