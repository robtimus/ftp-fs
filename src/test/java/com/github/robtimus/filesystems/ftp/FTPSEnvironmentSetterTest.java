/*
 * FTPSEnvironmentSetterTest.java
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
import java.lang.reflect.Method;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SuppressWarnings({ "nls", "javadoc" })
public class FTPSEnvironmentSetterTest {

    private final Method setter;
    private final String propertyName;
    private final Object propertyValue;

    public FTPSEnvironmentSetterTest(String methodName, String propertyName, Object propertyValue) {
        this.setter = findMethod(methodName);
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    private Method findMethod(String methodName) {
        for (Method method : FTPSEnvironment.class.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("Could not find method " + methodName);
    }

    @Parameters(name = "{0}")
    public static List<Object[]> getParameters() throws NoSuchAlgorithmException {
        Object[][] parameters = {
                { "withSecurityMode", "securityMode", SecurityMode.EXPLICIT, },
                { "withSSLContext", "sslContext", SSLContext.getDefault(), },
                { "withProtocol", "protocol", "tls", },
                { "withAuthCommand", "authCommand", "auth", },
                { "withKeyManager", "keyManager", new TestKeyManager(), },
                { "withTrustManager", "trustManager", new TestTrustManager(), },
                { "withHostnameVerifier", "hostnameVerifier", new TestHostnameVerifier(), },
                { "withEndpointCheckingEnabled", "endpointCheckingEnabled", true, },
                { "withEnabledSessionCreation", "enabledSessionCreation", true, },
                { "withNeedClientAuth", "needClientAuth", false, },
                { "withWantClientAuth", "wantClientAuth", false, },
                { "withUseClientMode", "useClientMode", true, },
                { "withEnabledCipherSuites", "enabledCipherSuites", new String[] { "suite1", "suite2", }, },
                { "withEnabledProtocols", "enabledProtocols", new String[] { "protocol1", "protocol2", }, },
        };
        return Arrays.asList(parameters);
    }

    @Test
    public void testSetter() throws ReflectiveOperationException {
        FTPSEnvironment env = new FTPSEnvironment();

        assertEquals(Collections.emptyMap(), env);

        setter.invoke(env, propertyValue);

        assertEquals(Collections.singletonMap(propertyName, propertyValue), env);
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
}
