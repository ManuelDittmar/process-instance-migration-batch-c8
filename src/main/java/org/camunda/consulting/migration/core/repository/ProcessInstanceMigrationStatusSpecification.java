package org.camunda.consulting.migration.core.repository;

import org.camunda.consulting.migration.core.model.Batch;
import org.camunda.consulting.migration.core.model.ProcessInstanceMigrationStatus;
import org.springframework.data.jpa.domain.Specification;

public class ProcessInstanceMigrationStatusSpecification {

    public static Specification<ProcessInstanceMigrationStatus> hasBatch(Batch batch) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("batch"), batch);
    }

    public static Specification<ProcessInstanceMigrationStatus> hasMigrationStatus(ProcessInstanceMigrationStatus.MigrationStatus status) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("migrationStatus"), status);
    }

    public static Specification<ProcessInstanceMigrationStatus> hasRetry(int retry) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(root.get("retry"), retry);
    }

    public static Specification<ProcessInstanceMigrationStatus> hasErrorMessageContaining(String errorMessage) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(root.get("errorMessage"), "%" + errorMessage + "%");
    }
}
