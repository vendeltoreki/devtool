package fixenv.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

public class PropertiesFileReader {
	public static Map<String,String> read(String propertiesFilePath) {
		Properties prop = new Properties();
		InputStream input = null;
		
		Map<String,String> res = new HashMap<>();

		try {
			File propertiesFile = new File(propertiesFilePath);
			if (propertiesFile.exists() && propertiesFile.isFile()) { 
				input = new FileInputStream(propertiesFilePath);
				
				prop.load(input);
				
				for (Entry<Object,Object> entry : prop.entrySet()) {
					String key = String.valueOf(entry.getKey());
					String value = String.valueOf(entry.getValue());
					
					res.put(key, value);
				}
				/*bundleEntry.setDbDriverClass(prop.getProperty("jdbc.default.driverClassName"));
				bundleEntry.setDbUrl(prop.getProperty("jdbc.default.url"));
				bundleEntry.setDbUsername(prop.getProperty("jdbc.default.username"));
				bundleEntry.setDbPassword(prop.getProperty("jdbc.default.password"));*/
			}
		} catch (IOException ex) {
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return res;
	}

}
