package com.liferay.devtool.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.liferay.devtool.context.ContextBase;

public class LogReader extends ContextBase {
	private String timeZone = "GMT";
	private String fileRegexPattern = "catalina\\.[0-9]{4}-[0-9]{2}-[0-9]{2}\\.log";
	private Date latestStartup = null;
	
	public void readFile(Path path) {
		try {
			final List<String> lines = Files.readAllLines(path);
			for (String line : lines) {
				if (line.contains("org.apache.catalina.startup.Catalina.start Server startup in")) {
					int pos = line.indexOf(" INFO");
					if (pos > -1) {
						String tsString = line.substring(0, pos);
						Date ts = StringUtils.parseLogTimestamp(tsString + " " + timeZone);
						if (ts != null && (latestStartup == null || ts.after(latestStartup))) {
							latestStartup = ts;
						}
					}
				}
			}
		} catch (IOException e) {
			getContext().getLogger().log(e);
		}
	}

	public void readDir(String dirPath) {
		try {
			Path dir = Paths.get(dirPath);
	
			Optional<Path> lastFilePath = Files.list(dir)
			    .filter(f -> !Files.isDirectory(f))
			    .filter(f -> f.getFileName().toString().matches(fileRegexPattern))
			    .max(Comparator.comparingLong(f -> f.toFile().lastModified()));
	
			if (lastFilePath.isPresent()) {
				readFile(lastFilePath.get());
			}
		} catch (Exception e) {
			getContext().getLogger().log(e);
		}
	}
	
	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public String getFileRegexPattern() {
		return fileRegexPattern;
	}

	public void setFileRegexPattern(String fileRegexPattern) {
		this.fileRegexPattern = fileRegexPattern;
	}

	public Date getLatestStartup() {
		return latestStartup;
	}

	public void setLatestStartup(Date latestStartup) {
		this.latestStartup = latestStartup;
	}

	public void readFile(String filePath) {
		readFile(Paths.get(filePath));
	}
}
