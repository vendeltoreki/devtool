package fixenv.util;

import java.io.File;
import java.util.Map;

public class ConfigUtil {
	private static final String MYSQL_BIN_PATH = "mysql.bin.path";
	private static final String MYSQL_BIN_PATH_DEFAULT = "C:\\Program Files\\MySQL\\MySQL Server 5.6\\bin\\";
	private static final String MYSQL_DUMP_PATH = "mysql.dump.path";
	private static final String MYSQL_DUMP_PATH_DEFAULT = "C:\\Users\\Liferay\\Documents\\dumps\\";
	private static final String MYSQL_CREATE_SCHEMA_OPTIONS = "mysql.create.schema.options";
	private static final String MYSQL_CREATE_SCHEMA_OPTIONS_DEFAULT = null;
	
	private static Map<String, String> config;
	
	public static Map<String, String> getConfig() {
		if (config == null) {
			config = PropertiesFileReader.read(FileUtil.getJarParentDirPath() + File.separator + "FixEnv.properties");
		}
		
		return config;
	}
	
	public static String getMySqlBinPath() {
		String path = getConfig().get(MYSQL_BIN_PATH);
		
		if (path != null) {
			path = FileUtil.replacePathSeparator(path);
		} else {
			path = MYSQL_BIN_PATH_DEFAULT;
		}

		return path;
	}

	public static String getMySqlDumpPath() {
		String path = getConfig().get(MYSQL_DUMP_PATH);
		
		if (path != null) {
			path = FileUtil.replacePathSeparator(path);
		} else {
			path = MYSQL_DUMP_PATH_DEFAULT;
		}

		return path;
	}

	public static String getMySqlCreateSchemaOptions() {
		String value = getConfig().get(MYSQL_CREATE_SCHEMA_OPTIONS);
		
		if (value != null) {
			return value;
		} else {
			return MYSQL_CREATE_SCHEMA_OPTIONS_DEFAULT;
		}
	}
	
	public static boolean isMySqlCreateSchemaOptionsDefined() {
		return getMySqlCreateSchemaOptions() != null && getMySqlCreateSchemaOptions().trim().length() > 0;
	}
}
