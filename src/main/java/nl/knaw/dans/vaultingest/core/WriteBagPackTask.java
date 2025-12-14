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

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultcatalog.api.DatasetDto;
import nl.knaw.dans.vaultcatalog.api.VersionExportDto;
import nl.knaw.dans.vaultingest.client.BagValidator;
import nl.knaw.dans.vaultingest.client.InvalidDepositException;
import nl.knaw.dans.vaultingest.client.VaultCatalogClient;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.bagpack.BagPackWriterFactory;
import nl.knaw.dans.vaultingest.core.util.IdMinter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class WriteBagPackTask implements Runnable {
    @NonNull
    private final Path depositDir;

    @NonNull
    private final Path outboxProcessed;

    @NonNull
    private final Path outboxFailed;

    @NonNull
    private final Path outboxRejected;

    @NonNull
    private final String storageRoot;
    @NonNull
    private final String dataSupplier;
    @NonNull
    private final BagPackWriterFactory rdaBagWriterFactory;
    @NonNull
    private final VaultCatalogClient vaultCatalogClient;
    @NonNull
    private final BagValidator bagValidator;
    @NonNull
    private final IdMinter idMinter;
    @NonNull
    private final DepositManager depositManager;
    @NonNull
    private final Path dveOutbox;

    private Deposit deposit;

    private UUID depositId;

    public void run() {
        try {
            log.info("[{}] START processing deposit", getDepositId(depositDir));
            var bagDir = getBagDir(depositDir);

            bagValidator.validate(getDepositId(depositDir), bagDir);

            log.debug("[{}] Loading deposit info", getDepositId(depositDir));
            deposit = depositManager.loadDeposit(depositDir, dataSupplier);
            processDeposit();

            depositManager.saveDepositProperties(deposit);
            log.debug("[{}] Saved deposit properties", getDepositId(depositDir));
            depositManager.updateDepositState(depositDir, Deposit.State.ACCEPTED, "Deposit accepted");
            Files.move(depositDir, outboxProcessed.resolve(depositDir.getFileName()));
            log.info("[{}] Moved deposit to outbox", getDepositId(depositDir));
        }
        catch (InvalidDepositException e) {
            log.warn("[{}] REJECTED deposit: {}", getDepositId(depositDir), e.getMessage());
            try {
                depositManager.updateDepositState(depositDir, Deposit.State.REJECTED, e.getMessage());
                Files.move(depositDir, outboxRejected.resolve(depositDir.getFileName()));
            }
            catch (IOException ioException) {
                log.error("[{}] Failed to move deposit to outbox-rejected", getDepositId(depositDir), ioException);
            }
        }
        catch (Exception e) {
            log.error("[{}] FAILED deposit: {}", getDepositId(depositDir), e.getMessage(), e);
            try {
                depositManager.updateDepositState(depositDir, Deposit.State.FAILED, e.getMessage());
                Files.move(depositDir, outboxFailed.resolve(depositDir.getFileName()));
            }
            catch (IOException ioException) {
                log.error("[{}] Failed to handle failed deposit", getDepositId(depositDir), ioException);
            }
        }
        log.info("[{}] END processing deposit", getDepositId(depositDir));
    }

    private UUID getDepositId(Path path) {
        if (depositId == null) {
            depositId = UUID.fromString(path.getFileName().toString());
        }
        return depositId;
    }

    private void processDeposit() throws InvalidDepositException, IOException {
        createSkeletonRecordInVaultCatalog();
        convertToBagPack();
    }

    private void createSkeletonRecordInVaultCatalog() throws IOException, InvalidDepositException {
        if (deposit.isUpdate()) {
            log.debug("[{}] Deposit is an update", deposit.getId());
            var dataset = vaultCatalogClient.findDataset(convertToSwordToken(deposit.getIsVersionOf()))
                .orElseThrow(() -> new InvalidDepositException(String.format("Dataset with sword token %s not found in vault catalog", deposit.getSwordToken())));
            checkDataSupplier(dataset);
            checkCreatedTimestamp(dataset);
            deposit.setNbn(dataset.getNbn());
            deposit.setObjectVersion(getNextOcflVersionNumber(dataset));
            vaultCatalogClient.addDatasetVersionFor(deposit);
        }
        else {
            deposit.setNbn(idMinter.mintUrnNbn());
            vaultCatalogClient.createDatasetFor(storageRoot, deposit);
        }
    }

    private String convertToSwordToken(String isVersionOf) {
        if (isVersionOf.startsWith("sword:")) {
            return isVersionOf;
        }
        else if (isVersionOf.startsWith("urn:uuid:")) {
            return "sword:" + isVersionOf.substring("urn:uuid:".length());
        }
        else {
            throw new IllegalArgumentException("Is-Version-Of value must start with 'sword:' or 'urn:uuid:'");
        }
    }

    private void checkDataSupplier(DatasetDto dataset) throws InvalidDepositException {
        if (!StringUtils.equals(deposit.getDataSupplier(), dataset.getDataSupplier())) {
            throw new InvalidDepositException(String.format(
                "Data supplier in deposit '%s' does not match the data supplier '%s' in the dataset to be updated, as registered in the Vault Catalog", deposit.getDataSupplier(),
                dataset.getDataSupplier()));
        }
    }

    private void checkCreatedTimestamp(DatasetDto dataset) throws InvalidDepositException {
        var latestCreatedTimestamp = dataset.getVersionExports().stream()
            .map(VersionExportDto::getCreatedTimestamp)
            .sorted(Comparator.reverseOrder()).findFirst().orElseThrow(() -> new IllegalStateException("No created timestamp found"));
        if (deposit.getCreationTimestamp().isBefore(latestCreatedTimestamp)) {
            throw new InvalidDepositException(String.format("Deposit creation timestamp %s is before the latest created timestamp %s in the dataset to be updated", deposit.getCreationTimestamp(),
                latestCreatedTimestamp));
        }
    }

    private int getNextOcflVersionNumber(DatasetDto dataset) {
        var numbers = dataset.getVersionExports().stream()
            .map(VersionExportDto::getOcflObjectVersionNumber)
            .sorted().toList();
        for (int i = 0; i < numbers.size(); i++) {
            if (numbers.get(i) != i + 1) {
                throw new IllegalStateException("The OCFL Object version sequence does not start with 1 or is not consecutive for dataset " + dataset.getNbn());
            }
        }
        return numbers.size() + 1;
    }

    private void convertToBagPack() {
        try {
            rdaBagWriterFactory.createBagPackWriter(deposit).writeTo(dveOutbox.resolve(outputFilename(deposit.getBagId(), deposit.getObjectVersion())));
            deposit.setState(Deposit.State.ACCEPTED, "Deposit accepted");
        }
        catch (Exception e) {
            throw new IllegalStateException("Error writing bag: " + e.getMessage(), e);
        }
    }

    private String outputFilename(@NonNull String bagId, @NonNull Integer objectVersion) {
        // strip anything before all colons (if present), and also the colon itself
        bagId = bagId.toLowerCase().replaceAll(".*:", "");
        long creationTime = System.currentTimeMillis();
        return String.format("vaas-%s_%d_v%s.zip", bagId, creationTime, objectVersion);
    }

    private Path getBagDir(Path path) throws InvalidDepositException {
        try (var list = Files.list(path)) {
            return list.filter(Files::isDirectory)
                .findFirst()
                .orElse(null);
        }
        catch (IOException e) {
            return null;
        }
    }
}
