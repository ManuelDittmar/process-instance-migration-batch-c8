package org.camunda.consulting.migration.api.dto;

import io.camunda.operate.search.ProcessInstanceFilter;
import lombok.Getter;
import lombok.Setter;
import org.camunda.consulting.migration.core.model.MappingInstruction;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public class BatchMigrationRequestDTO {

    private ProcessInstanceFilter processInstanceFilter;
    private long targetProcessDefinitionKey;
    private List<MappingInstruction> mappingInstructions;
    private Map<String, Object> variables;
    private boolean createPaused;

}
