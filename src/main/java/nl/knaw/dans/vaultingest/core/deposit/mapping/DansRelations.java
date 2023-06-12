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
package nl.knaw.dans.vaultingest.core.deposit.mapping;

import nl.knaw.dans.vaultingest.core.domain.metadata.DansRelation;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import org.w3c.dom.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DansRelations {
    private static final Map<String, String> labelToType = new HashMap<>();

    static {
        labelToType.put("relation", "relation");
        labelToType.put("conformsTo", "conforms_to");
        labelToType.put("hasFormat", "has_format");
        labelToType.put("hasPart", "has_part");
        labelToType.put("references", "references");
        labelToType.put("replaces", "replaces");
        labelToType.put("requires", "requires");
        labelToType.put("hasVersion", "has_version");
        labelToType.put("isFormatOf", "is_format_of");
        labelToType.put("isPartOf", "is_part_of");
        labelToType.put("isReferencedBy", "is_referenced_by");
        labelToType.put("isReplacedBy", "is_replaced_by");
        labelToType.put("isRequiredBy", "is_required_by");
        labelToType.put("isVersionOf", "is_version_of");
    }

    public static List<DansRelation> getDansRelations(Document document) {
        var queries = labelToType.keySet().stream()
            .map(name -> "/ddm:DDM/ddm:dcmiMetadata/ddm:" + name)
            .toArray(String[]::new);

        return XPathEvaluator.nodes(document.getDocumentElement(), queries)
            .map(item -> {
                var label = labelToType.get(item.getLocalName());
                var text = item.getTextContent();
                var uri = XPathEvaluator.strings(item, "@href").findFirst().orElse(null);

                return DansRelation.builder()
                    .type(label)
                    .text(text)
                    .uri(uri)
                    .build();
            })
            .collect(Collectors.toList());
    }
}
