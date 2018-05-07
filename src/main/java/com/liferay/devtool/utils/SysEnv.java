package com.liferay.devtool.utils;

import java.io.File;
import java.io.IOException;

public class SysEnv {

	public String getProperty(String name) {
		return System.getProperty(name);
	}

	public String getEnvVar(String envVarName) {
		return System.getenv(envVarName);
	}

	public File createFile(String filePath) {
		return new File(filePath);
	}

	public Process getRuntimeProcess(String command) throws IOException {
		return Runtime.getRuntime().exec(command);
	}

}
