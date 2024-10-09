package org.camunda.consulting.migration.core.service;

import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.model.ProcessInstance;
import io.camunda.operate.model.SearchResult;
import io.camunda.operate.search.ProcessInstanceFilter;
import io.camunda.operate.search.SearchQuery;
import org.camunda.consulting.migration.api.dto.BatchMigrationRequestDTO;
import org.camunda.consulting.migration.api.dto.BatchMigrationResponseDTO;
import org.camunda.consulting.migration.core.exception.CamundaResourceNotFoundException;
import org.camunda.consulting.migration.core.model.Batch;
import org.camunda.consulting.migration.core.model.ProcessInstanceMigrationStatus;
import org.camunda.consulting.migration.core.repository.BatchRepository;
import org.camunda.consulting.migration.core.repository.ProcessInstanceMigrationStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class InstanceFetcherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceFetcherService.class);

    private final ProcessInstanceMigrationStatusRepository instanceRepository;
    private final CamundaOperateClient operateClient;
    private final BatchRepository batchRepository;

    @Value("${process-instance-migration.retry-limit:3}")
    private int defaultRetryLimit;

    public InstanceFetcherService(ProcessInstanceMigrationStatusRepository instanceRepository, CamundaOperateClient operateClient, BatchRepository batchRepository) {
        this.instanceRepository = instanceRepository;
        this.operateClient = operateClient;
        this.batchRepository = batchRepository;
    }

    @Transactional
    public BatchMigrationResponseDTO submitBatch(BatchMigrationRequestDTO request) {
        Batch batch = new Batch();
        batch.setMappingInstructions(request.getMappingInstructions());
        batch.setTargetProcessDefinitionKey(request.getTargetProcessDefinitionKey());
        batch.setVariables(request.getVariables());
        if (request.isCreatePaused()) {
            batch.setStatus(Batch.BatchStatus.PAUSED);
        }
        batchRepository.save(batch);
        fetchAndStoreProcessInstances(batch, request.getProcessInstanceFilter(), null, 0);
        long itemCount = instanceRepository.countByBatch(batch);
        LOGGER.debug("Batch {} created with {} process instances", batch.getBatchId(), itemCount);
        return BatchMigrationResponseDTO.builder()
                .batch(batch)
                .processInstanceCount(itemCount)
                .build();
    }

    private void fetchAndStoreProcessInstances(Batch batch, ProcessInstanceFilter processInstanceFilter, List<Object> sortValues, int cumulativeFetchedCount) {
        SearchQuery searchQuery = new SearchQuery();
        searchQuery.setFilter(processInstanceFilter);

        if (sortValues != null) {
            searchQuery.setSearchAfter(sortValues);
        }

        SearchResult<ProcessInstance> searchResult;
        try {
            searchResult = operateClient.searchProcessInstanceResults(searchQuery);
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred while fetching process instances", e);
        }

        if (searchResult.getTotal() == 0) {
            throw new CamundaResourceNotFoundException("No process instances found for the given filter");
        }

        List<ProcessInstanceMigrationStatus> processInstanceEntities = searchResult.getItems().stream()
                .map(instance -> new ProcessInstanceMigrationStatus(
                        instance.getKey(),
                        batch,
                        ProcessInstanceMigrationStatus.MigrationStatus.MIGRATION_READY,
                        defaultRetryLimit
                ))
                .collect(Collectors.toList());

        instanceRepository.saveAll(processInstanceEntities);

        cumulativeFetchedCount += processInstanceEntities.size();

        if (searchResult.getTotal() > cumulativeFetchedCount) {
            LOGGER.debug("Total process instances: {}, Fetched process instances: {}", searchResult.getTotal(), cumulativeFetchedCount);
            fetchAndStoreProcessInstances(batch, processInstanceFilter, searchResult.getSortValues(), cumulativeFetchedCount);
        }
    }


}
