/*
 * FileTransferMode.java
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
 * The possible FTP file transfer modes.
 *
 * @author Rob Spoor
 */
public enum FileTransferMode implements OpenOption, CopyOption {
    /**
     * Indicates that files are to be transfered as streams of bytes.
     */
    STREAM(FTP.STREAM_TRANSFER_MODE),
    /**
     * Indicates that files are to be transfered as series of blocks.
     */
    BLOCK(FTP.BLOCK_TRANSFER_MODE),
    /**
     * Indicate that files are to be transfered as FTP compressed data.
     */
    COMPRESSED(FTP.COMPRESSED_TRANSFER_MODE),
    /**
     * Indicate that files are to be transferred as FTP (un)compressing data in the "deflate" compression format.
     *
     * @since 3.3
     */
    DEFLATE(FTP.DEFLATE_TRANSFER_MODE),
    ;

    private final int mode;

    FileTransferMode(int mode) {
        this.mode = mode;
    }

    void apply(FTPClient client) throws IOException {
        if (!client.setFileTransferMode(mode)) {
            throw new FTPFileSystemException(client.getReplyCode(), client.getReplyString());
        }
    }
}
