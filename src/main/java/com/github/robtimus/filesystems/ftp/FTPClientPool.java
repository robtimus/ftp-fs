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

import static com.github.robtimus.filesystems.ftp.FTPLogger.addCommandListener;
import static com.github.robtimus.filesystems.ftp.FTPLogger.clientNotConnected;
import static com.github.robtimus.filesystems.ftp.FTPLogger.closedInputStream;
import static com.github.robtimus.filesystems.ftp.FTPLogger.closedOutputStream;
import static com.github.robtimus.filesystems.ftp.FTPLogger.createLogger;
import static com.github.robtimus.filesystems.ftp.FTPLogger.createdClient;
import static com.github.robtimus.filesystems.ftp.FTPLogger.createdInputStream;
import static com.github.robtimus.filesystems.ftp.FTPLogger.createdOutputStream;
import static com.github.robtimus.filesystems.ftp.FTPLogger.createdPool;
import static com.github.robtimus.filesystems.ftp.FTPLogger.creatingPool;
import static com.github.robtimus.filesystems.ftp.FTPLogger.decreasedRefCount;
import static com.github.robtimus.filesystems.ftp.FTPLogger.disconnectedClient;
import static com.github.robtimus.filesystems.ftp.FTPLogger.drainedPoolForClose;
import static com.github.robtimus.filesystems.ftp.FTPLogger.drainedPoolForKeepAlive;
import static com.github.robtimus.filesystems.ftp.FTPLogger.failedToCreatePool;
import static com.github.robtimus.filesystems.ftp.FTPLogger.increasedRefCount;
import static com.github.robtimus.filesystems.ftp.FTPLogger.returnedBrokenClient;
import static com.github.robtimus.filesystems.ftp.FTPLogger.returnedClient;
import static com.github.robtimus.filesystems.ftp.FTPLogger.tookClient;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;

/**
 * A pool of FTP clients, allowing multiple commands to be executed concurrently.
 *
 * @author Rob Spoor
 */
final class FTPClientPool {

    private static final Logger LOGGER = createLogger(FTPClientPool.class);

    private static final AtomicLong CLIENT_COUNTER = new AtomicLong();

    private final String hostname;
    private final int port;

    private final FTPEnvironment env;
    private final FileSystemExceptionFactory exceptionFactory;

    private final BlockingQueue<Client> pool;
    private final long poolWaitTimeout;

    FTPClientPool(String hostname, int port, FTPEnvironment env) throws IOException {
        this.hostname = hostname;
        this.port = port;
        this.env = env.clone();
        this.exceptionFactory = env.getExceptionFactory();
        final int poolSize = env.getClientConnectionCount();
        this.pool = new ArrayBlockingQueue<>(poolSize);
        this.poolWaitTimeout = env.getClientConnectionWaitTimeout();

        creatingPool(LOGGER, hostname, port, poolSize, poolWaitTimeout);
        fillPool(hostname, port, poolSize);
    }

    @SuppressWarnings("resource")
    private void fillPool(String hostname, int port, final int poolSize) throws IOException {
        try {
            for (int i = 0; i < poolSize; i++) {
                pool.add(new Client(true));
            }
            createdPool(LOGGER, hostname, port, poolSize);
        } catch (IOException e) {
            // creating the pool failed, disconnect all clients
            failedToCreatePool(LOGGER, e);
            for (Client client : pool) {
                try {
                    client.disconnect();
                } catch (IOException e2) {
                    e.addSuppressed(e2);
                }
            }
            throw e;
        }
    }

    Client get() throws IOException {
        try {
            Client client = getWithinTimeout();
            try {
                tookClient(LOGGER, client.clientId, pool.size());
                if (!client.isConnected()) {
                    clientNotConnected(LOGGER, client.clientId);
                    client = new Client(true);
                }
            } catch (final Exception e) {
                // could not create a new client; re-add the broken client to the pool to prevent pool starvation
                pool.add(client);
                returnedBrokenClient(LOGGER, client.clientId, pool.size());
                throw e;
            }
            client.increaseRefCount();
            return client;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            InterruptedIOException iioe = new InterruptedIOException(e.getMessage());
            iioe.initCause(e);
            throw iioe;
        }
    }

    private Client getWithinTimeout() throws InterruptedException, IOException {
        if (poolWaitTimeout == 0) {
            return pool.take();
        }
        Client client = pool.poll(poolWaitTimeout, TimeUnit.MILLISECONDS);
        if (client == null) {
            throw new IOException(FTPMessages.clientConnectionWaitTimeoutExpired());
        }
        return client;
    }

