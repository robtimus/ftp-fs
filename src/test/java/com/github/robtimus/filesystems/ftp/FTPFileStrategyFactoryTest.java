/*
 * FTPFileStrategyFactoryTest.java
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

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import org.junit.jupiter.api.Test;

class FTPFileStrategyFactoryTest {

    @Test
    void testUnix() {
        assertSame(FTPFileStrategy.unix(), FTPFileStrategyFactory.UNIX.createFTPFileStrategy());
    }

    @Test
    void testNonUnix() {
        assertSame(FTPFileStrategy.nonUnix(), FTPFileStrategyFactory.NON_UNIX.createFTPFileStrategy());
    }

    @Test
    void testAutoDetect() {
        FTPFileStrategy autoDetect = FTPFileStrategy.autoDetect();
        FTPFileStrategy created = FTPFileStrategyFactory.AUTO_DETECT.createFTPFileStrategy();
        assertNotSame(autoDetect, created);
        assertSame(autoDetect.getClass(), created.getClass());
    }
}
