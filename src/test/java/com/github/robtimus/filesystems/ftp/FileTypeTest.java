/*
 * FileTypeTest.java
 * Copyright 2019 Rob Spoor
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import com.github.robtimus.filesystems.ftp.FileType.Format;

@SuppressWarnings("nls")
@TestInstance(Lifecycle.PER_CLASS)
class FileTypeTest {

    @Test
    void testApply() throws IOException {
        FTPClient client = mock(FTPClient.class);
        when(client.setFileType(FTP.ASCII_FILE_TYPE)).thenReturn(true);
        FileType.ascii().apply(client);
        verify(client).setFileType(FTP.ASCII_FILE_TYPE);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.ASCII_FILE_TYPE, FTP.NON_PRINT_TEXT_FORMAT)).thenReturn(true);
        FileType.ascii(Format.NON_PRINT).apply(client);
        verify(client).setFileType(FTP.ASCII_FILE_TYPE, FTP.NON_PRINT_TEXT_FORMAT);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.ASCII_FILE_TYPE, FTP.TELNET_TEXT_FORMAT)).thenReturn(true);
        FileType.ascii(Format.TELNET).apply(client);
        verify(client).setFileType(FTP.ASCII_FILE_TYPE, FTP.TELNET_TEXT_FORMAT);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.ASCII_FILE_TYPE, FTP.CARRIAGE_CONTROL_TEXT_FORMAT)).thenReturn(true);
        FileType.ascii(Format.CARRIAGE_CONTROL).apply(client);
        verify(client).setFileType(FTP.ASCII_FILE_TYPE, FTP.CARRIAGE_CONTROL_TEXT_FORMAT);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.EBCDIC_FILE_TYPE)).thenReturn(true);
        FileType.ebcdic().apply(client);
        verify(client).setFileType(FTP.EBCDIC_FILE_TYPE);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.EBCDIC_FILE_TYPE, FTP.NON_PRINT_TEXT_FORMAT)).thenReturn(true);
        FileType.ebcdic(Format.NON_PRINT).apply(client);
        verify(client).setFileType(FTP.EBCDIC_FILE_TYPE, FTP.NON_PRINT_TEXT_FORMAT);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.EBCDIC_FILE_TYPE, FTP.TELNET_TEXT_FORMAT)).thenReturn(true);
        FileType.ebcdic(Format.TELNET).apply(client);
        verify(client).setFileType(FTP.EBCDIC_FILE_TYPE, FTP.TELNET_TEXT_FORMAT);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.EBCDIC_FILE_TYPE, FTP.CARRIAGE_CONTROL_TEXT_FORMAT)).thenReturn(true);
        FileType.ebcdic(Format.CARRIAGE_CONTROL).apply(client);
        verify(client).setFileType(FTP.EBCDIC_FILE_TYPE, FTP.CARRIAGE_CONTROL_TEXT_FORMAT);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.BINARY_FILE_TYPE)).thenReturn(true);
        FileType.binary().apply(client);
        verify(client).setFileType(FTP.BINARY_FILE_TYPE);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.LOCAL_FILE_TYPE)).thenReturn(true);
        FileType.local().apply(client);
        verify(client).setFileType(FTP.LOCAL_FILE_TYPE);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.LOCAL_FILE_TYPE)).thenReturn(true);
        FileType.local(0).apply(client);
        verify(client).setFileType(FTP.LOCAL_FILE_TYPE);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileType(FTP.LOCAL_FILE_TYPE, 1024)).thenReturn(true);
        FileType.local(1024).apply(client);
        verify(client).setFileType(FTP.LOCAL_FILE_TYPE, 1024);
        verifyNoMoreInteractions(client);
    }

    @Test
    void testApplyFailure() throws IOException {
        FTPClient client = mock(FTPClient.class);
        when(client.setFileType(FTP.ASCII_FILE_TYPE)).thenReturn(false);
        assertThrows(FTPFileSystemException.class, () -> FileType.ascii().apply(client));
    }

    @Nested
    @TestInstance(Lifecycle.PER_CLASS)
    class Equals {

        @ParameterizedTest(name = "{0} - {1}")
        @MethodSource("equalsArguments")
        void testEquals(FileType fileType, FileType expected) {
            assertEquals(expected, fileType);
        }

        Arguments[] equalsArguments() {
            return new Arguments[] {
                    arguments(FileType.ascii(), FileType.ascii()),
                    arguments(FileType.ascii(null), FileType.ascii()),
                    arguments(FileType.ascii(Format.NON_PRINT), FileType.ascii(Format.NON_PRINT)),
                    arguments(FileType.ascii(Format.TELNET), FileType.ascii(Format.TELNET)),
                    arguments(FileType.ascii(Format.CARRIAGE_CONTROL), FileType.ascii(Format.CARRIAGE_CONTROL)),
                    arguments(FileType.ebcdic(), FileType.ebcdic()),
                    arguments(FileType.ebcdic(null), FileType.ebcdic()),
                    arguments(FileType.ebcdic(Format.NON_PRINT), FileType.ebcdic(Format.NON_PRINT)),
                    arguments(FileType.ebcdic(Format.TELNET), FileType.ebcdic(Format.TELNET)),
                    arguments(FileType.ebcdic(Format.CARRIAGE_CONTROL), FileType.ebcdic(Format.CARRIAGE_CONTROL)),
                    arguments(FileType.binary(), FileType.binary()),
                    arguments(FileType.local(), FileType.local()),
                    arguments(FileType.local(0), FileType.local(0)),
                    arguments(FileType.local(0), FileType.local()),
                    arguments(FileType.local(1024), FileType.local(1024)),
            };
        }

        @ParameterizedTest(name = "{0} - {1}")
        @MethodSource("notEqualsArguments")
        void testNotEquals(FileType fileType, Object unexpected) {
            assertNotEquals(unexpected, fileType);
        }

        Arguments[] notEqualsArguments() {
            return new Arguments[] {
                    arguments(FileType.ascii(Format.NON_PRINT), FileType.ascii()),
                    arguments(FileType.ascii(Format.TELNET), FileType.ascii()),
                    arguments(FileType.ascii(Format.CARRIAGE_CONTROL), FileType.ascii()),
                    arguments(FileType.ebcdic(Format.NON_PRINT), FileType.ebcdic()),
                    arguments(FileType.ebcdic(Format.TELNET), FileType.ebcdic()),
                    arguments(FileType.ebcdic(Format.CARRIAGE_CONTROL), FileType.ebcdic()),
                    arguments(FileType.local(1024), FileType.local()),
                    arguments(FileType.local(2048), FileType.local(1024)),

                    arguments(FileType.ebcdic(), FileType.ascii()),

                    arguments(FileType.ascii(), null),
                    arguments(FileType.ascii(null), null),
                    arguments(FileType.ascii(Format.NON_PRINT), null),
                    arguments(FileType.ascii(Format.TELNET), null),
                    arguments(FileType.ascii(Format.CARRIAGE_CONTROL), null),
                    arguments(FileType.ebcdic(), null),
                    arguments(FileType.ebcdic(null), null),
                    arguments(FileType.ebcdic(Format.NON_PRINT), null),
                    arguments(FileType.ebcdic(Format.TELNET), null),
                    arguments(FileType.ebcdic(Format.CARRIAGE_CONTROL), null),
                    arguments(FileType.binary(), null),
                    arguments(FileType.local(), null),
                    arguments(FileType.local(0), null),
                    arguments(FileType.local(1024), null),

                    arguments(FileType.ascii(), "foo"),
                    arguments(FileType.ascii(null), "foo"),
                    arguments(FileType.ascii(Format.NON_PRINT), "foo"),
                    arguments(FileType.ascii(Format.TELNET), "foo"),
                    arguments(FileType.ascii(Format.CARRIAGE_CONTROL), "foo"),
                    arguments(FileType.ebcdic(), "foo"),
                    arguments(FileType.ebcdic(null), "foo"),
                    arguments(FileType.ebcdic(Format.NON_PRINT), "foo"),
                    arguments(FileType.ebcdic(Format.TELNET), "foo"),
                    arguments(FileType.ebcdic(Format.CARRIAGE_CONTROL), "foo"),
                    arguments(FileType.binary(), "foo"),
                    arguments(FileType.local(), "foo"),
                    arguments(FileType.local(0), "foo"),
                    arguments(FileType.local(1024), "foo"),
            };
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("toStringArguments")
    void testToString(FileType fileType, String expected) {
        assertEquals(expected, fileType.toString());
    }

    Arguments[] toStringArguments() {
        return new Arguments[] {
                arguments(FileType.ascii(), "FileType.ascii"),
                arguments(FileType.ascii(null), "FileType.ascii"),
                arguments(FileType.ascii(Format.NON_PRINT), "FileType.ascii(NON_PRINT)"),
                arguments(FileType.ascii(Format.TELNET), "FileType.ascii(TELNET)"),
                arguments(FileType.ascii(Format.CARRIAGE_CONTROL), "FileType.ascii(CARRIAGE_CONTROL)"),
                arguments(FileType.ebcdic(), "FileType.ebcdic"),
                arguments(FileType.ebcdic(null), "FileType.ebcdic"),
                arguments(FileType.ebcdic(Format.NON_PRINT), "FileType.ebcdic(NON_PRINT)"),
                arguments(FileType.ebcdic(Format.TELNET), "FileType.ebcdic(TELNET)"),
                arguments(FileType.ebcdic(Format.CARRIAGE_CONTROL), "FileType.ebcdic(CARRIAGE_CONTROL)"),
                arguments(FileType.binary(), "FileType.binary"),
                arguments(FileType.local(), "FileType.local"),
                arguments(FileType.local(0), "FileType.local"),
                arguments(FileType.local(1024), "FileType.local(1024)"),
        };
    }
}
