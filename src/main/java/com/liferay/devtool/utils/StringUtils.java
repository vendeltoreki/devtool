package com.liferay.devtool.utils;

import java.util.Arrays;
import java.util.Collection;
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

}
