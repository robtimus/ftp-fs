/*
 * FTPEnvironment.java
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
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ServerSocketFactory;
import javax.net.SocketFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClient.HostnameResolver;
import org.apache.commons.net.ftp.FTPClient.NatServerResolverImpl;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPFileEntryParser;
import org.apache.commons.net.ftp.parser.FTPFileEntryParserFactory;
import com.github.robtimus.filesystems.FileSystemProviderSupport;

/**
 * A utility class to set up environments that can be used in the {@link FileSystemProvider#newFileSystem(URI, Map)} and
 * {@link FileSystemProvider#newFileSystem(Path, Map)} methods of {@link FTPFileSystemProvider}.
 *
 * @author Rob Spoor
 */
public class FTPEnvironment implements Map<String, Object> {

    private static final AtomicReference<FTPEnvironment> DEFAULTS = new AtomicReference<>();

    // connect support

    private static final String LOCAL_ADDR = "localAddr"; //$NON-NLS-1$
    private static final String LOCAL_PORT = "localPort"; //$NON-NLS-1$

    // login support

    private static final String USERNAME = "username"; //$NON-NLS-1$
    private static final String PASSWORD = "password"; //$NON-NLS-1$
    private static final String ACCOUNT = "account"; //$NON-NLS-1$

    // SocketClient

    private static final String SO_TIMEOUT = "soTimeout"; //$NON-NLS-1$
    private static final String SEND_BUFFER_SIZE = "sendBufferSize"; //$NON-NLS-1$
    private static final String RECEIVE_BUFFER_SIZE = "receiveBufferSize"; //$NON-NLS-1$
    private static final String TCP_NO_DELAY = "tcpNoDelay"; //$NON-NLS-1$
    private static final String KEEP_ALIVE = "keepAlive"; //$NON-NLS-1$
    private static final String SO_LINGER_ON = "soLinger.on"; //$NON-NLS-1$
    private static final String SO_LINGER_VALUE = "soLinger.val"; //$NON-NLS-1$
    private static final String SOCKET_FACTORY = "socketFactory"; //$NON-NLS-1$
    private static final String SERVER_SOCKET_FACTORY = "serverSocketFactory"; //$NON-NLS-1$
    private static final String CONNECT_TIMEOUT = "connectTimeout"; //$NON-NLS-1$
    private static final String PROXY = "proxy"; //$NON-NLS-1$
    private static final String CHARSET = "charset"; //$NON-NLS-1$
    private static final int DEFAULT_CONNECT_TIMEOUT = 30 * 1000; // 30 seconds

    // FTP

    private static final String CONTROL_ENCODING = "controlEncoding"; //$NON-NLS-1$
    private static final String STRICT_MULTILINE_PARSING = "strictMultilineParsing"; //$NON-NLS-1$

    // FTPClient

    private static final String DATA_TIMEOUT = "dataTimeout"; //$NON-NLS-1$
    private static final String PARSER_FACTORY = "parserFactory"; //$NON-NLS-1$
    private static final String IP_ADDRESS_FROM_PASV_RESPONSE = "ipAddressFromPasvResponse"; //$NON-NLS-1$
    private static final String REMOTE_VERIFICATION_ENABLED = "remoteVerificationEnabled"; //$NON-NLS-1$
    private static final String DEFAULT_DIR = "defaultDir"; //$NON-NLS-1$
    private static final String CONNECTION_MODE = "connectionMode"; //$NON-NLS-1$
    private static final String ACTIVE_PORT_RANGE_MIN = "activePortRange.min"; //$NON-NLS-1$
    private static final String ACTIVE_PORT_RANGE_MAX = "activePortRange.max"; //$NON-NLS-1$
    private static final String ACTIVE_EXTERNAL_IP_ADDRESS = "activeExternalIPAddress"; //$NON-NLS-1$
    private static final String PASSIVE_LOCAL_IP_ADDRESS = "passiveLocalIPAddress"; //$NON-NLS-1$
    private static final String REPORT_ACTIVE_EXTERNAL_IP_ADDRESS = "reportActiveExternalIPAddress"; //$NON-NLS-1$
    private static final String BUFFER_SIZE = "bufferSize"; //$NON-NLS-1$
    private static final String SEND_DATA_SOCKET_BUFFER_SIZE = "sendDataSocketBufferSize"; //$NON-NLS-1$
    private static final String RECEIVE_DATA_SOCKET_BUFFER_SIZE = "receiveDataSocketBufferSize"; //$NON-NLS-1$
    private static final String CLIENT_CONFIG = "clientConfig"; //$NON-NLS-1$
    private static final String USE_EPSV_WITH_IPV4 = "useEPSVwithIPv4"; //$NON-NLS-1$
    private static final String CONTROL_KEEP_ALIVE_TIMEOUT = "controlKeepAliveTimeout"; //$NON-NLS-1$
    private static final String CONTROL_KEEP_ALIVE_REPLY_TIMEOUT = "controlKeepAliveReplyTimeout"; //$NON-NLS-1$
    private static final String PASSIVE_NAT_WORKAROUND_STRATEGY = "passiveNatWorkaroundStrategy"; //$NON-NLS-1$
    private static final String AUTODETECT_ENCODING = "autodetectEncoding"; //$NON-NLS-1$
    private static final String LIST_HIDDEN_FILES = "listHiddenFiles"; //$NON-NLS-1$

    // FTP file system support

    private static final String POOL_CONFIG = "poolConfig"; //$NON-NLS-1$
    private static final String POOL_CONFIG_MAX_WAIT_TIME = POOL_CONFIG + ".maxWaitTime"; //$NON-NLS-1$
    private static final String POOL_CONFIG_MAX_IDLE_TIME = POOL_CONFIG + ".maxIdleTime"; //$NON-NLS-1$
    private static final String POOL_CONFIG_INITIAL_SIZE = POOL_CONFIG + ".initialSize"; //$NON-NLS-1$
    private static final String POOL_CONFIG_MAX_SIZE = POOL_CONFIG + ".maxSize"; //$NON-NLS-1$
    private static final String FILE_SYSTEM_EXCEPTION_FACTORY = "fileSystemExceptionFactory"; //$NON-NLS-1$
    private static final String FTP_FILE_STRATEGY_FACTORY = "ftpFileStrategyFactory"; //$NON-NLS-1$

