/*
 * FTPFileSystemInputStreamTest.java
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

import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.NON_UNIX;
import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.UNIX;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.filesystem.FileEntry;

@SuppressWarnings("nls")
class FTPFileSystemInputStreamTest {

    @Nested
    @DisplayName("Use UNIX FTP server: true; FTPFile strategy factory: UNIX")
    class UnixServerUsingUnixStrategy extends InputStreamTest {

        UnixServerUsingUnixStrategy() {
            super(true, UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: true; FTPFile strategy factory: NON_UNIX")
    class UnixServerUsingNonUnixStrategy extends InputStreamTest {

        UnixServerUsingNonUnixStrategy() {
            super(true, NON_UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: false; FTPFile strategy factory: UNIX")
    class NonUnixServerUsingUnixStrategy extends InputStreamTest {

        NonUnixServerUsingUnixStrategy() {
            super(false, UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: false; FTPFile strategy factory: NON_UNIX")
    class NonUnixServerNotUsingAbsoluteFilePaths extends InputStreamTest {

        NonUnixServerNotUsingAbsoluteFilePaths() {
            super(false, NON_UNIX);
        }
    }

    abstract static class InputStreamTest extends AbstractFTPFileSystemTest {

        private InputStreamTest(boolean useUnixFtpServer, StandardFTPFileStrategyFactory ftpFileStrategyFactory) {
            super(useUnixFtpServer, ftpFileStrategyFactory);
        }

        @Test
        void testReadSingle() throws IOException {
            final String content = "Hello World";

            FileEntry file = addFile("/foo");
            file.setContents(content);

            try (InputStream input = fileSystem.newInputStream(createPath("/foo"))) {
                assertEquals('H', input.read());
                assertEquals('e', input.read());
                assertEquals('l', input.read());
                assertEquals('l', input.read());
                assertEquals('o', input.read());
                assertEquals(' ', input.read());
                assertEquals('W', input.read());
                assertEquals('o', input.read());
                assertEquals('r', input.read());
                assertEquals('l', input.read());
                assertEquals('d', input.read());
                assertEquals(-1, input.read());
            }
        }

        @Test
        void testReadBulk() throws IOException {
            final String content = "Hello World";

            FileEntry file = addFile("/foo");
            file.setContents(content);

            byte[] b = new byte[20];
            try (InputStream input = fileSystem.newInputStream(createPath("/foo"))) {
                assertEquals(0, input.read(b, 0, 0));
                assertEquals(5, input.read(b, 1, 5));
                assertArrayEquals(content.substring(0, 5).getBytes(), Arrays.copyOfRange(b, 1, 6));
                assertEquals(content.length() - 5, input.read(b));
                assertArrayEquals(content.substring(5).getBytes(), Arrays.copyOfRange(b, 0, content.length() - 5));
                assertEquals(-1, input.read(b));
            }
        }

        @Test
        void testSkip() throws IOException {
            final String content = "Hello World";

            FileEntry file = addFile("/foo");
            file.setContents(content);

            try (InputStream input = fileSystem.newInputStream(createPath("/foo"))) {
                assertEquals(0, input.skip(0));
                assertArrayEquals(content.getBytes(), readRemaining(input));
            }
            try (InputStream input = fileSystem.newInputStream(createPath("/foo"))) {
                assertEquals(5, input.skip(5));
                assertArrayEquals(content.substring(5).getBytes(), readRemaining(input));
            }
            try (InputStream input = fileSystem.newInputStream(createPath("/foo"))) {
                assertEquals(content.length(), input.skip(content.length()));
                assertEquals(-1, input.read());
                assertEquals(0, input.skip(1));
            }
            try (InputStream input = fileSystem.newInputStream(createPath("/foo"))) {
                assertEquals(content.length(), input.skip(content.length() + 1));
                assertEquals(-1, input.read());
                assertEquals(0, input.skip(1));
            }
        }

        @Test
        void testAvailable() throws IOException {
            final String content = "Hello World";

            FileEntry file = addFile("/foo");
            file.setContents(content);

            try (InputStream input = fileSystem.newInputStream(createPath("/foo"))) {
                // there is a timing issue here that may cause a different result for different instances each time
                await().atMost(Duration.ofMillis(100)).pollDelay(Duration.ofMillis(10)).until(() -> input.available() == content.length());

                assertEquals(content.length(), input.available());

                for (int i = 0; i < 5; i++) {
                    input.read();
                }
                assertEquals(content.length() - 5, input.available());
                while (input.read() != -1) {
                    // do nothing
                }
                assertEquals(0, input.available());

                input.read();
                assertEquals(0, input.available());
            }
        }

        private byte[] readRemaining(InputStream input) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = input.read(buffer)) != -1) {
                output.write(buffer, 0, len);
            }
            return output.toByteArray();
        }
    }
}
