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
package nl.knaw.dans.vaultingest.core.deposit;

import nl.knaw.dans.bagit.domain.Bag;
import nl.knaw.dans.bagit.hash.SupportedAlgorithm;
import nl.knaw.dans.bagit.reader.BagReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class DepositManager {
    private final XmlReader xmlReader;

    public Deposit loadDeposit(Path path, String dataSupplier) {
        var depositId = path.getFileName().toString();

        try {
            var bagDir = getBagDir(path);

            log.debug("[{}] Reading bag from path {}", depositId, bagDir);
            var bag = new BagReader().read(bagDir);

            log.debug("[{}] Reading metadata/dataset.xml", depositId);
            var ddm = readXmlFile(bagDir.resolve(Path.of("metadata", "dataset.xml")));

            log.debug("[{}] Reading metadata/files.xml", depositId);
            var filesXml = readXmlFile(bagDir.resolve(Path.of("metadata", "files.xml")));

            log.debug("[{}] Generating original file paths if file exists", depositId);
            var originalFilePaths = getOriginalFilepaths(bagDir);

            log.debug("[{}] Reading deposit.properties", depositId);
            var depositProperties = getDepositProperties(path);

            log.debug("[{}] Generating payload file list", depositId);
            var payloadFiles = getPayloadFiles(bagDir, bag, ddm, filesXml, originalFilePaths);

            var builder = Deposit.builder()
                .id(path.getFileName().toString())
                .path(path)
                .ddm(ddm)
                .bag(new DepositBag(bag))
                .filesXml(filesXml)
                .payloadFiles(payloadFiles)
                .properties(depositProperties)
                .dataSupplier(dataSupplier);

            var deposit = builder.build();
            return customizeDeposit(deposit, depositProperties);
        }
        catch (Exception e) {
            log.error("[{}] Error loading deposit from disk: path={}", depositId, path, e);
            throw new RuntimeException(e);
        }
    }

    Deposit customizeDeposit(Deposit deposit, DepositProperties depositProperties) {
        return deposit;
    }

    public void saveDepositProperties(Deposit deposit) {
        var properties = deposit.getProperties();

        try {
            properties.save();
        }
        catch (ConfigurationException e) {
            log.error("Error saving deposit properties: depositId={}", deposit.getId(), e);
            throw new RuntimeException(e);
        }
    }

    public void updateDepositState(Path path, Deposit.State state, String message) {
        try {
            var depositProperties = getDepositProperties(path);
            depositProperties.setStateLabel(state.name());
            depositProperties.setStateDescription(message);

            depositProperties.save();
        }
        catch (ConfigurationException e) {
            log.error("Error updating deposit state: path={}, state={}, message={}", path, state, message, e);
            throw new RuntimeException(e);
        }
    }

    private Path getBagDir(Path path) throws IOException {
        try (var list = Files.list(path)) {
            return list.filter(Files::isDirectory)
                .findFirst()
                .orElseThrow();
        }
    }

    private Document readXmlFile(Path path) throws IOException, SAXException, ParserConfigurationException {
        return xmlReader.readXmlFile(path);
    }

    private DepositProperties getDepositProperties(Path path) throws ConfigurationException {
        var propertiesFile = path.resolve("deposit.properties");
        var params = new Parameters();
        var paramConfig = params.properties()
            .setFileName(propertiesFile.toString());

        var builder = new FileBasedConfigurationBuilder<FileBasedConfiguration>
            (PropertiesConfiguration.class, null, true)
            .configure(paramConfig);

        return new DepositProperties(builder);
    }

    private OriginalFilepaths getOriginalFilepaths(Path bagDir) throws IOException {
        var originalFilepathsFile = bagDir.resolve("original-filepaths.txt");
        var result = new OriginalFilepaths();

        if (Files.exists(originalFilepathsFile)) {
            try (var lines = Files.lines(originalFilepathsFile)) {
                lines.filter(StringUtils::isNotBlank)
                    .map(line -> line.split("\\s+", 2))
                    .forEach(line -> result.addMapping(
                        Path.of(line[1]), Path.of(line[0]))
                    );
            }
        }

        return result;
    }

    private Map<Path, Map<SupportedAlgorithm, String>> getPrecomputedChecksums(Path bagDir, Bag bag) {
        var manifests = new HashMap<Path, Map<SupportedAlgorithm, String>>();

        for (var manifest : bag.getPayLoadManifests()) {
            var alg = manifest.getAlgorithm();

            for (var entry : manifest.getFileToChecksumMap().entrySet()) {
                var relativePath = bagDir.relativize(entry.getKey());
                var checksum = entry.getValue();

                manifests.computeIfAbsent(relativePath, k -> new HashMap<>())
                    .put(alg, checksum);
            }
        }

        return manifests;
    }

    private List<PayloadFile> getPayloadFiles(Path bagDir, Bag bag, Document ddm, Document filesXml, OriginalFilepaths originalFilepaths) {
        var manifests = getPrecomputedChecksums(bagDir, bag);

        return XPathEvaluator.nodes(filesXml, "/files:files/files:file")
            .map(node -> {
                var filePath = node.getAttributes().getNamedItem("filepath").getTextContent();
                var physicalPath = bagDir.resolve(originalFilepaths.getPhysicalPath(Path.of(filePath)));
                var checksums = manifests.get(bagDir.relativize(physicalPath));

                return PayloadFile.builder()
                    .id("urn:uuid:" + UUID.randomUUID())
                    .physicalPath(physicalPath)
                    .filesXmlNode(node)
                    .ddmNode(ddm)
                    .checksums(checksums)
                    .build();
            })
            .collect(Collectors.toList());
    }

}
