package com.github.robtimus.filesystems.ftp;

import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.util.List;

public interface FileSystemStrategy {

    /**
     * Find children directories.
     *
     * @param client
     * @param path
     * @return
     * @throws IOException
     */
    List<FTPFile> getChildren(FTPFileSystemClient client, FTPPath path) throws IOException;

    /**
     * Find a ftp path.
     *
     * @param client
     * @param path
     * @return
     * @throws IOException
     */
    FTPFile getFTPFile(FTPFileSystemClient client, FTPPath path) throws IOException;

    /**
     * Find a link.
     *
     * @param client
     * @param ftpFile
     * @param path
     * @return
     * @throws IOException
     */
    FTPFile getLink(FTPFileSystemClient client, FTPFile ftpFile, FTPPath path) throws IOException;

}
