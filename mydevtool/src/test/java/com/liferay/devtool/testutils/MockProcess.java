package com.liferay.devtool.testutils;

import java.io.InputStream;
import java.io.OutputStream;

public class MockProcess extends Process {
	private InputStream inputStream;
	private InputStream errorStream;
	private int returnValue;
	private String exceptionMessage;

	public MockProcess() {
	}

	public void setInputStream(InputStream inputStream) {
		this.inputStream = inputStream;
	}

	public void setErrorStream(InputStream errorStream) {
		this.errorStream = errorStream;
	}

	public void setReturnValue(int returnValue) {
		this.returnValue = returnValue;
	}

	public void setExceptionMessage(String exceptionMessage) {
		this.exceptionMessage = exceptionMessage;
	}

	@Override
	public OutputStream getOutputStream() {
		return null;
	}

	@Override
	public InputStream getInputStream() {
		return inputStream;
	}

	@Override
	public InputStream getErrorStream() {
		return errorStream;
	}

	@Override
	public int waitFor() throws InterruptedException {
		if (exceptionMessage != null) {
			throw new RuntimeException(exceptionMessage);
		}

		return returnValue;
	}

	@Override
	public int exitValue() {
		return returnValue;
	}

	@Override
	public void destroy() {
	}
}