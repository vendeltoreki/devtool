package com.liferay.devtool.devenv.checks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.liferay.devtool.utils.SimpleCommand;

public class CommandDevEnvCheckEntry extends BaseDevEnvCheckEntry {
	private String command;

	public CommandDevEnvCheckEntry(String command) {
		super();
		this.command = command;
		title = "Check command \"" + command + "\"";
	}

	@Override
	public void runCheck() {
		SimpleCommand comm = new SimpleCommand();
		comm.setSysEnv(sysEnv);

		if (context.isWindows()) {
			comm.run("cmd.exe /c " + command);
		} else {
			comm.run(command);
		}

		if (comm.isSuccess()) {
			success("Command: \"" + command + "\"");
		} else {
			fail("Command \"" + command + "\" failed with exit code " + comm.getExitValue());
		}

		List<String> lines = new ArrayList<>();
		lines.addAll(comm.getStdOut());
		lines.addAll(comm.getStdErr());
		description = "Output: " + lines.stream().collect(Collectors.joining("\n"));
	}

}
