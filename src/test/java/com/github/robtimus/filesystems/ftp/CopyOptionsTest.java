/*
 * CopyOptionsTest.java
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
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import com.github.robtimus.filesystems.Messages;

class CopyOptionsTest {

    @Test
    void testToOpenOptions() {
        CopyOptions options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        Collection<OpenOption> openOptions = options.toOpenOptions(StandardOpenOption.READ);
        assertEquals(Arrays.asList(LinkOption.NOFOLLOW_LINKS, StandardOpenOption.READ), openOptions);
    }

    @Nested
    class ForCopy {

        @Test
        void testWithNoOptions() {
            CopyOptions options = CopyOptions.forCopy();
            assertFalse(options.replaceExisting);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReplaceExisting() {
            CopyOptions options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING);

            assertTrue(options.replaceExisting);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithNoFollowLinks() {
            CopyOptions options = CopyOptions.forCopy(LinkOption.NOFOLLOW_LINKS);

            assertFalse(options.replaceExisting);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithReplaceExistingAndNoFollowLinks() {
            CopyOptions options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);

            assertTrue(options.replaceExisting);
            assertNull(options.fileType);
            assertNull(options.fileStructure);
            assertNull(options.fileTransferMode);
        }

        @Test
        void testWithFileTypeAndFileStructureAndFileTransferMode() {
            CopyOptions options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS,
                    FileType.ascii(), FileStructure.FILE, FileTransferMode.STREAM);

            assertTrue(options.replaceExisting);
            assertEquals(FileType.ascii(), options.fileType);
            assertEquals(FileStructure.FILE, options.fileStructure);
            assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
        }

        @Test
        void testWithDuplicateFileTypeAndFileStructureAndFileTransferMode() {
            CopyOptions options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS,
                    FileType.ascii(), FileType.ascii(), FileStructure.FILE, FileStructure.FILE, FileTransferMode.STREAM, FileTransferMode.STREAM);

            assertTrue(options.replaceExisting);
            assertEquals(FileType.ascii(), options.fileType);
            assertEquals(FileStructure.FILE, options.fileStructure);
            assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
        }

        @Test
        void testWithCopyAttributes() {
            testWithInvalid(StandardCopyOption.COPY_ATTRIBUTES);
        }

        @Test
        void testWithAtomicMove() {
            testWithInvalid(StandardCopyOption.ATOMIC_MOVE);
        }

        private void testWithInvalid(CopyOption option) {
            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class, () -> CopyOptions.forCopy(option));
            assertChainEquals(Messages.fileSystemProvider().unsupportedCopyOption(option), exception);
        }

        @Test
        void testWithMultipleFileTypes() {
            testWithMultiples(FileType.ascii(), FileType.binary());
        }

        @Test
        void testWithDifferentMultipleFileStructures() {
            testWithMultiples(FileStructure.FILE, FileStructure.PAGE);
        }

        @Test
        void testWithDifferentMultipleFileTransferModes() {
            testWithMultiples(FileTransferMode.STREAM, FileTransferMode.BLOCK);
        }

        private void testWithMultiples(CopyOption... options) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CopyOptions.forCopy(options));
            assertChainEquals(Messages.fileSystemProvider().illegalCopyOptionCombination(options), exception);
        }
    }

    @Nested
    class ForMove {

        @Nested
        class SameFileSystem {

            @Test
            void testWithNoOptions() {
                CopyOptions options = CopyOptions.forMove(true);
                assertFalse(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithReplaceExisting() {
                CopyOptions options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING);

                assertTrue(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithAtomicMove() {
                CopyOptions options = CopyOptions.forMove(true, StandardCopyOption.ATOMIC_MOVE);

                assertFalse(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithReplaceExistingAndAtomicMove() {
                CopyOptions options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

                assertTrue(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithNoFollowLinks() {
                CopyOptions options = CopyOptions.forMove(true, LinkOption.NOFOLLOW_LINKS);

                assertFalse(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithReplaceExistingAndNoFollowLinks() {
                CopyOptions options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);

                assertTrue(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithAtomicMoveAndNoFollowLinks() {
                CopyOptions options = CopyOptions.forMove(true, StandardCopyOption.ATOMIC_MOVE, LinkOption.NOFOLLOW_LINKS);

                assertFalse(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithReplaceExistingAndAtomicMoveAndNoFollowLinks() {
                CopyOptions options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE,
                        LinkOption.NOFOLLOW_LINKS);

                assertTrue(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithDuplicateFileTypeAndFileStructureAndFileTransferMode() {
                CopyOptions options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS,
                        FileType.ascii(), FileType.ascii(), FileStructure.FILE, FileStructure.FILE, FileTransferMode.STREAM, FileTransferMode.STREAM);

                assertTrue(options.replaceExisting);
                assertEquals(FileType.ascii(), options.fileType);
                assertEquals(FileStructure.FILE, options.fileStructure);
                assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
            }

            @Test
            void testWithCopyAttributes() {
                testWithInvalid(true, StandardCopyOption.COPY_ATTRIBUTES);
            }

            @Test
            void testWithMultipleFileTypes() {
                testWithMultiples(true, FileType.ascii(), FileType.binary());
            }

            @Test
            void testWithMultipleFileStructures() {
                testWithMultiples(true, FileStructure.FILE, FileStructure.PAGE);
            }

            @Test
            void testWithMultipleFileTransferModes() {
                testWithMultiples(true, FileTransferMode.STREAM, FileTransferMode.BLOCK);
            }
        }

        @Nested
        class DifferentFileSystem {

            @Test
            void testWithNoOptions() {
                CopyOptions options = CopyOptions.forMove(false);

                assertFalse(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithReplaceExisting() {
                CopyOptions options = CopyOptions.forMove(false, StandardCopyOption.REPLACE_EXISTING);

                assertTrue(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithNoFollowLinks() {
                CopyOptions options = CopyOptions.forMove(false, LinkOption.NOFOLLOW_LINKS);

                assertFalse(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithReplaceExistingAndNoFollowLinks() {
                CopyOptions options = CopyOptions.forMove(false, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);

                assertTrue(options.replaceExisting);
                assertNull(options.fileType);
                assertNull(options.fileStructure);
                assertNull(options.fileTransferMode);
            }

            @Test
            void testWithDuplicateFileTypeAndFileStructureAndFileTransferMode() {
                CopyOptions options = CopyOptions.forMove(false, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS,
                        FileType.ascii(), FileType.ascii(), FileStructure.FILE, FileStructure.FILE, FileTransferMode.STREAM, FileTransferMode.STREAM);

                assertTrue(options.replaceExisting);
                assertEquals(FileType.ascii(), options.fileType);
                assertEquals(FileStructure.FILE, options.fileStructure);
                assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
            }

            @Test
            void testWithCopyAttributes() {
                testWithInvalid(false, StandardCopyOption.COPY_ATTRIBUTES);
            }

            @Test
            void testWithAtomicMove() {
                testWithInvalid(false, StandardCopyOption.ATOMIC_MOVE);
            }

            @Test
            void testWithMultipleFileTypes() {
                testWithMultiples(false, FileType.ascii(), FileType.binary());
            }

            @Test
            void testWithMultipleFileStructures() {
                testWithMultiples(false, FileStructure.FILE, FileStructure.PAGE);
            }

            @Test
            void testWithMultipleFileTransferModes() {
                testWithMultiples(false, FileTransferMode.STREAM, FileTransferMode.BLOCK);
            }
        }

        private void testWithInvalid(boolean sameFileSystem, CopyOption option) {
            UnsupportedOperationException exception = assertThrows(UnsupportedOperationException.class,
                    () -> CopyOptions.forMove(sameFileSystem, option));
            assertChainEquals(Messages.fileSystemProvider().unsupportedCopyOption(option), exception);
        }

        private void testWithMultiples(boolean sameFileSystem, CopyOption... options) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> CopyOptions.forMove(sameFileSystem, options));
            assertChainEquals(Messages.fileSystemProvider().illegalCopyOptionCombination(options), exception);
        }
    }
}
