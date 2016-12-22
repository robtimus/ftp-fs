/*
 * FileStructure.java
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

import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.OpenOption;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

/**
 * The possible FTP file structures.
 *
 * @author Rob Spoor
 */
public enum FileStructure implements OpenOption, CopyOption {
    /** Indicates that files are to be treated as a continuous sequence of bytes. */
    FILE(FTP.FILE_STRUCTURE),
    /** Indicates that files are to be treated as a sequence of records. */
    RECORD(FTP.RECORD_STRUCTURE),
    /** Indicates that files are to be treated as a set of independent indexed pages. */
    PAGE(FTP.PAGE_STRUCTURE),
    ;

    private final int structure;

    FileStructure(int structure) {
        this.structure = structure;
    }

    void apply(FTPClient client) throws IOException {
        if (!client.setFileStructure(structure)) {
            throw new FTPFileSystemException(client.getReplyCode(), client.getReplyString());
        }
    }
}
