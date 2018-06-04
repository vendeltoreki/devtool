package com.liferay.devtool.process;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

public class WindowsProcessToolUnitTest extends WindowsProcessTool {
	@Test
	public void test_processTaskListOutput() throws Exception {
		List<String> output = new ArrayList<>(Arrays.asList(new String[] {
				"\"Image Name\",\"PID\",\"Session Name\",\"Session#\",\"Mem Usage\",\"Status\",\"User Name\",\"CPU Time\",\"Window Title\"",
				"\"Apoint.exe\",\"15600\",\"Console\",\"4\",\"4,756 K\",\"Running\",\"DESKTOP-MPESIIJ\\Liferay\",\"0:02:31\",\"N/A\"",
				"\"cmd.exe\",\"18188\",\"Console\",\"4\",\"916 K\",\"Unknown\",\"DESKTOP-MPESIIJ\\Liferay\",\"0:00:00\",\"N/A\"",
				"\"java.exe\",\"16696\",\"Console\",\"4\",\"1,133,528 K\",\"Running\",\"DESKTOP-MPESIIJ\\Liferay\",\"0:17:38\",\"Tomcat\""
		}));
		
		List<Map<String, String>> processTaskList = processTaskListOutput(output);
		
		Set<String> header = new HashSet<>(Arrays.asList(new String[] {"Image Name","PID","Session Name","Session#","Mem Usage","Status","User Name","CPU Time","Window Title"}));
		assertThat(processTaskList.size(), equalTo(3));
		
		for (Map<String,String> record : processTaskList) {
			assertThat(record.keySet(), equalTo(header));
		}

		assertThat(processTaskList.get(2).get("Image Name"), equalTo("java.exe"));
		assertThat(processTaskList.get(2).get("PID"), equalTo("16696"));
		assertThat(processTaskList.get(2).get("Mem Usage"), equalTo("1,133,528 K"));
		assertThat(processTaskList.get(2).get("Window Title"), equalTo("Tomcat"));
	}
	
	@Test
	public void test_processTaskListOutput_emptyLines() throws Exception {
		List<String> output = new ArrayList<>(Arrays.asList(new String[] {
				"",
				"\"Image Name\",\"PID\",\"Session Name\",\"Session#\",\"Mem Usage\",\"Status\",\"User Name\",\"CPU Time\",\"Window Title\"",
				"\"Apoint.exe\",\"15600\",\"Console\",\"4\",\"4,756 K\",\"Running\",\"DESKTOP-MPESIIJ\\Liferay\",\"0:02:31\",\"N/A\"",
				"",
				"\"cmd.exe\",\"18188\",\"Console\",\"4\",\"916 K\",\"Unknown\",\"DESKTOP-MPESIIJ\\Liferay\",\"0:00:00\",\"N/A\"",
				"\"java.exe\",\"16696\",\"Console\",\"4\",\"1,133,528 K\",\"Running\",\"DESKTOP-MPESIIJ\\Liferay\",\"0:17:38\",\"Tomcat\"",
				""
		}));
		
		List<Map<String, String>> processTaskList = processTaskListOutput(output);
		
		Set<String> header = new HashSet<>(Arrays.asList(new String[] {"Image Name","PID","Session Name","Session#","Mem Usage","Status","User Name","CPU Time","Window Title"}));
		assertThat(processTaskList.size(), equalTo(3));
		
		for (Map<String,String> record : processTaskList) {
			assertThat(record.keySet(), equalTo(header));
		}

		assertThat(processTaskList.get(2).get("Image Name"), equalTo("java.exe"));
		assertThat(processTaskList.get(2).get("PID"), equalTo("16696"));
		assertThat(processTaskList.get(2).get("Mem Usage"), equalTo("1,133,528 K"));
		assertThat(processTaskList.get(2).get("Window Title"), equalTo("Tomcat"));
	}

