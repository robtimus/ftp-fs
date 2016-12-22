/*
 * ConnectionMode.java
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

import org.apache.commons.net.ftp.FTPClient;

/**
 * The possible FTP connection modes. Note that server-to-server is not supported.
 *
 * @author Rob Spoor
 */
public enum ConnectionMode {
    /** Indicates that FTP servers should connect to clients' data ports to initiate data transfers. */
    ACTIVE {
        @Override
        void apply(FTPClient client) {
            client.enterLocalActiveMode();
        }
    },
    /** Indicates that FTP servers are in passive mode, requiring clients to connect to the servers' data ports to initiate transfers. */
    PASSIVE {
        @Override
        void apply(FTPClient client) {
            client.enterLocalPassiveMode();
        }
    },
    ;

    abstract void apply(FTPClient client);
}
