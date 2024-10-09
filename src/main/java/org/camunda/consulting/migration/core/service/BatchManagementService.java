package org.camunda.consulting.migration.core.service;

import org.camunda.consulting.migration.api.dto.BatchMigrationItemQueryDTO;
import org.camunda.consulting.migration.api.dto.BatchResponseDTO;
import org.camunda.consulting.migration.api.dto.BatchStatusResponseDTO;
import org.camunda.consulting.migration.api.dto.ProcessInstanceMigrationStatusDTO;
import org.camunda.consulting.migration.core.exception.CamundaResourceNotFoundException;
import org.camunda.consulting.migration.core.exception.NoInstanceFoundException;
import org.camunda.consulting.migration.core.model.Batch;
import org.camunda.consulting.migration.core.model.ProcessInstanceMigrationStatus;
import org.camunda.consulting.migration.core.processor.BatchStateChangeEvent;
import org.camunda.consulting.migration.core.repository.BatchRepository;
import org.camunda.consulting.migration.core.repository.ProcessInstanceMigrationStatusRepository;
import org.camunda.consulting.migration.core.repository.ProcessInstanceMigrationStatusSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class BatchManagementService {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchManagementService.class);

    private final ProcessInstanceMigrationStatusRepository instanceRepository;
    private final BatchRepository batchRepository;
    private final ApplicationEventPublisher eventPublisher;

    public BatchManagementService(ProcessInstanceMigrationStatusRepository instanceRepository, BatchRepository batchRepository, ApplicationEventPublisher eventPublisher) {
        this.instanceRepository = instanceRepository;
        this.batchRepository = batchRepository;
        this.eventPublisher = eventPublisher;
    }

    public BatchStatusResponseDTO getBatchStatus(UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new CamundaResourceNotFoundException("Batch not found"));

        Map<String, Long> countPerStatus = new HashMap<>();

        for (ProcessInstanceMigrationStatus.MigrationStatus status : ProcessInstanceMigrationStatus.MigrationStatus.values()) {
            long count = instanceRepository.countByBatchAndMigrationStatus(batch, status);
            countPerStatus.put(status.name(), count);
        }

        BatchResponseDTO batchResponseDTO = new BatchResponseDTO(
                batch.getBatchId(),
                batch.getStatus().name(),
                batch.getTargetProcessDefinitionKey(),
                batch.getCreationTimestamp(),
                batch.getVariables()
        );

        return new BatchStatusResponseDTO(batchResponseDTO, countPerStatus);
    }

    public void pauseBatch(UUID batchId) throws NoInstanceFoundException {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NoInstanceFoundException("Batch not found"));

        if (batch.getStatus() != Batch.BatchStatus.READY) {
            throw new IllegalStateException("Batch is not in READY state");
        }
        batch.setStatus(Batch.BatchStatus.PAUSED);
        batchRepository.save(batch);
        eventPublisher.publishEvent(new BatchStateChangeEvent(this, batch.getBatchId(), Batch.BatchStatus.PAUSED));
    }

    public void unpauseBatch(UUID batchId) throws NoInstanceFoundException {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new NoInstanceFoundException("Batch not found"));

        if (batch.getStatus() != Batch.BatchStatus.PAUSED) {
            throw new IllegalStateException("Batch is not in PAUSED state");
        }

        batch.setStatus(Batch.BatchStatus.READY);
        batchRepository.save(batch);
        eventPublisher.publishEvent(new BatchStateChangeEvent(this, batch.getBatchId(), Batch.BatchStatus.READY));
    }

    public void deleteBatch(UUID batchId) throws NoInstanceFoundException {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new CamundaResourceNotFoundException("Batch not found"));
        batchRepository.delete(batch);
        LOGGER.info("Batch {} deleted", batchId);
    }

    public Page<ProcessInstanceMigrationStatusDTO> getBatchMigrationItems(UUID batchId, BatchMigrationItemQueryDTO queryDTO) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new CamundaResourceNotFoundException("Batch not found"));

        Pageable pageable = PageRequest.of(queryDTO.getPage(), queryDTO.getSize());

        Specification<ProcessInstanceMigrationStatus> specification = Specification
                .where(ProcessInstanceMigrationStatusSpecification.hasBatch(batch));

        if (queryDTO.getMigrationStatus() != null) {
            specification = specification.and(ProcessInstanceMigrationStatusSpecification.hasMigrationStatus(queryDTO.getMigrationStatus()));
        }

        if (queryDTO.getRetry() != null) {
            specification = specification.and(ProcessInstanceMigrationStatusSpecification.hasRetry(queryDTO.getRetry()));
        }

        if (queryDTO.getErrorMessage() != null) {
            specification = specification.and(ProcessInstanceMigrationStatusSpecification.hasErrorMessageContaining(queryDTO.getErrorMessage()));
        }

        return instanceRepository.findAll(specification, pageable)
                .map(instance -> new ProcessInstanceMigrationStatusDTO(
                        instance.getInstanceKey(),
                        instance.getMigrationStatus(),
                        instance.getRetry(),
                        instance.getErrorMessage()
                ));
    }

}