    private final Map<String, Object> map;

    /**
     * Creates a new FTP environment.
     */
    public FTPEnvironment() {
        map = new HashMap<>();
    }

    /**
     * Creates a new FTP environment.
     *
     * @param map The map to wrap.
     */
    public FTPEnvironment(Map<String, Object> map) {
        this.map = Objects.requireNonNull(map);
    }

    // connect support

    /**
     * Stores the local address to use.
     *
     * @param localAddr The local address to use.
     * @param localPort The local port to use.
     * @return This object.
     * @see Socket#bind(SocketAddress)
     * @see InetSocketAddress#InetSocketAddress(InetAddress, int)
     */
    @QueryParam(LOCAL_ADDR)
    @QueryParam(LOCAL_PORT)
    public FTPEnvironment withLocalAddress(InetAddress localAddr, int localPort) {
        put(LOCAL_ADDR, localAddr);
        put(LOCAL_PORT, localPort);
        return this;
    }

    // login support

    /**
     * Stores the credentials to use.
     *
     * @param username The username to use.
     * @param password The password to use.
     * @return This object.
     */
    public FTPEnvironment withCredentials(String username, char[] password) {
        put(USERNAME, username);
        put(PASSWORD, password);
        return this;
    }

    /**
     * Stores the credentials to use.
     *
     * @param username The username to use.
     * @param password The password to use.
     * @param account The account to use.
     * @return This object.
     */
    @QueryParam(ACCOUNT)
    public FTPEnvironment withCredentials(String username, char[] password, String account) {
        put(USERNAME, username);
        put(PASSWORD, password);
        put(ACCOUNT, account);
        return this;
    }

    // SocketClient

    /**
     * Stores the socket timeout.
     *
     * @param timeout The socket timeout in milliseconds.
     * @return This object.
     * @see Socket#setSoTimeout(int)
     */
    @QueryParam(SO_TIMEOUT)
    public FTPEnvironment withSoTimeout(int timeout) {
        put(SO_TIMEOUT, timeout);
        return this;
    }

    /**
     * Stores the socket send buffer size to use.
     *
     * @param size The size of the buffer in bytes.
     * @return This object.
     * @see Socket#setSendBufferSize(int)
     */
    @QueryParam(SEND_BUFFER_SIZE)
    public FTPEnvironment withSendBufferSize(int size) {
        put(SEND_BUFFER_SIZE, size);
        return this;
    }

    /**
     * Stores the socket receive buffer size to use.
     *
     * @param size The size of the buffer in bytes.
     * @return This object.
     * @see Socket#setReceiveBufferSize(int)
     */
    @QueryParam(RECEIVE_BUFFER_SIZE)
    public FTPEnvironment withReceiveBufferSize(int size) {
        put(RECEIVE_BUFFER_SIZE, size);
        return this;
    }

    /**
     * Stores whether or not the Nagle's algorithm ({@code TCP_NODELAY}) should be enabled.
     *
     * @param on {@code true} if Nagle's algorithm should be enabled, or {@code false} otherwise.
     * @return This object.
     * @see Socket#setTcpNoDelay(boolean)
     */
    @QueryParam(TCP_NO_DELAY)
    public FTPEnvironment withTcpNoDelay(boolean on) {
        put(TCP_NO_DELAY, on);
        return this;
    }

    /**
     * Stores whether or not {@code SO_KEEPALIVE} should be enabled.
     *
     * @param keepAlive {@code true} if keep-alive should be enabled, or {@code false} otherwise.
     * @return This object.
     * @see Socket#setKeepAlive(boolean)
     */
    @QueryParam(KEEP_ALIVE)
    public FTPEnvironment withKeepAlive(boolean keepAlive) {
        put(KEEP_ALIVE, keepAlive);
        return this;
    }

    /**
     * Stores whether or not {@code SO_LINGER} should be enabled, and if so, the linger time.
     *
     * @param on {@code true} if {@code SO_LINGER} should be enabled, or {@code false} otherwise.
     * @param linger The linger time in seconds, if {@code on} is {@code true}.
     * @return This object.
     * @see Socket#setSoLinger(boolean, int)
     */
    @QueryParam(SO_LINGER_ON)
    @QueryParam(SO_LINGER_VALUE)
    public FTPEnvironment withSoLinger(boolean on, int linger) {
        put(SO_LINGER_ON, on);
        put(SO_LINGER_VALUE, linger);
        return this;
    }

    /**
     * Stores the socket factory to use.
     *
     * @param factory The socket factory to use.
     * @return This object.
     */
    public FTPEnvironment withSocketFactory(SocketFactory factory) {
        put(SOCKET_FACTORY, factory);
        return this;
    }

    /**
     * Stores the server socket factory to use.
     *
     * @param factory The server socket factory to use.
     * @return This object.
     */
    public FTPEnvironment withServerSocketFactory(ServerSocketFactory factory) {
        put(SERVER_SOCKET_FACTORY, factory);
        return this;
    }

    /**
     * Stores the connection timeout to use.
     *
     * @param timeout The connection timeout in milliseconds.
     * @return This object.
     * @see Socket#connect(SocketAddress, int)
     */
    @QueryParam(CONNECT_TIMEOUT)
    public FTPEnvironment withConnectTimeout(int timeout) {
        put(CONNECT_TIMEOUT, timeout);
        return this;
    }

    /**
     * Stores the proxy to use.
     *
     * @param proxy The proxy to use.
     * @return This object.
     */
    public FTPEnvironment withProxy(Proxy proxy) {
        put(PROXY, proxy);
        return this;
    }

    /**
     * Stores the charset to use.
     *
     * @param charset The charset to use.
     * @return This object.
     */
    @QueryParam(CHARSET)
    public FTPEnvironment withCharset(Charset charset) {
        put(CHARSET, charset);
        return this;
    }

    // FTP

    /**
     * Stores the character encoding to be used by the FTP control connection.
     * Some FTP servers require that commands be issued in a non-ASCII encoding like UTF-8 so that filenames with multi-byte character
     * representations (e.g, Big 8) can be specified.
     *
     * @param encoding The character encoding to use.
     * @return This object.
     */
    @QueryParam(CONTROL_ENCODING)
    public FTPEnvironment withControlEncoding(String encoding) {
        put(CONTROL_ENCODING, encoding);
        return this;
    }

