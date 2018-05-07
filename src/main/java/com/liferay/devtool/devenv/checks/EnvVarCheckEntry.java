package com.liferay.devtool.devenv.checks;

import java.io.File;

import com.liferay.devtool.devenv.CheckStatus;

public class EnvVarCheckEntry extends BaseDevEnvCheckEntry {
	private String envVarName;
	private String expectedValue;
	private boolean checkHomeDir = false;

	public EnvVarCheckEntry(String envVarName, String expectedValue, boolean checkHomeDir) {
		super();
		this.envVarName = envVarName;
		this.expectedValue = expectedValue;
		this.checkHomeDir = checkHomeDir;
		title = "Check environment variable: \"" + envVarName + "\""
				+ (expectedValue != null ? ", expected=\"" + expectedValue + "\"" : "");
	}

	@Override
	public void runCheck() {
		String envVarValue = sysEnv.getEnvVar(envVarName);

		if (envVarValue == null) {
			fail("\"" + envVarName + "\" environment variable is not defined!");
		} else if (envVarValue.trim().length() == 0) {
			fail("\"" + envVarName + "\" environment variable is empty!");
		} else {
			if (checkHomeDir) {
				File dir = sysEnv.createFile(envVarValue);
				checkDirectory(envVarValue, dir);
			}

			if (expectedValue != null && !envVarValue.equals(expectedValue)) {
				fail("\"" + envVarName + "\" value is \"" + envVarValue + "\" instead of \"" + expectedValue + "\"");
			}
		}

		if (status != CheckStatus.FAIL) {
			success(envVarName + " = \"" + envVarValue + "\"");
			context.addEnvVar(envVarName, envVarValue);
		}
	}

	private void checkDirectory(String envVarValue, File dir) {
		if (!dir.exists()) {
			fail("\"" + envVarName + "\" directory \"" + envVarValue + "\" does not exist!");
		} else if (!dir.isDirectory()) {
			fail("\"" + envVarName + "\" file \"" + envVarValue + "\" is not a directory!");
		}
	}

}
