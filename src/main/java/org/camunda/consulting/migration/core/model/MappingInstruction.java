package org.camunda.consulting.migration.core.model;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class MappingInstruction {

    private String sourceElementId;
    private String targetElementId;

    public MappingInstruction(String sourceElementId, String targetElementId) {
        this.sourceElementId = sourceElementId;
        this.targetElementId = targetElementId;
    }
}

