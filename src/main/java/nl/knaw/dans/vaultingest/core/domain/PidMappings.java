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
package nl.knaw.dans.vaultingest.core.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PidMappings {
    private final Map<String, String> mapping = new HashMap<>();
    private final List<PidMapping> orderedMappings = new ArrayList<>();

    public void addMapping(String id, String path) {
        if (!mapping.containsKey(id)) {
            orderedMappings.add(new PidMapping(id, path));
        }

        mapping.put(id, path);
    }

    public Collection<PidMapping> getPidMappings() {
        return Collections.unmodifiableList(orderedMappings);
    }

    @Getter
    @AllArgsConstructor
    public static class PidMapping {
        private final String id;
        private final String path;
    }
}
