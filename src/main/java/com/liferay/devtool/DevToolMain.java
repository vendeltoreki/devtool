package com.liferay.devtool;

import com.liferay.devtool.cli.DevToolCli;
import com.liferay.devtool.context.DevToolContext;
import com.liferay.devtool.window.DevToolWindow;

public class DevToolMain {
	private DevToolContext context = DevToolContext.getDefault();

	public static void main(String[] args) {
		DevToolMain devToolMain = new DevToolMain();
		if (args == null || args.length == 0) {
			devToolMain.runWindow();
		} else {
			devToolMain.runCli(args);
		}
	}

	public void runWindow() {
		DevToolWindow window = new DevToolWindow();
		window.setContext(context);
		window.runWindowApp();
	}
	
	public void runCli(String[] args) {
		DevToolCli cli = new DevToolCli();
		cli.run(args);
	}
}
