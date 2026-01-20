package com.voyado.ingester.controller;

import com.voyado.ingester.model.EventResponse;
import com.voyado.ingester.model.SearchEvent;
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
@RequestMapping("/api/v1/events/search")
@RequiredArgsConstructor
@Slf4j
@Validated
public class SearchEventController {
    
    private final EventProcessingService processingService;
    
    @PostMapping
    @Timed(value = "api.search.event.single", description = "Time to process single search event")
    public CompletableFuture<ResponseEntity<EventResponse>> ingestSearchEvent(
            @Valid @RequestBody SearchEvent event) {
        
        log.info("Received search event for query: '{}'", event.getQuery());
        
        return processingService.processSearchEvent(event)
            .thenApply(eventId -> ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(EventResponse.success(eventId)))
            .exceptionally(ex -> {
                log.error("Error processing search event", ex);
                return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(EventResponse.error("Failed to process event: " + ex.getMessage()));
            });
    }
    
    @PostMapping("/batch")
    @Timed(value = "api.search.event.batch", description = "Time to process batch of search events")
    public CompletableFuture<ResponseEntity<BatchResponse>> ingestSearchEventBatch(
            @Valid @RequestBody List<SearchEvent> events) {
        
        log.info("Received batch of {} search events", events.size());
        
        List<CompletableFuture<String>> futures = events.stream()
            .map(processingService::processSearchEvent)
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
        return ResponseEntity.ok("Search event ingester is healthy");
    }
    
    public record BatchResponse(int accepted, int failed, int total) {}
}
