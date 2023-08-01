package fixenv;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import fixenv.util.FileUtil;

public class PurgeEnv {
	private String bundleRoot;
	private boolean all = false;
	
	public void setBundleRoot(String bundleRoot) {
		this.bundleRoot = bundleRoot;
	}

	public void setAll(boolean all) {
		this.all = all;
	}

	public void execute() {
		cleanDir(FileUtil.resolvePath(bundleRoot+"/osgi/state"));
		cleanDir(FileUtil.resolvePath(bundleRoot+"/data"));
		
		if (all) {
			cleanDir(FileUtil.resolvePath(bundleRoot+"/work"));
			cleanDir(FileUtil.resolvePath(bundleRoot+"/logs"));
			cleanDir(FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/work"));
			cleanDir(FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/temp"));
			cleanDir(FileUtil.resolvePath(bundleRoot+"/[TOMCAT]/logs"));
		}
	}

	private void cleanDir(String dirPath) {
		System.out.println("Cleaning directory: "+dirPath);
		
		try {
			File dir = new File(dirPath);
			
			if (dir.exists()) {
				Path pathToBeDeleted = dir.toPath();
			
				Files.walk(pathToBeDeleted)
					.filter(file -> !file.equals(pathToBeDeleted))
				    .sorted(Comparator.reverseOrder())
				    .map(Path::toFile)
				    //.forEach(f -> System.out.println("DEL:"+f));
				    .forEach(File::delete);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
}
