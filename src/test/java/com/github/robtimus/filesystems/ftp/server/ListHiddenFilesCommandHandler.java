/*
 * ListHiddenFilesCommandHandler.java
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

package com.github.robtimus.filesystems.ftp.server;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.ReplyCodes;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.core.util.StringUtil;
import org.mockftpserver.fake.command.ListCommandHandler;
import org.mockftpserver.fake.filesystem.DirectoryEntry;
import org.mockftpserver.fake.filesystem.FileSystemEntry;

/**
 * A command handler for LIST that supports the {@code -a} flag.
 *
 * @author Rob Spoor
 */
@SuppressWarnings("nls")
public class ListHiddenFilesCommandHandler extends ListCommandHandler {

    private final boolean includeDotEntry;

    /**
     * Creates a new LIST command handler.
     *
     * @param includeDotEntry {@code true} to include a dot entry, or {@code false} otherwise.
     */
    public ListHiddenFilesCommandHandler(boolean includeDotEntry) {
        this.includeDotEntry = includeDotEntry;
    }

    @Override
    protected void handle(Command command, Session session) {
        if (command.getParameter(0).startsWith("-a ")) {
            String path = command.getParameter(0).substring(3);
            handle(path, session);
        } else {
            super.handle(command, session);
        }
    }

    private void handle(String path, Session session) {
        // code mostly copied from ListCommandHandler.handle, but with added . entry

        verifyLoggedIn(session);

        path = getRealPath(session, path);

        // User must have read permission to the path
        if (getFileSystem().exists(path)) {
            this.replyCodeForFileSystemException = ReplyCodes.READ_FILE_ERROR;
            verifyReadPermission(session, path);
        }

        this.replyCodeForFileSystemException = ReplyCodes.SYSTEM_ERROR;
        List<?> fileEntries = getFileSystem().listFiles(path);
        Iterator<?> iter = fileEntries.iterator();
        List<String> lines = new ArrayList<>();
        while (iter.hasNext()) {
            FileSystemEntry entry = (FileSystemEntry) iter.next();
            lines.add(getFileSystem().formatDirectoryListing(entry));
        }
        FileSystemEntry entry = getFileSystem().getEntry(path);
        if (entry != null && entry.isDirectory() && includeDotEntry) {
            lines.add(0, getFileSystem().formatDirectoryListing(addDot(getFileSystem().getEntry(path))));
        }
        String result = StringUtil.join(lines, endOfLine());
        if (!result.isEmpty()) {
            result += endOfLine();
        }

        sendReply(session, ReplyCodes.TRANSFER_DATA_INITIAL_OK);

        session.openDataConnection();
        LOG.info("Sending [{}]", result);
        session.sendData(result.getBytes(), result.length());
        session.closeDataConnection();

        sendReply(session, ReplyCodes.TRANSFER_DATA_FINAL_OK);
    }

    private FileSystemEntry addDot(FileSystemEntry entry) {
        if (entry instanceof SymbolicLinkEntry) {
            entry = ((SymbolicLinkEntry) entry).resolve();
        }
        if (entry instanceof DirectoryEntry) {
            DirectoryEntry newEntry = new DirectoryEntry(entry.getPath() + "/.");
            newEntry.setLastModified(entry.getLastModified());
            newEntry.setOwner(entry.getOwner());
            newEntry.setGroup(entry.getGroup());
            newEntry.setPermissions(entry.getPermissions());
            return newEntry;
        }
        return entry;
    }
}
