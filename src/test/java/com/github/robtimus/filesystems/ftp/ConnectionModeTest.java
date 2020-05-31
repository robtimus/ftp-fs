/*
 * ConnectionModeTest.java
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
import org.apache.commons.net.ftp.FTPClient;
import org.junit.jupiter.api.Test;

@SuppressWarnings("javadoc")
public class ConnectionModeTest {

    @Test
    public void testApply() {
        FTPClient client = mock(FTPClient.class);
        ConnectionMode.ACTIVE.apply(client);
        verify(client).enterLocalActiveMode();
        verifyNoMoreInteractions(client);

        client = mock(FTPClient.class);
        ConnectionMode.PASSIVE.apply(client);
        verify(client).enterLocalPassiveMode();
        verifyNoMoreInteractions(client);
    }
}
