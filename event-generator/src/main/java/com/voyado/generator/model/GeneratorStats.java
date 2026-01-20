package com.voyado.generator.model;

public class GeneratorStats {
    private long totalSearchEvents;
    private long totalProductViewEvents;
    private long totalEvents;
    private long successfulEvents;
    private long failedEvents;
    private double eventsPerSecond;
    private boolean running;
    private long elapsedTimeMs;

    public GeneratorStats() {
    }

    public GeneratorStats(long totalSearchEvents, long totalProductViewEvents, long totalEvents, long successfulEvents, long failedEvents, double eventsPerSecond, boolean running, long elapsedTimeMs) {
        this.totalSearchEvents = totalSearchEvents;
        this.totalProductViewEvents = totalProductViewEvents;
        this.totalEvents = totalEvents;
        this.successfulEvents = successfulEvents;
        this.failedEvents = failedEvents;
        this.eventsPerSecond = eventsPerSecond;
        this.running = running;
        this.elapsedTimeMs = elapsedTimeMs;
    }

    public long getTotalSearchEvents() {
        return totalSearchEvents;
    }

    public void setTotalSearchEvents(long totalSearchEvents) {
        this.totalSearchEvents = totalSearchEvents;
    }

    public long getTotalProductViewEvents() {
        return totalProductViewEvents;
    }

    public void setTotalProductViewEvents(long totalProductViewEvents) {
        this.totalProductViewEvents = totalProductViewEvents;
    }

    public long getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(long totalEvents) {
        this.totalEvents = totalEvents;
    }

    public long getSuccessfulEvents() {
        return successfulEvents;
    }

    public void setSuccessfulEvents(long successfulEvents) {
        this.successfulEvents = successfulEvents;
    }

    public long getFailedEvents() {
        return failedEvents;
    }

    public void setFailedEvents(long failedEvents) {
        this.failedEvents = failedEvents;
    }

    public double getEventsPerSecond() {
        return eventsPerSecond;
    }

    public void setEventsPerSecond(double eventsPerSecond) {
        this.eventsPerSecond = eventsPerSecond;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    public long getElapsedTimeMs() {
        return elapsedTimeMs;
    }

    public void setElapsedTimeMs(long elapsedTimeMs) {
        this.elapsedTimeMs = elapsedTimeMs;
    }
}
