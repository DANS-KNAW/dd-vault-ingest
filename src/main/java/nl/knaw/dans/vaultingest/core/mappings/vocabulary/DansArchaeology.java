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

public class DansArchaeology {
    public static final String NS = "https://schemas.dans.knaw.nl/metadatablock/dansArchaeologyMetadata#";
    private static final Model m = ModelFactory.createDefaultModel();
    public static final Property dansArchisZaakId = m.createProperty(NS, "dansArchisZaakId");
    public static final Property dansArchisNumber = m.createProperty(NS, "dansArchisNumber");
    public static final Property dansArchisNumberId = m.createProperty(NS, "dansArchisNumberId");
    public static final Property dansArchisNumberType = m.createProperty(NS, "dansArchisNumberType");
    public static final Property dansAbrRapportType = m.createProperty(NS, "dansAbrRapportType");
    public static final Property dansAbrRapportNummer = m.createProperty(NS, "dansAbrRapportNummer");
    public static final Property dansAbrVerwervingswijze = m.createProperty(NS, "dansAbrVerwervingswijze");
    public static final Property dansAbrComplex = m.createProperty(NS, "dansAbrComplex");
    public static final Property dansAbrArtifact = m.createProperty(NS, "dansAbrArtifact");
    public static final Property dansAbrPeriod = m.createProperty(NS, "dansAbrPeriod");
}
