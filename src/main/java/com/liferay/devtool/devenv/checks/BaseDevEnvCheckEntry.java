package com.liferay.devtool.devenv.checks;

import com.liferay.devtool.devenv.CheckStatus;
import com.liferay.devtool.devenv.DevEnvCheckContext;
import com.liferay.devtool.utils.SysEnv;

public abstract class BaseDevEnvCheckEntry {
	protected SysEnv sysEnv;
	protected DevEnvCheckContext context;
	protected CheckStatus status = CheckStatus.UNKNOWN;
	protected String message;
	protected String title;
	protected String description;

	public abstract void runCheck();

	public DevEnvCheckContext getContext() {
		return context;
	}

	public void setContext(DevEnvCheckContext context) {
		this.context = context;
	}

	protected void fail(String failMessage) {
		message = failMessage;
		status = CheckStatus.FAIL;
	}

	protected void failIfUnknown(String failMessage) {
		if (status == CheckStatus.UNKNOWN) {
			fail(failMessage);
		}
	}

	protected void success(String successMessage) {
		message = successMessage;
		status = CheckStatus.SUCCESS;
	}

	protected boolean isFailed() {
		return status == CheckStatus.FAIL;
	}

	public CheckStatus getStatus() {
		return status;
	}

	public String getMessage() {
		return message;
	}

	public String getDescription() {
		return description;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public SysEnv getSysEnv() {
		return sysEnv;
	}

	public void setSysEnv(SysEnv sysEnv) {
		this.sysEnv = sysEnv;
	}

	public String getClassName() {
		return this.getClass().getSimpleName();
	}

	public void reset() {
		status = CheckStatus.UNKNOWN;
		message = null;
		description = null;
	}
}
