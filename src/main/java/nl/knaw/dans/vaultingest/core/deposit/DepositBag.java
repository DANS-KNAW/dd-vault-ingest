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
import nl.knaw.dans.bagit.domain.Manifest;
import nl.knaw.dans.bagit.domain.Version;
import nl.knaw.dans.bagit.hash.SupportedAlgorithm;
import nl.knaw.dans.bagit.writer.ManifestWriter;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.bagit.writer.MetadataWriter;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public class DepositBag {
    private final Bag bag;

    public Collection<Path> getMetadataFiles() throws IOException {
        var path = this.bag.getRootDir();
        try (var list = Files.list(path.resolve("metadata"))) {
            return list
                .map(path::relativize)
                .collect(Collectors.toList());
        }
    }

    public Set<SupportedAlgorithm> getPayloadManifestAlgorithms() {
        return bag.getPayLoadManifests().stream()
            .map(Manifest::getAlgorithm)
            .collect(Collectors.toSet());
    }

    public InputStream inputStreamForBagFile(Path path) {
        try {
            var target = bag.getRootDir().resolve(path);
            return new BufferedInputStream(Files.newInputStream(target));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Manifest getTagManifest(SupportedAlgorithm algorithm) {
        return bag.getTagManifests().stream()
            .filter(manifest -> manifest.getAlgorithm().equals(algorithm))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("No tag manifest found for algorithm " + algorithm));
    }

    public Set<SupportedAlgorithm> getTagManifestAlgorithms() {
        return bag.getTagManifests().stream()
            .map(Manifest::getAlgorithm)
            .collect(Collectors.toSet());
    }

    public List<String> getBagInfoValue(String key) {
        var value = bag.getMetadata().get(key);
        return value != null ? value : List.of();
    }

    public Path getBagDir() {
        return bag.getRootDir();
    }

    public void writeTagManifests() throws IOException {
        ManifestWriter.writeTagManifests(bag.getTagManifests(), bag.getRootDir(), bag.getRootDir(), StandardCharsets.UTF_8);
    }

    public void putBagInfoValue(String key, String value) throws IOException {
        bag.getMetadata().add(key, value);
        MetadataWriter.writeBagMetadata(bag.getMetadata(), Version.LATEST_BAGIT_VERSION(), bag.getRootDir(), StandardCharsets.UTF_8);

    }

    public List<String> getBagInfoValues(String key) {
        return bag.getMetadata().get(key);
    }
}
