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

import nl.knaw.dans.vaultingest.config.ContactPersonConfig;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DVCitation;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.List;

public class ContactPerson extends Base {
    public static List<Statement> toRDF(Resource resource, ContactPersonConfig contactPerson) {
        return toContactPerson(resource, contactPerson);
    }

    private static List<Statement> toContactPerson(Resource resource, ContactPersonConfig contactPerson) {
        return toComplexTerms(resource, DVCitation.datasetContact, List.of(contactPerson), (element, contact) -> {
            element.addProperty(DVCitation.datasetContactName, contact.getName());
            element.addProperty(DVCitation.datasetContactEmail, contact.getEmail());
            if (contact.getAffiliation() != null) {
                element.addProperty(DVCitation.datasetContactAffiliation, contact.getAffiliation());
            }
        });
    }
}
