package com.liferay.devtool.bundles;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.filechooser.FileSystemView;

import com.liferay.devtool.context.DevToolContext;
import com.liferay.devtool.utils.ConfigStorage;
import com.liferay.devtool.utils.StringUtils;

public class EventBasedFileSystemScanner {
	private static final String PROPS_ENABLED_SCAN_DIR = "enabled.scan.dir";
	private static final String PROPS_MAXDEPTH = "scan.max.depth";
	private static final String PROPS_SKIP_ROOT_DIR_NAMES = "skip.root.dirs";
	private static final String PROPS_SKIP_DIR_NAMES = "skip.dirs";
	private int maxDepth = 5;
	private Set<String> skipDirNames;
	private Set<String> restrictedRootDirNames;
	private Set<String> foundBundleDirs = new HashSet<>();
	private FileSystemScanEventListener fileSystemScanEventListener;
	private DevToolContext context;
	
	public static void main(String[] args) {
		System.out.println("started");
		long t = System.currentTimeMillis();
		EventBasedFileSystemScanner fss = new EventBasedFileSystemScanner();
		fss.setFileSystemScanEventListener(new FileSystemScanEventListener() {
			
			@Override
			public void onFoundGitRepo(String absolutePath) {
				System.out.println("found GIT: "+absolutePath);
			}
			
			@Override
			public void onFoundBundle(String absolutePath) {
				System.out.println("found Bundle: "+absolutePath);
			}
		});
		fss.scanLocalDisks();
		System.out.println("finished: " + (System.currentTimeMillis() - t) + " ms");
	}

	public EventBasedFileSystemScanner() {
		super();
		initSkipDirs();
	}

	public void scanByConfig() {
		ConfigStorage configStorage = new ConfigStorage("bundle_scan","Stores bundle scanning options");
		configStorage.setContext(context);
		boolean exists = configStorage.load();
		
		if (exists) {
			loadConfigValues(configStorage);

			for (String scanDirPath : configStorage.getList(PROPS_ENABLED_SCAN_DIR)) {
				scan(scanDirPath);
			}
		} else {
			scanLocalDisks();
			createConfig(configStorage);
		}
	}
	
	private void loadConfigValues(ConfigStorage configStorage) {
		Integer maxDepthValue = StringUtils.tryParseInt(configStorage.getValue(PROPS_MAXDEPTH));
		if (maxDepthValue != null) {
			maxDepth = maxDepthValue;
		}
		
		restrictedRootDirNames = StringUtils.createLowerStringSet(configStorage.getValue(PROPS_SKIP_ROOT_DIR_NAMES), ";");
		skipDirNames = StringUtils.createLowerStringSet(configStorage.getValue(PROPS_SKIP_ROOT_DIR_NAMES), ";");
	}

	private void createConfig(ConfigStorage configStorage) {
		Set<String> rootDirs = extractRootDirsFromFoundBundles();
		
		for (String rootDir : rootDirs) {
			configStorage.addToList(PROPS_ENABLED_SCAN_DIR, rootDir);
		}

		configStorage.addValue(PROPS_MAXDEPTH, String.valueOf(maxDepth));
		configStorage.addValue(PROPS_SKIP_ROOT_DIR_NAMES, StringUtils.join(restrictedRootDirNames, ";"));
		configStorage.addValue(PROPS_SKIP_DIR_NAMES, StringUtils.join(skipDirNames, ";"));
		
		configStorage.save();
	}

	private Set<String> extractRootDirsFromFoundBundles() {
		Set<String> rootDirs = new HashSet<>();
		
		for (String foundBundleDirPath : foundBundleDirs) {
			File foundBundleDir = new File(foundBundleDirPath);
			
			if (foundBundleDir.exists()) {
				File parent = foundBundleDir.getParentFile();
				if (parent != null) {
					parent = findParentIfThisIsProbablyTicketNumber(parent);
					
					String parentPath = parent.getAbsolutePath() + File.separator;
					if (!rootDirs.contains(parentPath)) {
						rootDirs.add(parentPath);
					}
				}
			}
		}
		
		rootDirs = removeRedundantSubDirectories(rootDirs);
		return rootDirs;
	}

	private File findParentIfThisIsProbablyTicketNumber(File file) {
		if (isProbablyTicketNumber(file.getName())) {
			return file.getParentFile() != null ? file.getParentFile() : file;
		} else {
			return file;
		}
	}

	private boolean isProbablyTicketNumber(String name) {
		return name.matches("[A-Z]{3,20}\\-[0-9]{1,4}");
	}

	private Set<String> removeRedundantSubDirectories(Set<String> rootDirs) {
		List<String> orderedList = new ArrayList<>(rootDirs);
		
		Collections.sort(orderedList);

		boolean changed = false;
		do {
			changed = false;
			for (int i = orderedList.size()-1; i>=1; --i) {
				String actual = orderedList.get(i);
				String previous = orderedList.get(i-1);
				
				if (actual.startsWith(previous)) {
					orderedList.remove(i);
					changed = true;
				}
			}
		} while (changed);

		return new HashSet<>(orderedList);
	}

