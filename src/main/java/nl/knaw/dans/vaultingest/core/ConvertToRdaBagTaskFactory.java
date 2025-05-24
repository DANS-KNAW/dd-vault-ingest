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
import nl.knaw.dans.vaultingest.client.BagValidator;
import nl.knaw.dans.vaultingest.client.VaultCatalogClient;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.deposit.Outbox;
import nl.knaw.dans.vaultingest.core.rdabag.DefaultRdaBagWriterFactory;
import nl.knaw.dans.vaultingest.core.util.IdMinter;

import java.nio.file.Path;
import java.util.Map;

@AllArgsConstructor
public class ConvertToRdaBagTaskFactory {
    private final String storageRoot;
    private final Map<String, String> dataSupplierMap;
    private final DefaultRdaBagWriterFactory rdaBagWriterFactory;
    private final VaultCatalogClient vaultCatalogClient;
    private final BagValidator bagValidator;
    private final IdMinter idMinter;
    private final DepositManager depositManager;
    private final Path dveOutbox;

    public ConvertToRdaBagTask create(Path path, Outbox outbox) {
        return new ConvertToRdaBagTask(path, outbox, storageRoot, dataSupplierMap, rdaBagWriterFactory, vaultCatalogClient, bagValidator, idMinter, depositManager, dveOutbox);
    }
}
