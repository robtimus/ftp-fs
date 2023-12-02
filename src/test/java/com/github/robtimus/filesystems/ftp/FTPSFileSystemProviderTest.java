/*
 * FTPSFileSystemProviderTest.java
 * Copyright 2023 Rob Spoor
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

import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.UNIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import javax.net.ssl.SSLException;
import org.junit.jupiter.api.Test;

@SuppressWarnings("nls")
class FTPSFileSystemProviderTest extends AbstractFTPFileSystemTest {

    // Cannot setup an FTPS server just yet, so just make sure that FTPS is used

    FTPSFileSystemProviderTest() {
        super(true, StandardFTPFileStrategyFactory.UNIX);
    }

    @Test
    void testNewFileSystem() {
        URI uri = URI.create(getBaseUrlWithCredentials().replace("ftp://", "ftps://"));
        FTPSEnvironment env = FTPSEnvironment.copy(createMinimalEnv(UNIX));
        assertThrows(SSLException.class, () -> FileSystems.newFileSystem(uri, env));
    }

    @Test
    void testGetPath() {
        URI uri = URI.create(getBaseUrlWithCredentials().replace("ftp://", "ftps://"));
        FileSystemNotFoundException exception = assertThrows(FileSystemNotFoundException.class, () -> Paths.get(uri));
        assertEquals(FTPFileSystemProvider.normalizeWithoutPassword(uri).toString(), exception.getMessage());
        assertInstanceOf(SSLException.class, exception.getCause());
    }
}