    Client getOrCreate() throws IOException {
        Client client = pool.poll();
        if (client == null) {
            // nothing was taken from the pool, so no risk of pool starvation if creating the client fails
            return new Client(false);
        }
        try {
            tookClient(LOGGER, client.clientId, pool.size());
            if (!client.isConnected()) {
                clientNotConnected(LOGGER, client.clientId);
                client = new Client(true);
            }
        } catch (final Exception e) {
            // could not create a new client; re-add the broken client to the pool to prevent pool starvation
            pool.add(client);
            returnedBrokenClient(LOGGER, client.clientId, pool.size());
            throw e;
        }
        client.increaseRefCount();
        return client;
    }

    void keepAlive() throws IOException {
        List<Client> clients = new ArrayList<>();
        pool.drainTo(clients);
        drainedPoolForKeepAlive(LOGGER);

        IOException exception = null;
        for (Client client : clients) {
            try {
                client.keepAlive();
            } catch (IOException e) {
                exception = add(exception, e);
            } finally {
                returnToPool(client);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    boolean isSecure() {
        return env instanceof FTPSEnvironment;
    }

    void close() throws IOException {
        List<Client> clients = new ArrayList<>();
        pool.drainTo(clients);
        drainedPoolForClose(LOGGER);

        IOException exception = null;
        for (Client client : clients) {
            try {
                client.disconnect();
            } catch (IOException e) {
                exception = add(exception, e);
            }
        }
        if (exception != null) {
            throw exception;
        }
    }

    private IOException add(IOException existing, IOException e) {
        if (existing == null) {
            return e;
        }
        existing.addSuppressed(e);
        return existing;
    }

    private void returnToPool(Client client) {
        assert client.refCount == 0;

        pool.add(client);
        returnedClient(LOGGER, client.clientId, pool.size());
    }

    final class Client implements Closeable {

        private final String clientId;

        private final FTPClient client;
        private final boolean pooled;

        private FileType fileType;
        private FileStructure fileStructure;
        private FileTransferMode fileTransferMode;

        private int refCount = 0;

        private Client(boolean pooled) throws IOException {
            this.clientId = "client-" + CLIENT_COUNTER.incrementAndGet(); //$NON-NLS-1$

            this.client = env.createClient(hostname, port);
            this.pooled = pooled;

            this.fileType = env.getDefaultFileType();
            this.fileStructure = env.getDefaultFileStructure();
            this.fileTransferMode = env.getDefaultFileTransferMode();

            createdClient(LOGGER, clientId, pooled);
            addCommandListener(LOGGER, client);
        }

        private void increaseRefCount() {
            refCount++;
            increasedRefCount(LOGGER, clientId, refCount);
        }

        private int decreaseRefCount() {
            if (refCount > 0) {
                refCount--;
                decreasedRefCount(LOGGER, clientId, refCount);
            }
            return refCount;
        }

        private void keepAlive() throws IOException {
            client.sendNoOp();
        }

        private boolean isConnected() {
            if (client.isConnected()) {
                try {
                    keepAlive();
                    return true;
                } catch (@SuppressWarnings("unused") IOException e) {
                    // the keep alive failed - treat as not connected, and actually disconnect quietly
                    disconnectQuietly();
                }
            }
            return false;
        }

        private void disconnect() throws IOException {
            client.disconnect();
            disconnectedClient(LOGGER, clientId);
        }

        private void disconnectQuietly() {
            try {
                disconnect();
            } catch (@SuppressWarnings("unused") IOException e) {
                // ignore
            }
        }

        @Override
        public void close() throws IOException {
            if (decreaseRefCount() == 0) {
                if (pooled) {
                    returnToPool(this);
                } else {
                    disconnect();
                }
            }
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

        @SuppressWarnings("resource")
        InputStream newInputStream(FTPPath path, OpenOptions options) throws IOException {
            assert options.read;

            applyTransferOptions(options);

            InputStream in = client.retrieveFileStream(path.path());
            if (in == null) {
                throw exceptionFactory.createNewInputStreamException(path.path(), client.getReplyCode(), client.getReplyString());
            }
            increaseRefCount();
            return new FTPInputStream(path, in, options.deleteOnClose);
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
                createdInputStream(LOGGER, clientId, path.path());
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
                        finalizeStream();
                    }
                    if (deleteOnClose) {
                        delete(path, false);
                    }
                    closedInputStream(LOGGER, clientId, path.path());
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
            increaseRefCount();
            return new FTPOutputStream(path, out, options.deleteOnClose);
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
                createdOutputStream(LOGGER, clientId, path.path());
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
                        finalizeStream();
                    }
                    if (deleteOnClose) {
                        delete(path, false);
                    }
                    closedOutputStream(LOGGER, clientId, path.path());
                }
            }
        }

        private void finalizeStream() throws IOException {
            assert refCount > 0;

            try {
                if (!client.completePendingCommand()) {
                    throw new FTPFileSystemException(client.getReplyCode(), client.getReplyString());
                }
            } finally {
                close();
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
