module com.github.robtimus.filesystems.ftp {
    requires com.github.robtimus.filesystems;
    requires transitive org.apache.commons.net;
    requires com.github.robtimus.pool;

    exports com.github.robtimus.filesystems.ftp;

    provides java.nio.file.spi.FileSystemProvider with com.github.robtimus.filesystems.ftp.FTPFileSystemProvider,
            com.github.robtimus.filesystems.ftp.FTPSFileSystemProvider;
}
