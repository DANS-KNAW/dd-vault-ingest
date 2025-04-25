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
package nl.knaw.dans.vaultingest.core.rdabag;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.knaw.dans.bagit.domain.Bag;
import nl.knaw.dans.bagit.reader.BagReader;
import nl.knaw.dans.bagit.verify.BagVerifier;
import nl.knaw.dans.vaultingest.AbstractTestWithTestDir;
import nl.knaw.dans.vaultingest.core.datacite.DataciteConverter;
import nl.knaw.dans.vaultingest.core.datacite.DataciteSerializer;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.oaiore.OaiOreConverter;
import nl.knaw.dans.vaultingest.core.oaiore.OaiOreSerializer;
import nl.knaw.dans.vaultingest.core.pidmapping.PidMappingConverter;
import nl.knaw.dans.vaultingest.core.pidmapping.PidMappingSerializer;
import nl.knaw.dans.vaultingest.core.testutils.TestCountryResolverSingleton;
import nl.knaw.dans.vaultingest.core.testutils.TestLanguageResolverSingleton;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DansBagToRdaBagEnricherTest extends AbstractTestWithTestDir {

    @Test
    public void write_should_keep_bag_valid() throws Exception {
        var manager = new DepositManager(new XmlReader());
        var inputDeposit = Path.of("src/test/resources/input/c169676f-5315-4d86-bde0-a62dbc915228");
        var testDeposit = testDir.resolve(inputDeposit.getFileName());
        FileUtils.copyDirectory(inputDeposit.toFile(), testDeposit.toFile());
        var deposit = manager.loadDeposit(testDeposit, Map.of("user001", "Name of user"));
        assertThat(isBagValid(deposit.getBagDir())).isTrue(); // Valid before enriching
        var rdaBag = testDir.resolve("rda-bag.zip");

        var enricher = new DansBagToRdaBagEnricher(
            deposit,
            new DataciteSerializer(),
            new PidMappingSerializer(),
            new OaiOreSerializer(new ObjectMapper()),
            new DataciteConverter(),
            new PidMappingConverter(),
            new OaiOreConverter(TestLanguageResolverSingleton.getInstance(), TestCountryResolverSingleton.getInstance())
        );

        enricher.write(rdaBag);
        // check that the following files are present in the bag
        assertThat(deposit.getBagDir().resolve("metadata/datacite.xml")).exists();
        assertThat(deposit.getBagDir().resolve("metadata/pid-mapping.txt")).exists();
        assertThat(deposit.getBagDir().resolve("metadata/oai-ore.jsonld")).exists();
        assertThat(deposit.getBagDir().resolve("metadata/oai-ore.rdf")).exists();
        assertThat(isBagValid(deposit.getBagDir())).isTrue(); // Valid after enriching
    }

    private boolean isBagValid(Path bagDir) throws Exception {
        BagReader bagReader = new BagReader();
        Bag bag = bagReader.read(bagDir);

        try (BagVerifier bagVerifier = new BagVerifier()) {
            bagVerifier.isValid(bag, false);
            return true;
        }
        catch (Exception e) {
            // if the exception class is in the nl.knaw.dans.bagit.exceptions package, then the bag is invalid
            if (e.getClass().getPackageName().equals("nl.knaw.dans.bagit.exceptions")) {
                return false;
            }
            throw e;
        }
    }
}