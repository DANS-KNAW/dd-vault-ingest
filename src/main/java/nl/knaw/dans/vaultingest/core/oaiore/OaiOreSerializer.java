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
package nl.knaw.dans.vaultingest.core.oaiore;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DVCitation;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DVCore;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DansArchaeology;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DansDVMetadata;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DansRel;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DansRights;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.DansTS;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.Datacite;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.ORE;
import nl.knaw.dans.vaultingest.core.mappings.vocabulary.PROV;
import org.apache.jena.query.DatasetFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.util.Context;
import org.apache.jena.sparql.util.Symbol;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.SchemaDO;

import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class OaiOreSerializer {

    private final ObjectMapper objectMapper;

    public OaiOreSerializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serializeAsRdf(Model model) {
        var topLevelResources = new Resource[] {
            ORE.AggregatedResource,
            ORE.Aggregation,
            ORE.ResourceMap,
        };

        applyNamespaces(model);

        var properties = new HashMap<String, Object>();
        properties.put("prettyTypes", topLevelResources);
        properties.put("showXmlDeclaration", "true");

        var output = new ByteArrayOutputStream();

        RDFWriter.create()
            .format(RDFFormat.RDFXML_ABBREV)
            .set(SysRIOT.sysRdfWriterProperties, properties)
            .source(model)
            .output(output);

        return output.toString();
    }

    public String serializeAsJsonLd(Model model) {
        var context = new Context();

        applyNamespaces(model);

        var namespaces = namespacesAsJsonObject(getUsedNamespaces(model));
        var contextStr = "{ \"@context\": [\n" +
            "    \"https://w3id.org/ore/context\",\n" +
            namespaces +
            "  ],\n" +
            "\n" +
            "   \"describes\": {\n" +
            "     \"@type\": \"Aggregation\",\n" +
            "     \"isDescribedBy\":  { \"@embed\": false } ,\n" +
            "     \"aggregates\":  { \"@embed\": true }  ,\n" +
            "     \"proxies\":  { \"@embed\": true }\n" +
            "   }\n" +
            " }";

        context.set(Symbol.create("jsonld.frame"), contextStr);
        var writer = RDFWriter.create()
            .format(RDFFormat.JSONLD11_PRETTY)
            .source(DatasetFactory.wrap(model).asDatasetGraph())
            .context(context)
            .build();

        var outputWriter = new StringWriter();
        writer.output(outputWriter);

        return outputWriter.toString();
    }

    private String namespacesAsJsonObject(Map<String, String> namespaces) {
        try {
            return objectMapper.writeValueAsString(namespaces);
        }
        catch (JsonProcessingException e) {
            log.error("Error serializing namespaces to JSON", e);
            throw new RuntimeException(e);
        }
    }

    private Map<String, String> getNamespaces() {
        var namespaces = new HashMap<String, String>();
        namespaces.put("cit", DVCitation.NS);
        namespaces.put("dcterms", DCTerms.NS);
        namespaces.put("datacite", Datacite.NS);
        namespaces.put("ore", ORE.NS);
        namespaces.put("dc", DC_11.NS);
        namespaces.put("foaf", FOAF.NS);
        namespaces.put("schema", SchemaDO.NS);
        namespaces.put("dvcore", DVCore.NS);
        namespaces.put("provo", PROV.NS);
        namespaces.put("dansREL", DansRel.NS);
        namespaces.put("dansRIG", DansRights.NS);
        namespaces.put("dansTS", DansTS.NS);
        namespaces.put("dansAR", DansArchaeology.NS);
        namespaces.put("dansVLT", DansDVMetadata.NS);

        return namespaces;
    }

    private Map<String, String> getUsedNamespaces(Model model) {
        var usedNamespaces = new HashSet<String>();
        model.listNameSpaces().forEachRemaining(usedNamespaces::add);

        return getNamespaces()
            .entrySet().stream()
            .filter(e -> usedNamespaces.contains(e.getValue()))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void applyNamespaces(Model model) {
        var namespaces = getNamespaces();

        for (var namespace : namespaces.entrySet()) {
            model.setNsPrefix(namespace.getKey(), namespace.getValue());
        }
    }
}
