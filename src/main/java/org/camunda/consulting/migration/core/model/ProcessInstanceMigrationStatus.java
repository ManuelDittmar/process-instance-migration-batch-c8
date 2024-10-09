package org.camunda.consulting.migration.core.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class ProcessInstanceMigrationStatus {

    @Id
    @Column(unique = true, nullable = false)
    private Long instanceKey;

    @ManyToOne
    @JoinColumn(name = "batch_id", nullable = false)
    private Batch batch;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MigrationStatus migrationStatus;

    @Column(nullable = false)
    private int retry;

    @Column
    private String errorMessage;

    public ProcessInstanceMigrationStatus(Long instanceKey, Batch batch, MigrationStatus migrationStatus, int retry) {
        this.instanceKey = instanceKey;
        this.batch = batch;
        this.migrationStatus = migrationStatus;
        this.retry = retry;
    }

    public enum MigrationStatus {
        MIGRATION_COMPLETED,
        MIGRATION_FAILED,
        MIGRATION_RETRY,
        MIGRATION_READY
    }
}


