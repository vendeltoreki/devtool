package com.liferay.devtool.devenv;

import com.liferay.devtool.devenv.checks.BaseDevEnvCheckEntry;

public interface DevEnvEventListener {
	public void onUpdate(BaseDevEnvCheckEntry entry);
}
