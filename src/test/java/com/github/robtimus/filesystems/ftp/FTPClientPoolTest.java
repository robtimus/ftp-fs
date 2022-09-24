/*
 * FTPClientPoolTest.java
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

import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.NON_UNIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.github.robtimus.filesystems.ftp.FTPClientPool.Client;

/**
 * @author Pei-Tang Huang
 */
class FTPClientPoolTest extends AbstractFTPFileSystemTest {

    FTPClientPoolTest() {
        // not going to use any default FTP file system, so FTPFile strategy factory doesn't matter
        super(false, null);
    }

    @Test
    void testGetWithTimeout() throws Exception {
        final int clientCount = 3;

        URI uri = getURI();
        FTPEnvironment env = createEnv(NON_UNIX)
                .withPoolConfig(FTPPoolConfig.custom()
                        .withMaxSize(clientCount)
                        .withMaxWaitTime(Duration.ofMillis(500))
                        .build()
                );

        FTPClientPool pool = new FTPClientPool(uri.getHost(), uri.getPort(), env);
        List<Client> clients = new ArrayList<>();
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
                // exhaust all available clients
                claimClients(pool, clientCount, clients);

                long startTime = System.currentTimeMillis();
                IOException exception = assertThrows(IOException.class, () -> claimClient(pool));
                assertEquals(FTPMessages.clientConnectionWaitTimeoutExpired(), exception.getMessage());
                assertThat(startTime, lessThanOrEqualTo(System.currentTimeMillis() - 500));
            });
        } finally {
            pool.close();
            for (Client client : clients) {
                client.close();
            }
        }
    }

    @SuppressWarnings("resource")
    private void claimClients(FTPClientPool pool, int clientCount, List<Client> clients) throws IOException {
        for (int i = 0; i < clientCount; i++) {
            clients.add(pool.get());
        }
    }

    @SuppressWarnings("resource")
    private void claimClient(FTPClientPool pool) throws IOException {
        pool.get();
    }
}
