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
import nl.knaw.dans.vaultingest.core.mappings.metadata.Description;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.w3c.dom.Document;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Descriptions extends Base {

    public static List<Statement> toRDF(Resource resource, Deposit deposit) {
        return toDescriptions(resource, getDescriptions(deposit.getDdm()));
    }

    static List<Description> getDescriptions(Document document) {
        // CIT009, profile / description
        var profileDescriptions = getAllProfileDescriptions(document);

        // CIT010, first title is for 002, the rest should go into the descriptions
        var titles = AlternativeTitles.getAlternativeTitles(document)
            .stream().skip(1)
            .map(s -> Description.builder()
                .value(s)
                .build()
            )
            .collect(Collectors.toList());

        // CIT011, dcmiMetadata / [tags]
        var dcmiDescriptions = XPathEvaluator.nodes(document,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:date",
                "/ddm:DDM/ddm:dcmiMetadata/dc:date",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateAccepted",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateCopyrighted",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:dateSubmitted",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:modified",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:issued",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:valid",
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:coverage")
            .map(node -> Description.builder()
                .type(node.getLocalName())
                .value(node.getTextContent().trim())
                .build()
            );

        // CIT012, dcmiMetadata / description
        var dcmiDescription = XPathEvaluator.strings(document,
                "/ddm:DDM/ddm:dcmiMetadata/dcterms:description")
            .map(value -> Description.builder()
                .value(value.trim())
                .build()
            );

        var result = Stream.concat(profileDescriptions,
            Stream.concat(dcmiDescriptions, dcmiDescription)
        ).collect(Collectors.toList());

        result.addAll(titles);

        return result;
    }

    public static Stream<Description> getAllProfileDescriptions(Document document) {
        return XPathEvaluator.strings(document,
            "/ddm:DDM/ddm:profile/dc:description",
            "/ddm:DDM/ddm:profile/dcterms:description"
        ).map(value -> Description.builder()
            .value(value.trim())
            .build()
        );
    }

    static List<Statement> toDescriptions(Resource resource, Collection<Description> descriptions) {
        return toComplexTerms(resource, DVCitation.dsDescription, descriptions, (element, description) -> {
            element.addProperty(DVCitation.dsDescriptionValue, description.getValue());

            if (description.getDate() != null) {
                element.addProperty(DVCitation.dsDescriptionDate, description.getDate());
            }
        });
    }
}
