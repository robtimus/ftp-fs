/*
 * FTPSEnvironment.java
 * Copyright 2016 Rob Spoor
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.Proxy;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.security.SecureRandom;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import org.apache.commons.net.ftp.FTPClient.HostnameResolver;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPSClient;
import org.apache.commons.net.ftp.parser.FTPFileEntryParserFactory;
import com.github.robtimus.filesystems.FileSystemProviderSupport;

/**
 * A utility class to set up environments that can be used in the {@link FileSystemProvider#newFileSystem(URI, Map)} and
 * {@link FileSystemProvider#newFileSystem(Path, Map)} methods of {@link FTPSFileSystemProvider}.
 *
 * @author Rob Spoor
 */
public class FTPSEnvironment extends FTPEnvironment {

    private static final String SECURITY_MODE = "securityMode"; //$NON-NLS-1$
    private static final String SSL_CONTEXT = "sslContext"; //$NON-NLS-1$
    private static final String PROTOCOL = "protocol"; //$NON-NLS-1$
    private static final String AUTH_COMMAND = "authCommand"; //$NON-NLS-1$
    private static final String KEY_MANAGER = "keyManager"; //$NON-NLS-1$
    private static final String TRUST_MANAGER = "trustManager"; //$NON-NLS-1$
    private static final String HOSTNAME_VERIFIER = "hostnameVerifier"; //$NON-NLS-1$
    private static final String ENDPOINT_CHECKING_ENABLED = "endpointCheckingEnabled"; //$NON-NLS-1$
    private static final String ENABLED_SESSION_CREATION = "enabledSessionCreation"; //$NON-NLS-1$
    private static final String NEED_CLIENT_AUTH = "needClientAuth"; //$NON-NLS-1$
    private static final String WANT_CLIENT_AUTH = "wantClientAuth"; //$NON-NLS-1$
    private static final String USE_CLIENT_MODE = "useClientMode"; //$NON-NLS-1$
    private static final String ENABLED_CIPHER_SUITES = "enabledCipherSuites"; //$NON-NLS-1$
    private static final String ENABLED_PROTOCOLS = "enabledProtocols"; //$NON-NLS-1$
    private static final String DATA_CHANNEL_PROTECTION_LEVEL = "dataChannelProtectionLevel"; //$NON-NLS-1$

    /**
     * Creates a new FTPS environment.
     */
    public FTPSEnvironment() {
        super();
    }

    /**
     * Creates a new FTPS environment.
     *
     * @param map The map to wrap.
     */
    public FTPSEnvironment(Map<String, Object> map) {
        super(map);
    }

    @SuppressWarnings("unchecked")
    static FTPSEnvironment wrap(Map<String, ?> map) {
        if (map instanceof FTPSEnvironment) {
            return (FTPSEnvironment) map;
        }
        return new FTPSEnvironment((Map<String, Object>) map);
    }

    @Override
    public FTPSEnvironment withLocalAddress(InetAddress localAddr, int localPort) {
        super.withLocalAddress(localAddr, localPort);
        return this;
    }

    @Override
    public FTPSEnvironment withCredentials(String username, char[] password) {
        super.withCredentials(username, password);
        return this;
    }

    @Override
    public FTPSEnvironment withCredentials(String username, char[] password, String account) {
        super.withCredentials(username, password, account);
        return this;
    }

    @Override
    public FTPSEnvironment withSoTimeout(int timeout) {
        super.withSoTimeout(timeout);
        return this;
    }

    @Override
    public FTPSEnvironment withSendBufferSize(int size) {
        super.withSendBufferSize(size);
        return this;
    }

    @Override
    public FTPSEnvironment withReceiveBufferSize(int size) {
        super.withReceiveBufferSize(size);
        return this;
    }

    @Override
    public FTPSEnvironment withTcpNoDelay(boolean on) {
        super.withTcpNoDelay(on);
        return this;
    }

    @Override
    public FTPSEnvironment withKeepAlive(boolean keepAlive) {
        super.withKeepAlive(keepAlive);
        return this;
    }

