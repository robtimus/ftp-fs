/*
 * FileSystemExceptionFactory.java
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

import java.nio.file.AccessDeniedException;
import java.nio.file.FileSystemException;
import java.nio.file.NoSuchFileException;
import java.nio.file.OpenOption;
import java.util.Collection;

/**
 * A factory for creating {@link FileSystemException}s based on replies from an FTP server.
 * <p>
 * It's not always possible to distinguish different types of errors. For instance, a 550 error code (file unavailable) could indicate that a file
 * does not exist (which should trigger a {@link NoSuchFileException}), or that a file is inaccessible (which should trigger a
 * {@link AccessDeniedException}), or possibly another reason.
 * This interface allows users to provide their own mapping, based on both the reply code and the reply string from an FTP reply.
 * <p>
 * Ideally implementations return exceptions that implement {@link FTPResponse}, such as {@link FTPNoSuchFileException},
 * {@link FTPAccessDeniedException} or {@link FTPFileSystemException}. This way, the original FTP reply code and message will be reserved.
 *
 * @author Rob Spoor
 */
public interface FileSystemExceptionFactory {

    /**
     * Creates a {@code FileSystemException} that indicates a file or directory cannot be retrieved.
     * <p>
     * Note that the LIST command is used to retrieve a file or directory. This will often return with a 226 code even if a file or directory cannot
     * be retrieved. This does not mean that the LIST call was actually successful.
     *
     * @param file A string identifying the file or directory.
     * @param replyCode The integer value of the reply code of the last FTP reply that triggered this method call.
     * @param replyString The entire text from the last FTP response that triggered this method call.
     * @return The created {@code FileSystemException}.
     */
    FileSystemException createGetFileException(String file, int replyCode, String replyString);

    /**
     * Creates a {@code FileSystemException} that indicates a directory cannot be used as the current working directory.
     *
     * @param directory A string identifying the directory.
     * @param replyCode The integer value of the reply code of the last FTP reply that triggered this method call.
     * @param replyString The entire text from the last FTP response that triggered this method call.
     * @return The created {@code FileSystemException}.
     */
    FileSystemException createChangeWorkingDirectoryException(String directory, int replyCode, String replyString);

    /**
     * Creates a {@code FileSystemException} that indicates a directory cannot be created.
     *
     * @param directory A string identifying the directory.
     * @param replyCode The integer value of the reply code of the last FTP reply that triggered this method call.
     * @param replyString The entire text from the last FTP response that triggered this method call.
     * @return The created {@code FileSystemException}.
     */
    FileSystemException createCreateDirectoryException(String directory, int replyCode, String replyString);

    /**
     * Creates a {@code FileSystemException} that indicates a file or directory cannot be deleted.
     *
     * @param file A string identifying the file or directory.
     * @param replyCode The integer value of the reply code of the last FTP reply that triggered this method call.
     * @param replyString The entire text from the last FTP response that triggered this method call.
     * @param isDirectory {@code true} if a directory cannot be deleted, or {@code false} if a file cannot be deleted.
     * @return The created {@code FileSystemException}.
     */
    FileSystemException createDeleteException(String file, int replyCode, String replyString, boolean isDirectory);

    /**
     * Creates a {@code FileSystemException} that indicates a file cannot be opened for reading.
     *
     * @param file A string identifying the file.
     * @param replyCode The integer value of the reply code of the last FTP reply that triggered this method call.
     * @param replyString The entire text from the last FTP response that triggered this method call.
     * @return The created {@code FileSystemException}.
     */
    FileSystemException createNewInputStreamException(String file, int replyCode, String replyString);

    /**
     * Creates a {@code FileSystemException} that indicates a file cannot be opened for writing.
     *
     * @param file A string identifying the file.
     * @param replyCode The integer value of the reply code of the last FTP reply that triggered this method call.
     * @param replyString The entire text from the last FTP response that triggered this method call.
     * @param options The open options used to open the file.
     * @return The created {@code FileSystemException}.
     */
    FileSystemException createNewOutputStreamException(String file, int replyCode, String replyString, Collection<? extends OpenOption> options);

    /**
     * Creates a {@code FileSystemException} that indicates a file or directory cannot be copied.
     *
     * @param file A string identifying the file or directory to be copied.
     * @param other A string identifying the file or directory to be copied to.
     * @param replyCode The integer value of the reply code of the last FTP reply that triggered this method call.
     * @param replyString The entire text from the last FTP response that triggered this method call.
     * @return The created {@code FileSystemException}.
     */
    FileSystemException createCopyException(String file, String other, int replyCode, String replyString);

    /**
     * Creates a {@code FileSystemException} that indicates a file or directory cannot be moved.
     *
     * @param file A string identifying the file or directory to be moved.
     * @param other A string identifying the file or directory to be moved to.
     * @param replyCode The integer value of the reply code of the last FTP reply that triggered this method call.
     * @param replyString The entire text from the last FTP response that triggered this method call.
     * @return The created {@code FileSystemException}.
     */
    FileSystemException createMoveException(String file, String other, int replyCode, String replyString);
}
