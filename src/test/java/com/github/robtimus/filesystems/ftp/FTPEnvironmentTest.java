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

import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.AUTO_DETECT;
import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.NON_UNIX;
import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.UNIX;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.io.IOException;
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
import org.apache.commons.net.ftp.FTPClient.HostnameResolver;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.parser.DefaultFTPFileEntryParserFactory;
import org.apache.commons.net.ftp.parser.FTPFileEntryParserFactory;
import org.junit.jupiter.api.Nested;
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
                arguments("withStrictMultilineParsing", "strictMultilineParsing", true),
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
                arguments("withListHiddenFiles", "listHiddenFiles", false),
                arguments("withClientConnectionCount", "clientConnectionCount", 5),
                arguments("withClientConnectionWaitTimeout", "clientConnectionWaitTimeout", 1000L),
                arguments("withFileSystemExceptionFactory", "fileSystemExceptionFactory", DefaultFileSystemExceptionFactory.INSTANCE),
                arguments("withFTPFileStrategyFactory", "ftpFileStrategyFactory", UNIX),
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

        env.withFTPFileStrategyFactory(UNIX);
        assertSame(FTPFileStrategy.unix(), env.getFTPFileStrategy());

        env.withFTPFileStrategyFactory(NON_UNIX);
        assertSame(FTPFileStrategy.nonUnix(), env.getFTPFileStrategy());

        env.withFTPFileStrategyFactory(AUTO_DETECT);
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

    @Nested
    class InitializePreConnectTest {

        @Nested
        class SendBufferSizeTest {

            @Test
            void testSendBufferSizeNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setSendBufferSize(anyInt());
            }

            @Test
            void testSendBufferSizeSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withSendBufferSize(100);
                env.initializePreConnect(client);

                verify(client).setSendBufferSize(100);
            }
        }

        @Nested
        class ReceiveBufferSizeTest {

            @Test
            void testSendBufferSizeNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setReceiveBufferSize(anyInt());
            }

            @Test
            void testReceiveBufferSizeSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withReceiveBufferSize(100);
                env.initializePreConnect(client);

                verify(client).setReceiveBufferSize(100);
            }
        }

        @Nested
        class SocketFactoryTestTest {

            @Test
            void testSocketFactoryNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setSocketFactory(any());
            }

            @Test
            void testSocketFactorySet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                SocketFactory socketFactory = mock(SocketFactory.class);

                FTPEnvironment env = new FTPEnvironment().withSocketFactory(socketFactory);
                env.initializePreConnect(client);

                verify(client).setSocketFactory(socketFactory);
            }

            @Test
            void testSocketFactorySetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withSocketFactory(null);
                env.initializePreConnect(client);

                verify(client).setSocketFactory(null);
            }
        }

        @Nested
        class ServerSocketFactoryTest {

            @Test
            void testServerSocketFactoryNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setServerSocketFactory(any());
            }

            @Test
            void testServerSocketFactorySet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                ServerSocketFactory serverSocketFactory = mock(ServerSocketFactory.class);

                FTPEnvironment env = new FTPEnvironment().withServerSocketFactory(serverSocketFactory);
                env.initializePreConnect(client);

                verify(client).setServerSocketFactory(serverSocketFactory);
            }

            @Test
            void testServerSocketFactorySetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withServerSocketFactory(null);
                env.initializePreConnect(client);

                verify(client).setServerSocketFactory(null);
            }
        }

        @Nested
        class ConnectTimeout {

            @Test
            void testConnectTimeoutNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setConnectTimeout(anyInt());
            }

            @Test
            void testConnectTimeoutSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withConnectTimeout(100);
                env.initializePreConnect(client);

                verify(client).setConnectTimeout(100);
            }
        }

        @Nested
        class ProxyTest {

            @Test
            void testProxyNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setProxy(any());
            }

            @Test
            void testProxySet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                Proxy proxy = mock(Proxy.class);

                FTPEnvironment env = new FTPEnvironment().withProxy(proxy);
                env.initializePreConnect(client);

                verify(client).setProxy(proxy);
            }

            @Test
            void testProxySetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withProxy(null);
                env.initializePreConnect(client);

                verify(client).setProxy(null);
            }
        }

        @Nested
        class CharsetTest {

            @Test
            void testCharsetNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setCharset(any());
            }

            @Test
            void testCharsetSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withCharset(StandardCharsets.UTF_8);
                env.initializePreConnect(client);

                verify(client).setCharset(StandardCharsets.UTF_8);
            }

            @Test
            void testCharsetSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withCharset(null);
                env.initializePreConnect(client);

                verify(client).setCharset(null);
            }
        }

        @Nested
        class ControlEncodingTest {

            @Test
            void testControlEncodingNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setControlEncoding(anyString());
            }

            @Test
            void testControlEncodingSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withControlEncoding("encoding");
                env.initializePreConnect(client);

                verify(client).setControlEncoding("encoding");
            }

            @Test
            void testControlEncodingSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withControlEncoding(null);
                env.initializePreConnect(client);

                verify(client).setControlEncoding(null);
            }
        }

        @Nested
        class StrictMultilineParsingTest {

            @Test
            void testStrictMultilineParsingNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setStrictMultilineParsing(anyBoolean());
            }

            @Test
            void testStrictMultilineParsingSetToTrue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withStrictMultilineParsing(true);
                env.initializePreConnect(client);

                verify(client).setStrictMultilineParsing(true);
            }

            @Test
            void testStrictMultilineParsingSetToFalse() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withStrictMultilineParsing(false);
                env.initializePreConnect(client);

                verify(client).setStrictMultilineParsing(false);
            }
        }

        @Nested
        class DataTimeout {

            @Test
            void testDataTimeoutNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setDataTimeout(anyInt());
            }

            @Test
            void testDataTimeoutSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withDataTimeout(100);
                env.initializePreConnect(client);

                verify(client).setDataTimeout(100);
            }
        }

        @Nested
        class ParserFactoryTest {

            @Test
            void testParserFactoryNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setParserFactory(any());
            }

            @Test
            void testParserFactorySet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPFileEntryParserFactory parserFactory = mock(FTPFileEntryParserFactory.class);

                FTPEnvironment env = new FTPEnvironment().withParserFactory(parserFactory);
                env.initializePreConnect(client);

                verify(client).setParserFactory(parserFactory);
            }

            @Test
            void testParserFactorySetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withParserFactory(null);
                env.initializePreConnect(client);

                verify(client).setParserFactory(null);
            }
        }

        @Nested
        class RemoteVerificationEnabledTest {

            @Test
            void testRemoteVerificationEnabledNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setRemoteVerificationEnabled(anyBoolean());
            }

            @Test
            void testRemoteVerificationEnabledSetToTrue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withRemoteVerificationEnabled(true);
                env.initializePreConnect(client);

                verify(client).setRemoteVerificationEnabled(true);
            }

            @Test
            void testRemoteVerificationEnabledSetToFalse() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withRemoteVerificationEnabled(false);
                env.initializePreConnect(client);

                verify(client).setRemoteVerificationEnabled(false);
            }
        }

        @Nested
        class ConnectionModeTest {

            @Test
            void testConnectionModeNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client).enterLocalActiveMode();
                verify(client, never()).enterLocalPassiveMode();
                verify(client, never()).enterRemoteActiveMode(any(), anyInt());
                verify(client, never()).enterRemotePassiveMode();
            }

            @Test
            void testConnectionModeSetToActive() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withConnectionMode(ConnectionMode.ACTIVE);
                env.initializePreConnect(client);

                verify(client).enterLocalActiveMode();
                verify(client, never()).enterLocalPassiveMode();
                verify(client, never()).enterRemoteActiveMode(any(), anyInt());
                verify(client, never()).enterRemotePassiveMode();
            }

            @Test
            void testConnectionModeSetToPassive() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withConnectionMode(ConnectionMode.PASSIVE);
                env.initializePreConnect(client);

                verify(client).enterLocalPassiveMode();
                verify(client, never()).enterLocalActiveMode();
                verify(client, never()).enterRemoteActiveMode(any(), anyInt());
                verify(client, never()).enterRemotePassiveMode();
            }

            @Test
            void testConnectionModeSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withConnectionMode(null);
                env.initializePreConnect(client);

                verify(client).enterLocalActiveMode();
                verify(client, never()).enterLocalPassiveMode();
                verify(client, never()).enterRemoteActiveMode(any(), anyInt());
                verify(client, never()).enterRemotePassiveMode();
            }
        }

        @Nested
        class ActivePortRangeTest {

            @Test
            void testActivePortRangeNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setActivePortRange(anyInt(), anyInt());
            }

            @Test
            void testActivePortRangeMinSetButNotMax() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.put("activePortRange.min", 1000);
                env.initializePostConnect(client);

                verify(client, never()).setActivePortRange(anyInt(), anyInt());
            }

            @Test
            void testActivePortRangeMaxSetButNotMin() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.put("activePortRange.max", 200);
                env.initializePostConnect(client);

                verify(client, never()).setActivePortRange(anyInt(), anyInt());
            }

            @Test
            void testActivePortRangeSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withActivePortRange(1000, 2000);
                env.initializePostConnect(client);

                verify(client).setActivePortRange(1000, 2000);
            }
        }

        @Nested
        class ActiveExternalIPAddressTest {

            @Test
            void testActiveExternalIPAddressNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setActiveExternalIPAddress(anyString());
            }

            @Test
            void testActiveExternalIPAddressSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withActiveExternalIPAddress("127.0.0.1");
                env.initializePostConnect(client);

                verify(client).setActiveExternalIPAddress("127.0.0.1");
            }

            @Test
            void testActiveExternalIPAddressSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withActiveExternalIPAddress(null);
                env.initializePostConnect(client);

                verify(client).setActiveExternalIPAddress(null);
            }
        }

        @Nested
        class PassiveLocalIPAddressTest {

            @Test
            void testPassiveLocalIPAddressNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setPassiveLocalIPAddress(anyString());
            }

            @Test
            void testPassiveLocalIPAddressSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withPassiveLocalIPAddress("127.0.0.1");
                env.initializePostConnect(client);

                verify(client).setPassiveLocalIPAddress("127.0.0.1");
            }

            @Test
            void testPassiveLocalIPAddressSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withPassiveLocalIPAddress(null);
                env.initializePostConnect(client);

                verify(client).setPassiveLocalIPAddress((String) null);
            }
        }

        @Nested
        class ReportActiveExternalIPAddressTest {

            @Test
            void testReportActiveExternalIPAddressNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setReportActiveExternalIPAddress(anyString());
            }

            @Test
            void testReportActiveExternalIPAddressSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withReportActiveExternalIPAddress("127.0.0.1");
                env.initializePostConnect(client);

                verify(client).setReportActiveExternalIPAddress("127.0.0.1");
            }

            @Test
            void testReportActiveExternalIPAddressSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withReportActiveExternalIPAddress(null);
                env.initializePostConnect(client);

                verify(client).setReportActiveExternalIPAddress(null);
            }
        }

        @Nested
        class BufferSizeTest {

            @Test
            void testBufferSizeNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setBufferSize(anyInt());
            }

            @Test
            void testBufferSizeSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withBufferSize(100);
                env.initializePreConnect(client);

                verify(client).setBufferSize(100);
            }
        }

        @Nested
        class SendDataSocketBufferSizeTest {

            @Test
            void testSendDataSocketBufferSizeNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setSendDataSocketBufferSize(anyInt());
            }

            @Test
            void testSendDataSocketBufferSizeSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withSendDataSocketBufferSize(100);
                env.initializePreConnect(client);

                verify(client).setSendDataSocketBufferSize(100);
            }
        }

        @Nested
        class ReceiveDataSocketBufferSizeTest {

            @Test
            void testReceiveDataSocketBufferSizeNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setReceieveDataSocketBufferSize(anyInt());
            }

            @Test
            void testReceiveDataSocketBufferSizeSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withReceiveDataSocketBufferSize(100);
                env.initializePreConnect(client);

                verify(client).setReceieveDataSocketBufferSize(100);
            }
        }

        @Nested
        class ClientConfigTest {

            @Test
            void testClientConfigNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).configure(any());
            }

            @Test
            void testClientConfigSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPClientConfig config = new FTPClientConfig();

                FTPEnvironment env = new FTPEnvironment().withClientConfig(config);
                env.initializePreConnect(client);

                // the config is copied
                verify(client).configure(any());
                verify(client, never()).configure(config);
            }

            @Test
            void testClientConfigSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withClientConfig(null);
                env.initializePreConnect(client);

                // a new config is set
                verify(client).configure(any());
            }
        }

        @Nested
        class UseEPSVwithIPv4Test {

            @Test
            void testUseEPSVwithIPv4NotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setUseEPSVwithIPv4(anyBoolean());
            }

            @Test
            void testUseEPSVwithIPv4SetToTrue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withUseEPSVwithIPv4(true);
                env.initializePreConnect(client);

                verify(client).setUseEPSVwithIPv4(true);
            }

            @Test
            void testUseEPSVwithIPv4SetToFalse() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withUseEPSVwithIPv4(false);
                env.initializePreConnect(client);

                verify(client).setUseEPSVwithIPv4(false);
            }
        }

        @Nested
        class ControlKeepAliveTimeoutTest {

            @Test
            void testControlKeepAliveTimeoutNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setControlKeepAliveTimeout(anyLong());
            }

            @Test
            void testControlKeepAliveTimeoutSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withControlKeepAliveTimeout(1000);
                env.initializePreConnect(client);

                // the environment accepts millis, and converts to seconds
                verify(client).setControlKeepAliveTimeout(1);
            }
        }

        @Nested
        class ControlKeepAliveReplyTimeoutTest {

            @Test
            void testControlKeepAliveReplyTimeoutNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setControlKeepAliveReplyTimeout(anyInt());
            }

            @Test
            void testControlKeepAliveReplyTimeoutSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withControlKeepAliveReplyTimeout(1000);
                env.initializePreConnect(client);

                verify(client).setControlKeepAliveReplyTimeout(1000);
            }
        }

        @Nested
        class PassiveNatWorkaroundStrategyTest {

            @Test
            void testPassiveNatWorkaroundStrategyNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setPassiveNatWorkaroundStrategy(any());
            }

            @Test
            void testPassiveNatWorkaroundStrategySet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                HostnameResolver resolver = mock(HostnameResolver.class);

                FTPEnvironment env = new FTPEnvironment().withPassiveNatWorkaroundStrategy(resolver);
                env.initializePreConnect(client);

                verify(client).setPassiveNatWorkaroundStrategy(resolver);
            }

            @Test
            void testPassiveNatWorkaroundStrategySetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withPassiveNatWorkaroundStrategy(null);
                env.initializePreConnect(client);

                verify(client).setPassiveNatWorkaroundStrategy(null);
            }
        }

        @Nested
        class AutodetectEncodingTest {

            @Test
            void testAutodetectEncodingNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setAutodetectUTF8(anyBoolean());
            }

            @Test
            void testAutodetectEncodingSetToTrue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withAutodetectEncoding(true);
                env.initializePreConnect(client);

                verify(client).setAutodetectUTF8(true);
            }

            @Test
            void testAutodetectEncodingSetToFalse() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withAutodetectEncoding(false);
                env.initializePreConnect(client);

                verify(client).setAutodetectUTF8(false);
            }
        }

        @Nested
        class ListHiddenFilesTest {

            @Test
            void testListHiddenFilesNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client).setListHiddenFiles(true);
            }

            @Test
            void testListHiddenFilesSetToTrue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withListHiddenFiles(true);
                env.initializePreConnect(client);

                verify(client).setListHiddenFiles(true);
            }

            @Test
            void testListHiddenFilesSetToFalse() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withListHiddenFiles(false);
                env.initializePreConnect(client);

                verify(client).setListHiddenFiles(false);
            }
        }
    }

    @Nested
    class ConnectTest {

        @Nested
        class LocalAddressTest {

            @Test
            void testLocalAddressNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);
                verify(client).setListHiddenFiles(true);
            }

            @Test
            void testLocalAddressSet() throws IOException {
                FTPClient client = mock(FTPClient.class);
                doReturn(21).when(client).getDefaultPort();

                String hostname = "localhost";
                InetAddress localHost = InetAddress.getLocalHost();
                int localPort = 8021;

                FTPEnvironment env = new FTPEnvironment().withLocalAddress(localHost, localPort);
                env.connect(client, hostname, -1);

                verify(client).connect(hostname, 21, localHost, localPort);
            }

            @Test
            void testLocalAddressSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);
                doReturn(21).when(client).getDefaultPort();

                String hostname = "localhost";
                FTPEnvironment env = new FTPEnvironment().withLocalAddress(null, 8021);
                env.connect(client, hostname, -1);

                verify(client).connect(hostname, 21);
            }
        }

        @Nested
        class CredentialsTest {

            @Test
            void testCredentialsNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.connect(client, "localhost", -1);

                verify(client, never()).login(nullable(String.class), nullable(String.class));
                verify(client, never()).login(nullable(String.class), nullable(String.class), nullable(String.class));
            }

            @Test
            void testCredentialsSetWithoutAccount() throws IOException {
                FTPClient client = mock(FTPClient.class);
                doReturn(true).when(client).login(anyString(), anyString());

                FTPEnvironment env = new FTPEnvironment().withCredentials("username", "password".toCharArray());
                env.connect(client, "localhost", -1);

                verify(client).login("username", "password");
                verify(client, never()).login(nullable(String.class), nullable(String.class), nullable(String.class));
            }

            @Test
            void testCredentialsSetWithAccount() throws IOException {
                FTPClient client = mock(FTPClient.class);
                doReturn(true).when(client).login(anyString(), anyString(), anyString());

                FTPEnvironment env = new FTPEnvironment().withCredentials("username", "password".toCharArray(), "account");
                env.connect(client, "localhost", -1);

                verify(client).login("username", "password", "account");
                verify(client, never()).login(nullable(String.class), nullable(String.class));
            }

            @Test
            void testCredentialsSetWithNullUsername() throws IOException {
                FTPClient client = mock(FTPClient.class);
                doReturn(true).when(client).login(nullable(String.class), anyString());

                FTPEnvironment env = new FTPEnvironment().withCredentials(null, "password".toCharArray());
                env.connect(client, "localhost", -1);

                verify(client).login(null, "password");
                verify(client, never()).login(nullable(String.class), nullable(String.class), nullable(String.class));
            }

            @Test
            void testCredentialsSetWithNullPassword() throws IOException {
                FTPClient client = mock(FTPClient.class);
                doReturn(true).when(client).login(anyString(), nullable(String.class));

                FTPEnvironment env = new FTPEnvironment().withCredentials("username", null);
                env.connect(client, "localhost", -1);

                verify(client).login("username", null);
                verify(client, never()).login(nullable(String.class), nullable(String.class), nullable(String.class));
            }
        }
    }

    @Nested
    class InitializePostConnectTest {

        @Nested
        class SoTimeoutTest {

            @Test
            void testSoTimeoutNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setSoTimeout(anyInt());
            }

            @Test
            void testSoTimeoutSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withSoTimeout(100);
                env.initializePostConnect(client);

                verify(client).setSoTimeout(100);
            }
        }

        @Nested
        class TcpNoDelayTest {

            @Test
            void testTcpNoDelayNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setTcpNoDelay(anyBoolean());
            }

            @Test
            void testTcpNoDelaySetToTrue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withTcpNoDelay(true);
                env.initializePostConnect(client);

                verify(client).setTcpNoDelay(true);
            }

            @Test
            void testTcpNoDelaySetToFalse() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withTcpNoDelay(false);
                env.initializePostConnect(client);

                verify(client).setTcpNoDelay(false);
            }
        }

        @Nested
        class KeepAliveTest {

            @Test
            void testKeepAliveNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setKeepAlive(anyBoolean());
            }

            @Test
            void testKeepAliveSetToTrue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withKeepAlive(true);
                env.initializePostConnect(client);

                verify(client).setKeepAlive(true);
            }

            @Test
            void testKeepAliveSetToFalse() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withKeepAlive(false);
                env.initializePostConnect(client);

                verify(client).setKeepAlive(false);
            }
        }

        @Nested
        class SoLingerTest {

            @Test
            void testSoLingerNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setSoLinger(anyBoolean(), anyInt());
            }

            @Test
            void testSoLingerSetToTrueWithoutValue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.put("soLinger.on", true);
                env.initializePostConnect(client);

                verify(client, never()).setSoLinger(anyBoolean(), anyInt());
            }

            @Test
            void testSoLingerSetToFalseWithoutValue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.put("soLinger.on", false);
                env.initializePostConnect(client);

                verify(client, never()).setSoLinger(anyBoolean(), anyInt());
            }

            @Test
            void testSoLingerWithValueWithoutFlag() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.put("soLinger.val", 100);
                env.initializePostConnect(client);

                verify(client, never()).setSoLinger(anyBoolean(), anyInt());
            }

            @Test
            void testSoLingerSetToTrueWithValue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withSoLinger(true, 100);
                env.initializePostConnect(client);

                verify(client).setSoLinger(true, 100);
            }

            @Test
            void testSoLingerSetToFalseWithValue() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withSoLinger(false, 100);
                env.initializePostConnect(client);

                verify(client).setSoLinger(false, 100);
            }
        }

        @Nested
        class DefaultDirectoryTest {

            @Test
            void testDefaultDirectoryNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).changeWorkingDirectory(anyString());
            }

            @Test
            void testDefaultDirectorySet() throws IOException {
                FTPClient client = mock(FTPClient.class);
                doReturn(true).when(client).changeWorkingDirectory(anyString());

                FTPEnvironment env = new FTPEnvironment().withDefaultDirectory("/path");
                env.initializePostConnect(client);

                verify(client).changeWorkingDirectory("/path");
            }

            @Test
            void testDefaultDirectorySetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);
                doReturn(true).when(client).changeWorkingDirectory(anyString());

                FTPEnvironment env = new FTPEnvironment().withDefaultDirectory(null);
                env.initializePostConnect(client);

                verify(client, never()).changeWorkingDirectory(anyString());
            }
        }

        @Nested
        class ConnectionModeTest {

            @Test
            void testConnectionModeNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client).enterLocalActiveMode();
                verify(client, never()).enterLocalPassiveMode();
                verify(client, never()).enterRemoteActiveMode(any(), anyInt());
                verify(client, never()).enterRemotePassiveMode();
            }

            @Test
            void testConnectionModeSetToActive() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withConnectionMode(ConnectionMode.ACTIVE);
                env.initializePreConnect(client);

                verify(client).enterLocalActiveMode();
                verify(client, never()).enterLocalPassiveMode();
                verify(client, never()).enterRemoteActiveMode(any(), anyInt());
                verify(client, never()).enterRemotePassiveMode();
            }

            @Test
            void testConnectionModeSetToPassive() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withConnectionMode(ConnectionMode.PASSIVE);
                env.initializePreConnect(client);

                verify(client).enterLocalPassiveMode();
                verify(client, never()).enterLocalActiveMode();
                verify(client, never()).enterRemoteActiveMode(any(), anyInt());
                verify(client, never()).enterRemotePassiveMode();
            }

            @Test
            void testConnectionModeSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withConnectionMode(null);
                env.initializePreConnect(client);

                verify(client).enterLocalActiveMode();
                verify(client, never()).enterLocalPassiveMode();
                verify(client, never()).enterRemoteActiveMode(any(), anyInt());
                verify(client, never()).enterRemotePassiveMode();
            }
        }

        @Nested
        class ActivePortRangeTest {

            @Test
            void testActivePortRangeNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setActivePortRange(anyInt(), anyInt());
            }

            @Test
            void testActivePortRangeMinSetButNotMax() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.put("activePortRange.min", 1000);
                env.initializePostConnect(client);

                verify(client, never()).setActivePortRange(anyInt(), anyInt());
            }

            @Test
            void testActivePortRangeMaxSetButNotMin() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.put("activePortRange.max", 200);
                env.initializePostConnect(client);

                verify(client, never()).setActivePortRange(anyInt(), anyInt());
            }

            @Test
            void testActivePortRangeSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withActivePortRange(1000, 2000);
                env.initializePostConnect(client);

                verify(client).setActivePortRange(1000, 2000);
            }
        }

        @Nested
        class ActiveExternalIPAddressTest {

            @Test
            void testActiveExternalIPAddressNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setActiveExternalIPAddress(anyString());
            }

            @Test
            void testActiveExternalIPAddressSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withActiveExternalIPAddress("127.0.0.1");
                env.initializePostConnect(client);

                verify(client).setActiveExternalIPAddress("127.0.0.1");
            }

            @Test
            void testActiveExternalIPAddressSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withActiveExternalIPAddress(null);
                env.initializePostConnect(client);

                verify(client).setActiveExternalIPAddress(null);
            }
        }

        @Nested
        class PassiveLocalIPAddressTest {

            @Test
            void testPassiveLocalIPAddressNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setPassiveLocalIPAddress(anyString());
            }

            @Test
            void testPassiveLocalIPAddressSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withPassiveLocalIPAddress("127.0.0.1");
                env.initializePostConnect(client);

                verify(client).setPassiveLocalIPAddress("127.0.0.1");
            }

            @Test
            void testPassiveLocalIPAddressSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withPassiveLocalIPAddress(null);
                env.initializePostConnect(client);

                verify(client).setPassiveLocalIPAddress((String) null);
            }
        }

        @Nested
        class ReportActiveExternalIPAddressTest {

            @Test
            void testReportActiveExternalIPAddressNotSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setReportActiveExternalIPAddress(anyString());
            }

            @Test
            void testReportActiveExternalIPAddressSet() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withReportActiveExternalIPAddress("127.0.0.1");
                env.initializePostConnect(client);

                verify(client).setReportActiveExternalIPAddress("127.0.0.1");
            }

            @Test
            void testReportActiveExternalIPAddressSetToNull() throws IOException {
                FTPClient client = mock(FTPClient.class);

                FTPEnvironment env = new FTPEnvironment().withReportActiveExternalIPAddress(null);
                env.initializePostConnect(client);

                verify(client).setReportActiveExternalIPAddress(null);
            }
        }
    }
}
