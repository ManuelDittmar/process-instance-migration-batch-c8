package org.camunda.consulting.migration.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
public class BatchResponseDTO {
    private UUID batchId;
    private String status;
    private long targetProcessDefinitionKey;
    private Timestamp creationTimestamp;
    private Map<String, Object> variables;
}
