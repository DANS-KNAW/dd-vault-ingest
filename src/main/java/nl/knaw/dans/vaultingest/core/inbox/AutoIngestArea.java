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

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.DepositToBagProcess;
import nl.knaw.dans.vaultingest.core.deposit.Outbox;

import java.io.IOException;
import java.util.concurrent.Executor;

@Slf4j
public class AutoIngestArea {
    private final Executor executor;
    private final IngestAreaWatcher ingestAreaWatcher;
    private final DepositToBagProcess depositToBagProcess;
    private final Outbox outbox;

    public AutoIngestArea(
        Executor executor,
        IngestAreaWatcher ingestAreaWatcher,
        DepositToBagProcess depositToBagProcess,
        Outbox outbox) {
        this.executor = executor;
        this.ingestAreaWatcher = ingestAreaWatcher;
        this.depositToBagProcess = depositToBagProcess;
        this.outbox = outbox;
    }

    public void start() {
        try {
            outbox.init(true);

            ingestAreaWatcher.start((path) -> {
                log.info("New item in inbox; path = {}", path);

                executor.execute(() -> depositToBagProcess.process(path, outbox));
            });
        }
        catch (IOException e) {
            log.error("Error while starting the ingest area watcher for outbox {}", outbox, e);
            throw new IllegalStateException("Error while starting the ingest area watcher for outbox " + outbox, e);
        }
    }

}
