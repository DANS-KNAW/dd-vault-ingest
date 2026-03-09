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

import org.datacite.schema.kernel_4.Resource;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import java.io.StringWriter;

public class DataciteSerializer {
    public String serialize(Resource resource) {
        try {
            var context = JAXBContext.newInstance(Resource.class);
            var marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            var writer = new StringWriter();
            marshaller.marshal(resource, writer);
            return writer.toString();
        }
        catch (Exception e) {
            throw new RuntimeException(String.format("Error converting resource to XML string: %s", e.getMessage()), e);
        }
    }
}
