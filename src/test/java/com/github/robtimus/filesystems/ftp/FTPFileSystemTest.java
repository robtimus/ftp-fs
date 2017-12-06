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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
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
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.net.ftp.FTPFile;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockito.verification.VerificationMode;
import com.github.robtimus.filesystems.attribute.SimpleGroupPrincipal;
import com.github.robtimus.filesystems.attribute.SimpleUserPrincipal;
import com.github.robtimus.filesystems.ftp.server.SymbolicLinkEntry;

@RunWith(Parameterized.class)
@SuppressWarnings({ "nls", "javadoc" })
public class FTPFileSystemTest extends AbstractFTPFileSystemTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    public FTPFileSystemTest(boolean useUnixFtpServer, boolean supportAbsoluteFilePaths) {
        super(useUnixFtpServer, supportAbsoluteFilePaths);
    }

    @Parameters(name = "Use UNIX FTP server: {0}; support absolute file paths: {1}")
    public static List<Object[]> getParameters() {
        Object[][] parameters = {
            { true, true, },
            { true, false, },
            { false, true, },
            { false, false, },
        };
        return Arrays.asList(parameters);
    }

    // FTPFileSystem.getPath

    @Test
    public void testGetPath() {
        testGetPath("/", "/");
        testGetPath("/foo/bar", "/", "/foo", "/bar");
        testGetPath("/foo/../bar", "/foo/", "../bar");
    }

    private void testGetPath(String path, String first, String... more) {
        FTPPath expected = createPath(path);
        Path actual = getFileSystem().getPath(first, more);
        assertEquals(expected, actual);
    }

    // FTPFileSystem.keepAlive

    @Test
    public void testKeepAlive() throws IOException {
        getFileSystem().keepAlive();
    }

    // FTPFileSystem.toUri

    @Test
    public void testToUri() {
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
        URI actual = getFileSystem().toUri(createPath(path));
        assertEquals(expectedUri, actual);
    }

    // FTPFileSystem.toAbsolutePath

    @Test
    public void testToAbsolutePath() {

        testToAbsolutePath("/", "/");
        testToAbsolutePath("/foo/bar", "/foo/bar");
        testToAbsolutePath("/foo/../bar", "/foo/../bar");

        testToAbsolutePath("", "/home/test");
        testToAbsolutePath("foo/bar", "/home/test/foo/bar");
        testToAbsolutePath("foo/../bar", "/home/test/foo/../bar");
    }

    private void testToAbsolutePath(String path, String expected) {
        FTPPath expectedPath = createPath(expected);
        Path actual = getFileSystem().toAbsolutePath(createPath(path));
        assertEquals(expectedPath, actual);
    }

    // FTPFileSystem.toRealPath

    @Test
    public void testToRealPathNoFollowLinks() throws IOException {
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
        Path actual = getFileSystem().toRealPath(createPath(path), LinkOption.NOFOLLOW_LINKS);
        assertEquals(expectedPath, actual);
    }

    @Test
    public void testToRealPathFollowLinks() throws IOException {
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
        Path actual = getFileSystem().toRealPath(createPath(path));
        assertEquals(expectedPath, actual);
    }

    @Test(expected = NoSuchFileException.class)
    public void testToRealPathBrokenLink() throws IOException {
        addSymLink("/foo", new FileEntry("/bar"));

        createPath("/foo").toRealPath();
    }

    @Test(expected = NoSuchFileException.class)
    public void testToRealPathNotExisting() throws IOException {
        createPath("/foo").toRealPath();
    }

    // FTPFileSystem.newInputStream

    @Test
    public void testNewInputStream() throws IOException {
        addFile("/foo/bar");

        try (InputStream input = getFileSystem().newInputStream(createPath("/foo/bar"))) {
            // don't do anything with the stream, there's a separate test for that
        }
        // verify that the file system can be used after closing the stream
        getFileSystem().checkAccess(createPath("/foo/bar"));
    }

    @Test
    public void testNewInputStreamDeleteOnClose() throws IOException {
        addFile("/foo/bar");

        OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
        try (InputStream input = getFileSystem().newInputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
        }
        assertNull(getFileSystemEntry("/foo/bar"));
        assertEquals(0, getChildCount("/foo"));
    }

    @Test(expected = FTPFileSystemException.class)
    public void testNewInputStreamFTPFailure() throws IOException {

        // failure: file not found

        try (InputStream input = getFileSystem().newInputStream(createPath("/foo/bar"))) {
            // don't do anything with the stream, there's a separate test for that
        } finally {
            verify(getExceptionFactory()).createNewInputStreamException(eq("/foo/bar"), eq(550), anyString());
        }
    }

    // FTPFileSystem.newOutputStream

    @Test
    public void testNewOutputStreamExisting() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");

        OpenOption[] options = { StandardOpenOption.WRITE };
        try (OutputStream output = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
        }
        // verify that the file system can be used after closing the stream
        getFileSystem().checkAccess(createPath("/foo/bar"));

        assertSame(foo, getFileSystemEntry("/foo"));
        assertSame(bar, getFileSystemEntry("/foo/bar"));
    }

    @Test
    public void testNewOutputStreamExistingDeleteOnClose() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");

        OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
        try (OutputStream output = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }
        // verify that the file system can be used after closing the stream
        getFileSystem().checkAccess(createPath("/foo"));

        assertSame(foo, getFileSystemEntry("/foo"));
        assertNull(getFileSystemEntry("/foo/bar"));
        assertEquals(0, getChildCount("/foo"));
    }

    @Test
    public void testNewOutputStreamExistingCreate() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");

        OpenOption[] options = { StandardOpenOption.CREATE };
        try (OutputStream output = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
        }
        // verify that the file system can be used after closing the stream
        getFileSystem().checkAccess(createPath("/foo/bar"));

        assertSame(foo, getFileSystemEntry("/foo"));
        assertSame(bar, getFileSystemEntry("/foo/bar"));
    }

    @Test
    public void testNewOutputStreamExistingCreateDeleteOnClose() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");

        OpenOption[] options = { StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE };
        try (OutputStream output = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }
        // verify that the file system can be used after closing the stream
        getFileSystem().checkAccess(createPath("/foo"));

        assertSame(foo, getFileSystemEntry("/foo"));
        assertNull(getFileSystemEntry("/foo/bar"));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testNewOutputStreamExistingCreateNew() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");

        OpenOption[] options = { StandardOpenOption.CREATE_NEW };
        try (OutputStream output = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
        } finally {
            // verify that the file system can be used after closing the stream
            getFileSystem().checkAccess(createPath("/foo/bar"));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }
    }

    @Test(expected = FTPFileSystemException.class)
    public void testNewOutputStreamExistingFTPFailure() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        bar.setPermissionsFromString("r--r--r--");

        // failure: no permission to write

        OpenOption[] options = { StandardOpenOption.WRITE };
        try (OutputStream input = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
        } finally {
            verify(getExceptionFactory()).createNewOutputStreamException(eq("/foo/bar"), eq(553), anyString(), anyCollectionOf(OpenOption.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }
    }

    @Test(expected = FTPFileSystemException.class)
    public void testNewOutputStreamExistingFTPFailureDeleteOnClose() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        bar.setPermissionsFromString("r--r--r--");

        // failure: no permission to write

        OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
        try (OutputStream input = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
        } finally {
            verify(getExceptionFactory()).createNewOutputStreamException(eq("/foo/bar"), eq(553), anyString(), anyCollectionOf(OpenOption.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
        }
    }

    @Test(expected = NoSuchFileException.class)
    public void testNewOutputStreamNonExistingNoCreate() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        OpenOption[] options = { StandardOpenOption.WRITE };
        try (OutputStream input = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
        }
    }

    @Test
    public void testNewOutputStreamNonExistingCreate() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        OpenOption[] options = { StandardOpenOption.CREATE };
        try (OutputStream input = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
        }
    }

    @Test
    public void testNewOutputStreamNonExistingCreateDeleteOnClose() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        OpenOption[] options = { StandardOpenOption.CREATE, StandardOpenOption.DELETE_ON_CLOSE };
        try (OutputStream input = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
            // we can't check here that /foo/bar exists, because it will only be stored in the file system once the stream is closed
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
            assertEquals(0, getChildCount("/foo"));
        }
    }

    @Test
    public void testNewOutputStreamNonExistingCreateNew() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        OpenOption[] options = { StandardOpenOption.CREATE_NEW };
        try (OutputStream input = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
        }
    }

    @Test
    public void testNewOutputStreamNonExistingCreateNewDeleteOnClose() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        OpenOption[] options = { StandardOpenOption.CREATE_NEW, StandardOpenOption.DELETE_ON_CLOSE };
        try (OutputStream input = getFileSystem().newOutputStream(createPath("/foo/bar"), options)) {
            // don't do anything with the stream, there's a separate test for that
            // we can't check here that /foo/bar exists, because it will only be stored in the file system once the stream is closed
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
        }
    }

    @Test(expected = FileSystemException.class)
    public void testNewOutputStreamDirectoryNoCreate() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        OpenOption[] options = { StandardOpenOption.WRITE };
        try (OutputStream input = getFileSystem().newOutputStream(createPath("/foo"), options)) {
            // don't do anything with the stream, there's a separate test for that
        } finally {
            verify(getExceptionFactory(), never()).createNewOutputStreamException(anyString(), anyInt(), anyString(),
                    anyCollectionOf(OpenOption.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertEquals(0, getChildCount("/foo"));
        }
    }

    @Test(expected = FileSystemException.class)
    public void testNewOutputStreamDirectoryDeleteOnClose() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        OpenOption[] options = { StandardOpenOption.DELETE_ON_CLOSE };
        try (OutputStream input = getFileSystem().newOutputStream(createPath("/foo"), options)) {
            // don't do anything with the stream, there's a separate test for that
        } finally {
            verify(getExceptionFactory(), never()).createNewOutputStreamException(anyString(), anyInt(), anyString(),
                    anyCollectionOf(OpenOption.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertEquals(0, getChildCount("/foo"));
        }
    }

    // FTPFileSystem.newByteChannel

    @Test
    public void testNewByteChannelRead() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        bar.setContents(new byte[1024]);

        Set<? extends OpenOption> options = EnumSet.noneOf(StandardOpenOption.class);
        try (SeekableByteChannel channel = getFileSystem().newByteChannel(createPath("/foo/bar"), options)) {
            // don't do anything with the channel, there's a separate test for that
            assertEquals(bar.getSize(), channel.size());
        }
        assertSame(foo, getFileSystemEntry("/foo"));
        assertSame(bar, getFileSystemEntry("/foo/bar"));
    }

    @Test(expected = FTPFileSystemException.class)
    public void testNewByteChannelReadNonExisting() throws IOException {

        // failure: file does not exist

        Set<? extends OpenOption> options = EnumSet.noneOf(StandardOpenOption.class);
        try (SeekableByteChannel channel = getFileSystem().newByteChannel(createPath("/foo/bar"), options)) {
            // don't do anything with the channel, there's a separate test for that
        } finally {
            verify(getExceptionFactory()).createNewInputStreamException(eq("/foo/bar"), eq(550), anyString());
            assertNull(getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/foo/bar"));
        }
    }

    @Test
    public void testNewByteChannelWrite() throws IOException {
        addFile("/foo/bar");

        Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.WRITE);
        try (SeekableByteChannel channel = getFileSystem().newByteChannel(createPath("/foo/bar"), options)) {
            // don't do anything with the channel, there's a separate test for that
            assertEquals(0, channel.size());
        }
    }

    @Test
    public void testNewByteChannelWriteAppend() throws IOException {
        FileEntry bar = addFile("/foo/bar");
        bar.setContents(new byte[1024]);

        Set<? extends OpenOption> options = EnumSet.of(StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        try (SeekableByteChannel channel = getFileSystem().newByteChannel(createPath("/foo/bar"), options)) {
            // don't do anything with the channel, there's a separate test for that
            assertEquals(bar.getSize(), channel.size());
        }
    }

    // FTPFileSystem.newDirectoryStream

    @Test
    public void testNewDirectoryStream() throws IOException {

        try (DirectoryStream<Path> stream = getFileSystem().newDirectoryStream(createPath("/"), AcceptAllFilter.INSTANCE)) {
            assertNotNull(stream);
            // don't do anything with the stream, there's a separate test for that
        }
    }

    @Test(expected = NoSuchFileException.class)
    public void testNewDirectoryStreamNotExisting() throws IOException {

        getFileSystem().newDirectoryStream(createPath("/foo"), AcceptAllFilter.INSTANCE);
    }

    @Test(expected = NotDirectoryException.class)
    public void testGetDirectoryStreamNotDirectory() throws IOException {
        addFile("/foo");

        getFileSystem().newDirectoryStream(createPath("/foo"), AcceptAllFilter.INSTANCE);
    }

    private static final class AcceptAllFilter implements Filter<Path> {

        private static final AcceptAllFilter INSTANCE = new AcceptAllFilter();

        @Override
        public boolean accept(Path entry) {
            return true;
        }
    }

    // FTPFileSystem.createNewDirectory

    @Test
    public void testCreateDirectory() throws IOException {
        assertNull(getFileSystemEntry("/foo"));

        getFileSystem().createDirectory(createPath("/foo"));

        FileSystemEntry entry = getFileSystemEntry("/foo");
        assertThat(entry, instanceOf(DirectoryEntry.class));
    }

    @Test(expected = FTPFileSystemException.class)
    public void testCreateDirectoryFTPFailure() throws IOException {
        DirectoryEntry root = getDirectory("/");
        root.setPermissionsFromString("r-xr-xr-x");

        // failure: read-only parent

        try {
            getFileSystem().createDirectory(createPath("/foo"));
        } finally {
            assertNull(getFileSystemEntry("/foo"));
            verify(getExceptionFactory()).createCreateDirectoryException(eq("/foo"), eq(550), anyString());
        }
    }

    // FTPFileSystem.delete

    @Test(expected = NoSuchFileException.class)
    public void testDeleteNonExisting() throws IOException {

        try {
            getFileSystem().delete(createPath("/foo"));
        } finally {
            verify(getExceptionFactory(), never()).createDeleteException(anyString(), anyInt(), anyString(), anyBoolean());
        }
    }

    @Test(expected = FTPFileSystemException.class)
    public void testDeleteRoot() throws IOException {

        try {
            getFileSystem().delete(createPath("/"));
        } finally {
            verify(getExceptionFactory()).createDeleteException(eq("/"), eq(550), anyString(), eq(true));
        }
    }

    @Test
    public void testDeleteFile() throws IOException {
        addFile("/foo/bar");
        FileSystemEntry foo = getFileSystemEntry("/foo");

        getFileSystem().delete(createPath("/foo/bar"));

        assertSame(foo, getFileSystemEntry("/foo"));
        assertNull(getFileSystemEntry("/foo/bar"));
    }

    @Test
    public void testDeleteEmptyDir() throws IOException {
        addDirectory("/foo/bar");
        FileSystemEntry foo = getFileSystemEntry("/foo");

        getFileSystem().delete(createPath("/foo/bar"));

        assertSame(foo, getFileSystemEntry("/foo"));
        assertNull(getFileSystemEntry("/foo/bar"));
    }

    @Test(expected = FTPFileSystemException.class)
    public void testDeleteFTPFailure() throws IOException {
        addDirectory("/foo/bar/baz");
        FileSystemEntry foo = getFileSystemEntry("/foo");
        FileSystemEntry bar = getFileSystemEntry("/foo/bar");

        // failure: non-empty directory

        try {
            getFileSystem().delete(createPath("/foo/bar"));
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            verify(getExceptionFactory()).createDeleteException(eq("/foo/bar"), eq(550), anyString(), eq(true));
        }
    }

    // FTPFileSystem.readSymbolicLink

    @Test
    public void testReadSymbolicLinkToFile() throws IOException {
        FileEntry foo = addFile("/foo");
        addSymLink("/bar", foo);

        FTPPath link = getFileSystem().readSymbolicLink(createPath("/bar"));
        assertEquals(createPath("/foo"), link);
    }

    @Test
    public void testReadSymbolicLinkToDirectory() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        addSymLink("/bar", foo);

        FTPPath link = getFileSystem().readSymbolicLink(createPath("/bar"));
        assertEquals(createPath("/foo"), link);
    }

    @Test
    public void testReadSymbolicLinkToNonExistingTarget() throws IOException {
        addSymLink("/bar", new FileEntry("/foo"));

        FTPPath link = getFileSystem().readSymbolicLink(createPath("/bar"));
        assertEquals(createPath("/foo"), link);
    }

    @Test(expected = NoSuchFileException.class)
    public void testReadSymbolicLinkNotExisting() throws IOException {

        getFileSystem().readSymbolicLink(createPath("/foo"));
    }

    @Test(expected = NotLinkException.class)
    public void testReadSymbolicLinkNoLinkButFile() throws IOException {
        addFile("/foo");

        getFileSystem().readSymbolicLink(createPath("/foo"));
    }

    @Test(expected = NotLinkException.class)
    public void testReadSymbolicLinkNoLinkButDirectory() throws IOException {
        addDirectory("/foo");

        getFileSystem().readSymbolicLink(createPath("/foo"));
    }

    // FTPFileSystem.copy

    @Test
    public void testCopySame() throws IOException {
        DirectoryEntry foo = addDirectory("/home/test/foo");
        DirectoryEntry bar = addDirectory("/home/test/foo/bar");

        CopyOption[] options = {};
        getFileSystem().copy(createPath("/home/test"), createPath(""), options);
        getFileSystem().copy(createPath("/home/test/foo"), createPath("foo"), options);
        getFileSystem().copy(createPath("/home/test/foo/bar"), createPath("foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/home/test/foo"));
        assertSame(bar, getFileSystemEntry("/home/test/foo/bar"));
        assertEquals(0, getChildCount("/home/test/foo/bar"));
    }

    @Test(expected = NoSuchFileException.class)
    public void testCopyNonExisting() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        CopyOption[] options = {};
        try {
            getFileSystem().copy(createPath("/foo/bar"), createPath("/foo/baz"), options);
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertEquals(0, getChildCount("/foo"));
        }
    }

    @Test(expected = FTPFileSystemException.class)
    public void testCopyFTPFailure() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");

        // failure: target parent does not exist

        CopyOption[] options = {};
        try {
            getFileSystem().copy(createPath("/foo/bar"), createPath("/baz/bar"), options);
        } finally {
            verify(getExceptionFactory()).createNewOutputStreamException(eq("/baz/bar"), eq(553), anyString(), anyCollectionOf(OpenOption.class));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertNull(getFileSystemEntry("/baz"));
            assertNull(getFileSystemEntry("/baz/bar"));
        }
    }

    @Test
    public void testCopyRoot() throws IOException {
        // copying a directory (including the root) will not copy its contents, so copying the root is allowed
        DirectoryEntry foo = addDirectory("/foo");

        CopyOption[] options = {};
        getFileSystem().copy(createPath("/"), createPath("/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));

        DirectoryEntry bar = getDirectory("/foo/bar");

        assertNotSame(getDirectory("/"), bar);
        assertEquals(0, getChildCount("/foo/bar"));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testCopyReplaceFile() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().copy(createPath("/baz"), createPath("/foo/bar"), options);
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test
    public void testCopyReplaceFileAllowed() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        getFileSystem().copy(createPath("/baz"), createPath("/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));
        assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
        // permissions are dropped during the delete/recreate
        assertEqualsMinusPath(bar, getFileSystemEntry("/foo/bar"), false);
        assertNotSame(baz, getFileSystemEntry("/foo/bar"));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testCopyReplaceNonEmptyDir() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().copy(createPath("/baz"), createPath("/foo"), options);
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test(expected = FTPFileSystemException.class)
    public void testCopyReplaceNonEmptyDirAllowed() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        try {
            getFileSystem().copy(createPath("/baz"), createPath("/foo"), options);
        } finally {
            verify(getExceptionFactory()).createDeleteException(eq("/foo"), eq(550), anyString(), eq(true));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testCopyReplaceEmptyDir() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().copy(createPath("/baz"), createPath("/foo"), options);
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test
    public void testCopyReplaceEmptyDirAllowed() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        getFileSystem().copy(createPath("/baz"), createPath("/foo"), options);

        assertThat(getFileSystemEntry("/foo"), instanceOf(DirectoryEntry.class));
        assertNotSame(foo, getFileSystemEntry("/foo"));
        assertNotSame(baz, getFileSystemEntry("/foo"));
    }

    @Test
    public void testCopyFile() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        baz.setOwner("root");

        CopyOption[] options = {};
        getFileSystem().copy(createPath("/baz"), createPath("/foo/bar"), options);

        assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
        assertSame(foo, getFileSystemEntry("/foo"));
        assertNotSame(baz, getFileSystemEntry("/foo/bar"));
        assertSame(baz, getFileSystemEntry("/baz"));
        assertNotEquals(baz.getOwner(), getFileSystemEntry("/foo/bar").getOwner());
    }

    @Test
    public void testCopyFileMultipleConnections() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        baz.setOwner("root");

        CopyOption[] options = {};
        @SuppressWarnings("resource")
        FTPFileSystem fs = getMultiClientFileSystem();
        fs.copy(createPath(fs, "/baz"), createPath(fs, "/foo/bar"), options);

        assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
        assertSame(foo, getFileSystemEntry("/foo"));
        assertNotSame(baz, getFileSystemEntry("/foo/bar"));
        assertSame(baz, getFileSystemEntry("/baz"));
        assertNotEquals(baz.getOwner(), getFileSystemEntry("/foo/bar").getOwner());
    }

    @Test
    public void testCopyEmptyDir() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");

        baz.setOwner("root");

        CopyOption[] options = {};
        getFileSystem().copy(createPath("/baz"), createPath("/foo/bar"), options);

        assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
        assertSame(foo, getFileSystemEntry("/foo"));
        assertNotSame(baz, getFileSystemEntry("/foo/bar"));
        assertSame(baz, getFileSystemEntry("/baz"));

        DirectoryEntry bar = getDirectory("/foo/bar");
        assertEquals(0, getChildCount("/foo/bar"));
        assertNotEquals(baz.getOwner(), bar.getOwner());
    }

    @Test
    public void testCopyNonEmptyDir() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");
        addFile("/baz/qux");

        baz.setOwner("root");

        CopyOption[] options = {};
        getFileSystem().copy(createPath("/baz"), createPath("/foo/bar"), options);

        assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
        assertSame(foo, getFileSystemEntry("/foo"));
        assertNotSame(baz, getFileSystemEntry("/foo/bar"));
        assertSame(baz, getFileSystemEntry("/baz"));

        DirectoryEntry bar = getDirectory("/foo/bar");
        assertEquals(0, getChildCount("/foo/bar"));
        assertNotEquals(baz.getOwner(), bar.getOwner());
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testCopyReplaceFileDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().copy(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test
    public void testCopyReplaceFileAllowedDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        getFileSystem().copy(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));
        assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
        // permissions are dropped during the copy/delete
        assertEqualsMinusPath(bar, getFileSystemEntry("/foo/bar"), false);
        assertNotSame(baz, getFileSystemEntry("/foo/bar"));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testCopyReplaceNonEmptyDirDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().copy(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo"), options);
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test(expected = FTPFileSystemException.class)
    public void testCopyReplaceNonEmptyDirAllowedDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        try {
            getFileSystem().copy(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo"), options);
        } finally {
            verify(getExceptionFactory()).createDeleteException(eq("/foo"), eq(550), anyString(), eq(true));
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testCopyReplaceEmptyDirDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().copy(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo"), options);
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test
    public void testCopyReplaceEmptyDirAllowedDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        getFileSystem().copy(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo"), options);

        assertThat(getFileSystemEntry("/foo"), instanceOf(DirectoryEntry.class));
        assertNotSame(foo, getFileSystemEntry("/foo"));
        assertNotSame(baz, getFileSystemEntry("/foo"));
    }

    @Test
    public void testCopyFileDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        baz.setOwner("root");

        CopyOption[] options = {};
        getFileSystem().copy(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);

        assertThat(getFileSystemEntry("/foo/bar"), instanceOf(FileEntry.class));
        assertSame(foo, getFileSystemEntry("/foo"));
        assertNotSame(baz, getFileSystemEntry("/foo/bar"));
        assertSame(baz, getFileSystemEntry("/baz"));
        assertNotEquals(baz.getOwner(), getFileSystemEntry("/foo/bar").getOwner());
    }

    @Test
    public void testCopyEmptyDirDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");

        baz.setOwner("root");

        CopyOption[] options = {};
        getFileSystem().copy(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);

        assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
        assertSame(foo, getFileSystemEntry("/foo"));
        assertNotSame(baz, getFileSystemEntry("/foo/bar"));
        assertSame(baz, getFileSystemEntry("/baz"));

        DirectoryEntry bar = getDirectory("/foo/bar");
        assertEquals(0, getChildCount("/foo/bar"));
        assertNotEquals(baz.getOwner(), bar.getOwner());
    }

    @Test
    public void testCopyNonEmptyDirDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");
        addFile("/baz/qux");

        baz.setOwner("root");

        CopyOption[] options = {};
        getFileSystem().copy(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);

        assertThat(getFileSystemEntry("/foo/bar"), instanceOf(DirectoryEntry.class));
        assertSame(foo, getFileSystemEntry("/foo"));
        assertNotSame(baz, getFileSystemEntry("/foo/bar"));
        assertSame(baz, getFileSystemEntry("/baz"));

        DirectoryEntry bar = getDirectory("/foo/bar");
        assertEquals(0, getChildCount("/foo/bar"));
        assertNotEquals(baz.getOwner(), bar.getOwner());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testCopyWithAttributes() throws IOException {
        addDirectory("/foo");
        addDirectory("/baz");
        addFile("/baz/qux");

        CopyOption[] options = { StandardCopyOption.COPY_ATTRIBUTES };
        getFileSystem().copy(createPath("/baz"), createPath("/foo/bar"), options);
    }

    // FTPFileSystem.move

    @Test
    public void testMoveSame() throws IOException {
        DirectoryEntry foo = addDirectory("/home/test/foo");
        DirectoryEntry bar = addDirectory("/home/test/foo/bar");
        SymbolicLinkEntry baz = addSymLink("/baz", foo);

        CopyOption[] options = {};
        getFileSystem().move(createPath("/"), createPath("/"), options);
        getFileSystem().move(createPath("/home/test"), createPath(""), options);
        getFileSystem().move(createPath("/home/test/foo"), createPath("foo"), options);
        getFileSystem().move(createPath("/home/test/foo/bar"), createPath("foo/bar"), options);
        getFileSystem().move(createPath("/home/test/foo"), createPath("/baz"), options);
        getFileSystem().move(createPath("/baz"), createPath("/home/test/foo"), options);

        assertSame(foo, getFileSystemEntry("/home/test/foo"));
        assertSame(bar, getFileSystemEntry("/home/test/foo/bar"));
        assertSame(baz, getFileSystemEntry("/baz"));
        assertEquals(0, getChildCount("/home/test/foo/bar"));
    }

    @Test(expected = NoSuchFileException.class)
    public void testMoveNonExisting() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/foo/bar"), createPath("/foo/baz"), options);
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertEquals(0, getChildCount("/foo"));
        }
    }

    @Test(expected = FTPFileSystemException.class)
    public void testMoveFTPFailure() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");

        // failure: non-existing target parent

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/foo/bar"), createPath("/baz/bar"), options);
        } finally {
            verify(getExceptionFactory()).createMoveException(eq("/foo/bar"), eq("/baz/bar"), eq(553), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertNull(getFileSystemEntry("/baz"));
        }
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void testMoveEmptyRoot() throws IOException {

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/"), createPath("/baz"), options);
        } finally {
            assertNull(getFileSystemEntry("/baz"));
        }
    }

    @Test(expected = DirectoryNotEmptyException.class)
    public void testMoveNonEmptyRoot() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/"), createPath("/baz"), options);
        } finally {
            assertSame(foo, getFileSystemEntry("/foo"));
            assertNull(getFileSystemEntry("/baz"));
        }
    }

    @Test(expected = FTPFileSystemException.class)
    public void testMoveReplaceFile() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry bar = addDirectory("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/baz"), createPath("/foo/bar"), options);
        } finally {
            verify(getExceptionFactory()).createMoveException(eq("/baz"), eq("/foo/bar"), eq(553), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test
    public void testMoveReplaceFileAllowed() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        getFileSystem().move(createPath("/baz"), createPath("/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));
        assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
        assertNull(getFileSystemEntry("/baz"));
    }

    @Test(expected = FTPFileSystemException.class)
    public void testMoveReplaceEmptyDir() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/baz"), createPath("/foo"), options);
        } finally {
            verify(getExceptionFactory()).createMoveException(eq("/baz"), eq("/foo"), eq(553), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test
    public void testMoveReplaceEmptyDirAllowed() throws IOException {
        addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        getFileSystem().move(createPath("/baz"), createPath("/foo"), options);

        assertEqualsMinusPath(baz, getFileSystemEntry("/foo"));
        assertNull(getFileSystemEntry("/baz"));
    }

    @Test
    public void testMoveFile() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        getFileSystem().move(createPath("/baz"), createPath("/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));
        assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
        assertNull(getFileSystemEntry("/baz"));
    }

    @Test
    public void testMoveEmptyDir() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");

        CopyOption[] options = {};
        getFileSystem().move(createPath("/baz"), createPath("/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));
        assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
        assertNull(getFileSystemEntry("/baz"));
    }

    @Test
    public void testMoveNonEmptyDir() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");
        FileEntry qux = addFile("/baz/qux");

        CopyOption[] options = {};
        getFileSystem().move(createPath("/baz"), createPath("/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));
        assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"));
        assertEqualsMinusPath(qux, getFileSystemEntry("/foo/bar/qux"));
        assertEquals(1, getChildCount("/foo"));
        assertEquals(1, getChildCount("/foo/bar"));
    }

    @Test
    public void testMoveNonEmptyDirSameParent() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry bar = addFile("/foo/bar");

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/foo"), createPath("/baz"), options);
        } finally {
            assertNull(getFileSystemEntry("/foo"));
            assertEqualsMinusPath(foo, getFileSystemEntry("/baz"));
            assertEqualsMinusPath(bar, getFileSystemEntry("/baz/bar"));
        }
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testMoveReplaceFileDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry bar = addDirectory("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);
        } finally {
            verify(getExceptionFactory(), never()).createMoveException(anyString(), anyString(), anyInt(), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(bar, getFileSystemEntry("/foo/bar"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test
    public void testMoveReplaceFileAllowedDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        addFile("/foo/bar");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        getFileSystem().move(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));
        // permissions are dropped during the copy/delete
        assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"), false);
        assertNull(getFileSystemEntry("/baz"));
    }

    @Test(expected = FileAlreadyExistsException.class)
    public void testMoveReplaceEmptyDirDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo"), options);
        } finally {
            verify(getExceptionFactory(), never()).createMoveException(anyString(), anyString(), anyInt(), anyString());
            assertSame(foo, getFileSystemEntry("/foo"));
            assertSame(baz, getFileSystemEntry("/baz"));
        }
    }

    @Test
    public void testMoveReplaceEmptyDirAllowedDifferentFileSystems() throws IOException {
        addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = { StandardCopyOption.REPLACE_EXISTING };
        getFileSystem().move(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo"), options);

        // permissions are dropped during the copy/delete
        assertEqualsMinusPath(baz, getFileSystemEntry("/foo"), false);
        assertNull(getFileSystemEntry("/baz"));
    }

    @Test
    public void testMoveFileDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        FileEntry baz = addFile("/baz");

        CopyOption[] options = {};
        getFileSystem().move(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));
        // permissions are dropped during the copy/delete
        assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"), false);
        assertNull(getFileSystemEntry("/baz"));
    }

    @Test
    public void testMoveEmptyDirDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");

        CopyOption[] options = {};
        getFileSystem().move(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);

        assertSame(foo, getFileSystemEntry("/foo"));
        // permissions are dropped during the copy/delete
        assertEqualsMinusPath(baz, getFileSystemEntry("/foo/bar"), false);
        assertNull(getFileSystemEntry("/baz"));
    }

    @Test(expected = FTPFileSystemException.class)
    public void testMoveNonEmptyDirDifferentFileSystems() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        DirectoryEntry baz = addDirectory("/baz");
        addFile("/baz/qux");

        CopyOption[] options = {};
        try {
            getFileSystem().move(createPath("/baz"), createPath(getMultiClientFileSystem(), "/foo/bar"), options);
        } finally {
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

    // FTPFileSystem.isSameFile

    @Test
    public void testIsSameFileEquals() throws IOException {

        assertTrue(getFileSystem().isSameFile(createPath("/"), createPath("/")));
        assertTrue(getFileSystem().isSameFile(createPath("/foo"), createPath("/foo")));
        assertTrue(getFileSystem().isSameFile(createPath("/foo/bar"), createPath("/foo/bar")));

        assertTrue(getFileSystem().isSameFile(createPath(""), createPath("")));
        assertTrue(getFileSystem().isSameFile(createPath("foo"), createPath("foo")));
        assertTrue(getFileSystem().isSameFile(createPath("foo/bar"), createPath("foo/bar")));

        assertTrue(getFileSystem().isSameFile(createPath(""), createPath("/home/test")));
        assertTrue(getFileSystem().isSameFile(createPath("/home/test"), createPath("")));
    }

    @Test
    public void testIsSameFileExisting() throws IOException {
        FileEntry bar = addFile("/home/test/foo/bar");
        addSymLink("/bar", bar);

        assertTrue(getFileSystem().isSameFile(createPath("/home/test"), createPath("")));
        assertTrue(getFileSystem().isSameFile(createPath("/home/test/foo"), createPath("foo")));
        assertTrue(getFileSystem().isSameFile(createPath("/home/test/foo/bar"), createPath("foo/bar")));

        assertTrue(getFileSystem().isSameFile(createPath(""), createPath("/home/test")));
        assertTrue(getFileSystem().isSameFile(createPath("foo"), createPath("/home/test/foo")));
        assertTrue(getFileSystem().isSameFile(createPath("foo/bar"), createPath("/home/test/foo/bar")));

        assertFalse(getFileSystem().isSameFile(createPath("foo"), createPath("foo/bar")));

        assertTrue(getFileSystem().isSameFile(createPath("/bar"), createPath("/home/test/foo/bar")));
    }

    @Test(expected = NoSuchFileException.class)
    public void testIsSameFileFirstNonExisting() throws IOException {

        getFileSystem().isSameFile(createPath("/foo"), createPath("/"));
    }

    @Test(expected = NoSuchFileException.class)
    public void testIsSameFileSecondNonExisting() throws IOException {

        getFileSystem().isSameFile(createPath("/"), createPath("/foo"));
    }

    // FTPFileSystem.isHidden

    @Test
    public void testIsHidden() throws IOException {
        addDirectory("/foo");
        addDirectory("/.foo");
        addFile("/foo/bar");
        addFile("/foo/.bar");

        assertFalse(getFileSystem().isHidden(createPath("/foo")));
        assertTrue(getFileSystem().isHidden(createPath("/.foo")));
        assertFalse(getFileSystem().isHidden(createPath("/foo/bar")));
        assertTrue(getFileSystem().isHidden(createPath("/foo/.bar")));
    }

    @Test(expected = NoSuchFileException.class)
    public void testIsHiddenNonExisting() throws IOException {
        getFileSystem().isHidden(createPath("/foo"));
    }

    // FTPFileSystem.checkAccess

    @Test(expected = NoSuchFileException.class)
    public void testCheckAccessNonExisting() throws IOException {
        getFileSystem().checkAccess(createPath("/foo/bar"));
    }

    @Test
    public void testCheckAccessNoModes() throws IOException {
        addDirectory("/foo/bar");

        getFileSystem().checkAccess(createPath("/foo/bar"));
    }

    @Test
    public void testCheckAccessOnlyRead() throws IOException {
        addDirectory("/foo/bar");

        getFileSystem().checkAccess(createPath("/foo/bar"), AccessMode.READ);
    }

    @Test
    public void testCheckAccessOnlyWriteNotReadOnly() throws IOException {
        addDirectory("/foo/bar");

        getFileSystem().checkAccess(createPath("/foo/bar"), AccessMode.WRITE);
    }

    @Test(expected = AccessDeniedException.class)
    public void testCheckAccessOnlyWriteReadOnly() throws IOException {
        DirectoryEntry bar = addDirectory("/foo/bar");
        bar.setPermissionsFromString("r-xr-xr-x");

        getFileSystem().checkAccess(createPath("/foo/bar"), AccessMode.WRITE);
    }

    @Test(expected = AccessDeniedException.class)
    public void testCheckAccessOnlyExecute() throws IOException {
        DirectoryEntry bar = addDirectory("/foo/bar");
        bar.setPermissionsFromString("rw-rw-rw-");

        getFileSystem().checkAccess(createPath("/foo/bar"), AccessMode.EXECUTE);
    }

    // FTPFileSystem.readAttributes (PosixFileAttributes variant)

    @Test
    public void testReadAttributesFileFollowLinks() throws IOException {
        FileEntry foo = addFile("/foo");
        foo.setContents(new byte[1024]);
        foo.setPermissionsFromString("r-xr-xr-x");
        foo.setOwner("user");
        foo.setGroup("group");

        PosixFileAttributes attributes = getFileSystem().readAttributes(createPath("/foo"));

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
    public void testReadAttributesFileNoFollowLinks() throws IOException {
        FileEntry foo = addFile("/foo");
        foo.setContents(new byte[1024]);
        foo.setPermissionsFromString("r-xr-xr-x");
        foo.setOwner("user");
        foo.setGroup("group");

        PosixFileAttributes attributes = getFileSystem().readAttributes(createPath("/foo"), LinkOption.NOFOLLOW_LINKS);

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
    public void testReadAttributesDirectoryFollowLinks() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setPermissionsFromString("r-xr-xr-x");
        foo.setOwner("user");
        foo.setGroup("group");

        PosixFileAttributes attributes = getFileSystem().readAttributes(createPath("/foo"));

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
    public void testReadAttributesDirectoryNoFollowLinks() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setPermissionsFromString("r-xr-xr-x");
        foo.setOwner("user");
        foo.setGroup("group");

        PosixFileAttributes attributes = getFileSystem().readAttributes(createPath("/foo"), LinkOption.NOFOLLOW_LINKS);

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
    public void testReadAttributesSymLinkToFileFollowLinks() throws IOException {
        FileEntry foo = addFile("/foo");
        foo.setContents(new byte[1024]);
        foo.setPermissionsFromString("r-xr-xr-x");
        foo.setOwner("user");
        foo.setGroup("group");
        SymbolicLinkEntry bar = addSymLink("/bar", foo);

        PosixFileAttributes attributes = getFileSystem().readAttributes(createPath("/bar"));

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
    public void testReadAttributesSymLinkToFileNoFollowLinks() throws IOException {
        FileEntry foo = addFile("/foo");
        foo.setPermissionsFromString("r-xr-xr-x");
        foo.setOwner("user");
        foo.setGroup("group");
        SymbolicLinkEntry bar = addSymLink("/bar", foo);

        PosixFileAttributes attributes = getFileSystem().readAttributes(createPath("/bar"), LinkOption.NOFOLLOW_LINKS);

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
    public void testReadAttributesSymLinkToDirectoryFollowLinks() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setPermissionsFromString("r-xr-xr-x");
        foo.setOwner("user");
        foo.setGroup("group");
        SymbolicLinkEntry bar = addSymLink("/bar", foo);

        PosixFileAttributes attributes = getFileSystem().readAttributes(createPath("/bar"));

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
    public void testReadAttributesSymLinkToDirectoryNoFollowLinks() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setPermissionsFromString("r-xr-xr-x");
        foo.setOwner("user");
        foo.setGroup("group");
        SymbolicLinkEntry bar = addSymLink("/bar", foo);

        PosixFileAttributes attributes = getFileSystem().readAttributes(createPath("/bar"), LinkOption.NOFOLLOW_LINKS);

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

    @Test(expected = NoSuchFileException.class)
    public void testReadAttributesNonExisting() throws IOException {
        getFileSystem().readAttributes(createPath("/foo"));
    }

    // FTPFileSystem.readAttributes (map variant)

    @Test
    public void testReadAttributesMapNoTypeLastModifiedTime() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "lastModifiedTime");
        assertEquals(Collections.singleton("basic:lastModifiedTime"), attributes.keySet());
        assertNotNull(attributes.get("basic:lastModifiedTime"));
    }

    @Test
    public void testReadAttributesMapNoTypeLastAccessTime() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "lastAccessTime");
        assertEquals(Collections.singleton("basic:lastAccessTime"), attributes.keySet());
        assertNotNull(attributes.get("basic:lastAccessTime"));
    }

    @Test
    public void testReadAttributesMapNoTypeCreateTime() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "creationTime");
        assertEquals(Collections.singleton("basic:creationTime"), attributes.keySet());
        assertNotNull(attributes.get("basic:creationTime"));
    }

    @Test
    public void testReadAttributesMapNoTypeBasicSize() throws IOException {
        FileEntry foo = addFile("/foo");
        foo.setContents(new byte[1024]);

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "size");
        Map<String, ?> expected = Collections.singletonMap("basic:size", foo.getSize());
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapNoTypeIsRegularFile() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "isRegularFile");
        Map<String, ?> expected = Collections.singletonMap("basic:isRegularFile", false);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapNoTypeIsDirectory() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "isDirectory");
        Map<String, ?> expected = Collections.singletonMap("basic:isDirectory", true);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapNoTypeIsSymbolicLink() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "isSymbolicLink");
        Map<String, ?> expected = Collections.singletonMap("basic:isSymbolicLink", false);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapNoTypeIsOther() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "isOther");
        Map<String, ?> expected = Collections.singletonMap("basic:isOther", false);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapNoTypeFileKey() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "fileKey");
        Map<String, ?> expected = Collections.singletonMap("basic:fileKey", null);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapNoTypeMultiple() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "size,isDirectory");
        Map<String, Object> expected = new HashMap<>();
        expected.put("basic:size", foo.getSize());
        expected.put("basic:isDirectory", true);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapNoTypeAll() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "*");
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

        attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:lastModifiedTime,*");
        assertNotNull(attributes.remove("basic:lastModifiedTime"));
        assertNotNull(attributes.remove("basic:lastAccessTime"));
        assertNotNull(attributes.remove("basic:creationTime"));
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapBasicLastModifiedTime() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:lastModifiedTime");
        assertEquals(Collections.singleton("basic:lastModifiedTime"), attributes.keySet());
        assertNotNull(attributes.get("basic:lastModifiedTime"));
    }

    @Test
    public void testReadAttributesMapBasicLastAccessTime() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:lastAccessTime");
        assertEquals(Collections.singleton("basic:lastAccessTime"), attributes.keySet());
        assertNotNull(attributes.get("basic:lastAccessTime"));
    }

    @Test
    public void testReadAttributesMapBasicCreateTime() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:creationTime");
        assertEquals(Collections.singleton("basic:creationTime"), attributes.keySet());
        assertNotNull(attributes.get("basic:creationTime"));
    }

    @Test
    public void testReadAttributesMapBasicSize() throws IOException {
        FileEntry foo = addFile("/foo");
        foo.setContents(new byte[1024]);

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:size");
        Map<String, ?> expected = Collections.singletonMap("basic:size", foo.getSize());
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapBasicIsRegularFile() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:isRegularFile");
        Map<String, ?> expected = Collections.singletonMap("basic:isRegularFile", false);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapBasicIsDirectory() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:isDirectory");
        Map<String, ?> expected = Collections.singletonMap("basic:isDirectory", true);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapBasicIsSymbolicLink() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:isSymbolicLink");
        Map<String, ?> expected = Collections.singletonMap("basic:isSymbolicLink", false);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapBasicIsOther() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:isOther");
        Map<String, ?> expected = Collections.singletonMap("basic:isOther", false);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapBasicFileKey() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:fileKey");
        Map<String, ?> expected = Collections.singletonMap("basic:fileKey", null);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapBasicMultiple() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:size,isDirectory");
        Map<String, Object> expected = new HashMap<>();
        expected.put("basic:size", foo.getSize());
        expected.put("basic:isDirectory", true);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapBasicAll() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:*");
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

        attributes = getFileSystem().readAttributes(createPath("/foo"), "basic:lastModifiedTime,*");
        assertNotNull(attributes.remove("basic:lastModifiedTime"));
        assertNotNull(attributes.remove("basic:lastAccessTime"));
        assertNotNull(attributes.remove("basic:creationTime"));
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixLastModifiedTime() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:lastModifiedTime");
        assertEquals(Collections.singleton("posix:lastModifiedTime"), attributes.keySet());
        assertNotNull(attributes.get("posix:lastModifiedTime"));
    }

    @Test
    public void testReadAttributesMapPosixLastAccessTime() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:lastAccessTime");
        assertEquals(Collections.singleton("posix:lastAccessTime"), attributes.keySet());
        assertNotNull(attributes.get("posix:lastAccessTime"));
    }

    @Test
    public void testReadAttributesMapPosixCreateTime() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:creationTime");
        assertEquals(Collections.singleton("posix:creationTime"), attributes.keySet());
        assertNotNull(attributes.get("posix:creationTime"));
    }

    @Test
    public void testReadAttributesMapPosixSize() throws IOException {
        FileEntry foo = addFile("/foo");
        foo.setContents(new byte[1024]);

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:size");
        Map<String, ?> expected = Collections.singletonMap("posix:size", foo.getSize());
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixIsRegularFile() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:isRegularFile");
        Map<String, ?> expected = Collections.singletonMap("posix:isRegularFile", false);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixIsDirectory() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:isDirectory");
        Map<String, ?> expected = Collections.singletonMap("posix:isDirectory", true);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixIsSymbolicLink() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:isSymbolicLink");
        Map<String, ?> expected = Collections.singletonMap("posix:isSymbolicLink", false);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixIsOther() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:isOther");
        Map<String, ?> expected = Collections.singletonMap("posix:isOther", false);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixFileKey() throws IOException {
        addDirectory("/foo");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:fileKey");
        Map<String, ?> expected = Collections.singletonMap("posix:fileKey", null);
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapOwnerOwner() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setOwner("test");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "owner:owner");
        Map<String, ?> expected = Collections.singletonMap("owner:owner", new SimpleUserPrincipal(foo.getOwner()));
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapOwnerAll() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setOwner("test");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "owner:*");
        Map<String, Object> expected = new HashMap<>();
        expected.put("owner:owner", new SimpleUserPrincipal(foo.getOwner()));
        assertEquals(expected, attributes);

        attributes = getFileSystem().readAttributes(createPath("/foo"), "owner:owner,*");
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixOwner() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setOwner("test");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:owner");
        Map<String, ?> expected = Collections.singletonMap("posix:owner", new SimpleUserPrincipal(foo.getOwner()));
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixGroup() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setGroup("test");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:group");
        Map<String, ?> expected = Collections.singletonMap("posix:group", new SimpleGroupPrincipal(foo.getGroup()));
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixPermissions() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setPermissionsFromString("r-xr-xr-x");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:permissions");
        Map<String, ?> expected = Collections.singletonMap("posix:permissions", PosixFilePermissions.fromString(foo.getPermissions().asRwxString()));
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixMultiple() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setOwner("test");
        foo.setGroup("test");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:size,owner,group");
        Map<String, Object> expected = new HashMap<>();
        expected.put("posix:size", foo.getSize());
        expected.put("posix:owner", new SimpleUserPrincipal(foo.getOwner()));
        expected.put("posix:group", new SimpleGroupPrincipal(foo.getGroup()));
        assertEquals(expected, attributes);
    }

    @Test
    public void testReadAttributesMapPosixAll() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setOwner("test");
        foo.setGroup("group");
        foo.setPermissionsFromString("r-xr-xr-x");

        Map<String, Object> attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:*");
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

        attributes = getFileSystem().readAttributes(createPath("/foo"), "posix:lastModifiedTime,*");
        assertNotNull(attributes.remove("posix:lastModifiedTime"));
        assertNotNull(attributes.remove("posix:lastAccessTime"));
        assertNotNull(attributes.remove("posix:creationTime"));
        assertEquals(expected, attributes);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadAttributesMapUnsupportedAttribute() throws IOException {
        DirectoryEntry foo = addDirectory("/foo");
        foo.setOwner("test");

        getFileSystem().readAttributes(createPath("/foo"), "posix:lastModifiedTime,owner,dummy");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testReadAttributesMapUnsupportedType() throws IOException {
        addDirectory("/foo");

        getFileSystem().readAttributes(createPath("/foo"), "zipfs:*");
    }

    // FTPFileSystem.getFTPFile

    @Test
    public void testGetFTPFileFile() throws IOException {
        addFile("/foo");

        FTPFile file = getFileSystem().getFTPFile(createPath("/foo"));
        assertNotNull(file);
        assertEquals("foo", file.getName());
        assertTrue(file.isFile());
    }

    @Test(expected = NoSuchFileException.class)
    public void testGetFTPFileFileNotExisting() throws IOException {

        try {
            getFileSystem().getFTPFile(createPath("/foo"));

        } finally {
            VerificationMode verificationMode = useUnixFtpServer() && supportAbsoluteFilePaths() ? times(1) : never();
            verify(getExceptionFactory(), verificationMode).createGetFileException(eq("/foo"), eq(226), anyString());
        }
    }

    @Test
    public void testGetFTPFileFileAccessDenied() throws IOException {
        addFile("/foo/bar");
        getFile("/foo/bar").setPermissionsFromString("---------");

        if (useUnixFtpServer() && supportAbsoluteFilePaths()) {
            thrown.expect(NoSuchFileException.class);
        }

        try {
            FTPFile file = getFileSystem().getFTPFile(createPath("/foo/bar"));
            assertNotNull(file);
            assertEquals("bar", file.getName());
            assertTrue(file.isFile());
            for (int access = FTPFile.USER_ACCESS; access <= FTPFile.WORLD_ACCESS; access++) {
                for (int permission = FTPFile.READ_PERMISSION; permission <= FTPFile.EXECUTE_PERMISSION; permission++) {
                    assertFalse(file.hasPermission(access, permission));
                }
            }

        } finally {
            VerificationMode verificationMode = useUnixFtpServer() && supportAbsoluteFilePaths() ? times(1) : never();
            verify(getExceptionFactory(), verificationMode).createGetFileException(eq("/foo/bar"), eq(550), anyString());
        }
    }

    @Test
    public void testGetFTPFileDirectory() throws IOException {
        addDirectory("/foo");

        FTPFile file = getFileSystem().getFTPFile(createPath("/foo"));
        assertNotNull(file);
        if (useUnixFtpServer() && supportAbsoluteFilePaths()) {
            assertEquals(".", file.getName());
        } else {
            assertEquals("foo", file.getName());
        }
        assertTrue(file.isDirectory());
    }

    @Test
    public void testGetFTPFileDirectoryAccessDenied() throws IOException {
        DirectoryEntry bar = addDirectory("/foo/bar");
        bar.setPermissionsFromString("---------");

        if (useUnixFtpServer() && supportAbsoluteFilePaths()) {
            thrown.expect(NoSuchFileException.class);
        }

        try {
            FTPFile file = getFileSystem().getFTPFile(createPath("/foo/bar"));
            assertNotNull(file);
            assertEquals("bar", file.getName());
            assertTrue(file.isDirectory());
            for (int access = FTPFile.USER_ACCESS; access <= FTPFile.WORLD_ACCESS; access++) {
                for (int permission = FTPFile.READ_PERMISSION; permission <= FTPFile.EXECUTE_PERMISSION; permission++) {
                    assertFalse(file.hasPermission(access, permission));
                }
            }

        } finally {
            VerificationMode verificationMode = useUnixFtpServer() && supportAbsoluteFilePaths() ? times(1) : never();
            verify(getExceptionFactory(), verificationMode).createGetFileException(eq("/foo/bar"), eq(550), anyString());
        }
    }
}
