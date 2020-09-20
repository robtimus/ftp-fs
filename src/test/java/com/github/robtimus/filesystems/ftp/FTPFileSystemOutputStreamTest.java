/*
 * FTPFileSystemOutputStreamTest.java
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.io.OutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockftpserver.fake.filesystem.FileEntry;

@SuppressWarnings("nls")
class FTPFileSystemOutputStreamTest {

    @Nested
    @DisplayName("Use UNIX FTP server: true; FTPFile strategy factory: UNIX")
    class UnixServerUsingUnixStrategy extends OutputStreamTest {

        UnixServerUsingUnixStrategy() {
            super(true, UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: true; FTPFile strategy factory: NON_UNIX")
    class UnixServerUsingNonUnixStrategy extends OutputStreamTest {

        UnixServerUsingNonUnixStrategy() {
            super(true, NON_UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: false; FTPFile strategy factory: UNIX")
    class NonUnixServerUsingUnixStrategy extends OutputStreamTest {

        NonUnixServerUsingUnixStrategy() {
            super(false, UNIX);
        }
    }

    @Nested
    @DisplayName("Use UNIX FTP server: false; FTPFile strategy factory: NON_UNIX")
    class NonUnixServerUsingNonUnixStrategy extends OutputStreamTest {

        NonUnixServerUsingNonUnixStrategy() {
            super(false, NON_UNIX);
        }
    }

    abstract static class OutputStreamTest extends AbstractFTPFileSystemTest {

        private OutputStreamTest(boolean useUnixFtpServer, StandardFTPFileStrategyFactory ftpFileStrategyFactory) {
            super(useUnixFtpServer, ftpFileStrategyFactory);
        }

        @Test
        void testWriteSingle() throws IOException {

            try (OutputStream output = fileSystem.newOutputStream(createPath("/foo"))) {
                output.write('H');
                output.write('e');
                output.write('l');
                output.write('l');
                output.write('o');
            }
            FileEntry file = getFile("/foo");
            assertEquals("Hello", getStringContents(file));
        }

        @Test
        void testWriteBulk() throws IOException {

            try (OutputStream output = fileSystem.newOutputStream(createPath("/foo"))) {
                output.write("Hello".getBytes());
            }
            FileEntry file = getFile("/foo");
            assertEquals("Hello", getStringContents(file));
        }
    }
}
