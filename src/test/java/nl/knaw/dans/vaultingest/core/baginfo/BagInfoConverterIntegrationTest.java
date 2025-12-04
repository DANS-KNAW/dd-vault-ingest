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
package nl.knaw.dans.vaultingest.core.baginfo;

import nl.knaw.dans.vaultingest.config.ContactPersonConfig;
import nl.knaw.dans.vaultingest.core.deposit.DepositBag;
import nl.knaw.dans.vaultingest.core.testutils.TestDepositManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class BagInfoConverterIntegrationTest {

    @Test
    void contactName() throws Exception {
        var depositBag = convertBagInfo();

        assertThat(depositBag.getBagInfoValues(BagInfoConverter.KEY_CONTACT_NAME))
            .containsOnly("Test Contact Person");
    }

    @Test
    void contactEmail() throws Exception {
        var depositBag = convertBagInfo();

        assertThat(depositBag.getBagInfoValues(BagInfoConverter.KEY_CONTACT_EMAIL))
            .containsOnly("test@example.com");
    }

    @Test
    void sourceOrganization() throws Exception {
        var depositBag = convertBagInfo();

        assertThat(depositBag.getBagInfoValues(BagInfoConverter.KEY_SOURCE_ORGANIZATION))
            .containsOnly("Data Supplier");
    }

    @Test
    void externalDescription() throws Exception {
        var depositBag = convertBagInfo();

        var descriptions = depositBag.getBagInfoValues(BagInfoConverter.KEY_EXTERNAL_DESCRIPTION);

        assertThat(descriptions)
            .isNotNull()
            .contains("This bags contains one or more examples of each mapping rule.");
    }

    @Test
    void internalSenderIdentifier() throws Exception {
        var depositBag = convertBagInfo();

        assertThat(depositBag.getBagInfoValues(BagInfoConverter.KEY_INTERNAL_SENDER_IDENTIFIER))
            .containsOnly("A bag containing examples for each mapping rule");
    }

    @Test
    void copyHasOrganizationalIdentifierToExternalIdentifier() throws Exception {
        var depositBag = convertBagInfo();

        var externalIdentifiers = depositBag.getBagInfoValues(BagInfoConverter.KEY_EXTERNAL_IDENTIFIER);
        var hasOrgIds = depositBag.getBagInfoValues(BagInfoConverter.KEY_HAS_ORGANIZATIONAL_IDENTIFIER);

        if (hasOrgIds != null && !hasOrgIds.isEmpty()) {
            assertThat(externalIdentifiers)
                .isNotNull()
                .containsAll(hasOrgIds);
        }
    }

    private DepositBag convertBagInfo() throws Exception {
        var manager = new TestDepositManager();
        var deposit = manager.loadDeposit(Path.of("/input/integration-test-complete-bag/c169676f-5315-4d86-bde0-a62dbc915228/"), "Data Supplier");

        var contactPersonConfig = new ContactPersonConfig();
        contactPersonConfig.setName("Test Contact Person");
        contactPersonConfig.setEmail("test@example.com");

        var depositBag = deposit.getBag();
        new BagInfoConverter().convert(deposit, contactPersonConfig, depositBag);

        return depositBag;
    }
}
