package com.liferay.devtool.bundles;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import javax.swing.filechooser.FileSystemView;

public class EventBasedFileSystemScanner {
	private int maxDepth = 5;
	private Set<String> skipDirNames;
	private Set<String> restrictedRootDirNames;
	private int statDirCount = 0;
	private Set<String> foundBundleDirs = new HashSet<>();
	private FileSystemScanEventListener fileSystemScanEventListener;
	
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
		initSkipDirs();
		File rootDir = new File(dirPath);
		//System.out.println(rootDir);
		scanDir(rootDir, 1);

		//System.out.println("dir count: " + statDirCount);
	}

	private void initSkipDirs() {
		String[] dirNames = new String[] { ".gradle", "caches", "modules", "work", "osgi", "AppData",
				"Application Data", "Microsoft", "Package Cache" };

		skipDirNames = new HashSet<>();
		for (String dirName : dirNames) {
			skipDirNames.add(dirName.trim().toLowerCase());
		}

		String[] rootNames = new String[] { "Apps", "Drivers", "Intel", "langpacks", "PerfLogs", "Program Files",
				"Program Files (x86)", "ProgramData", "Recovery", "tmp", "Windows", "$Recycle.Bin",
				"System Volume Information" };

		restrictedRootDirNames = new HashSet<>();
		for (String dirName : rootNames) {
			restrictedRootDirNames.add(dirName.trim().toLowerCase());
		}
	}

	private void scanDir(File dir, int depth) {
		++statDirCount;

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
	
	public void setFileSystemScanEventListener(FileSystemScanEventListener fileSystemScanEventListener) {
		this.fileSystemScanEventListener = fileSystemScanEventListener;
	}
}
