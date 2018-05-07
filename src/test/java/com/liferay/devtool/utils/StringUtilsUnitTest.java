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

}
