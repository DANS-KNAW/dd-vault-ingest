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

import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.reader.BagReader;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class DepositBagIntegrationTest {

    @Test
    void getMetadataFiles() throws Exception {
        var bag = getBag();
        var depositBag = new DepositBag(bag);

        var metadataFiles = depositBag.getMetadataFiles();

        assertThat(metadataFiles).containsOnly(
            Path.of("metadata/files.xml"),
            Path.of("metadata/dataset.xml")
        );
    }

    @Test
    void inputStreamForMetadataFile() throws Exception {
        var bag = getBag();
        var depositBag = new DepositBag(bag);

        try (var file = depositBag.inputStreamForMetadataFile(Path.of("metadata/dataset.xml"))) {
            var data = new String(file.readAllBytes());

            // check if it read the complete xml file
            assertThatNoException().isThrownBy(() -> {
                new XmlReader().readXmlString(data);
            });

            assertThat(data).startsWith("<ddm:DDM");
        }
    }

    @Test
    void getMetadataValue() throws Exception {
        var bag = getBag();
        var depositBag = new DepositBag(bag);

        assertThat(depositBag.getMetadataValue("Payload-Oxum")).containsOnly("3212481.4");
        assertThat(depositBag.getMetadataValue("Bagging-Date")).containsOnly("2022-10-23");
        assertThat(depositBag.getMetadataValue("Bag-Size")).containsOnly("3.1 MB");
        assertThat(depositBag.getMetadataValue("Created")).containsOnly("2016-11-12T23:41:11.000+00:00");
        assertThat(depositBag.getMetadataValue("Has-Organizational-Identifier")).containsOnly("REPO1:1234");
        assertThat(depositBag.getMetadataValue("Has-Organizational-Identifier-Version")).containsOnly("1.1");
        assertThat(depositBag.getMetadataValue("Does-Not-Exist")).isEmpty();
    }

    Bag getBag() throws Exception {
        var bagPath = "/input/integration-test-complete-bag/c169676f-5315-4d86-bde0-a62dbc915228/audiences/";
        var resource = Path.of(Objects.requireNonNull(getClass().getResource(bagPath)).getPath());

        return new BagReader().read(resource);
    }
}