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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.Test;
import com.github.robtimus.filesystems.ftp.FileType.Format;

@SuppressWarnings({ "nls", "javadoc" })
public class FileTypeTest {

    @Test
    public void testApply() throws IOException {
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
    public void testApplyFailure() throws IOException {
        FTPClient client = mock(FTPClient.class);
        when(client.setFileType(FTP.ASCII_FILE_TYPE)).thenReturn(false);
        assertThrows(FTPFileSystemException.class, () -> FileType.ascii().apply(client));
    }

    @Test
    public void testEquals() {
        assertEquals(FileType.ascii(), FileType.ascii());
        assertEquals(FileType.ascii(), FileType.ascii(null));
        assertEquals(FileType.ascii(Format.NON_PRINT), FileType.ascii(Format.NON_PRINT));
        assertEquals(FileType.ascii(Format.TELNET), FileType.ascii(Format.TELNET));
        assertEquals(FileType.ascii(Format.CARRIAGE_CONTROL), FileType.ascii(Format.CARRIAGE_CONTROL));
        assertEquals(FileType.ebcdic(), FileType.ebcdic());
        assertEquals(FileType.ebcdic(), FileType.ebcdic(null));
        assertEquals(FileType.ebcdic(Format.NON_PRINT), FileType.ebcdic(Format.NON_PRINT));
        assertEquals(FileType.ebcdic(Format.TELNET), FileType.ebcdic(Format.TELNET));
        assertEquals(FileType.ebcdic(Format.CARRIAGE_CONTROL), FileType.ebcdic(Format.CARRIAGE_CONTROL));
        assertEquals(FileType.binary(), FileType.binary());
        assertEquals(FileType.local(), FileType.local());
        assertEquals(FileType.local(0), FileType.local(0));
        assertEquals(FileType.local(), FileType.local(0));
        assertEquals(FileType.local(1024), FileType.local(1024));

        assertNotEquals(FileType.ascii(), FileType.ascii(Format.NON_PRINT));
        assertNotEquals(FileType.ascii(), FileType.ascii(Format.TELNET));
        assertNotEquals(FileType.ascii(), FileType.ascii(Format.CARRIAGE_CONTROL));
        assertNotEquals(FileType.ebcdic(), FileType.ebcdic(Format.NON_PRINT));
        assertNotEquals(FileType.ebcdic(), FileType.ebcdic(Format.TELNET));
        assertNotEquals(FileType.ebcdic(), FileType.ebcdic(Format.CARRIAGE_CONTROL));
        assertNotEquals(FileType.local(), FileType.local(1024));
        assertNotEquals(FileType.local(1024), FileType.local(2048));

        assertNotEquals(FileType.ascii(), FileType.ebcdic());

        assertNotEquals(null, FileType.ascii());
        assertNotEquals(null, FileType.ascii(null));
        assertNotEquals(null, FileType.ascii(Format.NON_PRINT));
        assertNotEquals(null, FileType.ascii(Format.TELNET));
        assertNotEquals(null, FileType.ascii(Format.CARRIAGE_CONTROL));
        assertNotEquals(null, FileType.ebcdic());
        assertNotEquals(null, FileType.ebcdic(null));
        assertNotEquals(null, FileType.ebcdic(Format.NON_PRINT));
        assertNotEquals(null, FileType.ebcdic(Format.TELNET));
        assertNotEquals(null, FileType.ebcdic(Format.CARRIAGE_CONTROL));
        assertNotEquals(null, FileType.binary());
        assertNotEquals(null, FileType.local());
        assertNotEquals(null, FileType.local(0));
        assertNotEquals(null, FileType.local(1024));

        assertNotEquals("foo", FileType.ascii());
        assertNotEquals("foo", FileType.ascii(null));
        assertNotEquals("foo", FileType.ascii(Format.NON_PRINT));
        assertNotEquals("foo", FileType.ascii(Format.TELNET));
        assertNotEquals("foo", FileType.ascii(Format.CARRIAGE_CONTROL));
        assertNotEquals("foo", FileType.ebcdic());
        assertNotEquals("foo", FileType.ebcdic(null));
        assertNotEquals("foo", FileType.ebcdic(Format.NON_PRINT));
        assertNotEquals("foo", FileType.ebcdic(Format.TELNET));
        assertNotEquals("foo", FileType.ebcdic(Format.CARRIAGE_CONTROL));
        assertNotEquals("foo", FileType.binary());
        assertNotEquals("foo", FileType.local());
        assertNotEquals("foo", FileType.local(0));
        assertNotEquals("foo", FileType.local(1024));
    }

    @Test
    public void testToString() {
        assertEquals("FileType.ascii", FileType.ascii().toString());
        assertEquals("FileType.ascii", FileType.ascii(null).toString());
        assertEquals("FileType.ascii(NON_PRINT)", FileType.ascii(Format.NON_PRINT).toString());
        assertEquals("FileType.ascii(TELNET)", FileType.ascii(Format.TELNET).toString());
        assertEquals("FileType.ascii(CARRIAGE_CONTROL)", FileType.ascii(Format.CARRIAGE_CONTROL).toString());
        assertEquals("FileType.ebcdic", FileType.ebcdic().toString());
        assertEquals("FileType.ebcdic", FileType.ebcdic(null).toString());
        assertEquals("FileType.ebcdic(NON_PRINT)", FileType.ebcdic(Format.NON_PRINT).toString());
        assertEquals("FileType.ebcdic(TELNET)", FileType.ebcdic(Format.TELNET).toString());
        assertEquals("FileType.ebcdic(CARRIAGE_CONTROL)", FileType.ebcdic(Format.CARRIAGE_CONTROL).toString());
        assertEquals("FileType.binary", FileType.binary().toString());
        assertEquals("FileType.local", FileType.local().toString());
        assertEquals("FileType.local", FileType.local(0).toString());
        assertEquals("FileType.local(1024)", FileType.local(1024).toString());
    }
}
