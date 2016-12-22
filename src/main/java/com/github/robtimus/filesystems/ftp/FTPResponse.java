/*
 * FTPResponse.java
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

/**
 * Represents a response from an FTP server.
 *
 * @author Rob Spoor
 */
public interface FTPResponse {

    /**
     * Returns the reply code of the FTP response.
     *
     * @return The integer value of the reply code of the FTP response.
     */
    int getReplyCode();

    /**
     * Returns the entire text from the FTP response.
     *
     * @return The entire text from the FTP response.
     */
    String getReplyString();
}
