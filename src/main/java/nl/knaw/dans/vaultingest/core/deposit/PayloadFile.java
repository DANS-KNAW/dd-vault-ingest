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
package nl.knaw.dans.vaultingest.core.deposit;

import nl.knaw.dans.bagit.hash.SupportedAlgorithm;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.w3c.dom.Node;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

@ToString
@EqualsAndHashCode
@Builder
public class PayloadFile {
    @Getter
    private final String id;
    @Getter
    private final Node filesXmlNode;
    @Getter
    private final Node ddmNode;
    private final Path physicalPath;
    private final Map<SupportedAlgorithm, String> checksums;

    public Path getDirectoryLabel() {
        return getFilePath().getParent();
    }

    public Path getFilename() {
        return getFilePath().getFileName();
    }

    public Path getPath() {
        var directoryLabel = getDirectoryLabel();

        if (directoryLabel != null) {
            return getDirectoryLabel().resolve(getFilename());
        }

        return getFilename();
    }

    public InputStream openInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(physicalPath.toFile()));
    }

    private String getFilePathAttribute() {
        return filesXmlNode.getAttributes().getNamedItem("filepath").getTextContent();
    }

    private Path getFilePath() {
        return Path.of(getFilePathAttribute());
    }

    public Map<SupportedAlgorithm, String> getChecksums() {
        return Collections.unmodifiableMap(checksums);
    }
}
