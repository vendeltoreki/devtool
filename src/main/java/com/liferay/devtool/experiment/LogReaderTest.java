package com.liferay.devtool.experiment;

import com.liferay.devtool.utils.LogReader;

public class LogReaderTest {

	public static void main(String[] args) {
		LogReader logReader = new LogReader();
		
		long t1 = System.currentTimeMillis();
		logReader.readDir("C:\\liferay\\bundles\\liferay-dxp-digital-enterprise-7.0-sp4\\tomcat-8.0.32\\logs");
		logReader.readDir("c:\\Users\\Liferay\\bundles\\tomcat-9.0.10\\logs\\");
		
		System.out.println("TOTAL TIME: "+(System.currentTimeMillis() - t1)+" ms");
		
		System.out.println("last start time: "+logReader.getLatestStartup());
	}

}
