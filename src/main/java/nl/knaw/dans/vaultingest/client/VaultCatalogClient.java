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
package nl.knaw.dans.vaultingest.client;

import nl.knaw.dans.vaultcatalog.api.DatasetDto;
import nl.knaw.dans.vaultcatalog.api.VersionExportDto;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;

import java.io.IOException;
import java.util.Optional;

public interface VaultCatalogClient {

    DatasetDto createDatasetFor(String storageroot, Deposit deposit) throws IOException;

    VersionExportDto addDatasetVersionFor(Deposit deposit) throws IOException;

    Optional<DatasetDto> findDataset(String swordToken) throws IOException;
}
