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

import nl.knaw.dans.vaultingest.core.deposit.PayloadFile;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DVCore;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.SchemaDO;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class DataFile extends Base {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static List<Statement> toRDF(Resource resource, PayloadFile payloadFile, LocalDate embargoDate) {
        var result = new ArrayList<Statement>();

        // FIL001A
        toBasicTerm(resource, SchemaDO.name, payloadFile.getPath().toString())
            .ifPresent(result::add);

        // FIL002A
        var directoryLabel = Optional.ofNullable(payloadFile.getDirectoryLabel())
            .map(Path::toString)
            .orElse(null);

        toBasicTerm(resource, DVCore.directoryLabel, directoryLabel)
            .ifPresent(result::add);

        // FIL004A
        toBasicTerm(resource, SchemaDO.description, getDescription(payloadFile.getFilesXmlNode()))
            .ifPresent(result::add);

        // FIL005, FIL006
        toBasicTerm(resource, DVCore.restricted, isRestricted(payloadFile.getFilesXmlNode(), payloadFile.getDdmNode()))
            .ifPresent(result::add);

        result.addAll(toComplexTerms(resource, DVCore.embargoed, embargoDate != null ? List.of(embargoDate) : Collections.emptyList(), (element, value) -> {
            if (value != null) {
                element.addProperty(DVCore.dateAvailable, formatter.format(value));
                element.addProperty(DVCore.reason, "");
            }
        }));

        return result;
    }

    static String getDescription(Node filesXmlNode) {
        return XPathEvaluator.strings(filesXmlNode, "dcterms:description")
            .findFirst().orElse(null);
    }

    static String getRestricted(Node filesXmlNode, Node ddm) {
        return Boolean.toString(isRestricted(filesXmlNode, ddm));
    }

    static boolean isRestricted(Node filesXmlNode, Node ddm) {
        var accessibleToRights = getAccessibleToRights(filesXmlNode);
        var accessRights = getAccessRights(ddm);

        if (accessibleToRights != null) {
            // if ANONYMOUS then false else true
            return !"ANONYMOUS".equals(accessibleToRights);
        }

        if (accessRights != null) {
            // if OPEN_ACCESS then false else true
            return !"OPEN_ACCESS".equals(accessRights);
        }

        return false;
    }

    static String getAccessibleToRights(Node filesXmlNode) {
        return XPathEvaluator.strings(filesXmlNode, "files:accessibleToRights")
            .findFirst()
            .orElse(null);
    }

    static String getAccessRights(Node ddm) {
        return XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:profile/ddm:accessRights")
            .map(String::trim)
            .findFirst()
            .orElse(null);
    }

    // TODO add mapping if FIL008 is mapped in the document (see Ingest Flow Mapping Rules)
    static String getEmbargo(Node ddm) {
        var value = XPathEvaluator.strings(ddm, "/ddm:DDM/ddm:profile/ddm:available")
            .findFirst()
            .map(String::trim)
            .map(formatter::parse)
            .orElse(null);

        if (value != null) {
            var now = OffsetDateTime.now();
            var date = OffsetDateTime.from(value);

            if (date.isAfter(now)) {
                return formatter.format(value);
            }
        }

        return null;
    }
}
