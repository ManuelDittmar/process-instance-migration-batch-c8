package org.camunda.consulting.migration.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.camunda.consulting.migration.api.dto.*;
import org.camunda.consulting.migration.core.service.BatchManagementService;
import org.camunda.consulting.migration.core.service.InstanceFetcherService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("api/v1/batches")
@Tag(name = "Batch Migration", description = "Operations related to batch migrations")
public class BatchController {

    private final BatchManagementService batchManagementService;
    private final InstanceFetcherService instanceFetcherService;

    public BatchController(BatchManagementService batchManagementService, InstanceFetcherService instanceFetcherService) {
        this.batchManagementService = batchManagementService;
        this.instanceFetcherService = instanceFetcherService;
    }

    @PostMapping
    @Operation(summary = "Create a batch migration", description = "Creates a new batch migration based on the provided request")
    public ResponseEntity<BatchMigrationResponseDTO> createBatchMigration(@RequestBody BatchMigrationRequestDTO request) {
        BatchMigrationResponseDTO createdBatch = instanceFetcherService.submitBatch(request);
        return ResponseEntity.ok(createdBatch);

    }

    @GetMapping("/{batchId}")
    @Operation(summary = "Get batch migration status", description = "Gets the status of the batch migration by batch ID")
    public ResponseEntity<BatchStatusResponseDTO> getBatchMigrationStatus(@PathVariable UUID batchId) {
        BatchStatusResponseDTO batchStatus = batchManagementService.getBatchStatus(batchId);
        return ResponseEntity.ok(batchStatus);
    }

    @DeleteMapping("/{batchId}")
    @Operation(summary = "Delete batch migration", description = "Deletes the batch migration and its associated entities by batch ID")
    public ResponseEntity<Void> deleteBatchMigration(@PathVariable UUID batchId) {
        batchManagementService.deleteBatch(batchId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{batchId}/pause")
    @Operation(summary = "Pause batch migration", description = "Pauses the batch migration by batch ID")
    public ResponseEntity<Void> pauseBatchMigration(@PathVariable UUID batchId) {
        batchManagementService.pauseBatch(batchId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{batchId}/unpause")
    @Operation(summary = "Unpause batch migration", description = "Unpauses the batch migration by batch ID")
    public ResponseEntity<Void> unpauseBatchMigration(@PathVariable UUID batchId) {
        batchManagementService.unpauseBatch(batchId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{batchId}/items")
    @Operation(summary = "Get batch migration items", description = "Retrieves the items of a batch migration based on batch ID and query parameters")
    public ResponseEntity<Page<ProcessInstanceMigrationStatusDTO>> getBatchMigrationItems(
            @PathVariable UUID batchId,
            @RequestBody BatchMigrationItemQueryDTO queryDTO) {
        Page<ProcessInstanceMigrationStatusDTO> instancesPage = batchManagementService.getBatchMigrationItems(batchId, queryDTO);
        return ResponseEntity.ok(instancesPage);
    }
}

