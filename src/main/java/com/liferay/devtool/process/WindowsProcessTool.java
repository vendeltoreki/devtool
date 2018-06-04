package com.liferay.devtool.process;

import static com.liferay.devtool.utils.StringUtils.notEmpty;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.liferay.devtool.utils.SimpleCommand;
import com.liferay.devtool.utils.StringUtils;
import com.liferay.devtool.utils.SysEnv;

public class WindowsProcessTool {
	private SysEnv sysEnv;
	private List<Map<String,String>> tasklistOutput;
	private List<Map<String,String>> wmicOutout;
	private List<Map<String,String>> netstatOutout;
	private Map<String,ProcessEntry> processes = new HashMap<>();
	
	public void refresh() {
		runTaskListCommand();
		runWmicCommand();
		runNetstatCommand();
		
		findProcesses();
	}
	
	private void findProcesses() {
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
		
		for(Map<String,String> record : wmicOutout) {
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
				}

				String desc = record.get("Description");
				if (notEmpty(desc)) {
					processEntry.setExecName(desc);
				}
				
				
			}
		}
		
		for(Map<String,String> record : netstatOutout) {
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
					System.out.println("--Listening:"+processEntry.getShortName()+" -- "+localAddr);
				}
			}
		}
		
		for (String pid : processes.keySet()) {
			ProcessEntry entry = processes.get(pid);
			if (entry.getCommandLine() != null && (
					entry.getCommandLine().toLowerCase().contains("tomcat") || 
					entry.getCommandLine().toLowerCase().contains("java") || 
					entry.getCommandLine().toLowerCase().contains("ant") || 
					entry.getCommandLine().toLowerCase().contains("mysqld") 
					)) {
				System.out.println(">>> "+pid+", "+entry.getShortName());
				
				System.out.println("\t"+entry.getCommandLine());
				if (entry.getListeningPorts() != null && !entry.getListeningPorts().isEmpty()) {
					System.out.println("\tPorts: "+StringUtils.join(entry.getListeningPorts(), ","));
				}
				
				System.out.print("\t"+pid);
				ProcessEntry parentEntry = entry.getParentProcess();
				while (parentEntry != null) {
					System.out.print(" --> "+parentEntry.getPid() + " ("+ parentEntry.getShortName() + ")");
					parentEntry = parentEntry.getParentProcess();
				}
				System.out.println();
			}
		}
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

		String command = "TASKLIST /V /FO CSV /FI \"USERNAME eq "+System.getenv("COMPUTERNAME")+"\\"+System.getenv("USERNAME")+"\"";
		comm.run(command);
		tasklistOutput = processTaskListOutput(comm.getStdOut());
	}

	private void runWmicCommand() {
		SimpleCommand comm = new SimpleCommand();
		comm.setSysEnv(sysEnv);

		String command = "wmic process list /FORMAT:CSV";
		comm.run(command);
		wmicOutout = processWmicOutput(comm.getStdOut());
	}

	private void runNetstatCommand() {
		SimpleCommand comm = new SimpleCommand();
		comm.setSysEnv(sysEnv);

		String command = "netstat -nao";
		comm.run(command);
		netstatOutout = processNetstatOutput(comm.getStdOut());
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
}
