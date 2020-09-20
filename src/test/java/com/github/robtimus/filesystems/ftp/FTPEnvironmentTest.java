/*
 * FTPEnvironmentTest.java
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.parser.DefaultFTPFileEntryParserFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings("nls")
@TestInstance(Lifecycle.PER_CLASS)
class FTPEnvironmentTest {

    FTPEnvironment createFTPEnvironment() {
        return new FTPEnvironment();
    }

    Class<? extends FTPEnvironment> environmentClass() {
        return FTPEnvironment.class;
    }

    private Method findMethod(String methodName) {
        for (Method method : environmentClass().getMethods()) {
            if (method.getName().equals(methodName) && method.getParameterTypes().length == 1) {
                return method;
            }
        }
        throw new AssertionError("Could not find method " + methodName);
    }

    Stream<Arguments> findSetters() {
        Arguments[] arguments = {
                arguments("withSoTimeout", "soTimeout", 1000),
                arguments("withSendBufferSize", "sendBufferSize", 4096),
                arguments("withReceiveBufferSize", "receiveBufferSize", 2048),
                arguments("withTcpNoDelay", "tcpNoDelay", true),
                arguments("withKeepAlive", "keepAlive", true),
                arguments("withSocketFactory", "socketFactory", SocketFactory.getDefault()),
                arguments("withServerSocketFactory", "serverSocketFactory", ServerSocketFactory.getDefault()),
                arguments("withConnectTimeout", "connectTimeout", 1000),
                arguments("withProxy", "proxy", new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 21))),
                arguments("withCharset", "charset", StandardCharsets.UTF_8),
                arguments("withControlEncoding", "controlEncoding", "UTF-8"),
                arguments("withStrictlyMultilineParsing", "strictMultilineParsing", true),
                arguments("withDataTimeout", "dataTimeout", 1000),
                arguments("withParserFactory", "parserFactory", new DefaultFTPFileEntryParserFactory()),
                arguments("withRemoteVerificationEnabled", "remoteVerificationEnabled", true),
                arguments("withDefaultDirectory", "defaultDir", "/"),
                arguments("withConnectionMode", "connectionMode", ConnectionMode.PASSIVE),
                arguments("withActiveExternalIPAddress", "activeExternalIPAddress", "127.0.0.1"),
                arguments("withPassiveLocalIPAddress", "passiveLocalIPAddress", "127.0.0.1"),
                arguments("withReportActiveExternalIPAddress", "reportActiveExternalIPAddress", "127.0.0.1"),
                arguments("withBufferSize", "bufferSize", 1000),
                arguments("withSendDataSocketBufferSize", "sendDataSocketBufferSize", 1024),
                arguments("withReceiveDataSocketBufferSize", "receiveDataSocketBufferSize", 2048),
                arguments("withClientConfig", "clientConfig", new FTPClientConfig()),
                arguments("withUseEPSVwithIPv4", "useEPSVwithIPv4", true),
                arguments("withControlKeepAliveTimeout", "controlKeepAliveTimeout", 1000L),
                arguments("withControlKeepAliveReplyTimeout", "controlKeepAliveReplyTimeout", 1000),
                arguments("withPassiveNatWorkaroundStrategy", "passiveNatWorkaroundStrategy", new FTPClient.NatServerResolverImpl(new FTPClient())),
                arguments("withAutodetectEncoding", "autodetectEncoding", true),
                arguments("withClientConnectionCount", "clientConnectionCount", 5),
                arguments("withClientConnectionWaitTimeout", "clientConnectionWaitTimeout", 1000L),
                arguments("withFileSystemExceptionFactory", "fileSystemExceptionFactory", DefaultFileSystemExceptionFactory.INSTANCE),
                arguments("withFTPFileStrategyFactory", "ftpFileStrategyFactory", FTPFileStrategyFactory.UNIX),
        };
        return Arrays.stream(arguments);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("findSetters")
    void testSetter(String methodName, String propertyName, Object propertyValue) throws ReflectiveOperationException {
        Method setter = findMethod(methodName);

        FTPEnvironment env = createFTPEnvironment();

        assertEquals(Collections.emptyMap(), env);

        setter.invoke(env, propertyValue);

        assertEquals(Collections.singletonMap(propertyName, propertyValue), env);
    }

    @Test
    void testWithLocalAddress() throws UnknownHostException {
        FTPEnvironment env = createFTPEnvironment();

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
    void testWithCredentialsWithoutAccount() {
        FTPEnvironment env = createFTPEnvironment();

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
    void testWithCredentialsWithAccount() {
        FTPEnvironment env = createFTPEnvironment();

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
    void testWithSoLinger() {
        FTPEnvironment env = createFTPEnvironment();

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
    void testWithActivePortRange() {
        FTPEnvironment env = createFTPEnvironment();

        assertEquals(Collections.emptyMap(), env);

        int minPort = 1234;
        int maxPort = 5678;

        env.withActivePortRange(minPort, maxPort);

        Map<String, Object> expected = new HashMap<>();
        expected.put("activePortRange.min", minPort);
        expected.put("activePortRange.max", maxPort);
        assertEquals(expected, env);
    }

    @Test
    void testGetFTPFileStrategy() {
        FTPEnvironment env = createFTPEnvironment();

        env.clear();
        assertSame(FTPFileStrategy.autoDetect().getClass(), env.getFTPFileStrategy().getClass());

        env.withFTPFileStrategyFactory(FTPFileStrategyFactory.UNIX);
        assertSame(FTPFileStrategy.unix(), env.getFTPFileStrategy());

        env.withFTPFileStrategyFactory(FTPFileStrategyFactory.NON_UNIX);
        assertSame(FTPFileStrategy.nonUnix(), env.getFTPFileStrategy());

        env.withFTPFileStrategyFactory(FTPFileStrategyFactory.AUTO_DETECT);
        assertSame(FTPFileStrategy.autoDetect().getClass(), env.getFTPFileStrategy().getClass());
    }

    @Test
    void testWithClientConnectionWaitTimeoutWithUnit() {
        FTPEnvironment env = createFTPEnvironment();

        assertEquals(Collections.emptyMap(), env);

        env.withClientConnectionWaitTimeout(1, TimeUnit.MINUTES);

        Map<String, Object> expected = new HashMap<>();
        expected.put("clientConnectionWaitTimeout", 60_000L);
        assertEquals(expected, env);
    }
}
