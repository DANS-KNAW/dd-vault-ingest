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

import nl.knaw.dans.vaultingest.core.rdabag.output.BagOutputWriter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class InMemoryOutputWriter implements BagOutputWriter {

    private final Map<Path, String> data = new HashMap<>();
    private boolean closed = false;

    @Override
    public void writeBagItem(InputStream inputStream, Path path) {
        var output = new ByteArrayOutputStream();

        try {
            inputStream.transferTo(output);
            data.put(path, output.toString());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        this.closed = true;
        // noop
    }

    public Map<Path, String> getData() {
        return data;
    }

    public boolean isClosed() {
        return closed;
    }
}
