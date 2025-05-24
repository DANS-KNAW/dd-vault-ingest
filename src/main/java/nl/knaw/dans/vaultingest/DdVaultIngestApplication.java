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
import nl.knaw.dans.lib.util.PingHealthCheck;
import nl.knaw.dans.lib.util.inbox.Inbox;
import nl.knaw.dans.vaultcatalog.client.ApiClient;
import nl.knaw.dans.vaultcatalog.client.DefaultApi;
import nl.knaw.dans.vaultingest.client.BagValidatorImpl;
import nl.knaw.dans.vaultingest.client.VaultCatalogClientImpl;
import nl.knaw.dans.vaultingest.config.DdVaultIngestFlowConfig;
import nl.knaw.dans.vaultingest.core.WriteBagPackTaskFactory;
import nl.knaw.dans.vaultingest.core.deposit.CsvLanguageResolver;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.deposit.FileCountryResolver;
import nl.knaw.dans.vaultingest.core.bagpack.BagPackWriterFactory;
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
        return "DD Vault Ingest";
    }

    @Override
    public void initialize(final Bootstrap<DdVaultIngestFlowConfig> bootstrap) {
    }

    @Override
    public void run(final DdVaultIngestFlowConfig configuration, final Environment environment) throws IOException {
        var languageResolver = new CsvLanguageResolver(
            configuration.getVaultIngest().getLanguages().getIso6391(),
            configuration.getVaultIngest().getLanguages().getIso6392()
        );

        var countryResolver = new FileCountryResolver(
            configuration.getVaultIngest().getSpatialCoverageCountryTermsPath()
        );
        var xmlReader = new XmlReader();
        var validateDansBagProxy = new ClientProxyBuilder<nl.knaw.dans.validatedansbag.invoker.ApiClient, nl.knaw.dans.validatedansbag.client.resources.DefaultApi>()
            .apiClient(new nl.knaw.dans.validatedansbag.invoker.ApiClient())
            .basePath(configuration.getValidateDansBag().getValidateUrl())
            .httpClient(configuration.getValidateDansBag().getHttpClient())
            .defaultApiCtor(nl.knaw.dans.validatedansbag.client.resources.DefaultApi::new)
            .build();
        var depositValidator = new BagValidatorImpl(validateDansBagProxy);
        var depositManager = new DepositManager(xmlReader);

        var rdaBagWriterFactory = new BagPackWriterFactory(
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

        var writeBagPackTaskFactory = new WriteBagPackTaskFactory(
            configuration.getVaultIngest().getOcflStorageRoot(),
            configuration.getVaultIngest().getDataSupplier(),
            configuration.getVaultIngest().getOutbox().getProcessed(),
            configuration.getVaultIngest().getOutbox().getFailed(),
            configuration.getVaultIngest().getOutbox().getRejected(),
            rdaBagWriterFactory,
            vaultCatalogClient,
            depositValidator,
            idMinter,
            depositManager,
            configuration.getVaultIngest().getBagPackOutputDir()
        );

        environment.lifecycle().manage(Inbox.builder()
            .inbox(configuration.getVaultIngest().getInbox().getPath())
            .interval(Math.toIntExact(configuration.getVaultIngest().getInbox().getPollingInterval().toMilliseconds()))
            .taskFactory(writeBagPackTaskFactory)
            .executorService(configuration.getVaultIngest().getTaskQueue().build(environment))

            .build());

        environment.healthChecks().register(
            "DansBagValidator",
            new PingHealthCheck("DansBagValidator", validateDansBagProxy.getApiClient().getHttpClient(), configuration.getValidateDansBag().getPingUrl()));

    }
}
