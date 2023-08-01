package com.liferay.devtool.process;

import static com.liferay.devtool.utils.StringUtils.notEmpty;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.liferay.devtool.utils.SimpleCommand;
import com.liferay.devtool.utils.StringUtils;
import com.liferay.devtool.utils.SysEnv;

public class WindowsProcessTool {
	private SysEnv sysEnv;
	private List<Map<String,String>> tasklistOutput;
	private List<Map<String,String>> wmicOutput;
	private List<Map<String,String>> netstatOutput;
	private Map<String,ProcessEntry> processes = new HashMap<>();
	private boolean taskListCommandEnabled = false;
	
	public void refresh() {
		if (taskListCommandEnabled) {
			runTaskListCommand();
		}
		runWmicCommand();
		runNetstatCommand();
		
		findProcesses();
	}
	
	private void findProcesses() {
		findProcessesInTaskList();
		findProcessesInWmicOutput();
		findProcessesInNetstatOutput();
	}

	public void printProcesses() {
		String[] keywords = new String[] { "tomcat", "java", "ant", "mysqld" };
		
		for (String pid : processes.keySet()) {
			ProcessEntry entry = processes.get(pid);
			if (StringUtils.containsAny(entry.getCommandLine(), keywords) || 
					StringUtils.containsAny(entry.getExecName(), keywords) || 
					StringUtils.containsAny(entry.getWindowTitle(), keywords)) {
				System.out.println(">>> "+pid+", "+entry.getShortName());
				
				System.out.println("\t"+entry.getCommandLine());
				
				if (entry.getBundlePath() != null) {
					System.out.println("\tBUNDLE: "+entry.getBundlePath());
				}

				if (entry.getListeningPorts() != null && !entry.getListeningPorts().isEmpty()) {
					System.out.println("\tPorts: "+StringUtils.join(entry.getListeningPorts(), ","));
				}
				
				System.out.print("\t"+pid);
				ProcessEntry parentEntry = entry.getParentProcess();
				int n = 0;
				while (parentEntry != null && n < 10) {
					System.out.print(" --> "+parentEntry.getPid() + " ("+ parentEntry.getShortName() + ")");
					parentEntry = parentEntry.getParentProcess();
					++n;
				}
				System.out.println();
			}
		}
	}

	private void findProcessesInNetstatOutput() {
		if (netstatOutput == null) {
			return;
		}
		
		for(Map<String,String> record : netstatOutput) {
			String pid = record.get("PID");
			String protocol = record.get("Proto");
			String state = record.get("State");
			
			if (notEmpty(pid) &&
					notEmpty(protocol) && protocol.equals("TCP") &&
					notEmpty(state) && state.equals("LISTENING")) {
				ProcessEntry processEntry = getOrCreateProcessEntry(pid);
				
				String localAddr = record.get("Local Address");
				if (notEmpty(localAddr) && 
						(localAddr.startsWith("0.0.0.0:") || localAddr.startsWith("127.0.0.1:"))) {
					
					processEntry.addPort(StringUtils.getPort(localAddr));
					//System.out.println("--Listening:"+processEntry.getShortName()+" -- "+localAddr);
				}
			}
		}
	}

	private void findProcessesInWmicOutput() {
		if (wmicOutput == null) {
			return;
		}

		for(Map<String,String> record : wmicOutput) {
			String pid = record.get("ProcessId");
			if (notEmpty(pid)) {
				ProcessEntry processEntry = getOrCreateProcessEntry(pid);
				
				String parentPid = record.get("ParentProcessId");
				if (notEmpty(parentPid) && !parentPid.equals("0")) {
					processEntry.setParentProcess(getOrCreateProcessEntry(parentPid));
				}
				
				String command = record.get("CommandLine");
				if (notEmpty(command)) {
					processEntry.setCommandLine(command);

					String bundlePath = parseBundlePathFromCommand(command);
					if (notEmpty(bundlePath)) {
						processEntry.setBundlePath(bundlePath);
					}
				}
				
				String desc = record.get("Description");
				if (notEmpty(desc)) {
					processEntry.setExecName(desc);
				}
			}
		}
	}

	private void findProcessesInTaskList() {
		if (tasklistOutput == null) {
			return;
		}
		
		for(Map<String,String> record : tasklistOutput) {
			String pid = record.get("PID");
			if (notEmpty(pid)) {
				ProcessEntry processEntry = getOrCreateProcessEntry(pid);

				String windowTitle = record.get("Window Title");
				if (notEmpty(windowTitle)) {
					processEntry.setWindowTitle(windowTitle);
				}
			}
		}
	}

