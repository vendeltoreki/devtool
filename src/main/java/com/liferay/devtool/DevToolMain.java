package com.liferay.devtool;

import com.liferay.devtool.utils.SysEnv;
import com.liferay.devtool.window.DevToolWindow;

public class DevToolMain {
	private SysEnv sysEnv = new SysEnv();

	public static void main(String[] args) {
		System.out.println("T:" + Thread.currentThread().getName() + " -- Main Started.");

		DevToolMain devToolMain = new DevToolMain();
		devToolMain.run();

		System.out.println("T:" + Thread.currentThread().getName() + " -- Main Finished.");
	}

	public void run() {
		DevToolWindow window = new DevToolWindow();
		window.setSysEnv(sysEnv);
		window.runWindowApp();
	}
}
