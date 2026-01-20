package com.voyado.ingester.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductViewEvent {
    
    @NotBlank(message = "Product ID is required")
    private String productId;
    
    @NotBlank(message = "User ID is required")
    private String userId;
    
    private String sessionId;
    
    private String productName;
    
    private String category;
    
    private BigDecimal price;
    
    @NotNull(message = "Timestamp is required")
    private Instant timestamp;
}
