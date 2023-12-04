/*
 * FTPFileSystemProvider.java
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
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileOwnerAttributeView;
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.github.robtimus.filesystems.FileSystemMap;
import com.github.robtimus.filesystems.LinkOptionSupport;
import com.github.robtimus.filesystems.Messages;
import com.github.robtimus.filesystems.URISupport;

/**
 * A provider for FTP file systems.
 *
 * @author Rob Spoor
 */
public class FTPFileSystemProvider extends FileSystemProvider {

    private final FileSystemMap<FTPFileSystem> fileSystems = new FileSystemMap<>(this::createFileSystem);

    /**
     * Returns the URI scheme that identifies this provider: {@code ftp}.
     */
    @Override
    public String getScheme() {
        return "ftp"; //$NON-NLS-1$
    }

    private FTPFileSystem createFileSystem(URI uri, Map<String, ?> env) throws IOException {
        FTPEnvironment environment = (FTPEnvironment) env;
        return new FTPFileSystem(this, uri, environment);
    }

    /**
     * Constructs a new {@code FileSystem} object identified by a URI.
     * <p>
     * The URI must have a {@link URI#getScheme() scheme} equal to {@link #getScheme()}, and no {@link URI#getQuery() query} or
     * {@link URI#getFragment() fragment}. Authentication credentials can be set either through the URI's {@link URI#getUserInfo() user information},
     * or through the given environment map, preferably through {@link FTPEnvironment}. The default directory can be set either through the URI's
     * {@link URI#getPath() path} or through the given environment map, preferably through {@link FTPEnvironment}.
     * <p>
     * This provider allows multiple file systems per host, but only one file system per user on a host.
     * Once a file system is {@link FileSystem#close() closed}, this provider allows a new file system to be created with the same URI and credentials
     * as the closed file system.
     */
    @Override
    public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
        FTPEnvironment environment = copy(env);

        boolean allowUserInfo = !environment.hasUsername();
        boolean allowPath = !environment.hasDefaultDir();

        checkURI(uri, allowUserInfo, allowPath);

        addUserInfoIfNeeded(environment, uri.getUserInfo());
        addDefaultDirIfNeeded(environment, uri.getPath());