	public void scanLocalDisks() {
		FileSystemView fsv = FileSystemView.getFileSystemView();
		for(File root : File.listRoots()) {
			String rootDescription = fsv.getSystemTypeDescription(root);
		    if (rootDescription.equals("Local Disk")) {
		    	System.out.println("scanning: "+root.getAbsolutePath());
		    	scan(root.getAbsolutePath());
		    }
		}
	}
	
	public void scan(String dirPath) {
		File rootDir = new File(dirPath);
		scanDir(rootDir, 1);
	}

	private void initSkipDirs() {
		skipDirNames = StringUtils.createLowerStringSet(new String[] {
				".gradle", "caches", "modules", "work", "osgi", "AppData",
				"Application Data", "Microsoft", "Package Cache" });

		restrictedRootDirNames = StringUtils.createLowerStringSet(new String[] {
				"Apps", "Drivers", "Intel", "langpacks", "PerfLogs", "Program Files",
				"Program Files (x86)", "ProgramData", "Recovery", "tmp", "Windows", "$Recycle.Bin",
				"System Volume Information" });
	}

	private void scanDir(File dir, int depth) {
		if (dir == null) {
			return;
		}

		File[] list = dir.listFiles();

		if (list == null || list.length == 0 || list.length > 100) {
			return;
		}

		for (File file : list) {
			if (file.isDirectory()) {
				if (file.getName().equals(".git")) {
					foundGitRepo(dir.getAbsolutePath());
				} else if (file.getName().startsWith("tomcat-")) {
					if (!foundBundleDirs.contains(dir.getAbsolutePath())) {
						if (checkIfLiferayDeployedOnTomcat(file)) {
							foundBundleDirs.add(dir.getAbsolutePath());
							foundBundle(dir.getAbsolutePath());
						}
					}
				} else if (file.getName().startsWith("wildfly-")) {
					if (!foundBundleDirs.contains(dir.getAbsolutePath())) {
						if (checkIfLiferayDeployedOnWildFly(file)) {
							foundBundleDirs.add(dir.getAbsolutePath());
							foundBundle(dir.getAbsolutePath());
						}
					}
				} else if (file.getName().startsWith("jboss-")) {
					if (!foundBundleDirs.contains(dir.getAbsolutePath())) {
						if (checkIfLiferayDeployedOnJBoss(file)) {
							foundBundleDirs.add(dir.getAbsolutePath());
							foundBundle(dir.getAbsolutePath());
						}
					}
				} else {
					if (!isRestrictedDir(depth, file.getName())) {
						scanDir(file, depth + 1);
					}
				}
			}
		}
	}

	private boolean checkIfLiferayDeployedOnTomcat(File webappServerRootDir) {
		return checkIfFileExists(webappServerRootDir.getAbsolutePath()+"\\webapps\\ROOT\\WEB-INF\\lib\\portal-impl.jar");
	}

	private boolean checkIfLiferayDeployedOnWildFly(File webappServerRootDir) {
		return checkIfFileExists(webappServerRootDir.getAbsolutePath()+"\\modules\\com\\liferay\\portal\\main\\portal-kernel.jar");
	}

	private boolean checkIfLiferayDeployedOnJBoss(File webappServerRootDir) {
		return checkIfFileExists(webappServerRootDir.getAbsolutePath()+"\\standalone\\deployments\\marketplace-portlet.war");
	}	
	
	private boolean checkIfFileExists(String filePath) {
		File file = new File(filePath);
		if (file.exists() && file.isFile() && file.length() > 0) {
			return true;
		}
		return false;
	}

	private boolean isRestrictedDir(int depth, String name) {
		if (depth > maxDepth) {
			return true;
		}

		if (skipDirNames.contains(name.toLowerCase())) {
			return true;
		}

		if (depth == 1 && restrictedRootDirNames.contains(name.toLowerCase())) {
			return true;
		}

		if (name.startsWith(".") && !name.equals(".git")) {
			return true;
		}

		return false;
	}

	private void foundBundle(String path) {
		if (fileSystemScanEventListener != null) {
			fileSystemScanEventListener.onFoundBundle(path);
		}
	}

	private void foundGitRepo(String path) {
		if (fileSystemScanEventListener != null) {
			fileSystemScanEventListener.onFoundGitRepo(path);
		}
	}
	
	public void setContext(DevToolContext context) {
		this.context = context;
	}

	public void setFileSystemScanEventListener(FileSystemScanEventListener fileSystemScanEventListener) {
		this.fileSystemScanEventListener = fileSystemScanEventListener;
	}
}
