package com.liferay.devtool.bundles.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

import com.liferay.devtool.bundles.BundleEntry;
import com.liferay.devtool.bundles.PatchingToolEntry;
import com.liferay.devtool.utils.JarManifestReader;
import com.liferay.devtool.utils.SysEnv;

public class PatchingToolReader {
	private SysEnv sysEnv;
	private BundleEntry bundleEntry;
	private PatchingToolEntry patchingToolEntry;
	
	public SysEnv getSysEnv() {
		return sysEnv;
	}

	public void setSysEnv(SysEnv sysEnv) {
		this.sysEnv = sysEnv;
	}

	public BundleEntry getBundleEntry() {
		return bundleEntry;
	}

	public void setBundleEntry(BundleEntry bundleEntry) {
		this.bundleEntry = bundleEntry;
	}

	public void readDetails() {
		String jarPath = bundleEntry.getRootDirPath()+"\\patching-tool\\lib\\patching-tool.jar";
		
		File jarFile = sysEnv.createFile(jarPath);
		if (jarFile.exists() && jarFile.isFile()) {
			patchingToolEntry = new PatchingToolEntry();
			patchingToolEntry.setRootDirPath(bundleEntry.getRootDirPath()+"\\patching-tool");
			readJarFile(jarFile);
			
			patchingToolEntry.setInternal(checkIfInternal(patchingToolEntry.getRootDirPath()));

			String propertiesFilePath = patchingToolEntry.getRootDirPath()+"\\default_src.properties";
			readPropertiesFile(propertiesFilePath);

			bundleEntry.setPatchingToolEntry(patchingToolEntry);
		}
	}

	private boolean checkIfInternal(String rootDirPath) {
		String internalLibPath = rootDirPath + "\\lib\\internal-modules";
		File internalLibDir = sysEnv.createFile(internalLibPath);
		
		return 
				internalLibDir.exists() && 
				internalLibDir.isDirectory() && 
				containsFiles(internalLibDir, "patching-tool-internal-modules.*\\.jar");
	}

	private boolean containsFiles(File internalLibDir, String regex) {
		for (File file : internalLibDir.listFiles()) {
			if (file.isFile()) {
				if (file.getName().matches(regex)) {
					return true;
				}
			}
		}
		return false;
	}

	private void readJarFile(File file) {
		JarManifestReader jarManifestReader = new JarManifestReader(); 

		/*
			Patching-Tool-Version: 2007
			Patching-Tool-Version-Display-Name: 2.0.7
			Patching-Tool-Build: 15
		 */
		jarManifestReader.addKeys(
			"Patching-Tool-Version",
			"Patching-Tool-Version-Display-Name",
			"Patching-Tool-Build");
		
		Map<String,String> res = jarManifestReader.readJarFile(file); 
		
		if (res.containsKey("Patching-Tool-Version-Display-Name")) {
			patchingToolEntry.setVersion(res.get("Patching-Tool-Version-Display-Name"));
		} else if (res.containsKey("Patching-Tool-Version")) {
			patchingToolEntry.setVersion(res.get("Patching-Tool-Version"));
		}
		
		if (res.containsKey("Patching-Tool-Build")) {
			patchingToolEntry.setBuild(res.get("Patching-Tool-Build"));
		}
		
	}
	
	private void readPropertiesFile(String propertiesFilePath) {
		Properties prop = new Properties();
		InputStream input = null;

		try {
			File propertiesFile = new File(propertiesFilePath);
			if (propertiesFile.exists() && propertiesFile.isFile()) { 
				input = new FileInputStream(propertiesFilePath);
				
				prop.load(input);

				/*patching.mode=source
				jdk.version=jdk8
				source.path=c:/liferay/src/de-33/  */
						
				String patchingMode = prop.getProperty("patching.mode");
				
				if (patchingMode.equals("source")) {
					String sourcePath = prop.getProperty("source.path");
					
					Path path = FileSystems.getDefault().getPath(sourcePath);
					if (!path.isAbsolute()) {
						Path rootPath = FileSystems.getDefault().getPath(propertiesFile.getParentFile().getAbsolutePath());
						sourcePath = rootPath.resolve(path).normalize().toAbsolutePath().toString();
					}
					System.out.println("SRC="+sourcePath);
					patchingToolEntry.setSourcePath(sourcePath);
				}
			}
		} catch (Exception ex) {
			System.err.println("Error while reading properties file: "+propertiesFilePath);
			ex.printStackTrace();
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
