package fixenv.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class SimpleCommand {
	private List<String> stdOut;
	private List<String> stdErr;
	private int exitValue;

	public void run(String command) {
		try {
			Process p = Runtime.getRuntime().exec(command);

			stdOut = readInputStreamToList(p.getInputStream());
			stdErr = readInputStreamToList(p.getErrorStream());
			exitValue = p.waitFor();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public List<String> readInputStreamToList(InputStream inputStream) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

		List<String> res = new ArrayList<>();
		String s = null;
		try {
			while ((s = reader.readLine()) != null) {
				res.add(s);
			}
		} catch (IOException e) {
			res.add(e.getMessage());
		}

		return res;
	}

	public List<String> getStdOut() {
		return stdOut;
	}

	public List<String> getStdErr() {
		return stdErr;
	}

	public int getExitValue() {
		return exitValue;
	}

	public boolean isSuccess() {
		return exitValue == 0;
	}
}