	@Test
	public void test_processWmicOutput_emptyLines() throws Exception {
		List<String> output = new ArrayList<>(Arrays.asList(new String[] {
				"",
				"Node,CommandLine,CSName,Description,ExecutablePath,ExecutionState,Handle,HandleCount,InstallDate,KernelModeTime,MaximumWorkingSetSize,MinimumWorkingSetSize,Name,OSName,OtherOperationCount,OtherTransferCount,PageFaults,PageFileUsage,ParentProcessId,PeakPageFileUsage,PeakVirtualSize,PeakWorkingSetSize,Priority,PrivatePageCount,ProcessId,QuotaNonPagedPoolUsage,QuotaPagedPoolUsage,QuotaPeakNonPagedPoolUsage,QuotaPeakPagedPoolUsage,ReadOperationCount,ReadTransferCount,SessionId,Status,TerminationDate,ThreadCount,UserModeTime,VirtualSize,WindowsVersion,WorkingSetSize,WriteOperationCount,WriteTransferCount",
				"",
				"DESKTOP-MPESIIJ,\"C:\\dev\\jdk1.8.0_151\\bin\\java.exe\"   -Djava.util.logging.config.file=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\conf\\logging.properties\" -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n  -Dfile.encoding=UTF8 -Djava.net.preferIPv4Stack=true -Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false -Duser.timezone=GMT -Xmx4000m -XX:MaxPermSize=512m  -Djava.endorsed.dirs=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\endorsed\" -classpath \"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\bin\\bootstrap.jar;c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\bin\\tomcat-juli.jar\" -Dcatalina.base=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\" -Dcatalina.home=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\" -Djava.io.tmpdir=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\temp\" org.apache.catalina.startup.Bootstrap  start,DESKTOP-MPESIIJ,java.exe,C:\\dev\\jdk1.8.0_151\\bin\\java.exe,,16696,1702,,4792968750,1380,200,java.exe,Microsoft Windows 10 Pro|C:\\WINDOWS|\\Device\\Harddisk0\\Partition3,23131553,502819127,3362714,3165700,16328,3652948,10993930240,3004628,8,3241676800,16696,137,268,180,273,2674961,2175390991,4,,,144,7242656250,10978717696,10.0.17134,1834221568,1638297,520158868",
				"",
				"DESKTOP-MPESIIJ,C:\\dev\\eclipse\\jee-oxygen\\eclipse\\eclipse.exe -data file:/C:/Users/Liferay/eclipse-workspace-devtool/ -os win32 -ws win32 -arch x86_64 -launcher C:\\dev\\eclipse\\jee-oxygen\\eclipse\\eclipse.exe -name Eclipse --launcher.library C:\\Users\\Liferay\\.p2\\pool\\plugins\\org.eclipse.equinox.launcher.win32.win32.x86_64_1.1.551.v20171108-1834\\eclipse_1630.dll -startup C:\\dev\\eclipse\\jee-oxygen\\eclipse\\\\plugins/org.eclipse.equinox.launcher_1.4.0.v20161219-1356.jar --launcher.appendVmargs -product org.eclipse.epp.package.jee.product -vm C:\\dev\\jdk1.8.0_151\\bin\\..\\jre\\bin\\server\\jvm.dll -vmargs -Dosgi.requiredJavaVersion=1.8 -Dosgi.instance.area.default=@user.home/eclipse-workspace -XX:MaxPermSize=384m -XX:+UseG1GC -XX:+UseStringDeduplication -Dosgi.requiredJavaVersion=1.8 -Xms3000m -Xmx3000m -Declipse.p2.max.threads=10 -Doomph.update.url=http://download.eclipse.org/oomph/updates/milestone/latest -Doomph.redirection.index.redirection=index:/-&gt;http://git.eclipse.org/c/oomph/org.eclipse.oomph.git/plain/setups/ ,DESKTOP-MPESIIJ,eclipse.exe,C:\\dev\\eclipse\\jee-oxygen\\eclipse\\eclipse.exe,,4260,1541,,2050312500,1380,200,eclipse.exe,Microsoft Windows 10 Pro|C:\\WINDOWS|\\Device\\Harddisk0\\Partition3,751238,11084612,3178538,3743476,1240,3754300,44266545152,2633108,8,3833319424,4260,184,782,206,835,277348,760758653,4,,,96,4624218750,44248616960,10.0.17134,2558455808,16338,55313545",
				""
		}));
		
		List<Map<String, String>> parsedResult = processWmicOutput(output);
		
		assertThat(parsedResult.size(), equalTo(2));

		assertThat(parsedResult.get(0).get("CommandLine"), equalTo( "\"C:\\dev\\jdk1.8.0_151\\bin\\java.exe\"   -Djava.util.logging.config.file=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\conf\\logging.properties\" -Djava.util.logging.manager=org.apache.juli.ClassLoaderLogManager -agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n  -Dfile.encoding=UTF8 -Djava.net.preferIPv4Stack=true -Dorg.apache.catalina.loader.WebappClassLoader.ENABLE_CLEAR_REFERENCES=false -Duser.timezone=GMT -Xmx4000m -XX:MaxPermSize=512m  -Djava.endorsed.dirs=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\endorsed\" -classpath \"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\bin\\bootstrap.jar;c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\bin\\tomcat-juli.jar\" -Dcatalina.base=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\" -Dcatalina.home=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\" -Djava.io.tmpdir=\"c:\\Users\\Liferay\\bundles_ee\\tomcat-8.0.32\\temp\" org.apache.catalina.startup.Bootstrap  start"));
		assertThat(parsedResult.get(0).get("ParentProcessId"), equalTo("16328"));
		assertThat(parsedResult.get(0).get("ProcessId"), equalTo("16696"));
	}