	private String parseBundlePathFromCommand(String command) {
		String tomcatHome = StringUtils.extractTomcatHomeFromCommand(command);
		if (tomcatHome != null) {
			File file = sysEnv.createFile(tomcatHome);
			if (file.exists() && file.isDirectory()) {
				return file.getParentFile().getAbsolutePath();
			}
		}
		
		return null;
	}

	private ProcessEntry getOrCreateProcessEntry(String pid) {
		if (processes.containsKey(pid)) {
			return processes.get(pid);
		} else {
			ProcessEntry processEntry = new ProcessEntry();
			processEntry.setPid(pid);
			processes.put(pid, processEntry);
			
			return processEntry;
		}
	}

	private void runTaskListCommand() {
		SimpleCommand comm = new SimpleCommand();
		comm.setSysEnv(sysEnv);

		String command = "cmd.exe /c " + "TASKLIST /V /FO CSV /FI \"USERNAME eq "+System.getenv("COMPUTERNAME")+"\\"+System.getenv("USERNAME")+"\"";
		comm.run(command);
		tasklistOutput = processTaskListOutput(comm.getStdOut());
	}

	private void runWmicCommand() {
		SimpleCommand comm = new SimpleCommand();
		comm.setSysEnv(sysEnv);

		String command = "cmd.exe /c " + "wmic process list /FORMAT:CSV";
		comm.run(command);
		wmicOutput = processWmicOutput(comm.getStdOut());
	}

	private void runNetstatCommand() {
		SimpleCommand comm = new SimpleCommand();
		comm.setSysEnv(sysEnv);

		String command = "cmd.exe /c " + "netstat -nao";
		comm.run(command);
		netstatOutput = processNetstatOutput(comm.getStdOut());
	}
	
	protected List<Map<String,String>> processTaskListOutput(List<String> output) {
		List<Map<String,String>> res = new ArrayList<>();
		
		List<String> headers = new ArrayList<>();
		boolean headerRead = false;
		for (String line : output) {
			if (line.trim().length() > 0) {
				if (!headerRead) {
					headers.addAll(Arrays.asList(StringUtils.splitQuotedCsv(line)));
					headerRead = true;
				} else {
					String[] data = StringUtils.splitQuotedCsv(line);
					
					Map<String,String> rec = new HashMap<>();
					for (int i=0; i<headers.size(); ++i) {
						rec.put(headers.get(i), data[i]);
					}
					
					res.add(rec);
				}
			}
		}
		
		return res;
	}

	protected List<Map<String, String>> processWmicOutput(List<String> output) {
		List<Map<String,String>> res = new ArrayList<>();
		
		List<String> headers = new ArrayList<>();
		boolean headerRead = false;
		for (String line : output) {
			if (line.trim().length() > 0) {
				if (!headerRead) {
					headers.addAll(Arrays.asList(line.split(",")));
					headerRead = true;
				} else {
					String[] data = line.split(",");
					
					if (data.length > headers.size()) {
						data = StringUtils.collapseCsvLine(data, 1, headers.size());
					}
					
					Map<String,String> rec = new HashMap<>();
					for (int i=0; i<headers.size(); ++i) {
						rec.put(headers.get(i), data[i]);
					}
					
					res.add(rec);
				}
			}
		}
		
		return res;
	}
	
	protected List<Map<String, String>> processNetstatOutput(List<String> output) {
		List<Map<String,String>> res = new ArrayList<>();
		
		List<String> headers = new ArrayList<>();
		boolean headerRead = false;
		for (String line : output) {
			line = line.trim().replaceAll("\\s{2,}", "|");
			String[] data = line.split("\\|");
			
			if (line.trim().length() > 0 && data.length > 2) {
				
				if (!headerRead) {
					headers.addAll(Arrays.asList(data));
					headerRead = true;
				} else {
					
					if (data.length < headers.size()) {
						data = StringUtils.fillCsvMissingFields(data, 3, headers.size());
					}
					
					Map<String,String> rec = new HashMap<>();
					for (int i=0; i<headers.size(); ++i) {
						rec.put(headers.get(i), data[i]);
					}
					
					res.add(rec);
				}
			}
		}
		
		return res;
	}

	public void setSysEnv(SysEnv sysEnv) {
		this.sysEnv = sysEnv;
	}
	
	public List<ProcessEntry> getProcessEntries() {
		List<ProcessEntry> res = new ArrayList<>();
		res.addAll(processes.values());
		return res;
	}
}
