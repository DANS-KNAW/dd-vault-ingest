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
package nl.knaw.dans.vaultingest.client;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.validatedansbag.client.api.ValidateCommandDto;
import nl.knaw.dans.validatedansbag.client.api.ValidateOkDto;
import nl.knaw.dans.validatedansbag.client.resources.DefaultApi;
import nl.knaw.dans.validatedansbag.invoker.ApiException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
public abstract class AbstractBagValidator implements BagValidator {
    private final DefaultApi api;

    @Override
    public void validate(UUID depositId, Path bagDir) throws InvalidDepositException, IOException {
        if (bagDir == null) {
            throw new InvalidDepositException("Bag directory cannot be null");
        }

        var command = new ValidateCommandDto()
            .bagLocation(bagDir.toString())
            .packageType(getPackageType());

        try {
            log.debug("[{}] Validating bag {} with command {}", depositId, bagDir, command);
            var result = api.validateLocalDirPost(command);
            if (Boolean.FALSE.equals(result.getIsCompliant())) {
                throw formatValidationError(result);
            }
            log.debug("[{}] Bag is compliant", depositId);
        }
        catch (ApiException e) {
            throw new RuntimeException("Could not validate bag", e);
        }

    }

    private InvalidDepositException formatValidationError(ValidateOkDto result) {
        if (result.getRuleViolations() == null) {
            return new InvalidDepositException("Bag was not valid according to Profile Version " + result.getProfileVersion() + ", but no violations were reported");
        }

        var violations = result.getRuleViolations().stream()
            .map(r -> String.format("- [%s] %s", r.getRule(), r.getViolation()))
            .collect(Collectors.joining("\n"));

        return new InvalidDepositException(String.format(
            "Bag was not valid according to Profile Version %s. Violations: \n%s",
            result.getProfileVersion(), violations)
        );
    }

    protected abstract ValidateCommandDto.PackageTypeEnum getPackageType();
}
