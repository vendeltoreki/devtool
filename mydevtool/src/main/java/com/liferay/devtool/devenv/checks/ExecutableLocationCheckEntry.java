package com.liferay.devtool.devenv.checks;

import java.util.List;

import com.liferay.devtool.utils.SimpleCommand;

public class ExecutableLocationCheckEntry extends BaseDevEnvCheckEntry {
	private String executable;
	private String expectedPath;

	public ExecutableLocationCheckEntry(String executable, String expectedPath) {
		super();
		this.executable = executable;
		this.expectedPath = expectedPath;
		title = "Check location of executable \"" + executable + "\"";
	}

	@Override
	public void runCheck() {
		SimpleCommand comm = new SimpleCommand();
		comm.setSysEnv(sysEnv);

		if (context.isWindows()) {
			comm.run("cmd.exe /c where " + executable);
		} else {
			comm.run("which " + executable);
		}

		if (comm.isSuccess()) {
			List<String> lines = comm.getStdOut();
			if (lines.size() >= 1) {
				String foundPath = lines.get(0).trim();
				String expectedResolvedPath = context.replaceEnvVarsInPath(expectedPath);
				if (foundPath.startsWith(expectedResolvedPath)) {
					success("The found executable location is in \"" + foundPath + "\" and the expected location is \""
							+ expectedResolvedPath + "\"");
				} else {
					fail("The found executable location is in \"" + foundPath + "\" and the expected location is \""
							+ expectedResolvedPath + "\"");
				}
			}
		} else {
			fail("Command \"" + executable + "\" failed with exit code " + comm.getExitValue());
		}

		failIfUnknown("Could not verify.");
	}

}
