package org.camunda.consulting.migration.core.processor;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.command.ClientStatusException;
import io.camunda.zeebe.client.api.command.MigrateProcessInstanceCommandStep1;
import io.github.resilience4j.retry.Retry;
import io.grpc.Status;
import org.camunda.consulting.migration.core.exception.CamundaResourceNotFoundException;
import org.camunda.consulting.migration.core.exception.RetrieableException;
import org.camunda.consulting.migration.core.model.Batch;
import org.camunda.consulting.migration.core.model.MappingInstruction;
import org.camunda.consulting.migration.core.model.ProcessInstanceMigrationStatus;
import org.camunda.consulting.migration.core.repository.BatchRepository;
import org.camunda.consulting.migration.core.repository.ProcessInstanceMigrationStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(value = "process-instance-migration.enabled", havingValue = "true", matchIfMissing = true)
public class MigrationProcessor {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrationProcessor.class);

    private final BatchRepository batchRepository;
    private final ProcessInstanceMigrationStatusRepository instanceRepository;
    private final ExecutorService executorService;
    private final ZeebeClient zeebeClient;
    private final Retry retry;

    private UUID currentBatchId;
    private AtomicBoolean isCurrentBatchPaused = new AtomicBoolean(false);

    public MigrationProcessor(BatchRepository batchRepository, ProcessInstanceMigrationStatusRepository instanceRepository, ExecutorService executorService, ZeebeClient zeebeClient, Retry retry) {
        this.batchRepository = batchRepository;
        this.instanceRepository = instanceRepository;
        this.executorService = executorService;
        this.zeebeClient = zeebeClient;
        this.retry = retry;
    }

    @EventListener
    public void handleBatchStateChangeEvent(BatchStateChangeEvent event) {
        LOGGER.info("Received event: {} for {}", event.getStatus(), event.getBatchId());
        if (event.getBatchId().equals(currentBatchId)) {
            LOGGER.info("Current batch {} was paused.", currentBatchId);
            isCurrentBatchPaused.set(true);
        }
    }

    @Scheduled(fixedRate = 10000)
    private void processReadyBatches() {
        batchRepository.findTopByStatusOrderByCreationTimestampAsc(Batch.BatchStatus.READY).ifPresent(batch -> {
            currentBatchId = batch.getBatchId();
            List<ProcessInstanceMigrationStatus> availableInstances = instanceRepository.findByBatchAndMigrationStatusIn(batch, Arrays.asList(
                    ProcessInstanceMigrationStatus.MigrationStatus.MIGRATION_READY,
                    ProcessInstanceMigrationStatus.MigrationStatus.MIGRATION_RETRY));

            if (availableInstances.isEmpty()) {
                processFinishedBash(batch);
            } else {
                processAvailableInstances(batch, availableInstances);
            }
        });

        isCurrentBatchPaused.set(false);
    }

    private void processAvailableInstances(Batch batch, List<ProcessInstanceMigrationStatus> availableInstances) {
        LOGGER.info("Processing batch: {}", batch.getBatchId());
        List<CompletableFuture<Void>> futures = availableInstances.stream()
                .map(item -> CompletableFuture.runAsync(() -> processItem(item, batch), executorService))
                .toList();

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allFutures.join();

        LOGGER.info("All items processed for batch: {}", batch.getBatchId());
    }

    private void processFinishedBash(Batch batch) {
        LOGGER.info("No items found for batch: {}", batch.getBatchId());
        if (isBatchMigratedSuccessfully(batch.getBatchId())) {
            batch.setStatus(Batch.BatchStatus.COMPLETED);
        } else {
            batch.setStatus(Batch.BatchStatus.FAILED);
        }

        batchRepository.save(batch);
        currentBatchId = null;
    }

    private void processItem(ProcessInstanceMigrationStatus item, Batch batch) {
        LOGGER.debug("Processing Instance: {} with TargetProcessDefinition {}", item.getInstanceKey(), batch.getTargetProcessDefinitionKey());
        Retry.decorateRunnable(retry, () -> executeMigration(item, batch)).run();
    }

    private void executeMigration(ProcessInstanceMigrationStatus item, Batch batch) {
        if (isCurrentBatchPaused.get()) {
            LOGGER.debug("Batch {} is paused. Skipping instance: {}", currentBatchId, item.getInstanceKey());
            return;
        }

        try {
            if (batch.getVariables() != null) {
                setVariables(item, batch);
            }

            migrateInstance(item, batch);
            item.setMigrationStatus(ProcessInstanceMigrationStatus.MigrationStatus.MIGRATION_COMPLETED);
            instanceRepository.save(item);

        } catch (ClientStatusException e) {
            item.setErrorMessage(e.getStatusCode().name());

            if (e.getStatusCode().equals(Status.Code.RESOURCE_EXHAUSTED)) {
                handleRetriableException(item, e);
            } else {
                handleNonRetriableException(item, e.getMessage());
            }

        } catch (Exception e) {
            item.setErrorMessage(e.getMessage().substring(0, Math.min(e.getMessage().length(), 255)));
            handleRetriableException(item, e);
        }
    }

    private void setVariables(ProcessInstanceMigrationStatus item, Batch batch) {
        zeebeClient.newSetVariablesCommand(item.getInstanceKey())
                .variables(batch.getVariables())
                .send()
                .join();
    }

    private void migrateInstance(ProcessInstanceMigrationStatus item, Batch batch) {
        MigrateProcessInstanceCommandStep1.MigrateProcessInstanceCommandStep2 step = zeebeClient.newMigrateProcessInstanceCommand(item.getInstanceKey())
                .migrationPlan(batch.getTargetProcessDefinitionKey());

        MigrateProcessInstanceCommandStep1.MigrateProcessInstanceCommandFinalStep finalStep = null;
        for (MappingInstruction instruction : batch.getMappingInstructions()) {
            finalStep = step.addMappingInstruction(instruction.getSourceElementId(), instruction.getTargetElementId());
        }

        finalStep.send().join();
    }

    private void handleRetriableException(ProcessInstanceMigrationStatus item, Exception e) {
        if (item.getRetry() <= 0) {
            handleNonRetriableException(item, e.getMessage());
        } else {
            LOGGER.error("[Retries left: {}] - Error migrating instance: {}: {}", item.getRetry(), item.getInstanceKey(), e.getMessage());
            item.setMigrationStatus(ProcessInstanceMigrationStatus.MigrationStatus.MIGRATION_RETRY);
            item.setRetry(item.getRetry() - 1);
            instanceRepository.save(item);
            throw new RetrieableException(e.getMessage());
        }
    }

    private void handleNonRetriableException(ProcessInstanceMigrationStatus item, String e) {
        LOGGER.error("[Cancel] - Error migrating instance: {}: {}", item.getInstanceKey(), e);
        item.setMigrationStatus(ProcessInstanceMigrationStatus.MigrationStatus.MIGRATION_FAILED);
        item.setRetry(0);
        instanceRepository.save(item);
    }


    private boolean isBatchMigratedSuccessfully(UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new CamundaResourceNotFoundException("Batch not found"));

        long countFailed = instanceRepository.countByBatchAndMigrationStatus(batch, ProcessInstanceMigrationStatus.MigrationStatus.MIGRATION_FAILED);

        return countFailed == 0;
    }
}
