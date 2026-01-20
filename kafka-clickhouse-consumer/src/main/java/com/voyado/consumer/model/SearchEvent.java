package com.voyado.consumer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchEvent {
    private String query;
    private String userId;
    private String sessionId;
    private Integer resultCount;
    private String category;
    private Instant timestamp;
}