	@Test
	public void test_processNetstatOutput() throws Exception {
		List<String> output = new ArrayList<>(Arrays.asList(new String[] {
				"",
				"Active Connections",
				"",
				"  Proto  Local Address          Foreign Address        State           PID",
				"  TCP    0.0.0.0:135            0.0.0.0:0              LISTENING       548",
				"",
				"  TCP    0.0.0.0:623            0.0.0.0:0              LISTENING       10668",
				"  TCP    0.0.0.0:8080           0.0.0.0:0              LISTENING       16696",
				"  TCP    0.0.0.0:8000           0.0.0.0:0              LISTENING       16696",
				"  UDP    192.168.106.1:53503    *:*                                    3464",
				"  TCP    192.168.0.103:57075    172.217.23.195:443     CLOSE_WAIT      12548",
				"  UDP    0.0.0.0:5005           *:*                                    6796",
				"  UDP    [::1]:51958            *:*                                    3464",
				"  UDP    [fe80::a11f:d3fa:57b5:a898%3]:1900  *:*                                    3464",
				"  UDP    [fe80::5914:e6c9:d46f:aebd%21]:1900  *:*                                    3464",
				"",
				""
		}));
		
		List<Map<String, String>> parsedResult = processNetstatOutput(output);
				
		assertThat(parsedResult.size(), equalTo(10));

		Set<String> header = new HashSet<>(Arrays.asList(new String[] {"Proto","Local Address","Foreign Address","State","PID"}));
		
		for (Map<String,String> record : parsedResult) {
			assertThat(record.keySet(), equalTo(header));
		}
		
		assertThat(parsedResult.get(2).get("Proto"), equalTo("TCP"));
		assertThat(parsedResult.get(2).get("Local Address"), equalTo("0.0.0.0:8080"));
		assertThat(parsedResult.get(2).get("Foreign Address"), equalTo("0.0.0.0:0"));
		assertThat(parsedResult.get(2).get("State"), equalTo("LISTENING"));
		assertThat(parsedResult.get(2).get("PID"), equalTo("16696"));

		assertThat(parsedResult.get(4).get("Proto"), equalTo("UDP"));
		assertThat(parsedResult.get(4).get("Local Address"), equalTo("192.168.106.1:53503"));
		assertThat(parsedResult.get(4).get("Foreign Address"), equalTo("*:*"));
		assertThat(parsedResult.get(4).get("State"), CoreMatchers.nullValue());
		assertThat(parsedResult.get(4).get("PID"), equalTo("3464"));
	}	
}
