package com.voyado.ingester.controller;

import com.voyado.ingester.model.EventResponse;
import com.voyado.ingester.model.ProductViewEvent;
import com.voyado.ingester.service.EventProcessingService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/events/product-view")
@RequiredArgsConstructor
@Slf4j
@Validated
public class ProductViewController {
    
    private final EventProcessingService processingService;
    
    @PostMapping
    @Timed(value = "api.product_view.event.single", description = "Time to process single product view event")
    public CompletableFuture<ResponseEntity<EventResponse>> ingestProductViewEvent(
            @Valid @RequestBody ProductViewEvent event) {
        
        log.info("Received product view event for product: '{}'", event.getProductId());
        
        return processingService.processProductViewEvent(event)
            .thenApply(eventId -> ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(EventResponse.success(eventId)))
            .exceptionally(ex -> {
                log.error("Error processing product view event", ex);
                return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EventResponse.error("Failed to process event: " + ex.getMessage()));
            });
    }
    
    @PostMapping("/batch")
    @Timed(value = "api.product_view.event.batch", description = "Time to process batch of product view events")
    public CompletableFuture<ResponseEntity<BatchResponse>> ingestProductViewEventBatch(
            @Valid @RequestBody List<ProductViewEvent> events) {
        
        log.info("Received batch of {} product view events", events.size());
        
        List<CompletableFuture<String>> futures = events.stream()
            .map(processingService::processProductViewEvent)
            .toList();
        
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                long successCount = futures.stream()
                    .filter(f -> !f.isCompletedExceptionally())
                    .count();
                    
                BatchResponse response = new BatchResponse(
                    (int) successCount,
                    events.size() - (int) successCount,
                    events.size()
                );
                
                return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(response);
            })
            .exceptionally(ex -> {
                log.error("Error processing batch", ex);
                return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new BatchResponse(0, events.size(), events.size()));
            });
    }
    
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Product view event ingester is healthy");
    }
    
    public record BatchResponse(int accepted, int failed, int total) {}
}
