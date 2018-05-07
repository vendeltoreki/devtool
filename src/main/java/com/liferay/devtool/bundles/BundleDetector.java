package com.liferay.devtool.bundles;

import java.util.ArrayList;
import java.util.List;

public class BundleDetector {
	private FileSystemScanner scanner;

	public void scan() {
		scanner = new FileSystemScanner();
		scanner.scan("C:\\");
	}

	public List<BundleEntry> getEntries() {
		if (scanner != null && scanner.getFoundBundles() != null) {
			return scanner.getFoundBundles();
		} else {
			return new ArrayList<>();
		}
	}
}
