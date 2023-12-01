/*
 * FTPFileSystemTest.java
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

import static com.github.robtimus.filesystems.SimpleAbstractPath.CURRENT_DIR;
import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.AUTO_DETECT;
import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.NON_UNIX;
import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.UNIX;
import static com.github.robtimus.junit.support.ThrowableAssertions.assertChainEquals;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.NotLinkException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockito.verification.VerificationMode;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.attribute.FileAttributeViewMetadata;
import com.github.robtimus.filesystems.attribute.SimpleGroupPrincipal;
import com.github.robtimus.filesystems.attribute.SimpleUserPrincipal;
import com.github.robtimus.filesystems.ftp.server.SymbolicLinkEntry;

@SuppressWarnings("nls")
class FTPFileSystemTest {

    @Test
    void testPrefixAttributes() {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("size", 1L);
        attributes.put("isDirectory", "false");
        attributes.put("owner", new SimpleUserPrincipal("test"));

        Map<String, Object> expected = new HashMap<>();
        expected.put("posix:size", 1L);
        expected.put("posix:isDirectory", "false");
        expected.put("posix:owner", new SimpleUserPrincipal("test"));

        assertEquals(expected, FTPFileSystem.prefixAttributes(attributes, FileAttributeViewMetadata.POSIX));
    }

    @Nested
    @DisplayName("Use UNIX FTP server: true; FTPFile strategy factory: UNIX")
    class UnixServerUsingUnixStrategy extends FileSystemTest {

        UnixServerUsingUnixStrategy() {
            super(true, UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: true; FTPFile strategy factory: NON_UNIX")
    class UnixServerUsingNonUnixStrategy extends FileSystemTest {

        UnixServerUsingNonUnixStrategy() {
            super(true, NON_UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: false; FTPFile strategy factory: AUTO_DETECT")
    class NonUnixServerUsingUnixStrategy extends FileSystemTest {

        NonUnixServerUsingUnixStrategy() {
            super(false, AUTO_DETECT);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: false; FTPFile strategy factory: NON_UNIX")
    class NonUnixServerNotUsingAbsoluteFilePaths extends FileSystemTest {

        NonUnixServerNotUsingAbsoluteFilePaths() {
            super(false, NON_UNIX);
        }
    }

    @Nested
    @DisplayName("List hidden files")
    @TestInstance(Lifecycle.PER_CLASS)
    class ListHiddenFiles extends AbstractFTPFileSystemTest {

        ListHiddenFiles() {
            // not going to use any default FTP file system, so FTPFile strategy factory doesn't matter
            super(true, null);
        }

        @ParameterizedTest(name = "List hidden files: {0}; strategy: {1}")
        @MethodSource("listHiddenFilesArguments")
        void testListHiddenFiles(boolean listHiddenFiles, FTPFileStrategyFactory strategyFactory, boolean expectFailure) throws IOException {
            URI uri = getURI();
            FTPEnvironment env = createEnv(strategyFactory)
                    .withListHiddenFiles(listHiddenFiles);
            try (FileSystem fs = FileSystems.newFileSystem(uri, env)) {
                addDirectory("/foo/bar");

                Path path = new FTPPath((FTPFileSystem) fs, "/foo");
                if (expectFailure) {
                    NotDirectoryException exception = assertThrows(NotDirectoryException.class, () -> Files.newDirectoryStream(path));
                    assertEquals("/foo", exception.getFile());
                } else {
                    try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                        Iterator<Path> iterator = stream.iterator();
                        assertTrue(iterator.hasNext());
                        assertEquals("bar", iterator.next().getFileName().toString());
                    }
                }
            }
        }

        Stream<Arguments> listHiddenFilesArguments() {
            Arguments[] arguments = {
                    arguments(false, UNIX, true),
                    arguments(false, NON_UNIX, false),
                    arguments(false, AUTO_DETECT, false),
                    arguments(true, UNIX, false),
                    arguments(true, NON_UNIX, false),
                    arguments(true, AUTO_DETECT, false),
            };
            return Arrays.stream(arguments);
        }
    }

    abstract static class FileSystemTest extends AbstractFTPFileSystemTest {

        private FileSystemTest(boolean useUnixFtpServer, StandardFTPFileStrategyFactory ftpFileStrategyFactory) {
            super(useUnixFtpServer, ftpFileStrategyFactory);
        }

        @Test
        void testGetPath() {
            testGetPath("/", "/");
            testGetPath("/foo/bar", "/", "/foo", "/bar");
            testGetPath("/foo/../bar", "/foo/", "../bar");
        }

        private void testGetPath(String path, String first, String... more) {
            FTPPath expected = createPath(path);
            Path actual = fileSystem.getPath(first, more);
            assertEquals(expected, actual);
        }

        @Test
        void testKeepAlive() {
            assertDoesNotThrow(fileSystem::keepAlive);
        }

        @Test
        void testToUri() {
            final String prefix = getBaseUrl();

            testToUri("/", prefix + "/");
            testToUri("/foo/bar", prefix + "/foo/bar");
            testToUri("/foo/../bar", prefix + "/bar");

            testToUri("", prefix + "/home/test");
            testToUri("foo/bar", prefix + "/home/test/foo/bar");
            testToUri("foo/../bar", prefix + "/home/test/bar");
        }

        private void testToUri(String path, String expected) {
            URI expectedUri = URI.create(expected);
            URI actual = createPath(path).toUri();
            assertEquals(expectedUri, actual);
        }

        @Test
        void testToAbsolutePath() {

            testToAbsolutePath("/", "/");
            testToAbsolutePath("/foo/bar", "/foo/bar");
            testToAbsolutePath("/foo/../bar", "/foo/../bar");

            testToAbsolutePath("", "/home/test");
            testToAbsolutePath("foo/bar", "/home/test/foo/bar");
            testToAbsolutePath("foo/../bar", "/home/test/foo/../bar");
        }

        private void testToAbsolutePath(String path, String expected) {
            FTPPath expectedPath = createPath(expected);
            Path actual = createPath(path).toAbsolutePath();
            assertEquals(expectedPath, actual);
        }

        @Nested
        class ToRealPath {

            @Test
            void testNoFollowLinks() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                addDirectory("/foo/bar");
                addDirectory("/bar");
                addFile("/home/test/foo/bar");
                FileEntry bar = addFile("/home/test/bar");

                // symbolic links
                SymbolicLinkEntry symLink = addSymLink("/hello", foo);
                addSymLink("/world", symLink);
                symLink = addSymLink("/home/test/baz", bar);
                addSymLink("/baz", symLink);

                testToNoFollowLinks("/", "/");
                testToNoFollowLinks("/foo/bar", "/foo/bar");
                testToNoFollowLinks("/foo/../bar", "/bar");

                testToNoFollowLinks("", "/home/test");
                testToNoFollowLinks("foo/bar", "/home/test/foo/bar");
                testToNoFollowLinks("foo/../bar", "/home/test/bar");
                testToNoFollowLinks(CURRENT_DIR, "/home/test");

                // symbolic links
                testToNoFollowLinks("/hello", "/hello");
                testToNoFollowLinks("/world", "/world");
                testToNoFollowLinks("/home/test/baz", "/home/test/baz");
                testToNoFollowLinks("/baz", "/baz");
            }

            private void testToNoFollowLinks(String path, String expected) throws IOException {
                FTPPath expectedPath = createPath(expected);
                Path actual = createPath(path).toRealPath(LinkOption.NOFOLLOW_LINKS);
                assertEquals(expectedPath, actual);
            }

            @Test
            void testFollowLinks() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                addDirectory("/foo/bar");
                addDirectory("/bar");
                addFile("/home/test/foo/bar");
                FileEntry bar = addFile("/home/test/bar");

                // symbolic links
                SymbolicLinkEntry symLink = addSymLink("/hello", foo);
                addSymLink("/world", symLink);
                symLink = addSymLink("/home/test/baz", bar);
                addSymLink("/baz", symLink);

                testFollowLinks("/", "/");
                testFollowLinks("/foo/bar", "/foo/bar");
                testFollowLinks("/foo/../bar", "/bar");

                testFollowLinks("", "/home/test");
                testFollowLinks("foo/bar", "/home/test/foo/bar");
                testFollowLinks("foo/../bar", "/home/test/bar");

                // symbolic links
                testFollowLinks("/hello", "/foo");
                testFollowLinks("/world", "/foo");
                testFollowLinks("/home/test/baz", "/home/test/bar");
                testFollowLinks("/baz", "/home/test/bar");
            }

            private void testFollowLinks(String path, String expected) throws IOException {
                FTPPath expectedPath = createPath(expected);
                Path actual = createPath(path).toRealPath();
                assertEquals(expectedPath, actual);
            }

            @Test
            void testBrokenLink() {
                addSymLink("/foo", new FileEntry("/bar"));

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> createPath("/foo").toRealPath());
                assertEquals("/bar", exception.getFile());
            }

            @Test
            void testNotExisting() {
                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> createPath("/foo").toRealPath());
                assertEquals("/foo", exception.getFile());
            }
        }

        @Nested
        class NewInputStream {

            @Test
            void testSuccess() throws IOException {
                addFile("/foo/bar");

                try (InputStream input = provider().newInputStream(createPath("/foo/bar"))) {
                    // don't do anything with the stream, there's a separate test for that
                }
                // verify that the file system can be used after closing the stream
                provider().checkAccess(createPath("/foo/bar"));
            }

            @Test
            void testDeleteOnClose() throws IOException {
                addFile("/foo/bar");

                OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
                try (InputStream input = provider().newInputStream(createPath("/foo/bar"), options)) {
                    // don't do anything with the stream, there's a separate test for that
                }
                assertNull(getFileSystemEntry("/foo/bar"));
                assertEquals(0, getChildCount("/foo"));
            }

            @Test
            void testDirectory() {
                testDirectory("/home", "/home");
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCurrentDirectory(String dir) {
                testDirectory(dir, getDefaultDir());
            }

            private void testDirectory(String dir, String expectedFile) {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath(dir);

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.newInputStream(path));
                assertEquals(expectedFile, exception.getFile());

                verify(getExceptionFactory()).createNewInputStreamException(eq(expectedFile), eq(550), anyString());
            }

            @Test
            void testFTPFailure() {
                // failure: file not found

                FTPFileSystemProvider provider = provider();
                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.newInputStream(createPath("/foo/bar")));
                assertEquals("/foo/bar", exception.getFile());

                verify(getExceptionFactory()).createNewInputStreamException(eq("/foo/bar"), eq(550), anyString());
            }
        }

        @Nested
        class NewOutputStream {

            @Test
            void testExisting() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");

                OpenOption[] options = { StandardOpenOption.WRITE };
                try (OutputStream output = provider().newOutputStream(createPath("/foo/bar"), options)) {
                    // don't do anything with the stream, there's a separate test for that
                }
                // verify that the file system can be used after closing the stream
                provider().checkAccess(createPath("/foo/bar"));

                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testExistingDeleteOnClose() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");

                OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
                try (OutputStream output = provider().newOutputStream(createPath("/foo/bar"), options)) {
                    // don't do anything with the stream, there's a separate test for that
                    assertSame(bar, getFileSystemEntry("/foo/bar"));
                }
                // verify that the file system can be used after closing the stream
                provider().checkAccess(createPath("/foo"));

                assertSame(foo, getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/foo/bar"));
                assertEquals(0, getChildCount("/foo"));
            }

            @Test
            void testExistingCreate() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");

                OpenOption[] options = { StandardOpenOption.CREATE };
                try (OutputStream output = provider().newOutputStream(createPath("/foo/bar"), options)) {
                    // don't do anything with the stream, there's a separate test for that
                }
                // verify that the file system can be used after closing the stream
                provider().checkAccess(createPath("/foo/bar"));

                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testExistingCreateDeleteOnClose() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");

                OpenOption[] options = { StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE };
                try (OutputStream output = provider().newOutputStream(createPath("/foo/bar"), options)) {
                    // don't do anything with the stream, there's a separate test for that
                    assertSame(bar, getFileSystemEntry("/foo/bar"));
                }
                // verify that the file system can be used after closing the stream
                provider().checkAccess(createPath("/foo"));

                assertSame(foo, getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testExistingCreateNew() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");
                OpenOption[] options = { StandardOpenOption.CREATE_NEW };

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.newOutputStream(path, options));
                assertEquals("/foo/bar", exception.getFile());

                // verify that the file system can be used after closing the stream
                assertDoesNotThrow(() -> provider().checkAccess(createPath("/foo/bar")));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testExistingFTPFailure() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                bar.setPermissionsFromString("r--r--r--");

                // failure: no permission to write

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");
                OpenOption[] options = { StandardOpenOption.WRITE };

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.newOutputStream(path, options));
                assertEquals("/foo/bar", exception.getFile());

                verify(getExceptionFactory()).createNewOutputStreamException(eq("/foo/bar"), eq(553), anyString(), anyCollection());
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testExistingFTPFailureDeleteOnClose() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                bar.setPermissionsFromString("r--r--r--");

                // failure: no permission to write

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");
                OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.newOutputStream(path, options));
                assertEquals("/foo/bar", exception.getFile());

                verify(getExceptionFactory()).createNewOutputStreamException(eq("/foo/bar"), eq(553), anyString(), anyCollection());
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testNonExistingNoCreate() {
                DirectoryEntry foo = addDirectory("/foo");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");
                OpenOption[] options = { StandardOpenOption.WRITE };

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.newOutputStream(path, options));
                assertEquals("/foo/bar", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testNonExistingCreate() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");

                OpenOption[] options = { StandardOpenOption.CREATE };
                try (OutputStream input = provider().newOutputStream(createPath("/foo/bar"), options)) {
                    // don't do anything with the stream, there's a separate test for that
                } finally {
                    assertSame(foo, getFileSystemEntry("/foo"));
                    assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
                }
            }

            @Test
            void testNonExistingCreateDeleteOnClose() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");

                OpenOption[] options = { StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE };
                try (OutputStream input = provider().newOutputStream(createPath("/foo/bar"), options)) {
                    // don't do anything with the stream, there's a separate test for that
                    // we can't check here that /foo/bar exists, because it will only be stored in the file system once the stream is closed
                } finally {
                    assertSame(foo, getFileSystemEntry("/foo"));
                    assertNull(getFileSystemEntry("/foo/bar"));
                    assertEquals(0, getChildCount("/foo"));
                }
            }

            @Test
            void testNonExistingCreateNew() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");

                OpenOption[] options = { StandardOpenOption.CREATE_NEW };
                try (OutputStream input = provider().newOutputStream(createPath("/foo/bar"), options)) {
                    // don't do anything with the stream, there's a separate test for that
                } finally {
                    assertSame(foo, getFileSystemEntry("/foo"));
                    assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
                }
            }

            @Test
            void testNonExistingCreateNewDeleteOnClose() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");

                OpenOption[] options = { StandardOpenOption.CREATE_NEW, StandardOpenOption.DELETE_ON_CLOSE };
                try (OutputStream input = provider().newOutputStream(createPath("/foo/bar"), options)) {
                    // don't do anything with the stream, there's a separate test for that
                    // we can't check here that /foo/bar exists, because it will only be stored in the file system once the stream is closed
                } finally {
                    assertSame(foo, getFileSystemEntry("/foo"));
                    assertNull(getFileSystemEntry("/foo/bar"));
                }
            }

            @Test
            void testDirectoryNoCreate() {
                testDirectoryNoCreate("/foo", "/foo");
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCurrentDirectoryNoCreate(String dir) {
                testDirectoryNoCreate(dir, getDefaultDir());
            }

            private void testDirectoryNoCreate(String dir, String expectedFile) {
                DirectoryEntry directory = addDirectoryIfNotExists(dir);

                int oldChildCount = getChildCount(dir);

                FTPFileSystemProvider provider = provider();
                FTPPath createPath = createPath(dir);
                OpenOption[] options = { StandardOpenOption.WRITE };

                FileSystemException exception = assertThrows(FileSystemException.class, () -> provider.newOutputStream(createPath, options));
                assertEquals(expectedFile, exception.getFile());
                assertEquals(Messages.fileSystemProvider().isDirectory(dir).getReason(), exception.getReason());

                verify(getExceptionFactory(), never()).createNewOutputStreamException(anyString(), anyInt(), anyString(), anyCollection());
                assertSame(directory, getFileSystemEntry(dir));
                assertEquals(oldChildCount, getChildCount(dir));
            }

            @Test
            void testDirectoryDeleteOnClose() {
                testDirectoryDeleteOnClose("/foo", "/foo");
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCurrentDirectoryDeleteOnClose(String dir) {
                testDirectoryDeleteOnClose(dir, getDefaultDir());
            }

            private void testDirectoryDeleteOnClose(String dir, String expectedFile) {
                DirectoryEntry directory = addDirectoryIfNotExists(dir);

                int oldChildCount = getChildCount(dir);

                FTPFileSystemProvider provider = provider();
                FTPPath createPath = createPath(dir);
                OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };

                FileSystemException exception = assertThrows(FileSystemException.class, () -> provider.newOutputStream(createPath, options));
                assertEquals(expectedFile, exception.getFile());
                assertEquals(Messages.fileSystemProvider().isDirectory(dir).getReason(), exception.getReason());

                verify(getExceptionFactory(), never()).createNewOutputStreamException(anyString(), anyInt(), anyString(), anyCollection());
                assertSame(directory, getFileSystemEntry(dir));
                assertEquals(oldChildCount, getChildCount(dir));
            }
        }

        @Nested
        class NewByteChannel {

            @Test
            void testRead() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                bar.setContents(new byte[1024]);

                Set<? extends OpenOption> options = EnumSet.noneOf(StandardOpenOption.class);
                try (SeekableByteChannel channel = provider().newByteChannel(createPath("/foo/bar"), options)) {
                    // don't do anything with the channel, there's a separate test for that
                    assertEquals(bar.getSize(), channel.size());
                }
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testReadNonExisting() {
                // failure: file does not exist

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");
                Set<? extends OpenOption> options = EnumSet.noneOf(StandardOpenOption.class);

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.newByteChannel(path, options));
                assertEquals("/foo/bar", exception.getFile());

                verify(getExceptionFactory()).createNewInputStreamException(eq("/foo/bar"), eq(550), anyString());
                assertNull(getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testWrite() throws IOException {
                addFile("/foo/bar");

                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.WRITE);
                try (SeekableByteChannel channel = provider().newByteChannel(createPath("/foo/bar"), options)) {
                    // don't do anything with the channel, there's a separate test for that
                    assertEquals(0, channel.size());
                }
            }

            @Test
            void testWriteAppend() throws IOException {
                FileEntry bar = addFile("/foo/bar");
                bar.setContents(new byte[1024]);

                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                try (SeekableByteChannel channel = provider().newByteChannel(createPath("/foo/bar"), options)) {
                    // don't do anything with the channel, there's a separate test for that
                    assertEquals(bar.getSize(), channel.size());
                }
            }

            @Test
            void testCreateWriteExisting() throws IOException {
                FileEntry bar = addFile("/foo/bar");

                byte[] newContents = "Lorem ipsum".getBytes();

                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                try (SeekableByteChannel channel = provider().newByteChannel(createPath("/foo/bar"), options)) {
                    // don't do anything with the channel, there's a separate test for that
                    assertEquals(0, channel.size());
                    channel.write(ByteBuffer.wrap(newContents));
                    assertEquals(newContents.length, channel.size());
                }

                assertArrayEquals(newContents, getContents(bar));
            }

            @Test
            void testCreateAppendExisting() throws IOException {
                FileEntry bar = addFile("/foo/bar");
                bar.setContents(new byte[1024]);

                byte[] newContents = "Lorem ipsum".getBytes();

                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                try (SeekableByteChannel channel = provider().newByteChannel(createPath("/foo/bar"), options)) {
                    // don't do anything with the channel, there's a separate test for that
                    long size = bar.getSize();
                    assertEquals(size, channel.size());
                    channel.write(ByteBuffer.wrap(newContents));
                    assertEquals(size + newContents.length, channel.size());
                }

                byte[] totalNewContents = new byte[1024 + newContents.length];
                System.arraycopy(newContents, 0, totalNewContents, 1024, newContents.length);

                assertArrayEquals(totalNewContents, getContents(bar));
            }

            @Test
            void testCreateWriteNonExisting() throws IOException {
                addDirectory("/foo");

                byte[] newContents = "Lorem ipsum".getBytes();

                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                try (SeekableByteChannel channel = provider().newByteChannel(createPath("/foo/bar"), options)) {
                    // don't do anything with the channel, there's a separate test for that
                    assertEquals(0, channel.size());
                    channel.write(ByteBuffer.wrap(newContents));
                    assertEquals(newContents.length, channel.size());
                }

                FileEntry bar = getFile("/foo/bar");

                assertArrayEquals(newContents, getContents(bar));
            }

            @Test
            void testCreateAppendNonExisting() throws IOException {
                addDirectory("/foo");

                byte[] newContents = "Lorem ipsum".getBytes();

                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                try (SeekableByteChannel channel = provider().newByteChannel(createPath("/foo/bar"), options)) {
                    // don't do anything with the channel, there's a separate test for that
                    assertEquals(0, channel.size());
                    channel.write(ByteBuffer.wrap(newContents));
                    assertEquals(newContents.length, channel.size());
                }

                FileEntry bar = getFile("/foo/bar");

                assertArrayEquals(newContents, getContents(bar));
            }

            @Test
            void testCreateNewWriteExisting() throws IOException {
                FileEntry bar = addFile("/foo/bar");
                byte[] oldContents = "Hello World".getBytes();
                bar.setContents(oldContents);

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");
                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.newByteChannel(path, options));
                assertEquals("/foo/bar", exception.getFile());

                assertArrayEquals(oldContents, getContents(bar));
            }

            @Test
            void testCreateNewAppendExisting() throws IOException {
                FileEntry bar = addFile("/foo/bar");
                bar.setContents(new byte[1024]);

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");
                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND);

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.newByteChannel(path, options));
                assertEquals("/foo/bar", exception.getFile());

                assertArrayEquals(new byte[1024], getContents(bar));
            }

            @Test
            void testCreateNewWriteNonExisting() throws IOException {
                addDirectory("/foo");

                byte[] newContents = "Lorem ipsum".getBytes();

                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
                try (SeekableByteChannel channel = provider().newByteChannel(createPath("/foo/bar"), options)) {
                    // don't do anything with the channel, there's a separate test for that
                    assertEquals(0, channel.size());
                    channel.write(ByteBuffer.wrap(newContents));
                    assertEquals(newContents.length, channel.size());
                }

                FileEntry bar = getFile("/foo/bar");

                assertArrayEquals(newContents, getContents(bar));
            }

            @Test
            void testCreateNewAppendNonExisting() throws IOException {
                addDirectory("/foo");

                byte[] newContents = "Lorem ipsum".getBytes();

                Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND);
                try (SeekableByteChannel channel = provider().newByteChannel(createPath("/foo/bar"), options)) {
                    // don't do anything with the channel, there's a separate test for that
                    assertEquals(0, channel.size());
                    channel.write(ByteBuffer.wrap(newContents));
                    assertEquals(newContents.length, channel.size());
                }

                FileEntry bar = getFile("/foo/bar");

                assertArrayEquals(newContents, getContents(bar));
            }

            @Test
            void testDirectory() {
                testDirectory("/home", "/home");
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCurrentDirectory(String dir) {
                testDirectory(dir, getDefaultDir());
            }

            private void testDirectory(String dir, String expectedFile) {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath(dir);

                Set<? extends OpenOption> options = EnumSet.noneOf(StandardOpenOption.class);
                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.newByteChannel(path, options));
                assertEquals(expectedFile, exception.getFile());

                verify(getExceptionFactory()).createNewInputStreamException(eq(expectedFile), eq(550), anyString());
            }
        }

        @Nested
        class NewDirectoryStream {

            @ParameterizedTest
            @ValueSource(strings = { "/", CURRENT_DIR })
            @EmptySource
            void testSuccess(String dir) throws IOException {
                try (DirectoryStream<Path> stream = provider().newDirectoryStream(createPath(dir), entry -> true)) {
                    assertNotNull(stream);
                    // don't do anything with the stream, there's a separate test for that
                }
            }

            @Test
            void testNotExisting() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.newDirectoryStream(path, entry -> true));
                assertEquals("/foo", exception.getFile());
            }

            @Test
            void testNotDirectory() {
                addFile("/foo");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                NotDirectoryException exception = assertThrows(NotDirectoryException.class, () -> provider.newDirectoryStream(path, entry -> true));
                assertEquals("/foo", exception.getFile());
            }

            @Test
            void testWithLinks() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                addFile("/foo/bar");
                SymbolicLinkEntry bar = addSymLink("/bar", foo);
                addSymLink("/baz", bar);

                try (DirectoryStream<Path> stream = provider().newDirectoryStream(createPath("/baz"), entry -> true)) {
                    Iterator<Path> iterator = stream.iterator();
                    assertTrue(iterator.hasNext());
                    assertEquals("bar", iterator.next().getFileName().toString());
                }
            }

            @Test
            void testWithBrokenLinks() {
                SymbolicLinkEntry bar = addSymLink("/bar", new DirectoryEntry("/foo"));
                addSymLink("/baz", bar);

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.newDirectoryStream(path, entry -> true));
                assertEquals("/foo", exception.getFile());
            }

            @Test
            void testWithLinkToFile() {
                FileEntry foo = addFile("/foo");
                addFile("/foo/bar");
                SymbolicLinkEntry bar = addSymLink("/bar", foo);
                addSymLink("/baz", bar);

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                NotDirectoryException exception = assertThrows(NotDirectoryException.class, () -> provider.newDirectoryStream(path, entry -> true));
                assertEquals("/foo", exception.getFile());
            }
        }

        @Nested
        class CreateDirectory {

            @Test
            void testSuccess() throws IOException {
                assertNull(getFileSystemEntry("/foo"));

                provider().createDirectory(createPath("/foo"));

                FileSystemEntry entry = getFileSystemEntry("/foo");
                assertThat(entry, instanceOf(DirectoryEntry.class));
            }

            @Test
            void testAlreadyExists() {
                addDirectory("/foo/bar");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.createDirectory(path));
                assertEquals("/foo/bar", exception.getFile());

                verify(getExceptionFactory(), never()).createCreateDirectoryException(anyString(), anyInt(), anyString());
                assertNotNull(getFileSystemEntry("/foo/bar"));
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCurrentDirectory(String dir) {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath(dir);

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.createDirectory(path));
                assertEquals(getDefaultDir(), exception.getFile());

                verify(getExceptionFactory(), never()).createCreateDirectoryException(anyString(), eq(550), anyString());
                assertNotNull(getFileSystemEntry(getDefaultDir()));
            }

            @Test
            void testFTPFailure() {
                DirectoryEntry root = getDirectory("/");
                root.setPermissionsFromString("r-xr-xr-x");

                // failure: read-only parent

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.createDirectory(path));
                assertEquals("/foo", exception.getFile());

                verify(getExceptionFactory()).createCreateDirectoryException(eq("/foo"), eq(550), anyString());
                assertNull(getFileSystemEntry("/foo"));
            }
        }

        @Nested
        class Delete {

            @Test
            void testNonExisting() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.delete(path));
                assertEquals("/foo", exception.getFile());

                verify(getExceptionFactory(), never()).createDeleteException(anyString(), anyInt(), anyString(), anyBoolean());
            }

            @Test
            void testRoot() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/");

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.delete(path));
                assertEquals("/", exception.getFile());

                verify(getExceptionFactory()).createDeleteException(eq("/"), eq(550), anyString(), eq(true));
            }

            @Test
            void testFile() throws IOException {
                addFile("/foo/bar");
                FileSystemEntry foo = getFileSystemEntry("/foo");

                provider().delete(createPath("/foo/bar"));

                assertSame(foo, getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testEmptyDir() throws IOException {
                addDirectory("/foo/bar");
                FileSystemEntry foo = getFileSystemEntry("/foo");

                provider().delete(createPath("/foo/bar"));

                assertSame(foo, getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/foo/bar"));
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCurrentDirectory(String dir) throws IOException {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath(dir);

                assertNotNull(getFileSystemEntry(getDefaultDir()));

                provider.delete(path);

                assertNull(getFileSystemEntry(getDefaultDir()));
            }

            @Test
            void testFTPFailure() {
                addDirectory("/foo/bar/baz");
                FileSystemEntry foo = getFileSystemEntry("/foo");
                FileSystemEntry bar = getFileSystemEntry("/foo/bar");

                // failure: non-empty directory

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.delete(path));
                assertEquals("/foo/bar", exception.getFile());

                verify(getExceptionFactory()).createDeleteException(eq("/foo/bar"), eq(550), anyString(), eq(true));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
            }
        }

        @Nested
        class ReadSymbolicLink {

            @Test
            void testToFile() throws IOException {
                FileEntry foo = addFile("/foo");
                addSymLink("/bar", foo);

                Path link = provider().readSymbolicLink(createPath("/bar"));
                assertEquals(createPath("/foo"), link);
            }

            @Test
            void testToDirectory() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                addSymLink("/bar", foo);

                Path link = provider().readSymbolicLink(createPath("/bar"));
                assertEquals(createPath("/foo"), link);
            }

            @Test
            void testToNonExistingTarget() throws IOException {
                addSymLink("/bar", new FileEntry("/foo"));

                Path link = provider().readSymbolicLink(createPath("/bar"));
                assertEquals(createPath("/foo"), link);
            }

            @Test
            void testNotExisting() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.readSymbolicLink(path));
                assertEquals("/foo", exception.getFile());
            }

            @Test
            void testNoLinkButFile() {
                addFile("/foo");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                NotLinkException exception = assertThrows(NotLinkException.class, () -> provider.readSymbolicLink(path));
                assertEquals("/foo", exception.getFile());
            }

            @ParameterizedTest
            @ValueSource(strings = { "/foo", CURRENT_DIR })
            @EmptySource
            void testNoLinkButDirectory(String dir) {
                addDirectoryIfNotExists(dir);

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath(dir);

                NotLinkException exception = assertThrows(NotLinkException.class, () -> provider.readSymbolicLink(path));
                assertEquals(dir, exception.getFile());
            }
        }

        @Nested
        class Copy {

            @Test
            void testSame() throws IOException {
                DirectoryEntry foo = addDirectory("/home/test/foo");
                DirectoryEntry bar = addDirectory("/home/test/foo/bar");

                CopyOption[] options = {};
                provider().copy(createPath("/home/test"), createPath(""), options);
                provider().copy(createPath("/home/test/foo"), createPath("foo"), options);
                provider().copy(createPath("/home/test/foo/bar"), createPath("foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/home/test/foo"));
                assertSame(bar, getFileSystemEntry("/home/test/foo/bar"));
                assertEquals(0, getChildCount("/home/test/foo/bar"));
            }

            @Test
            void testNonExisting() {
                DirectoryEntry foo = addDirectory("/foo");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/foo/bar");
                FTPPath target = createPath("/foo/baz");
                CopyOption[] options = {};

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.copy(source, target, options));
                assertEquals("/foo/bar", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertEquals(0, getChildCount("/foo"));
            }

            @Test
            void testFTPFailure() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");

                // failure: target parent does not exist

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/foo/bar");
                FTPPath target = createPath("/baz/bar");
                CopyOption[] options = {};

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.copy(source, target, options));
                assertEquals("/baz/bar", exception.getFile());

                verify(getExceptionFactory()).createNewOutputStreamException(eq("/baz/bar"), eq(553), anyString(), anyCollection());
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertNull(getFileSystemEntry("/baz"));
                assertNull(getFileSystemEntry("/baz/bar"));
            }

            @Test
            void testRoot() throws IOException {
                // copying a directory (including the root) will not copy its contents, so copying the root is allowed
                DirectoryEntry foo = addDirectory("/foo");

                CopyOption[] options = {};
                provider().copy(createPath("/"), createPath("/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));

                DirectoryEntry bar = getDirectory("/foo/bar");

                assertNotSame(getDirectory("/"), bar);
                assertEquals(0, getChildCount("/foo/bar"));
            }

            @Test
            void testReplaceFile() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath("/foo/bar");
                CopyOption[] options = {};

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.copy(source, target, options));
                assertEquals("/foo/bar", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceFileAllowed() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().copy(createPath("/baz"), createPath("/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));
                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
                // permissions are dropped during the delete/recreate
                assertEqualsMinusPath(bar, getFileSystemEntry("/foo/bar"), false);
                assertNotSame(baz, getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testReplaceNonEmptyDir() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath("/foo");
                CopyOption[] options = {};

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.copy(source, target, options));
                assertEquals("/foo", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceNonEmptyDirAllowed() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath("/foo");
                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.copy(source, target, options));
                assertEquals("/foo", exception.getFile());

                verify(getExceptionFactory()).createDeleteException(eq("/foo"), eq(550), anyString(), eq(true));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceEmptyDir() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath("/foo");
                CopyOption[] options = {};

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.copy(source, target, options));
                assertEquals("/foo", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceEmptyDirAllowed() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().copy(createPath("/baz"), createPath("/foo"), options);

                assertThat(getFileSystemEntry("/foo"), instanceOf(DirectoryEntry.class));
                assertNotSame(foo, getFileSystemEntry("/foo"));
                assertNotSame(baz, getFileSystemEntry("/foo"));
            }

            @Test
            void testFile() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                baz.setOwner("root");

                CopyOption[] options = {};
                provider().copy(createPath("/baz"), createPath("/foo/bar"), options);

                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertNotSame(baz, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
                assertNotEquals(baz.getOwner(), getFileSystemEntry("/foo/bar").getOwner());
            }

            @Test
            void testFileMultipleConnections() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                baz.setOwner("root");

                CopyOption[] options = {};
                multiClientFileSystem.copy(createPath(multiClientFileSystem, "/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertNotSame(baz, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
                assertNotEquals(baz.getOwner(), getFileSystemEntry("/foo/bar").getOwner());
            }

            @Test
            void testEmptyDir() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");

                baz.setOwner("root");

                CopyOption[] options = {};
                provider().copy(createPath("/baz"), createPath("/foo/bar"), options);

                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertNotSame(baz, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));

                DirectoryEntry bar = getDirectory("/foo/bar");
                assertEquals(0, getChildCount("/foo/bar"));
                assertNotEquals(baz.getOwner(), bar.getOwner());
            }

            @Test
            void testNonEmptyDir() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");
                addFile("/baz/qux");

                baz.setOwner("root");

                CopyOption[] options = {};
                provider().copy(createPath("/baz"), createPath("/foo/bar"), options);

                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertNotSame(baz, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));

                DirectoryEntry bar = getDirectory("/foo/bar");
                assertEquals(0, getChildCount("/foo/bar"));
                assertNotEquals(baz.getOwner(), bar.getOwner());
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCopyCurrentDir(String dir) throws IOException {
                addDirectory("/foo");

                CopyOption[] options = {};
                provider().copy(createPath(dir), createPath("/foo/bar"), options);

                assertNotNull(getDirectory("/foo"));
                assertNotNull(getDirectory("/foo/bar"));
                assertNotNull(getDirectory(getDefaultDir()));

                assertEquals(getChildCount(getDefaultDir()), getChildCount("/foo/bar"));
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCopyToCurrentDir(String dir) throws IOException {
                addDirectory("/baz");
                addFile("/baz/qux");

                int oldChildCount = getChildCount(getDefaultDir());

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().copy(createPath("/baz"), createPath(dir), options);

                assertNotNull(getDirectory(getDefaultDir()));
                assertNotNull(getDirectory("/baz"));

                assertEquals(oldChildCount, getChildCount(getDefaultDir()));
            }

            @Test
            void testReplaceFileDifferentFileSystems() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath(multiClientFileSystem, "/foo/bar");
                CopyOption[] options = {};

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.copy(source, target, options));
                assertEquals("/foo/bar", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceFileAllowedDifferentFileSystems() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));
                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
                // permissions are dropped during the copy/delete
                assertEqualsMinusPath(bar, getFileSystemEntry("/foo/bar"), false);
                assertNotSame(baz, getFileSystemEntry("/foo/bar"));
            }

            @Test
            void testReplaceNonEmptyDirDifferentFileSystems() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath(multiClientFileSystem, "/foo");
                CopyOption[] options = {};

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.copy(source, target, options));
                assertEquals("/foo", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceNonEmptyDirAllowedDifferentFileSystems() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath(multiClientFileSystem, "/foo");
                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.copy(source, target, options));
                assertEquals("/foo", exception.getFile());

                verify(getExceptionFactory()).createDeleteException(eq("/foo"), eq(550), anyString(), eq(true));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceEmptyDirDifferentFileSystems() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath(multiClientFileSystem, "/foo");
                CopyOption[] options = {};

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.copy(source, target, options));
                assertEquals("/foo", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceEmptyDirAllowedDifferentFileSystems() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo"), options);

                assertThat(getFileSystemEntry("/foo"), instanceOf(DirectoryEntry.class));
                assertNotSame(foo, getFileSystemEntry("/foo"));
                assertNotSame(baz, getFileSystemEntry("/foo"));
            }

            @Test
            void testFileDifferentFileSystems() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                baz.setOwner("root");

                CopyOption[] options = {};
                provider().copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertNotSame(baz, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
                assertNotEquals(baz.getOwner(), getFileSystemEntry("/foo/bar").getOwner());
            }

            @Test
            void testEmptyDirDifferentFileSystems() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");

                baz.setOwner("root");

                CopyOption[] options = {};
                provider().copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertNotSame(baz, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));

                DirectoryEntry bar = getDirectory("/foo/bar");
                assertEquals(0, getChildCount("/foo/bar"));
                assertNotEquals(baz.getOwner(), bar.getOwner());
            }

            @Test
            void testNonEmptyDirDifferentFileSystems() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");
                addFile("/baz/qux");

                baz.setOwner("root");

                CopyOption[] options = {};
                provider().copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertNotSame(baz, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));

                DirectoryEntry bar = getDirectory("/foo/bar");
                assertEquals(0, getChildCount("/foo/bar"));
                assertNotEquals(baz.getOwner(), bar.getOwner());
            }

            @Test
            void testWithAttributes() {
                addDirectory("/foo");
                addDirectory("/baz");
                addFile("/baz/qux");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath("/foo/bar");
                CopyOption[] options = { StandardCopyOption.COPY_ATTRIBUTES };

                UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                        () -> provider.copy(source, target, options));
                assertChainEquals(Messages.fileSystemProvider().unsupportedCopyOption(StandardCopyOption.COPY_ATTRIBUTES), exception);
            }
        }

        @Nested
        class Move {

            @Test
            void testSame() throws IOException {
                DirectoryEntry foo = addDirectory("/home/test/foo");
                DirectoryEntry bar = addDirectory("/home/test/foo/bar");
                SymbolicLinkEntry baz = addSymLink("/baz", foo);

                CopyOption[] options = {};
                provider().move(createPath("/"), createPath("/"), options);
                provider().move(createPath("/home/test"), createPath(""), options);
                provider().move(createPath("/home/test/foo"), createPath("foo"), options);
                provider().move(createPath("/home/test/foo/bar"), createPath("foo/bar"), options);
                provider().move(createPath("/home/test/foo"), createPath("/baz"), options);
                provider().move(createPath("/baz"), createPath("/home/test/foo"), options);

                assertSame(foo, getFileSystemEntry("/home/test/foo"));
                assertSame(bar, getFileSystemEntry("/home/test/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
                assertEquals(0, getChildCount("/home/test/foo/bar"));
            }

            @Test
            void testNonExisting() {
                DirectoryEntry foo = addDirectory("/foo");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/foo/bar");
                FTPPath target = createPath("/foo/baz");
                CopyOption[] options = {};

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.move(source, target, options));
                assertEquals("/foo/bar", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertEquals(0, getChildCount("/foo"));
            }

            @Test
            void testFTPFailure() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");

                // failure: non-existing target parent

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/foo/bar");
                FTPPath target = createPath("/baz/bar");
                CopyOption[] options = {};

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.move(source, target, options));
                assertEquals("/foo/bar", exception.getFile());
                assertEquals("/baz/bar", exception.getOtherFile());

                verify(getExceptionFactory()).createMoveException(eq("/foo/bar"), eq("/baz/bar"), eq(553), anyString());
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testEmptyRoot() {
                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/");
                FTPPath target = createPath("/baz");
                CopyOption[] options = {};

                DirectoryNotEmptyException exception = assertThrows(DirectoryNotEmptyException.class, () -> provider.move(source, target, options));
                assertEquals("/", exception.getFile());

                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testNonEmptyRoot() {
                DirectoryEntry foo = addDirectory("/foo");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/");
                FTPPath target = createPath("/baz");
                CopyOption[] options = {};

                DirectoryNotEmptyException exception = assertThrows(DirectoryNotEmptyException.class, () -> provider.move(source, target, options));
                assertEquals("/", exception.getFile());

                assertSame(foo, getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceFile() {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry bar = addDirectory("/foo/bar");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath("/foo/bar");
                CopyOption[] options = {};

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.move(source, target, options));
                assertEquals("/baz", exception.getFile());
                assertEquals("/foo/bar", exception.getOtherFile());

                verify(getExceptionFactory()).createMoveException(eq("/baz"), eq("/foo/bar"), eq(553), anyString());
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceFileAllowed() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().move(createPath("/baz"), createPath("/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));
                assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceEmptyDir() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath("/foo");
                CopyOption[] options = {};

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.move(source, target, options));
                assertEquals("/baz", exception.getFile());
                assertEquals("/foo", exception.getOtherFile());

                verify(getExceptionFactory()).createMoveException(eq("/baz"), eq("/foo"), eq(553), anyString());
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceEmptyDirAllowed() throws IOException {
                addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().move(createPath("/baz"), createPath("/foo"), options);

                assertEqualsMinusPath(baz, getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testFile() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                CopyOption[] options = {};
                provider().move(createPath("/baz"), createPath("/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));
                assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testEmptyDir() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");

                CopyOption[] options = {};
                provider().move(createPath("/baz"), createPath("/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));
                assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testNonEmptyDir() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");
                FileEntry qux = addFile("/baz/qux");

                CopyOption[] options = {};
                provider().move(createPath("/baz"), createPath("/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));
                assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
                assertEqualsMinusPath(qux, getFileSystemEntry("/foo/bar/qux"));
                assertEquals(1, getChildCount("/foo"));
                assertEquals(1, getChildCount("/foo/bar"));
            }

            @Test
            void testNonEmptyDirSameParent() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry bar = addFile("/foo/bar");

                CopyOption[] options = {};
                try {
                    provider().move(createPath("/foo"), createPath("/baz"), options);
                } finally {
                    assertNull(getFileSystemEntry("/foo"));
                    assertEqualsMinusPath(foo, getFileSystemEntry("/baz"));
                    assertEqualsMinusPath(bar, getFileSystemEntry("/baz/bar"));
                }
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testMoveCurrentDir(String dir) throws IOException {
                addDirectory("/foo");

                int oldChildCount = getChildCount(getDefaultDir());

                CopyOption[] options = {};
                provider().move(createPath(dir), createPath("/foo/bar"), options);

                assertNotNull(getDirectory("/foo"));
                assertNotNull(getDirectory("/foo/bar"));
                assertNull(getFileSystemEntry(getDefaultDir()));

                assertEquals(oldChildCount, getChildCount("/foo/bar"));
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testMoveToCurrentDir(String dir) throws IOException {
                addDirectory("/baz");
                addFile("/baz/qux");

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().move(createPath("/baz"), createPath(dir), options);

                assertNotNull(getDirectory(getDefaultDir()));
                assertNull(getFileSystemEntry("/baz"));

                assertEquals(1, getChildCount(getDefaultDir()));
            }

            @Test
            void testReplaceFileDifferentFileSystems() {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry bar = addDirectory("/foo/bar");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath(multiClientFileSystem, "/foo/bar");
                CopyOption[] options = {};

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.move(source, target, options));
                assertEquals("/foo/bar", exception.getFile());

                verify(getExceptionFactory(), never()).createMoveException(anyString(), anyString(), anyInt(), anyString());
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(bar, getFileSystemEntry("/foo/bar"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceFileAllowedDifferentFileSystems() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                addFile("/foo/bar");
                FileEntry baz = addFile("/baz");

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().move(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));
                // permissions are dropped during the copy/delete
                assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"), false);
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceEmptyDirDifferentFileSystems() {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath(multiClientFileSystem, "/foo");
                CopyOption[] options = {};

                FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class, () -> provider.move(source, target, options));
                assertEquals("/foo", exception.getFile());

                verify(getExceptionFactory(), never()).createMoveException(anyString(), anyString(), anyInt(), anyString());
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }

            @Test
            void testReplaceEmptyDirAllowedDifferentFileSystems() throws IOException {
                addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
                provider().move(createPath("/baz"), createPath(multiClientFileSystem, "/foo"), options);

                // permissions are dropped during the copy/delete
                assertEqualsMinusPath(baz, getFileSystemEntry("/foo"), false);
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testFileDifferentFileSystems() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                FileEntry baz = addFile("/baz");

                CopyOption[] options = {};
                provider().move(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));
                // permissions are dropped during the copy/delete
                assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"), false);
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testEmptyDirDifferentFileSystems() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");

                CopyOption[] options = {};
                provider().move(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

                assertSame(foo, getFileSystemEntry("/foo"));
                // permissions are dropped during the copy/delete
                assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"), false);
                assertNull(getFileSystemEntry("/baz"));
            }

            @Test
            void testNonEmptyDirDifferentFileSystems() {
                DirectoryEntry foo = addDirectory("/foo");
                DirectoryEntry baz = addDirectory("/baz");
                addFile("/baz/qux");

                FTPFileSystemProvider provider = provider();
                FTPPath source = createPath("/baz");
                FTPPath target = createPath(multiClientFileSystem, "/foo/bar");
                CopyOption[] options = {};

                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> provider.move(source, target, options));
                assertEquals("/baz", exception.getFile());

                verify(getExceptionFactory()).createDeleteException(eq("/baz"), eq(550), anyString(), eq(true));
                assertSame(foo, getFileSystemEntry("/foo"));
                assertSame(baz, getFileSystemEntry("/baz"));
            }
        }

        private void assertEqualsMinusPath(FileSystemEntry entry1, FileSystemEntry entry2) throws IOException {
            assertEqualsMinusPath(entry1, entry2, true);
        }

        private void assertEqualsMinusPath(FileSystemEntry entry1, FileSystemEntry entry2, boolean includePermissions) throws IOException {
            assertEquals(entry1.getClass(), entry2.getClass());
            assertEquals(entry1.getSize(), entry2.getSize());
            assertEquals(entry1.getOwner(), entry2.getOwner());
            assertEquals(entry1.getGroup(), entry2.getGroup());
            if (includePermissions) {
                assertEquals(entry1.getPermissions(), entry2.getPermissions());
            }

            if (entry1 instanceof FileEntry && entry2 instanceof FileEntry) {
                FileEntry file1 = (FileEntry) entry1;
                FileEntry file2 = (FileEntry) entry2;
                assertArrayEquals(getContents(file1), getContents(file2));
            }
        }

        @Nested
        class IsSameFile {

            @Test
            void testEquals() throws IOException {

                assertTrue(provider().isSameFile(createPath("/"), createPath("/")));
                assertTrue(provider().isSameFile(createPath("/foo"), createPath("/foo")));
                assertTrue(provider().isSameFile(createPath("/foo/bar"), createPath("/foo/bar")));

                assertTrue(provider().isSameFile(createPath(""), createPath("")));
                assertTrue(provider().isSameFile(createPath("foo"), createPath("foo")));
                assertTrue(provider().isSameFile(createPath("foo/bar"), createPath("foo/bar")));

                assertTrue(provider().isSameFile(createPath(""), createPath("/home/test")));
                assertTrue(provider().isSameFile(createPath("/home/test"), createPath("")));

                assertTrue(provider().isSameFile(createPath(CURRENT_DIR), createPath("/home/test")));
                assertTrue(provider().isSameFile(createPath("/home/test"), createPath(CURRENT_DIR)));

                assertTrue(provider().isSameFile(createPath(""), createPath(CURRENT_DIR)));
                assertTrue(provider().isSameFile(createPath(CURRENT_DIR), createPath("")));
            }

            @Test
            void testExisting() throws IOException {
                FileEntry bar = addFile("/home/test/foo/bar");
                addSymLink("/bar", bar);

                assertTrue(provider().isSameFile(createPath("/home/test"), createPath("")));
                assertTrue(provider().isSameFile(createPath("/home/test/foo"), createPath("foo")));
                assertTrue(provider().isSameFile(createPath("/home/test/foo/bar"), createPath("foo/bar")));

                assertTrue(provider().isSameFile(createPath(""), createPath("/home/test")));
                assertTrue(provider().isSameFile(createPath("foo"), createPath("/home/test/foo")));
                assertTrue(provider().isSameFile(createPath("foo/bar"), createPath("/home/test/foo/bar")));

                assertFalse(provider().isSameFile(createPath("foo"), createPath("foo/bar")));

                assertTrue(provider().isSameFile(createPath("/bar"), createPath("/home/test/foo/bar")));
            }

            @Test
            void testFirstNonExisting() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");
                FTPPath path2 = createPath("/");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.isSameFile(path, path2));
                assertEquals("/foo", exception.getFile());
            }

            @Test
            void testSecondNonExisting() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/");
                FTPPath path2 = createPath("/foo");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.isSameFile(path, path2));
                assertEquals("/foo", exception.getFile());
            }
        }

        @Nested
        class IsHidden {

            @Test
            void testSuccess() throws IOException {
                addDirectory("/foo");
                addDirectory("/.foo");
                addFile("/foo/bar");
                addFile("/foo/.bar");

                assertFalse(provider().isHidden(createPath("/foo")));
                assertTrue(provider().isHidden(createPath("/.foo")));
                assertFalse(provider().isHidden(createPath("/foo/bar")));
                assertTrue(provider().isHidden(createPath("/foo/.bar")));
                assertFalse(provider().isHidden(createPath("")));
                assertFalse(provider().isHidden(createPath(CURRENT_DIR)));
            }

            @Test
            void testNonExisting() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.isHidden(path));
                assertEquals("/foo", exception.getFile());
            }
        }

        @Nested
        class CheckAccess {

            @Test
            void testNonExisting() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> provider.checkAccess(path));
                assertEquals("/foo/bar", exception.getFile());
            }

            @Test
            void testNoModes() throws IOException {
                addDirectory("/foo/bar");

                provider().checkAccess(createPath("/foo/bar"));
            }

            @Test
            void testOnlyRead() throws IOException {
                addDirectory("/foo/bar");

                provider().checkAccess(createPath("/foo/bar"), AccessMode.READ);
            }

            @Test
            void testOnlyWriteNotReadOnly() throws IOException {
                addDirectory("/foo/bar");

                provider().checkAccess(createPath("/foo/bar"), AccessMode.WRITE);
            }

            @Test
            void testOnlyWriteReadOnly() {
                DirectoryEntry bar = addDirectory("/foo/bar");
                bar.setPermissionsFromString("r-xr-xr-x");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");

                AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> provider.checkAccess(path, AccessMode.WRITE));
                assertEquals("/foo/bar", exception.getFile());
            }

            @Test
            void testOnlyExecute() {
                DirectoryEntry bar = addDirectory("/foo/bar");
                bar.setPermissionsFromString("rw-rw-rw-");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo/bar");

                AccessDeniedException exception = assertThrows(AccessDeniedException.class, () -> provider.checkAccess(path, AccessMode.EXECUTE));
                assertEquals("/foo/bar", exception.getFile());
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCurrentDirectory(String dir) {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath(dir);

                assertDoesNotThrow(() -> provider.checkAccess(path, AccessMode.READ));
                assertDoesNotThrow(() -> provider.checkAccess(path, AccessMode.WRITE));
            }
        }

        @Nested
        class ReadAttributesObject {

            @Test
            void testFileFollowLinks() throws IOException {
                FileEntry foo = addFile("/foo");
                foo.setContents(new byte[1024]);
                foo.setPermissionsFromString("r-xr-xr-x");
                foo.setOwner("user");
                foo.setGroup("group");

                PosixFileAttributes attributes = provider().readAttributes(createPath("/foo"), PosixFileAttributes.class);

                assertEquals(foo.getSize(), attributes.size());
                assertEquals("user", attributes.owner().getName());
                assertEquals("group", attributes.group().getName());
                assertEquals(PosixFilePermissions.fromString("r-xr-xr-x"), attributes.permissions());
                assertFalse(attributes.isDirectory());
                assertTrue(attributes.isRegularFile());
                assertFalse(attributes.isSymbolicLink());
                assertFalse(attributes.isOther());
            }

            @Test
            void testFileNoFollowLinks() throws IOException {
                FileEntry foo = addFile("/foo");
                foo.setContents(new byte[1024]);
                foo.setPermissionsFromString("r-xr-xr-x");
                foo.setOwner("user");
                foo.setGroup("group");

                PosixFileAttributes attributes = provider().readAttributes(createPath("/foo"), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

                assertEquals(foo.getSize(), attributes.size());
                assertEquals("user", attributes.owner().getName());
                assertEquals("group", attributes.group().getName());
                assertEquals(PosixFilePermissions.fromString("r-xr-xr-x"), attributes.permissions());
                assertFalse(attributes.isDirectory());
                assertTrue(attributes.isRegularFile());
                assertFalse(attributes.isSymbolicLink());
                assertFalse(attributes.isOther());
            }

            @Test
            void testDirectoryFollowLinks() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setPermissionsFromString("r-xr-xr-x");
                foo.setOwner("user");
                foo.setGroup("group");

                PosixFileAttributes attributes = provider().readAttributes(createPath("/foo"), PosixFileAttributes.class);

                assertEquals(foo.getSize(), attributes.size());
                assertEquals("user", attributes.owner().getName());
                assertEquals("group", attributes.group().getName());
                assertEquals(PosixFilePermissions.fromString("r-xr-xr-x"), attributes.permissions());
                assertTrue(attributes.isDirectory());
                assertFalse(attributes.isRegularFile());
                assertFalse(attributes.isSymbolicLink());
                assertFalse(attributes.isOther());
            }

            @Test
            void testDirectoryNoFollowLinks() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setPermissionsFromString("r-xr-xr-x");
                foo.setOwner("user");
                foo.setGroup("group");

                PosixFileAttributes attributes = provider().readAttributes(createPath("/foo"), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

                assertEquals(foo.getSize(), attributes.size());
                assertEquals("user", attributes.owner().getName());
                assertEquals("group", attributes.group().getName());
                assertEquals(PosixFilePermissions.fromString("r-xr-xr-x"), attributes.permissions());
                assertTrue(attributes.isDirectory());
                assertFalse(attributes.isRegularFile());
                assertFalse(attributes.isSymbolicLink());
                assertFalse(attributes.isOther());
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCurrentDirectory(String dir) throws IOException {
                PosixFileAttributes attributes = provider().readAttributes(createPath(dir), PosixFileAttributes.class);

                // Directories always have size 0 when using sshd-core
                assertEquals(0, attributes.size());
                assertNotNull(attributes.owner().getName());
                assertNotNull(attributes.group().getName());
                assertNotNull(attributes.permissions());
                assertTrue(attributes.isDirectory());
                assertFalse(attributes.isRegularFile());
                assertFalse(attributes.isSymbolicLink());
                assertFalse(attributes.isOther());
            }

            @Test
            void testSymLinkToFileFollowLinks() throws IOException {
                FileEntry foo = addFile("/foo");
                foo.setContents(new byte[1024]);
                foo.setPermissionsFromString("r-xr-xr-x");
                foo.setOwner("user");
                foo.setGroup("group");
                SymbolicLinkEntry bar = addSymLink("/bar", foo);

                PosixFileAttributes attributes = provider().readAttributes(createPath("/bar"), PosixFileAttributes.class);

                assertEquals(foo.getSize(), attributes.size());
                assertNotEquals(bar.getSize(), attributes.size());
                assertEquals("user", attributes.owner().getName());
                assertEquals("group", attributes.group().getName());
                assertEquals(PosixFilePermissions.fromString("r-xr-xr-x"), attributes.permissions());
                assertFalse(attributes.isDirectory());
                assertTrue(attributes.isRegularFile());
                assertFalse(attributes.isSymbolicLink());
                assertFalse(attributes.isOther());
            }

            @Test
            void testSymLinkToFileNoFollowLinks() throws IOException {
                FileEntry foo = addFile("/foo");
                foo.setPermissionsFromString("r-xr-xr-x");
                foo.setOwner("user");
                foo.setGroup("group");
                SymbolicLinkEntry bar = addSymLink("/bar", foo);

                PosixFileAttributes attributes = provider().readAttributes(createPath("/bar"), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

                assertEquals(bar.getSize(), attributes.size());
                assertNotEquals(foo.getSize(), attributes.size());
                assertEquals("user", attributes.owner().getName());
                assertEquals("group", attributes.group().getName());
                assertEquals(PosixFilePermissions.fromString("rwxrwxrwx"), attributes.permissions());
                assertFalse(attributes.isDirectory());
                assertFalse(attributes.isRegularFile());
                assertTrue(attributes.isSymbolicLink());
                assertFalse(attributes.isOther());
            }

            @Test
            void testSymLinkToDirectoryFollowLinks() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setPermissionsFromString("r-xr-xr-x");
                foo.setOwner("user");
                foo.setGroup("group");
                SymbolicLinkEntry bar = addSymLink("/bar", foo);

                PosixFileAttributes attributes = provider().readAttributes(createPath("/bar"), PosixFileAttributes.class);

                assertEquals(foo.getSize(), attributes.size());
                assertNotEquals(bar.getSize(), attributes.size());
                assertEquals("user", attributes.owner().getName());
                assertEquals("group", attributes.group().getName());
                assertEquals(PosixFilePermissions.fromString("r-xr-xr-x"), attributes.permissions());
                assertTrue(attributes.isDirectory());
                assertFalse(attributes.isRegularFile());
                assertFalse(attributes.isSymbolicLink());
                assertFalse(attributes.isOther());
            }

            @Test
            void testSymLinkToDirectoryNoFollowLinks() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setPermissionsFromString("r-xr-xr-x");
                foo.setOwner("user");
                foo.setGroup("group");
                SymbolicLinkEntry bar = addSymLink("/bar", foo);

                PosixFileAttributes attributes = provider().readAttributes(createPath("/bar"), PosixFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

                assertEquals(bar.getSize(), attributes.size());
                assertNotEquals(foo.getSize(), attributes.size());
                assertEquals("user", attributes.owner().getName());
                assertEquals("group", attributes.group().getName());
                assertEquals(PosixFilePermissions.fromString("rwxrwxrwx"), attributes.permissions());
                assertFalse(attributes.isDirectory());
                assertFalse(attributes.isRegularFile());
                assertTrue(attributes.isSymbolicLink());
                assertFalse(attributes.isOther());
            }

            @Test
            void testNonExisting() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class,
                        () -> provider.readAttributes(path, PosixFileAttributes.class));
                assertEquals("/foo", exception.getFile());
            }

            @Test
            void testUnsupportedType() {
                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");
                Class<? extends BasicFileAttributes> type = DosFileAttributes.class;

                UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                        () -> provider.readAttributes(path, type));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttributesType(type), exception);
            }
        }

        @Nested
        class ReadAttributesMap {

            @ParameterizedTest(name = "{0}")
            @CsvSource({
                    "lastModifiedTime, lastModifiedTime",
                    "lastAccessTime, lastAccessTime",
                    "creationTime, creationTime",
                    "basic:lastModifiedTime, lastModifiedTime",
                    "basic:lastAccessTime, lastAccessTime",
                    "basic:creationTime, creationTime",
                    "posix:lastModifiedTime, lastModifiedTime",
                    "posix:lastAccessTime, lastAccessTime",
                    "posix:creationTime, creationTime"
            })
            void testSingleProperty(String attributeName, String expectedKey) throws IOException {
                addDirectory("/foo");
                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), attributeName);
                assertEquals(Collections.singleton(expectedKey), attributes.keySet());
                assertNotNull(attributes.get(expectedKey));
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "size", "basic:size", "posix:size" })
            void testSize(String attributeName) throws IOException {
                FileEntry foo = addFile("/foo");
                foo.setContents(new byte[1024]);
                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), attributeName);
                Map<String, ?> expected = Collections.singletonMap("size", foo.getSize());
                assertEquals(expected, attributes);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "isRegularFile", "basic:isRegularFile", "posix:isRegularFile" })
            void testIsRegularFile(String attributeName) throws IOException {
                addDirectory("/foo");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), attributeName);
                Map<String, ?> expected = Collections.singletonMap("isRegularFile", false);
                assertEquals(expected, attributes);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "isDirectory", "basic:isDirectory", "posix:isDirectory" })
            void testIsDirectory(String attributeName) throws IOException {
                addDirectory("/foo");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), attributeName);
                Map<String, ?> expected = Collections.singletonMap("isDirectory", true);
                assertEquals(expected, attributes);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "isSymbolicLink", "basic:isSymbolicLink", "posix:isSymbolicLink" })
            void testIsSymbolicLink(String attributeName) throws IOException {
                addDirectory("/foo");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), attributeName);
                Map<String, ?> expected = Collections.singletonMap("isSymbolicLink", false);
                assertEquals(expected, attributes);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "isOther", "basic:isOther", "posix:isOther" })
            void testIsOther(String attributeName) throws IOException {
                addDirectory("/foo");
                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), attributeName);
                Map<String, ?> expected = Collections.singletonMap("isOther", false);
                assertEquals(expected, attributes);
            }

            @ParameterizedTest(name = "{0}")
            @ValueSource(strings = { "fileKey", "basic:fileKey", "posix:fileKey" })
            void testFileKey(String attributeName) throws IOException {
                addDirectory("/foo");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), attributeName);
                Map<String, ?> expected = Collections.singletonMap("fileKey", null);
                assertEquals(expected, attributes);
            }

            @Test
            void testNoTypeMultiple() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "size,isDirectory");
                Map<String, Object> expected = new HashMap<>();
                expected.put("size", foo.getSize());
                expected.put("isDirectory", true);
                assertEquals(expected, attributes);
            }

            @Test
            void testNoTypeAll() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "*");
                Map<String, Object> expected = new HashMap<>();
                expected.put("size", foo.getSize());
                expected.put("isRegularFile", false);
                expected.put("isDirectory", true);
                expected.put("isSymbolicLink", false);
                expected.put("isOther", false);
                expected.put("fileKey", null);

                assertNotNull(attributes.remove("lastModifiedTime"));
                assertNotNull(attributes.remove("lastAccessTime"));
                assertNotNull(attributes.remove("creationTime"));
                assertEquals(expected, attributes);

                attributes = provider().readAttributes(createPath("/foo"), "lastModifiedTime,*");
                assertNotNull(attributes.remove("lastModifiedTime"));
                assertNotNull(attributes.remove("lastAccessTime"));
                assertNotNull(attributes.remove("creationTime"));
                assertEquals(expected, attributes);
            }

            @Test
            void testBasicMultiple() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "basic:size,isDirectory");
                Map<String, Object> expected = new HashMap<>();
                expected.put("size", foo.getSize());
                expected.put("isDirectory", true);
                assertEquals(expected, attributes);
            }

            @Test
            void testBasicAll() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "basic:*");
                Map<String, Object> expected = new HashMap<>();
                expected.put("size", foo.getSize());
                expected.put("isRegularFile", false);
                expected.put("isDirectory", true);
                expected.put("isSymbolicLink", false);
                expected.put("isOther", false);
                expected.put("fileKey", null);

                assertNotNull(attributes.remove("lastModifiedTime"));
                assertNotNull(attributes.remove("lastAccessTime"));
                assertNotNull(attributes.remove("creationTime"));
                assertEquals(expected, attributes);

                attributes = provider().readAttributes(createPath("/foo"), "basic:lastModifiedTime,*");
                assertNotNull(attributes.remove("lastModifiedTime"));
                assertNotNull(attributes.remove("lastAccessTime"));
                assertNotNull(attributes.remove("creationTime"));
                assertEquals(expected, attributes);
            }

            @Test
            void testOwnerOwner() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setOwner("test");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "owner:owner");
                Map<String, ?> expected = Collections.singletonMap("owner", new SimpleUserPrincipal(foo.getOwner()));
                assertEquals(expected, attributes);
            }

            @Test
            void testOwnerAll() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setOwner("test");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "owner:*");
                Map<String, Object> expected = new HashMap<>();
                expected.put("owner", new SimpleUserPrincipal(foo.getOwner()));
                assertEquals(expected, attributes);

                attributes = provider().readAttributes(createPath("/foo"), "owner:owner,*");
                assertEquals(expected, attributes);
            }

            @Test
            void testPosixOwner() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setOwner("test");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "posix:owner");
                Map<String, ?> expected = Collections.singletonMap("owner", new SimpleUserPrincipal(foo.getOwner()));
                assertEquals(expected, attributes);
            }

            @Test
            void testPosixGroup() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setGroup("test");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "posix:group");
                Map<String, ?> expected = Collections.singletonMap("group", new SimpleGroupPrincipal(foo.getGroup()));
                assertEquals(expected, attributes);
            }

            @Test
            void testPosixPermissions() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setPermissionsFromString("r-xr-xr-x");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "posix:permissions");
                Map<String, ?> expected = Collections.singletonMap("permissions",
                        PosixFilePermissions.fromString(foo.getPermissions().asRwxString()));
                assertEquals(expected, attributes);
            }

            @Test
            void testPosixMultiple() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setOwner("test");
                foo.setGroup("test");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "posix:size,owner,group");
                Map<String, Object> expected = new HashMap<>();
                expected.put("size", foo.getSize());
                expected.put("owner", new SimpleUserPrincipal(foo.getOwner()));
                expected.put("group", new SimpleGroupPrincipal(foo.getGroup()));
                assertEquals(expected, attributes);
            }

            @Test
            void testPosixAll() throws IOException {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setOwner("test");
                foo.setGroup("group");
                foo.setPermissionsFromString("r-xr-xr-x");

                Map<String, Object> attributes = provider().readAttributes(createPath("/foo"), "posix:*");
                Map<String, Object> expected = new HashMap<>();
                expected.put("size", foo.getSize());
                expected.put("isRegularFile", false);
                expected.put("isDirectory", true);
                expected.put("isSymbolicLink", false);
                expected.put("isOther", false);
                expected.put("fileKey", null);
                expected.put("owner", new SimpleUserPrincipal(foo.getOwner()));
                expected.put("group", new SimpleGroupPrincipal(foo.getGroup()));
                expected.put("permissions", PosixFilePermissions.fromString(foo.getPermissions().asRwxString()));

                assertNotNull(attributes.remove("lastModifiedTime"));
                assertNotNull(attributes.remove("lastAccessTime"));
                assertNotNull(attributes.remove("creationTime"));
                assertEquals(expected, attributes);

                attributes = provider().readAttributes(createPath("/foo"), "posix:lastModifiedTime,*");
                assertNotNull(attributes.remove("lastModifiedTime"));
                assertNotNull(attributes.remove("lastAccessTime"));
                assertNotNull(attributes.remove("creationTime"));
                assertEquals(expected, attributes);
            }

            @ParameterizedTest
            @ValueSource(strings = CURRENT_DIR)
            @EmptySource
            void testCurrentDirectory(String dir) throws IOException {
                Map<String, Object> attributes = provider().readAttributes(createPath(dir), "*");
                Map<String, Object> expected = new HashMap<>();
                // Directories always have size 0 when using sshd-core
                expected.put("size", 0L);
                expected.put("isRegularFile", false);
                expected.put("isDirectory", true);
                expected.put("isSymbolicLink", false);
                expected.put("isOther", false);
                expected.put("fileKey", null);

                assertNotNull(attributes.remove("lastModifiedTime"));
                assertNotNull(attributes.remove("lastAccessTime"));
                assertNotNull(attributes.remove("creationTime"));
                assertEquals(expected, attributes);

                attributes = provider().readAttributes(createPath(dir), "lastModifiedTime,*");
                assertNotNull(attributes.remove("lastModifiedTime"));
                assertNotNull(attributes.remove("lastAccessTime"));
                assertNotNull(attributes.remove("creationTime"));
                assertEquals(expected, attributes);
            }

            @Test
            void testUnsupportedAttribute() {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setOwner("test");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> provider.readAttributes(path, "posix:lastModifiedTime,owner,dummy"));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttribute("dummy"), exception);
            }

            @ParameterizedTest(name = "{0}")
            @CsvSource({
                    "basic:owner, owner",
                    "basic:permissions, permissions",
                    "basic:group, group",
                    "owner:permissions, permissions",
                    "owner:group, group",
                    "owner:size, size"
            })
            void testSupportedAttributeForWrongView(String attributes, String attribute) {
                DirectoryEntry foo = addDirectory("/foo");
                foo.setOwner("test");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> provider.readAttributes(path, attributes));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttribute(attribute), exception);
            }

            @Test
            void testUnsupportedView() {
                addDirectory("/foo");

                FTPFileSystemProvider provider = provider();
                FTPPath path = createPath("/foo");

                UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                        () -> provider.readAttributes(path, "zipfs:*"));
                assertChainEquals(Messages.fileSystemProvider().unsupportedFileAttributeView("zipfs"), exception);
            }
        }

        @Test
        void testPrefixAttributes() {
            Map<String, Object> attributes = new HashMap<>();
            attributes.put("size", 1L);
            attributes.put("isDirectory", "false");
            attributes.put("owner", new SimpleUserPrincipal("test"));

            Map<String, Object> expected = new HashMap<>();
            expected.put("posix:size", 1L);
            expected.put("posix:isDirectory", "false");
            expected.put("posix:owner", new SimpleUserPrincipal("test"));

            assertEquals(expected, FTPFileSystem.prefixAttributes(attributes, FileAttributeViewMetadata.POSIX));
        }

        @Nested
        class GetFTPFile {

            @Test
            void testFile() throws IOException {
                addFile("/foo");

                FTPFile file = fileSystem.getFTPFile(createPath("/foo"));
                assertNotNull(file);
                assertEquals("foo", file.getName());
                assertTrue(file.isFile());
            }

            @Test
            void testFileNotExisting() {
                FTPPath path = createPath("/foo");

                NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> fileSystem.getFTPFile(path));
                assertEquals("/foo", exception.getFile());

                VerificationMode verificationMode = useUnixFtpServer() && usesUnixFTPFileStrategyFactory() ? times(1) : never();
                verify(getExceptionFactory(), verificationMode).createGetFileException(eq("/foo"), eq(226), anyString());
            }

            @Test
            void testFileAccessDenied() throws IOException {
                addFile("/foo/bar");
                getFile("/foo/bar").setPermissionsFromString("---------");

                if (useUnixFtpServer() && usesUnixFTPFileStrategyFactory()) {
                    assertThrows(NoSuchFileException.class, this::testFileAccessDenied0);
                } else {
                    testFileAccessDenied0();
                }
            }

            private void testFileAccessDenied0() throws IOException {
                try {
                    FTPFile file = fileSystem.getFTPFile(createPath("/foo/bar"));
                    assertNotNull(file);
                    assertEquals("bar", file.getName());
                    assertTrue(file.isFile());
                    for (int access = FTPFile.USER_ACCESS; access <= FTPFile.WORLD_ACCESS; access++) {
                        for (int permission = FTPFile.READ_PERMISSION; permission <= FTPFile.EXECUTE_PERMISSION; permission++) {
                            assertFalse(file.hasPermission(access, permission));
                        }
                    }

                } finally {
                    VerificationMode verificationMode = useUnixFtpServer() && usesUnixFTPFileStrategyFactory() ? times(1) : never();
                    verify(getExceptionFactory(), verificationMode).createGetFileException(eq("/foo/bar"), eq(550), anyString());
                }
            }

            @Test
            void testDirectory() throws IOException {
                addDirectory("/foo");

                FTPFile file = fileSystem.getFTPFile(createPath("/foo"));
                assertNotNull(file);
                if (useUnixFtpServer() && usesUnixFTPFileStrategyFactory()) {
                    assertEquals(".", file.getName());
                } else {
                    assertEquals("foo", file.getName());
                }
                assertTrue(file.isDirectory());
            }

            @Test
            void testDirectoryAccessDenied() throws IOException {
                DirectoryEntry bar = addDirectory("/foo/bar");
                bar.setPermissionsFromString("---------");

                if (useUnixFtpServer() && usesUnixFTPFileStrategyFactory()) {
                    assertThrows(NoSuchFileException.class, this::testDirectoryAccessDenied0);
                } else {
                    testDirectoryAccessDenied0();
                }
            }

            private void testDirectoryAccessDenied0() throws IOException {
                try {
                    FTPFile file = fileSystem.getFTPFile(createPath("/foo/bar"));
                    assertNotNull(file);
                    assertEquals("bar", file.getName());
                    assertTrue(file.isDirectory());
                    for (int access = FTPFile.USER_ACCESS; access <= FTPFile.WORLD_ACCESS; access++) {
                        for (int permission = FTPFile.READ_PERMISSION; permission <= FTPFile.EXECUTE_PERMISSION; permission++) {
                            assertFalse(file.hasPermission(access, permission));
                        }
                    }

                } finally {
                    VerificationMode verificationMode = useUnixFtpServer() && usesUnixFTPFileStrategyFactory() ? times(1) : never();
                    verify(getExceptionFactory(), verificationMode).createGetFileException(eq("/foo/bar"), eq(550), anyString());
                }
            }
        }
    }
}
