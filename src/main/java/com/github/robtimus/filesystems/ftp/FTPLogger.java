/*
 * FTPLogger.java
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

import java.io.IOException;
import java.util.ResourceBundle;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.github.robtimus.filesystems.UTF8Control;

/**
 * A utility class to perform logging.
 *
 * @author Rob Spoor
 */
final class FTPLogger {

    private static final String BUNDLE_NAME = "com.github.robtimus.filesystems.ftp.fs"; //$NON-NLS-1$
    private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME, UTF8Control.INSTANCE);

    private FTPLogger() {
        throw new Error("cannot create instances of " + getClass().getName()); //$NON-NLS-1$
    }

    public static Logger createLogger(Class<?> clazz) {
        try {
            return LoggerFactory.getLogger(clazz);
        } catch (@SuppressWarnings("unused") NoClassDefFoundError e) {
            return null;
        }
    }

    private static synchronized String getMessage(String key) {
        return BUNDLE.getString(key);
    }

    public static void creatingPool(Logger logger, String hostname, int port, int poolSize, long poolWaitTimeout) {
        if (logger != null && logger.isDebugEnabled()) {
            if (port == -1) {
                logger.debug(String.format(getMessage("log.creatingPoolWithoutPort"), hostname, poolSize, poolWaitTimeout)); //$NON-NLS-1$
            } else {
                logger.debug(String.format(getMessage("log.creatingPoolWithPort"), hostname, port, poolSize, poolWaitTimeout)); //$NON-NLS-1$
            }
        }
    }

    public static void createdPool(Logger logger, String hostname, int port, int poolSize) {
        if (logger != null && logger.isDebugEnabled()) {
            if (port == -1) {
                logger.debug(String.format(getMessage("log.createdPoolWithoutPort"), hostname, poolSize)); //$NON-NLS-1$
            } else {
                logger.debug(String.format(getMessage("log.createdPoolWithPort"), hostname, port, poolSize)); //$NON-NLS-1$
            }
        }
    }

    public static void failedToCreatePool(Logger logger, IOException e) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(getMessage("log.failedToCreatePool"), e); //$NON-NLS-1$
        }
    }

    public static void createdClient(Logger logger, String clientId, boolean pooled) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.createdClient"), clientId, pooled)); //$NON-NLS-1$
        }
    }

    public static void tookClient(Logger logger, String clientId, int poolSize) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.tookClient"), clientId, poolSize)); //$NON-NLS-1$
        }
    }

    public static void clientNotConnected(Logger logger, String clientId) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.clientNotConnected"), clientId)); //$NON-NLS-1$
        }
    }

    public static void returnedClient(Logger logger, String clientId, int poolSize) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.returnedClient"), clientId, poolSize)); //$NON-NLS-1$
        }
    }

    public static void returnedBrokenClient(Logger logger, String clientId, int poolSize) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.returnedBrokenClient"), clientId, poolSize)); //$NON-NLS-1$
        }
    }

    public static void drainedPoolForKeepAlive(Logger logger) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(getMessage("log.drainedPoolForKeepAlive")); //$NON-NLS-1$
        }
    }

    public static void drainedPoolForClose(Logger logger) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(getMessage("log.drainedPoolForClose")); //$NON-NLS-1$
        }
    }

    public static void increasedRefCount(Logger logger, String clientId, int refCount) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.increasedRefCount"), clientId, refCount)); //$NON-NLS-1$
        }
    }

    public static void decreasedRefCount(Logger logger, String clientId, int refCount) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.decreasedRefCount"), clientId, refCount)); //$NON-NLS-1$
        }
    }

    public static void disconnectedClient(Logger logger, String clientId) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.disconnectedClient"), clientId)); //$NON-NLS-1$
        }
    }

    public static void createdInputStream(Logger logger, String clientId, String path) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.createdInputStream"), clientId, path)); //$NON-NLS-1$
        }
    }

    public static void closedInputStream(Logger logger, String clientId, String path) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.closedInputStream"), clientId, path)); //$NON-NLS-1$
        }
    }

    public static void createdOutputStream(Logger logger, String clientId, String path) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.createdOutputStream"), clientId, path)); //$NON-NLS-1$
        }
    }

    public static void closedOutputStream(Logger logger, String clientId, String path) {
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug(String.format(getMessage("log.closedOutputStream"), clientId, path)); //$NON-NLS-1$
        }
    }

    public static void addCommandListener(final Logger logger, FTPClient ftpClient) {
        if (logger != null && logger.isTraceEnabled()) {
            ftpClient.addProtocolCommandListener(new ProtocolCommandListener() {
                @Override
                public void protocolCommandSent(ProtocolCommandEvent event) {
                    String message = trimTrailingLineTerminator(event.getMessage());
                    logger.trace(String.format(getMessage("log.ftpCommandSent"), message)); //$NON-NLS-1$
                }

                @Override
                public void protocolReplyReceived(ProtocolCommandEvent event) {
                    String message = trimTrailingLineTerminator(event.getMessage());
                    logger.trace(String.format(getMessage("log.ftpReplyReceived"), message)); //$NON-NLS-1$
                }

                private String trimTrailingLineTerminator(String message) {
                    if (message == null) {
                        return null;
                    }
                    if (message.endsWith("\r\n")) { //$NON-NLS-1$
                        return message.substring(0, message.length() - 2);
                    }
                    if (message.endsWith("\n")) { //$NON-NLS-1$
                        return message.substring(0, message.length() - 1);
                    }
                    return message;
                }
            });
        }
    }
}
