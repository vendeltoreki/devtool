package com.liferay.devtool.experiment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.liferay.devtool.process.WindowsProcessTool;
import com.liferay.devtool.utils.SimpleCommand;
import com.liferay.devtool.utils.SysEnv;

public class WindowsProcessUtil {

	public static void main(String[] args) {
		long t = System.currentTimeMillis();

		//listEnvVariables();
		testWindowsProcessTool();
		//testRunCommand();

		System.out.println("finished: "+(System.currentTimeMillis() - t)+" ms");
	}


	private static void testRunCommand() {
		//String command = "TASKLIST /V /FO CSV /FI \"USERNAME eq "+System.getenv("COMPUTERNAME")+"\\"+System.getenv("USERNAME")+"\"";
		//String command = "TASKLIST /V /FO CSV";
		//String command = "wmic process list /FORMAT:CSV";
		String command = "netstat -nao";
		
		System.out.println("executing: "+command);
		
		run(command);
	}


	private static void testWindowsProcessTool() {
		WindowsProcessTool wp = new WindowsProcessTool();
		wp.setSysEnv(new SysEnv());
		wp.refresh();
		wp.printProcesses();
	}

	
	private static void listEnvVariables() {
		//COMPUTERNAME -- DESKTOP-MPESIIJ
		//USERNAME -- Liferay
		
		Map<String,String> vars = System.getenv();
		for (String key : vars.keySet()) {
			System.out.println(key+" -- "+vars.get(key));
		}
	}


	public static void run(String command) {
		try {
			Process p = Runtime.getRuntime().exec(command);
			/*OutputStream os = p.getOutputStream();
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(os));
			bw.write("DESKTOP");
			bw.newLine();
			bw.flush();*/	
			
			List<String> stdOut = readInputStreamToList(p.getInputStream());
			List<String> stdErr = readInputStreamToList(p.getErrorStream());
			
			for (String line : stdOut) {
				System.out.println(line);
			}

			for (String line : stdErr) {
				System.out.println("STDERR: "+line);
			}
			
			int exitValue = p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public static List<String> readInputStreamToList(InputStream inputStream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		List<String> res = new ArrayList<>();
		String s = null;
		try {
			while ((s = reader.readLine()) != null) {
				res.add(s);
			}
		} catch (IOException e) {
			res.add(e.getMessage());
		}

		return res;
	}	
}
