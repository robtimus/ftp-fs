/*
 * FTPSDataChannelProtectionLevel.java
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
 * Protection level of the data channel in a FTPS communication.
 *
 * <ul>
 * <li>C - Clear</li>
 * <li>S - Safe</li>
 * <li>E - Confidential</li>
 * <li>P - Private</li>
 * </ul>
 *
 * @see <a href="http://tools.ietf.org/html/rfc2228#section-3">RFC 2228, section 3</a>
 * @author Joerg Schaible
 */
public enum FTPSDataChannelProtectionLevel
{
    /** Clear. */
    C,
    /** Safe. */
    S,
    /** Confidential. */
    E,
    /** Private. */
    P;
}