    /**
     * Stores whether or not strict multiline parsing should be enabled, as per RFC 959, section 4.2.
     *
     * @param strictMultilineParsing {@code true} to enable strict multiline parsing, or {@code false} to disable it.
     * @return This object.
     * @since 2.1
     */
    @QueryParam(STRICT_MULTILINE_PARSING)
    public FTPEnvironment withStrictMultilineParsing(boolean strictMultilineParsing) {
        put(STRICT_MULTILINE_PARSING, strictMultilineParsing);
        return this;
    }

    // FTPClient

    /**
     * Stores the timeout to use when reading from data connections.
     *
     * @param timeout The timeout in milliseconds that is used when opening data connection sockets.
     * @return This object.
     * @since 3.1
     */
    @QueryParam(DATA_TIMEOUT)
    public FTPEnvironment withDataTimeout(Duration timeout) {
        put(DATA_TIMEOUT, timeout);
        return this;
    }

    /**
     * Stores the factory used for parser creation.
     *
     * @param parserFactory The factory object used to create {@link FTPFileEntryParser}s
     * @return This object.
     */
    public FTPEnvironment withParserFactory(FTPFileEntryParserFactory parserFactory) {
        put(PARSER_FACTORY, parserFactory);
        return this;
    }

    /**
     * Sets whether or not the IP address from the server's response should be used. Before version 3.1 (and version 3.9.0 of Apache Commons Net),
     * this has always been the case. Beginning with version 3.1, that IP address will be silently ignored, and replaced with the remote IP address of
     * the control connection, unless this configuration option is given, which restores the old behavior.
     * To enable this by default, use the system property {@link FTPClient#FTP_IP_ADDRESS_FROM_PASV_RESPONSE}.
     *
     * @param usingIpAddressFromPasvResponse {@code true} if the IP address from the server's response should be used,
     *                                           or {@code false} to ignore that IP address.
     * @return This object.
     * @since 3.1
     */
    @QueryParam(IP_ADDRESS_FROM_PASV_RESPONSE)
    public FTPEnvironment withIpAddressFromPasvResponse(boolean usingIpAddressFromPasvResponse) {
        put(IP_ADDRESS_FROM_PASV_RESPONSE, usingIpAddressFromPasvResponse);
        return this;
    }

    /**
     * Stores whether or not verification that the remote host taking part of a data connection is the same as the host to which the control
     * connection is attached should be enabled.
     *
     * @param enabled {@code true} to enable verification, or {@code false} to disable verification.
     * @return This object.
     */
    @QueryParam(REMOTE_VERIFICATION_ENABLED)
    public FTPEnvironment withRemoteVerificationEnabled(boolean enabled) {
        put(REMOTE_VERIFICATION_ENABLED, enabled);
        return this;
    }

    /**
     * Stores the default directory to use.
     * If it exists, this will be the directory that relative paths are resolved to.
     *
     * @param pathname The default directory to use.
     * @return This object.
     */
    @QueryParam(DEFAULT_DIR)
    public FTPEnvironment withDefaultDirectory(String pathname) {
        put(DEFAULT_DIR, pathname);
        return this;
    }

    /**
     * Stores the connection mode to use.
     * If the connection mode is not set, it will default to {@link ConnectionMode#ACTIVE}.
     *
     * @param connectionMode The connection mode to use.
     * @return This object.
     */
    @QueryParam(CONNECTION_MODE)
    public FTPEnvironment withConnectionMode(ConnectionMode connectionMode) {
        put(CONNECTION_MODE, connectionMode);
        return this;
    }

    /**
     * Stores the client side port range in active mode.
     *
     * @param minPort The lowest available port (inclusive).
     * @param maxPort The highest available port (inclusive).
     * @return This object.
     */
    @QueryParam(ACTIVE_PORT_RANGE_MIN)
    @QueryParam(ACTIVE_PORT_RANGE_MAX)
    public FTPEnvironment withActivePortRange(int minPort, int maxPort) {
        put(ACTIVE_PORT_RANGE_MIN, minPort);
        put(ACTIVE_PORT_RANGE_MAX, maxPort);
        return this;
    }

    /**
     * Stores the external IP address in active mode. Useful when there are multiple network cards.
     *
     * @param ipAddress The external IP address of this machine.
     * @return This object.
     */
    @QueryParam(ACTIVE_EXTERNAL_IP_ADDRESS)
    public FTPEnvironment withActiveExternalIPAddress(String ipAddress) {
        put(ACTIVE_EXTERNAL_IP_ADDRESS, ipAddress);
        return this;
    }

    /**
     * Stores the local IP address to use in passive mode. Useful when there are multiple network cards.
     *
     * @param ipAddress The local IP address of this machine.
     * @return This object.
     */
    @QueryParam(PASSIVE_LOCAL_IP_ADDRESS)
    public FTPEnvironment withPassiveLocalIPAddress(String ipAddress) {
        put(PASSIVE_LOCAL_IP_ADDRESS, ipAddress);
        return this;
    }

    /**
     * Stores the external IP address to report in EPRT/PORT commands in active mode. Useful when there are multiple network cards.
     *
     * @param ipAddress The external IP address of this machine.
     * @return This object.
     */
    @QueryParam(REPORT_ACTIVE_EXTERNAL_IP_ADDRESS)
    public FTPEnvironment withReportActiveExternalIPAddress(String ipAddress) {
        put(REPORT_ACTIVE_EXTERNAL_IP_ADDRESS, ipAddress);
        return this;
    }

    /**
     * Stores the buffer size to use.
     *
     * @param bufferSize The buffer size to use.
     * @return This object.
     */
    @QueryParam(BUFFER_SIZE)
    public FTPEnvironment withBufferSize(int bufferSize) {
        put(BUFFER_SIZE, bufferSize);
        return this;
    }

    /**
     * Stores the value to use for the data socket {@code SO_SNDBUF} option.
     *
     * @param bufferSizr The size of the buffer.
     * @return This object.
     */
    @QueryParam(SEND_DATA_SOCKET_BUFFER_SIZE)
    public FTPEnvironment withSendDataSocketBufferSize(int bufferSizr) {
        put(SEND_DATA_SOCKET_BUFFER_SIZE, bufferSizr);
        return this;
    }

