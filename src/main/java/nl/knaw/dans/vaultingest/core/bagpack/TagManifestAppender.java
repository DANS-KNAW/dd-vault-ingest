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
package nl.knaw.dans.vaultingest.core.bagpack;

import nl.knaw.dans.vaultingest.core.util.BagItFilePathEncoding;

import java.nio.file.Path;
import java.util.Map;

public class TagManifestAppender {

    public String appendNewEntries(Path rootDir, Map<Path, String> checksums) {
        var outputString = new StringBuilder();

        for (var entry : checksums.entrySet()) {
            var path = entry.getKey();

            if (path.isAbsolute() && !path.startsWith(rootDir.toAbsolutePath())) {
                throw new IllegalStateException(String.format("Path %s is not a child of %s", path, rootDir));
            }
            else {
                path = rootDir.resolve(path);
            }

            var relativePath = rootDir.relativize(path);
            var safeFilename = BagItFilePathEncoding.encodeFilePath(relativePath);

            outputString.append(String.format("%s  %s\n", entry.getValue(), safeFilename));
        }

        return outputString.toString();
    }

}
