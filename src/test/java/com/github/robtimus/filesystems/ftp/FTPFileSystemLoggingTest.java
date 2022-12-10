/*
 * FTPFileSystemLoggingTest.java
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

import static com.github.robtimus.filesystems.ftp.StandardFTPFileStrategyFactory.UNIX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.matchesRegex;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.log4j.Appender;
import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.github.robtimus.junit.support.extension.testlogger.Reload4jLoggerContext;
import com.github.robtimus.junit.support.extension.testlogger.TestLogger;

@SuppressWarnings("nls")
class FTPFileSystemLoggingTest extends AbstractFTPFileSystemTest {

    FTPFileSystemLoggingTest() {
        // not going to use any default FTP file system, so FTPFile strategy factory doesn't matter
        super(true, null);
    }

    @Test
    void testLogging(@TestLogger.ForClass(FTPClientPool.class) Reload4jLoggerContext logger) throws IOException {
        Appender appender = mock(Appender.class);
        logger.setLevel(Level.TRACE)
                .setAppender(appender)
                .useParentAppenders(false);

        URI uri = getURI();
        try (FileSystem fs = FileSystems.newFileSystem(uri, createEnv(UNIX))) {
            FTPFileSystemProvider.keepAlive(fs);
        }

        ArgumentCaptor<LoggingEvent> captor = ArgumentCaptor.forClass(LoggingEvent.class);
        verify(appender, atLeast(1)).doAppend(captor.capture());
        List<String> debugMessages = new ArrayList<>();
        List<String> traceMessages = new ArrayList<>();
        Set<String> names = new HashSet<>();
        Set<Level> levels = new HashSet<>();
        for (LoggingEvent event : captor.getAllValues()) {
            names.add(event.getLoggerName());
            levels.add(event.getLevel());
            if (Level.DEBUG.equals(event.getLevel())) {
                String message = assertInstanceOf(String.class, event.getMessage());
                debugMessages.add(message);
            } else if (Level.TRACE.equals(event.getLevel())) {
                String message = assertInstanceOf(String.class, event.getMessage());
                traceMessages.add(message);
            }
        }

        assertEquals(Collections.singleton(FTPClientPool.class.getName()), names);
        assertEquals(new HashSet<>(Arrays.asList(Level.DEBUG, Level.TRACE)), levels);

        // Don't test all messages, that's all handled by the pool
        // Just test the prefixes
        String hostname = uri.getHost();
        int port = uri.getPort();
        String prefix = port == -1 ? hostname : hostname + ":" + port;
        assertThat(debugMessages, everyItem(startsWith(prefix + " - ")));

        assertThat(debugMessages, hasItem(matchesRegex(prefix + " - client-\\d+ created")));

        assertThat(traceMessages, hasItem(matchesRegex(prefix + " - client-\\d+: FTP command: NOOP")));
        assertThat(traceMessages, hasItem(matchesRegex(prefix + " - client-\\d+: FTP reply  : 200 NOOP completed.")));
    }

    @Test
    void testCommandListener(@TestLogger.ForClass(FTPClientPool.class) Reload4jLoggerContext logger) throws IOException {
        Appender appender = mock(Appender.class);
        logger.setAppender(appender)
                .useParentAppenders(false);

        URI uri = getURI();
        try (FileSystem fs = FileSystems.newFileSystem(uri, createEnv(UNIX))) {
            logger.setLevel(Level.TRACE);
            reset(appender);
            FTPFileSystemProvider.keepAlive(fs);
            assertKeepAliveLogged(appender);

            // still trace level
            reset(appender);
            FTPFileSystemProvider.keepAlive(fs);
            assertKeepAliveLogged(appender);

            logger.setLevel(Level.DEBUG);
            reset(appender);
            FTPFileSystemProvider.keepAlive(fs);
            assertKeepAliveNotLogged(appender);

            // still debug level
            reset(appender);
            FTPFileSystemProvider.keepAlive(fs);
            assertKeepAliveNotLogged(appender);
        }
    }

    private void assertKeepAliveLogged(Appender appender) {
        ArgumentCaptor<LoggingEvent> captor = ArgumentCaptor.forClass(LoggingEvent.class);
        verify(appender, atLeast(1)).doAppend(captor.capture());

        List<String> traceMessages = captor.getAllValues().stream()
                .filter(event -> Level.TRACE.equals(event.getLevel()))
                .map(LoggingEvent::getMessage)
                .map(String.class::cast)
                .collect(Collectors.toList());

        // Only a single client exists in the pool, so only one NOOP is sent
        assertThat(traceMessages, contains(
                matchesRegex(".* - client-\\d+: FTP command: NOOP"),
                matchesRegex(".* - client-\\d+: FTP reply  : 200 NOOP completed.")
        ));
    }

    private void assertKeepAliveNotLogged(Appender appender) {
        ArgumentCaptor<LoggingEvent> captor = ArgumentCaptor.forClass(LoggingEvent.class);
        verify(appender, atLeast(1)).doAppend(captor.capture());

        List<String> traceMessages = captor.getAllValues().stream()
                .filter(event -> Level.TRACE.equals(event.getLevel()))
                .map(LoggingEvent::getMessage)
                .map(String.class::cast)
                .collect(Collectors.toList());

        assertEquals(Collections.emptyList(), traceMessages);
    }
}
