/*
 * OpenOptionsTest.java
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

import static com.github.robtimus.junit.support.ThrowableAssertions.assertChainEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.robtimus.filesystems.Messages;

class OpenOptionsTest {

    @Nested
    class ForNewInputStream {

        @Test
        void testWithNoOptions() {
            OpenOptions options = OpenOptions.forNewInputStream();
            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithRead() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.DELETE_ON_CLOSE);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReadAndDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithSparse() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.SPARSE);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReadAndSparse() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, StandardOpenOption.SPARSE);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnCloseAndSparse() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.SPARSE);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReadAndDeleteOnCloseAndSparse() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE,
                    StandardOpenOption.SPARSE);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithSync() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.SYNC);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReadAndSync() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, StandardOpenOption.SYNC);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnCloseAndSync() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.SYNC);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReadAndDeleteOnCloseAndSync() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.SYNC);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDsync() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.DSYNC);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReadAndDsync() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, StandardOpenOption.DSYNC);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnCloseAndDsync() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.DSYNC);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReadAndDeleteOnCloseAndDsync() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE,
                    StandardOpenOption.DSYNC);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithNoFollowLinks() {
            OpenOptions options = OpenOptions.forNewInputStream(LinkOption.NOFOLLOW_LINKS);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReadAndNoFollowLinks() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnCloseAndNoFollowLinks() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReadAndDeleteOnCloseAndNoFollowLinks() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE,
                    LinkOption.NOFOLLOW_LINKS);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithFileTypeAndFileStructureAndFileTransferMode() {
            OpenOptions options = OpenOptions.forNewInputStream(StandardOpenOption.READ, StandardOpenOption.DELETE_ON_CLOSE,
                    LinkOption.NOFOLLOW_LINKS,
                    FileType.ascii(), FileStructure.FILE, FileTransferMode.STREAM);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertEquals(FileType.ascii(), options.fileType);
            assertEquals(FileStructure.FILE, options.fileStructure);
            assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
        }

        @Test
        void testWithDuplicateFileTypeAndFileStructureAndFileTransferMode() {
            OpenOptions options = OpenOptions.forNewInputStream(FileType.ascii(), FileType.ascii(),
                    FileStructure.FILE, FileStructure.FILE, FileTransferMode.STREAM, FileTransferMode.STREAM);

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertEquals(FileType.ascii(), options.fileType);
            assertEquals(FileStructure.FILE, options.fileStructure);
            assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
        }

        @Test
        void testWithWrite() {
            testWithInvalid(StandardOpenOption.WRITE);
        }

        @Test
        void testWithAppend() {
            testWithInvalid(StandardOpenOption.APPEND);
        }

        @Test
        void testWithTruncateExisting() {
            testWithInvalid(StandardOpenOption.TRUNCATE_EXISTING);
        }

        @Test
        void testWithCreate() {
            testWithInvalid(StandardOpenOption.CREATE);
        }

        @Test
        void testWithCreateNew() {
            testWithInvalid(StandardOpenOption.CREATE_NEW);
        }

        private void testWithInvalid(OpenOption option) {
            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> OpenOptions.forNewInputStream(option));
            assertChainEquals(Messages.fileSystemProvider().unsupportedOpenOption(option), exception);
        }

        @Test
        void testWithMultipleFileTypes() {
            testWithMultiples(FileType.ascii(), FileType.binary());
        }

        @Test
        void testWithMultipleFileStructures() {
            testWithMultiples(FileStructure.FILE, FileStructure.PAGE);
        }

        @Test
        void testWithMultipleFileTransferModes() {
            testWithMultiples(FileTransferMode.STREAM, FileTransferMode.BLOCK);
        }

        private void testWithMultiples(OpenOption... options) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> OpenOptions.forNewInputStream(options));
            assertChainEquals(Messages.fileSystemProvider().illegalOpenOptionCombination(options), exception);
        }
    }

    @Nested
    class ForNewOutStream {

        @Test
        void testWithNoOptions() {
            OpenOptions options = OpenOptions.forNewOutputStream();
            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertTrue(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWrite() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE);
            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithAppend() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.APPEND);
            assertFalse(options.read);
            assertTrue(options.write);
            assertTrue(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.DELETE_ON_CLOSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithAppendAndDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.APPEND, StandardOpenOption.DELETE_ON_CLOSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertTrue(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithTruncateExisting() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.TRUNCATE_EXISTING);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndTruncateExisting() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithTruncateExistingAndDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.DELETE_ON_CLOSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndTruncateExistingAndDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.DELETE_ON_CLOSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithSparse() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.SPARSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndSparse() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.SPARSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithAppendAndSparse() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.APPEND, StandardOpenOption.SPARSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertTrue(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnCloseAndSparse() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.SPARSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndDeleteOnCloseAndSparse() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE,
                    StandardOpenOption.SPARSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithSync() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.SYNC);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndSync() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.SYNC);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnCloseAndSync() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.SYNC);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndDeleteOnCloseAndSync() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE,
                    StandardOpenOption.SYNC);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDsync() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.DSYNC);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndDsync() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.DSYNC);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnCloseAndDsync() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.DELETE_ON_CLOSE, StandardOpenOption.DSYNC);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndDeleteOnCloseAndDsync() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE,
                    StandardOpenOption.DSYNC);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithNoFollowLinks() {
            OpenOptions options = OpenOptions.forNewOutputStream(LinkOption.NOFOLLOW_LINKS);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndNoFollowLinks() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnCloseAndNoFollowLinks() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.DELETE_ON_CLOSE, LinkOption.NOFOLLOW_LINKS);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndDeleteOnCloseAndNoFollowLinks() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.DELETE_ON_CLOSE,
                    LinkOption.NOFOLLOW_LINKS);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndCreateAndDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.CREATE,
                    StandardOpenOption.DELETE_ON_CLOSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertTrue(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndCreateNewAndDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.CREATE_NEW,
                    StandardOpenOption.DELETE_ON_CLOSE);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertTrue(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWriteAndCreateAndCreateNew() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, StandardOpenOption.CREATE, StandardOpenOption.CREATE_NEW);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertTrue(options.create);
            assertTrue(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithFileTypeAndFileStructureAndFileTransferMode() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, FileType.ascii(),
                    FileStructure.FILE, FileTransferMode.STREAM);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertEquals(FileType.ascii(), options.fileType);
            assertEquals(FileStructure.FILE, options.fileStructure);
            assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
        }

        @Test
        void testWithDuplicateFileTypeAndFileStructureAndFileTransferMode() {
            OpenOptions options = OpenOptions.forNewOutputStream(StandardOpenOption.WRITE, FileType.ascii(), FileType.ascii(),
                    FileStructure.FILE, FileStructure.FILE, FileTransferMode.STREAM, FileTransferMode.STREAM);

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertEquals(FileType.ascii(), options.fileType);
            assertEquals(FileStructure.FILE, options.fileStructure);
            assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
        }

        @Test
        void testWithRead() {
            testWithInvalid(StandardOpenOption.READ);
        }

        private void testWithInvalid(OpenOption option) {
            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> OpenOptions.forNewOutputStream(option));
            assertChainEquals(Messages.fileSystemProvider().unsupportedOpenOption(option), exception);
        }

        @Test
        void testWithAppendAndTruncateExisting() {
            testWithIllegalCombination(StandardOpenOption.APPEND, StandardOpenOption.TRUNCATE_EXISTING);
        }

        private void testWithIllegalCombination(OpenOption... options) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> OpenOptions.forNewOutputStream(options));
            assertChainEquals(Messages.fileSystemProvider().illegalOpenOptionCombination(options), exception);
        }

        @Test
        void testWithMultipleFileTypes() {
            testWithMultiples(FileType.ascii(), FileType.binary());
        }

        @Test
        void testWithMultipleFileStructures() {
            testWithMultiples(FileStructure.FILE, FileStructure.PAGE);
        }

        @Test
        void testWithMultipleFileTransferModes() {
            testWithMultiples(FileTransferMode.STREAM, FileTransferMode.BLOCK);
        }

        private void testWithMultiples(OpenOption... options) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> OpenOptions.forNewOutputStream(options));
            assertChainEquals(Messages.fileSystemProvider().illegalOpenOptionCombination(options), exception);
        }
    }

    @Nested
    class ForNewByteChannel {

        @Test
        void testWithNoOptions() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.noneOf(StandardOpenOption.class));
            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithRead() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.READ));
            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithWrite() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.WRITE));

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithAppend() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.APPEND));

            assertFalse(options.read);
            assertTrue(options.write);
            assertTrue(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithTruncateExisting() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.TRUNCATE_EXISTING));

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithCreate() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.CREATE));

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertTrue(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithCreateNew() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.CREATE_NEW));

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertTrue(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDeleteOnClose() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.DELETE_ON_CLOSE));

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertTrue(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithSparse() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.SPARSE));
            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithSync() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.SYNC));

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithDsync() {
            OpenOptions options = OpenOptions.forNewByteChannel(EnumSet.of(StandardOpenOption.DSYNC));

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithFileTypeAndFileStructureAndFileTransferMode() {
            OpenOptions options = OpenOptions.forNewByteChannel(new HashSet<>(Arrays.asList(StandardOpenOption.WRITE, FileType.ascii(),
                    FileStructure.FILE, FileTransferMode.BLOCK)));

            assertFalse(options.read);
            assertTrue(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertEquals(FileType.ascii(), options.fileType);
            assertEquals(FileStructure.FILE, options.fileStructure);
            assertEquals(FileTransferMode.BLOCK, options.fileTransferMode);
        }

        @Test
        void testWithDuplicateFileTypeAndFileStructureAndFileTransferMode() {
            OpenOptions options = OpenOptions.forNewByteChannel(new HashSet<>(Arrays.asList(FileType.ascii(), FileType.ascii(),
                    FileStructure.FILE, FileStructure.FILE, FileTransferMode.BLOCK, FileTransferMode.BLOCK)));

            assertTrue(options.read);
            assertFalse(options.write);
            assertFalse(options.append);
            assertFalse(options.create);
            assertFalse(options.createNew);
            assertFalse(options.deleteOnClose);
            assertEquals(FileType.ascii(), options.fileType);
            assertEquals(FileStructure.FILE, options.fileStructure);
            assertEquals(FileTransferMode.BLOCK, options.fileTransferMode);
        }

        @Test
        void testWithUnsupported() {
            testWithUnsupported(DummyOption.DUMMY);
        }

        private void testWithUnsupported(OpenOption option) {
            Set<OpenOption> openOptions = Collections.singleton(option);
            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                    () -> OpenOptions.forNewByteChannel(openOptions));
            assertChainEquals(Messages.fileSystemProvider().unsupportedOpenOption(option), exception);
        }

        @Test
        void testWithReadAndWrite() {
            testWithIllegalCombination(StandardOpenOption.READ, StandardOpenOption.WRITE);
        }

        @Test
        void testWithReadAndAppend() {
            testWithIllegalCombination(StandardOpenOption.READ, StandardOpenOption.APPEND);
        }

        @Test
        void testWithAppendAndTruncateExisting() {
            testWithIllegalCombination(StandardOpenOption.APPEND, StandardOpenOption.TRUNCATE_EXISTING);
        }

        private void testWithIllegalCombination(StandardOpenOption... options) {
            Set<StandardOpenOption> openOptions = EnumSet.of(options[0], options);
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> OpenOptions.forNewByteChannel(openOptions));
            assertChainEquals(Messages.fileSystemProvider().illegalOpenOptionCombination(options), exception);
        }

        @Test
        void testWithMultipleFileTypes() {
            testWithMultiples(FileType.ascii(), FileType.binary());
        }

        @Test
        void testWithMultipleFileStructures() {
            testWithMultiples(FileStructure.FILE, FileStructure.PAGE);
        }

        @Test
        void testWithMultipleFileTransferModes() {
            testWithMultiples(FileTransferMode.STREAM, FileTransferMode.BLOCK);
        }

        private void testWithMultiples(OpenOption... options) {
            Set<OpenOption> openOptions = new LinkedHashSet<>(Arrays.asList(options));
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> OpenOptions.forNewByteChannel(openOptions));
            assertChainEquals(Messages.fileSystemProvider().illegalOpenOptionCombination(options), exception);
        }
    }

    enum DummyOption implements OpenOption {
        DUMMY
    }
}
