package com.liferay.devtool.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class JarManifestReader {
	private Set<String> watchKeys = new HashSet<>();
	
	public void addKeys(String... keys) {
		watchKeys.addAll(Arrays.asList(keys));
	}

	public Map<String, String> readJarFile(File file) {
		Map<String,String> res = null;
		
		ZipFile zipFile = null;
		try {
			zipFile = new ZipFile(file);
			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			boolean finished = false;
			while (entries.hasMoreElements() && !finished) {
				ZipEntry entry = entries.nextElement();

				if (entry.getName().toUpperCase().endsWith("MANIFEST.MF")) {
					ManifestDataReader manifestDataReader = new ManifestDataReader();
					manifestDataReader.setWatchKeys(watchKeys);
					
					readLinesOfInputStream(zipFile.getInputStream(entry), manifestDataReader);
					
					res = manifestDataReader.getData();
					finished = true;
				}
			}
		} catch (ZipException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (zipFile != null) {
					zipFile.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
        }
		
		return res;
	}

	private void readLinesOfInputStream(InputStream stream, ManifestDataReader manifestDataReader) {
		try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
			while (reader.ready()) {
				String line = reader.readLine();
				manifestDataReader.processLine(line);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
