/*
 * MDTMCommandHandler.java
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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.ReplyCodes;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.fake.command.AbstractFakeCommandHandler;
import org.mockftpserver.fake.filesystem.FileSystemEntry;

/**
 * A command handler for the MDTM command.
 *
 * @author Rob Spoor
 */
@SuppressWarnings("nls")
public class MDTMCommandHandler extends AbstractFakeCommandHandler {

    @Override
    protected void handle(Command command, Session session) {
        verifyLoggedIn(session);

        String path = getRealPath(session, command.getParameter(0));

        verifyFileSystemCondition(getFileSystem().exists(path), path, "filesystem.doesNotExist");
        verifyReadPermission(session, path);

        FileSystemEntry entry = getFileSystem().getEntry(path);
        session.sendReply(ReplyCodes.STAT_FILE_OK, getResponse(entry.getLastModified()));
    }

    private String getResponse(Date date) {
        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        return sdf.format(date);
    }
}
