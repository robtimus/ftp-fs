/*
 * DefaultFileSystemExceptionFactory.java
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

import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.util.Collection;

/**
 * A default {@link FileSystemExceptionFactory} that always returns an {@link FTPFileSystemException} unless specified otherwise.
 *
 * @author Rob Spoor
 */
public class DefaultFileSystemExceptionFactory implements FileSystemExceptionFactory {

    static final DefaultFileSystemExceptionFactory INSTANCE = new DefaultFileSystemExceptionFactory();

    /**
     * {@inheritDoc}
     * <p>
     * This default implementation does not return an {@link FTPFileSystemException}, but a {@link NoSuchFileException} instead.
     */
    @Override
    public FileSystemException createGetFileException(String file, int replyCode, String replyString) {
        return new NoSuchFileException(file);
    }

    @Override
    public FileSystemException createChangeWorkingDirectoryException(String directory, int replyCode, String replyString) {
        return new FTPFileSystemException(directory, replyCode, replyString);
    }

    @Override
    public FileSystemException createCreateDirectoryException(String directory, int replyCode, String replyString) {
        return new FTPFileSystemException(directory, replyCode, replyString);
    }

    @Override
    public FileSystemException createDeleteException(String file, int replyCode, String replyString, boolean isDirectory) {
        return new FTPFileSystemException(file, replyCode, replyString);
    }

    @Override
    public FileSystemException createNewInputStreamException(String file, int replyCode, String replyString) {
        return new FTPFileSystemException(file, replyCode, replyString);
    }

    @Override
    public FileSystemException createNewOutputStreamException(String file, int replyCode, String replyString,
            Collection<? extends OpenOption> options) {
        return new FTPFileSystemException(file, replyCode, replyString);
    }

    @Override
    public FileSystemException createCopyException(String file, String other, int replyCode, String replyString) {
        return new FTPFileSystemException(file, other, replyCode, replyString);
    }

    @Override
    public FileSystemException createMoveException(String file, String other, int replyCode, String replyString) {
        return new FTPFileSystemException(file, other, replyCode, replyString);
    }
}
