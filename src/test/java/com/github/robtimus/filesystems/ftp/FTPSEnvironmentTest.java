/*
 * FTPSEnvironmentTest.java
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

import static com.github.robtimus.junit.support.ThrowableAssertions.assertChainEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import org.apache.commons.net.ftp.FTPSClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.provider.Arguments;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.junit.support.test.CovariantReturnTests;

@SuppressWarnings("nls")
@TestInstance(Lifecycle.PER_CLASS)
class FTPSEnvironmentTest extends FTPEnvironmentTest {

    @Override
    FTPEnvironment createFTPEnvironment() {
        return new FTPSEnvironment();
    }

    @Override
    Class<? extends FTPEnvironment> environmentClass() {
        return FTPSEnvironment.class;
    }

    @Override
    Stream<Arguments> findSetters() {
        Arguments[] arguments = {
                arguments("withSecurityMode", "securityMode", SecurityMode.EXPLICIT),
                arguments("withSSLContext", "sslContext", assertDoesNotThrow(SSLContext::getDefault)),
                arguments("withProtocol", "protocol", "tls"),
                arguments("withAuthCommand", "authCommand", "auth"),
                arguments("withKeyManager", "keyManager", new TestKeyManager()),
                arguments("withTrustManager", "trustManager", new TestTrustManager()),
                arguments("withHostnameVerifier", "hostnameVerifier", new TestHostnameVerifier()),
                arguments("withEndpointCheckingEnabled", "endpointCheckingEnabled", true),
                arguments("withEnabledSessionCreation", "enabledSessionCreation", true),
                arguments("withNeedClientAuth", "needClientAuth", false),
                arguments("withWantClientAuth", "wantClientAuth", false),
                arguments("withUseClientMode", "useClientMode", true),
                arguments("withEnabledCipherSuites", "enabledCipherSuites", new String[] { "suite1", "suite2", }),
                arguments("withEnabledProtocols", "enabledProtocols", new String[] { "protocol1", "protocol2", }),
                arguments("withDataChannelProtectionLevel", "dataChannelProtectionLevel", DataChannelProtectionLevel.PRIVATE),
        };
        return Stream.concat(super.findSetters(), Arrays.stream(arguments));
    }

    @Override
    @Test
    void testWithQueryString() throws IOException {
        char[] password = "pass".toCharArray();
        FTPSEnvironment env = new FTPSEnvironment()
                .withCredentials("user", password);

        String queryString = "localAddr=127.0.0.1"
                + "&unknown1"
                + "&localPort=12345"
                + "&account=ACC"
                + "&soTimeout=1000"
                + "&sendBufferSize=1024"
                + "&receiveBufferSize=2048"
                + "&tcpNoDelay=true"
                + "&keepAlive=true"
                + "&soLinger.on=true"
                + "&soLinger.val=100"
                + "&connectTimeout=5"
                + "&charset=ASCII"
                + "&controlEncoding=UTF-8"
                + "&strictMultilineParsing=true"
                + "&dataTimeout=PT5S"
                + "&ipAddressFromPasvResponse=true"
                + "&remoteVerificationEnabled=true"
                + "&defaultDir=/home"
                + "&connectionMode=PASSIVE"
                + "&activePortRange.min=10000"
                + "&activePortRange.max=20000"
                + "&activeExternalIPAddress=127.0.0.2"
                + "&passiveLocalIPAddress=127.0.0.3"
                + "&reportActiveExternalIPAddress=127.0.0.4"
                + "&bufferSize=3072"
                + "&sendDataSocketBufferSize=4096"
                + "&receiveDataSocketBufferSize=5120"
                + "&useEPSVwithIPv4=true"
                + "&controlKeepAliveTimeout=PT10S"
                + "&controlKeepAliveReplyTimeout=PT15S"
                + "&autodetectEncoding=true"
                + "&listHiddenFiles=true"
                + "&poolConfig.maxWaitTime=PT5S"
                + "&poolConfig.maxIdleTime=PT10S"
                + "&poolConfig.initialSize=2"
                + "&poolConfig.maxSize=10"
                + "&securityMode=IMPLICIT"
                + "&protocol=TLS"
                + "&authCommand=CMD"
                + "&endpointCheckingEnabled=true"
                + "&enabledSessionCreation=true"
                + "&needClientAuth=true"
                + "&wantClientAuth=true"
                + "&useClientMode=true"
                + "&enabledCipherSuites=X,Y,Z"
                + "&enabledProtocols=A,B,C"
                + "&dataChannelProtectionLevel=CLEAR"
                + "&unknown2";

        env.withQueryString(queryString);

        FTPSEnvironment expected = new FTPSEnvironment()
                .withLocalAddress(InetAddress.getByName("127.0.0.1"), 12345)
                .withCredentials("user", password, "ACC")
                .withSoTimeout(1000)
                .withSendBufferSize(1024)
                .withReceiveBufferSize(2048)
                .withTcpNoDelay(true)
                .withKeepAlive(true)
                .withSoLinger(true, 100)
                .withConnectTimeout(5)
                .withCharset(StandardCharsets.US_ASCII)
                .withControlEncoding("UTF-8")
                .withStrictMultilineParsing(true)
                .withDataTimeout(Duration.ofSeconds(5))
                .withIpAddressFromPasvResponse(true)
                .withRemoteVerificationEnabled(true)
                .withDefaultDirectory("/home")
                .withConnectionMode(ConnectionMode.PASSIVE)
                .withActivePortRange(10000, 20000)
                .withActiveExternalIPAddress("127.0.0.2")
                .withPassiveLocalIPAddress("127.0.0.3")
                .withReportActiveExternalIPAddress("127.0.0.4")
                .withBufferSize(3072)
                .withSendDataSocketBufferSize(4096)
                .withReceiveDataSocketBufferSize(5120)
                .withUseEPSVwithIPv4(true)
                .withControlKeepAliveTimeout(Duration.ofSeconds(10))
                .withControlKeepAliveReplyTimeout(Duration.ofSeconds(15))
                .withAutodetectEncoding(true)
                .withListHiddenFiles(true)
                .withSecurityMode(SecurityMode.IMPLICIT)
                .withProtocol("TLS")
                .withAuthCommand("CMD")
                .withEndpointCheckingEnabled(true)
                .withEnabledSessionCreation(true)
                .withNeedClientAuth(true)
                .withWantClientAuth(true)
                .withUseClientMode(true)
                .withDataChannelProtectionLevel(DataChannelProtectionLevel.CLEAR);

        // FTPPoolConfig doesn't define equals, so it needs to be removed before env can be compared to expected
        FTPPoolConfig poolConfig = assertInstanceOf(FTPPoolConfig.class, env.remove("poolConfig"));

        assertEquals(Optional.of(Duration.ofSeconds(5)), poolConfig.maxWaitTime());
        assertEquals(Optional.of(Duration.ofSeconds(10)), poolConfig.maxIdleTime());
        assertEquals(2, poolConfig.initialSize());
        assertEquals(10, poolConfig.maxSize());

        // Enabled cipher suites and protocols are arrays and don't define equals, so they need to be removed before env can be compared to expected
        String[] enabledCipherSuites = assertInstanceOf(String[].class, env.remove("enabledCipherSuites"));
        assertArrayEquals(new String[] { "X", "Y", "Z" }, enabledCipherSuites);
        String[] enabledProtocols = assertInstanceOf(String[].class, env.remove("enabledProtocols"));
        assertArrayEquals(new String[] { "A", "B", "C" }, enabledProtocols);

        assertEquals(expected, env);
    }

    private static final class TestKeyManager implements KeyManager {
        // no content
    }

    private static final class TestTrustManager implements TrustManager {
        // no content
    }

    private static final class TestHostnameVerifier implements HostnameVerifier {

        @Override
        public boolean verify(String hostname, SSLSession session) {
            return false;
        }
    }

    @Nested
    @DisplayName("overrides all setters")
    class OverridesAllSetters implements CovariantReturnTests<FTPSEnvironment> {

        @Override
        public Class<FTPSEnvironment> objectType() {
            return FTPSEnvironment.class;
        }
    }

    @Nested
    class InitializePreConnectTest {

        @Nested
        class AuthCommandTest {

            @Test
            void testAuthCommandNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setAuthValue(anyString());
            }

            @Test
            void testAuthCommandSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withAuthCommand("command");
                env.initializePreConnect(client);

                verify(client).setAuthValue("command");
            }

            @Test
            void testAuthCommandSetToNull() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withAuthCommand(null);
                env.initializePreConnect(client);

                verify(client).setAuthValue(null);
            }
        }

        @Nested
        class KeyManagerTest {

            @Test
            void testKeyManagerNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setKeyManager(any());
            }

            @Test
            void testKeyManagerSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                KeyManager keyManager = mock(KeyManager.class);

                FTPSEnvironment env = new FTPSEnvironment().withKeyManager(keyManager);
                env.initializePreConnect(client);

                verify(client).setKeyManager(keyManager);
            }

            @Test
            void testKeyManagerSetToNull() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withKeyManager(null);
                env.initializePreConnect(client);

                verify(client).setKeyManager(null);
            }
        }

        @Nested
        class TrustManagerTest {

            @Test
            void testTrustManagerNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setTrustManager(any());
            }

            @Test
            void testTrustManagerSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                TrustManager trustManager = mock(TrustManager.class);

                FTPSEnvironment env = new FTPSEnvironment().withTrustManager(trustManager);
                env.initializePreConnect(client);

                verify(client).setTrustManager(trustManager);
            }

            @Test
            void testTrustManagerSetToNull() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withTrustManager(null);
                env.initializePreConnect(client);

                verify(client).setTrustManager(null);
            }
        }

        @Nested
        class HostnameVerifierTest {

            @Test
            void testHostnameVerifierNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setHostnameVerifier(any());
            }

            @Test
            void testHostnameVerifierSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                HostnameVerifier hostnameVerifier = mock(HostnameVerifier.class);

                FTPSEnvironment env = new FTPSEnvironment().withHostnameVerifier(hostnameVerifier);
                env.initializePreConnect(client);

                verify(client).setHostnameVerifier(hostnameVerifier);
            }

            @Test
            void testHostnameVerifierSetToNull() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withHostnameVerifier(null);
                env.initializePreConnect(client);

                verify(client).setHostnameVerifier(null);
            }
        }

        @Nested
        class EndpointCheckingEnabledTest {

            @Test
            void testEndpointCheckingEnabledNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setEndpointCheckingEnabled(anyBoolean());
            }

            @Test
            void testEndpointCheckingEnabledSetToTrue() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withEndpointCheckingEnabled(true);
                env.initializePreConnect(client);

                verify(client).setEndpointCheckingEnabled(true);
            }

            @Test
            void testEndpointCheckingEnabledSetToFalse() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withEndpointCheckingEnabled(false);
                env.initializePreConnect(client);

                verify(client).setEndpointCheckingEnabled(false);
            }
        }

        @Nested
        class EnabledSessionCreationTest {

            @Test
            void testEnabledSessionCreationNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setEnabledSessionCreation(anyBoolean());
            }

            @Test
            void testEnabledSessionCreationSetToTrue() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withEnabledSessionCreation(true);
                env.initializePreConnect(client);

                verify(client).setEnabledSessionCreation(true);
            }

            @Test
            void testEnabledSessionCreationSetToFalse() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withEnabledSessionCreation(false);
                env.initializePreConnect(client);

                verify(client).setEnabledSessionCreation(false);
            }
        }

        @Nested
        class NeedClientAuthTest {

            @Test
            void testNeedClientAuthNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setNeedClientAuth(anyBoolean());
            }

            @Test
            void testNeedClientAuthSetToTrue() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withNeedClientAuth(true);
                env.initializePreConnect(client);

                verify(client).setNeedClientAuth(true);
            }

            @Test
            void testNeedClientAuthSetToFalse() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withNeedClientAuth(false);
                env.initializePreConnect(client);

                verify(client).setNeedClientAuth(false);
            }
        }

        @Nested
        class WantClientAuthTest {

            @Test
            void testWantClientAuthNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setWantClientAuth(anyBoolean());
            }

            @Test
            void testWantClientAuthSetToTrue() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withWantClientAuth(true);
                env.initializePreConnect(client);

                verify(client).setWantClientAuth(true);
            }

            @Test
            void testWantClientAuthSetToFalse() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withWantClientAuth(false);
                env.initializePreConnect(client);

                verify(client).setWantClientAuth(false);
            }
        }

        @Nested
        class UseClientModeTest {

            @Test
            void testUseClientModeNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setUseClientMode(anyBoolean());
            }

            @Test
            void testUseClientModeSetToTrue() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withUseClientMode(true);
                env.initializePreConnect(client);

                verify(client).setUseClientMode(true);
            }

            @Test
            void testUseClientModeSetToFalse() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withUseClientMode(false);
                env.initializePreConnect(client);

                verify(client).setUseClientMode(false);
            }
        }

        @Nested
        class EnabledCipherSuitesTest {

            @Test
            void testEnabledCipherSuitesNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setEnabledCipherSuites(any());
            }

            @Test
            void testEnabledCipherSuitesSetToTrue() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withEnabledCipherSuites("1", "2", "3");
                env.initializePreConnect(client);

                verify(client).setEnabledCipherSuites(new String[] { "1", "2", "3", });
            }

            @Test
            void testEnabledCipherSuitesSetToNull() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withEnabledCipherSuites((String[]) null);
                env.initializePreConnect(client);

                verify(client).setEnabledCipherSuites(null);
            }
        }

        @Nested
        class EnabledProtocolsTest {

            @Test
            void testEnabledProtocolsNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePreConnect(client);

                verify(client, never()).setEnabledProtocols(any());
            }

            @Test
            void testEnabledProtocolsSetToTrue() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withEnabledProtocols("1", "2", "3");
                env.initializePreConnect(client);

                verify(client).setEnabledProtocols(new String[] { "1", "2", "3", });
            }

            @Test
            void testEnabledProtocolsSetToNull() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withEnabledProtocols((String[]) null);
                env.initializePreConnect(client);

                verify(client).setEnabledProtocols(null);
            }
        }
    }

    @Nested
    class ConnectTest {
        // added to skip inherited connect tests
    }

    @Nested
    class InitializePostConnectTest {

        @Nested
        class DataChannelProtectionLevelTest {

            @Test
            void testDataChannelProtectionLevelNotSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment();
                env.initializePostConnect(client);

                verify(client, never()).execPBSZ(anyLong());
                verify(client, never()).execPROT(anyString());
            }

            @Test
            void testDataChannelProtectionLevelSet() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withDataChannelProtectionLevel(DataChannelProtectionLevel.PRIVATE);
                env.initializePostConnect(client);

                verify(client).execPBSZ(0);
                verify(client).execPROT("P");
            }

            @Test
            void testDataChannelProtectionLevelSetToNull() throws IOException {
                FTPSClient client = mock(FTPSClient.class);

                FTPSEnvironment env = new FTPSEnvironment().withDataChannelProtectionLevel(null);
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> env.initializePostConnect(client));
                assertChainEquals(Messages.fileSystemProvider().env().missingProperty("dataChannelProtectionLevel"), exception);

                verify(client, never()).execPBSZ(anyLong());
                verify(client, never()).execPROT(anyString());
            }
        }
    }

    @Nested
    class LoginTest {
        // added to skip inherited login tests
    }

    @Nested
    class InitializePostLoginTest {
        // added to skip inherited post login tests
    }
}
