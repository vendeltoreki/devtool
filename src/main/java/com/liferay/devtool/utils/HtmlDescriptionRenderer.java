package com.liferay.devtool.utils;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.GitRepoEntry;
import com.liferay.devtool.bundles.TempDirEntry;

public class HtmlDescriptionRenderer {
	private BundleEntry entry;
	
	public String createDescription() {
		StringBuffer sb = new StringBuffer();
		sb.append("<html>");
		sb.append("root dir: " + entry.getRootDir() + "<br>\n");
		sb.append("memory: xmx=" + formatLimitInt(entry.getMemoryXmx(), 4000) + ", perm="
				+ formatLimitInt(entry.getMemoryPermSize(), 512) + "<br>\n");
		
		sb.append("Configured HTTP port: <b>" + entry.getConfiguredServerPorts().get(BundleEntry.PORT_HTTP) + "</b><br>\n");

		//sb.append("tomcat version: " + entry.getTomcatVersion() + "<br>\n");
		sb.append("<ul>");
		sb.append("<li>DB driver: " + formatNotNull(entry.getDbDriverClass()) + "</li>\n");
		sb.append("<li>DB URL: " + formatNotNull(entry.getDbUrl()) + "</li>\n");
		sb.append("<li>DB user: " + formatNotNull(entry.getDbUsername()) + ", password="
				+ formatNotNull(entry.getDbPassword()) + "</li>\n");
		sb.append("</ul>");

		if (entry.getRunningProcess() != null) {
			sb.append("<br>Running process:<br>");
			sb.append("<ul>");
			sb.append("<li>PID: " + entry.getRunningProcess().getPid() + "</li>\n");
			sb.append("<li>exec name: " + entry.getRunningProcess().getExecName() + "</li>\n");
			sb.append("<li>ports: " + StringUtils.join(entry.getRunningProcess().getListeningPorts(), ",") + "</li>\n");
			sb.append("<li>command line: " + StringUtils.replaceStrings(entry.getRunningProcess().getCommandLine(), " -D", "<br>-D") + "</li>\n");
			sb.append("</ul>");
		}

		if (entry.getDbSchemaEntry() != null) {
			sb.append("<br>DB schema:<br>");
			sb.append("<ul>");
			sb.append("<li>schema name: <b>" + entry.getDbSchemaEntry().getSchemaName() + "</b></li>\n");
			sb.append("<li>schema version: " + entry.getDbSchemaEntry().getSchemaVersion() + "</li>\n");
			sb.append("<li>table count: " + entry.getDbSchemaEntry().getTableCount() + "</li>\n");
			if (entry.getDbSchemaEntry().hasDeployedApps()) {
				sb.append("<li>Deployed apps: "+StringUtils.join(entry.getDbSchemaEntry().getDeployedApps(),", ") + "</li>\n");
			}
			if (entry.getDbSchemaEntry().getSizeInMb() != null) {
				sb.append("<li>Size in MB: " + entry.getDbSchemaEntry().getSizeInMb() + "</li>\n");
			}
			
			sb.append("</ul>");
		}

		if (entry.getPatchingToolEntry() != null) {
			sb.append("<br>Patching tool:<br>");
			sb.append("<ul>");
			sb.append("<li>version: <b>" + entry.getPatchingToolEntry().getVersion() + "</b></li>\n");
			sb.append("<li>build: " + entry.getPatchingToolEntry().getBuild() + "</li>\n");
			sb.append("<li>internal: " + entry.getPatchingToolEntry().isInternal() + "</li>\n");
			if (entry.getPatchingToolEntry().getSourcePath() != null) {
				sb.append("<li>source path: " + entry.getPatchingToolEntry().getSourcePath() + "</li>\n");
			}
			sb.append("</ul>");
		}
		
		if (entry.getGitRepos() != null && !entry.getGitRepos().isEmpty()) {
			sb.append("<br>Git Repos:<br>");
			sb.append("<ul>");
			for (GitRepoEntry repo : entry.getGitRepos()) {
				sb.append("<li>"+repo.toString()+"</li>\n");
			}
			sb.append("</ul>");
		}
		
		if (entry.getTempDirs() != null && !entry.getTempDirs().isEmpty()) {
			sb.append("<br>Temp dirs:<br>");
			sb.append("<ul>");
			for (TempDirEntry tempDir : entry.getTempDirs()) {
				sb.append("<li>"+tempDir.getRelativePath()+" -- "+formatMaxLimitLong(tempDir.getTotalSize(), 0) +"</li>\n");
			}
			sb.append("</ul>");
		}
		
		return sb.toString();
	}

	private String formatNotNull(String value) {
		if (value != null) {
			return "" + value;
		} else {
			return "<font color=red>" + value + "</font>";
		}
	}

	private String formatLimitInt(int value, int minLimit) {
		if (value >= minLimit) {
			return "" + value;
		} else {
			return "<font color=red>" + value + "</font>";
		}
	}
	
	private String formatMaxLimitLong(long value, long maxLimit) {
		if (value <= maxLimit) {
			return "" + value;
		} else {
			return "<font color=red>" + value + "</font>";
		}
	}

	public BundleEntry getEntry() {
		return entry;
	}

	public void setEntry(BundleEntry entry) {
		this.entry = entry;
	}
}
