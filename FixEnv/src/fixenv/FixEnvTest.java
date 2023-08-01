package fixenv;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;

import fixenv.util.ArchiveProcessor;

public class FixEnvTest {

	public static void main(String[] args) throws Exception {
		//testPath();
		//testTimeout();
		//testPort();
		//testSetenv();
		testThemeRead();
	}
	
	private static void testThemeRead() throws Exception {
		Calendar cal = Calendar.getInstance();
		long milliDiff = cal.get(Calendar.ZONE_OFFSET);

		System.out.println("diff: "+milliDiff);
		String filePath = "C:\\Users\\Liferay\\bundles\\osgi\\war\\classic-theme.war";
		
		ArchiveProcessor themeProcessor = new ArchiveProcessor();
		themeProcessor.process(filePath);
	}

	public static void testPath() throws Exception {
		String current = new java.io.File( "." ).getCanonicalPath();
		System.out.println("Current dir: "+current);
		
		String currentDir = System.getProperty("user.dir");
		System.out.println("Current dir using System: " +currentDir);
		
		Path currentRelativePath = Paths.get("");
		String s = currentRelativePath.toAbsolutePath().toString();
		System.out.println("Current relative path: " + s);
		
		String srcFile = FixEnvMain.class
				.getProtectionDomain()
				.getCodeSource()
				.getLocation()
				.getFile();
		
		System.out.println("Current relative path: " + srcFile);
		
	}

	public static void testTimeout() {
		FixTimeOut fixTimeOut = new FixTimeOut();
		//fixTimeOut.setBundleRoot("C:\\Users\\Liferay\\bundles_ee");
		fixTimeOut.setBundleRoot("C:\\liferay\\bundles\\dxp-11-2\\liferay-dxp-7.1.10.2-sp2\\");
		fixTimeOut.execute();
	}

	public static void testPort() {
		/*FixPort fixPort = new FixPort();
		fixPort.setBundleRoot("C:\\Users\\Liferay\\bundles_remote");
		fixPort.execute();*/
		
		FixPort fixPort = new FixPort();
		fixPort.setBundleRoot("C:\\liferay\\bundles\\de-80\\liferay-dxp-digital-enterprise-7.0.10.11-sp11");
		//fixPort.info();
		
		fixPort.setPortPrefix("71");
		fixPort.execute();
	}

	public static void testSetenv() {
		FixSetenv fixSetenv = new FixSetenv();
		fixSetenv.setBundleRoot("C:\\Users\\Liferay\\bundles");
		fixSetenv.execute();
	}
	
}
