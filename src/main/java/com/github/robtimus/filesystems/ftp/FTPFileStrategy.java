/*
 * FTPFileStrategy.java
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

import static com.github.robtimus.filesystems.SimpleAbstractPath.CURRENT_DIR;
import static com.github.robtimus.filesystems.SimpleAbstractPath.PARENT_DIR;
import static com.github.robtimus.filesystems.SimpleAbstractPath.ROOT_PATH;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import com.github.robtimus.filesystems.ftp.FTPClientPool.Client;

/**
 * A strategy for handling FTP files in an FTP server specific way.
 * This will help support FTP servers that return the current directory (.) when listing directories, and FTP servers that don't.
 *
 * @author Rob Spoor
 */
public abstract class FTPFileStrategy {

    final List<FTPFile> getChildren(Client client, Path path) throws IOException {
        return getChildren(client.ftpClient(), normalized(path), client.exceptionFactory());
    }

    final FTPFile getFTPFile(Client client, Path path) throws IOException {
        return getFTPFile(client.ftpClient(), normalized(path), client.exceptionFactory());
    }

    final FTPFile getLink(Client client, FTPFile ftpFile, Path path) throws IOException {
        return getLink(client.ftpClient(), ftpFile, normalized(path), client.exceptionFactory());
    }

    private Path normalized(Path path) {
        // Use normalized absolute form so especially fileName and parentPath don't return odd results for paths like "" or ending with "." or ".."
        return path.toAbsolutePath().normalize();
    }

    /**
     * Initializes the FTP file strategy. This method should be called only once, before calling any other method.
     * This default implementation does nothing.
     *
     * @param client The FTP client to use for initialization.
     * @throws IOException If an I/O error occurs.
     */
    protected void initialize(FTPClient client) throws IOException {
        // does nothing
    }

