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
package nl.knaw.dans.vaultingest.core.mappings.vocabulary;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;

public class DansRel {
    public static final String NS = "https://schemas.dans.knaw.nl/metadatablock/dansRelationMetadata#";
    private static final Model m = ModelFactory.createDefaultModel();
    public static final Property dansAudience = m.createProperty(NS, "dansAudience");
    public static final Property dansCollection = m.createProperty(NS, "dansCollection");
    public static final Property dansRelation = m.createProperty(NS, "dansRelation");
    public static final Property dansRelationType = m.createProperty(NS, "dansRelationType");
    public static final Property dansRelationURI = m.createProperty(NS, "dansRelationURI");
    public static final Property dansRelationText = m.createProperty(NS, "dansRelationText");
}
