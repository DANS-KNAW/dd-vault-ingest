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
package nl.knaw.dans.vaultingest.core.domain.metadata;

import lombok.Builder;
import lombok.Value;
import nl.knaw.dans.vaultingest.core.domain.ids.Identifier;

@Value
@Builder
public class DatasetCreator implements DatasetRelation {
    String name;

    @Override
    public String getDisplayName() {
        return this.getName();
    }

    @Override
    public String getAffiliation() {
        return null;
    }

    @Override
    public Identifier getIdentifier() {
        return null;
    }
}
