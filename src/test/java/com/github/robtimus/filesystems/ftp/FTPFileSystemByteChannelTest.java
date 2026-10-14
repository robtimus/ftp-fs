/*
 * FTPFileSystemByteChannelTest.java
 * Copyright 2026 Rob Spoor
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

import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.NON_UNIX;
import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.UNIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.EnumSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockftpserver.core.command.CommandHandler;
import org.mockftpserver.core.command.StaticReplyCommandHandler;
import org.mockftpserver.fake.filesystem.FileEntry;
import com.github.robtimus.filesystems.ftp.server.RETRCommandHandler;
import com.github.robtimus.filesystems.ftp.server.SymbolicLinkEntry;

@SuppressWarnings("nls")
class FTPFileSystemByteChannelTest {

    @Nested
    @DisplayName("Use UNIX FTP server: true; FTPFile strategy factory: UNIX")
    class UnixServerUsingUnixStrategy extends ByteChannelTest {

        UnixServerUsingUnixStrategy() {
            super(true, UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: true; FTPFile strategy factory: NON_UNIX")
    class UnixServerUsingNonUnixStrategy extends ByteChannelTest {

        UnixServerUsingNonUnixStrategy() {
            super(true, NON_UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: false; FTPFile strategy factory: UNIX")
    class NonUnixServerUsingUnixStrategy extends ByteChannelTest {

        NonUnixServerUsingUnixStrategy() {
            super(false, UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: false; FTPFile strategy factory: NON_UNIX")
    class NonUnixServerNotUsingAbsoluteFilePaths extends ByteChannelTest {

        NonUnixServerNotUsingAbsoluteFilePaths() {
            super(false, NON_UNIX);
        }
    }

    abstract static class ByteChannelTest extends AbstractFTPFileSystemTest {

        private ByteChannelTest(boolean useUnixFtpServer, StandardFTPFileStrategyFactory ftpFileStrategyFactory) {
            super(useUnixFtpServer, ftpFileStrategyFactory);
        }

        @Test
        void testReadFromPosition() throws IOException {
            addFile("/foo").setContents("Hello World");

            try (SeekableByteChannel channel = newByteChannel()) {
                channel.position(6);
                assertEquals("World", read(channel, 1024));
            }
        }

        @Test
        void testSeekBackwardsAndForwards() throws IOException {
            addFile("/foo").setContents("Hello World");

            try (SeekableByteChannel channel = newByteChannel()) {
                assertEquals("Hello", read(channel, 5));
                channel.position(1);
                assertEquals("ell", read(channel, 3));
                channel.position(9);
                assertEquals("ld", read(channel, 1024));
                channel.position(0);
                assertEquals("Hello World", read(channel, 1024));
            }
        }

        @Test
        void testPositionAfterReadsAndSeeks() throws IOException {
            addFile("/foo").setContents("Hello World");

            try (SeekableByteChannel channel = newByteChannel()) {
                assertEquals(0, channel.position());
                read(channel, 5);
                assertEquals(5, channel.position());
                channel.position(2);
                assertEquals(2, channel.position());
                read(channel, 3);
                assertEquals(5, channel.position());
                channel.position(20);
                assertEquals(20, channel.position());
            }
        }

        @Test
        void testSeekToCurrentPositionContinuesDownload() throws IOException {
            addFile("/foo").setContents("Hello World");

            try (SeekableByteChannel channel = newByteChannel()) {
                assertEquals("Hello", read(channel, 5));
                // a new download would fail
                delete("/foo");
                channel.position(5);
                assertEquals(" World", read(channel, 1024));
            }
        }

        @Test
        void testReadAtSizeReturnsEndOfFileWithoutDownloading() throws IOException {
            addFile("/foo").setContents("Hello World");

            try (SeekableByteChannel channel = newByteChannel()) {
                channel.position(11);
                // a new download would fail
                delete("/foo");
                assertEquals(-1, channel.read(ByteBuffer.allocate(10)));
                assertEquals(11, channel.position());
            }
        }

        @Test
        void testReadPastSizeReturnsEndOfFileWithoutDownloading() throws IOException {
            addFile("/foo").setContents("Hello World");

            try (SeekableByteChannel channel = newByteChannel()) {
                channel.position(20);
                // a new download would fail
                delete("/foo");
                assertEquals(-1, channel.read(ByteBuffer.allocate(10)));
                assertEquals(20, channel.position());
            }
        }

        @Test
        void testSeekThroughLinkUsesSizeOfLinkedFile() throws IOException {
            FileEntry foo = addFile("/foo");
            // larger than the listed size of the link itself
            byte[] contents = new byte[(int) SymbolicLinkEntry.SIZE + 36];
            Arrays.fill(contents, (byte) 'x');
            System.arraycopy("World".getBytes(), 0, contents, contents.length - 5, 5);
            foo.setContents(contents);
            addSymLink("/bar", foo);

            try (SeekableByteChannel channel = provider().newByteChannel(createPath("/bar"), EnumSet.of(StandardOpenOption.READ))) {
                assertEquals(contents.length, channel.size());
                channel.position(contents.length - 5);
                assertEquals("World", read(channel, 1024));
            }
        }

        @Test
        void testNegativePositionIsRejected() throws IOException {
            addFile("/foo").setContents("Hello World");

            try (SeekableByteChannel channel = newByteChannel()) {
                assertThrows(IllegalArgumentException.class, () -> channel.position(-1));
                assertEquals(0, channel.position());
                assertEquals("Hello World", read(channel, 1024));
            }
        }

        @Test
        void testPositionOnClosedChannelIsRejected() throws IOException {
            addFile("/foo").setContents("Hello World");

            SeekableByteChannel channel = newByteChannel();
            channel.close();
            assertThrows(ClosedChannelException.class, () -> channel.position(1));
        }

        @Test
        void testRefusedRestartIsReported() throws IOException {
            addFile("/foo").setContents("Hello World");

            CommandHandler restHandler = setCommandHandler("REST", new StaticReplyCommandHandler(502, "REST not implemented"));
            try (SeekableByteChannel channel = newByteChannel()) {
                channel.position(6);
                ByteBuffer buffer = ByteBuffer.allocate(5);
                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> channel.read(buffer));
                assertEquals("/foo", exception.getFile());
                assertEquals(502, exception.getReplyCode());

                verify(getExceptionFactory()).createNewInputStreamException(eq("/foo"), eq(502), anyString());
            } finally {
                setCommandHandler("REST", restHandler);
            }
        }

        @Test
        void testSeekIgnoresReplyToAbortedDownload() throws IOException {
            addFile("/foo").setContents("Hello World");

            CommandHandler retrHandler = setCommandHandler("RETR", new RETRCommandHandler(426));
            try (SeekableByteChannel channel = newByteChannel()) {
                assertEquals("He", read(channel, 2));
                channel.position(6);
                assertEquals("Wor", read(channel, 3));
                channel.position(1);
                assertEquals("ello", read(channel, 4));
            } finally {
                setCommandHandler("RETR", retrHandler);
            }
        }

        @Test
        void testCloseIgnoresReplyToAbortedDownload() throws IOException {
            addFile("/foo").setContents("Hello World");

            CommandHandler retrHandler = setCommandHandler("RETR", new RETRCommandHandler(426));
            try {
                SeekableByteChannel channel = newByteChannel();
                assertEquals("He", read(channel, 2));
                channel.close();
            } finally {
                setCommandHandler("RETR", retrHandler);
            }
        }

        @Test
        void testSeekReportsFailedDownload() throws IOException {
            addFile("/foo").setContents("Hello World");

            CommandHandler retrHandler = setCommandHandler("RETR", new RETRCommandHandler(451));
            try (SeekableByteChannel channel = newByteChannel()) {
                assertEquals("Hello World", read(channel, 1024));
                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, () -> channel.position(0));
                assertEquals(451, exception.getReplyCode());
                assertEquals(11, channel.position());
            } finally {
                setCommandHandler("RETR", retrHandler);
            }
        }

        @Test
        void testCloseReportsFailedDownload() throws IOException {
            addFile("/foo").setContents("Hello World");

            CommandHandler retrHandler = setCommandHandler("RETR", new RETRCommandHandler(451));
            try {
                SeekableByteChannel channel = newByteChannel();
                assertEquals("Hello World", read(channel, 1024));
                FTPFileSystemException exception = assertThrows(FTPFileSystemException.class, channel::close);
                assertEquals(451, exception.getReplyCode());
            } finally {
                setCommandHandler("RETR", retrHandler);
            }
        }

        @Test
        void testDeleteOnCloseDeletesOnlyWhenClosed() throws IOException {
            addFile("/foo").setContents("Hello World");

            try (SeekableByteChannel channel = newByteChannel(StandardOpenOption.DELETE_ON_CLOSE)) {
                assertEquals("Hello", read(channel, 5));
                channel.position(6);
                assertEquals("World", read(channel, 1024));
                assertNotNull(getFileSystemEntry("/foo"));
            }
            assertNull(getFileSystemEntry("/foo"));
        }

        private SeekableByteChannel newByteChannel(StandardOpenOption... options) throws IOException {
            return provider().newByteChannel(createPath("/foo"), EnumSet.of(StandardOpenOption.READ, options));
        }

        private String read(SeekableByteChannel channel, int count) throws IOException {
            ByteBuffer buffer = ByteBuffer.allocate(count);
            while (buffer.hasRemaining() && channel.read(buffer) != -1) {
                // keep reading
            }
            return new String(buffer.array(), 0, buffer.position());
        }
    }
}
