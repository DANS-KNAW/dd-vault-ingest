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

import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommonDepositManagerIntegrationTest {

    @Test
    void loadDeposit() {
        var manager = new DepositManager(new XmlReader());

        var s = getClass().getResource("/input/0b9bb5ee-3187-4387-bb39-2c09536c79f7");
        assert s != null;

        var deposit = manager.loadDeposit(Path.of(s.getPath()), "Name of user");
        assertThat(deposit.getId()).isEqualTo("0b9bb5ee-3187-4387-bb39-2c09536c79f7");
    }

    @Test
    void loadDeposit_should_handle_OriginalFilePaths() throws Exception {
        var manager = new DepositManager(new XmlReader());

        var s = getClass().getResource("/input/0b9bb5ee-3187-4387-bb39-2c09536c79f7");
        assert s != null;
        var path = Path.of(s.getPath());

        // first verify there is actually an original-filepaths.txt file
        assertThat(Files.exists(path.resolve("audiences/original-filepaths.txt"))).isTrue();

        var deposit = manager.loadDeposit(path, "Name of user");
        var files = deposit.getPayloadFiles();

        // check that the file paths are the original ones, not the renamed ones
        assertThat(files).extracting("path").map(Object::toString)
            .containsOnly(
                "data/random images/image01.png",
                "data/random images/image02.jpeg",
                "data/random images/image03.jpeg",
                "data/a/deeper/path/With some file.txt"
            );

        // check that the files are actually readable
        for (var file : files) {
            var stream = new ByteArrayOutputStream();

            try (var input = file.openInputStream()) {
                input.transferTo(stream);
            }

            assertThat(stream.size()).isGreaterThan(0);
        }
    }
}