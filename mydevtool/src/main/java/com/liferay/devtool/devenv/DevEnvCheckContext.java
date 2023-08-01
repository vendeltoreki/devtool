package com.liferay.devtool.devenv;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public class DevEnvCheckContext {
	private OsType osType = null;
	private Map<String, String> envVars = new HashMap<>();

	public OsType getOsType() {
		return osType;
	}

	public void setOsType(OsType osType) {
		this.osType = osType;
	}

	public boolean isWindows() {
		return osType != null && osType == OsType.WINDOWS;
	}

	public void addEnvVar(String envVarName, String envVarValue) {
		envVars.put(envVarName, envVarValue);
	}

	public String getEnvVar(String envVarName) {
		return envVars.get(envVarName);
	}

	public String replaceEnvVarsInPath(String path) {
		String res = path;
		for (String key : envVars.keySet()) {
			String keyVar = "%" + key + "%";
			if (res.contains(keyVar)) {
				res = res.replaceAll(keyVar, Matcher.quoteReplacement(envVars.get(key)));
			}
		}

		if (isWindows()) {
			res = res.replaceAll("\\/", "\\\\");
		} else {
			res = res.replaceAll("\\\\", "/");
		}

		return res;
	}

}
