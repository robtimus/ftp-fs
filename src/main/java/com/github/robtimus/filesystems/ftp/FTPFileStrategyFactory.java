/*
 * FTPFileStrategyFactory.java
 * Copyright 2019 Rob Spoor
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
 * A factory for creating {@link FTPFileStrategy} objects.
 *
 * @author Rob Spoor
 */
public interface FTPFileStrategyFactory {

    /**
     * An {@link FTPFileStrategy} factory that delegates to {@link FTPFileStrategy#unix()}.
     */
    FTPFileStrategyFactory UNIX = new FTPFileStrategyFactory() {
        @Override
        public FTPFileStrategy createFTPFileStrategy() {
            return FTPFileStrategy.unix();
        }
    };

    /**
     * An {@link FTPFileStrategy} factory that delegates to {@link FTPFileStrategy#nonUnix()}.
     */
    FTPFileStrategyFactory NON_UNIX = new FTPFileStrategyFactory() {
        @Override
        public FTPFileStrategy createFTPFileStrategy() {
            return FTPFileStrategy.nonUnix();
        }
    };

    /**
     * An {@link FTPFileStrategy} factory that delegates to {@link FTPFileStrategy#autoDetect()}.
     */
    FTPFileStrategyFactory AUTO_DETECT = new FTPFileStrategyFactory() {
        @Override
        public FTPFileStrategy createFTPFileStrategy() {
            return FTPFileStrategy.autoDetect();
        }
    };

    /**
     * Creates an {@link FTPFileStrategy}. This {@code FTPFileStrategy} will be tied to a specific FTP file system.
     * <p>
     * Note: it is allowed to return shared instances if these do not contain any state that is specific to an FTP file system.
     *
     * @return The created {@code FTPFileStrategy}.
     */
    FTPFileStrategy createFTPFileStrategy();
}
