package org.camunda.consulting.migration.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.camunda.consulting.migration.core.model.ProcessInstanceMigrationStatus;

@Getter
@Setter
@AllArgsConstructor
public class ProcessInstanceMigrationStatusDTO {

    private Long instanceKey;
    private ProcessInstanceMigrationStatus.MigrationStatus migrationStatus;
    private int retry;
    private String errorMessage;

}
