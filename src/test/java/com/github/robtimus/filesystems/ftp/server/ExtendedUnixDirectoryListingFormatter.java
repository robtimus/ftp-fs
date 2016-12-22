/*
 * ExtendedUnixDirectoryListingFormatter.java
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

import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockftpserver.fake.filesystem.UnixDirectoryListingFormatter;

/**
 * An extended version of {@link UnixDirectoryListingFormatter} that supports symbolic links.
 *
 * @author Rob Spoor
 */
@SuppressWarnings("nls")
public class ExtendedUnixDirectoryListingFormatter extends UnixDirectoryListingFormatter {

    @Override
    public String format(FileSystemEntry fileSystemEntry) {
        String formatted = super.format(fileSystemEntry);
        if (fileSystemEntry instanceof SymbolicLinkEntry) {
            SymbolicLinkEntry symLink = (SymbolicLinkEntry) fileSystemEntry;
            formatted = "l" + formatted.substring(1) + " -> " + symLink.getTarget().getPath();
        }
        return formatted;
    }
}
