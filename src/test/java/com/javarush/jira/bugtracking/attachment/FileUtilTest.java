package com.javarush.jira.bugtracking.attachment;

import com.javarush.jira.common.error.IllegalRequestDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStream;
import static org.junit.jupiter.api.Assertions.*;

class FileUtilTest {
    @TempDir Path directory;
    private MockMultipartFile file() {
        return new MockMultipartFile("file", "report.txt", "text/plain", "report".getBytes());
    }
    @Test void createsDirectoryAndCopiesStream() throws Exception {
        Path nested = directory.resolve("nested");
        FileUtil.upload(file(), nested.toString(), "report.txt");
        assertEquals("report", Files.readString(nested.resolve("report.txt")));
    }
    @Test void replacesExistingFile() throws Exception {
        Files.writeString(directory.resolve("report.txt"), "old");
        FileUtil.upload(file(), directory.toString(), "report.txt");
        assertEquals("report", Files.readString(directory.resolve("report.txt")));
    }
    @Test void rejectsEmptyFile() {
        assertThrows(IllegalRequestDataException.class, () -> FileUtil.upload(
                new MockMultipartFile("file", new byte[0]), directory.toString(), "empty"));
    }
    @Test void rejectsTraversalAndAbsoluteNames() {
        for (String name : new String[]{"../escape", directory.resolve("absolute").toString(), "", "sub/file"}) {
            assertThrows(IllegalRequestDataException.class, () -> FileUtil.upload(file(), directory.toString(), name));
        }
    }
    @Test void reportsUnwritableDirectory() throws Exception {
        Path regularFile = Files.writeString(directory.resolve("not-directory"), "data");
        assertThrows(IllegalRequestDataException.class, () -> FileUtil.upload(file(), regularFile.toString(), "file"));
    }
    @Test void failedStreamPreservesExistingFileAndCleansTemporaryFile() throws Exception {
        Files.writeString(directory.resolve("report.txt"), "original");
        MockMultipartFile broken = new MockMultipartFile("file", "report.txt", "text/plain", new byte[]{1}) {
            @Override public InputStream getInputStream() throws IOException { throw new IOException("broken stream"); }
        };
        assertThrows(IllegalRequestDataException.class, () -> FileUtil.upload(broken, directory.toString(), "report.txt"));
        assertEquals("original", Files.readString(directory.resolve("report.txt")));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }
}
