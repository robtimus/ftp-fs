/*
 * FileTransferModeTest.java
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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.Test;

class FileTransferModeTest {

    @Test
    void testApply() throws IOException {
        FTPClient client = mock(FTPClient.class);
        when(client.setFileTransferMode(FTP.STREAM_TRANSFER_MODE)).thenReturn(true);
        FileTransferMode.STREAM.apply(client);
        verify(client).setFileTransferMode(FTP.STREAM_TRANSFER_MODE);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileTransferMode(FTP.BLOCK_TRANSFER_MODE)).thenReturn(true);
        FileTransferMode.BLOCK.apply(client);
        verify(client).setFileTransferMode(FTP.BLOCK_TRANSFER_MODE);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileTransferMode(FTP.COMPRESSED_TRANSFER_MODE)).thenReturn(true);
        FileTransferMode.COMPRESSED.apply(client);
        verify(client).setFileTransferMode(FTP.COMPRESSED_TRANSFER_MODE);
        verifyNoMoreInteractions(client);
    }

    @Test
    void testApplyFailure() throws IOException {
        FTPClient client = mock(FTPClient.class);
        when(client.setFileTransferMode(FTP.STREAM_TRANSFER_MODE)).thenReturn(false);
        assertThrows(FTPFileSystemException.class, () -> FileTransferMode.STREAM.apply(client));
    }
}