    /**
     * Stores the value to use for the data socket {@code SO_RCVBUF} option.
     *
     * @param bufferSize The size of the buffer.
     * @return This object.
     */
    @QueryParam(RECEIVE_DATA_SOCKET_BUFFER_SIZE)
    public FTPEnvironment withReceiveDataSocketBufferSize(int bufferSize) {
        put(RECEIVE_DATA_SOCKET_BUFFER_SIZE, bufferSize);
        return this;
    }

    /**
     * Stores the FTP client config to use.
     *
     * @param clientConfig The client config to use.
     * @return This object.
     */
    public FTPEnvironment withClientConfig(FTPClientConfig clientConfig) {
        put(CLIENT_CONFIG, clientConfig);
        return this;
    }

    /**
     * Stores whether or not to use EPSV with IPv4. Might be worth enabling in some circumstances.
     * <p>
     * For example, when using IPv4 with NAT it may work with some rare configurations.
     * E.g. if FTP server has a static PASV address (external network) and the client is coming from another internal network.
     * In that case the data connection after PASV command would fail, while EPSV would make the client succeed by taking just the port.
     *
     * @param selected The flag to use.
     * @return This object.
     */
    @QueryParam(USE_EPSV_WITH_IPV4)
    public FTPEnvironment withUseEPSVwithIPv4(boolean selected) {
        put(USE_EPSV_WITH_IPV4, selected);
        return this;
    }

    /**
     * Stores the time to wait between sending control connection keep-alive messages when processing file upload or download.
     *
     * @param timeout The keep-alive timeout to use.
     * @return This object.
     * @since 3.1
     */
    @QueryParam(CONTROL_KEEP_ALIVE_TIMEOUT)
    public FTPEnvironment withControlKeepAliveTimeout(Duration timeout) {
        put(CONTROL_KEEP_ALIVE_TIMEOUT, timeout);
        return this;
    }

    /**
     * Stores how long to wait for control keep-alive message replies.
     *
     * @param timeout The keep-alive reply timeout to use.
     * @return This object.
     * @since 3.1
     */
    @QueryParam(CONTROL_KEEP_ALIVE_REPLY_TIMEOUT)
    public FTPEnvironment withControlKeepAliveReplyTimeout(Duration timeout) {
        put(CONTROL_KEEP_ALIVE_REPLY_TIMEOUT, timeout);
        return this;
    }

    /**
     * Stores the workaround strategy to replace the PASV mode reply addresses.
     * This gets around the problem that some NAT boxes may change the reply.
     *
     * The default implementation is {@link NatServerResolverImpl}, i.e. site-local replies are replaced.
     *
     * @param resolver The workaround strategy to replace internal IP's in passive mode, or {@code null} to disable the workaround
     *            (i.e. use PASV mode reply address.)
     * @return This object.
     * @since 1.1
     */
    public FTPEnvironment withPassiveNatWorkaroundStrategy(HostnameResolver resolver) {
        put(PASSIVE_NAT_WORKAROUND_STRATEGY, resolver);
        return this;
    }

    /**
     * Stores whether or not automatic server encoding detection should be enabled.
     * Note that only UTF-8 is supported.
     *
     * @param autodetect {@code true} to enable automatic server encoding detection, or {@code false} to disable it.
     * @return This object.
     */
    @QueryParam(AUTODETECT_ENCODING)
    public FTPEnvironment withAutodetectEncoding(boolean autodetect) {
        put(AUTODETECT_ENCODING, autodetect);
        return this;
    }

    /**
     * Stores whether or not to list hidden files.
     * If this flag is not set, it will default to {@code true}.
     * <p>
     * Note that if this flag is set to {@code false}, the current directory will (most likely) not be returned when listing directories.
     * That means that {@link StandardFTPFileStrategyFactory#UNIX} will not work correctly.
     * <p>
     * Ideally, only set this flag (to {@code false}) if the FTP server does not support the {@code -a} option for {@code LIST} commands.
     *
     * @param listHiddenFiles {@code true} to list hidden files, or {@code false} to omit them.
     * @return This object.
     * @since 2.0
     */
    @QueryParam(LIST_HIDDEN_FILES)
    public FTPEnvironment withListHiddenFiles(boolean listHiddenFiles) {
        put(LIST_HIDDEN_FILES, listHiddenFiles);
        return this;
    }

    // FTP file system support

    /**
     * Stores the pool config to use.
     * <p>
     * The {@linkplain FTPPoolConfig#maxSize() maximum pool size} influences the number of concurrent threads that can access an FTP file system.
     * <br>
     * If the {@linkplain FTPPoolConfig#maxWaitTime() maximum wait time} is {@linkplain Duration#isNegative() negative}, FTP file systems wait
     * indefinitely until a client connection is available. This is the default setting if no pool config is defined.
     *
     * @param poolConfig The pool config to use.
     * @return This object.
     * @since 3.0
     */
    @QueryParam(POOL_CONFIG_MAX_WAIT_TIME)
    @QueryParam(POOL_CONFIG_MAX_IDLE_TIME)
    @QueryParam(POOL_CONFIG_INITIAL_SIZE)
    @QueryParam(POOL_CONFIG_MAX_SIZE)
    public FTPEnvironment withPoolConfig(FTPPoolConfig poolConfig) {
        put(POOL_CONFIG, poolConfig);
        return this;
    }

    /**
     * Stores the file system exception factory to use.
     *
     * @param factory The file system exception factory to use.
     * @return This object.
     */
    public FTPEnvironment withFileSystemExceptionFactory(FileSystemExceptionFactory factory) {
        put(FILE_SYSTEM_EXCEPTION_FACTORY, factory);
        return this;
    }

    /**
     * Stores the FTP file strategy factory to use.
     * If the FTP file strategy factory is not set, it will default to {@link StandardFTPFileStrategyFactory#AUTO_DETECT}.
     *
     * @param factory The FTP file strategy factory to use.
     * @return This object.
     */
    public FTPEnvironment withFTPFileStrategyFactory(FTPFileStrategyFactory factory) {
        put(FTP_FILE_STRATEGY_FACTORY, factory);
        return this;
    }

