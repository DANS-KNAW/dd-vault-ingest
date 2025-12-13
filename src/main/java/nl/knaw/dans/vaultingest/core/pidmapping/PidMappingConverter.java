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
package nl.knaw.dans.vaultingest.core.pidmapping;

import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.util.BagItFilePathEncoding;

public class PidMappingConverter {

    public PidMappings convert(Deposit deposit) {
        var dataPath = "data/";
        var mappings = new PidMappings();

        var doi = deposit.getDoi();

        if (doi != null) {
            mappings.addMapping(doi, dataPath);
        }

        for (var file : deposit.getPayloadFiles()) {
            mappings.addMapping(file.getId(), BagItFilePathEncoding.encodeFilePath(file.getPath()));
        }

        return mappings;
    }
}
