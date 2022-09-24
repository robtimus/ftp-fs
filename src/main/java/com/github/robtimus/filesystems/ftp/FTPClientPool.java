/*
 * FTPClientPool.java
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

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.util.Calendar;
import java.util.Collection;
import org.apache.commons.net.ProtocolCommandEvent;
import org.apache.commons.net.ProtocolCommandListener;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import com.github.robtimus.pool.LogLevel;
import com.github.robtimus.pool.Pool;
import com.github.robtimus.pool.PoolConfig;
import com.github.robtimus.pool.PoolLogger;
import com.github.robtimus.pool.PoolableObject;

/**
 * A pool of FTP clients, allowing multiple commands to be executed concurrently.
 *
 * @author Rob Spoor
 */
final class FTPClientPool {

    private final String hostname;
    private final int port;

    private final FTPEnvironment env;
    private final FileSystemExceptionFactory exceptionFactory;

    private final Pool<Client, IOException> pool;
    private final PoolLogger logger;

    FTPClientPool(String hostname, int port, FTPEnvironment env) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.env = env;
        this.exceptionFactory = env.getExceptionFactory();

        PoolConfig config = env.getPoolConfig().config();
        logger = PoolLogger.custom()
                .withLoggerClass(FTPClientPool.class)
                .withMessagePrefix((port == -1 ? hostname : hostname + ":" + port) + " - ") //$NON-NLS-1$ //$NON-NLS-2$
                .withObjectPrefix("client-") //$NON-NLS-1$
                .build();
        pool = new Pool<>(config, Client::new, logger);
    }

    Client get() throws IOException {
        try {
            return pool.acquire(() -> new IOException(FTPMessages.clientConnectionWaitTimeoutExpired()));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            InterruptedIOException iioe = new InterruptedIOException(e.getMessage());
            iioe.initCause(e);
            throw iioe;
        }
    }

    Client getOrCreate() throws IOException {
        return pool.acquireOrCreate();
    }

    void keepAlive() throws IOException {
        // Actually, no need to do anything; clients are validated using a keep-alive signal by the forAllIdleObjects call
        pool.forAllIdleObjects(client -> {
            // does nothing
        });
    }

    boolean isSecure() {
        return env instanceof FTPSEnvironment;
    }

    void close() throws IOException {
        pool.shutdown();
    }

    final class Client extends PoolableObject<IOException> implements Closeable {

        private final FTPClient client;

        private FileType fileType;
        private FileStructure fileStructure;
        private FileTransferMode fileTransferMode;

        private ProtocolCommandListener listener;

        private Client() throws IOException {
            this.client = env.createClient(hostname, port);

            this.fileType = env.getDefaultFileType();
            this.fileStructure = env.getDefaultFileStructure();
            this.fileTransferMode = env.getDefaultFileTransferMode();

            listener = null;
            configureListener();
        }

        private void configureListener() {
            /*
             * This method is called initially, but again each time a client is acquired (also for keep-alive), in case the log level has changed.
             * This is done in the validate method, which ensures the NoOp is logged if called from keepAlive.
             *
             * The combination of constructor + validate should be enough to handle all cases for get and getOrCreate - either the client was pooled,
             * and validate is called, or it wasn't and the constructor will be called.
             *
             * Use logger.isEnabled instead of isEnabled, otherwise logging will not be enabled when objects are created (not pooled yet).
             */
            boolean canLog = logger.isEnabled(LogLevel.TRACE);
            if (canLog && listener == null) {
                listener = new FTPCommandLogger();
                client.addProtocolCommandListener(listener);

            } else if (!canLog && listener != null) {
                client.removeProtocolCommandListener(listener);
                listener = null;
            }
        }

        private final class FTPCommandLogger implements ProtocolCommandListener {

            @Override
            public void protocolCommandSent(ProtocolCommandEvent event) {
                logger.objectEvent(LogLevel.TRACE, Client.this, () -> {
                    String message = trimTrailingLineTerminator(event.getMessage());
                    return FTPMessages.ftpCommandSent(message);
                });
            }

            @Override
            public void protocolReplyReceived(ProtocolCommandEvent event) {
                logger.objectEvent(LogLevel.TRACE, Client.this, () -> {
                    String message = trimTrailingLineTerminator(event.getMessage());
                    return FTPMessages.ftpReplyReceived(message);
                });
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
        }

        @Override
        protected boolean validate() {
            if (client.isConnected()) {
                configureListener();
                try {
                    client.sendNoOp();
                    return true;
                } catch (@SuppressWarnings("unused") IOException e) {
                    // the keep alive failed - let the pool call releaseResources
                }
            }
            return false;
        }

        @Override
        protected void releaseResources() throws IOException {
            client.disconnect();
        }

        @Override
        public void close() throws IOException {
            release();
        }

        FTPClient ftpClient() {
            return client;
        }

        FileSystemExceptionFactory exceptionFactory() {
            return exceptionFactory;
        }

        String pwd() throws IOException {
            String pwd = client.printWorkingDirectory();
            if (pwd == null) {
                throw new FTPFileSystemException(client.getReplyCode(), client.getReplyString());
            }
            return pwd;
        }

        private void applyTransferOptions(TransferOptions options) throws IOException {
            if (options.fileType != null && options.fileType != fileType) {
                options.fileType.apply(client);
                fileType = options.fileType;
            }
            if (options.fileStructure != null && options.fileStructure != fileStructure) {
                options.fileStructure.apply(client);
                fileStructure = options.fileStructure;
            }
            if (options.fileTransferMode != null && options.fileTransferMode != fileTransferMode) {
                options.fileTransferMode.apply(client);
                fileTransferMode = options.fileTransferMode;
            }
        }

        InputStream newInputStream(FTPPath path, OpenOptions options) throws IOException {
            assert options.read;

            applyTransferOptions(options);

            InputStream in = client.retrieveFileStream(path.path());
            if (in == null) {
                throw exceptionFactory.createNewInputStreamException(path.path(), client.getReplyCode(), client.getReplyString());
            }
            in = new FTPInputStream(path, in, options.deleteOnClose);
            addReference(in);
            return in;
        }

        private final class FTPInputStream extends InputStream {

            private final FTPPath path;
            private final InputStream in;
            private final boolean deleteOnClose;

            private boolean open = true;

            private FTPInputStream(FTPPath path, InputStream in, boolean deleteOnClose) {
                this.path = path;
                this.in = in;
                this.deleteOnClose = deleteOnClose;
                logEvent(() -> FTPMessages.createdInputStream(path.path()));
            }

            @Override
            public int read() throws IOException {
                return in.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return in.read(b);
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                return in.read(b, off, len);
            }

            @Override
            public long skip(long n) throws IOException {
                return in.skip(n);
            }

            @Override
            public int available() throws IOException {
                return in.available();
            }

            @Override
            public void close() throws IOException {
                if (open) {
                    try {
                        in.close();
                    } finally {
                        // always finalize the stream, to prevent pool starvation
                        // set open to false as well, to prevent finalizing the stream twice
                        open = false;
                        finalizeStream(this);
                    }
                    if (deleteOnClose) {
                        delete(path, false);
                    }
                    logEvent(() -> FTPMessages.closedInputStream(path.path()));
                }
            }

            @Override
            public synchronized void mark(int readlimit) {
                in.mark(readlimit);
            }

            @Override
            public synchronized void reset() throws IOException {
                in.reset();
            }

            @Override
            public boolean markSupported() {
                return in.markSupported();
            }
        }

        @SuppressWarnings("resource")
        OutputStream newOutputStream(FTPPath path, OpenOptions options) throws IOException {
            assert options.write;

            applyTransferOptions(options);

            OutputStream out = options.append ? client.appendFileStream(path.path()) : client.storeFileStream(path.path());
            if (out == null) {
                throw exceptionFactory.createNewOutputStreamException(path.path(), client.getReplyCode(), client.getReplyString(), options.options);
            }
            out = new FTPOutputStream(path, out, options.deleteOnClose);
            addReference(out);
            return out;
        }

        private final class FTPOutputStream extends OutputStream {

            private final FTPPath path;
            private final OutputStream out;
            private final boolean deleteOnClose;

            private boolean open = true;

            private FTPOutputStream(FTPPath path, OutputStream out, boolean deleteOnClose) {
                this.path = path;
                this.out = out;
                this.deleteOnClose = deleteOnClose;
                logEvent(() -> FTPMessages.createdOutputStream(path.path()));
            }

            @Override
            public void write(int b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte[] b) throws IOException {
                out.write(b);
            }

            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                out.write(b, off, len);
            }

            @Override
            public void flush() throws IOException {
                out.flush();
            }

            @Override
            public void close() throws IOException {
                if (open) {
                    try {
                        out.close();
                    } finally {
                        // always finalize the stream, to prevent pool starvation
                        // set open to false as well, to prevent finalizing the stream twice
                        open = false;
                        finalizeStream(this);
                    }
                    if (deleteOnClose) {
                        delete(path, false);
                    }
                    logEvent(() -> FTPMessages.closedOutputStream(path.path()));
                }
            }
        }

        private void finalizeStream(Object stream) throws IOException {
            try {
                if (!client.completePendingCommand()) {
                    throw new FTPFileSystemException(client.getReplyCode(), client.getReplyString());
                }
            } finally {
                removeReference(stream);
            }
        }

        void storeFile(FTPPath path, InputStream local, TransferOptions options, Collection<? extends OpenOption> openOptions) throws IOException {
            applyTransferOptions(options);

            if (!client.storeFile(path.path(), local)) {
                throw exceptionFactory.createNewOutputStreamException(path.path(), client.getReplyCode(), client.getReplyString(), openOptions);
            }
        }

        void mkdir(FTPPath path, FTPFileStrategy ftpFileStrategy) throws IOException {
            if (!client.makeDirectory(path.path())) {
                int replyCode = client.getReplyCode();
                String replyString = client.getReplyString();
                if (fileExists(path, ftpFileStrategy)) {
                    throw new FileAlreadyExistsException(path.path());
                }
                throw exceptionFactory.createCreateDirectoryException(path.path(), replyCode, replyString);
            }
        }

        private boolean fileExists(FTPPath path, FTPFileStrategy ftpFileStrategy) {
            try {
                ftpFileStrategy.getFTPFile(this, path);
                return true;
            } catch (@SuppressWarnings("unused") IOException e) {
                // the file actually may exist, but throw the original exception instead
                return false;
            }
        }

        void delete(FTPPath path, boolean isDirectory) throws IOException {
            boolean success = isDirectory ? client.removeDirectory(path.path()) : client.deleteFile(path.path());
            if (!success) {
                throw exceptionFactory.createDeleteException(path.path(), client.getReplyCode(), client.getReplyString(), isDirectory);
            }
        }

        void rename(FTPPath source, FTPPath target) throws IOException {
            if (!client.rename(source.path(), target.path())) {
                throw exceptionFactory.createMoveException(source.path(), target.path(), client.getReplyCode(), client.getReplyString());
            }
        }

        Calendar mdtm(FTPPath path) throws IOException {
            FTPFile file = client.mdtmFile(path.path());
            return file == null ? null : file.getTimestamp();
        }
    }
}
