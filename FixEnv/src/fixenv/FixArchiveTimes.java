package fixenv;

import java.io.File;
import java.util.Calendar;

import fixenv.util.ArchiveProcessor;
import fixenv.util.FileUtil;

public class FixArchiveTimes {
	private String bundleRoot;

	public void setBundleRoot(String bundleRoot) {
		this.bundleRoot = bundleRoot;
	}

	public void execute() {
		//checkFilesInDir("/osgi/war");
		//checkFilesInDir("/osgi/modules");
		//checkFilesInDir("/deploy");
	}
	
	public void checkFilesInDir(String dir) {
			
		File warDeployDir = new File(FileUtil.resolvePath(bundleRoot + dir));
		
		if (!warDeployDir.exists() || !warDeployDir.isDirectory()) {
			System.out.println("Not a dir: "+warDeployDir);
			return;
		}

		Calendar cal = Calendar.getInstance();
		
		long gmtOffsetMillis = cal.get(Calendar.ZONE_OFFSET);
		gmtOffsetMillis += 1000*60*60;
		
		System.out.println("Checking files in WAR deploy dir: "+warDeployDir+" (GMT offset: "+(gmtOffsetMillis/1000/60/60)+" hrs)");

		
		boolean updated = false;
		for (File file : warDeployDir.listFiles()) {
			if (
				!file.getName().toLowerCase().endsWith(".war") &&
				!file.getName().toLowerCase().endsWith(".jar")
			) {
				continue;
			}
			
			try {
				ArchiveProcessor archiveProcessor = new ArchiveProcessor();
				//archiveProcessor.setTargetTime(System.currentTimeMillis() - (1000*60*60*10));
				archiveProcessor.setTargetTime(System.currentTimeMillis() - gmtOffsetMillis);
				
				if (archiveProcessor.checkFile(file.getAbsolutePath())) {
					System.out.println("Processing WAR file: "+file.getName());
					archiveProcessor.process(file.getAbsolutePath());
					
					updated = true;
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		if (!updated) {
			System.out.println("All WAR files OK!");
		}
	}

}
