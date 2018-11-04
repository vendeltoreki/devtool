package com.liferay.devtool.bundles;

public class TempDirEntry {
	private String relativePath;
	private int numberOfFiles;
	private int numberOfDirs;
	private long totalSize;
	
	public TempDirEntry() {
	}

	public TempDirEntry(TempDirEntry original) {
		this.relativePath = original.relativePath;
		this.numberOfFiles = original.numberOfFiles;
		this.numberOfDirs = original.numberOfDirs;
		this.totalSize = original.totalSize;
	}
	
	public String getRelativePath() {
		return relativePath;
	}

	public void setRelativePath(String relativePath) {
		this.relativePath = relativePath;
	}

	public int getNumberOfFiles() {
		return numberOfFiles;
	}

	public void setNumberOfFiles(int numberOfFiles) {
		this.numberOfFiles = numberOfFiles;
	}

	public int getNumberOfDirs() {
		return numberOfDirs;
	}

	public void setNumberOfDirs(int numberOfDirs) {
		this.numberOfDirs = numberOfDirs;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}
}
