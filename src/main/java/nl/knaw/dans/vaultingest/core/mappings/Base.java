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

import nl.knaw.dans.vaultingest.core.mappings.ids.DAI;
import nl.knaw.dans.vaultingest.core.mappings.ids.ISNI;
import nl.knaw.dans.vaultingest.core.mappings.ids.ORCID;
import nl.knaw.dans.vaultingest.core.mappings.ids.VIAF;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import nl.knaw.dans.vaultingest.core.xml.XmlNamespaces;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class Base {

    static String getFirstValue(Node node, String expression) {
        return XPathEvaluator.strings(node, expression).map(String::trim).findFirst().orElse(null);
    }

    static DAI getDAI(Node node) {
        return XPathEvaluator.strings(node, "dcx-dai:DAI").map(DAI::new).findFirst().orElse(null);
    }

    static ISNI getISNI(Node node) {
        return XPathEvaluator.strings(node, "dcx-dai:ISNI").map(ISNI::new).findFirst().orElse(null);
    }

    static ORCID getORCID(Node node) {
        return XPathEvaluator.strings(node, "dcx-dai:ORCID").map(ORCID::new).findFirst().orElse(null);
    }

    static VIAF getVIAF(Node node) {
        // the example doesnt use dcx-dai:VIAF, should be looked into
        //        return XPathEvaluator.strings(node, "dcx-dai:VIAF").map(VIAF::new).findFirst().orElse(null);
        return XPathEvaluator.strings(node, "dcx-dai:identifier[@scheme='VIAF']/@value")
            .map(VIAF::new).findFirst().orElse(null);
    }

    static String getIdTypeNamespace(Document document) {
        return document.lookupPrefix(XmlNamespaces.NAMESPACE_ID_TYPE);
    }

    static List<Statement> toBasicTerms(Resource resource, Property property, Collection<String> values) {
        if (values == null) {
            return List.of();
        }

        var model = resource.getModel();
        var result = new ArrayList<Statement>();

        for (var value : values) {
            result.add(model.createStatement(
                resource,
                property,
                value
            ));
        }

        return result;
    }

    static Optional<Statement> toBasicTerm(Resource resource, Property property, String value) {
        if (value == null) {
            return Optional.empty();
        }

        var model = resource.getModel();

        return Optional.of(model.createStatement(
            resource,
            property,
            value
        ));
    }

    static Optional<Statement> toBasicTerm(Resource resource, Property property, Boolean value) {
        if (value == null) {
            return Optional.empty();
        }

        var model = resource.getModel();

        return Optional.of(model.createStatement(
            resource,
            property,
            model.createTypedLiteral(value)
        ));
    }

    static <T> List<Statement> toComplexTerms(Resource resource, Property property, Collection<T> values, TermMapper<T> mapper) {
        if (values == null) {
            return List.of();
        }

        var model = resource.getModel();
        var result = new ArrayList<Statement>();

        for (var value : values) {
            var element = model.createResource();
            mapper.map(element, value);

            result.add(model.createStatement(
                resource,
                property,
                element
            ));
        }

        return result;
    }

    public interface TermMapper<T> {

        void map(Resource element, T value);
    }
}
