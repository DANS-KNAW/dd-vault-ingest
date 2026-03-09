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
package nl.knaw.dans.vaultingest.core.testutils;

import lombok.Getter;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.deposit.DepositManager;
import nl.knaw.dans.vaultingest.core.xml.XmlReader;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestDepositManager extends DepositManager {
    private Deposit deposit;

    @Getter
    private boolean saveDepositCalled = false;

    @Getter
    private Deposit.State lastState = null;

    @Getter
    private String lastMessage = null;

    public TestDepositManager() {
        super(new XmlReader());
    }

    private TestDepositManager(Deposit deposit) {
        super(new XmlReader());
        this.deposit = deposit;
    }

    @Override
    public Deposit loadDeposit(Path path, String dataSupplier) {
        if (this.deposit != null) {
            return this.deposit;
        }

        var resource = getClass().getResource(path.toString());
        assert resource != null;
        var resourcePath = Path.of(resource.getPath());

        try {
            var tempDir = Files.createTempDirectory("test-deposit");
            var targetPath = tempDir.resolve(path.getFileName());
            FileUtils.copyDirectory(resourcePath.toFile(), targetPath.toFile());

            // Remove any "org-*" directories that might have been copied from contaminated test resources
            try (var list = Files.list(targetPath)) {
                list.filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().startsWith("org-"))
                    .forEach(p -> {
                        try {
                            FileUtils.deleteDirectory(p.toFile());
                        }
                        catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            }

            return super.loadDeposit(targetPath, dataSupplier);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void saveDepositProperties(Deposit deposit) {
        saveDepositCalled = true;
    }

    @Override
    public void updateDepositState(Path path, Deposit.State state, String message) {
        lastState = state;
        lastMessage = message;
    }
}
