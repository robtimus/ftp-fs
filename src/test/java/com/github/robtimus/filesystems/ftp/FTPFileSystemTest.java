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

import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.AUTO_DETECT;
import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.NON_UNIX;
import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.UNIX;
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
import org.junit.jupiter.params.provider.MethodSource;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockito.verification.VerificationMode;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.attribute.SimpleGroupPrincipal;
import com.github.robtimus.filesystems.attribute.SimpleUserPrincipal;
import com.github.robtimus.filesystems.ftp.server.SymbolicLinkEntry;

@SuppressWarnings("nls")
class FTPFileSystemTest {

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

        // FTPFileSystem.getPath

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

        // FTPFileSystem.keepAlive

        @Test
        void testKeepAlive() {
            assertDoesNotThrow(fileSystem::keepAlive);
        }

        // FTPFileSystem.toUri

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
            URI actual = fileSystem.toUri(createPath(path));
            assertEquals(expectedUri, actual);
        }

        // FTPFileSystem.toAbsolutePath

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
            Path actual = fileSystem.toAbsolutePath(createPath(path));
            assertEquals(expectedPath, actual);
        }

        // FTPFileSystem.toRealPath

        @Test
        void testToRealPathNoFollowLinks() throws IOException {
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

            testToRealPathNoFollowLinks("/", "/");
            testToRealPathNoFollowLinks("/foo/bar", "/foo/bar");
            testToRealPathNoFollowLinks("/foo/../bar", "/bar");

            testToRealPathNoFollowLinks("", "/home/test");
            testToRealPathNoFollowLinks("foo/bar", "/home/test/foo/bar");
            testToRealPathNoFollowLinks("foo/../bar", "/home/test/bar");

            // symbolic links
            testToRealPathNoFollowLinks("/hello", "/hello");
            testToRealPathNoFollowLinks("/world", "/world");
            testToRealPathNoFollowLinks("/home/test/baz", "/home/test/baz");
            testToRealPathNoFollowLinks("/baz", "/baz");
        }

        private void testToRealPathNoFollowLinks(String path, String expected) throws IOException {
            FTPPath expectedPath = createPath(expected);
            Path actual = fileSystem.toRealPath(createPath(path), LinkOption.NOFOLLOW_LINKS);
            assertEquals(expectedPath, actual);
        }

        @Test
        void testToRealPathFollowLinks() throws IOException {
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

            testToRealPathFollowLinks("/", "/");
            testToRealPathFollowLinks("/foo/bar", "/foo/bar");
            testToRealPathFollowLinks("/foo/../bar", "/bar");

            testToRealPathFollowLinks("", "/home/test");
            testToRealPathFollowLinks("foo/bar", "/home/test/foo/bar");
            testToRealPathFollowLinks("foo/../bar", "/home/test/bar");

            // symbolic links
            testToRealPathFollowLinks("/hello", "/foo");
            testToRealPathFollowLinks("/world", "/foo");
            testToRealPathFollowLinks("/home/test/baz", "/home/test/bar");
            testToRealPathFollowLinks("/baz", "/home/test/bar");
        }

        private void testToRealPathFollowLinks(String path, String expected) throws IOException {
            FTPPath expectedPath = createPath(expected);
            Path actual = fileSystem.toRealPath(createPath(path));
            assertEquals(expectedPath, actual);
        }

        @Test
        void testToRealPathBrokenLink() {
            addSymLink("/foo", new FileEntry("/bar"));

            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> createPath("/foo").toRealPath());
            assertEquals("/bar", exception.getFile());
        }

        @Test
        void testToRealPathNotExisting() {
            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> createPath("/foo").toRealPath());
            assertEquals("/foo", exception.getFile());
        }

        // FTPFileSystem.newInputStream

        @Test
        void testNewInputStream() throws IOException {
            addFile("/foo/bar");

            try (InputStream input = fileSystem.newInputStream(createPath("/foo/bar"))) {
                // don't do anything with the stream, there's a separate test for that
            }
            // verify that the file system can be used after closing the stream
            fileSystem.checkAccess(createPath("/foo/bar"));
        }

        @Test
        void testNewInputStreamDeleteOnClose() throws IOException {
            addFile("/foo/bar");

            OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
            try (InputStream input = fileSystem.newInputStream(createPath("/foo/bar"), options)) {
                // don't do anything with the stream, there's a separate test for that
            }
            assertNull(getFileSystemEntry("/foo/bar"));
            assertEquals(0, getChildCount("/foo"));
        }

        @Test
        void testNewInputStreamFTPFailure() {

            // failure: file not found

            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> fileSystem.newInputStream(createPath("/foo/bar")));
            assertEquals("/foo/bar", exception.getFile());

            verify(getExceptionFactory()).createNewInputStreamException(eq("/foo/bar"), eq(550), anyString());
        }

        // FTPFileSystem.newOutputStream

        @Test
        void testNewOutputStreamExisting() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");

            OpenOption[] options = { StandardOpenOption.WRITE };
            try (OutputStream output = fileSystem.newOutputStream(createPath("/foo/bar"), options)) {
                // don't do anything with the stream, there's a separate test for that
            }
            // verify that the file system can be used after closing the stream
            fileSystem.checkAccess(createPath("/foo/bar"));

            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testNewOutputStreamExistingDeleteOnClose() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");

            OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
            try (OutputStream output = fileSystem.newOutputStream(createPath("/foo/bar"), options)) {
                // don't do anything with the stream, there's a separate test for that
                assertSame(bar, getFileSystemEntry("/foo/bar"));
            }
            // verify that the file system can be used after closing the stream
            fileSystem.checkAccess(createPath("/foo"));

            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
            assertEquals(0, getChildCount("/foo"));
        }

        @Test
        void testNewOutputStreamExistingCreate() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");

            OpenOption[] options = { StandardOpenOption.CREATE };
            try (OutputStream output = fileSystem.newOutputStream(createPath("/foo/bar"), options)) {
                // don't do anything with the stream, there's a separate test for that
            }
            // verify that the file system can be used after closing the stream
            fileSystem.checkAccess(createPath("/foo/bar"));

            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testNewOutputStreamExistingCreateDeleteOnClose() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");

            OpenOption[] options = { StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE };
            try (OutputStream output = fileSystem.newOutputStream(createPath("/foo/bar"), options)) {
                // don't do anything with the stream, there's a separate test for that
                assertSame(bar, getFileSystemEntry("/foo/bar"));
            }
            // verify that the file system can be used after closing the stream
            fileSystem.checkAccess(createPath("/foo"));

            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testNewOutputStreamExistingCreateNew() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");

            OpenOption[] options = { StandardOpenOption.CREATE_NEW };
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.newOutputStream(createPath("/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            // verify that the file system can be used after closing the stream
            assertDoesNotThrow(() -> fileSystem.checkAccess(createPath("/foo/bar")));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testNewOutputStreamExistingFTPFailure() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            bar.setPermissionsFromString("r--r--r--");

            // failure: no permission to write

            OpenOption[] options = { StandardOpenOption.WRITE };
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.newOutputStream(createPath("/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            verify(getExceptionFactory()).createNewOutputStreamException(eq("/foo/bar"), eq(553), anyString(), anyCollection());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testNewOutputStreamExistingFTPFailureDeleteOnClose() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            bar.setPermissionsFromString("r--r--r--");

            // failure: no permission to write

            OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.newOutputStream(createPath("/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            verify(getExceptionFactory()).createNewOutputStreamException(eq("/foo/bar"), eq(553), anyString(), anyCollection());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testNewOutputStreamNonExistingNoCreate() {
            DirectoryEntry foo = addDirectory("/foo");

            OpenOption[] options = { StandardOpenOption.WRITE };
            NoSuchFileException exception = assertThrows(NoSuchFileException.class,
                    () -> fileSystem.newOutputStream(createPath("/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testNewOutputStreamNonExistingCreate() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");

            OpenOption[] options = { StandardOpenOption.CREATE };
            try (OutputStream input = fileSystem.newOutputStream(createPath("/foo/bar"), options)) {
                // don't do anything with the stream, there's a separate test for that
            } finally {
                assertSame(foo, getFileSystemEntry("/foo"));
                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
            }
        }

        @Test
        void testNewOutputStreamNonExistingCreateDeleteOnClose() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");

            OpenOption[] options = { StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE };
            try (OutputStream input = fileSystem.newOutputStream(createPath("/foo/bar"), options)) {
                // don't do anything with the stream, there's a separate test for that
                // we can't check here that /foo/bar exists, because it will only be stored in the file system once the stream is closed
            } finally {
                assertSame(foo, getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/foo/bar"));
                assertEquals(0, getChildCount("/foo"));
            }
        }

        @Test
        void testNewOutputStreamNonExistingCreateNew() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");

            OpenOption[] options = { StandardOpenOption.CREATE_NEW };
            try (OutputStream input = fileSystem.newOutputStream(createPath("/foo/bar"), options)) {
                // don't do anything with the stream, there's a separate test for that
            } finally {
                assertSame(foo, getFileSystemEntry("/foo"));
                assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
            }
        }

        @Test
        void testNewOutputStreamNonExistingCreateNewDeleteOnClose() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");

            OpenOption[] options = { StandardOpenOption.CREATE_NEW, StandardOpenOption.DELETE_ON_CLOSE };
            try (OutputStream input = fileSystem.newOutputStream(createPath("/foo/bar"), options)) {
                // don't do anything with the stream, there's a separate test for that
                // we can't check here that /foo/bar exists, because it will only be stored in the file system once the stream is closed
            } finally {
                assertSame(foo, getFileSystemEntry("/foo"));
                assertNull(getFileSystemEntry("/foo/bar"));
            }
        }

        @Test
        void testNewOutputStreamDirectoryNoCreate() {
            DirectoryEntry foo = addDirectory("/foo");

            OpenOption[] options = { StandardOpenOption.WRITE };
            FileSystemException exception = assertThrows(FileSystemException.class, () -> fileSystem.newOutputStream(createPath("/foo"), options));
            assertEquals("/foo", exception.getFile());
            assertEquals(Messages.fileSystemProvider().isDirectory("/foo").getReason(), exception.getReason());

            verify(getExceptionFactory(), never()).createNewOutputStreamException(anyString(), anyInt(), anyString(), anyCollection());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertEquals(0, getChildCount("/foo"));
        }

        @Test
        void testNewOutputStreamDirectoryDeleteOnClose() {
            DirectoryEntry foo = addDirectory("/foo");

            OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
            FileSystemException exception = assertThrows(FileSystemException.class, () -> fileSystem.newOutputStream(createPath("/foo"), options));
            assertEquals("/foo", exception.getFile());
            assertEquals(Messages.fileSystemProvider().isDirectory("/foo").getReason(), exception.getReason());

            verify(getExceptionFactory(), never()).createNewOutputStreamException(anyString(), anyInt(), anyString(), anyCollection());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertEquals(0, getChildCount("/foo"));
        }

        // FTPFileSystem.newByteChannel

        @Test
        void testNewByteChannelRead() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            bar.setContents(new byte[1024]);

            Set<? extends OpenOption> options = EnumSet.noneOf(StandardOpenOption.class);
            try (SeekableByteChannel channel = fileSystem.newByteChannel(createPath("/foo/bar"), options)) {
                // don't do anything with the channel, there's a separate test for that
                assertEquals(bar.getSize(), channel.size());
            }
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testNewByteChannelReadNonExisting() {

            // failure: file does not exist

            Set<? extends OpenOption> options = EnumSet.noneOf(StandardOpenOption.class);
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.newByteChannel(createPath("/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            verify(getExceptionFactory()).createNewInputStreamException(eq("/foo/bar"), eq(550), anyString());
            assertNull(getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testNewByteChannelWrite() throws IOException {
            addFile("/foo/bar");

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.WRITE);
            try (SeekableByteChannel channel = fileSystem.newByteChannel(createPath("/foo/bar"), options)) {
                // don't do anything with the channel, there's a separate test for that
                assertEquals(0, channel.size());
            }
        }

        @Test
        void testNewByteChannelWriteAppend() throws IOException {
            FileEntry bar = addFile("/foo/bar");
            bar.setContents(new byte[1024]);

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND);
            try (SeekableByteChannel channel = fileSystem.newByteChannel(createPath("/foo/bar"), options)) {
                // don't do anything with the channel, there's a separate test for that
                assertEquals(bar.getSize(), channel.size());
            }
        }

        @Test
        void testNewByteChannelCreateWriteExisting() throws IOException {
            FileEntry bar = addFile("/foo/bar");

            byte[] newContents = "Lorem ipsum".getBytes();

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            try (SeekableByteChannel channel = fileSystem.newByteChannel(createPath("/foo/bar"), options)) {
                // don't do anything with the channel, there's a separate test for that
                assertEquals(0, channel.size());
                channel.write(ByteBuffer.wrap(newContents));
                assertEquals(newContents.length, channel.size());
            }

            assertArrayEquals(newContents, getContents(bar));
        }

        @Test
        void testNewByteChannelCreateAppendExisting() throws IOException {
            FileEntry bar = addFile("/foo/bar");
            bar.setContents(new byte[1024]);

            byte[] newContents = "Lorem ipsum".getBytes();

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            try (SeekableByteChannel channel = fileSystem.newByteChannel(createPath("/foo/bar"), options)) {
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
        void testNewByteChannelCreateWriteNonExisting() throws IOException {
            addDirectory("/foo");

            byte[] newContents = "Lorem ipsum".getBytes();

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            try (SeekableByteChannel channel = fileSystem.newByteChannel(createPath("/foo/bar"), options)) {
                // don't do anything with the channel, there's a separate test for that
                assertEquals(0, channel.size());
                channel.write(ByteBuffer.wrap(newContents));
                assertEquals(newContents.length, channel.size());
            }

            FileEntry bar = getFile("/foo/bar");

            assertArrayEquals(newContents, getContents(bar));
        }

        @Test
        void testNewByteChannelCreateAppendNonExisting() throws IOException {
            addDirectory("/foo");

            byte[] newContents = "Lorem ipsum".getBytes();

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            try (SeekableByteChannel channel = fileSystem.newByteChannel(createPath("/foo/bar"), options)) {
                // don't do anything with the channel, there's a separate test for that
                assertEquals(0, channel.size());
                channel.write(ByteBuffer.wrap(newContents));
                assertEquals(newContents.length, channel.size());
            }

            FileEntry bar = getFile("/foo/bar");

            assertArrayEquals(newContents, getContents(bar));
        }

        @Test
        void testNewByteChannelCreateNewWriteExisting() throws IOException {
            FileEntry bar = addFile("/foo/bar");
            byte[] oldContents = "Hello World".getBytes();
            bar.setContents(oldContents);

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.newByteChannel(createPath("/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            assertArrayEquals(oldContents, getContents(bar));
        }

        @Test
        void testNewByteChannelCreateNewAppendExisting() throws IOException {
            FileEntry bar = addFile("/foo/bar");
            bar.setContents(new byte[1024]);

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND);
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.newByteChannel(createPath("/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            assertArrayEquals(new byte[1024], getContents(bar));
        }

        @Test
        void testNewByteChannelCreateNewWriteNonExisting() throws IOException {
            addDirectory("/foo");

            byte[] newContents = "Lorem ipsum".getBytes();

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            try (SeekableByteChannel channel = fileSystem.newByteChannel(createPath("/foo/bar"), options)) {
                // don't do anything with the channel, there's a separate test for that
                assertEquals(0, channel.size());
                channel.write(ByteBuffer.wrap(newContents));
                assertEquals(newContents.length, channel.size());
            }

            FileEntry bar = getFile("/foo/bar");

            assertArrayEquals(newContents, getContents(bar));
        }

        @Test
        void testNewByteChannelCreateNewAppendNonExisting() throws IOException {
            addDirectory("/foo");

            byte[] newContents = "Lorem ipsum".getBytes();

            Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.CREATE_NEW, StandardOpenOption.APPEND);
            try (SeekableByteChannel channel = fileSystem.newByteChannel(createPath("/foo/bar"), options)) {
                // don't do anything with the channel, there's a separate test for that
                assertEquals(0, channel.size());
                channel.write(ByteBuffer.wrap(newContents));
                assertEquals(newContents.length, channel.size());
            }

            FileEntry bar = getFile("/foo/bar");

            assertArrayEquals(newContents, getContents(bar));
        }

        // FTPFileSystem.newDirectoryStream

        @Test
        void testNewDirectoryStream() throws IOException {

            try (DirectoryStream<Path> stream = fileSystem.newDirectoryStream(createPath("/"), entry -> true)) {
                assertNotNull(stream);
                // don't do anything with the stream, there's a separate test for that
            }
        }

        @Test
        void testNewDirectoryStreamNotExisting() {
            NoSuchFileException exception = assertThrows(NoSuchFileException.class,
                    () -> fileSystem.newDirectoryStream(createPath("/foo"), entry -> true));
            assertEquals("/foo", exception.getFile());
        }

        @Test
        void testNewDirectoryStreamNotDirectory() {
            addFile("/foo");

            NotDirectoryException exception = assertThrows(NotDirectoryException.class,
                    () -> fileSystem.newDirectoryStream(createPath("/foo"), entry -> true));
            assertEquals("/foo", exception.getFile());
        }

        @Test
        void testnewDirectoryStreamWithLinks() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            addFile("/foo/bar");
            SymbolicLinkEntry bar = addSymLink("/bar", foo);
            addSymLink("/baz", bar);

            try (DirectoryStream<Path> stream = fileSystem.newDirectoryStream(createPath("/baz"), entry -> true)) {
                Iterator<Path> iterator = stream.iterator();
                assertTrue(iterator.hasNext());
                assertEquals("bar", iterator.next().getFileName().toString());
            }
        }

        @Test
        void testnewDirectoryStreamWithBrokenLinks() {
            SymbolicLinkEntry bar = addSymLink("/bar", new DirectoryEntry("/foo"));
            addSymLink("/baz", bar);

            NoSuchFileException exception = assertThrows(NoSuchFileException.class,
                    () -> fileSystem.newDirectoryStream(createPath("/foo"), entry -> true));
            assertEquals("/foo", exception.getFile());
        }

        @Test
        void testnewDirectoryStreamWithLinkToFile() {
            FileEntry foo = addFile("/foo");
            addFile("/foo/bar");
            SymbolicLinkEntry bar = addSymLink("/bar", foo);
            addSymLink("/baz", bar);

            NotDirectoryException exception = assertThrows(NotDirectoryException.class,
                    () -> fileSystem.newDirectoryStream(createPath("/foo"), entry -> true));
            assertEquals("/foo", exception.getFile());
        }

        // FTPFileSystem.createNewDirectory

        @Test
        void testCreateDirectory() throws IOException {
            assertNull(getFileSystemEntry("/foo"));

            fileSystem.createDirectory(createPath("/foo"));

            FileSystemEntry entry = getFileSystemEntry("/foo");
            assertThat(entry, instanceOf(DirectoryEntry.class));
        }

        @Test
        void testCreateDirectoryAlreadyExists() {
            addDirectory("/foo/bar");

            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.createDirectory(createPath("/foo/bar")));
            assertEquals("/foo/bar", exception.getFile());

            verify(getExceptionFactory(), never()).createCreateDirectoryException(anyString(), anyInt(), anyString());
            assertNotNull(getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testCreateDirectoryFTPFailure() {
            DirectoryEntry root = getDirectory("/");
            root.setPermissionsFromString("r-xr-xr-x");

            // failure: read-only parent

            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> fileSystem.createDirectory(createPath("/foo")));
            assertEquals("/foo", exception.getFile());

            verify(getExceptionFactory()).createCreateDirectoryException(eq("/foo"), eq(550), anyString());
            assertNull(getFileSystemEntry("/foo"));
        }

        // FTPFileSystem.delete

        @Test
        void testDeleteNonExisting() {

            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> fileSystem.delete(createPath("/foo")));
            assertEquals("/foo", exception.getFile());

            verify(getExceptionFactory(), never()).createDeleteException(anyString(), anyInt(), anyString(), anyBoolean());
        }

        @Test
        void testDeleteRoot() {

            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> fileSystem.delete(createPath("/")));
            assertEquals("/", exception.getFile());

            verify(getExceptionFactory()).createDeleteException(eq("/"), eq(550), anyString(), eq(true));
        }

        @Test
        void testDeleteFile() throws IOException {
            addFile("/foo/bar");
            FileSystemEntry foo = getFileSystemEntry("/foo");

            fileSystem.delete(createPath("/foo/bar"));

            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testDeleteEmptyDir() throws IOException {
            addDirectory("/foo/bar");
            FileSystemEntry foo = getFileSystemEntry("/foo");

            fileSystem.delete(createPath("/foo/bar"));

            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testDeleteFTPFailure() {
            addDirectory("/foo/bar/baz");
            FileSystemEntry foo = getFileSystemEntry("/foo");
            FileSystemEntry bar = getFileSystemEntry("/foo/bar");

            // failure: non-empty directory

            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> fileSystem.delete(createPath("/foo/bar")));
            assertEquals("/foo/bar", exception.getFile());

            verify(getExceptionFactory()).createDeleteException(eq("/foo/bar"), eq(550), anyString(), eq(true));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }

        // FTPFileSystem.readSymbolicLink

        @Test
        void testReadSymbolicLinkToFile() throws IOException {
            FileEntry foo = addFile("/foo");
            addSymLink("/bar", foo);

            FTPPath link = fileSystem.readSymbolicLink(createPath("/bar"));
            assertEquals(createPath("/foo"), link);
        }

        @Test
        void testReadSymbolicLinkToDirectory() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            addSymLink("/bar", foo);

            FTPPath link = fileSystem.readSymbolicLink(createPath("/bar"));
            assertEquals(createPath("/foo"), link);
        }

        @Test
        void testReadSymbolicLinkToNonExistingTarget() throws IOException {
            addSymLink("/bar", new FileEntry("/foo"));

            FTPPath link = fileSystem.readSymbolicLink(createPath("/bar"));
            assertEquals(createPath("/foo"), link);
        }

        @Test
        void testReadSymbolicLinkNotExisting() {

            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> fileSystem.readSymbolicLink(createPath("/foo")));
            assertEquals("/foo", exception.getFile());
        }

        @Test
        void testReadSymbolicLinkNoLinkButFile() {
            addFile("/foo");

            NotLinkException exception = assertThrows(NotLinkException.class, () -> fileSystem.readSymbolicLink(createPath("/foo")));
            assertEquals("/foo", exception.getFile());
        }

        @Test
        void testReadSymbolicLinkNoLinkButDirectory() {
            addDirectory("/foo");

            NotLinkException exception = assertThrows(NotLinkException.class, () -> fileSystem.readSymbolicLink(createPath("/foo")));
            assertEquals("/foo", exception.getFile());
        }

        // FTPFileSystem.copy

        @Test
        void testCopySame() throws IOException {
            DirectoryEntry foo = addDirectory("/home/test/foo");
            DirectoryEntry bar = addDirectory("/home/test/foo/bar");

            CopyOption[] options = {};
            fileSystem.copy(createPath("/home/test"), createPath(""), options);
            fileSystem.copy(createPath("/home/test/foo"), createPath("foo"), options);
            fileSystem.copy(createPath("/home/test/foo/bar"), createPath("foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/home/test/foo"));
            assertSame(bar, getFileSystemEntry("/home/test/foo/bar"));
            assertEquals(0, getChildCount("/home/test/foo/bar"));
        }

        @Test
        void testCopyNonExisting() {
            DirectoryEntry foo = addDirectory("/foo");

            CopyOption[] options = {};
            NoSuchFileException exception = assertThrows(NoSuchFileException.class,
                    () -> fileSystem.copy(createPath("/foo/bar"), createPath("/foo/baz"), options));
            assertEquals("/foo/bar", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertEquals(0, getChildCount("/foo"));
        }

        @Test
        void testCopyFTPFailure() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");

            // failure: target parent does not exist

            CopyOption[] options = {};
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.copy(createPath("/foo/bar"), createPath("/baz/bar"), options));
            assertEquals("/baz/bar", exception.getFile());

            verify(getExceptionFactory()).createNewOutputStreamException(eq("/baz/bar"), eq(553), anyString(), anyCollection());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertNull(getFileSystemEntry("/baz"));
            assertNull(getFileSystemEntry("/baz/bar"));
        }

        @Test
        void testCopyRoot() throws IOException {
            // copying a directory (including the root) will not copy its contents, so copying the root is allowed
            DirectoryEntry foo = addDirectory("/foo");

            CopyOption[] options = {};
            fileSystem.copy(createPath("/"), createPath("/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));

            DirectoryEntry bar = getDirectory("/foo/bar");

            assertNotSame(getDirectory("/"), bar);
            assertEquals(0, getChildCount("/foo/bar"));
        }

        @Test
        void testCopyReplaceFile() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.copy(createPath("/baz"), createPath("/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testCopyReplaceFileAllowed() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            fileSystem.copy(createPath("/baz"), createPath("/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));
            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
            // permissions are dropped during the delete/recreate
            assertEqualsMinusPath(bar, getFileSystemEntry("/foo/bar"), false);
            assertNotSame(baz, getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testCopyReplaceNonEmptyDir() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.copy(createPath("/baz"), createPath("/foo"), options));
            assertEquals("/foo", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testCopyReplaceNonEmptyDirAllowed() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.copy(createPath("/baz"), createPath("/foo"), options));
            assertEquals("/foo", exception.getFile());

            verify(getExceptionFactory()).createDeleteException(eq("/foo"), eq(550), anyString(), eq(true));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testCopyReplaceEmptyDir() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.copy(createPath("/baz"), createPath("/foo"), options));
            assertEquals("/foo", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testCopyReplaceEmptyDirAllowed() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            fileSystem.copy(createPath("/baz"), createPath("/foo"), options);

            assertThat(getFileSystemEntry("/foo"), instanceOf(DirectoryEntry.class));
            assertNotSame(foo, getFileSystemEntry("/foo"));
            assertNotSame(baz, getFileSystemEntry("/foo"));
        }

        @Test
        void testCopyFile() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            baz.setOwner("root");

            CopyOption[] options = {};
            fileSystem.copy(createPath("/baz"), createPath("/foo/bar"), options);

            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNotSame(baz, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
            assertNotEquals(baz.getOwner(), getFileSystemEntry("/foo/bar").getOwner());
        }

        @Test
        void testCopyFileMultipleConnections() throws IOException {
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
        void testCopyEmptyDir() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");

            baz.setOwner("root");

            CopyOption[] options = {};
            fileSystem.copy(createPath("/baz"), createPath("/foo/bar"), options);

            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNotSame(baz, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));

            DirectoryEntry bar = getDirectory("/foo/bar");
            assertEquals(0, getChildCount("/foo/bar"));
            assertNotEquals(baz.getOwner(), bar.getOwner());
        }

        @Test
        void testCopyNonEmptyDir() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");
            addFile("/baz/qux");

            baz.setOwner("root");

            CopyOption[] options = {};
            fileSystem.copy(createPath("/baz"), createPath("/foo/bar"), options);

            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNotSame(baz, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));

            DirectoryEntry bar = getDirectory("/foo/bar");
            assertEquals(0, getChildCount("/foo/bar"));
            assertNotEquals(baz.getOwner(), bar.getOwner());
        }

        @Test
        void testCopyReplaceFileDifferentFileSystems() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testCopyReplaceFileAllowedDifferentFileSystems() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            fileSystem.copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));
            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
            // permissions are dropped during the copy/delete
            assertEqualsMinusPath(bar, getFileSystemEntry("/foo/bar"), false);
            assertNotSame(baz, getFileSystemEntry("/foo/bar"));
        }

        @Test
        void testCopyReplaceNonEmptyDirDifferentFileSystems() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo"), options));
            assertEquals("/foo", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testCopyReplaceNonEmptyDirAllowedDifferentFileSystems() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo"), options));
            assertEquals("/foo", exception.getFile());

            verify(getExceptionFactory()).createDeleteException(eq("/foo"), eq(550), anyString(), eq(true));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testCopyReplaceEmptyDirDifferentFileSystems() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo"), options));
            assertEquals("/foo", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testCopyReplaceEmptyDirAllowedDifferentFileSystems() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            fileSystem.copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo"), options);

            assertThat(getFileSystemEntry("/foo"), instanceOf(DirectoryEntry.class));
            assertNotSame(foo, getFileSystemEntry("/foo"));
            assertNotSame(baz, getFileSystemEntry("/foo"));
        }

        @Test
        void testCopyFileDifferentFileSystems() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            baz.setOwner("root");

            CopyOption[] options = {};
            fileSystem.copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNotSame(baz, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
            assertNotEquals(baz.getOwner(), getFileSystemEntry("/foo/bar").getOwner());
        }

        @Test
        void testCopyEmptyDirDifferentFileSystems() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");

            baz.setOwner("root");

            CopyOption[] options = {};
            fileSystem.copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNotSame(baz, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));

            DirectoryEntry bar = getDirectory("/foo/bar");
            assertEquals(0, getChildCount("/foo/bar"));
            assertNotEquals(baz.getOwner(), bar.getOwner());
        }

        @Test
        void testCopyNonEmptyDirDifferentFileSystems() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");
            addFile("/baz/qux");

            baz.setOwner("root");

            CopyOption[] options = {};
            fileSystem.copy(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNotSame(baz, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));

            DirectoryEntry bar = getDirectory("/foo/bar");
            assertEquals(0, getChildCount("/foo/bar"));
            assertNotEquals(baz.getOwner(), bar.getOwner());
        }

        @Test
        void testCopyWithAttributes() {
            addDirectory("/foo");
            addDirectory("/baz");
            addFile("/baz/qux");

            FTPPath source = createPath("/baz");
            FTPPath target = createPath("/foo/bar");
            CopyOption[] options = { StandardCopyOption.COPY_ATTRIBUTES };

            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                    () -> fileSystem.copy(source, target, options));
            assertEquals(Messages.fileSystemProvider().unsupportedCopyOption(StandardCopyOption.COPY_ATTRIBUTES).getMessage(),
                    exception.getMessage());
        }

        // FTPFileSystem.move

        @Test
        void testMoveSame() throws IOException {
            DirectoryEntry foo = addDirectory("/home/test/foo");
            DirectoryEntry bar = addDirectory("/home/test/foo/bar");
            SymbolicLinkEntry baz = addSymLink("/baz", foo);

            CopyOption[] options = {};
            fileSystem.move(createPath("/"), createPath("/"), options);
            fileSystem.move(createPath("/home/test"), createPath(""), options);
            fileSystem.move(createPath("/home/test/foo"), createPath("foo"), options);
            fileSystem.move(createPath("/home/test/foo/bar"), createPath("foo/bar"), options);
            fileSystem.move(createPath("/home/test/foo"), createPath("/baz"), options);
            fileSystem.move(createPath("/baz"), createPath("/home/test/foo"), options);

            assertSame(foo, getFileSystemEntry("/home/test/foo"));
            assertSame(bar, getFileSystemEntry("/home/test/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
            assertEquals(0, getChildCount("/home/test/foo/bar"));
        }

        @Test
        void testMoveNonExisting() {
            DirectoryEntry foo = addDirectory("/foo");

            CopyOption[] options = {};
            NoSuchFileException exception = assertThrows(NoSuchFileException.class,
                    () -> fileSystem.move(createPath("/foo/bar"), createPath("/foo/baz"), options));
            assertEquals("/foo/bar", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertEquals(0, getChildCount("/foo"));
        }

        @Test
        void testMoveFTPFailure() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");

            // failure: non-existing target parent

            CopyOption[] options = {};
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.move(createPath("/foo/bar"), createPath("/baz/bar"), options));
            assertEquals("/foo/bar", exception.getFile());
            assertEquals("/baz/bar", exception.getOtherFile());

            verify(getExceptionFactory()).createMoveException(eq("/foo/bar"), eq("/baz/bar"), eq(553), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveEmptyRoot() {

            CopyOption[] options = {};
            DirectoryNotEmptyException exception = assertThrows(DirectoryNotEmptyException.class,
                    () -> fileSystem.move(createPath("/"), createPath("/baz"), options));
            assertEquals("/", exception.getFile());

            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveNonEmptyRoot() {
            DirectoryEntry foo = addDirectory("/foo");

            CopyOption[] options = {};
            DirectoryNotEmptyException exception = assertThrows(DirectoryNotEmptyException.class,
                    () -> fileSystem.move(createPath("/"), createPath("/baz"), options));
            assertEquals("/", exception.getFile());

            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveReplaceFile() {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry bar = addDirectory("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.move(createPath("/baz"), createPath("/foo/bar"), options));
            assertEquals("/baz", exception.getFile());
            assertEquals("/foo/bar", exception.getOtherFile());

            verify(getExceptionFactory()).createMoveException(eq("/baz"), eq("/foo/bar"), eq(553), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveReplaceFileAllowed() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            fileSystem.move(createPath("/baz"), createPath("/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));
            assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveReplaceEmptyDir() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.move(createPath("/baz"), createPath("/foo"), options));
            assertEquals("/baz", exception.getFile());
            assertEquals("/foo", exception.getOtherFile());

            verify(getExceptionFactory()).createMoveException(eq("/baz"), eq("/foo"), eq(553), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveReplaceEmptyDirAllowed() throws IOException {
            addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            fileSystem.move(createPath("/baz"), createPath("/foo"), options);

            assertEqualsMinusPath(baz, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveFile() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            fileSystem.move(createPath("/baz"), createPath("/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));
            assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveEmptyDir() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");

            CopyOption[] options = {};
            fileSystem.move(createPath("/baz"), createPath("/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));
            assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveNonEmptyDir() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");
            FileEntry qux = addFile("/baz/qux");

            CopyOption[] options = {};
            fileSystem.move(createPath("/baz"), createPath("/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));
            assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
            assertEqualsMinusPath(qux, getFileSystemEntry("/foo/bar/qux"));
            assertEquals(1, getChildCount("/foo"));
            assertEquals(1, getChildCount("/foo/bar"));
        }

        @Test
        void testMoveNonEmptyDirSameParent() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry bar = addFile("/foo/bar");

            CopyOption[] options = {};
            try {
                fileSystem.move(createPath("/foo"), createPath("/baz"), options);
            } finally {
                assertNull(getFileSystemEntry("/foo"));
                assertEqualsMinusPath(foo, getFileSystemEntry("/baz"));
                assertEqualsMinusPath(bar, getFileSystemEntry("/baz/bar"));
            }
        }

        @Test
        void testMoveReplaceFileDifferentFileSystems() {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry bar = addDirectory("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.move(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options));
            assertEquals("/foo/bar", exception.getFile());

            verify(getExceptionFactory(), never()).createMoveException(anyString(), anyString(), anyInt(), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveReplaceFileAllowedDifferentFileSystems() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            addFile("/foo/bar");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            fileSystem.move(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));
            // permissions are dropped during the copy/delete
            assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"), false);
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveReplaceEmptyDirDifferentFileSystems() {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            FileAlreadyExistsException exception = assertThrows(FileAlreadyExistsException.class,
                    () -> fileSystem.move(createPath("/baz"), createPath(multiClientFileSystem, "/foo"), options));
            assertEquals("/foo", exception.getFile());

            verify(getExceptionFactory(), never()).createMoveException(anyString(), anyString(), anyInt(), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveReplaceEmptyDirAllowedDifferentFileSystems() throws IOException {
            addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
            fileSystem.move(createPath("/baz"), createPath(multiClientFileSystem, "/foo"), options);

            // permissions are dropped during the copy/delete
            assertEqualsMinusPath(baz, getFileSystemEntry("/foo"), false);
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveFileDifferentFileSystems() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            FileEntry baz = addFile("/baz");

            CopyOption[] options = {};
            fileSystem.move(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));
            // permissions are dropped during the copy/delete
            assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"), false);
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveEmptyDirDifferentFileSystems() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");

            CopyOption[] options = {};
            fileSystem.move(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options);

            assertSame(foo, getFileSystemEntry("/foo"));
            // permissions are dropped during the copy/delete
            assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"), false);
            assertNull(getFileSystemEntry("/baz"));
        }

        @Test
        void testMoveNonEmptyDirDifferentFileSystems() {
            DirectoryEntry foo = addDirectory("/foo");
            DirectoryEntry baz = addDirectory("/baz");
            addFile("/baz/qux");

            CopyOption[] options = {};
            FTPFileSystemException exception = assertThrows(FTPFileSystemException.class,
                    () -> fileSystem.move(createPath("/baz"), createPath(multiClientFileSystem, "/foo/bar"), options));
            assertEquals("/baz", exception.getFile());

            verify(getExceptionFactory()).createDeleteException(eq("/baz"), eq(550), anyString(), eq(true));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(baz, getFileSystemEntry("/baz"));
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

        // FTPFileSystem.isSameFile

        @Test
        void testIsSameFileEquals() throws IOException {

            assertTrue(fileSystem.isSameFile(createPath("/"), createPath("/")));
            assertTrue(fileSystem.isSameFile(createPath("/foo"), createPath("/foo")));
            assertTrue(fileSystem.isSameFile(createPath("/foo/bar"), createPath("/foo/bar")));

            assertTrue(fileSystem.isSameFile(createPath(""), createPath("")));
            assertTrue(fileSystem.isSameFile(createPath("foo"), createPath("foo")));
            assertTrue(fileSystem.isSameFile(createPath("foo/bar"), createPath("foo/bar")));

            assertTrue(fileSystem.isSameFile(createPath(""), createPath("/home/test")));
            assertTrue(fileSystem.isSameFile(createPath("/home/test"), createPath("")));
        }

        @Test
        void testIsSameFileExisting() throws IOException {
            FileEntry bar = addFile("/home/test/foo/bar");
            addSymLink("/bar", bar);

            assertTrue(fileSystem.isSameFile(createPath("/home/test"), createPath("")));
            assertTrue(fileSystem.isSameFile(createPath("/home/test/foo"), createPath("foo")));
            assertTrue(fileSystem.isSameFile(createPath("/home/test/foo/bar"), createPath("foo/bar")));

            assertTrue(fileSystem.isSameFile(createPath(""), createPath("/home/test")));
            assertTrue(fileSystem.isSameFile(createPath("foo"), createPath("/home/test/foo")));
            assertTrue(fileSystem.isSameFile(createPath("foo/bar"), createPath("/home/test/foo/bar")));

            assertFalse(fileSystem.isSameFile(createPath("foo"), createPath("foo/bar")));

            assertTrue(fileSystem.isSameFile(createPath("/bar"), createPath("/home/test/foo/bar")));
        }

        @Test
        void testIsSameFileFirstNonExisting() {

            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> fileSystem.isSameFile(createPath("/foo"), createPath("/")));
            assertEquals("/foo", exception.getFile());
        }

        @Test
        void testIsSameFileSecondNonExisting() {

            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> fileSystem.isSameFile(createPath("/"), createPath("/foo")));
            assertEquals("/foo", exception.getFile());
        }

        // FTPFileSystem.isHidden

        @Test
        void testIsHidden() throws IOException {
            addDirectory("/foo");
            addDirectory("/.foo");
            addFile("/foo/bar");
            addFile("/foo/.bar");

            assertFalse(fileSystem.isHidden(createPath("/foo")));
            assertTrue(fileSystem.isHidden(createPath("/.foo")));
            assertFalse(fileSystem.isHidden(createPath("/foo/bar")));
            assertTrue(fileSystem.isHidden(createPath("/foo/.bar")));
        }

        @Test
        void testIsHiddenNonExisting() {
            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> fileSystem.isHidden(createPath("/foo")));
            assertEquals("/foo", exception.getFile());
        }

        // FTPFileSystem.checkAccess

        @Test
        void testCheckAccessNonExisting() {
            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> fileSystem.checkAccess(createPath("/foo/bar")));
            assertEquals("/foo/bar", exception.getFile());
        }

        @Test
        void testCheckAccessNoModes() throws IOException {
            addDirectory("/foo/bar");

            fileSystem.checkAccess(createPath("/foo/bar"));
        }

        @Test
        void testCheckAccessOnlyRead() throws IOException {
            addDirectory("/foo/bar");

            fileSystem.checkAccess(createPath("/foo/bar"), AccessMode.READ);
        }

        @Test
        void testCheckAccessOnlyWriteNotReadOnly() throws IOException {
            addDirectory("/foo/bar");

            fileSystem.checkAccess(createPath("/foo/bar"), AccessMode.WRITE);
        }

        @Test
        void testCheckAccessOnlyWriteReadOnly() {
            DirectoryEntry bar = addDirectory("/foo/bar");
            bar.setPermissionsFromString("r-xr-xr-x");

            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> fileSystem.checkAccess(createPath("/foo/bar"), AccessMode.WRITE));
            assertEquals("/foo/bar", exception.getFile());
        }

        @Test
        void testCheckAccessOnlyExecute() {
            DirectoryEntry bar = addDirectory("/foo/bar");
            bar.setPermissionsFromString("rw-rw-rw-");

            AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                    () -> fileSystem.checkAccess(createPath("/foo/bar"), AccessMode.EXECUTE));
            assertEquals("/foo/bar", exception.getFile());
        }

        // FTPFileSystem.readAttributes (PosixFileAttributes variant)

        @Test
        void testReadAttributesFileFollowLinks() throws IOException {
            FileEntry foo = addFile("/foo");
            foo.setContents(new byte[1024]);
            foo.setPermissionsFromString("r-xr-xr-x");
            foo.setOwner("user");
            foo.setGroup("group");

            PosixFileAttributes attributes = fileSystem.readAttributes(createPath("/foo"));

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
        void testReadAttributesFileNoFollowLinks() throws IOException {
            FileEntry foo = addFile("/foo");
            foo.setContents(new byte[1024]);
            foo.setPermissionsFromString("r-xr-xr-x");
            foo.setOwner("user");
            foo.setGroup("group");

            PosixFileAttributes attributes = fileSystem.readAttributes(createPath("/foo"), LinkOption.NOFOLLOW_LINKS);

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
        void testReadAttributesDirectoryFollowLinks() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setPermissionsFromString("r-xr-xr-x");
            foo.setOwner("user");
            foo.setGroup("group");

            PosixFileAttributes attributes = fileSystem.readAttributes(createPath("/foo"));

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
        void testReadAttributesDirectoryNoFollowLinks() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setPermissionsFromString("r-xr-xr-x");
            foo.setOwner("user");
            foo.setGroup("group");

            PosixFileAttributes attributes = fileSystem.readAttributes(createPath("/foo"), LinkOption.NOFOLLOW_LINKS);

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
        void testReadAttributesSymLinkToFileFollowLinks() throws IOException {
            FileEntry foo = addFile("/foo");
            foo.setContents(new byte[1024]);
            foo.setPermissionsFromString("r-xr-xr-x");
            foo.setOwner("user");
            foo.setGroup("group");
            SymbolicLinkEntry bar = addSymLink("/bar", foo);

            PosixFileAttributes attributes = fileSystem.readAttributes(createPath("/bar"));

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
        void testReadAttributesSymLinkToFileNoFollowLinks() throws IOException {
            FileEntry foo = addFile("/foo");
            foo.setPermissionsFromString("r-xr-xr-x");
            foo.setOwner("user");
            foo.setGroup("group");
            SymbolicLinkEntry bar = addSymLink("/bar", foo);

            PosixFileAttributes attributes = fileSystem.readAttributes(createPath("/bar"), LinkOption.NOFOLLOW_LINKS);

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
        void testReadAttributesSymLinkToDirectoryFollowLinks() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setPermissionsFromString("r-xr-xr-x");
            foo.setOwner("user");
            foo.setGroup("group");
            SymbolicLinkEntry bar = addSymLink("/bar", foo);

            PosixFileAttributes attributes = fileSystem.readAttributes(createPath("/bar"));

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
        void testReadAttributesSymLinkToDirectoryNoFollowLinks() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setPermissionsFromString("r-xr-xr-x");
            foo.setOwner("user");
            foo.setGroup("group");
            SymbolicLinkEntry bar = addSymLink("/bar", foo);

            PosixFileAttributes attributes = fileSystem.readAttributes(createPath("/bar"), LinkOption.NOFOLLOW_LINKS);

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
        void testReadAttributesNonExisting() {
            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> fileSystem.readAttributes(createPath("/foo")));
            assertEquals("/foo", exception.getFile());
        }

        // FTPFileSystem.readAttributes (map variant)

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "lastModifiedTime, basic:lastModifiedTime",
                "lastAccessTime, basic:lastAccessTime",
                "creationTime, basic:creationTime",
                "basic:lastModifiedTime, basic:lastModifiedTime",
                "basic:lastAccessTime, basic:lastAccessTime",
                "basic:creationTime, basic:creationTime",
                "posix:lastModifiedTime, posix:lastModifiedTime",
                "posix:lastAccessTime, posix:lastAccessTime",
                "posix:creationTime, posix:creationTime"
        })
        void testReadAttributesMap(String attributeName, String expectedKey) throws IOException {
            addDirectory("/foo");
            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), attributeName);
            assertEquals(Collections.singleton(expectedKey), attributes.keySet());
            assertNotNull(attributes.get(expectedKey));
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "size, basic:size",
                "basic:size, basic:size",
                "posix:size, posix:size"
        })
        void testReadAttributesMapSize(String attributeName, String expectedKey) throws IOException {
            FileEntry foo = addFile("/foo");
            foo.setContents(new byte[1024]);
            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), attributeName);
            Map<String, ?> expected = Collections.singletonMap(expectedKey, foo.getSize());
            assertEquals(expected, attributes);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "isRegularFile, basic:isRegularFile",
                "basic:isRegularFile, basic:isRegularFile",
                "posix:isRegularFile, posix:isRegularFile"
        })
        void testReadAttributesMapIsRegularFile(String attributeName, String expectedKey) throws IOException {
            addDirectory("/foo");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), attributeName);
            Map<String, ?> expected = Collections.singletonMap(expectedKey, false);
            assertEquals(expected, attributes);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "isDirectory, basic:isDirectory",
                "basic:isDirectory, basic:isDirectory",
                "posix:isDirectory, posix:isDirectory"
        })
        void testReadAttributesMapIsDirectory(String attributeName, String expectedKey) throws IOException {
            addDirectory("/foo");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), attributeName);
            Map<String, ?> expected = Collections.singletonMap(expectedKey, true);
            assertEquals(expected, attributes);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "isSymbolicLink, basic:isSymbolicLink",
                "basic:isSymbolicLink, basic:isSymbolicLink",
                "posix:isSymbolicLink, posix:isSymbolicLink"
        })
        void testReadAttributesMapIsSymbolicLink(String attributeName, String expectedKey) throws IOException {
            addDirectory("/foo");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), attributeName);
            Map<String, ?> expected = Collections.singletonMap(expectedKey, false);
            assertEquals(expected, attributes);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "isOther, basic:isOther",
                "basic:isOther, basic:isOther",
                "posix:isOther, posix:isOther"
        })
        void testReadAttributesMapIsOther(String attributeName, String expectedKey) throws IOException {
            addDirectory("/foo");
            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), attributeName);
            Map<String, ?> expected = Collections.singletonMap(expectedKey, false);
            assertEquals(expected, attributes);
        }

        @ParameterizedTest(name = "{0} -> {1}")
        @CsvSource({
                "fileKey, basic:fileKey",
                "basic:fileKey, basic:fileKey",
                "posix:fileKey, posix:fileKey"
        })
        void testReadAttributesMapFileKey(String attributeName, String expectedKey) throws IOException {
            addDirectory("/foo");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), attributeName);
            Map<String, ?> expected = Collections.singletonMap(expectedKey, null);
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapNoTypeMultiple() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "size,isDirectory");
            Map<String, Object> expected = new HashMap<>();
            expected.put("basic:size", foo.getSize());
            expected.put("basic:isDirectory", true);
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapNoTypeAll() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "*");
            Map<String, Object> expected = new HashMap<>();
            expected.put("basic:size", foo.getSize());
            expected.put("basic:isRegularFile", false);
            expected.put("basic:isDirectory", true);
            expected.put("basic:isSymbolicLink", false);
            expected.put("basic:isOther", false);
            expected.put("basic:fileKey", null);

            assertNotNull(attributes.remove("basic:lastModifiedTime"));
            assertNotNull(attributes.remove("basic:lastAccessTime"));
            assertNotNull(attributes.remove("basic:creationTime"));
            assertEquals(expected, attributes);

            attributes = fileSystem.readAttributes(createPath("/foo"), "basic:lastModifiedTime,*");
            assertNotNull(attributes.remove("basic:lastModifiedTime"));
            assertNotNull(attributes.remove("basic:lastAccessTime"));
            assertNotNull(attributes.remove("basic:creationTime"));
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapBasicMultiple() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "basic:size,isDirectory");
            Map<String, Object> expected = new HashMap<>();
            expected.put("basic:size", foo.getSize());
            expected.put("basic:isDirectory", true);
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapBasicAll() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "basic:*");
            Map<String, Object> expected = new HashMap<>();
            expected.put("basic:size", foo.getSize());
            expected.put("basic:isRegularFile", false);
            expected.put("basic:isDirectory", true);
            expected.put("basic:isSymbolicLink", false);
            expected.put("basic:isOther", false);
            expected.put("basic:fileKey", null);

            assertNotNull(attributes.remove("basic:lastModifiedTime"));
            assertNotNull(attributes.remove("basic:lastAccessTime"));
            assertNotNull(attributes.remove("basic:creationTime"));
            assertEquals(expected, attributes);

            attributes = fileSystem.readAttributes(createPath("/foo"), "basic:lastModifiedTime,*");
            assertNotNull(attributes.remove("basic:lastModifiedTime"));
            assertNotNull(attributes.remove("basic:lastAccessTime"));
            assertNotNull(attributes.remove("basic:creationTime"));
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapOwnerOwner() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setOwner("test");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "owner:owner");
            Map<String, ?> expected = Collections.singletonMap("owner:owner", new SimpleUserPrincipal(foo.getOwner()));
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapOwnerAll() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setOwner("test");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "owner:*");
            Map<String, Object> expected = new HashMap<>();
            expected.put("owner:owner", new SimpleUserPrincipal(foo.getOwner()));
            assertEquals(expected, attributes);

            attributes = fileSystem.readAttributes(createPath("/foo"), "owner:owner,*");
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapPosixOwner() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setOwner("test");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "posix:owner");
            Map<String, ?> expected = Collections.singletonMap("posix:owner", new SimpleUserPrincipal(foo.getOwner()));
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapPosixGroup() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setGroup("test");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "posix:group");
            Map<String, ?> expected = Collections.singletonMap("posix:group", new SimpleGroupPrincipal(foo.getGroup()));
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapPosixPermissions() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setPermissionsFromString("r-xr-xr-x");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "posix:permissions");
            Map<String, ?> expected = Collections.singletonMap("posix:permissions",
                    PosixFilePermissions.fromString(foo.getPermissions().asRwxString()));
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapPosixMultiple() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setOwner("test");
            foo.setGroup("test");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "posix:size,owner,group");
            Map<String, Object> expected = new HashMap<>();
            expected.put("posix:size", foo.getSize());
            expected.put("posix:owner", new SimpleUserPrincipal(foo.getOwner()));
            expected.put("posix:group", new SimpleGroupPrincipal(foo.getGroup()));
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapPosixAll() throws IOException {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setOwner("test");
            foo.setGroup("group");
            foo.setPermissionsFromString("r-xr-xr-x");

            Map<String, Object> attributes = fileSystem.readAttributes(createPath("/foo"), "posix:*");
            Map<String, Object> expected = new HashMap<>();
            expected.put("posix:size", foo.getSize());
            expected.put("posix:isRegularFile", false);
            expected.put("posix:isDirectory", true);
            expected.put("posix:isSymbolicLink", false);
            expected.put("posix:isOther", false);
            expected.put("posix:fileKey", null);
            expected.put("posix:owner", new SimpleUserPrincipal(foo.getOwner()));
            expected.put("posix:group", new SimpleGroupPrincipal(foo.getGroup()));
            expected.put("posix:permissions", PosixFilePermissions.fromString(foo.getPermissions().asRwxString()));

            assertNotNull(attributes.remove("posix:lastModifiedTime"));
            assertNotNull(attributes.remove("posix:lastAccessTime"));
            assertNotNull(attributes.remove("posix:creationTime"));
            assertEquals(expected, attributes);

            attributes = fileSystem.readAttributes(createPath("/foo"), "posix:lastModifiedTime,*");
            assertNotNull(attributes.remove("posix:lastModifiedTime"));
            assertNotNull(attributes.remove("posix:lastAccessTime"));
            assertNotNull(attributes.remove("posix:creationTime"));
            assertEquals(expected, attributes);
        }

        @Test
        void testReadAttributesMapUnsupportedAttribute() {
            DirectoryEntry foo = addDirectory("/foo");
            foo.setOwner("test");

            FTPPath path = createPath("/foo");

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> fileSystem.readAttributes(path, "posix:lastModifiedTime,owner,dummy"));
            assertEquals(Messages.fileSystemProvider().unsupportedFileAttribute("dummy").getMessage(), exception.getMessage());
        }

        @Test
        void testReadAttributesMapUnsupportedType() {
            addDirectory("/foo");

            FTPPath path = createPath("/foo");

            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                    () -> fileSystem.readAttributes(path, "zipfs:*"));
            assertEquals(Messages.fileSystemProvider().unsupportedFileAttributeView("zipfs").getMessage(), exception.getMessage());
        }

        // FTPFileSystem.getFTPFile

        @Test
        void testGetFTPFileFile() throws IOException {
            addFile("/foo");

            FTPFile file = fileSystem.getFTPFile(createPath("/foo"));
            assertNotNull(file);
            assertEquals("foo", file.getName());
            assertTrue(file.isFile());
        }

        @Test
        void testGetFTPFileFileNotExisting() {

            NoSuchFileException exception = assertThrows(NoSuchFileException.class, () -> fileSystem.getFTPFile(createPath("/foo")));
            assertEquals("/foo", exception.getFile());

            VerificationMode verificationMode = useUnixFtpServer() && usesUnixFTPFileStrategyFactory() ? times(1) : never();
            verify(getExceptionFactory(), verificationMode).createGetFileException(eq("/foo"), eq(226), anyString());
        }

        @Test
        void testGetFTPFileFileAccessDenied() throws IOException {
            addFile("/foo/bar");
            getFile("/foo/bar").setPermissionsFromString("---------");

            if (useUnixFtpServer() && usesUnixFTPFileStrategyFactory()) {
                assertThrows(NoSuchFileException.class, this::testGetFTPFileFileAccessDenied0);
            } else {
                testGetFTPFileFileAccessDenied0();
            }
        }

        private void testGetFTPFileFileAccessDenied0() throws IOException {
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
        void testGetFTPFileDirectory() throws IOException {
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
        void testGetFTPFileDirectoryAccessDenied() throws IOException {
            DirectoryEntry bar = addDirectory("/foo/bar");
            bar.setPermissionsFromString("---------");

            if (useUnixFtpServer() && usesUnixFTPFileStrategyFactory()) {
                assertThrows(NoSuchFileException.class, this::testGetFTPFileDirectoryAccessDenied0);
            } else {
                testGetFTPFileDirectoryAccessDenied0();
            }
        }

        private void testGetFTPFileDirectoryAccessDenied0() throws IOException {
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
