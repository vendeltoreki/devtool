package com.liferay.devtool.experiment;

import com.liferay.devtool.utils.LogReader;

public class LogReaderTest {

	public static void main(String[] args) {
		LogReader logReader = new LogReader();
		//logReader.read("C:\\liferay\\bundles\\liferay-dxp-digital-enterprise-7.0-sp4\\tomcat-8.0.32\\logs\\catalina.2018-02-19.log");
		logReader.readDir("C:\\liferay\\bundles\\liferay-dxp-digital-enterprise-7.0-sp4\\tomcat-8.0.32\\logs");
	}

}
