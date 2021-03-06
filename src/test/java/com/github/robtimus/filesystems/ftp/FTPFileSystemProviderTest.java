/*
 * FTPFileSystemProviderTest.java
 * Copyright 2016 Rob Spoor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.robtimus.filesystems.ftp;

import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.UNIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.ProviderMismatchException;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.filesystem.FileEntry;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.URISupport;

@SuppressWarnings("nls")
class FTPFileSystemProviderTest extends AbstractFTPFileSystemTest {

    FTPFileSystemProviderTest() {
        // there's no need to test the FTP file system itself, so just use UNIX; the FTPFile strategy factory doesn't matter
        super(true, null);
    }

    // support for Paths and Files

    @Test
    void testPathsAndFilesSupport() throws IOException {

        try (FTPFileSystem fs = newFileSystem(createEnv(UNIX))) {
            Path path = Paths.get(URI.create(getBaseUrl() + "/foo"));
            assertThat(path, instanceOf(FTPPath.class));
            // as required by Paths.get
            assertEquals(path, path.toAbsolutePath());

            // the file does not exist yet
            assertFalse(Files.exists(path));

            Files.createFile(path);
            try {
                // the file now exists
                assertTrue(Files.exists(path));

                byte[] content = new byte[1024];
                new Random().nextBytes(content);
                try (OutputStream output = Files.newOutputStream(path, FileType.binary())) {
                    output.write(content);
                }

                // check the file directly
                FileEntry file = getFile("/foo");
                assertArrayEquals(content, getContents(file));

            } finally {

                Files.delete(path);
                assertFalse(Files.exists(path));

                assertNull(getFileSystemEntry("/foo"));
            }
        }
    }

    @Test
    void testPathsAndFilesSupportFileSystemNotFound() {
        URI uri = URI.create("ftp://ftp.github.com/");
        FileSystemNotFoundException exception = assertThrows(FileSystemNotFoundException.class, () -> Paths.get(uri));
        assertEquals(uri.toString(), exception.getMessage());
    }

    // FTPFileSystemProvider.removeFileSystem

    @Test
    void testRemoveFileSystem() throws IOException {
        addDirectory("/foo/bar");

        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        URI uri;
        try (FTPFileSystem fs = newFileSystem(provider, createEnv(UNIX))) {
            FTPPath path = new FTPPath(fs, "/foo/bar");

            uri = path.toUri();

            assertFalse(provider.isHidden(path));
        }
        FileSystemNotFoundException exception = assertThrows(FileSystemNotFoundException.class, () -> provider.getPath(uri));
        assertEquals(uri.toString(), exception.getMessage());
    }

    // FTPFileSystemProvider.getPath

    @Test
    void testGetPath() throws IOException {
        Map<String, String> inputs = new HashMap<>();
        inputs.put("/", "/");
        inputs.put("foo", "/home/test/foo");
        inputs.put("/foo", "/foo");
        inputs.put("foo/bar", "/home/test/foo/bar");
        inputs.put("/foo/bar", "/foo/bar");

        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        try (FTPFileSystem fs = newFileSystem(provider, createEnv(UNIX))) {
            for (Map.Entry<String, String> entry : inputs.entrySet()) {
                URI uri = fs.getPath(entry.getKey()).toUri();
                Path path = provider.getPath(uri);
                assertThat(path, instanceOf(FTPPath.class));
                assertEquals(entry.getValue(), ((FTPPath) path).path());
            }
            for (Map.Entry<String, String> entry : inputs.entrySet()) {
                URI uri = fs.getPath(entry.getKey()).toUri();
                uri = URISupport.create(uri.getScheme().toUpperCase(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), null, null);
                Path path = provider.getPath(uri);
                assertThat(path, instanceOf(FTPPath.class));
                assertEquals(entry.getValue(), ((FTPPath) path).path());
            }
        }
    }

    @Test
    void testGetPathNoScheme() {
        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        URI uri = URI.create("/foo/bar");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> provider.getPath(uri));
        assertEquals(Messages.uri().notAbsolute(uri).getMessage(), exception.getMessage());
    }

    @Test
    void testGetPathInvalidScheme() {
        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        URI uri = URI.create("https://www.github.com/");
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> provider.getPath(uri));
        assertEquals(Messages.uri().invalidScheme(uri, "ftp").getMessage(), exception.getMessage());
    }

    @Test
    void testGetPathFileSystemNotFound() {
        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        URI uri = URI.create("ftp://ftp.github.com/");
        FileSystemNotFoundException exception = assertThrows(FileSystemNotFoundException.class, () -> provider.getPath(uri));
        assertEquals(uri.toString(), exception.getMessage());
    }

    // FTPFileSystemProvider.isSameFile

    @Test
    void testIsSameFileWithDifferentTypes() throws IOException {

        FTPFileSystemProvider ftpProvider = new FTPFileSystemProvider();

        @SuppressWarnings("resource")
        FileSystem defaultFileSystem = FileSystems.getDefault();
        FileSystemProvider defaultProvider = defaultFileSystem.provider();

        try (FTPFileSystem fs1 = newFileSystem(ftpProvider, createEnv(UNIX))) {
            FTPPath path1 = new FTPPath(fs1, "pom.xml");
            Path path2 = Paths.get("pom.xml");

            assertFalse(ftpProvider.isSameFile(path1, path2));
            assertFalse(defaultProvider.isSameFile(path2, path1));
        }
    }

    // FTPFileSystemProvider.getFileAttributeView

    @Test
    void testGetFileAttributeViewBasic() throws IOException {

        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        try (FTPFileSystem fs = newFileSystem(provider, createEnv(UNIX))) {
            FTPPath path = new FTPPath(fs, "/foo/bar");

            BasicFileAttributeView view = fs.provider().getFileAttributeView(path, BasicFileAttributeView.class);
            assertNotNull(view);
            assertEquals("basic", view.name());
        }
    }

    @Test
    void testGetFileAttributeViewPosix() throws IOException {

        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        try (FTPFileSystem fs = newFileSystem(provider, createEnv(UNIX))) {
            FTPPath path = new FTPPath(fs, "/foo/bar");

            PosixFileAttributeView view = fs.provider().getFileAttributeView(path, PosixFileAttributeView.class);
            assertNotNull(view);
            assertEquals("posix", view.name());
        }
    }

    @Test
    void testGetFileAttributeViewReadAttributes() throws IOException {
        addDirectory("/foo/bar");

        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        try (FTPFileSystem fs = newFileSystem(provider, createEnv(UNIX))) {
            FTPPath path = new FTPPath(fs, "/foo/bar");

            BasicFileAttributeView view = fs.provider().getFileAttributeView(path, BasicFileAttributeView.class);
            assertNotNull(view);

            BasicFileAttributes attributes = view.readAttributes();
            assertTrue(attributes.isDirectory());
        }
    }

    // FTPFileSystemProvider.keepAlive

    @Test
    void testKeepAliveWithFTPFileSystem() throws IOException {
        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        try (FTPFileSystem fs = newFileSystem(provider, createEnv(UNIX))) {
            assertDoesNotThrow(() -> FTPFileSystemProvider.keepAlive(fs));
        }
    }

    @Test
    void testKeepAliveWithNonFTPFileSystem() {
        @SuppressWarnings("resource")
        FileSystem defaultFileSystem = FileSystems.getDefault();
        assertThrows(ProviderMismatchException.class, () -> FTPFileSystemProvider.keepAlive(defaultFileSystem));
    }

    @Test
    void testKeepAliveWithNullFTPFileSystem() {
        assertThrows(ProviderMismatchException.class, () -> FTPFileSystemProvider.keepAlive(null));
    }

    // FTPFileSystemProvider.createDirectory through Files.createDirectories

    @Test
    void testCreateDirectories() throws IOException {
        addDirectory("/foo/bar");

        FTPFileSystemProvider provider = new FTPFileSystemProvider();
        try (FTPFileSystem fs = newFileSystem(provider, createEnv(UNIX))) {
            FTPPath path = new FTPPath(fs, "/foo/bar");
            Files.createDirectories(path);
        }

        assertNotNull(getFileSystemEntry("/foo/bar"));
    }

    private FTPFileSystem newFileSystem(Map<String, ?> env) throws IOException {
        return (FTPFileSystem) FileSystems.newFileSystem(getURI(), env);
    }

    private FTPFileSystem newFileSystem(FTPFileSystemProvider provider, Map<String, ?> env) throws IOException {
        return (FTPFileSystem) provider.newFileSystem(getURI(), env);
    }
}
