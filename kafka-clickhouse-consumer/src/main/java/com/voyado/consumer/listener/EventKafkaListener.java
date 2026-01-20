package com.voyado.consumer.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.voyado.consumer.model.ProductViewEvent;
import com.voyado.consumer.model.SearchEvent;
import com.voyado.consumer.service.ClickHouseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventKafkaListener {

    private final ClickHouseService clickHouseService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${kafka.topics.search-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeSearchEvent(String message) {
        try {
            SearchEvent event = objectMapper.readValue(message, SearchEvent.class);
            log.debug("Consumed search event: {}", event.getUserId());
            clickHouseService.insertSearchEvent(event);
        } catch (Exception e) {
            log.error("Failed to process search event: {}", message, e);
        }
    }

    @KafkaListener(topics = "${kafka.topics.product-view-events}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeProductViewEvent(String message) {
        try {
            ProductViewEvent event = objectMapper.readValue(message, ProductViewEvent.class);
            log.debug("Consumed product view event: {}", event.getUserId());
            clickHouseService.insertProductViewEvent(event);
        } catch (Exception e) {
            log.error("Failed to process product view event: {}", message, e);
        }
    }
}
