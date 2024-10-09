package org.camunda.consulting.migration.api.dto;

import lombok.*;
import org.camunda.consulting.migration.core.model.Batch;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchMigrationResponseDTO {

    private Batch batch;
    private long processInstanceCount;

}
