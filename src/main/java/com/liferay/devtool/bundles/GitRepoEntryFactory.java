package com.liferay.devtool.bundles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

public class GitRepoEntryFactory {
	private String originUrl;
	private String upstreamUrl;
	private String currentBranch;

	public GitRepoEntry create(File gitRootDir) {
		if (gitRootDir == null || !gitRootDir.isDirectory()) {
			return null;
		}

		String configPath = gitRootDir.getAbsolutePath() + File.separator + ".git" + File.separator + "config";
		String headPath = gitRootDir.getAbsolutePath() + File.separator + ".git" + File.separator + "HEAD";

		readConfigFile(configPath);
		readHeadFile(headPath);

		GitRepoEntry repo = new GitRepoEntry();
		repo.setRootDir(gitRootDir);
		repo.setName(gitRootDir.getName());
		repo.setOriginUrl(originUrl);
		repo.setUpstreamUrl(upstreamUrl);
		repo.setCurrentBranch(currentBranch);
		return repo;
	}

	private void readConfigFile(String configPath) {
		try (FileInputStream inputStream = new FileInputStream(configPath)) {
			String everything = IOUtils.toString(inputStream, Charset.defaultCharset());
			processFile(everything.split("\n"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processFile(String[] lines) {
		int status = 0;
		for (String line : lines) {
			if (line.contains("[remote \"origin\"]")) {
				status = 1;
			} else if (line.contains("[remote \"upstream\"]")) {
				status = 2;
			} else if (line.startsWith("[")) {
				status = 0;
			} else if (line.trim().startsWith("url = ")) {
				if (status == 1) {
					originUrl = parseValue(line);
					status = 0;
				} else if (status == 2) {
					upstreamUrl = parseValue(line);
					status = 0;
				}
			}
		}

	}

	private String parseValue(String line) {
		if (line == null) {
			return null;
		}

		int n = line.indexOf("=");
		if (n > -1) {
			return line.substring(n + 1).trim();
		} else {
			return line.trim();
		}
	}

	private void readHeadFile(String headPath) {
		try (FileInputStream inputStream = new FileInputStream(headPath)) {
			String everything = IOUtils.toString(inputStream, Charset.defaultCharset());
			processHeadFile(everything.split("\n"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processHeadFile(String[] lines) {
		for (String line : lines) {
			if (line.startsWith("ref:")) {
				int i = line.lastIndexOf("/");
				if (i > -1) {
					currentBranch = line.substring(i + 1).trim();
				}
			}
		}
	}

}