    /**
     * Returns the direct children for a path.
     *
     * @param client The FTP client to use.
     * @param path The path to return the direct children for.
     * @param exceptionFactory The file system exception factory to use.
     * @return The direct children for the given path.
     * @throws NoSuchFileException If the given path does not exist.
     * @throws NotDirectoryException If the given path does not represent a directory.
     * @throws IOException If an I/O error occurs.
     */
    protected abstract List<FTPFile> getChildren(FTPClient client, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException;

    /**
     * Returns a single FTP file.
     *
     * @param client The FTP client to use.
     * @param path The path to return the matching FTP file for.
     * @param exceptionFactory The file system exception factory to use.
     * @return The FTP file matching the given path.
     * @throws NoSuchFileException If the given path does not exist.
     * @throws IOException If an I/O error occurs.
     */
    protected abstract FTPFile getFTPFile(FTPClient client, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException;

    /**
     * Returns an FTP file representing a link.
     * This can be as simple as returning the object if {@link FTPFile#getLink()} is not {@code null}, or it can be more complex.
     *
     * @param client The FTP client to use.
     * @param ftpFile The FTP file that represents the possible link.
     * @param path The path to the FTP file.
     * @param exceptionFactory The file system exception factory to use.
     * @return An FTP file representing a link if the given FTP file and path represent a link, or {@code null} if they represent a non-link.
     * @throws IOException If an I/O error occurs.
     */
    protected abstract FTPFile getLink(FTPClient client, FTPFile ftpFile, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException;

    /**
     * Returns a path's file name.
     * This method should only be called by sub classes to retrieve the file name of paths passed to their methods.
     *
     * @param path The path to return the file name of.
     * @return The file name of the given path.
     */
    protected final String fileName(Path path) {
        return ((FTPPath) path).fileName();
    }

    /**
     * Returns a path's full path.
     * This method should only be called by sub classes to retrieve the full path of paths passed to their methods.
     *
     * @param path The path to return the full path of.
     * @return The full path of the given path.
     */
    protected final String path(Path path) {
        return ((FTPPath) path).path();
    }

    /**
     * Returns a path's parent path.
     * This method should only be called by sub classes to retrieve the parent path of paths passed to their methods.
     *
     * @param path The path to return the parent path of.
     * @return The parent path of the given path, or {@code null} if the path has no parent.
     */
    protected final String parentPath(Path path) {
        return ((FTPPath) path).toAbsolutePath().parentPath();
    }

    /**
     * Throws a {@link FileSystemException} if the given array of FTP files is empty.
     * This method will delegate to {@link FileSystemExceptionFactory#createGetFileException(String, int, String)} if needed.
     *
     * @param ftpFiles The array of FTP files to check.
     * @param path The path that was used to retrieve the FTP files.
     * @param client The FTP client that was used to retrieve the FTP files.
     * @param exceptionFactory The file system exception factory to use.
     * @throws FileSystemException If the given array of FTP files is empty.
     */
    protected final void throwIfEmpty(FTPFile[] ftpFiles, Path path, FTPClient client, FileSystemExceptionFactory exceptionFactory)
            throws FileSystemException {

        if (ftpFiles.length == 0) {
            throw exceptionFactory.createGetFileException(path(path), client.getReplyCode(), client.getReplyString());
        }
    }

    /**
     * Returns a strategy for Unix-like FTP file systems.
     * It is assumed that these return an entry for the current directory (.) when listing directories.
     * It is also assumed that these support absolute paths to list files.
     *
     * @return A strategy for Unix-like FTP file systems.
     */
    public static FTPFileStrategy unix() {
        return Unix.INSTANCE;
    }

    /**
     * Returns a strategy for non-Unix-like FTP file systems.
     * It is assumed that these do not return an entry for the current directory (.) when listing directories.
     * As a result, this strategy will list a file's parent to get information about a file.
     * <p>
     * This strategy should be used for FTP file systems that do not support absolute paths to list files.
     *
     * @return A strategy for non-Unix-like FTP file systems.
     */
    public static FTPFileStrategy nonUnix() {
        return NonUnix.INSTANCE;
    }

    /**
     * Returns a strategy that will detect whether or not an FTP file system is Unix-like or not.
     * It will do so by listing the root and checking for the presence of an entry for the current directory (.).
     *
     * @return A strategy that will detect whether or not an FTP file system is Unix-like or not.
     */
    public static FTPFileStrategy autoDetect() {
        return new AutoDetect();
    }

    private static final class Unix extends FTPFileStrategy {

        private static final FTPFileStrategy INSTANCE = new Unix();

        @Override
        protected List<FTPFile> getChildren(FTPClient client, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException {
            FTPFile[] ftpFiles = client.listFiles(path(path));

            if (ftpFiles.length == 0) {
                throw new NoSuchFileException(path(path));
            }
            boolean isDirectory = false;
            List<FTPFile> children = new ArrayList<>(ftpFiles.length);
            for (FTPFile ftpFile : ftpFiles) {
                String fileName = FTPFileSystem.getFileName(ftpFile);
                if (CURRENT_DIR.equals(fileName)) {
                    isDirectory = true;
                } else if (!PARENT_DIR.equals(fileName)) {
                    children.add(ftpFile);
                }
            }

            if (!isDirectory) {
                throw new NotDirectoryException(path(path));
            }

            return children;
        }

        @Override
        protected FTPFile getFTPFile(FTPClient client, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException {
            final String name = fileName(path);

            FTPFile[] ftpFiles = client.listFiles(path(path), f -> {
                String fileName = FTPFileSystem.getFileName(f);
                return CURRENT_DIR.equals(fileName)
                        || name != null && name.equals(fileName);
            });
            throwIfEmpty(ftpFiles, path, client, exceptionFactory);
            if (ftpFiles.length == 1) {
                return ftpFiles[0];
            }
            for (FTPFile ftpFile : ftpFiles) {
                if (CURRENT_DIR.equals(FTPFileSystem.getFileName(ftpFile))) {
                    return ftpFile;
                }
            }
            throw new IllegalStateException();
        }

        @Override
        protected FTPFile getLink(FTPClient client, FTPFile ftpFile, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException {
            if (ftpFile.getLink() != null) {
                return ftpFile;
            }
            if (ftpFile.isDirectory() && CURRENT_DIR.equals(FTPFileSystem.getFileName(ftpFile))) {
                // The file is returned using getFTPFile, which returns the . (current directory) entry for directories.
                // List the parent (if any) instead.

                final String parentPath = parentPath(path);
                final String name = fileName(path);

                if (parentPath == null) {
                    // path is /, there is no link
                    return null;
                }

                FTPFile[] ftpFiles = client.listFiles(parentPath,
                        f -> (f.isDirectory() || f.isSymbolicLink()) && name.equals(FTPFileSystem.getFileName(f)));
                throwIfEmpty(ftpFiles, path, client, exceptionFactory);
                return ftpFiles[0].getLink() == null ? null : ftpFiles[0];
            }
            return null;
        }

        @Override
        @SuppressWarnings("nls")
        public String toString() {
            return "UNIX";
        }
    }

    private static final class NonUnix extends FTPFileStrategy {

        private static final FTPFileStrategy INSTANCE = new NonUnix();

        @Override
        protected List<FTPFile> getChildren(FTPClient client, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException {
            FTPFile[] ftpFiles = client.listFiles(path(path));

            boolean isDirectory = false;
            List<FTPFile> children = new ArrayList<>(ftpFiles.length);
            for (FTPFile ftpFile : ftpFiles) {
                String fileName = FTPFileSystem.getFileName(ftpFile);
                if (CURRENT_DIR.equals(fileName)) {
                    isDirectory = true;
                } else if (!PARENT_DIR.equals(fileName)) {
                    children.add(ftpFile);
                }
            }

            if (!isDirectory && children.size() <= 1) {
                // either zero or one, check the parent to see if the path exists and is a directory
                Path currentPath = path;
                FTPFile currentFtpFile = getFTPFile(client, currentPath, exceptionFactory);
                while (currentFtpFile.isSymbolicLink()) {
                    currentPath = path.resolve(currentFtpFile.getLink());
                    currentFtpFile = getFTPFile(client, currentPath, exceptionFactory);
                }
                if (!currentFtpFile.isDirectory()) {
                    throw new NotDirectoryException(path(path));
                }
            }

            return children;
        }

        @Override
        protected FTPFile getFTPFile(FTPClient client, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException {
            final String parentPath = parentPath(path);
            final String name = fileName(path);

            if (parentPath == null) {
                // path is /, but that cannot be listed
                FTPFile rootFtpFile = new FTPFile();
                rootFtpFile.setName(ROOT_PATH);
                rootFtpFile.setType(FTPFile.DIRECTORY_TYPE);
                return rootFtpFile;
            }

            FTPFile[] ftpFiles = client.listFiles(parentPath, f -> name.equals(FTPFileSystem.getFileName(f)));
            if (ftpFiles.length == 0) {
                throw new NoSuchFileException(path(path));
            }
            if (ftpFiles.length == 1) {
                return ftpFiles[0];
            }
            throw new IllegalStateException();
        }

        @Override
        protected FTPFile getLink(FTPClient client, FTPFile ftpFile, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException {
            // getFTPFile always returns the entry in the parent, so there's no need to list the parent here.
            return ftpFile.getLink() == null ? null : ftpFile;
        }

        @Override
        @SuppressWarnings("nls")
        public String toString() {
            return "NON_UNIX";
        }
    }

    private static final class AutoDetect extends FTPFileStrategy {

        private FTPFileStrategy delegate;

        @Override
        protected void initialize(FTPClient client) throws IOException {
            if (delegate != null) {
                throw new IllegalStateException(FTPMessages.autoDetectFileStrategyAlreadyInitialized());
            }

            FTPFile[] ftpFiles = client.listFiles(ROOT_PATH, f -> {
                String fileName = FTPFileSystem.getFileName(f);
                return CURRENT_DIR.equals(fileName);
            });
            delegate = ftpFiles.length == 0 ? NonUnix.INSTANCE : Unix.INSTANCE;
        }

        private void checkInitialized() {
            if (delegate == null) {
                throw new IllegalStateException(FTPMessages.autoDetectFileStrategyNotInitialized());
            }
        }

        @Override
        protected List<FTPFile> getChildren(FTPClient client, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException {
            checkInitialized();
            return delegate.getChildren(client, path, exceptionFactory);
        }

        @Override
        protected FTPFile getFTPFile(FTPClient client, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException {
            checkInitialized();
            return delegate.getFTPFile(client, path, exceptionFactory);
        }

        @Override
        protected FTPFile getLink(FTPClient client, FTPFile ftpFile, Path path, FileSystemExceptionFactory exceptionFactory) throws IOException {
            checkInitialized();
            return delegate.getLink(client, ftpFile, path, exceptionFactory);
        }

        @Override
        @SuppressWarnings("nls")
        public String toString() {
            return "AUTO_DETECT";
        }
    }
}