    FTPEnvironment withQueryString(String rawQueryString) throws IOException {
        new QueryParamProcessor(this).processQueryString(rawQueryString);
        return this;
    }

    FTPEnvironment withQueryParam(String name, String value) throws IOException {
        switch (name) {
            case LOCAL_ADDR:
                put(LOCAL_ADDR, InetAddress.getByName(value));
                break;
            case LOCAL_PORT:
                put(LOCAL_PORT, Integer.parseInt(value));
                break;
            case ACCOUNT:
                put(ACCOUNT, value);
                break;
            case SO_TIMEOUT:
                withSoTimeout(Integer.parseInt(value));
                break;
            case SEND_BUFFER_SIZE:
                withSendBufferSize(Integer.parseInt(value));
                break;
            case RECEIVE_BUFFER_SIZE:
                withReceiveBufferSize(Integer.parseInt(value));
                break;
            case TCP_NO_DELAY:
                withTcpNoDelay(Boolean.parseBoolean(value));
                break;
            case KEEP_ALIVE:
                withKeepAlive(Boolean.parseBoolean(value));
                break;
            case SO_LINGER_ON:
                put(SO_LINGER_ON, Boolean.parseBoolean(value));
                break;
            case SO_LINGER_VALUE:
                put(SO_LINGER_VALUE, Integer.parseInt(value));
                break;
            case CONNECT_TIMEOUT:
                withConnectTimeout(Integer.parseInt(value));
                break;
            case CHARSET:
                withCharset(Charset.forName(value));
                break;
            case CONTROL_ENCODING:
                withControlEncoding(value);
                break;
            case STRICT_MULTILINE_PARSING:
                withStrictMultilineParsing(Boolean.parseBoolean(value));
                break;
            case DATA_TIMEOUT:
                withDataTimeout(Duration.parse(value));
                break;
            case IP_ADDRESS_FROM_PASV_RESPONSE:
                withIpAddressFromPasvResponse(Boolean.parseBoolean(value));
                break;
            case REMOTE_VERIFICATION_ENABLED:
                withRemoteVerificationEnabled(Boolean.parseBoolean(value));
                break;
            case DEFAULT_DIR:
                withDefaultDirectory(value);
                break;
            case CONNECTION_MODE:
                withConnectionMode(ConnectionMode.valueOf(value));
                break;
            case ACTIVE_PORT_RANGE_MIN:
                put(ACTIVE_PORT_RANGE_MIN, Integer.parseInt(value));
                break;
            case ACTIVE_PORT_RANGE_MAX:
                put(ACTIVE_PORT_RANGE_MAX, Integer.parseInt(value));
                break;
            case ACTIVE_EXTERNAL_IP_ADDRESS:
                withActiveExternalIPAddress(value);
                break;
            case PASSIVE_LOCAL_IP_ADDRESS:
                withPassiveLocalIPAddress(value);
                break;
            case REPORT_ACTIVE_EXTERNAL_IP_ADDRESS:
                withReportActiveExternalIPAddress(value);
                break;
            case BUFFER_SIZE:
                withBufferSize(Integer.parseInt(value));
                break;
            case SEND_DATA_SOCKET_BUFFER_SIZE:
                withSendDataSocketBufferSize(Integer.parseInt(value));
                break;
            case RECEIVE_DATA_SOCKET_BUFFER_SIZE:
                withReceiveDataSocketBufferSize(Integer.parseInt(value));
                break;
            case USE_EPSV_WITH_IPV4:
                withUseEPSVwithIPv4(Boolean.parseBoolean(value));
                break;
            case CONTROL_KEEP_ALIVE_TIMEOUT:
                withControlKeepAliveTimeout(Duration.parse(value));
                break;
            case CONTROL_KEEP_ALIVE_REPLY_TIMEOUT:
                withControlKeepAliveReplyTimeout(Duration.parse(value));
                break;
            case AUTODETECT_ENCODING:
                withAutodetectEncoding(Boolean.parseBoolean(value));
                break;
            case LIST_HIDDEN_FILES:
                withListHiddenFiles(Boolean.parseBoolean(value));
                break;
            default:
                // ignore
                break;
        }
        return this;
    }

    boolean hasUsername() {
        return containsKey(USERNAME);
    }

    boolean hasDefaultDir() {
        return containsKey(DEFAULT_DIR);
    }

    String getUsername() {
        return FileSystemProviderSupport.getValue(this, USERNAME, String.class, null);
    }

    FileType getDefaultFileType() {
        // explicitly set in initializePostConnect
        return FileType.binary();
    }

    FileStructure getDefaultFileStructure() {
        // as specified by FTPClient
        return FileStructure.FILE;
    }

    FileTransferMode getDefaultFileTransferMode() {
        // as specified by FTPClient
        return FileTransferMode.STREAM;
    }

    FTPPoolConfig getPoolConfig() {
        return FileSystemProviderSupport.getValue(this, POOL_CONFIG, FTPPoolConfig.class, FTPPoolConfig.defaultConfig());
    }

    FileSystemExceptionFactory getExceptionFactory() {
        return FileSystemProviderSupport.getValue(this, FILE_SYSTEM_EXCEPTION_FACTORY, FileSystemExceptionFactory.class,
                DefaultFileSystemExceptionFactory.INSTANCE);
    }

    FTPFileStrategy getFTPFileStrategy() {
        FTPFileStrategyFactory factory = FileSystemProviderSupport.getValue(this, FTP_FILE_STRATEGY_FACTORY, FTPFileStrategyFactory.class,
                StandardFTPFileStrategyFactory.AUTO_DETECT);
        return factory.createFTPFileStrategy();
    }

    FTPClient createClient(String hostname, int port) throws IOException {
        FTPClient client = new FTPClient();
        initializePreConnect(client);
        connect(client, hostname, port);
        initializePostConnect(client);
        login(client);
        initializePostLogin(client);
        verifyConnection(client);
        return client;
    }

    // this method is called twice, pre-connect and post-login, to make sure these settings get set and don't get reset by FTPClient
    private void applyConnectionSettings(FTPClient client) throws IOException {
        FileSystemProviderSupport.getValue(this, CONNECTION_MODE, ConnectionMode.class, ConnectionMode.ACTIVE).apply(client);

        configureACtivePortRange(client);

        configureActiveExternalIPAddress(client);
        configurePassiveLocalIPAddress(client);
        configureReportActiveExternalIPAddress(client);
    }

