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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
        };
        return Stream.concat(super.findSetters(), Arrays.stream(arguments));
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

    static Stream<Arguments> findMethodsToOverride() {
        List<Arguments> arguments = new ArrayList<>();
        for (Method method : FTPEnvironment.class.getMethods()) {
            if (method.getReturnType() == FTPEnvironment.class) {
                arguments.add(arguments(method));
            }
        }
        return arguments.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("findMethodsToOverride")
    void testMethodOverride(Method parentMethod) throws NoSuchMethodException {
        Method method = FTPSEnvironment.class.getMethod(parentMethod.getName(), parentMethod.getParameterTypes());
        assertEquals(FTPSEnvironment.class, method.getReturnType());
    }
}
