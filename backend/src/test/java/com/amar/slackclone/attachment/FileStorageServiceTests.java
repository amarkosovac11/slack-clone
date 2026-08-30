package com.amar.slackclone.attachment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTests {
    @TempDir Path directory;

    @Test void storesAllowedFileUnderOpaqueKey() throws Exception {
        var storage = new FileStorageService(directory.toString());
        var file = new MockMultipartFile("file", "../../private.txt", "text/plain", "safe".getBytes());
        var key = storage.store(file);
        assertFalse(key.contains("private"));
        assertEquals("safe", storage.load(key).getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
    }

    @Test void rejectsDisallowedAndOversizedFiles() throws Exception {
        var storage = new FileStorageService(directory.toString());
        assertThrows(IllegalArgumentException.class, () -> storage.store(
                new MockMultipartFile("file", "payload.exe", "application/octet-stream", new byte[]{1})));
        assertThrows(IllegalArgumentException.class, () -> storage.store(
                new MockMultipartFile("file", "huge.txt", "text/plain", new byte[10 * 1024 * 1024 + 1])));
    }
}
