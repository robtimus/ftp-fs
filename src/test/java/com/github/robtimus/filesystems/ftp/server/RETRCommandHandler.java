/*
 * RETRCommandHandler.java
 * Copyright 2026 Rob Spoor
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

package com.github.robtimus.filesystems.ftp.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.ReplyCodes;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.core.session.SessionKeys;
import org.mockftpserver.core.util.IoUtil;
import org.mockftpserver.fake.command.RetrCommandHandler;
import org.mockftpserver.fake.filesystem.FileEntry;
import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockftpserver.fake.filesystem.FileSystemException;

/**
 * A command handler for RETR that starts at the restart offset stored by {@link RESTCommandHandler}, and follows links to files.
 */
@SuppressWarnings("nls")
public class RETRCommandHandler extends RetrCommandHandler {

    private final int finalReplyCode;

    /**
     * Creates a new RETR command handler that replies 226 after the file.
     */
    public RETRCommandHandler() {
        this(ReplyCodes.TRANSFER_DATA_FINAL_OK);
    }

    /**
     * Creates a new RETR command handler.
     *
     * @param finalReplyCode The reply code to send after the file, to simulate failed or aborted transfers.
     */
    public RETRCommandHandler(int finalReplyCode) {
        this.finalReplyCode = finalReplyCode;
    }

    @Override
    protected void handle(Command command, Session session) {
        // code mostly copied from RetrCommandHandler.handle, but with support for the restart offset and links to files

        Long offset = (Long) session.getAttribute(RESTCommandHandler.RESTART_OFFSET);
        session.removeAttribute(RESTCommandHandler.RESTART_OFFSET);

        verifyLoggedIn(session);
        this.replyCodeForFileSystemException = ReplyCodes.READ_FILE_ERROR;

        String path = getRealPath(session, command.getRequiredParameter(0));
        FileSystemEntry entry = getFileSystem().getEntry(path);
        if (entry instanceof SymbolicLinkEntry) {
            entry = ((SymbolicLinkEntry) entry).resolve();
        }
        verifyFileSystemCondition(entry != null, path, "filesystem.doesNotExist");
        verifyFileSystemCondition(!entry.isDirectory(), path, "filesystem.isNotAFile");
        FileEntry fileEntry = (FileEntry) entry;

        // User must have read permission to the file
        verifyReadPermission(session, path);

        // User must have execute permission to the parent directory
        verifyExecutePermission(session, getFileSystem().getParent(path));

        sendReply(session, ReplyCodes.TRANSFER_DATA_INITIAL_OK);
        session.openDataConnection();
        byte[] bytes;
        try (InputStream input = fileEntry.createInputStream()) {
            bytes = IoUtil.readBytes(input);
        } catch (IOException e) {
            throw new FileSystemException(fileEntry.getPath(), null, e);
        }

        if (session.getAttribute(SessionKeys.ASCII_TYPE) != Boolean.FALSE) {
            bytes = convertLfToCrLf(bytes);
        }
        int start = offset == null ? 0 : (int) Math.min(offset, bytes.length);
        session.sendData(Arrays.copyOfRange(bytes, start, bytes.length), bytes.length - start);
        session.closeDataConnection();
        sendReply(session, finalReplyCode);
    }
}
