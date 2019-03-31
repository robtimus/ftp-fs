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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.nio.file.CopyOption;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import com.github.robtimus.filesystems.Messages;

@SuppressWarnings({ "nls", "javadoc" })
public class CopyOptionsTest {

    @Test
    public void testToOpenOptions() {
        CopyOptions options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        Collection<OpenOption> openOptions = options.toOpenOptions(StandardOpenOption.READ);
        assertEquals(Arrays.asList(LinkOption.NOFOLLOW_LINKS, StandardOpenOption.READ), openOptions);
    }

    @Test
    public void testForCopy() {
        CopyOptions options = CopyOptions.forCopy();
        assertFalse(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING);
        assertTrue(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forCopy(LinkOption.NOFOLLOW_LINKS);
        assertFalse(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        assertTrue(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS, FileType.ascii(), FileStructure.FILE,
                FileTransferMode.STREAM);
        assertTrue(options.replaceExisting);
        assertEquals(FileType.ascii(), options.fileType);
        assertEquals(FileStructure.FILE, options.fileStructure);
        assertEquals(FileTransferMode.STREAM, options.fileTransferMode);

        options = CopyOptions.forCopy(StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS, FileType.ascii(), FileType.ascii(),
                FileStructure.FILE, FileStructure.FILE, FileTransferMode.STREAM, FileTransferMode.STREAM);
        assertTrue(options.replaceExisting);
        assertEquals(FileType.ascii(), options.fileType);
        assertEquals(FileStructure.FILE, options.fileStructure);
        assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
    }

    @Test
    public void testForCopyWithInvalid() {
        testForCopyWithInvalid(StandardCopyOption.COPY_ATTRIBUTES);
        testForCopyWithInvalid(StandardCopyOption.ATOMIC_MOVE);
    }

    private void testForCopyWithInvalid(CopyOption option) {
        try {
            CopyOptions.forCopy(option);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            UnsupportedOperationException expected = Messages.fileSystemProvider().unsupportedCopyOption(option);
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testForCopyWithDuplicates() {
        testForCopyWithDuplicates(FileType.ascii(), FileType.binary());
        testForCopyWithDuplicates(FileStructure.FILE, FileStructure.PAGE);
        testForCopyWithDuplicates(FileTransferMode.STREAM, FileTransferMode.BLOCK);
    }

    private void testForCopyWithDuplicates(CopyOption... options) {
        try {
            CopyOptions.forCopy(options);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            IllegalArgumentException expected = Messages.fileSystemProvider().illegalCopyOptionCombination(options);
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testForMove() {
        CopyOptions options = CopyOptions.forMove(true);
        assertFalse(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(true, StandardCopyOption.ATOMIC_MOVE);
        assertFalse(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        assertTrue(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(true, LinkOption.NOFOLLOW_LINKS);
        assertFalse(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        assertTrue(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(true, StandardCopyOption.ATOMIC_MOVE, LinkOption.NOFOLLOW_LINKS);
        assertFalse(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE, LinkOption.NOFOLLOW_LINKS);
        assertTrue(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(true, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS, FileType.ascii(), FileType.ascii(),
                FileStructure.FILE, FileStructure.FILE, FileTransferMode.STREAM, FileTransferMode.STREAM);
        assertTrue(options.replaceExisting);
        assertEquals(FileType.ascii(), options.fileType);
        assertEquals(FileStructure.FILE, options.fileStructure);
        assertEquals(FileTransferMode.STREAM, options.fileTransferMode);

        options = CopyOptions.forMove(false);
        assertFalse(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(false, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(false, LinkOption.NOFOLLOW_LINKS);
        assertFalse(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(false, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        assertTrue(options.replaceExisting);
        assertNull(options.fileType);
        assertNull(options.fileStructure);
        assertNull(options.fileTransferMode);

        options = CopyOptions.forMove(false, StandardCopyOption.REPLACE_EXISTING, LinkOption.NOFOLLOW_LINKS, FileType.ascii(), FileType.ascii(),
                FileStructure.FILE, FileStructure.FILE, FileTransferMode.STREAM, FileTransferMode.STREAM);
        assertTrue(options.replaceExisting);
        assertEquals(FileType.ascii(), options.fileType);
        assertEquals(FileStructure.FILE, options.fileStructure);
        assertEquals(FileTransferMode.STREAM, options.fileTransferMode);
    }

    @Test
    public void testForMoveWithInvalid() {
        testForMoveWithInvalid(true, StandardCopyOption.COPY_ATTRIBUTES);
        testForMoveWithInvalid(false, StandardCopyOption.COPY_ATTRIBUTES);
        testForMoveWithInvalid(false, StandardCopyOption.ATOMIC_MOVE);
    }

    private void testForMoveWithInvalid(boolean sameFileSystem, CopyOption option) {
        try {
            CopyOptions.forMove(sameFileSystem, option);
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            UnsupportedOperationException expected = Messages.fileSystemProvider().unsupportedCopyOption(option);
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }

    @Test
    public void testForMoveWithDuplicates() {
        testForMoveWithDuplicates(true, FileType.ascii(), FileType.binary());
        testForMoveWithDuplicates(false, FileType.ascii(), FileType.binary());
        testForMoveWithDuplicates(true, FileStructure.FILE, FileStructure.PAGE);
        testForMoveWithDuplicates(false, FileStructure.FILE, FileStructure.PAGE);
        testForMoveWithDuplicates(true, FileTransferMode.STREAM, FileTransferMode.BLOCK);
        testForMoveWithDuplicates(false, FileTransferMode.STREAM, FileTransferMode.BLOCK);
    }

    private void testForMoveWithDuplicates(boolean sameFileSystem, CopyOption... options) {
        try {
            CopyOptions.forMove(sameFileSystem, options);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            IllegalArgumentException expected = Messages.fileSystemProvider().illegalCopyOptionCombination(options);
            assertEquals(expected.getMessage(), e.getMessage());
        }
    }
}
