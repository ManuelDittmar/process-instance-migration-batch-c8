package org.camunda.consulting.migration.core.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Cascade;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Getter
@Setter
public class Batch {

    @Id
    @GeneratedValue
    private UUID batchId;

    @Column(nullable = false)
    private Timestamp creationTimestamp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BatchStatus status;

    @Column(nullable = false)
    private long targetProcessDefinitionKey;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProcessInstanceMigrationStatus> migrationStatuses;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "mapping_instruction", joinColumns = @JoinColumn(name = "batch_id"))
    @Cascade(org.hibernate.annotations.CascadeType.ALL)
    private List<MappingInstruction> mappingInstructions;

    @Lob
    @Column(columnDefinition = "TEXT")
    @JsonIgnore
    private String variablesJson;

    @Transient
    private Map<String, Object> variables;

    public Batch() {
        this.creationTimestamp = new Timestamp(System.currentTimeMillis());
        this.status = BatchStatus.READY;
    }

    public Map<String, Object> getVariables() {
        if (variablesJson != null) {
            ObjectMapper objectMapper = new ObjectMapper();
            try {
                return objectMapper.readValue(variablesJson, Map.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to deserialize variables from JSON", e);
            }
        }
        return null;
    }

    public void setVariables(Map<String, Object> variables) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            this.variablesJson = objectMapper.writeValueAsString(variables);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize variables to JSON", e);
        }
        this.variables = variables;
    }

    public enum BatchStatus {
        READY,
        PAUSED,
        COMPLETED,
        FAILED
    }
}