    @Override
    public FTPSEnvironment withSoLinger(boolean on, int linger) {
        super.withSoLinger(on, linger);
        return this;
    }

    @Override
    public FTPSEnvironment withSocketFactory(SocketFactory factory) {
        super.withSocketFactory(factory);
        return this;
    }

    @Override
    public FTPSEnvironment withServerSocketFactory(ServerSocketFactory factory) {
        super.withServerSocketFactory(factory);
        return this;
    }

    @Override
    public FTPSEnvironment withConnectTimeout(int timeout) {
        super.withConnectTimeout(timeout);
        return this;
    }

    @Override
    public FTPSEnvironment withProxy(Proxy proxy) {
        super.withProxy(proxy);
        return this;
    }

    @Override
    public FTPSEnvironment withCharset(Charset charset) {
        super.withCharset(charset);
        return this;
    }

    @Override
    public FTPSEnvironment withControlEncoding(String encoding) {
        super.withControlEncoding(encoding);
        return this;
    }

    /**
     * {@inheritDoc}
     * @deprecated This method is named incorrectly. Use {@link #withStrictMultilineParsing(boolean)} instead.
     */
    @Override
    @Deprecated
    public FTPSEnvironment withStrictlyMultilineParsing(boolean strictMultilineParsing) {
        super.withStrictlyMultilineParsing(strictMultilineParsing);
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 2.1
     */
    @Override
    public FTPSEnvironment withStrictMultilineParsing(boolean strictMultilineParsing) {
        super.withStrictMultilineParsing(strictMultilineParsing);
        return this;
    }

    @Override
    public FTPSEnvironment withDataTimeout(int timeout) {
        super.withDataTimeout(timeout);
        return this;
    }

    @Override
    public FTPSEnvironment withParserFactory(FTPFileEntryParserFactory parserFactory) {
        super.withParserFactory(parserFactory);
        return this;
    }

    @Override
    public FTPSEnvironment withRemoteVerificationEnabled(boolean enabled) {
        super.withRemoteVerificationEnabled(enabled);
        return this;
    }

    @Override
    public FTPSEnvironment withDefaultDirectory(String pathname) {
        super.withDefaultDirectory(pathname);
        return this;
    }

    @Override
    public FTPSEnvironment withConnectionMode(ConnectionMode connectionMode) {
        super.withConnectionMode(connectionMode);
        return this;
    }

    @Override
    public FTPSEnvironment withActivePortRange(int minPort, int maxPort) {
        super.withActivePortRange(minPort, maxPort);
        return this;
    }

    @Override
    public FTPSEnvironment withActiveExternalIPAddress(String ipAddress) {
        super.withActiveExternalIPAddress(ipAddress);
        return this;
    }

    @Override
    public FTPSEnvironment withPassiveLocalIPAddress(String ipAddress) {
        super.withPassiveLocalIPAddress(ipAddress);
        return this;
    }

    @Override
    public FTPSEnvironment withReportActiveExternalIPAddress(String ipAddress) {
        super.withReportActiveExternalIPAddress(ipAddress);
        return this;
    }

    @Override
    public FTPSEnvironment withBufferSize(int bufferSize) {
        super.withBufferSize(bufferSize);
        return this;
    }

    @Override
    public FTPSEnvironment withSendDataSocketBufferSize(int bufferSizr) {
        super.withSendDataSocketBufferSize(bufferSizr);
        return this;
    }

    @Override
    public FTPSEnvironment withReceiveDataSocketBufferSize(int bufferSize) {
        super.withReceiveDataSocketBufferSize(bufferSize);
        return this;
    }

    @Override
    public FTPSEnvironment withClientConfig(FTPClientConfig clientConfig) {
        super.withClientConfig(clientConfig);
        return this;
    }

    @Override
    public FTPSEnvironment withUseEPSVwithIPv4(boolean selected) {
        super.withUseEPSVwithIPv4(selected);
        return this;
    }

    @Override
    public FTPSEnvironment withControlKeepAliveTimeout(long timeout) {
        super.withControlKeepAliveTimeout(timeout);
        return this;
    }

    @Override
    public FTPSEnvironment withControlKeepAliveReplyTimeout(int timeout) {
        super.withControlKeepAliveReplyTimeout(timeout);
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 1.1
     */
    @Override
    public FTPSEnvironment withPassiveNatWorkaroundStrategy(HostnameResolver resolver) {
        super.withPassiveNatWorkaroundStrategy(resolver);
        return this;
    }

    @Override
    public FTPSEnvironment withAutodetectEncoding(boolean autodetect) {
        super.withAutodetectEncoding(autodetect);
        return this;
    }

    /**
     * {@inheritDoc}
     * @since 2.0
     */
    @Override
    public FTPSEnvironment withListHiddenFiles(boolean listHiddenFiles) {
        super.withListHiddenFiles(listHiddenFiles);
        return this;
    }

    @Override
    public FTPSEnvironment withClientConnectionCount(int count) {
        super.withClientConnectionCount(count);
        return this;
    }

    @Override
    public FTPSEnvironment withClientConnectionWaitTimeout(long timeout) {
        super.withClientConnectionWaitTimeout(timeout);
        return this;
    }

    @Override
    public FTPSEnvironment withClientConnectionWaitTimeout(long duration, TimeUnit unit) {
        super.withClientConnectionWaitTimeout(duration, unit);
        return this;
    }

    @Override
    public FTPSEnvironment withFileSystemExceptionFactory(FileSystemExceptionFactory factory) {
        super.withFileSystemExceptionFactory(factory);
        return this;
    }

    @Override
    public FTPSEnvironment withFTPFileStrategyFactory(FTPFileStrategyFactory factory) {
        super.withFTPFileStrategyFactory(factory);
        return this;
    }

    /**
     * Stores the security mode to use.
     * If the security mode is not set, it will default to {@link SecurityMode#EXPLICIT}.
     *
     * @param securityMode The security mode to use.
     * @return This object.
     */
    public FTPSEnvironment withSecurityMode(SecurityMode securityMode) {
        put(SECURITY_MODE, securityMode);
        return this;
    }

    /**
     * Stores the SSL context to use.
     * If the SSL context is not set, the {@link #withProtocol(String) protocol} will be used instead.
     *
     * @param sslContext The SSL context to use.
     * @return This object.
     */
    public FTPSEnvironment withSSLContext(SSLContext sslContext) {
        put(SSL_CONTEXT, sslContext);
        return this;
    }

    /**
     * Stores the protocol to use.
     * If the protocol is not set, it will default to {@code TLS}.
     * The protocol will be ignored if an {@link #withSSLContext(SSLContext) SSLContext} is stored.
     *
     * @param protocol The protocol to use.
     * @return This object.
     */
    public FTPSEnvironment withProtocol(String protocol) {
        put(PROTOCOL, protocol);
        return this;
    }

    /**
     * Stores the AUTH command to use.
     *
     * @param command The AUTH command to use.
     * @return This object.
     */
    public FTPSEnvironment withAuthCommand(String command) {
        put(AUTH_COMMAND, command);
        return this;
    }

    /**
     * Stores the key manager to use.
     *
     * @param keyManager The key manager to use.
     * @return This object.
     * @see SSLContext#init(KeyManager[], TrustManager[], SecureRandom)
     */
    public FTPSEnvironment withKeyManager(KeyManager keyManager) {
        put(KEY_MANAGER, keyManager);
        return this;
    }

    /**
     * Stores the trust manager to use.
     *
     * @param trustManager The trust manager to use.
     * @return This object.
     * @see SSLContext#init(KeyManager[], TrustManager[], SecureRandom)
     */
    public FTPSEnvironment withTrustManager(TrustManager trustManager) {
        put(TRUST_MANAGER, trustManager);
        return this;
    }

    /**
     * Stores the hostname verifier to use.
     * The verifier is only used on {@link #withUseClientMode(boolean) client mode} connections.
     *
     * @param hostnameVerifier The hostname verifier to use.
     * @return This object.
     */
    public FTPSEnvironment withHostnameVerifier(HostnameVerifier hostnameVerifier) {
        put(HOSTNAME_VERIFIER, hostnameVerifier);
        return this;
    }

    /**
     * Stores whether or not endpoint identification using the HTTPS algorithm should be enabled.
     * The default behaviour is for this to be disabled.
     * This check is only performed on {@link #withUseClientMode(boolean) client mode} connections.
     *
     * @param enabled {@code true} if endpoint identification should be enabled, or {@code false} if it should be disabled.
     * @return This object.
     * @see SSLSocket#setSSLParameters(SSLParameters)
     * @see SSLParameters#setEndpointIdentificationAlgorithm(String)
     */
    public FTPSEnvironment withEndpointCheckingEnabled(boolean enabled) {
        put(ENDPOINT_CHECKING_ENABLED, enabled);
        return this;
    }

    /**
     * Stores whether or not new SSL sessions may be established by sockets.
     *
     * @param established The established socket flag.
     * @return This object.
     * @see SSLSocket#setEnableSessionCreation(boolean)
     */
    public FTPSEnvironment withEnabledSessionCreation(boolean established) {
        put(ENABLED_SESSION_CREATION, established);
        return this;
    }

    /**
     * Stores whether or not sockets will require client authentication.
     *
     * @param needClientAuth The need client authentication flag.
     * @return This object.
     * @see SSLSocket#setNeedClientAuth(boolean)
     */
    public FTPSEnvironment withNeedClientAuth(boolean needClientAuth) {
        put(NEED_CLIENT_AUTH, needClientAuth);
        return this;
    }

    /**
     * Stores whether or not sockets will request client authentication.
     *
     * @param wantClientAuth The want client authentication flag.
     * @return This object.
     * @see SSLSocket#setWantClientAuth(boolean)
     */
    public FTPSEnvironment withWantClientAuth(boolean wantClientAuth) {
        put(WANT_CLIENT_AUTH, wantClientAuth);
        return this;
    }

    /**
     * Stores whether or not sockets are set to use client mode in their first handshake.
     *
     * @param useClientMode The use client mode flag.
     * @return This object.
     * @see SSLSocket#setUseClientMode(boolean)
     */
    public FTPSEnvironment withUseClientMode(boolean useClientMode) {
        put(USE_CLIENT_MODE, useClientMode);
        return this;
    }

    /**
     * Stores the names of the cipher suites which could be enabled for use on connections.
     *
     * @param cipherSuites The names of the cipher suites to use.
     * @return This object.
     * @see SSLSocket#setEnabledCipherSuites(String[])
     */
    public FTPSEnvironment withEnabledCipherSuites(String... cipherSuites) {
        put(ENABLED_CIPHER_SUITES, cipherSuites);
        return this;
    }

    /**
     * Stores which particular protocol versions are enabled for use on connections.
     *
     * @param protocolVersions The protocol versions to use.
     * @return This object.
     * @see SSLSocket#setEnabledProtocols(String[])
     */
    public FTPSEnvironment withEnabledProtocols(String... protocolVersions) {
        put(ENABLED_PROTOCOLS, protocolVersions);
        return this;
    }

    /**
     * Stores the data channel protection level to use.
     *
     * @param dataChannelProtectionLevel The data channel protection level to use.
     * @return This object.
     * @see SSLSocket#setUseClientMode(boolean)
     * @since 2.2
     */
    public FTPSEnvironment withDataChannelProtectionLevel(DataChannelProtectionLevel dataChannelProtectionLevel) {
        put(DATA_CHANNEL_PROTECTION_LEVEL, dataChannelProtectionLevel);
        return this;
    }

    @Override
    FTPSClient createClient(String hostname, int port) throws IOException {
        SecurityMode securityMode = FileSystemProviderSupport.getValue(this, SECURITY_MODE, SecurityMode.class, SecurityMode.EXPLICIT);
        boolean isImplicit = securityMode.isImplicit;
        SSLContext context = FileSystemProviderSupport.getValue(this, SSL_CONTEXT, SSLContext.class, null);

        FTPSClient client;
        if (context == null) {
            String protocol = FileSystemProviderSupport.getValue(this, PROTOCOL, String.class, null);
            if (protocol == null) {
                client = new FTPSClient(isImplicit);
            } else {
                client = new FTPSClient(protocol, isImplicit);
            }
        } else {
            client = new FTPSClient(isImplicit, context);
        }
        initializePreConnect(client);
        connect(client, hostname, port);
        initializePostConnect(client);
        login(client);
        initializePostLogin(client);
        verifyConnection(client);
        return client;
    }

    void initializePreConnect(FTPSClient client) throws IOException {
        super.initializePreConnect(client);

        if (containsKey(AUTH_COMMAND)) {
            String auth = FileSystemProviderSupport.getValue(this, AUTH_COMMAND, String.class, null);
            client.setAuthValue(auth);
        }
        if (containsKey(KEY_MANAGER)) {
            KeyManager keyManager = FileSystemProviderSupport.getValue(this, KEY_MANAGER, KeyManager.class, null);
            client.setKeyManager(keyManager);
        }
        if (containsKey(TRUST_MANAGER)) {
            TrustManager trustManager = FileSystemProviderSupport.getValue(this, TRUST_MANAGER, TrustManager.class, null);
            client.setTrustManager(trustManager);
        }
        if (containsKey(HOSTNAME_VERIFIER)) {
            HostnameVerifier hostnameVerifier = FileSystemProviderSupport.getValue(this, HOSTNAME_VERIFIER, HostnameVerifier.class, null);
            client.setHostnameVerifier(hostnameVerifier);
        }

        if (containsKey(ENDPOINT_CHECKING_ENABLED)) {
            boolean enable = FileSystemProviderSupport.getBooleanValue(this, ENDPOINT_CHECKING_ENABLED);
            client.setEndpointCheckingEnabled(enable);
        }
        if (containsKey(ENABLED_SESSION_CREATION)) {
            boolean isCreation = FileSystemProviderSupport.getBooleanValue(this, ENABLED_SESSION_CREATION);
            client.setEnabledSessionCreation(isCreation);
        }
        if (containsKey(NEED_CLIENT_AUTH)) {
            boolean isNeedClientAuth = FileSystemProviderSupport.getBooleanValue(this, NEED_CLIENT_AUTH);
            client.setNeedClientAuth(isNeedClientAuth);
        }
        if (containsKey(WANT_CLIENT_AUTH)) {
            boolean isWantClientAuth = FileSystemProviderSupport.getBooleanValue(this, WANT_CLIENT_AUTH);
            client.setWantClientAuth(isWantClientAuth);
        }
        if (containsKey(USE_CLIENT_MODE)) {
            boolean isClientMode = FileSystemProviderSupport.getBooleanValue(this, USE_CLIENT_MODE);
            client.setUseClientMode(isClientMode);
        }

        if (containsKey(ENABLED_CIPHER_SUITES)) {
            String[] cipherSuites = FileSystemProviderSupport.getValue(this, ENABLED_CIPHER_SUITES, String[].class, null);
            client.setEnabledCipherSuites(cipherSuites);
        }

        if (containsKey(ENABLED_PROTOCOLS)) {
            String[] protocolVersions = FileSystemProviderSupport.getValue(this, ENABLED_PROTOCOLS, String[].class, null);
            client.setEnabledProtocols(protocolVersions);
        }
    }

    void initializePostConnect(FTPSClient client) throws IOException {
        super.initializePostConnect(client);

        if (containsKey(DATA_CHANNEL_PROTECTION_LEVEL)) {
            DataChannelProtectionLevel dataChannelProtectionLevel
                    = FileSystemProviderSupport.getValue(this, DATA_CHANNEL_PROTECTION_LEVEL, DataChannelProtectionLevel.class);
            // 0 means streaming, see https://tools.ietf.org/html/rfc4217#section-9
            client.execPBSZ(0);
            dataChannelProtectionLevel.apply(client);
        }
    }

    @Override
    public FTPSEnvironment clone() {
        return (FTPSEnvironment) super.clone();
    }
}
