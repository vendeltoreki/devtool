package com.liferay.devtool.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

public class ManifestDataReaderUnitTest {

	private ManifestDataReader manifestDataReader;

	@Before
	public void init() throws Exception {
		manifestDataReader = new ManifestDataReader();
	}

	@Test
	public void test_processLine_singleLine() throws Exception {
		addWatchKeys("ABC");
		processLines("ABC: DEF");
		assertThat(getResult(), equalTo(createMap("ABC","DEF")));
	}

	@Test
	public void test_processLine_singleLine_filter() throws Exception {
		addWatchKeys("ABC");
		processLines("ABC: DEF", "GHI: JKL");
		assertThat(getResult(), equalTo(createMap("ABC","DEF")));
	}

	@Test
	public void test_processLine_singleLine_filterMultiple() throws Exception {
		addWatchKeys("ABC","GHI");
		processLines("ABC: DEF", "GHI: JKL");
		assertThat(getResult(), equalTo(createMap(
				"ABC","DEF",
				"GHI","JKL"
				)));
	}

	@Test
	public void test_processLine_multiLine_filterMultiple() throws Exception {
		addWatchKeys("ABC","GHI");
		processLines(
				"ABC: DEF",
				" QWERTY",
				" ASDFGH",
				"GHI: JKL"
				);
		assertThat(getResult(), equalTo(createMap(
				"ABC","DEFQWERTYASDFGH",
				"GHI","JKL"
				)));
	}

	@Test
	public void test_processLine_multiLine_filterMultiple_2() throws Exception {
		addWatchKeys("ABC","GHI");
		processLines(
				"ABC: DEF",
				" QWERTY",
				" ASDFGH",
				"MNO: PQR",
				"GHI: JKL"
				);
		assertThat(getResult(), equalTo(createMap(
				"ABC","DEFQWERTYASDFGH",
				"GHI","JKL"
				)));
	}	

	@Test
	public void test_processLine_multiLine_filterMultiple_lineBreakAtEnd() throws Exception {
		addWatchKeys("ABC","GHI");
		processLines(
				"ABC: DEF",
				" QWERTY",
				" ASDFGH",
				"MNO: PQR",
				"GHI: JKL",
				""
				);
		assertThat(getResult(), equalTo(createMap(
				"ABC","DEFQWERTYASDFGH",
				"GHI","JKL"
				)));
	}	

	@Test
	public void test_processLine_multiLine_filterMultiple_spaceInValues() throws Exception {
		addWatchKeys("ABC","GHI");
		processLines(
				"ABC: DEF ",
				" QWERTY ",
				" ASDFGH ",
				"MNO: PQR ",
				"GHI: JKL ",
				""
				);
		assertThat(getResult(), equalTo(createMap(
				"ABC","DEF QWERTY ASDFGH ",
				"GHI","JKL "
				)));
	}	
	
	private void addWatchKeys(String... watchKeys) {
		manifestDataReader.setWatchKeys(new HashSet<>(Arrays.asList(watchKeys)));
	}

	private void processLines(String... lines) {
		for (String line : lines) {
			manifestDataReader.processLine(line);
		}
	}
	
	private Map<String,String> getResult() {
		return manifestDataReader.getData();
	}	
	
	private Map<String,String> createMap(String... values) {
		Map<String,String> res = new HashMap<>();
		int n = values.length / 2;
		for (int i=0; i<n; ++i) {
			res.put(values[i*2], values[i*2+1]);
		}

		return res;
	}

}
