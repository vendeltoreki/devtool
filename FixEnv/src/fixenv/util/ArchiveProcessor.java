package fixenv.util;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.HashMap;
import java.util.Map;

public class ArchiveProcessor {
	private long targetTime = System.currentTimeMillis() - (1000*60*60*10);
	
	public long getTargetTime() {
		return targetTime;
	}

	public void setTargetTime(long targetTime) {
		this.targetTime = targetTime;
	}

	public boolean checkFile(String filePath) throws Exception {
		Map<String, String> env = new HashMap<>();

		URI uri = URI.create(pathForArchiveFs(filePath));
		try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
			for (Path rootDir : zipfs.getRootDirectories()) {
				boolean found = Files.walk(rootDir)
					.filter(f -> Files.isRegularFile(f))
					.anyMatch(f -> filterFile(f));
				
				if (found) {
					return true;
				}
			}
		}
		
		return false;
	}

	public void process(String filePath) throws Exception {
		Map<String, String> env = new HashMap<>();
		//env.put("create", "true");

		URI uri = URI.create(pathForArchiveFs(filePath));

		try (FileSystem zipfs = FileSystems.newFileSystem(uri, env)) {
			for (Path rootDir : zipfs.getRootDirectories()) {
				Files.walk(rootDir)
					.filter(f -> Files.isRegularFile(f))
					.filter(f -> filterFile(f))
					.forEach(f -> {
						try {
							//System.out.println("updating:"+f);
							Files.setLastModifiedTime(f, FileTime.fromMillis(targetTime));
						} catch (IOException e) { }
					});
			}
		}
	}
	
	private String pathForArchiveFs(String filePath) {
		return "jar:file:/"+filePath.replace('\\', '/');
	}

	public boolean filterFile(Path path) {
		try {
			return Files.getLastModifiedTime(path).toMillis() > targetTime;
		} catch (IOException e) {
			return false;
		}
	}
}
