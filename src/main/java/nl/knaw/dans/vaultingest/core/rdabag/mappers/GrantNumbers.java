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
package nl.knaw.dans.vaultingest.core.rdabag.mappers;

import nl.knaw.dans.vaultingest.core.domain.metadata.GrantNumber;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCitation;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.SchemaDO;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GrantNumbers {

    public static List<Statement> toGrantNumbers(Resource resource, Collection<GrantNumber> grantNumbers) {
        if (grantNumbers == null) {
            return List.of();
        }

        var model = resource.getModel();
        var result = new ArrayList<Statement>();

        for (var grantNumber : grantNumbers) {
            var element = model.createResource();

            if (grantNumber.getAgency() != null) {
                element.addProperty(DVCitation.grantNumberAgency, grantNumber.getAgency());
            }
            if (grantNumber.getValue() != null) {
                element.addProperty(DVCitation.grantNumberValue, grantNumber.getValue());
            }

            result.add(model.createStatement(
                resource,
                SchemaDO.sponsor,
                element
            ));
        }

        return result;
    }
}
