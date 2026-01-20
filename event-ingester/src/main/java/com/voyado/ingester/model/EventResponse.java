package com.voyado.ingester.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventResponse {
    private String eventId;
    private String status;
    private Instant receivedAt;
    private String message;
    
    public static EventResponse success(String eventId) {
        return new EventResponse(
            eventId,
            "ACCEPTED",
            Instant.now(),
            "Event successfully ingested"
        );
    }
    
    public static EventResponse error(String message) {
        return new EventResponse(
            null,
            "ERROR",
            Instant.now(),
            message
        );
    }
}
