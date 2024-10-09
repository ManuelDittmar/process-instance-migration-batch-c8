package org.camunda.consulting.migration.core.repository;

import org.camunda.consulting.migration.core.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BatchRepository extends JpaRepository<Batch, UUID> {

    Optional<Batch> findTopByStatusOrderByCreationTimestampAsc(Batch.BatchStatus status);
}

