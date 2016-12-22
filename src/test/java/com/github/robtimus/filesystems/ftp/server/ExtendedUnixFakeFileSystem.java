/*
 * ExtendedUnixFakeFileSystem.java
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

import java.util.List;
import org.mockftpserver.fake.filesystem.FileSystemEntry;
import org.mockftpserver.fake.filesystem.UnixFakeFileSystem;

/**
 * An extended version of {@link UnixFakeFileSystem} that supports symbolic links.
 *
 * @author Rob Spoor
 */
public class ExtendedUnixFakeFileSystem extends UnixFakeFileSystem {

    @SuppressWarnings("javadoc")
    public ExtendedUnixFakeFileSystem() {
        setDirectoryListingFormatter(new ExtendedUnixDirectoryListingFormatter());
    }

    private String resolveLinks(String path) {
        FileSystemEntry entry = getEntry(path);
        if (entry instanceof SymbolicLinkEntry && entry.isDirectory()) {
            return ((SymbolicLinkEntry) entry).resolve().getPath();
        }
        return path;
    }

    @Override
    public List<?> listFiles(String path) {
        return super.listFiles(resolveLinks(path));
    }

    @Override
    public List<?> listNames(String path) {
        return super.listNames(resolveLinks(path));
    }
}
