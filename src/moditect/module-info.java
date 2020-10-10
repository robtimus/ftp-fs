module com.github.robtimus.filesystems.ftp {
    requires com.github.robtimus.filesystems;
    requires transitive org.apache.commons.net;
    requires static org.slf4j;

    exports com.github.robtimus.filesystems.ftp;

    provides java.nio.file.spi.FileSystemProvider with com.github.robtimus.filesystems.ftp.FTPFileSystemProvider;
}
