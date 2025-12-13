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

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdOptions;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.SysRIOT;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.SchemaDO;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;
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

        setNamespacePrefixes(model);

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
        /*
         * Framing got a bit more involved in Jena 5. We first have to create a JSON-LD, read it as a JSON structure, create a frame as a JSON structure,
         * and then apply the frame using the JSON-LD Java library.
         */

        // The type must be set to "ResourceMap" so that the resource-map in the model will be used as the root resource.
        // The context must be created as a string; setNamespacePrefixes has no effect.
        var frame = String.format("""
            {
              "@context": %s,
              "@type": "ore:ResourceMap"
            }
            """, namespacesAsJsonObject(getUsedNamespaces(model)));

        String jsonLd = RDFWriter.create()
            .source(model)
            .format(RDFFormat.JSONLD11_PRETTY)
            .asString();

        JsonStructure inputJson;
        try (JsonReader r = Json.createReader(new StringReader(jsonLd))) {
            inputJson = r.read();
        }

        JsonStructure frameJson;
        try (JsonReader r = Json.createReader(new StringReader(frame))) {
            frameJson = r.read();
        }

        JsonDocument inputDoc = JsonDocument.of(inputJson);
        JsonDocument frameDoc = JsonDocument.of(frameJson);

        JsonLdOptions options = new JsonLdOptions();
        options.setOmitGraph(true);

        try {
            JsonStructure framed = JsonLd.frame(inputDoc, frameDoc)
                .options(options).get();

            StringWriter out = new StringWriter();
            Json.createWriter(out).write(framed);

            return out.toString();
        }
        catch (Exception e) {
            log.error("Error framing JSON-LD", e);
            throw new RuntimeException(e);
        }
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
        namespaces.put("dansRelationMetadata", DansRel.NS);
        namespaces.put("dansRights", DansRights.NS);
        namespaces.put("dansTemporalSpatial", DansTS.NS);
        namespaces.put("dansArchaeologyMetadata", DansArchaeology.NS);
        namespaces.put("dansDataVaultMetadata", DansDVMetadata.NS);

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

    private void setNamespacePrefixes(Model model) {
        var namespaces = getNamespaces();

        for (var namespace : namespaces.entrySet()) {
            model.setNsPrefix(namespace.getKey(), namespace.getValue());
        }
    }
}
