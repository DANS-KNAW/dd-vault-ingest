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
package nl.knaw.dans.vaultingest.core.mappings;

import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.mappings.metadata.Contributor;
import nl.knaw.dans.vaultingest.core.mappings.metadata.DatasetRelation;
import nl.knaw.dans.vaultingest.core.mappings.metadata.Description;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Datacite {

    public static String getTitle(Deposit deposit) {
        return Titles.getTitle(deposit.getDdm());
    }

    public static List<DatasetRelation> getAuthors(Deposit deposit) {
        var items = new ArrayList<DatasetRelation>();
        items.addAll(Authors.getAuthors(deposit.getDdm()));
        items.addAll(Authors.getCreators(deposit.getDdm()));
        items.addAll(Authors.getOrganizations(deposit.getDdm()));

        return items;
    }

    public static List<Description> getDescriptions(Deposit deposit) {
        return Descriptions.getDescriptions(deposit.getDdm());
    }

    public static List<Contributor> getContributors(Deposit deposit) {
        return Contributors.getContributors(deposit.getDdm());
    }

    public static LocalDate getPublicationDate(Deposit deposit) {
        return Available.getAvailableDate(deposit.getDdm());
    }
}
