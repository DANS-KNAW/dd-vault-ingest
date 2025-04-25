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
import nl.knaw.dans.vaultingest.core.deposit.Outbox;
import nl.knaw.dans.vaultingest.core.rdabag.DefaultRdaBagWriterFactory;
import nl.knaw.dans.vaultingest.core.util.IdMinter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@RequiredArgsConstructor
public class ConvertToRdaBagTask implements Runnable {
    @NonNull
    private final Path path;

    @NonNull
    private final Outbox outbox;

    @NonNull
    private final Map<String, String> dataSupplierMap;
    @NonNull
    private final DefaultRdaBagWriterFactory rdaBagWriterFactory;
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
            log.info("[{}] START processing deposit", getDepositId(path));
            var bagDir = getBagDir(path);

            bagValidator.validate(getDepositId(path), bagDir);

            log.debug("[{}] Loading deposit info", getDepositId(path));
            deposit = depositManager.loadDeposit(path, dataSupplierMap);
            processDeposit();

            depositManager.saveDepositProperties(deposit);
            log.debug("[{}] Saved deposit properties", getDepositId(path));
            outbox.moveDeposit(deposit);
            log.info("[{}] Moved deposit to outbox", getDepositId(path));
        }
        catch (InvalidDepositException e) {
            log.warn("[{}] REJECTED deposit: {}", getDepositId(path), e.getMessage());
            handleFailedDeposit(path, outbox, Deposit.State.REJECTED, e);
        }
        catch (Throwable e) {
            log.error("[{}] FAILED deposit: {}", getDepositId(path), e.getMessage());
            handleFailedDeposit(path, outbox, Deposit.State.FAILED, e);
        }
        log.info("[{}] END processing deposit", getDepositId(path));
    }

    private UUID getDepositId(Path path) {
        if (depositId == null) {
            depositId = UUID.fromString(path.getFileName().toString());
        }
        return depositId;
    }

    private void processDeposit() throws InvalidDepositException, IOException {
        createSkeletonRecordInVaultCatalog();
        convertToRdaBag();
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
            vaultCatalogClient.createDatasetFor(deposit);
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

    private void convertToRdaBag() throws IOException {
        try {
            rdaBagWriterFactory.createRdaBagWriter(deposit).write(dveOutbox.resolve(outputFilename(deposit.getBagId(), deposit.getObjectVersion())));
            deposit.setState(Deposit.State.ACCEPTED, "Deposit accepted");
        }
        catch (Exception e) {
            throw new IllegalStateException("Error writing bag: " + e.getMessage(), e);
        }
    }

    private String outputFilename(String bagId, Integer objectVersion) {
        Objects.requireNonNull(bagId);
        Objects.requireNonNull(objectVersion);

        // strip anything before all colons (if present), and also the colon itself
        bagId = bagId.toLowerCase().replaceAll(".*:", "");

        return String.format("vaas-%s-v%s.zip", bagId, objectVersion);
    }

    private void handleFailedDeposit(Path path, Outbox outbox, Deposit.State state, Throwable error) {
        try {
            depositManager.updateDepositState(path, state, error.getMessage());
            log.info("[{}] Moving deposit {} to outbox: {}", getDepositId(path), path, state);
            outbox.move(path, state);
        }
        catch (Throwable e) {
            log.error("[{}] Failed to update deposit state and move deposit to outbox", getDepositId(path), e);

            try {
                outbox.move(path, Deposit.State.FAILED);
            }
            catch (IOException ioException) {
                log.error("[{}] Failed to move deposit to outbox, leaving it in the inbox.", getDepositId(path), ioException);
            }
        }
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
