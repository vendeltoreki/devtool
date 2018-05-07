package com.liferay.devtool.bundles;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.FileSystems;
import java.util.Properties;

import org.apache.commons.io.IOUtils;

import com.liferay.devtool.utils.StringUtils;

public class GitRepoEntryFactory {
	private String originUrl;
	private String upstreamUrl;
	private String currentBranch;
	private String buildTargetDir;
	private File gitRootDirFile;

	public GitRepoEntry create(File gitRootDir) {
		gitRootDirFile = gitRootDir;
		
		if (gitRootDir == null || !gitRootDir.isDirectory()) {
			return null;
		}

		String configPath = gitRootDir.getAbsolutePath() + File.separator + ".git" + File.separator + "config";
		String headPath = gitRootDir.getAbsolutePath() + File.separator + ".git" + File.separator + "HEAD";
		
		String localUserName = System.getProperty("user.name");
		
		readConfigFile(configPath);
		readHeadFile(headPath);
		
		String appServerPropertiesPath = gitRootDir.getAbsolutePath() + File.separator + "app.server."+localUserName+".properties";
		readPropertiesFile(appServerPropertiesPath);
		
		if (buildTargetDir == null) {
			appServerPropertiesPath = gitRootDir.getAbsolutePath() + File.separator + "app.server.properties";
			readPropertiesFile(appServerPropertiesPath);			
		}

		GitRepoEntry repo = new GitRepoEntry();
		repo.setRootDir(gitRootDir);
		repo.setName(gitRootDir.getName());
		repo.setOriginUrl(originUrl);
		repo.setUpstreamUrl(upstreamUrl);
		repo.setCurrentBranch(currentBranch);
		repo.setBuildTargetDir(buildTargetDir);
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

	private void readPropertiesFile(String propertiesFilePath) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			File propertiesFile = new File(propertiesFilePath);
			if (propertiesFile.exists() && propertiesFile.isFile()) { 
				input = new FileInputStream(propertiesFilePath);
				prop.load(input);
				
				// app.server.parent.dir=${project.dir}/../bundles_ee
				
				String appServDir = prop.getProperty("app.server.parent.dir");
				String parentDir = gitRootDirFile.getAbsolutePath();
				appServDir = StringUtils.replacePathParam(appServDir, "${project.dir}", parentDir);
				
				String absolutePath = FileSystems.getDefault().getPath(appServDir).normalize().toAbsolutePath().toString();
				buildTargetDir = absolutePath;
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
}
