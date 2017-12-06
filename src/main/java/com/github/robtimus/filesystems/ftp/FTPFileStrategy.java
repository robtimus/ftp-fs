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

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;
import com.github.robtimus.filesystems.ftp.FTPClientPool.Client;

/**
 * A strategy for handling FTP files in an FTP server specific way. This will help support FTP servers that return the current directory (.) when
 * listing directories, and FTP servers that don't.
 *
 * @author Rob Spoor
 */
abstract class FTPFileStrategy {

    abstract List<FTPFile> getChildren(Client client, FTPPath path) throws IOException;

    abstract FTPFile getFTPFile(Client client, FTPPath path) throws IOException;

    abstract FTPFile getLink(Client client, FTPFile ftpFile, FTPPath path) throws IOException;

    static FTPFileStrategy getInstance(Client client, boolean supportAbsoluteFilePaths) throws IOException {
        if (!supportAbsoluteFilePaths) {
            // NonUnix uses the parent directory to list files
            return NonUnix.INSTANCE;
        }

        FTPFile[] ftpFiles = client.listFiles("/", new FTPFileFilter() { //$NON-NLS-1$
            @Override
            public boolean accept(FTPFile ftpFile) {
                String fileName = FTPFileSystem.getFileName(ftpFile);
                return FTPFileSystem.CURRENT_DIR.equals(fileName);
            }
        });
        return ftpFiles.length == 0 ? NonUnix.INSTANCE : Unix.INSTANCE;
    }

    private static final class Unix extends FTPFileStrategy {

        private static final FTPFileStrategy INSTANCE = new Unix();

        @Override
        List<FTPFile> getChildren(Client client, FTPPath path) throws IOException {

            FTPFile[] ftpFiles = client.listFiles(path.path());

            if (ftpFiles.length == 0) {
                throw new NoSuchFileException(path.path());
            }
            boolean isDirectory = false;
            List<FTPFile> children = new ArrayList<>(ftpFiles.length);
            for (FTPFile ftpFile : ftpFiles) {
                String fileName = FTPFileSystem.getFileName(ftpFile);
                if (FTPFileSystem.CURRENT_DIR.equals(fileName)) {
                    isDirectory = true;
                } else if (!FTPFileSystem.PARENT_DIR.equals(fileName)) {
                    children.add(ftpFile);
                }
            }

            if (!isDirectory) {
                throw new NotDirectoryException(path.path());
            }

            return children;
        }

        @Override
        FTPFile getFTPFile(Client client, FTPPath path) throws IOException {
            final String name = path.fileName();

            FTPFile[] ftpFiles = client.listFiles(path.path(), new FTPFileFilter() {
                @Override
                public boolean accept(FTPFile ftpFile) {
                    String fileName = FTPFileSystem.getFileName(ftpFile);
                    return FTPFileSystem.CURRENT_DIR.equals(fileName) || (name != null && name.equals(fileName));
                }
            });
            client.throwIfEmpty(path.path(), ftpFiles);
            if (ftpFiles.length == 1) {
                return ftpFiles[0];
            }
            for (FTPFile ftpFile : ftpFiles) {
                if (FTPFileSystem.CURRENT_DIR.equals(FTPFileSystem.getFileName(ftpFile))) {
                    return ftpFile;
                }
            }
            throw new IllegalStateException();
        }

        @Override
        FTPFile getLink(Client client, FTPFile ftpFile, FTPPath path) throws IOException {
            if (ftpFile.getLink() != null) {
                return ftpFile;
            }
            if (ftpFile.isDirectory() && FTPFileSystem.CURRENT_DIR.equals(FTPFileSystem.getFileName(ftpFile))) {
                // The file is returned using getFTPFile, which returns the . (current directory) entry for directories.
                // List the parent (if any) instead.

                final String parentPath = path.toAbsolutePath().parentPath();
                final String name = path.fileName();

                if (parentPath == null) {
                    // path is /, there is no link
                    return null;
                }

                FTPFile[] ftpFiles = client.listFiles(parentPath, new FTPFileFilter() {
                    @Override
                    public boolean accept(FTPFile ftpFile) {
                        return (ftpFile.isDirectory() || ftpFile.isSymbolicLink()) && name.equals(FTPFileSystem.getFileName(ftpFile));
                    }
                });
                client.throwIfEmpty(path.path(), ftpFiles);
                return ftpFiles[0].getLink() == null ? null : ftpFiles[0];
            }
            return null;
        }
    }

    private static final class NonUnix extends FTPFileStrategy {

        private static final FTPFileStrategy INSTANCE = new NonUnix();

        @Override
        List<FTPFile> getChildren(Client client, FTPPath path) throws IOException {

            FTPFile[] ftpFiles = client.listFiles(path.path());

            boolean isDirectory = false;
            List<FTPFile> children = new ArrayList<>(ftpFiles.length);
            for (FTPFile ftpFile : ftpFiles) {
                String fileName = FTPFileSystem.getFileName(ftpFile);
                if (FTPFileSystem.CURRENT_DIR.equals(fileName)) {
                    isDirectory = true;
                } else if (!FTPFileSystem.PARENT_DIR.equals(fileName)) {
                    children.add(ftpFile);
                }
            }

            if (!isDirectory && children.size() <= 1) {
                // either zero or one, check the parent to see if the path exists and is a directory
                FTPPath currentPath = path;
                FTPFile currentFtpFile = getFTPFile(client, currentPath);
                while (currentFtpFile.isSymbolicLink()) {
                    currentPath = path.resolve(currentFtpFile.getLink());
                    currentFtpFile = getFTPFile(client, currentPath);
                }
                if (!currentFtpFile.isDirectory()) {
                    throw new NotDirectoryException(path.path());
                }
            }

            return children;
        }

        @Override
        FTPFile getFTPFile(Client client, FTPPath path) throws IOException {
            final String parentPath = path.toAbsolutePath().parentPath();
            final String name = path.fileName();

            if (parentPath == null) {
                // path is /, but that cannot be listed
                FTPFile rootFtpFile = new FTPFile();
                rootFtpFile.setName("/"); //$NON-NLS-1$
                rootFtpFile.setType(FTPFile.DIRECTORY_TYPE);
                return rootFtpFile;
            }

            FTPFile[] ftpFiles = client.listFiles(parentPath, new FTPFileFilter() {
                @Override
                public boolean accept(FTPFile ftpFile) {
                    return name.equals(FTPFileSystem.getFileName(ftpFile));
                }
            });
            if (ftpFiles.length == 0) {
                throw new NoSuchFileException(path.path());
            }
            if (ftpFiles.length == 1) {
                return ftpFiles[0];
            }
            throw new IllegalStateException();
        }

        @Override
        FTPFile getLink(Client client, FTPFile ftpFile, FTPPath path) throws IOException {
            // getFTPFile always returns the entry in the parent, so there's no need to list the parent here.
            return ftpFile.getLink() == null ? null : ftpFile;
        }
    }
}
