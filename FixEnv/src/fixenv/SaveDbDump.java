package fixenv;

import java.util.Map;

import fixenv.util.ConfigUtil;
import fixenv.util.FileUtil;
import fixenv.util.MySqlUtil;
import fixenv.util.PropertiesFileReader;

public class SaveDbDump {
	private String bundleRoot;
	private String dumpName;
	
	public void setBundleRoot(String bundleRoot) {
		this.bundleRoot = bundleRoot;
	}

	public void setDumpName(String dumpName) {
		this.dumpName = dumpName;
	}

	public void execute() {
		Map<String,String> props = PropertiesFileReader.read(FileUtil.resolvePath(bundleRoot+"/portal-ext.properties"));

		MySqlUtil mySqlUtil = new MySqlUtil();

		mySqlUtil.init(props);
		
		if (!mySqlUtil.isLocalMySql()) {
			return;
		}
		
		mySqlUtil.saveDump(ConfigUtil.getMySqlDumpPath()+"\\"+dumpName+".sql");
	}
	
	
}
