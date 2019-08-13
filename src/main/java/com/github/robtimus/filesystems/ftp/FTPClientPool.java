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
import java.nio.file.OpenOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A pool of FTP clients, allowing multiple commands to be executed concurrently.
 *
 * @author Rob Spoor
 */
final class FTPClientPool {

    private final Logger log = LoggerFactory.getLogger(FTPClientPool.class);

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
        log.debug("Creating FTPClientPool to {}:{} with clientConnectionCount: {} and clientConnectionWaitTimeout: {}",
                this.hostname, this.port, poolSize, this.poolWaitTimeout);

        try {
            for (int i = 0; i < poolSize; i++) {
                pool.add(new Client(true));
            }
            log.debug("FTPClientPool created with {} clients in pool.", poolSize);
        } catch (IOException e) {
            // creating the pool failed, disconnect all clients
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
                if (!client.isConnected()) {
                    Client newClient = new Client(true);
                    log.debug("Created new client {} since client {} isn't connected.", newClient, client);
                    client = newClient;
                }
            } catch (final Exception e) {
                // could not create a new client; re-add the broken client to the pool to prevent pool starvation
                pool.add(client);
                log.debug("Broken client {} has been re-add to the pool to prevent pool starvation, current pool size: {}.", client, pool.size());
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
        Client client;
        if (poolWaitTimeout == 0) {
            client = pool.take();
        } else {
            client = pool.poll(poolWaitTimeout, TimeUnit.MILLISECONDS);
            if (client == null) {
                throw new IOException(FTPMessages.clientConnectionWaitTimeoutExpired());
            }
        }
        log.debug("Client {} has been taken from pool, current pool size: {}", client, pool.size());
        return client;
    }

    Client getOrCreate() throws IOException {
        Client client = pool.poll();
        if (client == null) {
            // nothing was taken from the pool, so no risk of pool starvation if creating the client fails
            return new Client(false);
        }
        try {
            if (!client.isConnected()) {
                client = new Client(true);
            }
        } catch (final Exception e) {
            // could not create a new client; re-add the broken client to the pool to prevent pool starvation
            pool.add(client);
            throw e;
        }
        client.increaseRefCount();
        return client;
    }

    void keepAlive() throws IOException {
        List<Client> clients = new ArrayList<>();
        pool.drainTo(clients);

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
        log.debug("Client {} has been returned to pool, current pool size: {}.", client, pool.size());
    }

    private static final AtomicInteger clientCounter = new AtomicInteger(0);

    final class Client implements Closeable {

        private final Logger log = LoggerFactory.getLogger(Client.class);

        private final FTPClient client;
        private final boolean pooled;

        private final String clientId = "client-" + FTPClientPool.clientCounter.incrementAndGet();

        private FileType fileType;
        private FileStructure fileStructure;
        private FileTransferMode fileTransferMode;

        private int refCount = 0;

        @Override
        public String toString() {
            return clientId + (pooled ? " (pooled)" : " (non-pooled");
        }

        private Client(boolean pooled) throws IOException {
            this.client = env.createClient(hostname, port);
            this.pooled = pooled;

            this.fileType = env.getDefaultFileType();
            this.fileStructure = env.getDefaultFileStructure();
            this.fileTransferMode = env.getDefaultFileTransferMode();

            log.debug("Client {} has been created.", this);
        }

        private void increaseRefCount() {
            refCount++;
            log.debug("Referenced count of {} has been increased to {}.", this, refCount);
        }

        private int decreaseRefCount() {
            if (refCount > 0) {
                refCount--;
            }
            log.debug("Referenced count of {} has been decreased to {}.", this, refCount);
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
        }

        private void disconnectQuietly() {
            try {
                client.disconnect();
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
        InputStream newInputStream(String path, OpenOptions options) throws IOException {
            assert options.read;

            applyTransferOptions(options);

            InputStream in = client.retrieveFileStream(path);
            if (in == null) {
                throw exceptionFactory.createNewInputStreamException(path, client.getReplyCode(), client.getReplyString());
            }
            increaseRefCount();
            return new FTPInputStream(path, in, options.deleteOnClose);
        }

        private final class FTPInputStream extends InputStream {

            private final String path;
            private final InputStream in;
            private final boolean deleteOnClose;

            private boolean open = true;

            private FTPInputStream(String path, InputStream in, boolean deleteOnClose) {
                this.path = path;
                this.in = in;
                this.deleteOnClose = deleteOnClose;
                log.debug("FTPInputStream to \"{}\" of {} has been created.", path, Client.this);
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
                    in.close();
                    open = false;
                    finalizeStream();
                    if (deleteOnClose) {
                        delete(path, false);
                    }
                }
                log.debug("FTPInputStream to \"{}\" of {} has been closed.", path, Client.this);
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
        OutputStream newOutputStream(String path, OpenOptions options) throws IOException {
            assert options.write;

            applyTransferOptions(options);

            OutputStream out = options.append ? client.appendFileStream(path) : client.storeFileStream(path);
            if (out == null) {
                throw exceptionFactory.createNewOutputStreamException(path, client.getReplyCode(), client.getReplyString(), options.options);
            }
            increaseRefCount();
            return new FTPOutputStream(path, out, options.deleteOnClose);
        }

        private final class FTPOutputStream extends OutputStream {

            private final String path;
            private final OutputStream out;
            private final boolean deleteOnClose;

            private boolean open = true;

            private FTPOutputStream(String path, OutputStream out, boolean deleteOnClose) {
                this.path = path;
                this.out = out;
                this.deleteOnClose = deleteOnClose;
                log.debug("FTPOutputStream to \"{}\" of {} has been created.", path, Client.this);
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
                    out.close();
                    open = false;
                    finalizeStream();
                    if (deleteOnClose) {
                        delete(path, false);
                    }
                }
                log.debug("FTPOutputStream to \"{}\" of {} has been closed.", path, Client.this);
            }
        }

        private void finalizeStream() throws IOException {
            assert refCount > 0;

            if (!client.completePendingCommand()) {
                throw new FTPFileSystemException(client.getReplyCode(), client.getReplyString());
            }
            if (decreaseRefCount() == 0) {
                if (pooled) {
                    returnToPool(Client.this);
                } else {
                    disconnect();
                }
            }
        }

        void storeFile(String path, InputStream local, TransferOptions options, Collection<? extends OpenOption> openOptions) throws IOException {
            applyTransferOptions(options);

            if (!client.storeFile(path, local)) {
                throw exceptionFactory.createNewOutputStreamException(path, client.getReplyCode(), client.getReplyString(), openOptions);
            }
        }

        void mkdir(String path) throws IOException {
            if (!client.makeDirectory(path)) {
                throw exceptionFactory.createCreateDirectoryException(path, client.getReplyCode(), client.getReplyString());
            }
        }

        void delete(String path, boolean isDirectory) throws IOException {
            boolean success = isDirectory ? client.removeDirectory(path) : client.deleteFile(path);
            if (!success) {
                throw exceptionFactory.createDeleteException(path, client.getReplyCode(), client.getReplyString(), isDirectory);
            }
        }

        void rename(String source, String target) throws IOException {
            if (!client.rename(source, target)) {
                throw exceptionFactory.createMoveException(source, target, client.getReplyCode(), client.getReplyString());
            }
        }

        Calendar mdtm(String path) throws IOException {
            FTPFile file = client.mdtmFile(path);
            return file == null ? null : file.getTimestamp();
        }
    }
}
