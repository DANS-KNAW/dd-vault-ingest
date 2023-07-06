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

import io.dropwizard.Application;
import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.client.DepositValidator;
import nl.knaw.dans.vaultingest.client.OcflObjectVersionApi;
import nl.knaw.dans.vaultingest.client.VaultCatalogClient;
import nl.knaw.dans.vaultingest.core.DepositToBagProcess;
import nl.knaw.dans.vaultingest.core.IdMinter;
import nl.knaw.dans.vaultingest.core.deposit.CsvLanguageResolver;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.deposit.DepositOutbox;
import nl.knaw.dans.vaultingest.core.deposit.FileCountryResolver;
import nl.knaw.dans.vaultingest.core.inbox.AutoIngestArea;
import nl.knaw.dans.vaultingest.core.inbox.IngestAreaDirectoryWatcher;
import nl.knaw.dans.vaultingest.core.rdabag.DefaultRdaBagWriterFactory;
import nl.knaw.dans.vaultingest.core.rdabag.output.ZipBagOutputWriterFactory;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import nl.knaw.dans.vaultingest.health.DansBagValidatorHealthCheck;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public class DdVaultIngestFlowApplication extends Application<DdVaultIngestFlowConfiguration> {

    public static void main(final String[] args) throws Exception {
        new DdVaultIngestFlowApplication().run(args);
    }

    @Override
    public String getName() {
        return "Dd Vault Ingest Flow";
    }

    @Override
    public void initialize(final Bootstrap<DdVaultIngestFlowConfiguration> bootstrap) {
        // TODO: application initialization
    }

    @Override
    public void run(final DdVaultIngestFlowConfiguration configuration, final Environment environment) throws IOException {
        var dansBagValidatorClient = new JerseyClientBuilder(environment)
            .withProvider(MultiPartFeature.class)
            .using(configuration.getValidateDansBag().getHttpClient())
            .build(getName());

        var languageResolver = new CsvLanguageResolver(
            configuration.getIngestFlow().getLanguages().getIso6391(),
            configuration.getIngestFlow().getLanguages().getIso6392()
        );

        var countryResolver = new FileCountryResolver(
            configuration.getIngestFlow().getSpatialCoverageCountryTermsPath()
        );
        var xmlReader = new XmlReader();
        var depositValidator = new DepositValidator(dansBagValidatorClient, configuration.getValidateDansBag().getValidateUrl());
        var depositFactory = new DepositManager(xmlReader);

        var rdaBagWriterFactory = new DefaultRdaBagWriterFactory(
            environment.getObjectMapper(),
            languageResolver,
            countryResolver
        );

        var outputWriterFactory = new ZipBagOutputWriterFactory(configuration.getIngestFlow().getRdaBagOutputDir());

        var ocflObjectVersionApi = new OcflObjectVersionApi();
        ocflObjectVersionApi.setCustomBaseUrl(configuration.getVaultCatalog().getUrl().toString());

        var vaultCatalogRepository = new VaultCatalogClient(ocflObjectVersionApi);

        var depositToBagProcess = new DepositToBagProcess(
            rdaBagWriterFactory,
            outputWriterFactory,
            vaultCatalogRepository,
            depositFactory,
            depositValidator,
            new IdMinter()
        );

        var taskQueue = configuration.getIngestFlow().getTaskQueue().build(environment);

        var ingestAreaDirectoryWatcher = new IngestAreaDirectoryWatcher(
            500,
            configuration.getIngestFlow().getAutoIngest().getInbox()
        );

        var autoIngestOutbox = new DepositOutbox(configuration.getIngestFlow().getAutoIngest().getOutbox());
        var inboxListener = new AutoIngestArea(
            taskQueue,
            ingestAreaDirectoryWatcher,
            depositToBagProcess,
            autoIngestOutbox
        );

        inboxListener.start();

        environment.healthChecks().register(
            "DansBagValidator",
            new DansBagValidatorHealthCheck(
                dansBagValidatorClient, configuration.getValidateDansBag().getPingUrl()
            )
        );
    }
}
