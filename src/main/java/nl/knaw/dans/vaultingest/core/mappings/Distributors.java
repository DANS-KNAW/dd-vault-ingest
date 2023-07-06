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
package nl.knaw.dans.vaultingest.core.mappings;

import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.mappings.metadata.Distributor;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.w3c.dom.Document;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Distributors extends Base {

    // CIT024
    public static List<Statement> toRDF(Resource resource, Deposit deposit) {
        return toDistributors(resource, getDistributors(deposit.getDdm()));
    }

    static List<Distributor> getDistributors(Document document) {
        var filter = "[text() != 'DANS' and text() != 'DANS/KNAW' and text() != 'DANS-KNAW']";

        return XPathEvaluator.strings(document,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:publisher" + filter,
                "/ddm:DDM/ddm:dcmiMetadata/dc:publisher" + filter)
            .map(value -> Distributor.builder()
                .name(value)
                .build())
            .collect(Collectors.toList());
    }

    static List<Statement> toDistributors(Resource resource, Collection<Distributor> distributors) {
        return toComplexTerms(resource, DVCitation.distributor, distributors, (element, distributor) -> {
            if (distributor.getName() != null) {
                element.addProperty(DVCitation.distributorName, distributor.getName());
            }
        });
    }
}
