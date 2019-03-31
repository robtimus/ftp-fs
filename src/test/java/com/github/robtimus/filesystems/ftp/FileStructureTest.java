/*
 * FileStructureTest.java
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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import java.io.IOException;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.junit.Test;

@SuppressWarnings("javadoc")
public class FileStructureTest {

    @Test
    public void testApply() throws IOException {
        FTPClient client = mock(FTPClient.class);
        when(client.setFileStructure(FTP.FILE_STRUCTURE)).thenReturn(true);
        FileStructure.FILE.apply(client);
        verify(client).setFileStructure(FTP.FILE_STRUCTURE);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileStructure(FTP.RECORD_STRUCTURE)).thenReturn(true);
        FileStructure.RECORD.apply(client);
        verify(client).setFileStructure(FTP.RECORD_STRUCTURE);
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        when(client.setFileStructure(FTP.PAGE_STRUCTURE)).thenReturn(true);
        FileStructure.PAGE.apply(client);
        verify(client).setFileStructure(FTP.PAGE_STRUCTURE);
        verifyNoMoreInteractions(client);
    }

    @Test(expected = FTPFileSystemException.class)
    public void testApplyFailure() throws IOException {
        FTPClient client = mock(FTPClient.class);
        when(client.setFileStructure(FTP.FILE_STRUCTURE)).thenReturn(false);
        FileStructure.FILE.apply(client);
    }
}
