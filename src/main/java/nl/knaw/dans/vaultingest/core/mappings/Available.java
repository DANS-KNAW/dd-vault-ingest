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
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.w3c.dom.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class Available extends Base {
    private static final DateTimeFormatter yyyyMMddFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static List<Statement> toRDF(Resource resource, Deposit deposit) {
        return toDistributionDate(resource, getAvailableDate(deposit.getDdm())).stream().toList();
    }

    public static LocalDate getAvailableDate(Document document) {
        return XPathEvaluator.strings(document, "/ddm:DDM/ddm:profile/ddm:available")
            .findFirst()
            .map(Available::toYearMonthDayFormat)
            .orElse(null);
    }

    public static LocalDate getEmbargoDate(Document document) {
        var availableDate = getAvailableDate(document);
        // If after now then it's an embargo date otherwise null
        if (availableDate != null && availableDate.isAfter(LocalDate.now())) {
            return availableDate;
        }
        else {
            return null;
        }
    }

    static LocalDate toYearMonthDayFormat(String text) {
        return LocalDate.parse(text);
    }

    static Optional<Statement> toDistributionDate(Resource resource, LocalDate distributionDate) {
        return toBasicTerm(resource, DVCitation.distributionDate, yyyyMMddFormatter.format(distributionDate));
    }
}
