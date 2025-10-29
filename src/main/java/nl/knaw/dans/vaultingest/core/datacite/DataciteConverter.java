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
package nl.knaw.dans.vaultingest.core.datacite;

import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.vaultingest.core.deposit.Deposit;
import nl.knaw.dans.vaultingest.core.mappings.Datacite;
import org.datacite.schema.kernel_4.Affiliation;
import org.datacite.schema.kernel_4.DescriptionType;
import org.datacite.schema.kernel_4.NameIdentifier;
import org.datacite.schema.kernel_4.Resource;
import org.datacite.schema.kernel_4.ResourceType;

import java.time.format.DateTimeFormatter;

@Slf4j
public class DataciteConverter {
    private static final DateTimeFormatter yearFormatter = DateTimeFormatter.ofPattern("yyyy");

    public Resource convert(Deposit deposit) {
        return getResource(deposit);
    }

    private Resource getResource(Deposit deposit) {
        var resource = new Resource();
        resource.setResourceType(getResourceType());
        // DATACITE001
        resource.setIdentifier(getIdentifier(deposit));
        // DATACITE003
        resource.setTitles(getTitles(deposit));
        // DATACITE004, DATACITE005, DATACITE006
        resource.setCreators(getCreators(deposit));
        // DATACITE008, DATACITE009, DATACITE010, DATACITE011, DATACITE012
        resource.setDescriptions(getDescriptions(deposit));
        // DATACITE015
        resource.setPublicationYear(getPublicationYear(deposit));

        return resource;
    }

    private String getPublicationYear(Deposit deposit) {
        var date = Datacite.getPublicationDate(deposit);

        if (date == null) {
            return null;
        }

        return date.format(yearFormatter);
    }

    private Resource.Titles getTitles(Deposit deposit) {
        var titles = new Resource.Titles();
        var title = new Resource.Titles.Title();
        title.setValue(Datacite.getTitle(deposit));
        titles.getTitle().add(title);
        return titles;
    }

    private Resource.ResourceType getResourceType() {
        var resourceType = new Resource.ResourceType();
        resourceType.setResourceTypeGeneral(ResourceType.DATASET);
        return resourceType;
    }

    private Resource.Creators getCreators(Deposit deposit) {
        var creators = new Resource.Creators();
        var authors = Datacite.getAuthors(deposit);

        var results = authors.stream().map(a -> {
            var creator = new Resource.Creators.Creator();
            var name = new Resource.Creators.Creator.CreatorName();
            name.setValue(a.getDisplayName());
            creator.setCreatorName(name);

            if (a.getAffiliation() != null) {
                var affiliation = new Affiliation();
                affiliation.setValue("(" + a.getAffiliation() + ")");
                creator.getAffiliation().add(affiliation);
            }

            var nameIdentifier = a.getIdentifier();

            if (nameIdentifier != null) {
                var identifier = new NameIdentifier();
                identifier.setValue(nameIdentifier.getValue());
                identifier.setNameIdentifierScheme(nameIdentifier.getScheme());
                identifier.setSchemeURI(nameIdentifier.getSchemeURI());

                creator.getNameIdentifier().add(identifier);
            }

            return creator;
        }).toList();

        creators.getCreator().addAll(results);

        return creators;
    }

    private Resource.Identifier getIdentifier(Deposit deposit) {
        var id = deposit.getDoi();

        var identifier = new Resource.Identifier();
        identifier.setIdentifierType("DOI");
        identifier.setValue(id == null ? ":none" : id);

        return identifier;
    }

    private Resource.Descriptions getDescriptions(Deposit deposit) {
        var descriptions = new Resource.Descriptions();

        Datacite.getDescriptions(deposit)
            .forEach(item -> {
                var description = new Resource.Descriptions.Description();
                description.setDescriptionType(DescriptionType.ABSTRACT);
                description.getContent().add(item.getValue());
                descriptions.getDescription().add(description);

            });

        return descriptions;
    }

}
