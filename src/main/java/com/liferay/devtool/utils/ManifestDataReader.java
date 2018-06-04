package com.liferay.devtool.utils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ManifestDataReader {
	private Set<String> watchKeys = new HashSet<>();
	private Map<String,String> data = new HashMap<>();
	private String currentKey = null;
	private StringBuffer currentValue = null;
	
	public void processLine(String line) {
		if (isKeyStart(line)) {
			addPreviousValue();
			String[] split = StringUtils.splitFirst(line, ": ");
			if (split.length >= 2) {
				String key = split[0].trim();
				if (watchKeys.contains(key)) {
					currentKey = key;
					currentValue = new StringBuffer(split[1]);
				}
			}
		} else if (isValueNextLine(line)) {
			if (currentValue != null) {
				currentValue.append(line.substring(1));
			}
		}		
	}

	private void addPreviousValue() {
		if (currentKey != null && currentValue != null) {
			data.put(currentKey, currentValue.toString());
		}
		currentKey = null;
		currentValue = null;
	}

	private boolean isKeyStart(String line) {
		return line != null && line.length() >= 1 && !line.startsWith(" ");
	}

	private boolean isValueNextLine(String line) {
		return line != null && line.length() >= 1 && line.startsWith(" ");
	}
	
	public Map<String, String> getData() {
		addPreviousValue();
		return data;
	}

	public void setWatchKeys(Set<String> watchKeys) {
		this.watchKeys = watchKeys;
	}
}