    private void configureACtivePortRange(FTPClient client) {
        if (containsKey(ACTIVE_PORT_RANGE_MIN) && containsKey(ACTIVE_PORT_RANGE_MAX)) {
            int minPort = FileSystemProviderSupport.getIntValue(this, ACTIVE_PORT_RANGE_MIN);
            int maxPort = FileSystemProviderSupport.getIntValue(this, ACTIVE_PORT_RANGE_MAX);
            client.setActivePortRange(minPort, maxPort);
        }
    }

    private void configureActiveExternalIPAddress(FTPClient client) throws UnknownHostException {
        if (containsKey(ACTIVE_EXTERNAL_IP_ADDRESS)) {
            String ipAddress = FileSystemProviderSupport.getValue(this, ACTIVE_EXTERNAL_IP_ADDRESS, String.class, null);
            client.setActiveExternalIPAddress(ipAddress);
        }
    }

    private void configurePassiveLocalIPAddress(FTPClient client) throws UnknownHostException {
        if (containsKey(PASSIVE_LOCAL_IP_ADDRESS)) {
            String ipAddress = FileSystemProviderSupport.getValue(this, PASSIVE_LOCAL_IP_ADDRESS, String.class, null);
            client.setPassiveLocalIPAddress(ipAddress);
        }
    }

    private void configureReportActiveExternalIPAddress(FTPClient client) throws UnknownHostException {
        if (containsKey(REPORT_ACTIVE_EXTERNAL_IP_ADDRESS)) {
            String ipAddress = FileSystemProviderSupport.getValue(this, REPORT_ACTIVE_EXTERNAL_IP_ADDRESS, String.class, null);
            client.setReportActiveExternalIPAddress(ipAddress);
        }
    }

    void initializePreConnect(FTPClient client) throws IOException {
        configureListHiddenFiles(client);

        configureSendBufferSize(client);
        configureReceiveBufferSize(client);

        configureSocketFactory(client);
        configureServerSocketFactory(client);

        configureConnectTimeout(client);

        configureProxy(client);

        configureCharset(client);

        configureControlEncoding(client);

        configureStrictMultilineParsing(client);

        configureDataTimeout(client);

        configureParserFactory(client);

        configureIpAddressFromPasvResponse(client);

        configureRemoteVerificationEnabled(client);

        applyConnectionSettings(client);

        configureBufferSize(client);
        configureSendDataSocketBufferSize(client);
        configureReceiveDataSocketBufferSize(client);

        configureClientConfig(client);

        configurePassiveNatWorkaroundStrategy(client);

        configureUseEPSVwithIPv4(client);

        configureControlKeepAliveTimeout(client);
        configureControlKeepAliveReplyTimeout(client);

        configureAutodetectEncoding(client);
    }

    private void configureListHiddenFiles(FTPClient client) {
        boolean listHiddenFiles = FileSystemProviderSupport.getBooleanValue(this, LIST_HIDDEN_FILES, true);
        client.setListHiddenFiles(listHiddenFiles);
    }

    private void configureSendBufferSize(FTPClient client) throws SocketException {
        if (containsKey(SEND_BUFFER_SIZE)) {
            int size = FileSystemProviderSupport.getIntValue(this, SEND_BUFFER_SIZE);
            client.setSendBufferSize(size);
        }
    }

    private void configureReceiveBufferSize(FTPClient client) throws SocketException {
        if (containsKey(RECEIVE_BUFFER_SIZE)) {
            int size = FileSystemProviderSupport.getIntValue(this, RECEIVE_BUFFER_SIZE);
            client.setReceiveBufferSize(size);
        }
    }

    private void configureSocketFactory(FTPClient client) {
        if (containsKey(SOCKET_FACTORY)) {
            SocketFactory factory = FileSystemProviderSupport.getValue(this, SOCKET_FACTORY, SocketFactory.class, null);
            client.setSocketFactory(factory);
        }
    }

    private void configureServerSocketFactory(FTPClient client) {
        if (containsKey(SERVER_SOCKET_FACTORY)) {
            ServerSocketFactory factory = FileSystemProviderSupport.getValue(this, SERVER_SOCKET_FACTORY, ServerSocketFactory.class, null);
            client.setServerSocketFactory(factory);
        }
    }

    private void configureConnectTimeout(FTPClient client) {
        int connectTimeout = FileSystemProviderSupport.getIntValue(this, CONNECT_TIMEOUT, DEFAULT_CONNECT_TIMEOUT);
        client.setConnectTimeout(connectTimeout);
    }

    private void configureProxy(FTPClient client) {
        if (containsKey(PROXY)) {
            Proxy proxy = FileSystemProviderSupport.getValue(this, PROXY, Proxy.class, null);
            client.setProxy(proxy);
        }
    }

    private void configureCharset(FTPClient client) {
        if (containsKey(CHARSET)) {
            Charset charset = FileSystemProviderSupport.getValue(this, CHARSET, Charset.class, null);
            client.setCharset(charset);
        }
    }

    private void configureControlEncoding(FTPClient client) {
        if (containsKey(CONTROL_ENCODING)) {
            String controlEncoding = FileSystemProviderSupport.getValue(this, CONTROL_ENCODING, String.class, null);
            client.setControlEncoding(controlEncoding);
        }
    }

    private void configureStrictMultilineParsing(FTPClient client) {
        if (containsKey(STRICT_MULTILINE_PARSING)) {
            boolean strictMultilineParsing = FileSystemProviderSupport.getBooleanValue(this, STRICT_MULTILINE_PARSING);
            client.setStrictMultilineParsing(strictMultilineParsing);
        }
    }

    private void configureDataTimeout(FTPClient client) {
        if (containsKey(DATA_TIMEOUT)) {
            Duration timeout = FileSystemProviderSupport.getValue(this, DATA_TIMEOUT, Duration.class);
            client.setDataTimeout(timeout);
        }
    }

    private void configureParserFactory(FTPClient client) {
        if (containsKey(PARSER_FACTORY)) {
            FTPFileEntryParserFactory parserFactory = FileSystemProviderSupport.getValue(this, PARSER_FACTORY, FTPFileEntryParserFactory.class, null);
            client.setParserFactory(parserFactory);
        }
    }

