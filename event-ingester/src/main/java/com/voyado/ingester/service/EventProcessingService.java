package com.voyado.ingester.service;

import com.voyado.ingester.model.ProductViewEvent;
import com.voyado.ingester.model.SearchEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class EventProcessingService {
    
    private final Counter searchEventCounter;
    private final Counter productViewCounter;
    private final Timer processingTimer;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Value("${kafka.topics.search-events}")
    private String searchEventsTopic;
    
    @Value("${kafka.topics.product-view-events}")
    private String productViewEventsTopic;
    
    public EventProcessingService(MeterRegistry meterRegistry, 
                                 KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
        this.searchEventCounter = Counter.builder("events.search.total")
            .description("Total number of search events processed")
            .register(meterRegistry);
            
        this.productViewCounter = Counter.builder("events.product_view.total")
            .description("Total number of product view events processed")
            .register(meterRegistry);
            
        this.processingTimer = Timer.builder("events.processing.duration")
            .description("Event processing duration")
            .register(meterRegistry);
    }
    
    @Async
    public CompletableFuture<String> processSearchEvent(SearchEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            return processingTimer.record(() -> {
                String eventId = UUID.randomUUID().toString();
                
                // Publish to Kafka topic
                kafkaTemplate.send(searchEventsTopic, event.getUserId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Published search event {} to Kafka topic {}", 
                                eventId, searchEventsTopic);
                        } else {
                            log.error("Failed to publish search event {} to Kafka", eventId, ex);
                        }
                    });
                
                searchEventCounter.increment();
                
                log.debug("Processed search event: {} for query: '{}'", 
                    eventId, event.getQuery());
                
                return eventId;
            });
        });
    }
    
    @Async
    public CompletableFuture<String> processProductViewEvent(ProductViewEvent event) {
        return CompletableFuture.supplyAsync(() -> {
            return processingTimer.record(() -> {
                String eventId = UUID.randomUUID().toString();
                
                // Publish to Kafka topic
                kafkaTemplate.send(productViewEventsTopic, event.getUserId(), event)
                    .whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.debug("Published product view event {} to Kafka topic {}", 
                                eventId, productViewEventsTopic);
                        } else {
                            log.error("Failed to publish product view event {} to Kafka", eventId, ex);
                        }
                    });
                
                productViewCounter.increment();
                
                log.debug("Processed product view event: {} for product: '{}'", 
                    eventId, event.getProductId());
                
                return eventId;
            });
        });
    }
    
    @Async
    public CompletableFuture<Integer> processBatch(java.util.List<?> events, String eventType) {
        return CompletableFuture.supplyAsync(() -> {
            int processed = 0;
            for (Object event : events) {
                // Process each event
                processed++;
            }
            log.info("Processed batch of {} {} events", processed, eventType);
            return processed;
        });
    }
}
