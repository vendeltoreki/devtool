package com.liferay.devtool.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class LogReader {
	private String timeZone = "GMT";
	private Date latestStartup = null;
	
	public void readFile(String filePath) {
		try {
			final List<String> lines = Files.readAllLines(Paths.get(filePath));
			for (String line : lines) {
				if (line.contains("org.apache.catalina.startup.Catalina.start Server startup in")) {
					System.out.println(line);
					int pos = line.indexOf(" INFO");
					if (pos > -1) {
						String tsString = line.substring(0, pos);
						System.out.println("\""+tsString+"\"");
						Date ts = StringUtils.parseLogTimestamp(tsString + " " + timeZone);
						System.out.println("ts: "+ts);
						if (ts != null && (latestStartup == null || ts.after(latestStartup))) {
							latestStartup = ts;
						}
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println("latestStartup: "+latestStartup);
	}

	public void readDir(String dirPath) {
		try {
			Path dir = Paths.get(dirPath);
	
			Optional<Path> lastFilePath = Files.list(dir)
			    .filter(f -> !Files.isDirectory(f))
			    .filter(f -> f.getFileName().toString().matches(""))
			    .max(Comparator.comparingLong(f -> f.toFile().lastModified()));
	
			if ( lastFilePath.isPresent() ) {
			    // do your code here, lastFilePath contains all you need
			}
		} catch (Exception ex) {
			
		}
	}

}
