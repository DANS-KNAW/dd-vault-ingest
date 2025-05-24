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
package nl.knaw.dans.vaultingest.config;

import lombok.Getter;
import nl.knaw.dans.lib.util.ExecutorServiceFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;

@Getter
public class VaultIngestConfig {
    @NotNull
    private String ocflStorageRoot;

    @NotNull
    private Path bagPackOutputDir;

    @NotNull
    private String dataSupplier;

    @NotNull
    @Valid
    private InboxConfig inbox;

    @NotNull
    @Valid
    private OutboxWithRejectedConfig outbox;

    @NotNull
    @Valid
    private LanguageConfig languages;

    @NotNull
    @Valid
    private Path spatialCoverageCountryTermsPath;

    @NotNull
    @Valid
    private ExecutorServiceFactory taskQueue;
}
