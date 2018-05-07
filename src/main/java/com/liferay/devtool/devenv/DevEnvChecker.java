package com.liferay.devtool.devenv;

import java.util.ArrayList;
import java.util.List;

import com.liferay.devtool.devenv.checks.BaseDevEnvCheckEntry;
import com.liferay.devtool.devenv.checks.CommandDevEnvCheckEntry;
import com.liferay.devtool.devenv.checks.EnvVarCheckEntry;
import com.liferay.devtool.devenv.checks.ExecutableLocationCheckEntry;
import com.liferay.devtool.devenv.checks.JarManifestCheck;
import com.liferay.devtool.devenv.checks.OsTypeCheckEntry;
import com.liferay.devtool.utils.SysEnv;

public class DevEnvChecker {
	private List<BaseDevEnvCheckEntry> checks = new ArrayList<>();
	private DevEnvCheckContext context = new DevEnvCheckContext();
	private DevEnvEventListener listener;
	private SysEnv sysEnv;

	public SysEnv getSysEnv() {
		return sysEnv;
	}

	public void setSysEnv(SysEnv sysEnv) {
		this.sysEnv = sysEnv;
	}

	public DevEnvChecker() {
		super();
	}

	public static void testRun() {
		DevEnvChecker checker = new DevEnvChecker();
		checker.addChecks();
		checker.runChecks();
		checker.printChecks();
	}

	public void addChecks() {
		addCheck(new OsTypeCheckEntry());
		addCheck(new EnvVarCheckEntry("JAVA_HOME", null, true));
		addCheck(new EnvVarCheckEntry("JRE_HOME", null, true));
		addCheck(new EnvVarCheckEntry("ANT_HOME", null, true));
		addCheck(new EnvVarCheckEntry("ANT_OPTS", "-Xms4096m -Xmx4096m", false));
		addCheck(new EnvVarCheckEntry("JPDA_ADDRESS", "8000", false));
		addCheck(new EnvVarCheckEntry("JPDA_TRANSPORT", "dt_socket", false));
		addCheck(new EnvVarCheckEntry("ASDFGH", "qwerty", false));
		addCheck(new CommandDevEnvCheckEntry("java -version"));
		addCheck(new CommandDevEnvCheckEntry("javac -version"));
		addCheck(new CommandDevEnvCheckEntry("ant -version"));
		addCheck(new CommandDevEnvCheckEntry("git --version"));
		addCheck(new CommandDevEnvCheckEntry("blade version"));
		addCheck(new CommandDevEnvCheckEntry("asdfgh version"));
		addCheck(new JarManifestCheck("%ANT_HOME%/lib/", "Bundle-Version", "3.12.1.v20160829-0950"));
		addCheck(new ExecutableLocationCheckEntry("java", "%JAVA_HOME%/bin/"));
		addCheck(new ExecutableLocationCheckEntry("javac", "%JAVA_HOME%/bin/"));
		addCheck(new ExecutableLocationCheckEntry("ant", "%ANT_HOME%/bin/"));
	}

	public void addCheck(BaseDevEnvCheckEntry entry) {
		entry.setContext(context);
		entry.setSysEnv(sysEnv);
		checks.add(entry);
	}

	public void runChecks() {
		for (BaseDevEnvCheckEntry entry : checks) {
			entry.runCheck();
			System.out.println(entry.getMessage());
			if (listener != null) {
				listener.onUpdate(entry);
			}
		}
	}

	public void printChecks() {
		for (BaseDevEnvCheckEntry entry : checks) {
			System.out.println("Check: " + entry.getClass().getSimpleName() + " -- " + entry.getStatus().name() + ": "
					+ entry.getMessage());
		}
	}

	public List<BaseDevEnvCheckEntry> getChecks() {
		return checks;
	}

	public void setListener(DevEnvEventListener listener) {
		this.listener = listener;
	}
}
