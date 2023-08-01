package fixenv.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import fixenv.FixEnvMain;

public class FileUtil {

	public static String resolvePath(String path) {
		String res = replaceVar(path, "[TOMCAT]");
		
		return res;
	}

	private static String replaceVar(String path, String varName) {
		int i = path.indexOf(varName);
		
		if (i >- 1) {
			String parentPath = path.substring(0, i);
			File parentDir = new File(parentPath);
			
			if (parentDir.exists() && parentDir.isDirectory()) {
				String foundName = null;
				for (File file : parentDir.listFiles()) {
					if (file.isDirectory() && file.getName().toLowerCase().startsWith("tomcat")) {
						foundName = file.getName();
						break;
					}
				}
				
				if (foundName != null) {
					return parentPath + foundName + path.substring(i+varName.length());
				}
			}
		}
		
		return path;
	}

	public static String findBundleRoot(String bundlesParentDir, String name) {
		String rootDir = null;
		
		if (name != null && name.trim().length() > 0) {
			rootDir = checkDir(bundlesParentDir, "bundles_" + name);
			
			if (rootDir == null && name.contains("-")) {
				rootDir = checkDir(bundlesParentDir, "bundles_" + name.replace('-', '_'));
			}
		} else {
			rootDir = checkDir(bundlesParentDir, "bundles");
		}

		return rootDir;
	}
	
	private static String checkDir(String bundlesParentDir, String dirName) {
		String possibleRootDir = bundlesParentDir + File.separator + dirName;

		if (new File(possibleRootDir).exists()) {
			return possibleRootDir;
		}
		
		return null;
		
	}
	
	public static String getWorkingDir() {
		String currentDir = System.getProperty("user.dir");
	
		return currentDir;
	}
	
	public static String getJarPath() {
		String srcFile = FixEnvMain.class
				.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.getFile();
		
		if (srcFile.startsWith("/")) {
			srcFile = srcFile.substring(1);
		}
		
		if (srcFile.contains("/")) {
			srcFile = srcFile.replace("/", "\\");
		}
		
		return srcFile;
	}

	public static String getJarParentDirPath() {
		return StringUtils.splitLast(getJarPath(), "\\")[0];
	}
	
	public static String findBundleDirOnPath(String workingDir) {
		File currentDir = new File(workingDir);

		while (currentDir != null && !isBundleDir(currentDir) && !isTomcatDir(currentDir)) {
			currentDir = currentDir.getParentFile();
		}

		if (currentDir != null) {
			if (isTomcatDir(currentDir)) {
				return currentDir.getParentFile().getAbsolutePath();
			} else if (isBundleDir(currentDir)) {
				return currentDir.getAbsolutePath();
			}
		}
		
		return null;
	}

	private static boolean isBundleDir(File currentDir) {
		if (currentDir == null) {
			return false;
		}
		
		if (!currentDir.isDirectory()) {
			return false;
		}
		
		File[] files = currentDir.listFiles();
		
		if (files.length > 100) {
			return false;
		}
		
		for (File file : files) {
			if (isTomcatDir(file)) {
				return true;
			}
		}
		
		return false;
	}

	private static boolean isTomcatDir(File file) {
		if (file == null) {
			return false;
		}
		
		if (!file.isDirectory()) {
			return false;
		}
		
		return file.getName().startsWith("tomcat");
	}

	public static List<File> getWebServerDirs(String bundleDir) {
		List<File> res = new ArrayList<>();
		
		if (bundleDir == null) {
			return res;
		}
		
		File bundleDirFile = new File(bundleDir);
		
		if (bundleDirFile == null || !bundleDirFile.exists() || !bundleDirFile.isDirectory()) {
			return res;
		}
		
		File[] files = bundleDirFile.listFiles();
		
		for (File file : files) {
			if (isTomcatDir(file)) {
				res.add(file);
			}
		}		
		
		return res;
	}

	public static List<String> getWebServersInfo(String bundleDir, String workingDir) {
		List<String> res = new ArrayList<>();

		List<File> webServerDirs = getWebServerDirs(bundleDir);
		
		if (webServerDirs.size() == 0) {
			res.add("--------------------------------------------------------------");
			res.add("-- WARNING! No web server directories found in \""+bundleDir+"\"!");
			res.add("--------------------------------------------------------------");
		} else if (webServerDirs.size() > 1) {
			res.add("--------------------------------------------------------------");
			res.add("-- WARNING! Multiple web server directories found in \""+bundleDir+"\"!");
			res.add("");
			
			if (workingDir != null) {
				long latestTs = 0;
				for (File webServerDir : webServerDirs) {
					if (webServerDir.lastModified() > latestTs) {
						latestTs = webServerDir.lastModified();
					}
				}

				for (File webServerDir : webServerDirs) {
					res.add("\t- "+webServerDir.getName()+" - "+StringUtils.formatTimestamp(webServerDir.lastModified()) + (webServerDir.lastModified() == latestTs ? " (latest build)" : ""));
				}
			}
			res.add("--------------------------------------------------------------");
		}
		
		return res;
	}
}
