package fixenv;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import fixenv.util.FileUtil;
import fixenv.util.ParamUtil;

public class FixEnvMain {

	public static void main(String[] args) throws Exception {
		long t = System.currentTimeMillis();
		
		ParamUtil param = new ParamUtil(args);
		
		String workingDir = FileUtil.getWorkingDir();
		String bundleDir = FileUtil.findBundleDirOnPath(workingDir);
		
		String bundleName = param.getBundleName();

		if (bundleDir == null) {
			bundleDir = FileUtil.findBundleRoot(workingDir, bundleName);
		}

		if (bundleDir == null) {
			System.err.println("Bundle root not found in "+workingDir);
			System.exit(1);
		}

		System.out.println("Found bundle root: "+bundleDir);
		
		checkForMultipleWebServers(bundleDir, workingDir);
		
		if (param.isHelp()) {
			showHelp();
		} else if (param.isInfo()) {
			showInfo(bundleDir);
		} else if (param.isFix()) {
			fixBundle(bundleDir);
			
			if (param.getPortPrefix() != null) {
				fixPort(bundleDir, param.getPortPrefix());
			}
		} else if (param.isPurge()) {
			purgeBundle(bundleDir, param.isAll());
		} else if (param.isCleanDb()) {
			cleanDb(bundleDir);
		} else if (param.isSaveDump()) {
			saveDump(bundleDir, param.getDumpName());
		} else if (param.isUpgrade()) {
			setupUpgrade(bundleDir);
		} else if (param.isReset()) {
			fixBundle(bundleDir);
			purgeBundle(bundleDir, param.isAll());
			cleanDb(bundleDir);
		}
		
		System.out.println("\nFinished in: "+(System.currentTimeMillis() - t)+" ms");
	}

	private static void checkForMultipleWebServers(String bundleDir, String workingDir) {
		List<String> webServersInfo = FileUtil.getWebServersInfo(bundleDir, workingDir);
		
		for (String info : webServersInfo) {
			System.err.println(info);
		}
	}

	private static void purgeBundle(String bundleDir, boolean all) {
		PurgeEnv purgeEnv = new PurgeEnv();
		purgeEnv.setBundleRoot(bundleDir);
		purgeEnv.setAll(all);
		purgeEnv.execute();
	}

	private static void cleanDb(String bundleDir) {
		CleanDb cleanDb = new CleanDb();
		cleanDb.setBundleRoot(bundleDir);
		cleanDb.execute();
	}

	private static void saveDump(String bundleDir, String dumpName) {
		SaveDbDump saveDbDump = new SaveDbDump();
		saveDbDump.setBundleRoot(bundleDir);
		saveDbDump.setDumpName(dumpName);
		saveDbDump.execute();
	}
	
	private static void fixBundle(String bundleRootDir) {
		FixSetenv fixSetenv = new FixSetenv();
		fixSetenv.setBundleRoot(bundleRootDir);
		fixSetenv.execute();

		FixTimeOut fixTimeOut = new FixTimeOut();
		fixTimeOut.setBundleRoot(bundleRootDir);
		fixTimeOut.execute();
		
		FixArchiveTimes fixWarTimes = new FixArchiveTimes();
		fixWarTimes.setBundleRoot(bundleRootDir);
		fixWarTimes.execute();
	}
	
	private static void fixPort(String bundleRootDir, String portPrefix) {
		FixPort fixPort = new FixPort();
		fixPort.setBundleRoot(bundleRootDir);
		fixPort.setPortPrefix(portPrefix);
		fixPort.execute();
	}
	
	private static void showHelp() throws Exception {
		String current = new java.io.File( "." ).getCanonicalPath();
		System.out.println("Current dir: "+current);
		
		String currentDir = System.getProperty("user.dir");
		System.out.println("Current dir using System: " +currentDir);
		
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Current relative path: " + s);
		
		String srcFile = FixEnvMain.class
				.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.getFile();
		
		System.out.println("Current relative path: " + srcFile);
	}
	
	private static void showInfo(String bundleRootDir) throws Exception {
		FixPort fixPort = new FixPort();
		fixPort.setBundleRoot(bundleRootDir);
		fixPort.info();
		
		FixSetenv fixSetenv = new FixSetenv();
		fixSetenv.setBundleRoot(bundleRootDir);
		fixSetenv.info();

		FixTimeOut fixTimeOut = new FixTimeOut();
		fixTimeOut.setBundleRoot(bundleRootDir);
		fixTimeOut.info();
	}
	
	private static void setupUpgrade(String bundleDir) throws Exception {
		UpgradeSetup upgradeSetup = new UpgradeSetup();
		upgradeSetup.setBundleRoot(bundleDir);
		upgradeSetup.initialize();
	}
	
}
