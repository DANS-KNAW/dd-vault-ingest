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
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.DCTerms;
import org.w3c.dom.Document;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class AvailableDate extends Base {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static Optional<Statement> toRDF(Resource resource, Deposit deposit) {
        var available = getAvailableDate(deposit.getDdm());
        return toAvailable(resource, available);
    }

    static Optional<Statement> toAvailable(Resource resource, LocalDate available) {
        return toBasicTerm(resource, DCTerms.available, available.format(formatter));
    }

    static LocalDate getAvailableDate(Document document) {
        return XPathEvaluator.strings(document, "/ddm:DDM/ddm:profile/ddm:available")
            .findFirst()
            .map(AvailableDate::toYearMonthDayFormat)
            .orElse(null);
    }

    static LocalDate toYearMonthDayFormat(String text) {
        return LocalDate.parse(text);
    }
}
