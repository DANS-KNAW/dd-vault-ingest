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

import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import nl.knaw.dans.vaultingest.core.xml.XPathEvaluator;
import nl.knaw.dans.vaultingest.core.xml.XmlNamespaces;
import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Builder
@ToString
@Getter
public class Deposit {
    public enum State {
        PUBLISHED,
        ACCEPTED,
        REJECTED,
        FAILED,
        DRAFT,
        FINALIZING,
        INVALID,
        SUBMITTED,
        UPLOADED
    }

    private final String id;
    private final Document ddm;
    private final Document filesXml;
    private final List<PayloadFile> payloadFiles;
    private final Path path;
    private final DepositProperties properties;
    private final DepositBag bag;
    private final boolean migration;
    @Setter
    private String nbn;
    @Setter
    private Long objectVersion;
    @Setter
    private String dataSupplier;

    public Set<SupportedAlgorithm> getPayloadManifestAlgorithms() {
        return getBag().getPayloadManifestAlgorithms();
    }

    public State getState() {
        return State.valueOf(properties.getStateLabel());
    }

    public String getStateDescription() {
        return properties.getStateDescription();
    }

    public List<String> getMetadataValue(String key) {
        return bag.getBagInfoValue(key);
    }

    public boolean isUpdate() {
        return !bag.getBagInfoValue("Is-Version-Of").isEmpty();
    }

    public String getIsVersionOf() {
        return bag.getBagInfoValue("Is-Version-Of").stream().findFirst().orElse(null);
    }

    public String getSwordToken() {
        return properties.getSwordToken();
    }

    public String getDepositorId() {
        return properties.getDepositorId();
    }

    public void setState(State state, String message) {
        properties.setStateLabel(state.name());
        properties.setStateDescription(message);
    }

    public String getBagId() {
        return properties.getBagId();
    }

    public String getDoi() {
        var prefix = ddm.lookupPrefix(XmlNamespaces.NAMESPACE_ID_TYPE);
        var expr = new String[] {
            String.format("/ddm:DDM/ddm:dcmiMetadata/dcterms:identifier[@xsi:type='%s:DOI']", prefix),
            String.format("/ddm:DDM/ddm:dcmiMetadata/dc:identifier[@xsi:type='%s:DOI']", prefix)
        };

        var dois = XPathEvaluator.strings(ddm, expr).toList();

        if (dois.size() != 1) {
            throw new IllegalStateException("There should be exactly one DOI in the DDM, but found " + dois.size() + " DOIs");
        }

        var doi = dois.get(0);

        if (StringUtils.isBlank(doi)) {
            throw new IllegalStateException("DOI is blank in the DDM");
        }

        return doi;
    }

    public Collection<Path> getMetadataFiles() throws IOException {
        return bag.getMetadataFiles();
    }

    public InputStream inputStreamForBagFile(Path path) {
        return bag.inputStreamForBagFile(path);
    }

    public Path getBagDir() {
        return bag.getBagDir();
    }
}