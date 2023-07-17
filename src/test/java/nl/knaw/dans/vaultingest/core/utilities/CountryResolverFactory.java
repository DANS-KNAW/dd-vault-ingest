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
package nl.knaw.dans.vaultingest.core.utilities;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.deposit.CountryResolver;
import nl.knaw.dans.vaultingest.core.deposit.FileCountryResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
public class CountryResolverFactory {
    private static CountryResolver instance;

    public static CountryResolver getInstance() throws Exception {
        if (instance == null) {
            try {
                var path = Path.of(
                    Objects.requireNonNull(
                        CountryResolverFactory.class
                            .getResource("/debug-etc/spatial-coverage-country-terms.txt")
                    ).getPath()
                );

                instance = new FileCountryResolver(path);
            }
            catch (IOException e) {
                e.printStackTrace();
                log.error("Could not load csv", e);
                throw e;
            }
        }

        return instance;
    }
}
