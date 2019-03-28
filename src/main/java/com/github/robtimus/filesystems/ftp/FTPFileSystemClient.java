package com.github.robtimus.filesystems.ftp;

import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPFileFilter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.OpenOption;
import java.util.Calendar;
import java.util.Collection;

public interface FTPFileSystemClient {

    void keepAlive() throws IOException;

    String pwd() throws IOException;

    InputStream newInputStream(String path, OpenOptions options) throws IOException;

    OutputStream newOutputStream(String path, OpenOptions options) throws IOException;

    void storeFile(String path, InputStream local, TransferOptions options, Collection<? extends OpenOption> openOptions) throws IOException;

    FTPFile[] listFiles(String path) throws IOException;

    FTPFile[] listFiles(String path, FTPFileFilter filter) throws IOException;

    void throwIfEmpty(String path, FTPFile[] ftpFiles) throws IOException;

    void mkdir(String path) throws IOException;

    void delete(String path, boolean isDirectory) throws IOException;

    void rename(String source, String target) throws IOException;

    Calendar mdtm(String path) throws IOException;

}
