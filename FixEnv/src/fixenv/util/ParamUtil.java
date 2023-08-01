package fixenv.util;

public class ParamUtil {
	private String bundleName;
	private boolean info;
	private boolean fix = true;
	private boolean help;
	private boolean purge;
	private boolean cleanDb;
	private boolean saveDump;
	private boolean upgrade;
	private boolean reset;
	private Integer updatePort;
	private String portPrefix;
	private String dumpName;
	private boolean all;
	
	public ParamUtil(String[] params) {
		parse(params);
	}

	public void parse(String[] params) {
		if (params != null) {
			for (String param : params) {
				if (param != null && param.trim().length() > 0) {
					if (param.equals("info")) {
						info = true;
						fix = false;
					} else if (param.equals("help")) {
						help = true;
						fix = false;
					} else if (param.equals("purge")) {
						purge = true;
						fix = false;
					} else if (param.equals("cleandb")) {
						cleanDb = true;
						fix = false;
					} else if (param.equals("savedump")) {
						saveDump = true;
						fix = false;
					} else if (param.equals("upgrade")) {
						upgrade = true;
						fix = false;
					} else if (param.equals("reset")) {
						reset = true;
						fix = false;
					} else if (param.startsWith("-")) {
						processParam(param);
					} else {
						bundleName = param;
					}
				}
			}
		}
	}
	
	private void processParam(String param) {
		if (param.startsWith("-p")) {
			Integer port = null;
			
			try {
				String portStr = param.substring(2);
			
				if (portStr.length() > 0 && portStr.length() <= 2) {
					portPrefix = portStr;
				} else {
					port = Integer.parseInt(portStr);
				}
			} catch (Exception ex) { /* ignore */ }
			
			if (port != null || portPrefix != null) {
				updatePort = port;
			}
		} else if (param.startsWith("-dn")) {
			dumpName = param.substring(3);
		} else if (param.equals("-all")) {
			all = true;
		}
	}

	public boolean isInfo() {
		return info;
	}
	
	public boolean isFix() {
		return fix;
	}
	
	public boolean isHelp() {
		return help;
	}

	public boolean isAll() {
		return all;
	}

	public boolean isReset() {
		return reset;
	}

	public String getBundleName() {
		return bundleName;
	}

	public boolean isPurge() {
		return purge;
	}

	public boolean isCleanDb() {
		return cleanDb;
	}

	public boolean isSaveDump() {
		return saveDump;
	}

	public boolean isUpgrade() {
		return upgrade;
	}

	public void setUpgrade(boolean upgrade) {
		this.upgrade = upgrade;
	}

	public Integer getUpdatePort() {
		return updatePort;
	}

	public String getPortPrefix() {
		return portPrefix;
	}

	public String getDumpName() {
		return dumpName;
	}
}
