/*
 * FTPFileSystem.java
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

import com.github.robtimus.filesystems.AbstractDirectoryStream;
import com.github.robtimus.filesystems.FileSystemProviderSupport;
import com.github.robtimus.filesystems.LinkOptionSupport;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.PathMatcherSupport;
import com.github.robtimus.filesystems.URISupport;
import com.github.robtimus.filesystems.attribute.SimpleGroupPrincipal;
import com.github.robtimus.filesystems.attribute.SimpleUserPrincipal;
import com.github.robtimus.filesystems.ftp.FTPClientPool.Client;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryStream.Filter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.NotLinkException;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.StandardOpenOption;
import java.nio.file.WatchService;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * An FTP file system.
 *
 * @author Rob Spoor
 */
class FTPFileSystem extends FileSystem {

    private static final String CURRENT_DIR = "."; //$NON-NLS-1$
    private static final String PARENT_DIR = ".."; //$NON-NLS-1$

    @SuppressWarnings("nls")
    private static final Set<String> SUPPORTED_FILE_ATTRIBUTE_VIEWS = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("basic", "owner", "posix")));

    private final FTPFileSystemProvider provider;
    private final Iterable<Path> rootDirectories;
    private final FileStore fileStore;
    private final Iterable<FileStore> fileStores;

    private final FTPClientPool clientPool;
    private final URI uri;
    private final String defaultDirectory;

    private final AtomicBoolean open = new AtomicBoolean(true);

    FTPFileSystem(FTPFileSystemProvider provider, URI uri, FTPEnvironment env) throws IOException {
        this.provider = Objects.requireNonNull(provider);
        this.rootDirectories = Collections.<Path>singleton(new FTPPath(this, "/")); //$NON-NLS-1$
        this.fileStore = new FTPFileStore(this);
        this.fileStores = Collections.<FileStore>singleton(fileStore);

        this.clientPool = new FTPClientPool(uri.getHost(), uri.getPort(), env);
        this.uri = Objects.requireNonNull(uri);

        try (Client client = clientPool.get()) {
            this.defaultDirectory = client.pwd();
        }
    }

    @Override
    public FTPFileSystemProvider provider() {
        return provider;
    }

    @Override
    public void close() throws IOException {
        if (open.getAndSet(false)) {
            provider.removeFileSystem(uri);
            clientPool.close();
        }
    }

    @Override
    public boolean isOpen() {
        return open.get();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public String getSeparator() {
        return "/"; //$NON-NLS-1$
    }

    @Override
    public Iterable<Path> getRootDirectories() {
        return rootDirectories;
    }

    @Override
    public Iterable<FileStore> getFileStores() {
        return fileStores;
    }

    @Override
    public Set<String> supportedFileAttributeViews() {
        return SUPPORTED_FILE_ATTRIBUTE_VIEWS;
    }

    @Override
    public Path getPath(String first, String... more) {
        StringBuilder sb = new StringBuilder(first);
        for (String s : more) {
            sb.append("/").append(s); //$NON-NLS-1$
        }
        return new FTPPath(this, sb.toString());
    }

    @Override
    public PathMatcher getPathMatcher(String syntaxAndPattern) {
        final Pattern pattern = PathMatcherSupport.toPattern(syntaxAndPattern);
        return new PathMatcher() {
            @Override
            public boolean matches(Path path) {
                return pattern.matcher(path.toString()).matches();
            }
        };
    }

    @Override
    public UserPrincipalLookupService getUserPrincipalLookupService() {
        throw Messages.unsupportedOperation(FileSystem.class, "getUserPrincipalLookupService"); //$NON-NLS-1$
    }

    @Override
    public WatchService newWatchService() throws IOException {
        throw Messages.unsupportedOperation(FileSystem.class, "newWatchService"); //$NON-NLS-1$
    }

    void keepAlive() throws IOException {
        clientPool.keepAlive();
    }

    boolean isSecure() {
        return clientPool.isSecure();
    }

    URI toUri(FTPPath path) {
        FTPPath absPath = toAbsolutePath(path).normalize();
        return toUri(absPath.path());
    }

    URI toUri(String path) {
        return URISupport.create(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), path, null, null);
    }

    FTPPath toAbsolutePath(FTPPath path) {
        if (path.isAbsolute()) {
            return path;
        }
        return new FTPPath(this, defaultDirectory + "/" + path.path()); //$NON-NLS-1$
    }

    FTPPath toRealPath(FTPPath path, LinkOption... options) throws IOException {
        boolean followLinks = LinkOptionSupport.followLinks(options);
        try (Client client = clientPool.get()) {
            return toRealPath(client, path, followLinks).ftpPath;
        }
    }

    private FTPPathAndFilePair toRealPath(Client client, FTPPath path, boolean followLinks) throws IOException {
        FTPPath absPath = toAbsolutePath(path).normalize();
        // call getFTPFile to verify the file exists
        FTPFile ftpFile = getFTPFile(client, absPath);

        if (followLinks && isPossibleSymbolicLink(ftpFile)) {
            FTPFile link = getLink(client, ftpFile, absPath);
            if (link != null) {
                return toRealPath(client, new FTPPath(this, link.getLink()), followLinks);
            }
        }
        return new FTPPathAndFilePair(absPath, ftpFile);
    }

    private static final class FTPPathAndFilePair {
        private final FTPPath ftpPath;
        private final FTPFile ftpFile;

        private FTPPathAndFilePair(FTPPath ftpPath, FTPFile ftpFile) {
            this.ftpPath = ftpPath;
            this.ftpFile = ftpFile;
        }
    }

    private boolean isPossibleSymbolicLink(FTPFile ftpFile) {
        return ftpFile.isSymbolicLink() || (ftpFile.isDirectory() && CURRENT_DIR.equals(ftpFile.getName()));
    }

    String toString(FTPPath path) {
        return path.path();
    }

    InputStream newInputStream(FTPPath path, OpenOption... options) throws IOException {
        OpenOptions openOptions = OpenOptions.forNewInputStream(options);

        try (Client client = clientPool.get()) {
            return newInputStream(client, path, openOptions);
        }
    }

    private InputStream newInputStream(Client client, FTPPath path, OpenOptions options) throws IOException {
        assert options.read;

        return client.newInputStream(path.path(), options);
    }

    OutputStream newOutputStream(FTPPath path, OpenOption... options) throws IOException {
        OpenOptions openOptions = OpenOptions.forNewOutputStream(options);

        try (Client client = clientPool.get()) {
            return newOutputStream(client, path, false, openOptions).out;
        }
    }

    @SuppressWarnings("resource")
    private FTPFileAndOutputStreamPair newOutputStream(Client client, FTPPath path, boolean requireFTPFile, OpenOptions options) throws IOException {

        // retrieve the file unless create is true and createNew is false, because then the file can be created
        FTPFile ftpFile = null;
        if (!options.create || options.createNew) {
            ftpFile = findFTPFile(client, path);
            if (ftpFile != null && ftpFile.isDirectory()) {
                throw Messages.fileSystemProvider().isDirectory(path.path());
            }
            if (!options.createNew && ftpFile == null) {
                throw new NoSuchFileException(path.path());
            } else if (options.createNew && ftpFile != null) {
                throw new FileAlreadyExistsException(path.path());
            }
        }
        // else the file can be created if necessary

        if (ftpFile == null && requireFTPFile) {
            ftpFile = findFTPFile(client, path);
        }

        OutputStream out = client.newOutputStream(path.path(), options);
        return new FTPFileAndOutputStreamPair(ftpFile, out);
    }

    private static final class FTPFileAndOutputStreamPair {

        private final FTPFile ftpFile;
        private final OutputStream out;

        private FTPFileAndOutputStreamPair(FTPFile ftpFile, OutputStream out) {
            this.ftpFile = ftpFile;
            this.out = out;
        }
    }

    @SuppressWarnings("resource")
    SeekableByteChannel newByteChannel(FTPPath path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        if (attrs.length > 0) {
            throw Messages.fileSystemProvider().unsupportedCreateFileAttribute(attrs[0].name());
        }

        OpenOptions openOptions = OpenOptions.forNewByteChannel(options);

        try (Client client = clientPool.get()) {
            if (openOptions.read) {
                // use findFTPFile instead of getFTPFile, to let the opening of the stream provide the correct error message
                FTPFile ftpFile = findFTPFile(client, path);
                InputStream in = newInputStream(client, path, openOptions);
                long size = ftpFile == null ? 0 : ftpFile.getSize();
                return FileSystemProviderSupport.createSeekableByteChannel(in, size);
            }

            // if append then we need the FTP file, to find the initial position of the channel
            boolean requireFTPFile = openOptions.append;
            FTPFileAndOutputStreamPair outPair = newOutputStream(client, path, requireFTPFile, openOptions);
            long initialPosition = outPair.ftpFile == null ? 0 : outPair.ftpFile.getSize();
            return FileSystemProviderSupport.createSeekableByteChannel(outPair.out, initialPosition);
        }
    }

    DirectoryStream<Path> newDirectoryStream(final FTPPath path, Filter<? super Path> filter) throws IOException {
        FTPFile[] ftpFiles;
        try (Client client = clientPool.get()) {
            ftpFiles = client.listFiles(path.path());
        }

        if (ftpFiles.length == 0) {
            throw new NoSuchFileException(path.path());
        }
        boolean isDirectory = false;
        List<FTPFile> children = new ArrayList<>(ftpFiles.length);
        for (FTPFile ftpFile : ftpFiles) {
            String fileName = ftpFile.getName();
            if (CURRENT_DIR.equals(fileName)) {
                isDirectory = true;
            } else if (!PARENT_DIR.equals(fileName)) {
                children.add(ftpFile);
            }
        }

        if (!isDirectory) {
            throw new NotDirectoryException(path.path());
        }
        return new FTPPathDirectoryStream(path, children, filter);
    }

    private static final class FTPPathDirectoryStream extends AbstractDirectoryStream<Path> {

        private final FTPPath path;
        private final List<FTPFile> files;
        private Iterator<FTPFile> iterator;

        private FTPPathDirectoryStream(FTPPath path, List<FTPFile> files, Filter<? super Path> filter) {
            super(filter);
            this.path = path;
            this.files = files;
        }

        @Override
        protected void setupIteration() {
            iterator = files.iterator();
        }

        @Override
        protected Path getNext() throws IOException {
            return iterator.hasNext() ? path.resolve(iterator.next().getName()) : null;
        }
    }

    void createDirectory(FTPPath path, FileAttribute<?>... attrs) throws IOException {
        if (attrs.length > 0) {
            throw Messages.fileSystemProvider().unsupportedCreateFileAttribute(attrs[0].name());
        }

        try (Client client = clientPool.get()) {
            client.mkdir(path.path());
        }
    }

    void delete(FTPPath path) throws IOException {
        try (Client client = clientPool.get()) {
            FTPFile ftpFile = getFTPFile(client, path);
            boolean isDirectory = ftpFile.isDirectory();
            client.delete(path.path(), isDirectory);
        }
    }

    FTPPath readSymbolicLink(FTPPath path) throws IOException {
        try (Client client = clientPool.get()) {
            FTPFile ftpFile = getFTPFile(client, path);
            FTPFile link = getLink(client, ftpFile, path);
            if (link == null) {
                throw new NotLinkException(path.path());
            }
            return path.resolveSibling(link.getLink());
        }
    }

    void copy(FTPPath source, FTPPath target, CopyOption... options) throws IOException {
        boolean sameFileSystem = source.getFileSystem() == target.getFileSystem();
        CopyOptions copyOptions = CopyOptions.forCopy(options);

        try (Client client = clientPool.get()) {
            // get the FTP file to determine whether a directory needs to be created or a file needs to be copied
            // Files.copy specifies that for links, the final target must be copied
            FTPPathAndFilePair sourcePair = toRealPath(client, source, true);

            if (!sameFileSystem) {
                copyAcrossFileSystems(client, source, sourcePair.ftpFile, target, copyOptions);
                return;
            }

            try {
                if (sourcePair.ftpPath.path().equals(toRealPath(client, target, true).ftpPath.path())) {
                    // non-op, don't do a thing as specified by Files.copy
                    return;
                }
            } catch (@SuppressWarnings("unused") NoSuchFileException e) {
                // the target does not exist or either path is an invalid link, ignore the error and continue
            }

            FTPFile targetFtpFile = findFTPFile(client, target);

            if (targetFtpFile != null) {
                if (copyOptions.replaceExisting) {
                    client.delete(target.path(), targetFtpFile.isDirectory());
                } else {
                    throw new FileAlreadyExistsException(target.path());
                }
            }

            if (sourcePair.ftpFile.isDirectory()) {
                client.mkdir(target.path());
            } else {
                try (Client client2 = clientPool.getOrCreate()) {
                    copyFile(client, source, client2, target, copyOptions);
                }
            }
        }
    }

    private void copyAcrossFileSystems(Client sourceClient, FTPPath source, FTPFile sourceFtpFile, FTPPath target, CopyOptions options)
            throws IOException {

        try (Client targetClient = target.getFileSystem().clientPool.getOrCreate()) {

            FTPFile targetFtpFile = findFTPFile(targetClient, target);

            if (targetFtpFile != null) {
                if (options.replaceExisting) {
                    targetClient.delete(target.path(), targetFtpFile.isDirectory());
                } else {
                    throw new FileAlreadyExistsException(target.path());
                }
            }

            if (sourceFtpFile.isDirectory()) {
                sourceClient.mkdir(target.path());
            } else {
                copyFile(sourceClient, source, targetClient, target, options);
            }
        }
    }

    private void copyFile(Client sourceClient, FTPPath source, Client targetClient, FTPPath target, CopyOptions options) throws IOException {
        OpenOptions inOptions = OpenOptions.forNewInputStream(options.toOpenOptions(StandardOpenOption.READ));
        OpenOptions outOptions = OpenOptions
                .forNewOutputStream(options.toOpenOptions(StandardOpenOption.WRITE, StandardOpenOption.CREATE));
        try (InputStream in = sourceClient.newInputStream(source.path(), inOptions)) {
            targetClient.storeFile(target.path(), in, outOptions, outOptions.options);
        }
    }

    void move(FTPPath source, FTPPath target, CopyOption... options) throws IOException {
        boolean sameFileSystem = source.getFileSystem() == target.getFileSystem();
        CopyOptions copyOptions = CopyOptions.forMove(sameFileSystem, options);

        try (Client client = clientPool.get()) {
            if (!sameFileSystem) {
                FTPFile ftpFile = getFTPFile(client, source);
                if (getLink(client, ftpFile, source) != null) {
                    throw new IOException(FTPMessages.copyOfSymbolicLinksAcrossFileSystemsNotSupported());
                }
                copyAcrossFileSystems(client, source, ftpFile, target, copyOptions);
                client.delete(source.path(), ftpFile.isDirectory());
                return;
            }

            try {
                if (isSameFile(client, source, target)) {
                    // non-op, don't do a thing as specified by Files.move
                    return;
                }
            } catch (@SuppressWarnings("unused") NoSuchFileException e) {
                // the source or target does not exist or either path is an invalid link
                // call getFTPFile to ensure the source file exists
                // ignore any error to target or if the source link is invalid
                getFTPFile(client, source);
            }

            if (toAbsolutePath(source).parentPath() == null) {
                // cannot move or rename the root
                throw new DirectoryNotEmptyException(source.path());
            }

            FTPFile targetFTPFile = findFTPFile(client, target);
            if (copyOptions.replaceExisting && targetFTPFile != null) {
                client.delete(target.path(), targetFTPFile.isDirectory());
            }

            client.rename(source.path(), target.path());
        }
    }

    boolean isSameFile(FTPPath path, FTPPath path2) throws IOException {
        if (path.getFileSystem() != path2.getFileSystem()) {
            return false;
        }
        if (path.equals(path2)) {
            return true;
        }
        try (Client client = clientPool.get()) {
            return isSameFile(client, path, path2);
        }
    }

    private boolean isSameFile(Client client, FTPPath path, FTPPath path2) throws IOException {
        if (path.equals(path2)) {
            return true;
        }
        return toRealPath(client, path, true).ftpPath.path().equals(toRealPath(client, path2, true).ftpPath.path());
    }

    boolean isHidden(FTPPath path) throws IOException {
        // call getFTPFile to check for existence
        try (Client client = clientPool.get()) {
            getFTPFile(client, path);
        }
        String fileName = path.fileName();
        return !CURRENT_DIR.equals(fileName) && !PARENT_DIR.equals(fileName) && fileName.startsWith("."); //$NON-NLS-1$
    }

    FileStore getFileStore(FTPPath path) throws IOException {
        // call getFTPFile to check existence of the path
        try (Client client = clientPool.get()) {
            getFTPFile(client, path);
        }
        return fileStore;
    }

    void checkAccess(FTPPath path, AccessMode... modes) throws IOException {
        try (Client client = clientPool.get()) {
            FTPFile ftpFile = getFTPFile(client, path);
            for (AccessMode mode : modes) {
                if (!hasAccess(ftpFile, mode)) {
                    throw new AccessDeniedException(path.path());
                }
            }
        }
    }

    private boolean hasAccess(FTPFile ftpFile, AccessMode mode) {
        switch (mode) {
        case READ:
            return ftpFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION);
        case WRITE:
            return ftpFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION);
        case EXECUTE:
            return ftpFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION);
        default:
            return false;
        }
    }

    PosixFileAttributes readAttributes(FTPPath path, LinkOption... options) throws IOException {
        boolean followLinks = LinkOptionSupport.followLinks(options);
        try (Client client = clientPool.get()) {
            FTPPathAndFilePair pair = toRealPath(client, path, followLinks);

            // pair.ftpFile.getTimestamp() is most likely based on a too broad precision (day), so use mdtm to retrieve the timestamp (if available)
            Calendar lastModified = client.mdtm(pair.ftpPath.path());

            // we need to call getLink unless followLinks is true, because for folders otherwise the data will not be accurate
            FTPFile link = followLinks ? null : getLink(client, pair.ftpFile, path);

            FTPFile ftpFile = link == null ? pair.ftpFile : link;

            return new FTPPathFileAttributes(ftpFile, lastModified);
        }
    }

    private static final class FTPPathFileAttributes implements PosixFileAttributes {

        private static final FileTime EPOCH = FileTime.fromMillis(0L);

        private final FTPFile ftpFile;
        private final FileTime lastModified;

        private FTPPathFileAttributes(FTPFile ftpFile, Calendar lastModified) {
            this.ftpFile = ftpFile;
            if (lastModified == null) {
                Calendar timestamp = ftpFile.getTimestamp();
                this.lastModified = timestamp == null ? EPOCH : FileTime.fromMillis(timestamp.getTimeInMillis());
            } else {
                this.lastModified = FileTime.fromMillis(lastModified.getTimeInMillis());
            }
        }

        @Override
        public UserPrincipal owner() {
            String user = ftpFile.getUser();
            return user == null ? null : new SimpleUserPrincipal(user);
        }

        @Override
        public GroupPrincipal group() {
            String group = ftpFile.getGroup();
            return group == null ? null : new SimpleGroupPrincipal(group);
        }

        @Override
        public Set<PosixFilePermission> permissions() {
            Set<PosixFilePermission> permissions = EnumSet.noneOf(PosixFilePermission.class);
            addPermissionIfSet(ftpFile, FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION, PosixFilePermission.OWNER_READ, permissions);
            addPermissionIfSet(ftpFile, FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION, PosixFilePermission.OWNER_WRITE, permissions);
            addPermissionIfSet(ftpFile, FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION, PosixFilePermission.OWNER_EXECUTE, permissions);
            addPermissionIfSet(ftpFile, FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION, PosixFilePermission.GROUP_READ, permissions);
            addPermissionIfSet(ftpFile, FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION, PosixFilePermission.GROUP_WRITE, permissions);
            addPermissionIfSet(ftpFile, FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION, PosixFilePermission.GROUP_EXECUTE, permissions);
            addPermissionIfSet(ftpFile, FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION, PosixFilePermission.OTHERS_READ, permissions);
            addPermissionIfSet(ftpFile, FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION, PosixFilePermission.OTHERS_WRITE, permissions);
            addPermissionIfSet(ftpFile, FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION, PosixFilePermission.OTHERS_EXECUTE, permissions);
            return permissions;
        }

        private void addPermissionIfSet(FTPFile ftpFile, int access, int permission, PosixFilePermission value,
                Set<PosixFilePermission> permissions) {

            if (ftpFile.hasPermission(access, permission)) {
                permissions.add(value);
            }
        }

        @Override
        public FileTime lastModifiedTime() {
            return lastModified;
        }

        @Override
        public FileTime lastAccessTime() {
            return lastModifiedTime();
        }

        @Override
        public FileTime creationTime() {
            return lastModifiedTime();
        }

        @Override
        public boolean isRegularFile() {
            return ftpFile.isFile();
        }

        @Override
        public boolean isDirectory() {
            return ftpFile.isDirectory();
        }

        @Override
        public boolean isSymbolicLink() {
            return ftpFile.isSymbolicLink();
        }

        @Override
        public boolean isOther() {
            return false;
        }

        @Override
        public long size() {
            return ftpFile.getSize();
        }

        @Override
        public Object fileKey() {
            return null;
        }
    }

    @SuppressWarnings("nls")
    private static final Set<String> BASIC_ATTRIBUTES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "basic:lastModifiedTime", "basic:lastAccessTime", "basic:creationTime", "basic:size",
            "basic:isRegularFile", "basic:isDirectory", "basic:isSymbolicLink", "basic:isOther", "basic:fileKey")));

    @SuppressWarnings("nls")
    private static final Set<String> OWNER_ATTRIBUTES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "owner:owner")));

    @SuppressWarnings("nls")
    private static final Set<String> POSIX_ATTRIBUTES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "posix:lastModifiedTime", "posix:lastAccessTime", "posix:creationTime", "posix:size",
            "posix:isRegularFile", "posix:isDirectory", "posix:isSymbolicLink", "posix:isOther", "posix:fileKey",
            "posix:owner", "posix:group", "posix:permissions")));

    Map<String, Object> readAttributes(FTPPath path, String attributes, LinkOption... options) throws IOException {

        String view;
        int pos = attributes.indexOf(':');
        if (pos == -1) {
            view = "basic"; //$NON-NLS-1$
            attributes = "basic:" + attributes; //$NON-NLS-1$
        } else {
            view = attributes.substring(0, pos);
        }
        if (!SUPPORTED_FILE_ATTRIBUTE_VIEWS.contains(view)) {
            throw Messages.fileSystemProvider().unsupportedFileAttributeView(view);
        }

        Set<String> allowedAttributes;
        if (attributes.startsWith("basic:")) { //$NON-NLS-1$
            allowedAttributes = BASIC_ATTRIBUTES;
        } else if (attributes.startsWith("owner:")) { //$NON-NLS-1$
            allowedAttributes = OWNER_ATTRIBUTES;
        } else if (attributes.startsWith("posix:")) { //$NON-NLS-1$
            allowedAttributes = POSIX_ATTRIBUTES;
        } else {
            // should not occur
            throw Messages.fileSystemProvider().unsupportedFileAttributeView(attributes.substring(0, attributes.indexOf(':')));
        }

        Map<String, Object> result = getAttributeMap(attributes, allowedAttributes);

        PosixFileAttributes posixAttributes = readAttributes(path, options);

        for (Map.Entry<String, Object> entry : result.entrySet()) {
            switch (entry.getKey()) {
            case "basic:lastModifiedTime": //$NON-NLS-1$
            case "posix:lastModifiedTime": //$NON-NLS-1$
                entry.setValue(posixAttributes.lastModifiedTime());
                break;
            case "basic:lastAccessTime": //$NON-NLS-1$
            case "posix:lastAccessTime": //$NON-NLS-1$
                entry.setValue(posixAttributes.lastAccessTime());
                break;
            case "basic:creationTime": //$NON-NLS-1$
            case "posix:creationTime": //$NON-NLS-1$
                entry.setValue(posixAttributes.creationTime());
                break;
            case "basic:size": //$NON-NLS-1$
            case "posix:size": //$NON-NLS-1$
                entry.setValue(posixAttributes.size());
                break;
            case "basic:isRegularFile": //$NON-NLS-1$
            case "posix:isRegularFile": //$NON-NLS-1$
                entry.setValue(posixAttributes.isRegularFile());
                break;
            case "basic:isDirectory": //$NON-NLS-1$
            case "posix:isDirectory": //$NON-NLS-1$
                entry.setValue(posixAttributes.isDirectory());
                break;
            case "basic:isSymbolicLink": //$NON-NLS-1$
            case "posix:isSymbolicLink": //$NON-NLS-1$
                entry.setValue(posixAttributes.isSymbolicLink());
                break;
            case "basic:isOther": //$NON-NLS-1$
            case "posix:isOther": //$NON-NLS-1$
                entry.setValue(posixAttributes.isOther());
                break;
            case "basic:fileKey": //$NON-NLS-1$
            case "posix:fileKey": //$NON-NLS-1$
                entry.setValue(posixAttributes.fileKey());
                break;
            case "owner:owner": //$NON-NLS-1$
            case "posix:owner": //$NON-NLS-1$
                entry.setValue(posixAttributes.owner());
                break;
            case "posix:group": //$NON-NLS-1$
                entry.setValue(posixAttributes.group());
                break;
            case "posix:permissions": //$NON-NLS-1$
                entry.setValue(posixAttributes.permissions());
                break;
            default:
                // should not occur
                throw new IllegalStateException("unexpected attribute name: " + entry.getKey()); //$NON-NLS-1$
            }
        }
        return result;
    }

    private Map<String, Object> getAttributeMap(String attributes, Set<String> allowedAttributes) {
        int indexOfColon = attributes.indexOf(':');
        String prefix = attributes.substring(0, indexOfColon + 1);
        attributes = attributes.substring(indexOfColon + 1);

        String[] attributeList = attributes.split(","); //$NON-NLS-1$
        Map<String, Object> result = new HashMap<>(allowedAttributes.size());

        for (String attribute : attributeList) {
            String prefixedAttribute = prefix + attribute;
            if (allowedAttributes.contains(prefixedAttribute)) {
                result.put(prefixedAttribute, null);
            } else if ("*".equals(attribute)) { //$NON-NLS-1$
                for (String s : allowedAttributes) {
                    result.put(s, null);
                }
            } else {
                throw Messages.fileSystemProvider().unsupportedFileAttribute(attribute);
            }
        }
        return result;
    }

    FTPFile getFTPFile(FTPPath path) throws IOException {
        try (Client client = clientPool.get()) {
            return getFTPFile(client, path);
        }
    }

    private FTPFile getFTPFile(Client client, FTPPath path) throws IOException {
        final String name = path.fileName();

        FTPFile[] ftpFiles = client.listFiles(path.path(), new FTPFileFilter() {
            @Override
            public boolean accept(FTPFile ftpFile) {
                String fileName = ftpFile.getName();
                return CURRENT_DIR.equals(fileName) || (name != null && containsFileName(fileName, name));
            }
        });
        client.throwIfEmpty(path.path(), ftpFiles);
        if (ftpFiles.length == 1) {
            return ftpFiles[0];
        }
        for (FTPFile ftpFile : ftpFiles) {
            if (CURRENT_DIR.equals(ftpFile.getName())) {
                return ftpFile;
            }
        }
        throw new IllegalStateException();
    }

    private boolean containsFileName(String ftpFilePath, String fileName) {
        int pos = ftpFilePath.lastIndexOf(getSeparator());
        if (pos == -1) {
            return ftpFilePath.equals(fileName);
        } else {
            return pos != (ftpFilePath.length() - 1) && ftpFilePath.substring(pos + 1).equals(fileName);
        }
    }

    private FTPFile findFTPFile(Client client, FTPPath path) throws IOException {
        try {
            return getFTPFile(client, path);
        } catch (@SuppressWarnings("unused") NoSuchFileException e) {
            return null;
        }
    }

    private FTPFile getLink(Client client, FTPFile ftpFile, FTPPath path) throws IOException {
        if (ftpFile.getLink() != null) {
            return ftpFile;
        }
        if (ftpFile.isDirectory() && CURRENT_DIR.equals(ftpFile.getName())) {
            // The file is returned using getFTPFile(), which returns the . (current directory) entry for directories.
            // List the parent (if any) instead.

            final String parentPath = toAbsolutePath(path).parentPath();
            final String name = path.fileName();

            if (parentPath == null) {
                // path is /, there is no link
                return null;
            }

            FTPFile[] ftpFiles = client.listFiles(parentPath, new FTPFileFilter() {
                @Override
                public boolean accept(FTPFile ftpFile) {
                    return (ftpFile.isDirectory() || ftpFile.isSymbolicLink()) && name.equals(ftpFile.getName());
                }
            });
            client.throwIfEmpty(path.path(), ftpFiles);
            return ftpFiles[0].getLink() == null ? null : ftpFiles[0];
        }
        return null;
    }

    long getTotalSpace() {
        // FTPClient does not support retrieving the total space
        return Long.MAX_VALUE;
    }

    long getUsableSpace() {
        // FTPClient does not support retrieving the usable space
        return Long.MAX_VALUE;
    }

    long getUnallocatedSpace() {
        // FTPClient does not support retrieving the unallocated space
        return Long.MAX_VALUE;
    }
}
