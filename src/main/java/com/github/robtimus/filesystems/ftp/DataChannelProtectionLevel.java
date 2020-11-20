/*
 * DataChannelProtectionLevel.java
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

import java.io.IOException;
import org.apache.commons.net.ftp.FTPSClient;

/**
 * The possible data channel protection levels.
 * See <a href="http://tools.ietf.org/html/rfc2228#section-3">RFC 2228, section 3</a> for more information.
 *
 * @author Rob Spoor
 * @since 2.2
 */
public enum DataChannelProtectionLevel {
    /** Indicates that the data channel will carry the raw data of the file transfer, with no security applied. */
    CLEAR("C"), //$NON-NLS-1$
    /** Indicates that the data will be integrity protected. */
    SAFE("S"), //$NON-NLS-1$
    /** Indicates that the data will be confidentiality protected. */
    CONFIDENTIAL("E"), //$NON-NLS-1$
    /** Indicates that the data will be integrity and confidentiality protected. */
    PRIVATE("P"), //$NON-NLS-1$
    ;

    private final String prot;

    DataChannelProtectionLevel(String prot) {
        this.prot = prot;
    }

    void apply(FTPSClient client) throws IOException {
        client.execPROT(prot);
    }
}
