package com.liferay.devtool.process;

import java.io.File;
import java.io.IOException;

import com.liferay.devtool.DevToolContext;
import com.liferay.devtool.bundles.BundleEntry;

public class BundleRunner {
	private DevToolContext context;
	private BundleEntry bundleEntry;

	public BundleEntry getBundleEntry() {
		return bundleEntry;
	}

	public void setBundleEntry(BundleEntry bundleEntry) {
		this.bundleEntry = bundleEntry;
	}

	public DevToolContext getContext() {
		return context;
	}

	public void setContext(DevToolContext context) {
		this.context = context;
	}

	public void start() {
	    //String command = "cmd /c cd c:\\liferay\\bundles\\de-33\\tomcat-8.0.32\\bin\\ && catalina jpda start";
		String command = "cmd /c cd " + findWebServerBinDir() + " && catalina jpda start";
		context.getLogger().log("Executing command: \""+command+"\"");
		
		try {
			Process process = Runtime.getRuntime().exec(command);
			int exitCode = process.waitFor();
			context.getLogger().log("Finished. Exit code="+exitCode);
		} catch (IOException e) {
			context.getLogger().log(e);
		} catch (InterruptedException e) {
			context.getLogger().log(e);
		}		
	}

	private String findWebServerBinDir() {
		return bundleEntry.getWebServerDir().getAbsolutePath()
			+ File.separator
			+ "bin"
			+ File.separator;
	}

	public void stop() {
		String command = "cmd /c cd " + findWebServerBinDir() + " && shutdown";
		context.getLogger().log("Executing command: \""+command+"\"");
		
		try {
			Process process = Runtime.getRuntime().exec(command);
			int exitCode = process.waitFor();
			context.getLogger().log("Finished. Exit code="+exitCode);
		} catch (IOException e) {
			context.getLogger().log(e);
		} catch (InterruptedException e) {
			context.getLogger().log(e);
		}		
	}
	
}
