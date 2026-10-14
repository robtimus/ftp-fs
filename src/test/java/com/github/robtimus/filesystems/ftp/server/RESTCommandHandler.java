/*
 * RESTCommandHandler.java
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

import org.mockftpserver.core.command.Command;
import org.mockftpserver.core.command.ReplyCodes;
import org.mockftpserver.core.session.Session;
import org.mockftpserver.fake.command.AbstractFakeCommandHandler;

/**
 * A command handler for REST that stores the restart offset for {@link RETRCommandHandler}.
 */
@SuppressWarnings("nls")
public class RESTCommandHandler extends AbstractFakeCommandHandler {

    static final String RESTART_OFFSET = "restartOffset";

    @Override
    protected void handle(Command command, Session session) {
        verifyLoggedIn(session);

        long offset = Long.parseLong(command.getRequiredParameter(0));
        session.setAttribute(RESTART_OFFSET, offset);
        sendReply(session, ReplyCodes.REST_OK, "rest");
    }
}
