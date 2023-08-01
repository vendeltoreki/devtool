package fixenv.util;

import java.util.Map;

public class MySqlUtil {
	private static final String DRIVER_CLASS_NAME = "jdbc.default.driverClassName";
	private static final String URL = "jdbc.default.url";
	private static final String USERNAME = "jdbc.default.username";
	private static final String PASSWORD = "jdbc.default.password";
	
	private String driverClassName;
	private String url;
	private String userName;
	private String password;
	private String schemaName;
	
	public void init(Map<String,String> props) {
		driverClassName = props.get(DRIVER_CLASS_NAME);
		url = props.get(URL);
		userName = props.get(USERNAME);
		password = props.get(PASSWORD);

		schemaName = StringUtils.extractSchemaNameFromMySqlUrl(url);
		
		System.out.println("-- DB Configuration --------------------------------");
		System.out.println(DRIVER_CLASS_NAME+"="+driverClassName);
		System.out.println(URL+"="+url);
		System.out.println(USERNAME+"="+userName);
		System.out.println(PASSWORD+"="+password);
		System.out.println("schema name: "+schemaName);

		if (!isLocalMySql()) {
			System.out.println("DB is a not local MySQL instance!");
		}
	}
	
	public void executeSQLCommand(String command) {
		executeCommand("\""+ConfigUtil.getMySqlBinPath()+"mysql\" -u "+userName+" -p"+password+" -e \""+command+"\"");
	}

	public void executeCommand(String command) {
		System.out.println("Executing command: "+command);
		
		SimpleCommand cmd = new SimpleCommand();
		//cmd.run("cmd.exe /c '"+command+"'");
		cmd.run(command);
		
		if (!cmd.isSuccess()) {
			System.out.println("Exit value: "+cmd.getExitValue());
			for (String line : cmd.getStdOut()) {
				System.out.println(line);
			}
	
			for (String line : cmd.getStdErr()) {
				System.out.println(line);
			}
		}
	}
	
	public boolean isLocalMySql() {
		return driverClassName.startsWith("com.mysql") &&
			url.startsWith("jdbc:mysql://localhost");
	}

	public void saveDump(String sqlFilePath) {
		/*
		mysqldump.exe --defaults-file="tmpyuqalp.cnf"  --user=root --host=localhost --protocol=tcp --port=3306 --default-character-set=utf8 --column-statistics=0 --skip-triggers "lportal_62"
		mysqldump -u root -pmypassword pos > d:\pos.sql
		C:\Program Files\MySQL\MySQL Server 5.7\bin\mysqldump.exe
		"C:\Program Files\MySQL\MySQL Server 5.6\bin\mysql" -u root -ppassword -e "DROP DATABASE `lportal_master`;"
		"C:\Program Files\MySQL\MySQL Server 5.6\bin\mysql" -u root -ppassword -e "CREATE SCHEMA `lportal_master`;"
		*/
		
		executeCommand("\""+ConfigUtil.getMySqlBinPath()+"mysqldump\" -u "+userName+" -p"+password+" "+schemaName+" > \""+sqlFilePath+"\"");
	}

	public void cleanSchema() {
		/*
		"C:\Program Files\MySQL\MySQL Server 5.6\bin\mysql" -u root -ppassword -e "DROP DATABASE `lportal_master`;"
		"C:\Program Files\MySQL\MySQL Server 5.6\bin\mysql" -u root -ppassword -e "CREATE SCHEMA `lportal_master`;"
		*/
		
		executeSQLCommand("DROP DATABASE `"+schemaName+"`;");

		if (ConfigUtil.isMySqlCreateSchemaOptionsDefined()) {
			executeSQLCommand("CREATE SCHEMA `"+schemaName+"` "+ConfigUtil.getMySqlCreateSchemaOptions()+" ;");
		} else {
			executeSQLCommand("CREATE SCHEMA `"+schemaName+"`;");
		}
	}
}
