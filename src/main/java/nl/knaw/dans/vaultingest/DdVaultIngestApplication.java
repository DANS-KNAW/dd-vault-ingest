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

package nl.knaw.dans.vaultingest;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.ClientProxyBuilder;
import nl.knaw.dans.lib.util.ManagedExecutorService;
import nl.knaw.dans.lib.util.PingHealthCheck;
import nl.knaw.dans.vaultcatalog.client.ApiClient;
import nl.knaw.dans.vaultcatalog.client.DefaultApi;
import nl.knaw.dans.vaultingest.client.DepositBagValidator;
import nl.knaw.dans.vaultingest.client.MigrationBagValidator;
import nl.knaw.dans.vaultingest.client.VaultCatalogClientImpl;
import nl.knaw.dans.vaultingest.config.DdVaultIngestFlowConfig;
import nl.knaw.dans.vaultingest.core.ConvertToRdaBagTaskFactory;
import nl.knaw.dans.vaultingest.core.deposit.CsvLanguageResolver;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.deposit.DepositOutbox;
import nl.knaw.dans.vaultingest.core.deposit.FileCountryResolver;
import nl.knaw.dans.vaultingest.core.deposit.MigrationDepositManager;
import nl.knaw.dans.vaultingest.core.inbox.AutoIngestArea;
import nl.knaw.dans.vaultingest.core.inbox.IngestAreaDirectoryWatcher;
import nl.knaw.dans.vaultingest.core.inbox.MigrationIngestArea;
import nl.knaw.dans.vaultingest.core.rdabag.DefaultRdaBagWriterFactory;
import nl.knaw.dans.vaultingest.core.util.IdMinter;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;

import java.io.IOException;

@Slf4j
public class DdVaultIngestApplication extends Application<DdVaultIngestFlowConfig> {

    public static void main(final String[] args) throws Exception {
        new DdVaultIngestApplication().run(args);
    }

    @Override
    public String getName() {
        return "DD Vault Ingest Flow";
    }

    @Override
    public void initialize(final Bootstrap<DdVaultIngestFlowConfig> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final DdVaultIngestFlowConfig configuration, final Environment environment) throws IOException {
        var languageResolver = new CsvLanguageResolver(
            configuration.getIngestFlow().getLanguages().getIso6391(),
            configuration.getIngestFlow().getLanguages().getIso6392()
        );

        var countryResolver = new FileCountryResolver(
            configuration.getIngestFlow().getSpatialCoverageCountryTermsPath()
        );
        var xmlReader = new XmlReader();
        var validateDansBagProxy = new ClientProxyBuilder<nl.knaw.dans.validatedansbag.invoker.ApiClient, nl.knaw.dans.validatedansbag.client.resources.DefaultApi>()
            .apiClient(new nl.knaw.dans.validatedansbag.invoker.ApiClient())
            .basePath(configuration.getValidateDansBag().getValidateUrl())
            .httpClient(configuration.getValidateDansBag().getHttpClient())
            .defaultApiCtor(nl.knaw.dans.validatedansbag.client.resources.DefaultApi::new)
            .build();
        var depositValidator = new DepositBagValidator(validateDansBagProxy);
        var depositManager = new DepositManager(xmlReader);

        var rdaBagWriterFactory = new DefaultRdaBagWriterFactory(
            environment.getObjectMapper(),
            languageResolver,
            countryResolver
        );

        var vaultCatalogProxy = new ClientProxyBuilder<ApiClient, DefaultApi>()
            .apiClient(new ApiClient())
            .basePath(configuration.getVaultCatalog().getUrl())
            .httpClient(configuration.getVaultCatalog().getHttpClient())
            .defaultApiCtor(DefaultApi::new)
            .build();
        var vaultCatalogClient = new VaultCatalogClientImpl(vaultCatalogProxy);
        var idMinter = new IdMinter();

        var autoIngestConvertToRdaBagTaskFactory = new ConvertToRdaBagTaskFactory(
            configuration.getIngestFlow().getStorageRoot(),
            configuration.getIngestFlow().getAutoIngest().getDataSuppliers(),
            rdaBagWriterFactory,
            vaultCatalogClient,
            depositValidator,
            idMinter,
            depositManager,
            configuration.getIngestFlow().getRdaBagOutputDir()
        );

        var taskQueue = configuration.getIngestFlow().getTaskQueue().build(environment);

        environment.lifecycle().manage(new ManagedExecutorService(taskQueue));

        var ingestAreaDirectoryWatcher = new IngestAreaDirectoryWatcher(
            500,
            configuration.getIngestFlow().getAutoIngest().getInbox()
        );

        environment.lifecycle().manage(new AutoIngestArea(
            taskQueue,
            ingestAreaDirectoryWatcher,
            autoIngestConvertToRdaBagTaskFactory,
            new DepositOutbox(configuration.getIngestFlow().getAutoIngest().getOutbox())));

        var migrationDepositValidator = new MigrationBagValidator(validateDansBagProxy);
        var migrationDepositManager = new MigrationDepositManager(xmlReader);

        var migrationIngestConvertToRdaBagTaskFactory = new ConvertToRdaBagTaskFactory(
            configuration.getIngestFlow().getStorageRoot(),
            configuration.getIngestFlow().getMigration().getDataSuppliers(),
            rdaBagWriterFactory,
            vaultCatalogClient,
            migrationDepositValidator,
            idMinter,
            migrationDepositManager,
            configuration.getIngestFlow().getRdaBagOutputDir()
        );

        // TODO: implement API to call this.
        new MigrationIngestArea(
            taskQueue,
            migrationIngestConvertToRdaBagTaskFactory,
            configuration.getIngestFlow().getMigration().getInbox(),
            new DepositOutbox(configuration.getIngestFlow().getMigration().getOutbox())
        );

        environment.healthChecks().register(
            "DansBagValidator",
            new PingHealthCheck("DansBagValidator", validateDansBagProxy.getApiClient().getHttpClient(), configuration.getValidateDansBag().getPingUrl()));

    }
}
