package com.liferay.devtool.utils;

import java.util.regex.Pattern;

public class StringUtils {
	public static String replacePathParam(String text, String paramName, String paramValue) {
		// app.server.parent.dir=${project.dir}/../bundles_ee
		
		String escapedName = Pattern.quote(paramName);
		String escapedValue = paramValue.replaceAll("\\\\", "\\\\\\\\");
		
		//System.out.println("\""+escapedName+"\", \""+escapedValue+"\"");
		
		return text.replaceAll(escapedName, escapedValue);
	}
	
}
