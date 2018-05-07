package com.liferay.devtool.bundles;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FileSystemScanner {
	private int maxDepth = 5;
	private Set<String> skipDirNames;
	private Set<String> restrictedRootDirNames;
	private int statDirCount = 0;
	private List<GitRepoEntry> foundGitRepos = new ArrayList<>();
	private List<BundleEntry> foundBundles = new ArrayList<>();
	private Set<String> foundBundleDirs = new HashSet<>();

	public static void main(String[] args) {
		System.out.println("started");
		long t = System.currentTimeMillis();
		FileSystemScanner fss = new FileSystemScanner();
		fss.scan("C:\\");
		System.out.println("finished: " + (System.currentTimeMillis() - t) + " ms");
	}

	public void scan(String dirPath) {
		initSkipDirs();
		File rootDir = new File(dirPath);
		System.out.println(rootDir);
		scanDir(rootDir, 1);

		System.out.println("GIT Repos:");
		for (GitRepoEntry entry : foundGitRepos) {
			System.out.println("\t" + entry);
		}
		System.out.println("Bundle:");
		for (BundleEntry entry : foundBundles) {
			System.out.println("\t" + entry);
		}
		System.out.println("dir count: " + statDirCount);
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
					foundGitRepos.add(createGitRepoEntry(dir));
				} else if (file.getName().startsWith("tomcat-")) {
					if (!foundBundleDirs.contains(dir.getAbsolutePath())) {
						foundBundleDirs.add(dir.getAbsolutePath());
						foundBundles.add(createBundleEntry(dir));
					}
				} else {
					if (!isRestrictedDir(depth, file.getName())) {
						scanDir(file, depth + 1);
					}
				}
			} else if (file.isFile()) {
			}
		}
	}

	private GitRepoEntry createGitRepoEntry(File gitRootDir) {
		GitRepoEntryFactory gitRepoEntryFactory = new GitRepoEntryFactory();
		GitRepoEntry repo = gitRepoEntryFactory.create(gitRootDir);
		return repo;
	}

	private BundleEntry createBundleEntry(File bundleRootDir) {
		BundleEntryFactory bundleEntryFactory = new BundleEntryFactory();
		BundleEntry bundle = bundleEntryFactory.create(bundleRootDir);
		return bundle;
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

	public List<BundleEntry> getFoundBundles() {
		return foundBundles;
	}

	public List<GitRepoEntry> getFoundGitRepos() {
		return foundGitRepos;
	}
}
