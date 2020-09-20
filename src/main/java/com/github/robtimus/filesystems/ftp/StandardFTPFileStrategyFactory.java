/*
 * StandardFTPFileStrategyFactory.java
 * Copyright 2020 Rob Spoor
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
 * Defines the standard {@link FTPFileStrategy} factories.
 *
 * @author Rob Spoor
 * @since 2.0
 */
public enum StandardFTPFileStrategyFactory implements FTPFileStrategyFactory {

    /**
     * An {@link FTPFileStrategy} factory that delegates to {@link FTPFileStrategy#unix()}.
     */
    UNIX(FTPFileStrategy::unix),

    /**
     * An {@link FTPFileStrategy} factory that delegates to {@link FTPFileStrategy#nonUnix()}.
     */
    NON_UNIX(FTPFileStrategy::nonUnix),

    /**
     * An {@link FTPFileStrategy} factory that delegates to {@link FTPFileStrategy#autoDetect()}.
     */
    AUTO_DETECT(FTPFileStrategy::autoDetect),
    ;

    private final FTPFileStrategyFactory delegate;

    StandardFTPFileStrategyFactory(FTPFileStrategyFactory supplier) {
        this.delegate = supplier;
    }

    @Override
    public FTPFileStrategy createFTPFileStrategy() {
        return delegate.createFTPFileStrategy();
    }
}
