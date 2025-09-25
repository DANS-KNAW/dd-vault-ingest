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
import com.github.jsonldjava.core.JsonLdOptions;
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
import org.apache.jena.riot.JsonLDWriteContext;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.riot.RDFWriter;
import org.apache.jena.riot.SysRIOT;

import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.DC_11;
import org.apache.jena.vocabulary.SchemaDO;

import java.io.ByteArrayOutputStream;

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
         * Framing is a way to control the layout of the resulting JSON-LD document. We want to get a similar result as Dataverse. By default, Jena will produce a document with the key "@graph", which
         * contains an array of resources. The document we are producing describes a resource map, which is a single resource. The aggregation it describes (i.e., the dataset) can be embedded in the
         * resource map, as in their turn, can the files that are aggregated by the dataset.
         *
         * Please note that the code contains several pieces that must not be changed, or Jena will revert to using "@graph", sometimes seven an empty "@graph".
         */

        // The type must be set to "ResourceMap" so that the resource-map in the model will be used as the root resource.
        // The context must be created as a string; setNamespacePrefixes has no effect.
        var frame = String.format("""
            {
              "@context": %s,
              "@type": "ore:ResourceMap"
            }
            """, namespacesAsJsonObject(getUsedNamespaces(model)));

        JsonLdOptions opts = new JsonLdOptions();
        opts.setOmitGraph(true); // To suppress inserting the "@graph" key. Otherwise, Jena will insert it, even though it is not necessary.
        opts.setPruneBlankNodeIdentifiers(true); // To suppress generating IDs for nodes that do not have an explicit identifier.

        var ctx = new JsonLDWriteContext(); // We use JSON-LD 1.0. TODO: How to achieve the same result with JSON-LD 1.1?
        ctx.setFrame(frame); // Use the frame defined above.
        ctx.setOptions(opts);

        return RDFWriter.create()
            .source(model) // Do not wrap the model in a graph
            .format(RDFFormat.JSONLD10_FRAME_PRETTY) // Use JSON-LD 1.0. It is essential to use this format, otherwise the frame will not be applied.
            .context(ctx)
            .asString();
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

    private void setNamespacePrefixes(Model model) {
        var namespaces = getNamespaces();

        for (var namespace : namespaces.entrySet()) {
            model.setNsPrefix(namespace.getKey(), namespace.getValue());
        }
    }
}
