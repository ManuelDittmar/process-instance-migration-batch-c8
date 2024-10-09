package org.camunda.consulting.migration.api.dto;


import lombok.*;

import java.util.Map;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchStatusResponseDTO {

    BatchResponseDTO batch;
    Map<String, Long> countByStatus;

}
