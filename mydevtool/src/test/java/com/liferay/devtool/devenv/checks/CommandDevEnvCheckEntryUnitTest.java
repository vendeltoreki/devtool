package com.liferay.devtool.devenv.checks;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

import com.liferay.devtool.devenv.CheckStatus;
import com.liferay.devtool.devenv.DevEnvCheckContext;
import com.liferay.devtool.testutils.MockProcess;
import com.liferay.devtool.utils.SysEnv;

public class CommandDevEnvCheckEntryUnitTest {

	private MockProcess mockProcess;
	private SysEnv sysEnvMock;
	private CommandDevEnvCheckEntry commandDevEnvCheckEntry;

	@Before
	public void init() throws Exception {
		mockProcess = new MockProcess();

		sysEnvMock = mock(SysEnv.class);
		when(sysEnvMock.getRuntimeProcess(any())).thenReturn(mockProcess);

		DevEnvCheckContext devEnvCheckContext = new DevEnvCheckContext();
		
		commandDevEnvCheckEntry = new CommandDevEnvCheckEntry("test");
		commandDevEnvCheckEntry.setContext(devEnvCheckContext);
		commandDevEnvCheckEntry.setSysEnv(sysEnvMock);
	}

	@Test
	public void test_runCheck_success() throws Exception {
		mockProcess.setInputStream(createInputStreamFromString("output"));
		mockProcess.setErrorStream(createInputStreamFromString("error"));

		commandDevEnvCheckEntry.runCheck();

		assertThat(commandDevEnvCheckEntry.getStatus(), equalTo(CheckStatus.SUCCESS));
	}

	@Test
	public void test_runCheck_fail() throws Exception {
		mockProcess.setInputStream(createInputStreamFromString("output"));
		mockProcess.setErrorStream(createInputStreamFromString("error"));
		mockProcess.setReturnValue(10);

		commandDevEnvCheckEntry.runCheck();

		assertThat(commandDevEnvCheckEntry.getStatus(), equalTo(CheckStatus.FAIL));
	}
	
	private InputStream createInputStreamFromString(String string) {
		InputStream is = new ByteArrayInputStream(string.getBytes());
		return is;
	}
}
