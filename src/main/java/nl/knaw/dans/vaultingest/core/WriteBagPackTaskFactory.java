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
package nl.knaw.dans.vaultingest.core;

import lombok.AllArgsConstructor;
import nl.knaw.dans.lib.util.inbox.InboxTaskFactory;
import nl.knaw.dans.vaultingest.client.BagValidator;
import nl.knaw.dans.vaultingest.client.VaultCatalogClient;
import nl.knaw.dans.vaultingest.core.bagpack.BagPackWriterFactory;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.util.IdMinter;

import java.nio.file.Path;

@AllArgsConstructor
public class WriteBagPackTaskFactory implements InboxTaskFactory {
    private final String ocflStorageRoot;
    private final String dataSupplier;
    private final Path outboxProcessed;
    private final Path outboxFailed;
    private final Path outboxRejected;
    private final BagPackWriterFactory rdaBagWriterFactory;
    private final VaultCatalogClient vaultCatalogClient;
    private final BagValidator bagValidator;
    private final IdMinter idMinter;
    private final DepositManager depositManager;
    private final Path dveOutbox;

    public Runnable createInboxTask(Path path) {
        return new WriteBagPackTask(path,
            outboxProcessed, outboxFailed, outboxRejected, ocflStorageRoot, dataSupplier, rdaBagWriterFactory, vaultCatalogClient, bagValidator, idMinter, depositManager, dveOutbox);
    }
}
