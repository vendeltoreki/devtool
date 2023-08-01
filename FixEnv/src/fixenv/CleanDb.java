package fixenv;

import java.util.Map;

import fixenv.util.FileUtil;
import fixenv.util.MySqlUtil;
import fixenv.util.PropertiesFileReader;

public class CleanDb {
	private String bundleRoot;
	
	public void setBundleRoot(String bundleRoot) {
		this.bundleRoot = bundleRoot;
	}

	public void execute() {
		Map<String,String> props = PropertiesFileReader.read(FileUtil.resolvePath(bundleRoot+"/portal-ext.properties"));
		
		MySqlUtil mySqlUtil = new MySqlUtil();

		mySqlUtil.init(props);
		
		if (!mySqlUtil.isLocalMySql()) {
			return;
		}
		
		mySqlUtil.cleanSchema();
		
	}
}
