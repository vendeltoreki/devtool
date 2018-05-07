package com.liferay.devtool.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.liferay.devtool.testutils.MockProcess;

public class SimpleCommandUnitTest {

	private MockProcess mockProcess;
	private SysEnv sysEnvMock;
	private SimpleCommand simpleCommand;

	@Before
	public void init() throws Exception {
		mockProcess = new MockProcess();

		sysEnvMock = mock(SysEnv.class);
		when(sysEnvMock.getRuntimeProcess(any())).thenReturn(mockProcess);

		simpleCommand = new SimpleCommand();
		simpleCommand.setSysEnv(sysEnvMock);
	}

	@Test
	public void test_run_success() throws Exception {
		mockProcess.setInputStream(createInputStreamFromString("output"));
		mockProcess.setErrorStream(createInputStreamFromString("error"));

		simpleCommand.run("test");

		assertThat(simpleCommand.getExitValue(), equalTo(0));
		assertThat(simpleCommand.isSuccess(), equalTo(true));

		assertEquals(new ArrayList<String>(Arrays.asList("output")), simpleCommand.getStdOut());
		assertEquals(new ArrayList<String>(Arrays.asList("error")), simpleCommand.getStdErr());
	}

	@Test
	public void test_run_success_multiLine() throws Exception {
		mockProcess.setInputStream(createInputStreamFromString("output\notherline\n\nlastline"));
		mockProcess.setErrorStream(createInputStreamFromString("error\notherline\n\nlastline"));

		simpleCommand.run("test");

		assertThat(simpleCommand.getExitValue(), equalTo(0));
		assertThat(simpleCommand.isSuccess(), equalTo(true));

		assertEquals(new ArrayList<String>(Arrays.asList("output", "otherline", "", "lastline")),
				simpleCommand.getStdOut());
		assertEquals(new ArrayList<String>(Arrays.asList("error", "otherline", "", "lastline")),
				simpleCommand.getStdErr());
	}

	@Test
	public void test_run_fail() throws Exception {
		mockProcess.setReturnValue(111);
		mockProcess.setInputStream(createInputStreamFromString("output"));
		mockProcess.setErrorStream(createInputStreamFromString("error"));

		simpleCommand.run("test");

		assertThat(simpleCommand.getExitValue(), equalTo(111));
		assertThat(simpleCommand.isSuccess(), equalTo(false));

		assertEquals(new ArrayList<String>(Arrays.asList("output")), simpleCommand.getStdOut());
		assertEquals(new ArrayList<String>(Arrays.asList("error")), simpleCommand.getStdErr());
	}

	@Test(expected = RuntimeException.class)
	public void test_run_exception() throws Exception {
		mockProcess.setReturnValue(111);
		mockProcess.setExceptionMessage("Error found");
		mockProcess.setInputStream(createInputStreamFromString("output"));
		mockProcess.setErrorStream(createInputStreamFromString("error"));

		simpleCommand.run("test");
	}

	private InputStream createInputStreamFromString(String string) {
		InputStream is = new ByteArrayInputStream(string.getBytes());
		return is;
	}

}
