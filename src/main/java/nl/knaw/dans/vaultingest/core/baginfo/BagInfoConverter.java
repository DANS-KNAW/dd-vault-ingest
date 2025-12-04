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
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.deposit.DepositBag;
import nl.knaw.dans.vaultingest.core.mappings.Descriptions;
import nl.knaw.dans.vaultingest.core.mappings.Titles;

import java.io.IOException;

public class BagInfoConverter {
    public static final String KEY_CONTACT_NAME = "Contact-Name";
    public static final String KEY_CONTACT_EMAIL = "Contact-Email";
    public static final String KEY_SOURCE_ORGANIZATION = "Source-Organization";
    public static final String KEY_EXTERNAL_DESCRIPTION = "External-Description";
    public static final String KEY_EXTERNAL_IDENTIFIER = "External-Identifier";
    public static final String KEY_INTERNAL_SENDER_IDENTIFIER = "Internal-Sender-Identifier";
    public static final String KEY_HAS_ORGANIZATIONAL_IDENTIFIER = "Has-Organizational-Identifier";

    public void convert(Deposit deposit, ContactPersonConfig contactPersonConfig, DepositBag depositBag) throws IOException {
        // BAGINFO001A
        depositBag.putBagInfoValue(KEY_CONTACT_NAME, contactPersonConfig.getName());
        depositBag.putBagInfoValue(KEY_CONTACT_EMAIL, contactPersonConfig.getEmail());

        // BAGINFO002A
        depositBag.putBagInfoValue(KEY_SOURCE_ORGANIZATION, deposit.getDataSupplier());

        // BAGINFO005A
        try (var s = Descriptions.getAllProfileDescriptions(deposit.getDdm())) {
            for (var d : s.toList()) {
                depositBag.putBagInfoValue(KEY_EXTERNAL_DESCRIPTION, d.getValue());
            }
        }

        // BAGINFO006A
        copyHasOrganizationalIdentifierToExternalIdentifier(depositBag);

        // BAGINFO007A
        depositBag.putBagInfoValue(KEY_INTERNAL_SENDER_IDENTIFIER, Titles.getTitle(deposit.getDdm()));
    }

    private void copyHasOrganizationalIdentifierToExternalIdentifier(DepositBag depositBag) throws IOException {
        var hasOrgIds = depositBag.getBagInfoValues(KEY_HAS_ORGANIZATIONAL_IDENTIFIER);
        var externalDescriptions = depositBag.getBagInfoValues(KEY_EXTERNAL_IDENTIFIER);

        if (hasOrgIds != null) {
            for (var orgId : hasOrgIds) {
                if (externalDescriptions == null || !externalDescriptions.contains(orgId)) {
                    depositBag.putBagInfoValue(KEY_EXTERNAL_IDENTIFIER, orgId);
                }
            }
        }
    }

}
