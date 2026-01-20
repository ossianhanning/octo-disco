package com.voyado.consumer.service;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseCredentials;
import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseRequest;
import com.clickhouse.client.ClickHouseResponse;
import com.voyado.consumer.model.ProductViewEvent;
import com.voyado.consumer.model.SearchEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
public class ClickHouseService {

    @Value("${clickhouse.host}")
    private String clickhouseHost;

    @Value("${clickhouse.port}")
    private int clickhousePort;

    @Value("${clickhouse.database}")
    private String database;

    @Value("${clickhouse.user:default}")
    private String user;

    @Value("${clickhouse.password:}")
    private String password;

    private ClickHouseNode server;
    private final Counter searchEventCounter;
    private final Counter productViewCounter;
    private final Timer searchEventTimer;
    private final Timer productViewTimer;

    public ClickHouseService(MeterRegistry meterRegistry) {
        this.searchEventCounter = Counter.builder("clickhouse.search_events.inserted")
                .description("Number of search events inserted into ClickHouse")
                .register(meterRegistry);
        this.productViewCounter = Counter.builder("clickhouse.product_view_events.inserted")
                .description("Number of product view events inserted into ClickHouse")
                .register(meterRegistry);
        this.searchEventTimer = Timer.builder("clickhouse.search_events.insert.time")
                .description("Time to insert search events into ClickHouse")
                .register(meterRegistry);
        this.productViewTimer = Timer.builder("clickhouse.product_view_events.insert.time")
                .description("Time to insert product view events into ClickHouse")
                .register(meterRegistry);
    }

    @PostConstruct
    public void init() {
        log.info("Connecting to ClickHouse with user: '{}', password length: {}", user, password != null ? password.length() : 0);
        
        String connectionUrl = String.format("http://%s:%s@%s:%d/%s", user, password, clickhouseHost, clickhousePort, database);
        server = ClickHouseNode.of(connectionUrl);
        
        log.info("ClickHouse connection initialized: {} as user {}", connectionUrl.replaceAll(":.*@", ":***@"), user);
        
        createTablesIfNotExist();
    }

    private void createTablesIfNotExist() {
        try {
            ClickHouseClient client = ClickHouseClient.newInstance();
            String connectionUrl = String.format("http://%s:%s@%s:%d/%s", user, password, clickhouseHost, clickhousePort, database);
            ClickHouseNode authServer = ClickHouseNode.of(connectionUrl);
            
            ClickHouseRequest<?> testRequest = client.read(authServer).query("SELECT 1");
            try (ClickHouseResponse testResponse = testRequest.executeAndWait()) {
                log.info("ClickHouse connection test successful");
            }
            
            String createSearchEventsTable = """
                CREATE TABLE IF NOT EXISTS search_events (
                    user_id String,
                    search_query String,
                    timestamp DateTime64(3),
                    session_id String,
                    results_count Int32,
                    ingested_at DateTime DEFAULT now()
                ) ENGINE = MergeTree()
                ORDER BY (timestamp, user_id)
                PARTITION BY toYYYYMM(timestamp)
                """;

            ClickHouseRequest<?> request = client.read(authServer).query(createSearchEventsTable);
            try (ClickHouseResponse response = request.executeAndWait()) {
                log.info("Search events table ensured");
            }

            String createProductViewTable = """
                CREATE TABLE IF NOT EXISTS product_view_events (
                    user_id String,
                    product_id String,
                    timestamp DateTime64(3),
                    session_id String,
                    price Float64,
                    category String,
                    ingested_at DateTime DEFAULT now()
                ) ENGINE = MergeTree()
                ORDER BY (timestamp, user_id)
                PARTITION BY toYYYYMM(timestamp)
                """;

            request = client.read(authServer).query(createProductViewTable);
            try (ClickHouseResponse response = request.executeAndWait()) {
                log.info("Product view events table ensured");
            }
            
            client.close();

        } catch (ClickHouseException e) {
            log.error("Failed to create tables - ClickHouse error: {}", e.getMessage(), e);
            throw new RuntimeException("ClickHouse connection failed during startup", e);
        } catch (Exception e) {
            log.error("Unexpected error during table creation: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to initialize ClickHouse tables", e);
        }
    }

    public void insertSearchEvent(SearchEvent event) {
        searchEventTimer.record(() -> {
            try {
                ClickHouseClient client = ClickHouseClient.newInstance();
                String connectionUrl = String.format("http://%s:%s@%s:%d/%s", user, password, clickhouseHost, clickhousePort, database);
                ClickHouseNode authServer = ClickHouseNode.of(connectionUrl);
                
                long epochSeconds = event.getTimestamp().getEpochSecond();
                
                String query = String.format(
                    "INSERT INTO search_events (user_id, search_query, timestamp, session_id, results_count) VALUES ('%s', '%s', toDateTime64(%d, 3), '%s', %d)",
                    escapeSql(event.getUserId()),
                    escapeSql(event.getQuery()),
                    epochSeconds,
                    escapeSql(event.getSessionId()),
                    event.getResultCount() != null ? event.getResultCount() : 0
                );

                log.debug("Executing ClickHouse query: {}", query);
                ClickHouseRequest<?> request = client.read(authServer).query(query);
                try (ClickHouseResponse response = request.executeAndWait()) {
                    searchEventCounter.increment();
                    log.debug("Inserted search event for user: {}", event.getUserId());
                }
                client.close();
            } catch (ClickHouseException e) {
                log.error("Failed to insert search event for user {}: {}", event.getUserId(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error inserting search event for user {}: {}", event.getUserId(), e.getMessage(), e);
            }
        });
    }

    public void insertProductViewEvent(ProductViewEvent event) {
        productViewTimer.record(() -> {
            try {
                ClickHouseClient client = ClickHouseClient.newInstance();
                String connectionUrl = String.format("http://%s:%s@%s:%d/%s", user, password, clickhouseHost, clickhousePort, database);
                ClickHouseNode authServer = ClickHouseNode.of(connectionUrl);
                
                long epochSeconds = event.getTimestamp().getEpochSecond();
                
                String query = String.format(
                    "INSERT INTO product_view_events (user_id, product_id, timestamp, session_id, price, category) VALUES ('%s', '%s', toDateTime64(%d, 3), '%s', %.2f, '%s')",
                    escapeSql(event.getUserId()),
                    escapeSql(event.getProductId()),
                    epochSeconds,
                    escapeSql(event.getSessionId()),
                    event.getPrice() != null ? event.getPrice().doubleValue() : 0.0,
                    escapeSql(event.getCategory())
                );

                log.debug("Executing ClickHouse query: {}", query);
                ClickHouseRequest<?> request = client.read(authServer).query(query);
                try (ClickHouseResponse response = request.executeAndWait()) {
                    productViewCounter.increment();
                    log.debug("Inserted product view event for user: {}", event.getUserId());
                }
                client.close();
            } catch (ClickHouseException e) {
                log.error("Failed to insert product view event for user {}: {}", event.getUserId(), e.getMessage(), e);
            } catch (Exception e) {
                log.error("Unexpected error inserting product view event for user {}: {}", event.getUserId(), e.getMessage(), e);
            }
        });
    }

    private String escapeSql(String value) {
        if (value == null) return "";
        return value.replace("'", "\\'");
    }
}