    private void configureIpAddressFromPasvResponse(FTPClient client) {
        if (containsKey(IP_ADDRESS_FROM_PASV_RESPONSE)) {
            boolean usingIpAddressFromPasvResponse = FileSystemProviderSupport.getBooleanValue(this, IP_ADDRESS_FROM_PASV_RESPONSE);
            client.setIpAddressFromPasvResponse(usingIpAddressFromPasvResponse);
        }
    }

    private void configureRemoteVerificationEnabled(FTPClient client) {
        if (containsKey(REMOTE_VERIFICATION_ENABLED)) {
            boolean enable = FileSystemProviderSupport.getBooleanValue(this, REMOTE_VERIFICATION_ENABLED);
            client.setRemoteVerificationEnabled(enable);
        }
    }

    private void configureBufferSize(FTPClient client) {
        if (containsKey(BUFFER_SIZE)) {
            int bufSize = FileSystemProviderSupport.getIntValue(this, BUFFER_SIZE);
            client.setBufferSize(bufSize);
        }
    }

    private void configureSendDataSocketBufferSize(FTPClient client) {
        if (containsKey(SEND_DATA_SOCKET_BUFFER_SIZE)) {
            int bufSize = FileSystemProviderSupport.getIntValue(this, SEND_DATA_SOCKET_BUFFER_SIZE);
            client.setSendDataSocketBufferSize(bufSize);
        }
    }

    private void configureReceiveDataSocketBufferSize(FTPClient client) {
        if (containsKey(RECEIVE_DATA_SOCKET_BUFFER_SIZE)) {
            int bufSize = FileSystemProviderSupport.getIntValue(this, RECEIVE_DATA_SOCKET_BUFFER_SIZE);
            client.setReceieveDataSocketBufferSize(bufSize);
        }
    }

    private void configureClientConfig(FTPClient client) {
        if (containsKey(CLIENT_CONFIG)) {
            FTPClientConfig clientConfig = FileSystemProviderSupport.getValue(this, CLIENT_CONFIG, FTPClientConfig.class, null);
            if (clientConfig != null) {
                clientConfig = new FTPClientConfig(clientConfig);
            }
            client.configure(clientConfig);
        }
    }

    private void configurePassiveNatWorkaroundStrategy(FTPClient client) {
        if (containsKey(PASSIVE_NAT_WORKAROUND_STRATEGY)) {
            HostnameResolver resolver = FileSystemProviderSupport.getValue(this, PASSIVE_NAT_WORKAROUND_STRATEGY, HostnameResolver.class, null);
            client.setPassiveNatWorkaroundStrategy(resolver);
        }
    }

    private void configureUseEPSVwithIPv4(FTPClient client) {
        if (containsKey(USE_EPSV_WITH_IPV4)) {
            boolean selected = FileSystemProviderSupport.getBooleanValue(this, USE_EPSV_WITH_IPV4);
            client.setUseEPSVwithIPv4(selected);
        }
    }

    private void configureControlKeepAliveTimeout(FTPClient client) {
        if (containsKey(CONTROL_KEEP_ALIVE_TIMEOUT)) {
            Duration controlIdle = FileSystemProviderSupport.getValue(this, CONTROL_KEEP_ALIVE_TIMEOUT, Duration.class);
            client.setControlKeepAliveTimeout(controlIdle);
        }
    }

    private void configureControlKeepAliveReplyTimeout(FTPClient client) {
        if (containsKey(CONTROL_KEEP_ALIVE_REPLY_TIMEOUT)) {
            Duration timeout = FileSystemProviderSupport.getValue(this, CONTROL_KEEP_ALIVE_REPLY_TIMEOUT, Duration.class);
            client.setControlKeepAliveReplyTimeout(timeout);
        }
    }

    private void configureAutodetectEncoding(FTPClient client) {
        if (containsKey(AUTODETECT_ENCODING)) {
            boolean autodetect = FileSystemProviderSupport.getBooleanValue(this, AUTODETECT_ENCODING);
            client.setAutodetectUTF8(autodetect);
        }
    }

    void connect(FTPClient client, String hostname, int port) throws IOException {
        if (port == -1) {
            port = client.getDefaultPort();
        }

        InetAddress localAddr = FileSystemProviderSupport.getValue(this, LOCAL_ADDR, InetAddress.class, null);
        if (localAddr != null) {
            int localPort = FileSystemProviderSupport.getIntValue(this, LOCAL_PORT);
            client.connect(hostname, port, localAddr, localPort);
        } else {
            client.connect(hostname, port);
        }
    }

    void initializePostConnect(FTPClient client) throws IOException {
        configureSoTimeout(client);

        configureTcpNoDelay(client);

        configureKeepAlive(client);

        configureSoLinger(client);
    }

    private void configureSoTimeout(FTPClient client) throws SocketException {
        if (containsKey(SO_TIMEOUT)) {
            int timeout = FileSystemProviderSupport.getIntValue(this, SO_TIMEOUT);
            client.setSoTimeout(timeout);
        }
    }

    private void configureTcpNoDelay(FTPClient client) throws SocketException {
        if (containsKey(TCP_NO_DELAY)) {
            boolean on = FileSystemProviderSupport.getBooleanValue(this, TCP_NO_DELAY);
            client.setTcpNoDelay(on);
        }
    }

    private void configureKeepAlive(FTPClient client) throws SocketException {
        if (containsKey(KEEP_ALIVE)) {
            boolean keepAlive = FileSystemProviderSupport.getBooleanValue(this, KEEP_ALIVE);
            client.setKeepAlive(keepAlive);
        }
    }

    private void configureSoLinger(FTPClient client) throws SocketException {
        if (containsKey(SO_LINGER_ON) && containsKey(SO_LINGER_VALUE)) {
            boolean on = FileSystemProviderSupport.getBooleanValue(this, SO_LINGER_ON);
            int val = FileSystemProviderSupport.getIntValue(this, SO_LINGER_VALUE);
            client.setSoLinger(on, val);
        }
    }

