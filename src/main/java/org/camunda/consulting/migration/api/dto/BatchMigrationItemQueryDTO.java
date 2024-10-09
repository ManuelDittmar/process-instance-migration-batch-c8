package org.camunda.consulting.migration.api.dto;

import lombok.Getter;
import lombok.Setter;
import org.camunda.consulting.migration.core.model.ProcessInstanceMigrationStatus.MigrationStatus;

@Getter
@Setter
public class BatchMigrationItemQueryDTO {

    private MigrationStatus migrationStatus;
    private Integer retry;
    private String errorMessage;

    private int page = 0;
    private int size = 20;

}
