package com.voyado.generator.service;

import com.github.javafaker.Faker;
import com.voyado.generator.model.GeneratorStats;
import com.voyado.generator.model.ProductViewEvent;
import com.voyado.generator.model.SearchEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class LoadGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(LoadGeneratorService.class);

    private final WebClient webClient;
    private final Faker faker = new Faker();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong searchEventCount = new AtomicLong(0);
    private final AtomicLong productViewCount = new AtomicLong(0);
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);
    private Instant startTime;

    private final Counter searchEventCounter;
    private final Counter productViewCounter;
    private final Counter successCounter;
    private final Counter failureCounter;

    @Value("${ingester.base-url}")
    private String ingesterBaseUrl;

    @Value("${generator.batch-size:100}")
    private int batchSize;

    @Value("${generator.concurrent-requests:50}")
    private int concurrentRequests;

    public LoadGeneratorService(WebClient.Builder webClientBuilder, MeterRegistry meterRegistry) {
        this.webClient = webClientBuilder.build();
        this.searchEventCounter = Counter.builder("generator.search_events.sent")
                .description("Number of search events sent")
                .register(meterRegistry);
        this.productViewCounter = Counter.builder("generator.product_view_events.sent")
                .description("Number of product view events sent")
                .register(meterRegistry);
        this.successCounter = Counter.builder("generator.requests.success")
                .description("Number of successful requests")
                .register(meterRegistry);
        this.failureCounter = Counter.builder("generator.requests.failed")
                .description("Number of failed requests")
                .register(meterRegistry);
    }

    public void startGeneration(long totalEvents, int searchPercentage) {
        if (running.compareAndSet(false, true)) {
            startTime = Instant.now();
            resetCounters();
            log.info("Starting load generation: {} total events, {}% search events", totalEvents, searchPercentage);

            Flux.range(0, (int) (totalEvents / batchSize))
                    .flatMap(i -> generateAndSendBatch(searchPercentage), concurrentRequests)
                    .doOnComplete(() -> {
                        running.set(false);
                        log.info("Load generation completed. Stats: {}", getStats());
                    })
                    .doOnError(error -> {
                        running.set(false);
                        log.error("Load generation failed", error);
                    })
                    .subscribe();
        } else {
            log.warn("Load generation already running");
        }
    }

    private Mono<Void> generateAndSendBatch(int searchPercentage) {
        List<SearchEvent> searchEvents = new ArrayList<>();
        List<ProductViewEvent> productViewEvents = new ArrayList<>();

        for (int i = 0; i < batchSize; i++) {
            if (faker.random().nextInt(100) < searchPercentage) {
                searchEvents.add(generateSearchEvent());
            } else {
                productViewEvents.add(generateProductViewEvent());
            }
        }

        Mono<Void> searchMono = Mono.empty();
        Mono<Void> productMono = Mono.empty();

        if (!searchEvents.isEmpty()) {
            searchMono = sendSearchEvents(searchEvents);
        }

        if (!productViewEvents.isEmpty()) {
            productMono = sendProductViewEvents(productViewEvents);
        }

        return Mono.when(searchMono, productMono);
    }

    private Mono<Void> sendSearchEvents(List<SearchEvent> events) {
        return webClient.post()
                .uri(ingesterBaseUrl + "/api/v1/events/search/batch")
                .bodyValue(events)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> {
                    searchEventCount.addAndGet(events.size());
                    successCount.incrementAndGet();
                    searchEventCounter.increment(events.size());
                    successCounter.increment();
                })
                .doOnError(error -> {
                    failureCount.incrementAndGet();
                    failureCounter.increment();
                    log.error("Failed to send search events batch", error);
                })
                .onErrorResume(e -> Mono.empty())
                .timeout(Duration.ofSeconds(5));
    }

    private Mono<Void> sendProductViewEvents(List<ProductViewEvent> events) {
        return webClient.post()
                .uri(ingesterBaseUrl + "/api/v1/events/product-view/batch")
                .bodyValue(events)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnSuccess(v -> {
                    productViewCount.addAndGet(events.size());
                    successCount.incrementAndGet();
                    productViewCounter.increment(events.size());
                    successCounter.increment();
                })
                .doOnError(error -> {
                    failureCount.incrementAndGet();
                    failureCounter.increment();
                    log.error("Failed to send product view events batch", error);
                })
                .onErrorResume(e -> Mono.empty())
                .timeout(Duration.ofSeconds(5));
    }

    private SearchEvent generateSearchEvent() {
        return new SearchEvent(
                faker.commerce().productName(), // query
                "user-" + faker.number().numberBetween(1, 10000), // userId
                UUID.randomUUID().toString(), // sessionId
                faker.number().numberBetween(0, 100), // resultCount
                faker.commerce().department(), // category
                Instant.now().toString() // timestamp as ISO-8601 string
        );
    }

    private ProductViewEvent generateProductViewEvent() {
        return new ProductViewEvent(
                "user-" + faker.number().numberBetween(1, 10000),
                "product-" + faker.number().numberBetween(1, 50000),
                System.currentTimeMillis(),
                UUID.randomUUID().toString(),
                Double.parseDouble(faker.commerce().price().replace(",", "")),
                faker.commerce().department()
        );
    }

    public GeneratorStats getStats() {
        long elapsed = startTime != null ? Duration.between(startTime, Instant.now()).toMillis() : 0;
        long total = searchEventCount.get() + productViewCount.get();
        double eventsPerSecond = elapsed > 0 ? (total * 1000.0) / elapsed : 0;

        return new GeneratorStats(
                searchEventCount.get(),
                productViewCount.get(),
                total,
                successCount.get(),
                failureCount.get(),
                eventsPerSecond,
                running.get(),
                elapsed
        );
    }

    public void stopGeneration() {
        running.set(false);
        log.info("Stopping load generation");
    }

    private void resetCounters() {
        searchEventCount.set(0);
        productViewCount.set(0);
        successCount.set(0);
        failureCount.set(0);
    }

    public boolean isRunning() {
        return running.get();
    }
}
