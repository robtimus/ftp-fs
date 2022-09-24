/*
 * FTPSFileSystemProvider.java
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

import java.util.Map;

/**
 * A provider for FTPS file systems.
 *
 * @author Rob Spoor
 */
public class FTPSFileSystemProvider extends FTPFileSystemProvider {

    /**
     * Returns the URI scheme that identifies this provider: {@code ftps}.
     */
    @Override
    public String getScheme() {
        return "ftps"; //$NON-NLS-1$
    }

    @Override
    FTPSEnvironment copy(Map<String, ?> env) {
        return FTPSEnvironment.copy(env);
    }
}