    void login(FTPClient client) throws IOException {
        String username = getUsername();
        char[] passwordChars = FileSystemProviderSupport.getValue(this, PASSWORD, char[].class, null);
        String password = passwordChars != null ? new String(passwordChars) : null;
        String account = FileSystemProviderSupport.getValue(this, ACCOUNT, String.class, null);
        if (account != null) {
            login(client, username, password, account);
        } else if (username != null || password != null) {
            login(client, username, password);
        }
        // else no account or username/password - don't log in
    }

    private void login(FTPClient client, String username, String password, String account) throws IOException {
        if (!client.login(username, password, account)) {
            throw new FTPFileSystemException(client.getReplyCode(), client.getReplyString());
        }
    }

    private void login(FTPClient client, String username, String password) throws IOException {
        if (!client.login(username, password)) {
            throw new FTPFileSystemException(client.getReplyCode(), client.getReplyString());
        }
    }

    void initializePostLogin(FTPClient client) throws IOException {
        applyConnectionSettings(client);

        // default to binary
        client.setFileType(FTP.BINARY_FILE_TYPE);

        configureDefaultDir(client);
    }

    private void configureDefaultDir(FTPClient client) throws IOException {
        String defaultDir = FileSystemProviderSupport.getValue(this, DEFAULT_DIR, String.class, null);
        if (defaultDir != null && !client.changeWorkingDirectory(defaultDir)) {
            throw getExceptionFactory().createChangeWorkingDirectoryException(defaultDir, client.getReplyCode(), client.getReplyString());
        }
    }

    void verifyConnection(FTPClient client) throws IOException {
        if (client.printWorkingDirectory() == null) {
            throw new FTPFileSystemException(client.getReplyCode(), client.getReplyString());
        }
    }

    // Map / Object

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return map.containsValue(value);
    }

    @Override
    public Object get(Object key) {
        return map.get(key);
    }

    @Override
    public Object put(String key, Object value) {
        return map.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        return map.remove(key);
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        map.putAll(m);
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public Set<String> keySet() {
        return map.keySet();
    }

    @Override
    public Collection<Object> values() {
        return map.values();
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return map.equals(o);
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @Override
    public String toString() {
        return map.toString();
    }

    /**
     * Copies a map to create a new {@link FTPEnvironment} instance.
     * If the given map is an instance of {@link FTPSEnvironment}, this method will return a new {@link FTPSEnvironment} instance.
     *
     * @param env The map to copy. It can be an {@link FTPEnvironment} instance, but does not have to be.
     * @return A new {@link FTPEnvironment} instance that is a copy of the given map.
     * @since 3.0
     */
    public static FTPEnvironment copy(Map<String, ?> env) {
        if (env instanceof FTPSEnvironment) {
            return new FTPSEnvironment(new HashMap<>(env));
        }
        return env == null
                ? new FTPEnvironment()
                : new FTPEnvironment(new HashMap<>(env));
    }

    /**
     * Sets the default FTP environment.
     * This is used in {@link FTPFileSystemProvider#getPath(URI)} when a file system needs to be created, since no environment can be passed.
     * This way, certain settings like {@link #withPoolConfig(FTPPoolConfig) pool configuration} can still be applied.
     *
     * @param defaultEnvironment The default FTP environment. Use {@code null} to reset it to an empty environment.
     * @since 3.2
     */
    public static void setDefault(FTPEnvironment defaultEnvironment) {
        DEFAULTS.set(copy(defaultEnvironment));
    }

    static FTPEnvironment copyOfDefault() {
        return copy(DEFAULTS.get());
    }

    /**
     * Indicates which query parameters can be used to define environment values.
     *
     * @author Rob Spoor
     * @since 3.2
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    @Repeatable(QueryParams.class)
    public @interface QueryParam {

        /**
         * The name of the query parameter.
         */
        String value();
    }

    /**
     * A container for {@link QueryParam} annotations.
     *
     * @author Rob Spoor
     * @since 3.2
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.SOURCE)
    @Documented
    public @interface QueryParams {

        /**
         * The contained {@link QueryParam} annotations.
         */
        QueryParam[] value();
    }

    static class QueryParamProcessor {

        private final FTPEnvironment env;
        private FTPPoolConfig.Builder poolConfigBuilder;

        QueryParamProcessor(FTPEnvironment env) {
            this.env = env;
        }

        private void processQueryString(String rawQueryString) throws IOException {
            int start = 0;
            int indexOfAmp = rawQueryString.indexOf('&', start);
            while (indexOfAmp != -1) {
                processQueryParam(rawQueryString, start, indexOfAmp);
                start = indexOfAmp + 1;
                indexOfAmp = rawQueryString.indexOf('&', start);
            }
            processQueryParam(rawQueryString, start, rawQueryString.length());

            if (poolConfigBuilder != null) {
                env.withPoolConfig(poolConfigBuilder.build());
            }
        }

        private void processQueryParam(String rawQueryString, int start, int end) throws IOException {
            int indexOfEquals = rawQueryString.indexOf('=', start);
            if (indexOfEquals == -1 || indexOfEquals > end) {
                String name = decode(rawQueryString.substring(start, end));
                processQueryParam(name, ""); //$NON-NLS-1$
            } else {
                String name = decode(rawQueryString.substring(start, indexOfEquals));
                String value = decode(rawQueryString.substring(indexOfEquals + 1, end));
                processQueryParam(name, value);
            }
        }

        void processQueryParam(String name, String value) throws IOException {
            switch (name) {
                case POOL_CONFIG_MAX_WAIT_TIME:
                    poolConfigBuilder().withMaxWaitTime(Duration.parse(value));
                    break;
                case POOL_CONFIG_MAX_IDLE_TIME:
                    poolConfigBuilder().withMaxIdleTime(Duration.parse(value));
                    break;
                case POOL_CONFIG_INITIAL_SIZE:
                    poolConfigBuilder().withInitialSize(Integer.parseInt(value));
                    break;
                case POOL_CONFIG_MAX_SIZE:
                    poolConfigBuilder().withMaxSize(Integer.parseInt(value));
                    break;
                default:
                    env.withQueryParam(name, value);
                    break;
            }
        }

        private String decode(String value) {
            return URLDecoder.decode(value, StandardCharsets.UTF_8);
        }

        private FTPPoolConfig.Builder poolConfigBuilder() {
            if (poolConfigBuilder == null) {
                poolConfigBuilder = env.getPoolConfig().toBuilder();
            }
            return poolConfigBuilder;
        }
    }
}
