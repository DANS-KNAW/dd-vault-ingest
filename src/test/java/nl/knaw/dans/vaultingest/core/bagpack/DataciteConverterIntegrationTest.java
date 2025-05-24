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

import nl.knaw.dans.vaultingest.core.datacite.DataciteConverter;
import nl.knaw.dans.vaultingest.core.datacite.DataciteSerializer;
import nl.knaw.dans.vaultingest.core.testutils.TestDepositManager;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class DataciteConverterIntegrationTest {

    @Test
    void identifier() throws Exception {
        var doc = loadResource();

        assertThat(XPathEvaluator.strings(doc, "//datacite:identifier")
            .collect(Collectors.toList()))
            .containsOnly("10.17026/dans-z6y-5y2e");
    }

    @Test
    void creators() throws Exception {
        var doc = loadResource();

        assertThat(XPathEvaluator.strings(doc, "//datacite:creators/datacite:creator/datacite:creatorName")
            .collect(Collectors.toList()))
            .containsOnly("Unformatted Creator", "I Lastname", "Creator Organization");

        assertThat(XPathEvaluator.strings(doc, "//datacite:creators/datacite:creator/datacite:affiliation")
            .collect(Collectors.toList()))
            .containsOnly("(Example Org)");

        assertThat(XPathEvaluator.strings(doc, "//datacite:creators/datacite:creator/datacite:nameIdentifier")
            .collect(Collectors.toList()))
            .containsOnly("0000-1111-2222-3333", "123456789");

        assertThat(XPathEvaluator.strings(doc, "//datacite:creators/datacite:creator/datacite:nameIdentifier/@nameIdentifierScheme")
            .collect(Collectors.toList()))
            .containsOnly("ORCID", "VIAF");
    }

    @Test
    void titles() throws Exception {
        var doc = loadResource();

        assertThat(XPathEvaluator.strings(doc, "//datacite:titles/datacite:title")
            .collect(Collectors.toList()))
            .containsOnly("A bag containing examples for each mapping rule");
    }

    @Test
    void resourceType() throws Exception {
        var doc = loadResource();

        assertThat(XPathEvaluator.strings(doc, "//datacite:resourceType/@resourceTypeGeneral")
            .collect(Collectors.toList()))
            .containsOnly("Dataset");
    }

    @Test
    void publicationYear() throws Exception {
        var doc = loadResource();

        assertThat(XPathEvaluator.strings(doc, "//datacite:publicationYear")
            .collect(Collectors.toList()))
            .containsOnly("2015");
    }

    @Test
    void descriptions() throws Exception {
        var doc = loadResource();

        assertThat(XPathEvaluator.strings(doc, "//datacite:descriptions/datacite:description")
            .collect(Collectors.toList()))
            .containsOnly("This bags contains one or more examples of each mapping rule.",
                "A second description",
                "some date",
                "some acceptance date",
                "some copyright date",
                "some submission date",
                "some modified date",
                "some issuing date",
                "some validation date",
                "some coverage description",
                "Even more descriptions",
                "DCTERMS title 2",
                "DCTERMS alt title 1",
                "DCTERMS alt title 2");

        assertThat(XPathEvaluator.strings(doc, "//datacite:descriptions/datacite:description/@descriptionType")
            .collect(Collectors.toList()))
            .containsOnly("Abstract");
    }

    // serialize to XML, then convert to Node, so we can use XPath to test the output
    private Document loadResource() throws Exception {
        var manager = new TestDepositManager();
        var deposit = manager.loadDeposit(Path.of("/input/integration-test-complete-bag/c169676f-5315-4d86-bde0-a62dbc915228/"), "Name of user");

        var resource = new DataciteConverter().convert(deposit);
        var xmlString = new DataciteSerializer().serialize(resource);

        return new XmlReader().readXmlString(xmlString);
    }
}