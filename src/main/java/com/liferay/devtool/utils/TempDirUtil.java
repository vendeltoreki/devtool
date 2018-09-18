package com.liferay.devtool.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.TempDirEntry;

public class TempDirUtil {
	private String bundlePath;
	private String webServerPath;
	private List<TempDirEntry> tempDirEntries;
	
	private static final String[] DIR_NAMES = new String[] {
		"osgi/state",
		"work", 
		//"data",
		"tomcat/work",
		"tomcat/temp"
	};
	
	private static final String TOMCAT_PREFIX = "tomcat/";
	
	public void scanTempDirs() {
		if (tempDirEntries == null) {
			tempDirEntries = new ArrayList<>();
		}

		for (String dirName : DIR_NAMES) {
			TempDirEntry tempDirEntry = scanTempDir(dirName);
			if (tempDirEntry != null) {
				tempDirEntries.add(tempDirEntry);
			}
		}
		
		if (tempDirEntries.isEmpty()) {
			tempDirEntries = null;
		}
	}
	
	public void clean() {
		long t = System.currentTimeMillis();
		
		for (String dirName : DIR_NAMES) {
			String absolutePath = findAbsolutePath(dirName);
			try {
				deleteContentsOfDirectory(absolutePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		System.out.println("TOTAL TIME: "+(System.currentTimeMillis() - t));
	}

	private TempDirEntry scanTempDir(String path) {
		String absolutePath = findAbsolutePath(path);

		File tempDir = new File(absolutePath);
		if (tempDir.exists() && tempDir.isDirectory()) {
			Path p = FileSystems.getDefault().getPath(absolutePath);
			long[] size = dirSize(p);
		
			TempDirEntry tempDirEntry = new TempDirEntry();
			tempDirEntry.setRelativePath(path);
			tempDirEntry.setTotalSize(size[0]);
			tempDirEntry.setNumberOfFiles((int)size[1]);
			tempDirEntry.setNumberOfDirs((int)size[2]);
			
			return tempDirEntry;
		}
		
		return null;
	}

	private String findAbsolutePath(String path) {
		String absolutePath = null;
		
		if (path.startsWith(TOMCAT_PREFIX)) {
			absolutePath = webServerPath + File.separator + path.substring(TOMCAT_PREFIX.length());
		} else {
			absolutePath = bundlePath + File.separator + path;
		}

		return absolutePath;
	}
	
	private long[] dirSize(Path path) {
	    final AtomicLong size = new AtomicLong(0);
	    final AtomicLong numberOfFiles = new AtomicLong(0);
	    final AtomicLong numberOfDirs = new AtomicLong(0);

	    try {
	        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
	            @Override
	            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
	                numberOfFiles.incrementAndGet();
	                size.addAndGet(attrs.size());
	                return FileVisitResult.CONTINUE;
	            }

	            @Override
	            public FileVisitResult visitFileFailed(Path file, IOException exc) {
	                //System.out.println("skipped: " + file + " (" + exc + ")");
	                return FileVisitResult.CONTINUE;
	            }

	            @Override
	            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
	                //if (exc != null) {
	                    //System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
	            	//}
	                numberOfDirs.incrementAndGet();
	                return FileVisitResult.CONTINUE;
	            }
	        });
	    } catch (IOException e) {
	        throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
	    }

	    return new long[] {size.get(), numberOfFiles.get(), numberOfDirs.get()};
	}

	public static void deleteContentsOfDirectory(String directoryPath) throws IOException {
		Path mainDirPath = Paths.get(directoryPath);

	    if (Files.exists(mainDirPath)) {
	        Files.walkFileTree(mainDirPath, new SimpleFileVisitor<Path>() {
	            @Override
	            public FileVisitResult visitFile(Path path, BasicFileAttributes basicFileAttributes) throws IOException {
	            	//System.out.println("delete file: "+path);
	                Files.delete(path);
	            	
	                return FileVisitResult.CONTINUE;
	            }

	            @Override
	            public FileVisitResult postVisitDirectory(Path directory, IOException ioException) throws IOException {
	                if (!mainDirPath.equals(directory)) {
		            	//System.out.println("delete dir: "+directory);
	                	Files.delete(directory);
	                }
	                return FileVisitResult.CONTINUE;
	            }
	        });
	    }
	}	
	
	public String getBundlePath() {
		return bundlePath;
	}

	public void setBundlePath(String bundlePath) {
		this.bundlePath = bundlePath;
	}

	public String getWebServerPath() {
		return webServerPath;
	}

	public void setWebServerPath(String webServerPath) {
		this.webServerPath = webServerPath;
	}

	public List<TempDirEntry> getTempDirEntries() {
		return tempDirEntries;
	}

	public void extractPathsFromBundleEntry(BundleEntry entry) {
		setBundlePath(entry.getRootDir().getAbsolutePath());
		setWebServerPath(entry.getWebServerDir().getAbsolutePath());
	}

}
