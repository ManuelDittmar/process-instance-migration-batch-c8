package org.camunda.consulting.migration.core.repository;

import org.camunda.consulting.migration.core.model.Batch;
import org.camunda.consulting.migration.core.model.ProcessInstanceMigrationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessInstanceMigrationStatusRepository extends JpaRepository<ProcessInstanceMigrationStatus, Long>, JpaSpecificationExecutor<ProcessInstanceMigrationStatus> {

    long countByBatch(Batch batch);

    long countByBatchAndMigrationStatus(Batch batch, ProcessInstanceMigrationStatus.MigrationStatus status);

    List<ProcessInstanceMigrationStatus> findByBatch(Batch batch);

    List<ProcessInstanceMigrationStatus> findByBatchAndMigrationStatusIn(Batch batch, List<ProcessInstanceMigrationStatus.MigrationStatus> statuses);
}

