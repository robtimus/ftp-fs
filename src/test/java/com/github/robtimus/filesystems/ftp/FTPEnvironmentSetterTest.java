/*
 * FTPEnvironmentSetterTest.java
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
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.parser.DefaultFTPFileEntryParserFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
@SuppressWarnings({ "nls", "javadoc" })
public class FTPEnvironmentSetterTest {

    private final Method setter;
    private final String propertyName;
    private final Object propertyValue;

    public FTPEnvironmentSetterTest(String methodName, String propertyName, Object propertyValue) {
        this.setter = findMethod(methodName);
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    private Method findMethod(String methodName) {
        for (Method method : FTPEnvironment.class.getMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("Could not find method " + methodName);
    }

    @Parameters(name = "{0}")
    public static List<Object[]> getParameters() {
        Object[][] parameters = {
            { "withSoTimeout", "soTimeout", 1000, },
            { "withSendBufferSize", "sendBufferSize", 4096, },
            { "withReceiveBufferSize", "receiveBufferSize", 2048, },
            { "withTcpNoDelay", "tcpNoDelay", true, },
            { "withKeepAlive", "keepAlive", true, },
            { "withSocketFactory", "socketFactory", SocketFactory.getDefault(), },
            { "withServerSocketFactory", "serverSocketFactory", ServerSocketFactory.getDefault(), },
            { "withConnectTimeout", "connectTimeout", 1000, },
            { "withProxy", "proxy", new Proxy(Proxy.Type.HTTP, new InetSocketAddress("localhost", 21)), },
            { "withCharset", "charset", StandardCharsets.UTF_8, },
            { "withControlEncoding", "controlEncoding", "UTF-8", },
            { "withStrictlyMultilineParsing", "strictMultilineParsing", true, },
            { "withDataTimeout", "dataTimeout", 1000, },
            { "withParserFactory", "parserFactory", new DefaultFTPFileEntryParserFactory(), },
            { "withRemoteVerificationEnabled", "remoteVerificationEnabled", true, },
            { "withDefaultDirectory", "defaultDir", "/", },
            { "withConnectionMode", "connectionMode", ConnectionMode.PASSIVE, },
            { "withActiveExternalIPAddress", "activeExternalIPAddress", "127.0.0.1", },
            { "withPassiveLocalIPAddress", "passiveLocalIPAddress", "127.0.0.1", },
            { "withReportActiveExternalIPAddress", "reportActiveExternalIPAddress", "127.0.0.1", },
            { "withBufferSize", "bufferSize", 1000, },
            { "withSendDataSocketBufferSize", "sendDataSocketBufferSize", 1024, },
            { "withReceiveDataSocketBufferSize", "receiveDataSocketBufferSize", 2048, },
            { "withClientConfig", "clientConfig", new FTPClientConfig(), },
            { "withUseEPSVwithIPv4", "useEPSVwithIPv4", true, },
            { "withControlKeepAliveTimeout", "controlKeepAliveTimeout", 1000L, },
            { "withControlKeepAliveReplyTimeout", "controlKeepAliveReplyTimeout", 1000, },
            { "withPassiveNatWorkaround", "passiveNatWorkaround", false, },
            { "withPassiveNatWorkaroundStrategy", "passiveNatWorkaroundStrategy", new FTPClient.NatServerResolverImpl(new FTPClient()), },
            { "withAutodetectEncoding", "autodetectEncoding", true, },
            { "withClientConnectionCount", "clientConnectionCount", 5, },
            { "withFileSystemExceptionFactory", "fileSystemExceptionFactory", DefaultFileSystemExceptionFactory.INSTANCE, },
            { "withActualTotalSpaceCalculation", "calculateActualTotalSpace", true, },
        };
        return Arrays.asList(parameters);
    }

    @Test
    public void testSetter() throws ReflectiveOperationException {
        FTPEnvironment env = new FTPEnvironment();

        assertEquals(Collections.emptyMap(), env);

        setter.invoke(env, propertyValue);

        assertEquals(Collections.singletonMap(propertyName, propertyValue), env);
    }
}
