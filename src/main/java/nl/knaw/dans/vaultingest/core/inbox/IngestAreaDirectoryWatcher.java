/*
 * Copyright (C) 2023 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.vaultingest.core.inbox;

import io.dropwizard.lifecycle.Managed;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@RequiredArgsConstructor
public class IngestAreaDirectoryWatcher implements IngestAreaWatcher, Managed {
    @NonNull
    private final long pollingInterval;
    @NonNull
    private final Path directory;

    private FileAlterationMonitor monitor;

    @Override
    public void start(IngestAreaItemCreated callback) {
        log.debug("Starting listener; path = {}", directory);
        var filter = FileFilterUtils.and(
            FileFilterUtils.directoryFileFilter(),
            FileFilterUtils.asFileFilter(f -> f.getParentFile().equals(directory.toFile()))
        );

        var observer = new FileAlterationObserver(directory.toFile(), filter);
        observer.addListener(new EventHandler(callback));
        monitor = new FileAlterationMonitor(pollingInterval);
        monitor.addObserver(observer);

        log.debug("Processing existing items in {}", directory);
        processExistingItems(callback);

        try {
            log.debug("Starting FileAlterationMonitor for directory {}", directory);
            monitor.start();
        }
        catch (Exception e) {
            throw new IllegalStateException(String.format("Could not start monitoring %s", directory), e);
        }
    }

    @Override
    public void stop() {
        try {
            log.debug("Stopping FileAlterationMonitor for directory {}", directory);
            monitor.stop();
        }
        catch (Exception e) {
            throw new IllegalStateException(String.format("Could not stop monitoring %s", directory), e);
        }
    }

    private void processExistingItems(IngestAreaItemCreated callback) {
        try {
            try (var files = Files.list(directory)) {
                files.filter(Files::isDirectory).forEach(dir -> callback.onItemCreated(dir.toAbsolutePath()));
            }
        }
        catch (IOException e) {
            throw new IllegalStateException(String.format("Could not list %s", directory), e);
        }
    }

    private static class EventHandler extends FileAlterationListenerAdaptor {
        private final IngestAreaItemCreated callback;

        private EventHandler(IngestAreaItemCreated callback) {
            this.callback = callback;
        }

        @Override
        public void onStart(FileAlterationObserver observer) {
            // TODO implement the duplicate logic?
        }

        @Override
        public void onDirectoryCreate(File directory) {
            log.debug("Directory created: {}", directory);
            callback.onItemCreated(directory.toPath().toAbsolutePath());
        }
    }
}
