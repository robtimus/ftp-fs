# ftp-fs

The `ftp-fs` library provides support for FTP and FTPS NIO.2 file systems, allowing FTP servers to be accessed in a similar way to local file systems.

## Creating file systems

If the FTP file system library is available on the class path, it will register [FileSystemProviders](http://docs.oracle.com/javase/8/docs/api/java/nio/file/spi/FileSystemProvider.html) for schemes `ftp` and `ftps`. This allows you to create FTP and FTPS file systems using the `newFileSystem` methods of class [FileSystems](http://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystems.html). You can use classes [FTPEnvironment](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPEnvironment.html) and [FTPSEnvironment](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPSEnvironment.html) to help create the environment maps for those methods:

    FTPEnvironment env = new FTPEnvironment()
            .withCredentials(username, password);
    FileSystem fs = FileSystems.newFileSystem(URI.create("ftp://example.org"), env);

Note that, for security reasons, it's not allowed to pass the credentials as part of the URI when creating a file system. It must be passed through the environment, as shown above.

## Creating paths

After a file system has been created, [Paths](http://docs.oracle.com/javase/8/docs/api/java/nio/file/Path.html) can be created through the file system itself using its [getPath](http://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystem.html#getPath-java.lang.String-java.lang.String...-) method. As long as the file system is not closed, it's also possible to use [Paths.get](http://docs.oracle.com/javase/8/docs/api/java/nio/file/Paths.html#get-java.net.URI-). Note that if the file system was created with credentials, the username must be part of the URL. For instance:

    // created without credentials
    Path path1 = Paths.get(URI.create("ftp://example.org"));
    // created with credentials
    Path path2 = Paths.get(URI.create("ftp://username@example.org")); 

If the username in the URI does not match the username used to create the file system, this will cause a [FileSystemNotFoundException](http://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystemNotFoundException.html) to be thrown.

## Attributes

### File attributes

FTP file systems fully support read-access to the following attributes:

* Attributes defined in [BasicFileAttributeView](http://docs.oracle.com/javase/8/docs/api/java/nio/file/attribute/BasicFileAttributeView.html) and [BasicFileAttributes](http://docs.oracle.com/javase/8/docs/api/java/nio/file/attribute/BasicFileAttributes.html), available both with and without prefixes `basic:` or `posix:`.
* Attributes defined in [FileOwnerAttributeView](http://docs.oracle.com/javase/8/docs/api/java/nio/file/attribute/FileOwnerAttributeView.html), available with prefixes `owner:` or `posix:`.
* Attributes defined in [PosixFileAttributeView](http://docs.oracle.com/javase/8/docs/api/java/nio/file/attribute/PosixFileAttributeView.html) and [PosixFileAttributes](http://docs.oracle.com/javase/8/docs/api/java/nio/file/attribute/PosixFileAttributes.html), available with prefix `posix:`.

Attempting to set any of these attributes, either through one of the file attribute views or through a file system, will result in an [UnsupportedOperationException](http://docs.oracle.com/javase/8/docs/api/java/lang/UnsupportedOperationException.html).

### File store attributes

When calling [getAttribute](http://docs.oracle.com/javase/8/docs/api/java/nio/file/FileStore.html#getAttribute-java.lang.String-) on a file store, the following attributes are supported:

* `totalSpace`: returns the same value as the [getTotalSpace](http://docs.oracle.com/javase/8/docs/api/java/nio/file/FileStore.html#getTotalSpace--) method.
* `usableSpace`: returns the same value as the [getUsableSpace](http://docs.oracle.com/javase/8/docs/api/java/nio/file/FileStore.html#getUsableSpace--) method.
* `unallocatedSpace`: returns the same value as the [getUnallocatedSpace](http://docs.oracle.com/javase/8/docs/api/java/nio/file/FileStore.html#getUnallocatedSpace--) method.

Because FTP servers do not (often) return these values, by default these methods will all return `Long.MAX_VALUE`. For `totalSpace`, it's possible to return the actual total space, by calling [withActualTotalSpaceCalculation](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPEnvironment.html#withActualTotalSpaceCalculation-boolean-)
 on an [FTPEnvironment](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPEnvironment.html) instance before using it to create the file system. Beware that this call will perform multiple calls to the FTP server. It's therefore not advised to do this.

There is no support for [FileStoreAttributeView](http://docs.oracle.com/javase/8/docs/api/java/nio/file/attribute/FileStoreAttributeView.html). Calling [getFileStoreAttributeView](http://docs.oracle.com/javase/8/docs/api/java/nio/file/FileStore.html#getFileStoreAttributeView-java.lang.Class-) on a file store will simply return `null`.

## FTPS support

To create an FTPS connection instead of an FTP connection, use `ftps` as the scheme. Also, use class [FTPSEnvironment](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPSEnvironment.html) instead of class [FTPEnvironment](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPEnvironment.html) to create the file system. Using an [FTPEnvironment](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPEnvironment.html) instance is still allowed, but you will not be able to specify FTPS specific properties.

## Error handling

Unfortunately, FTP servers can use the same code for multiple erroneous situations. For example, code 550 can indicate that a file does not exist, or that access to an existing file is not allowed. Because of this, most methods do not throw the correct exception ([NoSuchFileException](http://docs.oracle.com/javase/8/docs/api/java/nio/file/NoSuchFileException.html), [AccessDeniedException](http://docs.oracle.com/javase/8/docs/api/java/nio/file/AccessDeniedException.html), etc).

To allow this behaviour to be modified, class [FTPEnvironment](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPEnvironment.html) has method [withFileSystemExceptionFactory](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPEnvironment.html#withFileSystemExceptionFactory-com.github.robtimus.filesystems.ftp.FileSystemExceptionFactory-) that allows you to specify a custom [FileSystemExceptionFactory](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FileSystemExceptionFactory.html) implementation which will be used to create exceptions based on the reply code and string of the command that triggered the error. By default, an instance of class [DefaultFileSystemExceptionFactory](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/DefaultFileSystemExceptionFactory.html) is used.

The `ftp-fs` library provides subclasses for [FileSystemException](http://docs.oracle.com/javase/8/docs/api/java/nio/file/FileSystemException.html) and several of its subclasses to allow the FTP server's reply code and string to be reserved. Instances of these classes can be returned by [FileSystemExceptionFactory](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FileSystemExceptionFactory.html) implementations as needed.

## Thread safety

The FTP protocol is fundamentally not thread safe. To overcome this limitation, FTP file systems maintain multiple connections to FTP servers. The number of connections determines the number of concurrent operations that can be executed. If all connections are busy, a new operation will block until a connection becomes available. Class [FTPEnvironment](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPEnvironment.html) has method [withClientConnectionCount](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPEnvironment.html#withClientConnectionCount-int-) that allows you to specify the number of connections to use. If no connection count is explicitly set, the default will be 5. 

When a stream or channel is opened for reading or writing, the connection will block because it will wait for the download or upload to finish. This will not occur until the stream or channel is closed. It is therefore advised to close streams and channels as soon as possible.

## Connection management

Because FTP file systems use multiple connections to an FTP server, it's possible that one or more of these connections become stale. Class [FTPFileSystemProvider](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPFileSystemProvider.html) has static method [keepAlive](https://robtimus.github.io/ftp-fs/apidocs/com/github/robtimus/filesystems/ftp/FTPFileSystemProvider.html#keepAlive-java.nio.file.FileSystem-) that, if given an instance of an FTP file system, will send a keep-alive signal (NOOP) over each of its idle connections. You should ensure that this method is called on a regular interval.

## Limitations

FTP file systems knows the following limitations:

* All paths use `/` as separator. `/` is not allowed inside file or directory names.
* File attributes cannot be set, either when creating files or directories or afterwards.
* Symbolic links can be read and traversed, but not created.
* There is no support for hard links.
* Files can be marked as executable if the FTP server indicates it is. That does not mean the file can be executed in the local JVM.
* [SeekableByteChannel](http://docs.oracle.com/javase/8/docs/api/java/nio/channels/SeekableByteChannel.html) is supported because it's used by [Files.createFile](http://docs.oracle.com/javase/8/docs/api/java/nio/file/Files.html#createFile-java.nio.file.Path-java.nio.file.attribute.FileAttribute...-). However, these channels do not support seeking specific positions or truncating.
* When copying files, an FTP file system will attempt to claim 2 connections, one for downloading and one for uploading. To prevent deadlocks, if only one connection is available, files are downloaded to memory before being uploaded over the same connection. This means that copying large files may cause memory issues.
* There is no support for [UserPrincipalLookupService](http://docs.oracle.com/javase/8/docs/api/java/nio/file/attribute/UserPrincipalLookupService.html).
* There is no support for [WatchService](http://docs.oracle.com/javase/8/docs/api/java/nio/file/WatchService.html).
