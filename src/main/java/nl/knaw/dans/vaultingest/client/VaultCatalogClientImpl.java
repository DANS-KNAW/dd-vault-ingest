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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultcatalog.api.DatasetDto;
import nl.knaw.dans.vaultcatalog.api.VersionExportDto;
import nl.knaw.dans.vaultcatalog.client.ApiException;
import nl.knaw.dans.vaultcatalog.client.DefaultApi;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@AllArgsConstructor
public class VaultCatalogClientImpl implements VaultCatalogClient {
    private final DefaultApi vaultCatalogApi;

    @Override
    public DatasetDto createDatasetFor(String storageRoot, Deposit deposit) throws IOException {
        var versionExportDto = new VersionExportDto()
            .bagId(deposit.getBagId())
            .datasetNbn(deposit.getNbn())
            .ocflObjectVersionNumber(deposit.getObjectVersion())
            .createdTimestamp(deposit.getCreationTimestamp())
            .skeletonRecord(true);

        var datasetDto = new DatasetDto()
            .nbn(deposit.getNbn())
            .ocflStorageRoot(storageRoot)
            .swordToken(deposit.getSwordToken())
            .dataSupplier(deposit.getDataSupplier())
            .addVersionExportsItem(versionExportDto);

        try {
            vaultCatalogApi.addDataset(datasetDto.getNbn(), datasetDto);
            return datasetDto;
        }
        catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public VersionExportDto addDatasetVersionFor(Deposit deposit) throws IOException {
        var versionExportDto = new VersionExportDto()
            .datasetNbn(deposit.getNbn())
            .bagId(deposit.getBagId())
            .ocflObjectVersionNumber(deposit.getObjectVersion())
            .createdTimestamp(deposit.getCreationTimestamp())
            .skeletonRecord(true);

        try {
            vaultCatalogApi.setVersionExport(deposit.getNbn(), versionExportDto.getOcflObjectVersionNumber(), versionExportDto);
            return versionExportDto;
        }
        catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<DatasetDto> findDataset(String swordToken) throws IOException {
        try {
            return Optional.ofNullable(vaultCatalogApi.getDatasetBySwordToken(swordToken));
        }
        catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }
}
