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
package nl.knaw.dans.vaultingest.core.bagpack;

import nl.knaw.dans.bagit.hash.SupportedAlgorithm;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.ZipUtil;
import nl.knaw.dans.vaultingest.core.datacite.DataciteConverter;
import nl.knaw.dans.vaultingest.core.datacite.DataciteSerializer;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.oaiore.OaiOreConverter;
import nl.knaw.dans.vaultingest.core.oaiore.OaiOreSerializer;
import nl.knaw.dans.vaultingest.core.pidmapping.PidMappingConverter;
import nl.knaw.dans.vaultingest.core.pidmapping.PidMappingSerializer;
import nl.knaw.dans.vaultingest.core.util.MultiDigestInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Enriching a DANS Bag with metadata so that it becomes an RDA "BagPack".
 */
@Slf4j
@RequiredArgsConstructor
public class BagPackWriter {
    @NonNull
    private final Deposit deposit;

    @NonNull
    private final DataciteSerializer dataciteSerializer;

    @NonNull
    private final PidMappingSerializer pidMappingSerializer;

    @NonNull
    private final OaiOreSerializer oaiOreSerializer;

    @NonNull
    private final DataciteConverter dataciteConverter;

    @NonNull
    private final PidMappingConverter pidMappingConverter;

    @NonNull
    private final OaiOreConverter oaiOreConverter;

    private final Map<Path, Map<SupportedAlgorithm, String>> changedChecksums = new HashMap<>();
    private Set<SupportedAlgorithm> tagManifestAlgorithms;

    public void writeTo(Path bagPack) throws IOException {
        this.tagManifestAlgorithms = deposit.getBag().getTagManifestAlgorithms();

        log.debug("[{}] Adding metadata/datacite.xml", deposit.getId());
        var resource = dataciteConverter.convert(deposit);
        var dataciteXml = dataciteSerializer.serialize(resource);
        checksummedWriteToOutput(Path.of("metadata/datacite.xml"), dataciteXml);

        log.debug("[{}] Adding metadata/oai-ore[.rdf|.jsonld]", deposit.getId());
        var oaiOre = oaiOreConverter.convert(deposit);
        var rdf = oaiOreSerializer.serializeAsRdf(oaiOre);
        var jsonld = oaiOreSerializer.serializeAsJsonLd(oaiOre);
        checksummedWriteToOutput(Path.of("metadata/oai-ore.rdf"), rdf);
        checksummedWriteToOutput(Path.of("metadata/oai-ore.jsonld"), jsonld);

        log.debug("[{}] Adding metadata/pid-mapping.txt", deposit.getId());
        var pidMappings = pidMappingConverter.convert(deposit);
        var pidMappingsSerialized = pidMappingSerializer.serialize(pidMappings);
        checksummedWriteToOutput(Path.of("metadata/pid-mapping.txt"), pidMappingsSerialized);

        // bag-info.txt does not need changing, as no payload files are added or removed

        // must be last, because all other files must have been written
        log.debug("[{}] Modifying tagmanifest-*.txt files", deposit.getId());
        modifyTagManifests(); // Add checksums for new metadata files

        log.debug("[{}] Creating ZIP file", deposit.getId());
        var tempZipFile = bagPack.resolveSibling(bagPack.getFileName() + ".tmp");
        log.debug("[{}] Zipping directory {} to {}", deposit.getId(), deposit.getBagDir(), tempZipFile);
        ZipUtil.zipDirectory(deposit.getBagDir(), tempZipFile, true);
        log.debug("[{}] Moving {} to {}", deposit.getId(), tempZipFile, bagPack);
        Files.move(tempZipFile, bagPack);
    }

    private void modifyTagManifests() throws IOException {
        for (var algorithm : tagManifestAlgorithms) {
            var tagManifest = deposit.getBag().getTagManifest(algorithm);
            var fileToChecksum = tagManifest.getFileToChecksumMap();

            for (var entry : changedChecksums.entrySet()) {
                var path = deposit.getBagDir().resolve(entry.getKey());
                var checksums = entry.getValue();
                var checksum = checksums.get(algorithm);
                if (checksum != null) {
                    fileToChecksum.put(path, checksum);
                }
            }
            tagManifest.setFileToChecksumMap(fileToChecksum);
        }
        deposit.getBag().writeTagManifests();
    }

    private void checksummedWriteToOutput(Path path, String content) throws IOException {
        try (var input = new MultiDigestInputStream(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)), tagManifestAlgorithms)) {
            try (var outputStream = FileUtils.openOutputStream(deposit.getBagDir().resolve(path).toFile())) {
                IOUtils.copy(input, outputStream);
                var result = input.getChecksums();
                log.debug("[{}] Checksums for {}: {}", deposit.getId(), path, result);
                changedChecksums.put(path, result);
            }
        }
        catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Algorithm not supported", e);
        }
    }
}
