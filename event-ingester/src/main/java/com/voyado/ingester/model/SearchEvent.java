package com.voyado.ingester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchEvent {
    
    @NotBlank(message = "Search query is required")
    private String query;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    private String sessionId;
    
    private Integer resultCount;
    
    private String category;
    
    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
}
