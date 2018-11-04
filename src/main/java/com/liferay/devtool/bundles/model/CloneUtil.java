package com.liferay.devtool.bundles.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.liferay.devtool.bundles.GitRepoEntry;
import com.liferay.devtool.bundles.TempDirEntry;

public class CloneUtil {

	public static List<GitRepoEntry> cloneGitRepoList(List<GitRepoEntry> list) {
		if (list == null) {
			return null;
		}
		
		List<GitRepoEntry> res = new ArrayList<>(list.size());
		for (GitRepoEntry entry : list) {
			res.add(new GitRepoEntry(entry));
		}
		return res;
	}

	public static List<TempDirEntry> cloneTempDirList(List<TempDirEntry> list) {
		if (list == null) {
			return null;
		}
		
		List<TempDirEntry> res = new ArrayList<>(list.size());
		for (TempDirEntry entry : list) {
			res.add(new TempDirEntry(entry));
		}
		
		return res;
	}

	public static Map<String, String> cloneStringStringMap(Map<String, String> src) {
		if (src == null) {
			return null;
		}
		
		Map<String,String> res = new HashMap<>();
		for (String key : src.keySet()) {
			res.put(key, src.get(key));
		}
		
		return res;
	}

	public static Set<String> cloneStringSet(Set<String> src) {
		if (src == null) {
			return null;
		}
		
		Set<String> res = new HashSet<>();
		for (String entry : src) {
			res.add(entry);
		}
		
		return res;
	}

	public static List<String> cloneStringList(List<String> src) {
		if (src == null) {
			return null;
		}
		
		List<String> res = new ArrayList<>(src.size());
		for (String entry : src) {
			res.add(entry);
		}
		
		return res;
	}

}
