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
package nl.knaw.dans.vaultingest.core.util;

import nl.knaw.dans.bagit.hash.SupportedAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class MultiDigestInputStream extends InputStream {
    private final InputStream inputStream;
    private final Map<SupportedAlgorithm, DigestInputStream> digestInputStreams;

    public MultiDigestInputStream(InputStream inputStream, Collection<SupportedAlgorithm> algorithms) throws NoSuchAlgorithmException {
        this.digestInputStreams = new HashMap<>();

        var input = inputStream;

        for (var alg : algorithms) {
            var digestInputStream = new DigestInputStream(input, MessageDigest.getInstance(alg.getMessageDigestName()));
            digestInputStreams.put(alg, digestInputStream);
            input = digestInputStream;
        }

        this.inputStream = input;
    }

    public Map<SupportedAlgorithm, String> getChecksums() {
        var result = new HashMap<SupportedAlgorithm, String>();

        for (var entry : digestInputStreams.entrySet()) {
            result.put(entry.getKey(), bytesToHex(entry.getValue().getMessageDigest().digest()));
        }

        return result;
    }

    private String bytesToHex(byte[] digest) {
        var sb = new StringBuilder();
        for (var b : digest) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    @Override
    public int read() throws IOException {
        return this.inputStream.read();
    }

    @Override
    // Overriding this method has significant performance benefits for the DigestInputStream
    public int read(byte[] b, int off, int len) throws IOException {
        return this.inputStream.read(b, off, len);
    }

    @Override
    public void close() throws IOException {
        super.close();

        this.inputStream.close();

        for (var value : digestInputStreams.values()) {
            value.close();
        }
    }
}
