package com.liferay.devtool.devenv.checks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import com.liferay.devtool.devenv.CheckStatus;

public class JarManifestCheck extends BaseDevEnvCheckEntry {
	private String searchDirPath;
	private String manifestKey;
	private String expectedValue;

	public JarManifestCheck(String searchDirPath, String manifestKey, String expectedValue) {
		super();
		this.searchDirPath = searchDirPath;
		this.manifestKey = manifestKey;
		this.expectedValue = expectedValue;
		title = "Check mainfest in JAR file: " + searchDirPath + ", key=" + manifestKey + ", expected=" + expectedValue;
	}

	@Override
	public void runCheck() {
		String actualDirPath = context.replaceEnvVarsInPath(searchDirPath);

		File dir = sysEnv.createFile(actualDirPath);

		checkDirectory(dir);

		if (!isFailed()) {
			recursiveSearchForFiles(dir);
		}

		if (status == CheckStatus.UNKNOWN) {
			fail("Matching manifest could not be found.");
		}
	}

	private void recursiveSearchForFiles(File dir) {
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) {
				recursiveSearchForFiles(file);
			} else if (file.isFile()) {
				if (file.getName().toLowerCase().endsWith(".jar")) {
					readJarFile(file);
				}
			}
		}
	}

	private void readJarFile(File file) {
		ZipFile zipFile;
		try {
			zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();

				if (entry.getName().toUpperCase().endsWith("MANIFEST.MF")) {
					InputStream stream = zipFile.getInputStream(entry);
					BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
					while (reader.ready()) {
						String line = reader.readLine();
						if (line.startsWith(manifestKey)) {
							String[] split = line.split(":");
							if (split.length >= 2) {
								if (split[1].trim().equals(expectedValue)) {
									success("Key \"" + manifestKey + "\" matches \"" + expectedValue + "\"");
								} else {
									fail("Manifest value not matched: \"" + split[1].trim() + "\", expected=\""
											+ expectedValue + "\"");
								}
							}
						}
					}
				}
			}
		} catch (ZipException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void checkDirectory(File dir) {
		if (!dir.exists()) {
			fail("Directory \"" + dir.getAbsolutePath() + "\" does not exist!");
		} else if (!dir.isDirectory()) {
			fail("File \"" + dir.getAbsolutePath() + "\" is not a directory!");
		}
	}
}
