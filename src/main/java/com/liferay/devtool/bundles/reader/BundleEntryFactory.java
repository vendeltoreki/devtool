package com.liferay.devtool.bundles.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.WebServerType;
import com.liferay.devtool.utils.TempDirUtil;

public class BundleEntryFactory {
	private BundleEntry bundleEntry;
	private File rootDir;
	private File webServerDir;
	private WebServerType webServerType;
	private int webServerCount = 0;
	
	public BundleEntry create(File bundleRootDir) {
		rootDir = bundleRootDir;

		bundleEntry = new BundleEntry();

		if (rootDir == null || !rootDir.isDirectory()) {
			return null;
		}
		
		bundleEntry.setRootDir(rootDir);
		bundleEntry.setName(rootDir.getName());

		String propertiesPath = rootDir.getAbsolutePath() + File.separator + "portal-ext.properties";
		readPropertiesFile(propertiesPath);

		findWebServerDir(rootDir);

		if (webServerDir != null) {
			bundleEntry.setWebServerDir(webServerDir);
			bundleEntry.setWebServerType(webServerType);
			bundleEntry.setMultipleWebServers(webServerCount > 1);
	
			if (webServerType == WebServerType.TOMCAT) {
				String setenvPath = webServerDir.getAbsolutePath() + File.separator + "bin" + File.separator + "setenv.bat";
		
				String mysqlJarPath = webServerDir.getAbsolutePath() + File.separator + "lib" + File.separator + "ext"
						+ File.separator + "mysql.jar";
				String serverXmlPath = webServerDir.getAbsolutePath() + File.separator + "conf" + File.separator + "server.xml";
	
				readSetenvFile(setenvPath);
				readServerXml(serverXmlPath);
			}
		}
		
		scanTempDirs();
		
		return bundleEntry;
	}

	private void scanTempDirs() {
		TempDirUtil tempDirUtil = new TempDirUtil();
		tempDirUtil.setBundlePath(rootDir.getAbsolutePath());
		tempDirUtil.setWebServerPath(webServerDir.getAbsolutePath());
		tempDirUtil.scanTempDirs();
		
		bundleEntry.setTempDirs(tempDirUtil.getTempDirEntries());
	}

	private void readServerXml(String serverXmlPath) {

	}

	private void findWebServerDir(File bundleRootDir) {
		webServerCount = 0;
		for (File dir : bundleRootDir.listFiles()) {
			if (dir.isDirectory() && dir.getName().startsWith("tomcat-")) {
				webServerDir = dir;
				webServerType = WebServerType.TOMCAT;
				++webServerCount;
			} else if (dir.isDirectory() && dir.getName().startsWith("wildfly-")) {
				webServerDir = dir;
				webServerType = WebServerType.WILDFLY;
				++webServerCount;
			} else if (dir.isDirectory() && dir.getName().startsWith("jboss-")) {
				webServerDir = dir;
				webServerType = WebServerType.JBOSS;
				++webServerCount;
			}
		}
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
		//System.out.println("setenv file: " + headPath);
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
				//System.out.println("[CATALINA_OPTS]: " + line);

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


}
