package com.liferay.devtool.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	public static String replacePathParam(String text, String paramName, String paramValue) {
		// app.server.parent.dir=${project.dir}/../bundles_ee
		
		String escapedName = Pattern.quote(paramName);
		String escapedValue = paramValue.replaceAll("\\\\", "\\\\\\\\");
		
		//System.out.println("\""+escapedName+"\", \""+escapedValue+"\"");
		
		return text.replaceAll(escapedName, escapedValue);
	}
	
	public static String[] collapseCsvLine(String[] data, int collapsePos, int targetSize) {
		String[] res = new String[targetSize];
		
		int sourcePos = 0;
		for (int i=0; i<res.length; ++i) {
			if (i == collapsePos) {
				int collapseLength = (data.length - targetSize)+1;
				res[i] = join(Arrays.copyOfRange(data, collapsePos, collapsePos + collapseLength), ",");
				sourcePos += collapseLength;
			} else {
				res[i] = data[sourcePos];
				++sourcePos;
			}
		}
		
		return res;
	}
	
	public static String join(String[] data, String sep) {
		StringBuffer sb = new StringBuffer();
		for (int i=0; i<data.length; ++i) {
			sb.append(data[i]);
			if (i<data.length-1) {
				sb.append(sep);
			}
		}
		
		return sb.toString();
	}

	public static String join(Collection<String> data, String sep) {
		StringBuffer sb = new StringBuffer();
		int i = 0;
		for (String str : data) {
			sb.append(str);
			if (i < data.size() - 1) {
				sb.append(sep);
			}
			++i;
		}
		
		return sb.toString();
	}
	
	public static String[] fillCsvMissingFields(String[] data, int missingPos, int targetSize) {
		if (data.length == targetSize) {
			return data;
		} else if (data.length < targetSize) {
			int missingLength = targetSize - data.length;
					
			String[] res = new String[targetSize];
			for (int i=0; i<missingPos; ++i) {
				res[i] = data[i];
			}
			
			for (int i=missingPos+missingLength; i<targetSize; ++i) {
				res[i] = data[i-missingLength];
			}			
			return res;
		} else {
			return Arrays.copyOfRange(data, 0, targetSize);
		}
	}

	public static String[] splitQuotedCsv(String line) {
		return stripQuotes(line).split("\",\"");
	}

	public static String stripQuotes(String line) {
		String res = line;
		if (res.startsWith("\"")) {
			res = res.substring(1);
		}
		
		if (res.endsWith("\"")) {
			res = res.substring(0, res.length()-1);
		}
		
		return res;
	}

	public static boolean notEmpty(String text) {
		return text != null && text.trim().length() > 0;
	}

	public static String getPort(String address) {
		int n = address.lastIndexOf(":");
		if (n>-1) {
			return address.substring(n+1, address.length());
		} else {
			return null;
		}
	}

	public static String[] splitFirst(String text, String separator) {
		int i = text.indexOf(separator);
		if (i>-1) {
			return new String[] {text.substring(0, i), text.substring(i+separator.length())};
		} else {
			return new String[] {text};
		}
	}

	public static String[] splitLast(String text, String separator) {
		int i = text.lastIndexOf(separator);
		if (i>-1) {
			return new String[] {text.substring(0, i), text.substring(i+separator.length())};
		} else {
			return new String[] {text};
		}
	}
	
	public static String removeParamsFromUrl(String url) {
		int qPos = url.indexOf("?");
		if (qPos > -1) {
			return url.substring(0, qPos);
		} else {
			return url;
		}
		
	}
	
	public static String extractSchemaNameFromMySqlUrl(String url) {
		String mySqlPrefix = "jdbc:mysql://";
		
		if (url.startsWith(mySqlPrefix)) {
			String[] split = splitLast(removeParamsFromUrl(url.substring(mySqlPrefix.length())),"/");
			return split[split.length-1];
		}
		
		return null;
	}

	public static String extractTomcatHomeFromCommand(String command) {
		String res = extractParameterFromJavaCommand(command, "catalina.home");
		
		if (res == null) {
			res = extractParameterFromJavaCommand(command, "catalina.base");
		}
		
		return res;
	}

	public static String extractTimezoneFromCommand(String command) {
		return extractParameterFromJavaCommandNoQuotes(command, "user.timezone");
	}
	
	private static String extractParameterFromJavaCommand(String command, String paramName) {
		// TODO escape paramName
		Pattern pattern = Pattern.compile("-D"+escapeRegexp(paramName)+"=\\\"(.*?)\\\"");
		Matcher matcher = pattern.matcher(command);
		if (matcher.find()) {
		    return matcher.group(1);
		}

		return null;
	}

	private static String extractParameterFromJavaCommandNoQuotes(String command, String paramName) {
		// TODO escape paramName
		Pattern pattern = Pattern.compile("-D"+escapeRegexp(paramName)+"=(.*?) ");
		Matcher matcher = pattern.matcher(command);
		if (matcher.find()) {
		    return matcher.group(1);
		}

		return null;
	}
	
	public static String escapeRegexp(String pattern) {
		return pattern.replaceAll("\\.", "\\\\.");
	}

	public static boolean containsAny(String text, String[] keywords) {
		if (text != null && keywords != null && keywords.length > 0) {
			String lowerCaseText = text.toLowerCase();
			for (String keyword : keywords) {
				if (lowerCaseText.contains(keyword.toLowerCase())) {
					return true;
				}
			}
		}
		
		return false;
	}

	public static String replaceStrings(String text, String find, String replace) {
		if (text == null || text.trim().length() == 0 || find == null || replace == null) {
			return null;
		}
		
		return text.replaceAll(find, replace);
	}

	public static Set<String> createLowerStringSet(String value, String separator) {
		return createLowerStringSet(value.split(separator));
	}

	public static Set<String> createLowerStringSet(String[] values) {
		Set<String> res = new HashSet<>();
		for (String dirName : values) {
			res.add(dirName.trim().toLowerCase());
		}
		return res;
	}

	public static Integer tryParseInt(String value) {
		if (value != null && value.trim().length() > 0) {
			try {
				return Integer.parseInt(value);
			} catch (NumberFormatException nfe) {
				return null;
			}
		} else {
			return null;
		}
	}
	
	public static String truncateString(String text, int maxLength) {
		if (text == null) {
			return null;
		}
		
		if (text.length() <= maxLength) {
			return text;
		} else {
			return text.substring(0, maxLength) + "..";
		}
	}

	public static Date parseTimestamp(String tsString) {
		Date res = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
			res = sdf.parse(tsString);
		} catch (ParseException e) { /* ignore */ }
		return res;
	}

	public static String formatTimestamp(Date tsDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
		return sdf.format(tsDate);
	}
	
	public static Date parseWmicTimestamp(String tsString) {
		String tsPart = null;
		String tzPart = null;
		if (tsString.contains("+")) {
			int p = tsString.lastIndexOf("+");
			tsPart = tsString.substring(0,p);
			tzPart = tsString.substring(p);
		} else if (tsString.contains("-")) {
			int p = tsString.lastIndexOf("-");
			tsPart = tsString.substring(0,p);
			tzPart = tsString.substring(p);
		} else {
			return null;
		}
		
		if (tsPart != null && tzPart != null && tzPart.matches("[\\+\\-][0-9]{3}")) {
			String sign = tzPart.substring(0,1);
			String value = tzPart.substring(1);
			int tzMinutes = tryParseInt(value);
			int tzHours = tzMinutes / 60;
			tzMinutes = tzMinutes % 60;
			
			tzPart = sign + String.format("%02d%02d", tzHours, tzMinutes);
			
			if (tsPart.length() > 18) {
				tsPart = tsPart.substring(0,18);
			}
		}
		
		Date res = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss.SSS Z");
			res = sdf.parse(tsPart + " " + tzPart);
		} catch (ParseException e) { /* ignore */ }
		return res;
	}

	public static Date parseLogTimestamp(String tsString) {
		Date res = null;
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss.SSS Z");
			res = sdf.parse(tsString);
		} catch (ParseException e) { /* ignore */ }
		return res;
	}
}
