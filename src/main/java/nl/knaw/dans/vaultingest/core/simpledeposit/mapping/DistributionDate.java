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
package nl.knaw.dans.vaultingest.core.simpledeposit.mapping;

import nl.knaw.dans.vaultingest.core.rdabag.converter.mappers.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Document;

import java.util.Optional;

public class DistributionDate extends Base {
    private static final DateTimeFormatter yyyymmddPattern = DateTimeFormat.forPattern("YYYY-MM-dd");

    public static Optional<Statement> toRDF(Document document, Resource resource) {
        return toDistributionDate(resource, getDistributionDate(document));
    }

    public static String getDistributionDate(Document document) {
        return XPathEvaluator.strings(document, "/ddm:DDM/ddm:profile/ddm:available")
            .findFirst()
            .map(DistributionDate::toYearMonthDayFormat)
            .orElse(null);
    }

    static String toYearMonthDayFormat(String text) {
        var date = DateTime.parse(text);
        return yyyymmddPattern.print(date);
    }

    public static Optional<Statement> toDistributionDate(Resource resource, String distributionDate) {
        return toBasicTerm(resource, DVCitation.distributionDate, distributionDate);
    }
}
