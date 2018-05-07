package com.liferay.devtool.bundles;

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

public class BundleEntryFactory {
	private BundleEntry bundleEntry;

	public BundleEntry create(File bundleRootDir) {
		bundleEntry = new BundleEntry();

		if (bundleRootDir == null || !bundleRootDir.isDirectory()) {
			return null;
		}

		bundleEntry.setRootDir(bundleRootDir);
		bundleEntry.setName(bundleRootDir.getName());

		String propertiesPath = bundleRootDir.getAbsolutePath() + File.separator + "portal-ext.properties";

		File tomcatDir = findTomcatDir(bundleRootDir);

		bundleEntry.setTomcatDir(tomcatDir);

		String setenvPath = tomcatDir.getAbsolutePath() + File.separator + "bin" + File.separator + "setenv.bat";

		String mysqlJarPath = tomcatDir.getAbsolutePath() + File.separator + "lib" + File.separator + "ext"
				+ File.separator + "mysql.jar";
		String serverXmlPath = tomcatDir.getAbsolutePath() + File.separator + "conf" + File.separator + "server.xml";

		readPropertiesFile(propertiesPath);
		readSetenvFile(setenvPath);
		readServerXml(serverXmlPath);

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
			input = new FileInputStream(propertiesFilePath);
			prop.load(input);

			bundleEntry.setDbDriverClass(prop.getProperty("jdbc.default.driverClassName"));
			bundleEntry.setDbUrl(prop.getProperty("jdbc.default.url"));
			bundleEntry.setDbUsername(prop.getProperty("jdbc.default.username"));
			bundleEntry.setDbPassword(prop.getProperty("jdbc.default.password"));

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

}
