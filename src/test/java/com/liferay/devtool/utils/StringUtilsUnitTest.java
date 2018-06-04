package com.liferay.devtool.utils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class StringUtilsUnitTest {
	@Test
	public void test_replacePathParam() throws Exception {
		assertThat(StringUtils.replacePathParam("${project.dir}/../bundles_ee", "${project.dir}", "C:\\user\\test"),
				equalTo("C:\\user\\test/../bundles_ee"));
		assertThat(StringUtils.replacePathParam("${project.dir}/../bundles_ee", "${project.dir}", "/user/home/test"),
				equalTo("/user/home/test/../bundles_ee"));
	}

	@Test
	public void test_collapseCsvLine() throws Exception {
		assertThat(StringUtils.collapseCsvLine(new String[] {"A","B","C","D","E"}, 1, 2),
				equalTo(new String[] {"A","B,C,D,E"}));

		assertThat(StringUtils.collapseCsvLine(new String[] {"A","B","C","D","E"}, 1, 3),
				equalTo(new String[] {"A","B,C,D","E"}));

		assertThat(StringUtils.collapseCsvLine(new String[] {"A","B","C","D","E"}, 1, 4),
				equalTo(new String[] {"A","B,C","D","E"}));

		assertThat(StringUtils.collapseCsvLine(new String[] {"A","B","C","D","E"}, 1, 5),
				equalTo(new String[] {"A","B","C","D","E"}));
		

		assertThat(StringUtils.collapseCsvLine(new String[] {"A","B"}, 0, 1),
				equalTo(new String[] {"A,B"}));
		
		assertThat(StringUtils.collapseCsvLine(new String[] {"A","B"}, 0, 2),
				equalTo(new String[] {"A","B"}));

		assertThat(StringUtils.collapseCsvLine(new String[] {"A"}, 0, 1),
				equalTo(new String[] {"A"}));
	}
	
	@Test
	public void test_fillCsvMissingFields() throws Exception {
		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 0, 3),
				equalTo(new String[] {"A","B","C"}));
		
		
		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 0, 4),
				equalTo(new String[] {null,"A","B","C"}));
		
		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 1, 4),
				equalTo(new String[] {"A",null,"B","C"}));

		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 2, 4),
				equalTo(new String[] {"A","B",null,"C"}));

		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 3, 4),
				equalTo(new String[] {"A","B","C",null}));
		
		
		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 0, 5),
				equalTo(new String[] {null,null,"A","B","C"}));
		
		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 1, 5),
				equalTo(new String[] {"A",null,null,"B","C"}));

		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 2, 5),
				equalTo(new String[] {"A","B",null,null,"C"}));

		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 3, 5),
				equalTo(new String[] {"A","B","C",null,null}));

		
		assertThat(StringUtils.fillCsvMissingFields(new String[] {"A","B","C"}, 0, 2),
				equalTo(new String[] {"A","B"}));
	}
	
	@Test
	public void test_splitFirst() throws Exception {
		assertThat(StringUtils.splitFirst("", ": "),
				equalTo(new String[] {""}));
		
		assertThat(StringUtils.splitFirst("ABC", ": "),
				equalTo(new String[] {"ABC"}));

		assertThat(StringUtils.splitFirst("ABC: DEF", ": "),
				equalTo(new String[] {"ABC","DEF"}));

		assertThat(StringUtils.splitFirst("ABC: DEF", ":"),
				equalTo(new String[] {"ABC"," DEF"}));

		assertThat(StringUtils.splitFirst("ABC: DEF ", ": "),
				equalTo(new String[] {"ABC","DEF "}));

		assertThat(StringUtils.splitFirst("ABC: DEF: GHI", ": "),
				equalTo(new String[] {"ABC","DEF: GHI"}));
	}
	
	@Test
	public void test_splitLast() throws Exception {
		assertThat(StringUtils.splitLast("", "/"),
				equalTo(new String[] {""}));
		
		assertThat(StringUtils.splitLast("ABC/DEF", "/"),
				equalTo(new String[] {"ABC","DEF"}));

		assertThat(StringUtils.splitLast("ABC/DEF/GHI", "/"),
				equalTo(new String[] {"ABC/DEF","GHI"}));
	}
	
	@Test
	public void test_removeParamsFromUrl() throws Exception {
		String connString = "jdbc:mysql://localhost/lportal_62?characterEncoding=UTF-8&dontTrackOpenResources=true&holdResultsOpenOverStatementClose=true&useFastDateParsing=false&useUnicode=true";
		assertThat(StringUtils.removeParamsFromUrl(connString),
				equalTo("jdbc:mysql://localhost/lportal_62"));
	}
	
	@Test
	public void test_extractSchemaNameFromMySqlUrl() throws Exception {
		String connString = "jdbc:mysql://localhost/lportal_62?characterEncoding=UTF-8&dontTrackOpenResources=true&holdResultsOpenOverStatementClose=true&useFastDateParsing=false&useUnicode=true";
		assertThat(StringUtils.extractSchemaNameFromMySqlUrl(connString),
				equalTo("lportal_62"));
	}
}
