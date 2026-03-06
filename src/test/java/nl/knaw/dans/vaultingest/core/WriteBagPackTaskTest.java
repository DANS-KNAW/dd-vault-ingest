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

import nl.knaw.dans.vaultcatalog.api.DatasetDto;
import nl.knaw.dans.vaultingest.AbstractTestWithTestDir;
import nl.knaw.dans.vaultingest.client.BagValidator;
import nl.knaw.dans.vaultingest.client.VaultCatalogClient;
import nl.knaw.dans.vaultingest.core.bagpack.BagPackWriter;
import nl.knaw.dans.vaultingest.core.bagpack.BagPackWriterFactory;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.util.IdMinter;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class WriteBagPackTaskTest extends AbstractTestWithTestDir {

    private final Path outboxProcessed = testDir.resolve("outbox/processed");
    private final Path outboxFailed = testDir.resolve("outbox/failed");
    private final Path outboxRejected = testDir.resolve("outbox/rejected");
    private final Path dveOutbox = testDir.resolve("dve-outbox");

    @BeforeEach
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Files.createDirectories(outboxProcessed);
        Files.createDirectories(outboxFailed);
        Files.createDirectories(outboxRejected);
        Files.createDirectories(dveOutbox);
    }

    @Test
    void run_should_backup_bag_and_use_original_for_processing_on_success() throws Exception {
        var depositId = UUID.randomUUID();
        var depositDir = testDir.resolve(depositId.toString());
        var bagDir = depositDir.resolve("my-bag");
        Files.createDirectories(bagDir.resolve("data"));
        Files.writeString(bagDir.resolve("bagit.txt"), "BagIt-Version: 1.0\nTag-File-Character-Encoding: UTF-8\n");
        Files.writeString(bagDir.resolve("manifest-sha1.txt"), "");
        Files.writeString(depositDir.resolve("deposit.properties"), "creation.timestamp=2023-01-01T00:00:00Z\ndataverse.bag-id=" + depositId);

        var bagValidator = Mockito.mock(BagValidator.class);
        var vaultCatalogClient = Mockito.mock(VaultCatalogClient.class);
        var idMinter = Mockito.mock(IdMinter.class);
        var depositManager = Mockito.mock(DepositManager.class);
        var rdaBagWriterFactory = Mockito.mock(BagPackWriterFactory.class);
        var bagPackWriter = Mockito.mock(BagPackWriter.class);

        var deposit = Mockito.mock(Deposit.class);
        Mockito.when(deposit.getId()).thenReturn(depositId.toString());
        Mockito.when(deposit.getBagId()).thenReturn(depositId.toString());
        Mockito.when(deposit.getObjectVersion()).thenReturn(1);
        Mockito.when(deposit.isUpdate()).thenReturn(false);
        Mockito.when(deposit.getBagDir()).thenReturn(bagDir);
        var properties = Mockito.mock(nl.knaw.dans.vaultingest.core.deposit.DepositProperties.class);
        Mockito.when(deposit.getProperties()).thenReturn(properties);
        Mockito.when(properties.getBagId()).thenReturn(depositId.toString());

        Mockito.when(depositManager.loadDeposit(any(), anyString())).thenReturn(deposit);
        Mockito.when(rdaBagWriterFactory.createBagPackWriter(any())).thenReturn(bagPackWriter);
        Mockito.when(idMinter.mintUrnNbn()).thenReturn("urn:nbn:nl:ui:13-test");

        // Need to mock dataset for createSkeletonRecordInVaultCatalog
        var dataset = Mockito.mock(DatasetDto.class);
        Mockito.when(vaultCatalogClient.findDataset(anyString())).thenReturn(java.util.Optional.of(dataset));

        var task = new WriteBagPackTask(
            depositDir,
            outboxProcessed,
            outboxFailed,
            outboxRejected,
            "root",
            "user",
            rdaBagWriterFactory,
            vaultCatalogClient,
            bagValidator,
            idMinter,
            depositManager,
            dveOutbox
        );

        task.run();

        // Verify original depositDir (containing everything) was moved to outboxProcessed
        var movedDepositDir = outboxProcessed.resolve(depositId.toString());
        assertThat(movedDepositDir).exists();
        assertThat(depositDir).doesNotExist();

        // Verify that the backup bag (org-) was cleaned up from the moved directory
        assertThat(movedDepositDir.resolve("org-my-bag")).doesNotExist();
        assertThat(movedDepositDir.resolve("my-bag")).exists();

        // Verify that bagValidator was called with the original bag path
        Mockito.verify(bagValidator).validate(eq(depositId), eq(bagDir));

        // Verify BagPackWriter was called
        Mockito.verify(bagPackWriter).writeTo(any());
    }

    @Test
    void run_should_move_to_rejected_on_InvalidDepositException() throws Exception {
        var depositId = UUID.randomUUID();
        var depositDir = testDir.resolve(depositId.toString());
        Files.createDirectories(depositDir);

        var bagValidator = Mockito.mock(BagValidator.class);
        Mockito.doThrow(new nl.knaw.dans.vaultingest.client.InvalidDepositException("Invalid"))
            .when(bagValidator).validate(any(), any());

        var task = new WriteBagPackTask(
            depositDir,
            outboxProcessed,
            outboxFailed,
            outboxRejected,
            "root",
            "user",
            Mockito.mock(BagPackWriterFactory.class),
            Mockito.mock(VaultCatalogClient.class),
            bagValidator,
            Mockito.mock(IdMinter.class),
            Mockito.mock(DepositManager.class),
            dveOutbox
        );

        task.run();

        assertThat(outboxRejected.resolve(depositId.toString())).exists();
        assertThat(depositDir).doesNotExist();
    }
}
