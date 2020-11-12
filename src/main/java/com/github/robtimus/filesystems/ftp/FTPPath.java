/*
 * FTPPath.java
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileStore;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchEvent.Modifier;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFileAttributes;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.SimpleAbstractPath;

/**
 * A path for FTP file systems.
 *
 * @author Rob Spoor
 */
class FTPPath extends SimpleAbstractPath {

    private final FTPFileSystem fs;

    FTPPath(FTPFileSystem fs, String path) {
        super(path);
        this.fs = Objects.requireNonNull(fs);
    }

    private FTPPath(FTPFileSystem fs, String path, boolean normalized) {
        super(path, normalized);
        this.fs = Objects.requireNonNull(fs);
    }

    @Override
    protected FTPPath createPath(String path) {
        return new FTPPath(fs, path, true);
    }

    @Override
    public FTPFileSystem getFileSystem() {
        return fs;
    }

    @Override
    public FTPPath getRoot() {
        return (FTPPath) super.getRoot();
    }

    @Override
    public FTPPath getFileName() {
        return (FTPPath) super.getFileName();
    }

    @Override
    public FTPPath getParent() {
        return (FTPPath) super.getParent();
    }

    @Override
    public FTPPath getName(int index) {
        return (FTPPath) super.getName(index);
    }

    @Override
    public FTPPath subpath(int beginIndex, int endIndex) {
        return (FTPPath) super.subpath(beginIndex, endIndex);
    }

    @Override
    public FTPPath normalize() {
        return (FTPPath) super.normalize();
    }

    @Override
    public FTPPath resolve(Path other) {
        return (FTPPath) super.resolve(other);
    }

    @Override
    public FTPPath resolve(String other) {
        return (FTPPath) super.resolve(other);
    }

    @Override
    public FTPPath resolveSibling(Path other) {
        return (FTPPath) super.resolveSibling(other);
    }

    @Override
    public FTPPath resolveSibling(String other) {
        return (FTPPath) super.resolveSibling(other);
    }

    @Override
    public FTPPath relativize(Path other) {
        return (FTPPath) super.relativize(other);
    }

    @Override
    public URI toUri() {
        return fs.toUri(this);
    }

    @Override
    public FTPPath toAbsolutePath() {
        return fs.toAbsolutePath(this);
    }

    @Override
    public FTPPath toRealPath(LinkOption... options) throws IOException {
        return fs.toRealPath(this, options);
    }

    @Override
    public WatchKey register(WatchService watcher, Kind<?>[] events, Modifier... modifiers) throws IOException {
        throw Messages.unsupportedOperation(Path.class, "register"); //$NON-NLS-1$
    }

    @Override
    public String toString() {
        return fs.toString(this);
    }

    InputStream newInputStream(OpenOption... options) throws IOException {
        return fs.newInputStream(this, options);
    }

    OutputStream newOutputStream(OpenOption... options) throws IOException {
        return fs.newOutputStream(this, options);
    }

    SeekableByteChannel newByteChannel(Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return fs.newByteChannel(this, options, attrs);
    }

    DirectoryStream<Path> newDirectoryStream(Filter<? super Path> filter) throws IOException {
        return fs.newDirectoryStream(this, filter);
    }

    void createDirectory(FileAttribute<?>... attrs) throws IOException {
        fs.createDirectory(this, attrs);
    }

    void delete() throws IOException {
        fs.delete(this);
    }

    FTPPath readSymbolicLink() throws IOException {
        return fs.readSymbolicLink(this);
    }

    void copy(FTPPath target, CopyOption... options) throws IOException {
        fs.copy(this, target, options);
    }

    void move(FTPPath target, CopyOption... options) throws IOException {
        fs.move(this, target, options);
    }

    @SuppressWarnings("resource")
    boolean isSameFile(Path other) throws IOException {
        if (this.equals(other)) {
            return true;
        }
        if (other == null || getFileSystem() != other.getFileSystem()) {
            return false;
        }
        return fs.isSameFile(this, (FTPPath) other);
    }

    boolean isHidden() throws IOException {
        return fs.isHidden(this);
    }

    FileStore getFileStore() throws IOException {
        return fs.getFileStore(this);
    }

    void checkAccess(AccessMode... modes) throws IOException {
        fs.checkAccess(this, modes);
    }

    PosixFileAttributes readAttributes(LinkOption... options) throws IOException {
        return fs.readAttributes(this, options);
    }

    Map<String, Object> readAttributes(String attributes, LinkOption... options) throws IOException {
        return fs.readAttributes(this, attributes, options);
    }
}
