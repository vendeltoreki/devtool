package fixenv;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import fixenv.util.FileUtil;
import fixenv.util.PropertiesFileReader;
import fixenv.util.StringUtils;

public class UpgradeSetup {
	private String bundleRoot;
	
	public void setBundleRoot(String bundleRoot) {
		this.bundleRoot = bundleRoot;
	}

	public void initialize() throws Exception {
		System.out.println("init upgrade");
		replaceAppServerProperties(FileUtil.resolvePath(bundleRoot+"/tools/portal-tools-db-upgrade-client/app-server.properties"));
		replaceUpgradeExtProperties(FileUtil.resolvePath(bundleRoot+"/tools/portal-tools-db-upgrade-client/portal-upgrade-ext.properties"));
		
		Map<String,String> props = PropertiesFileReader.read(FileUtil.resolvePath(bundleRoot+"/portal-ext.properties"));
		replaceDatabaseProperties(FileUtil.resolvePath(bundleRoot+"/tools/portal-tools-db-upgrade-client/portal-upgrade-database.properties"), props);
	}

	private void replaceAppServerProperties(String filePath) throws Exception {
		System.out.println("--processing: \""+filePath+"\"");
		
		List<String> lines = readFileToList(filePath);

		List<String> updatedLines = new ArrayList<>();

		String currentServer = null;
		
		for (String line : lines) {
			if (line.startsWith("## ")) {
				currentServer = line.substring(3).toLowerCase().trim();
			} else if (currentServer != null && currentServer.startsWith("tomcat")) {
				if (line.trim().startsWith("#") && line.contains("=")) {
					line = line.substring(line.indexOf("#")+1);
					System.out.println("replaced: \""+line+"\"");
				}
			}
			
			updatedLines.add(line);
		}
		
		writeListToFile(filePath, updatedLines);		
	}

	private void replaceDatabaseProperties(String filePath, Map<String,String> props) throws Exception {
		System.out.println("--processing: \""+filePath+"\"");

		List<String> lines = readFileToList(filePath);

		List<String> updatedLines = new ArrayList<>();

		String currentServer = null;
		
		for (String line : lines) {
			if (line.startsWith("## ")) {
				currentServer = line.substring(3).toLowerCase().trim();
			} else if (currentServer != null && currentServer.startsWith("mysql")) {
				if (line.trim().startsWith("#")) {
					line = line.substring(line.indexOf("#")+1);
					
					if (line.startsWith("jdbc.default.url=")) {
						String url = props.get("jdbc.default.url");
						String schemaName = StringUtils.extractSchemaNameFromMySqlUrl(url);
						
						line = line.replace("lportal", schemaName);
						System.out.println("replaced: \""+line+"\"");
					} else if (line.startsWith("jdbc.default.username=")) {
						line = line + props.get("jdbc.default.username");
						System.out.println("replaced: \""+line+"\"");
					} else if (line.startsWith("jdbc.default.password=")) {
						line = line + props.get("jdbc.default.password");
						System.out.println("replaced: \""+line+"\"");
					}
				}
			}
			
			updatedLines.add(line);
		}
		
		writeListToFile(filePath, updatedLines);			
	}

	private void replaceUpgradeExtProperties(String filePath) throws Exception {
		System.out.println("--processing: \""+filePath+"\"");
		
		List<String> lines = readFileToList(filePath);

		List<String> updatedLines = new ArrayList<>();

		for (String line : lines) {
			if (line.startsWith("#liferay.home=")) {
				line = line.substring(1);
				System.out.println("replaced: \""+line+"\"");
			}
			
			updatedLines.add(line);
		}
		
		writeListToFile(filePath, updatedLines);		
	}

	private List<String> readFileToList(String filePath) throws IOException {
		return Files.readAllLines(new File(filePath).toPath(), Charset.defaultCharset() );
	}

	private void writeListToFile(String filePath, List<String> lines) throws IOException {
		Files.write(new File(filePath).toPath(), lines, Charset.defaultCharset());
	}
	
}
