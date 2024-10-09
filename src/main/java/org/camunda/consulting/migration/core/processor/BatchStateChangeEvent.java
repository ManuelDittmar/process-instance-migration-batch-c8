package org.camunda.consulting.migration.core.processor;

import lombok.Getter;
import lombok.Setter;
import org.camunda.consulting.migration.core.model.Batch;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

@Getter
@Setter
public class BatchStateChangeEvent extends ApplicationEvent {
    private final UUID batchId;
    private final Batch.BatchStatus status;

    public BatchStateChangeEvent(Object source, UUID batchId, Batch.BatchStatus status) {
        super(source);
        this.batchId = batchId;
        this.status = status;
    }
}

