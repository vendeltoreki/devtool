package com.liferay.devtool.devenv.checks;

import com.liferay.devtool.devenv.CheckStatus;
import com.liferay.devtool.devenv.OsType;

public class OsTypeCheckEntry extends BaseDevEnvCheckEntry {
	public OsTypeCheckEntry() {
		super();
		title = "Check OS type";
	}

	@Override
	public void runCheck() {
		String osName = sysEnv.getProperty("os.name");
		;
		if (osName.toLowerCase().contains("windows")) {
			context.setOsType(OsType.WINDOWS);
		} else {
			context.setOsType(OsType.LINUX);
		}
		message = osName;
		status = CheckStatus.SUCCESS;
	}

}
