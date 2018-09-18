package com.liferay.devtool.process;

import java.io.File;
import java.io.IOException;

import com.liferay.devtool.bundles.BundleEntry;

public class BundleRunner {
	private BundleEntry bundleEntry;

	public BundleEntry getBundleEntry() {
		return bundleEntry;
	}

	public void setBundleEntry(BundleEntry bundleEntry) {
		this.bundleEntry = bundleEntry;
	}

	public void start() {
	    //String command = "cmd /c cd c:\\liferay\\bundles\\de-33\\tomcat-8.0.32\\bin\\ && catalina jpda start";
		String command = "cmd /c cd " + findWebServerBinDir() + " && catalina jpda start";
		System.out.println("Executing command: \""+command+"\"");
		
		try {
			Process process = Runtime.getRuntime().exec(command);
			int exitCode = process.waitFor();
			System.out.println("Finished. Exit code="+exitCode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		System.out.println("Executing command: \""+command+"\"");
		
		try {
			Process process = Runtime.getRuntime().exec(command);
			int exitCode = process.waitFor();
			System.out.println("Finished. Exit code="+exitCode);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
}
