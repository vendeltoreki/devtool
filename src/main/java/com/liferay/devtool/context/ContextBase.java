package com.liferay.devtool.context;

public class ContextBase {
	private DevToolContext context;

	public DevToolContext getContext() {
		if (context != null) {
			return context;
		} else {
			return DevToolContext.getDefault();
		}
	}

	public void setContext(DevToolContext context) {
		this.context = context;
	}

}
