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
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DansRel;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.w3c.dom.Document;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class InCollection extends Base {
    public static List<Statement> toRDF(Resource resource, Deposit deposit) {
        return toDansCollections(resource, getInCollections(deposit.getDdm()));
    }

    static List<String> getInCollections(Document document) {
        var results = XPathEvaluator.strings(document,
                "/ddm:DDM/ddm:dcmiMetadata/ddm:inCollection" +
                    "[@subjectScheme = 'DANS Collection' and @schemeURI = 'https://vocabularies.dans.knaw.nl/collections']" +
                    "/@valueURI")
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());

        return List.copyOf(results);
    }

    static List<Statement> toDansCollections(Resource resource, Collection<String> collections) {
        return toBasicTerms(resource, DansRel.dansCollection, collections);
    }
}