        String username = environment.getUsername();
        URI normalizedURI = normalizeWithUsername(uri, username);
        return fileSystems.add(normalizedURI, environment);
    }

    FTPEnvironment copy(Map<String, ?> env) {
        return FTPEnvironment.copy(env);
    }

    private void addUserInfoIfNeeded(FTPEnvironment environment, String userInfo) {
        if (userInfo != null) {
            int indexOfColon = userInfo.indexOf(':');
            if (indexOfColon == -1) {
                environment.withCredentials(userInfo, new char[0]);
            } else {
                environment.withCredentials(userInfo.substring(0, indexOfColon), userInfo.substring(indexOfColon + 1).toCharArray());
            }
        }
    }

    private void addDefaultDirIfNeeded(FTPEnvironment environment, String path) {
        if (path != null && !path.isEmpty()) {
            environment.withDefaultDirectory(path);
        }
    }

    /**
     * Returns an existing {@code FileSystem} created by this provider.
     * <p>
     * The URI must have a {@link URI#getScheme() scheme} equal to {@link #getScheme()}, and no non-empty {@link URI#getPath() path},
     * {@link URI#getQuery() query} or {@link URI#getFragment() fragment}. Because the original credentials were possibly provided through an
     * environment map, the URI can contain {@link URI#getUserInfo() user information}, although this should not contain a password for security
     * reasons.
     * <p>
     * Once a file system is {@link FileSystem#close() closed}, this provider will throw a {@link FileSystemNotFoundException}.
     */
    @Override
    public FileSystem getFileSystem(URI uri) {
        checkURI(uri, true, false);

        URI normalizedURI = normalizeWithoutPassword(uri);
        return fileSystems.get(normalizedURI);
    }

    /**
     * Return a {@code Path} object by converting the given {@link URI}. The resulting {@code Path} is associated with a {@link FileSystem} that
     * already exists, or is constructed automatically.
     * <p>
     * The URI must have a {@link URI#getScheme() scheme} equal to {@link #getScheme()}, and no {@link URI#getQuery() query} or
     * {@link URI#getFragment() fragment}. Because the original credentials were possibly provided through an environment map,
     * the URI can contain {@link URI#getUserInfo() user information}, although for security reasons this should only contain a password to support
     * automatically creating file systems.
     * <p>
     * If no matching file system existed yet, a new one is created. The default environment for {@link FTPEnvironment#setDefault(FTPEnvironment) FTP}
     * or {@link FTPSEnvironment#setDefault(FTPSEnvironment) FTPS} is used for this, to allow configuring the resulting file system.
     * <p>
     * Remember to close any newly created file system.
     */
    @Override
    @SuppressWarnings("resource")
    public Path getPath(URI uri) {
        checkURI(uri, true, true);

        URI normalizedURI = normalizeWithoutPassword(uri);
        FTPEnvironment env = copyOfDefaultEnvironment();

        addUserInfoIfNeeded(env, uri.getUserInfo());
        // Do not add any default dir

        try {
            FTPFileSystem fs = fileSystems.addIfNotExists(normalizedURI, env);
            return fs.getPath(uri.getPath());
        } catch (IOException e) {
            // FileSystemProvider.getPath mandates that a FileSystemNotFoundException should be thrown if no file system could be created
            // automatically
            FileSystemNotFoundException exception = new FileSystemNotFoundException(normalizedURI.toString());
            exception.initCause(e);
            throw exception;
        }
    }

    FTPEnvironment copyOfDefaultEnvironment() {
        return FTPEnvironment.copyOfDefault();
    }

    private void checkURI(URI uri, boolean allowUserInfo, boolean allowPath) {
        if (!uri.isAbsolute()) {
            throw Messages.uri().notAbsolute(uri);
        }
        if (!getScheme().equalsIgnoreCase(uri.getScheme())) {
            throw Messages.uri().invalidScheme(uri, getScheme());
        }
        if (!allowUserInfo && uri.getUserInfo() != null && !uri.getUserInfo().isEmpty()) {
            throw Messages.uri().hasUserInfo(uri);
        }
        if (uri.isOpaque()) {
            throw Messages.uri().notHierarchical(uri);
        }
        if (!allowPath && !hasEmptyPath(uri) && !"/".equals(uri.getPath())) { //$NON-NLS-1$
            throw Messages.uri().hasPath(uri);
        }
        if (uri.getQuery() != null && !uri.getQuery().isEmpty()) {
            throw Messages.uri().hasQuery(uri);
        }
        if (uri.getFragment() != null && !uri.getFragment().isEmpty()) {
            throw Messages.uri().hasFragment(uri);
        }
    }

    private static boolean hasEmptyPath(URI uri) {
        return uri.getPath() == null || uri.getPath().isEmpty();
    }

    void removeFileSystem(URI uri) {
        URI normalizedURI = normalizeWithoutPassword(uri);
        fileSystems.remove(normalizedURI);
    }

    static URI normalizeWithoutPassword(URI uri) {
        String userInfo = uri.getUserInfo();
        if (userInfo == null && hasEmptyPath(uri) && uri.getQuery() == null && uri.getFragment() == null) {
            // nothing to normalize, return the URI
            return uri;
        }
        String username = null;
        if (userInfo != null) {
            int index = userInfo.indexOf(':');
            username = index == -1 ? userInfo : userInfo.substring(0, index);
        }
        // no path, query or fragment
        return URISupport.create(uri.getScheme(), username, uri.getHost(), uri.getPort(), null, null, null);
    }

    static URI normalizeWithUsername(URI uri, String username) {
        if (username == null && uri.getUserInfo() == null && hasEmptyPath(uri) && uri.getQuery() == null && uri.getFragment() == null) {
            // nothing to normalize or add, return the URI
            return uri;
        }
        // no path, query or fragment
        return URISupport.create(uri.getScheme(), username, uri.getHost(), uri.getPort(), null, null, null);
    }

    /**
     * Opens a file, returning an input stream to read from the file.
     * This method works in exactly the manner specified by the {@link Files#newInputStream(Path, OpenOption...)} method.
     * <p>
     * In addition to the standard open options, this method also supports single occurrences of each of {@link FileType}, {@link FileStructure} and
     * {@link FileTransferMode}. These three option types, with defaults of {@link FileType#binary()}, {@link FileStructure#FILE} and
     * {@link FileTransferMode#STREAM}, persist for all calls that support file transfers:
     * <ul>
     * <li>{@link #newInputStream(Path, OpenOption...)}</li>
     * <li>{@link #newOutputStream(Path, OpenOption...)}</li>
     * <li>{@link #newByteChannel(Path, Set, FileAttribute...)}</li>
     * <li>{@link #copy(Path, Path, CopyOption...)}</li>
     * <li>{@link #move(Path, Path, CopyOption...)}</li>
     * </ul>
     * <p>
     * Note: while the returned input stream is not closed, the path's file system will have one available connection fewer.
     * It is therefore essential that the input stream is closed as soon as possible.
     */
    @Override
    public InputStream newInputStream(Path path, OpenOption... options) throws IOException {
        return toFTPPath(path).newInputStream(options);
    }

    /**
     * Opens or creates a file, returning an output stream that may be used to write bytes to the file.
     * This method works in exactly the manner specified by the {@link Files#newOutputStream(Path, OpenOption...)} method.
     * <p>
     * In addition to the standard open options, this method also supports single occurrences of each of {@link FileType}, {@link FileStructure} and
     * {@link FileTransferMode}. These three option types, with defaults of {@link FileType#binary()}, {@link FileStructure#FILE} and
     * {@link FileTransferMode#STREAM}, persist for all calls that support file transfers:
     * <ul>
     * <li>{@link #newInputStream(Path, OpenOption...)}</li>
     * <li>{@link #newOutputStream(Path, OpenOption...)}</li>
     * <li>{@link #newByteChannel(Path, Set, FileAttribute...)}</li>
     * <li>{@link #copy(Path, Path, CopyOption...)}</li>
     * <li>{@link #move(Path, Path, CopyOption...)}</li>
     * </ul>
     * <p>
     * Note: while the returned output stream is not closed, the path's file system will have one available connection fewer.
     * It is therefore essential that the output stream is closed as soon as possible.
     */
    @Override
    public OutputStream newOutputStream(Path path, OpenOption... options) throws IOException {
        return toFTPPath(path).newOutputStream(options);
    }

    /**
     * Opens or creates a file, returning a seekable byte channel to access the file.
     * This method works in exactly the manner specified by the {@link Files#newByteChannel(Path, Set, FileAttribute...)} method.
     * <p>
     * In addition to the standard open options, this method also supports single occurrences of each of {@link FileType}, {@link FileStructure} and
     * {@link FileTransferMode}. These three option types, with defaults of {@link FileType#binary()}, {@link FileStructure#FILE} and
     * {@link FileTransferMode#STREAM}, persist for all calls that support file transfers:
     * <ul>
     * <li>{@link #newInputStream(Path, OpenOption...)}</li>
     * <li>{@link #newOutputStream(Path, OpenOption...)}</li>
     * <li>{@link #newByteChannel(Path, Set, FileAttribute...)}</li>
     * <li>{@link #copy(Path, Path, CopyOption...)}</li>
     * <li>{@link #move(Path, Path, CopyOption...)}</li>
     * </ul>
     * <p>
     * This method does not support any file attributes to be set. If any file attributes are given, an {@link UnsupportedOperationException} will be
     * thrown.
     * <p>
     * Note: while the returned channel is not closed, the path's file system will have one available connection fewer.
     * It is therefore essential that the channel is closed as soon as possible.
     */
    @Override
    public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options, FileAttribute<?>... attrs) throws IOException {
        return toFTPPath(path).newByteChannel(options, attrs);
    }

    @Override
    public DirectoryStream<Path> newDirectoryStream(Path dir, Filter<? super Path> filter) throws IOException {
        return toFTPPath(dir).newDirectoryStream(filter);
    }

    /**
     * Creates a new directory.
     * This method works in exactly the manner specified by the {@link Files#createDirectory(Path, FileAttribute...)} method.
     * <p>
     * This method does not support any file attributes to be set. If any file attributes are given, an {@link UnsupportedOperationException} will be
     * thrown.
     */
    @Override
    public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
        toFTPPath(dir).createDirectory(attrs);
    }

    @Override
    public void delete(Path path) throws IOException {
        toFTPPath(path).delete();
    }

    @Override
    public Path readSymbolicLink(Path link) throws IOException {
        return toFTPPath(link).readSymbolicLink();
    }

    /**
     * Copy a file to a target file.
     * This method works in exactly the manner specified by the {@link Files#copy(Path, Path, CopyOption...)} method except that both the source and
     * target paths must be associated with this provider.
     * <p>
     * In addition to the standard copy options, this method also supports single occurrences of each of {@link FileType}, {@link FileStructure} and
     * {@link FileTransferMode}. These three option types, with defaults of {@link FileType#binary()}, {@link FileStructure#FILE} and
     * {@link FileTransferMode#STREAM}, persist for all calls that support file transfers:
     * <ul>
     * <li>{@link #newInputStream(Path, OpenOption...)}</li>
     * <li>{@link #newOutputStream(Path, OpenOption...)}</li>
     * <li>{@link #newByteChannel(Path, Set, FileAttribute...)}</li>
     * <li>{@link #copy(Path, Path, CopyOption...)}</li>
     * <li>{@link #move(Path, Path, CopyOption...)}</li>
     * </ul>
     * <p>
     * {@link StandardCopyOption#COPY_ATTRIBUTES} and {@link StandardCopyOption#ATOMIC_MOVE} are not supported though.
     */
    @Override
    public void copy(Path source, Path target, CopyOption... options) throws IOException {
        toFTPPath(source).copy(toFTPPath(target), options);
    }

    /**
     * Move or rename a file to a target file.
     * This method works in exactly the manner specified by the {@link Files#move(Path, Path, CopyOption...)} method except that both the source and
     * target paths must be associated with this provider.
     * <p>
     * In addition to the standard copy options, this method also supports single occurrences of each of {@link FileType}, {@link FileStructure} and
     * {@link FileTransferMode}. These three option types, with defaults of {@link FileType#binary()}, {@link FileStructure#FILE} and
     * {@link FileTransferMode#STREAM}, persist for all calls that support file transfers:
     * <ul>
     * <li>{@link #newInputStream(Path, OpenOption...)}</li>
     * <li>{@link #newOutputStream(Path, OpenOption...)}</li>
     * <li>{@link #newByteChannel(Path, Set, FileAttribute...)}</li>
     * <li>{@link #copy(Path, Path, CopyOption...)}</li>
     * <li>{@link #move(Path, Path, CopyOption...)}</li>
     * </ul>
     * <p>
     * {@link StandardCopyOption#COPY_ATTRIBUTES} is not supported though. {@link StandardCopyOption#ATOMIC_MOVE} is only supported if the paths have
     * the same file system.
     */
    @Override
    public void move(Path source, Path target, CopyOption... options) throws IOException {
        toFTPPath(source).move(toFTPPath(target), options);
    }

    @Override
    public boolean isSameFile(Path path, Path path2) throws IOException {
        return toFTPPath(path).isSameFile(path2);
    }

    @Override
    public boolean isHidden(Path path) throws IOException {
        return toFTPPath(path).isHidden();
    }

    @Override
    public FileStore getFileStore(Path path) throws IOException {
        return toFTPPath(path).getFileStore();
    }

    @Override
    public void checkAccess(Path path, AccessMode... modes) throws IOException {
        toFTPPath(path).checkAccess(modes);
    }

    /**
     * Returns a file attribute view of a given type.
     * This method works in exactly the manner specified by the {@link Files#getFileAttributeView(Path, Class, LinkOption...)} method.
     * <p>
     * This provider supports {@link BasicFileAttributeView}, {@link FileOwnerAttributeView} and {@link PosixFileAttributeView}.
     * All other classes will result in a {@code null} return value.
     * <p>
     * Note that the returned {@link FileAttributeView} is read-only; any attempt to change any attributes through the view will result in an
     * {@link UnsupportedOperationException} to be thrown.
     */
    @Override
    public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
        Objects.requireNonNull(type);
        boolean followLinks = LinkOptionSupport.followLinks(options);
        return toFTPPath(path).getFileAttributeView(type, followLinks);
    }

    /**
     * Reads a file's attributes as a bulk operation.
     * This method works in exactly the manner specified by the {@link Files#readAttributes(Path, Class, LinkOption...)} method.
     * <p>
     * This provider supports {@link BasicFileAttributes} and {@link PosixFileAttributes} (there is no {@code FileOwnerFileAttributes}).
     * All other classes will result in an {@link UnsupportedOperationException} to be thrown.
     */
    @Override
    public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options) throws IOException {
        if (type == BasicFileAttributes.class || type == PosixFileAttributes.class) {
            boolean followLinks = LinkOptionSupport.followLinks(options);
            return type.cast(toFTPPath(path).readAttributes(followLinks));
        }
        throw Messages.fileSystemProvider().unsupportedFileAttributesType(type);
    }

    /**
     * Reads a set of file attributes as a bulk operation.
     * This method works in exactly the manner specified by the {@link Files#readAttributes(Path, String, LinkOption...)} method.
     * <p>
     * This provider supports views {@code basic}, {@code owner} and {code posix}, where {@code basic} will be used if no view is given.
     * All other views will result in an {@link UnsupportedOperationException} to be thrown.
     */
    @Override
    public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options) throws IOException {
        boolean followLinks = LinkOptionSupport.followLinks(options);
        return toFTPPath(path).readAttributes(attributes, followLinks);
    }

    /**
     * Sets the value of a file attribute.
     * This method works in exactly the manner specified by the {@link Files#setAttribute(Path, String, Object, LinkOption...)} method.
     * <p>
     * This provider does not support attributes for paths to be set. This method will always throw an {@link UnsupportedOperationException}.
     */
    @Override
    public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
        throw Messages.unsupportedOperation(FileSystemProvider.class, "setAttribute"); //$NON-NLS-1$
    }

    private static FTPPath toFTPPath(Path path) {
        Objects.requireNonNull(path);
        if (path instanceof FTPPath) {
            return (FTPPath) path;
        }
        throw new ProviderMismatchException();
    }

    /**
     * Send a keep-alive signal for an FTP file system.
     *
     * @param fs The FTP file system to send a keep-alive signal for.
     * @throws ProviderMismatchException If the given file system is not an FTP file system (not created by an {@code FTPFileSystemProvider}).
     * @throws IOException If an I/O error occurred.
     */
    public static void keepAlive(FileSystem fs) throws IOException {
        if (fs instanceof FTPFileSystem) {
            ((FTPFileSystem) fs).keepAlive();
            return;
        }
        throw new ProviderMismatchException();
    }
}
