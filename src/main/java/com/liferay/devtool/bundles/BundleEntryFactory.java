package com.liferay.devtool.bundles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

public class BundleEntryFactory {
	private BundleEntry bundleEntry;
	private File rootDir;
	private File tomcatDir;

	public BundleEntry create(File bundleRootDir) {
		rootDir = bundleRootDir;

		bundleEntry = new BundleEntry();

		if (rootDir == null || !rootDir.isDirectory()) {
			return null;
		}
		
		bundleEntry.setRootDir(rootDir);
		bundleEntry.setName(rootDir.getName());

		String propertiesPath = rootDir.getAbsolutePath() + File.separator + "portal-ext.properties";

		tomcatDir = findTomcatDir(rootDir);

		bundleEntry.setTomcatDir(tomcatDir);

		String setenvPath = tomcatDir.getAbsolutePath() + File.separator + "bin" + File.separator + "setenv.bat";

		String mysqlJarPath = tomcatDir.getAbsolutePath() + File.separator + "lib" + File.separator + "ext"
				+ File.separator + "mysql.jar";
		String serverXmlPath = tomcatDir.getAbsolutePath() + File.separator + "conf" + File.separator + "server.xml";

		readPropertiesFile(propertiesPath);
		readSetenvFile(setenvPath);
		readServerXml(serverXmlPath);

		scanTempDir("osgi/state");
		scanTempDir("work");
		scanTempDir("data");
		scanTempDir("tomcat/work");
		scanTempDir("tomcat/temp");
		
		return bundleEntry;
	}

	private void readServerXml(String serverXmlPath) {

	}

	private File findTomcatDir(File bundleRootDir) {
		for (File dir : bundleRootDir.listFiles()) {
			if (dir.isDirectory() && dir.getName().startsWith("tomcat-")) {
				return dir;
			}
		}

		return null;
	}

	private void readPropertiesFile(String propertiesFilePath) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			File propertiesFile = new File(propertiesFilePath);
			if (propertiesFile.exists() && propertiesFile.isFile()) { 
				input = new FileInputStream(propertiesFilePath);
				prop.load(input);
				
				bundleEntry.setDbDriverClass(prop.getProperty("jdbc.default.driverClassName"));
				bundleEntry.setDbUrl(prop.getProperty("jdbc.default.url"));
				bundleEntry.setDbUsername(prop.getProperty("jdbc.default.username"));
				bundleEntry.setDbPassword(prop.getProperty("jdbc.default.password"));
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private void readSetenvFile(String headPath) {
		System.out.println("setenv file: " + headPath);
		try (FileInputStream inputStream = new FileInputStream(headPath)) {
			String everything = IOUtils.toString(inputStream, Charset.defaultCharset());
			processSetenvFile(everything.split("\n"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processSetenvFile(String[] lines) {
		for (String line : lines) {
			// -Xmx4000m -XX:MaxPermSize=512m
			if (line.contains("CATALINA_OPTS")) {
				System.out.println("[CATALINA_OPTS]: " + line);

				String xmx = tryParseFirstMatch(line, "-Xmx([0-9]+)m");
				String maxPerm = tryParseFirstMatch(line, "-XX:MaxPermSize=([0-9]+)m");

				if (xmx != null) {
					bundleEntry.setMemoryXmx(Integer.valueOf(xmx));
				}

				if (maxPerm != null) {
					bundleEntry.setMemoryPermSize(Integer.valueOf(maxPerm));
				}
			}
		}
	}

	private String tryParseFirstMatch(String line, String pattern) {
		String res = null;

		Pattern p = Pattern.compile(pattern);
		Matcher m = p.matcher(line);
		if (m.find()) {
			res = m.group(1);
			// System.out.println("MATCH: "+res);
		}

		return res;
	}

	private void scanTempDir(String path) {
		String absolutePath = null;
		
		String tomcatPrefix = "tomcat/";
		if (path.startsWith(tomcatPrefix)) {
			absolutePath = tomcatDir.getAbsolutePath() + File.separator + path.substring(tomcatPrefix.length());
		} else {
			absolutePath = rootDir.getAbsolutePath() + File.separator + path;
		}
		
		//System.out.println("ABS PATH="+absolutePath);

		File tempDir = new File(absolutePath);
		if (tempDir.exists() && tempDir.isDirectory()) {
			Path p = FileSystems.getDefault().getPath(absolutePath);
			long[] size = dirSize(p);
		
			TempDirEntry tempDirEntry = new TempDirEntry();
			tempDirEntry.setRelativePath(path);
			tempDirEntry.setTotalSize(size[0]);
			tempDirEntry.setNumberOfFiles((int)size[1]);
			tempDirEntry.setNumberOfDirs((int)size[2]);
			
			if (bundleEntry.getTempDirs() == null) {
				bundleEntry.setTempDirs(new ArrayList<>());
			}
			
			bundleEntry.getTempDirs().add(tempDirEntry);
		}
	}
	
	public long[] dirSize(Path path) {
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
	                System.out.println("skipped: " + file + " (" + exc + ")");
	                return FileVisitResult.CONTINUE;
	            }

	            @Override
	            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
	                if (exc != null)
	                    System.out.println("had trouble traversing: " + dir + " (" + exc + ")");
	                numberOfDirs.incrementAndGet();
	                return FileVisitResult.CONTINUE;
	            }
	        });
	    } catch (IOException e) {
	        throw new AssertionError("walkFileTree will not throw IOException if the FileVisitor does not");
	    }

	    System.out.println("PATH="+path);
	    System.out.println("\tsize="+size.get()+", files="+numberOfFiles.get()+", dirs="+numberOfDirs.get());
	    return new long[] {size.get(), numberOfFiles.get(), numberOfDirs.get()};
	}	
}
