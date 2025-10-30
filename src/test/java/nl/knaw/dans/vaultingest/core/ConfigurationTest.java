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

import com.fasterxml.jackson.databind.DeserializationFeature;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import nl.knaw.dans.vaultingest.config.DdVaultIngestConfig;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class ConfigurationTest {

    private final Path testDir = new File("target/test/" + getClass().getSimpleName()).toPath();
    private YamlConfigurationFactory<DdVaultIngestConfig> factory;

    @BeforeEach
    void setUp() throws IOException {
        FileUtils.deleteQuietly(testDir.toFile());
        Files.createDirectories(testDir);
        final var mapper = Jackson.newObjectMapper().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        factory = new YamlConfigurationFactory<>(DdVaultIngestConfig.class, Validators.newValidator(), mapper, "dw");
    }

    @Test
    public void assembly_dist_cfg_does_not_throw() throws IOException {
        File cfgFile = testDir.resolve("config.yml").toFile();
        Files.copy(Paths.get("src/main/assembly/dist/cfg/config.yml"), cfgFile.toPath());
        assertDoesNotThrow(() -> factory.build(FileInputStream::new, cfgFile.toString()));
    }

    @Test
    public void debug_etc_does_not_throw() throws IOException {
        File cfgFile = testDir.resolve("config.yml").toFile();
        Files.copy(Path.of("src/test/resources/debug-etc/config.yml"), cfgFile.toPath());
        assertDoesNotThrow(() -> factory.build(FileInputStream::new, cfgFile.toString()));
    }
}
