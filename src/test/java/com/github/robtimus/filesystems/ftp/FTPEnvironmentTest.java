/*
 * FTPEnvironmentSetter.java
 * Copyright 2017 Rob Spoor
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

import static org.junit.Assert.assertEquals;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.Test;

@SuppressWarnings({ "nls", "javadoc" })
public class FTPEnvironmentTest {

    @Test
    public void testWithLocalAddress() throws UnknownHostException {
        FTPEnvironment env = new FTPEnvironment();

        assertEquals(Collections.emptyMap(), env);

        InetAddress localAddr = InetAddress.getLocalHost();
        int localPort = 21;

        env.withLocalAddress(localAddr, localPort);

        Map<String, Object> expected = new HashMap<>();
        expected.put("localAddr", localAddr);
        expected.put("localPort", localPort);
        assertEquals(expected, env);
    }

    @Test
    public void testWithCredentialsWithoutAccount() {
        FTPEnvironment env = new FTPEnvironment();

        assertEquals(Collections.emptyMap(), env);

        String username = UUID.randomUUID().toString();
        char[] password = UUID.randomUUID().toString().toCharArray();

        env.withCredentials(username, password);

        Map<String, Object> expected = new HashMap<>();
        expected.put("username", username);
        expected.put("password", password);
        assertEquals(expected, env);
    }

    @Test
    public void testWithCredentialsWithAccount() {
        FTPEnvironment env = new FTPEnvironment();

        assertEquals(Collections.emptyMap(), env);

        String username = UUID.randomUUID().toString();
        char[] password = UUID.randomUUID().toString().toCharArray();
        String account = UUID.randomUUID().toString();

        env.withCredentials(username, password, account);

        Map<String, Object> expected = new HashMap<>();
        expected.put("username", username);
        expected.put("password", password);
        expected.put("account", account);
        assertEquals(expected, env);
    }

    @Test
    public void testWithSoLinger() {
        FTPEnvironment env = new FTPEnvironment();

        assertEquals(Collections.emptyMap(), env);

        boolean on = true;
        int linger = 5000;

        env.withSoLinger(on, linger);

        Map<String, Object> expected = new HashMap<>();
        expected.put("soLinger.on", on);
        expected.put("soLinger.val", linger);
        assertEquals(expected, env);
    }

    @Test
    public void testWithActivePortRange() {
        FTPEnvironment env = new FTPEnvironment();

        assertEquals(Collections.emptyMap(), env);

        int minPort = 1234;
        int maxPort = 5678;

        env.withActivePortRange(minPort, maxPort);

        Map<String, Object> expected = new HashMap<>();
        expected.put("activePortRange.min", minPort);
        expected.put("activePortRange.max", maxPort);
        assertEquals(expected, env);
    }
}
