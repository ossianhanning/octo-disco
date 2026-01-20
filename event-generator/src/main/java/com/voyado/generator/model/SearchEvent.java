package com.voyado.generator.model;

public class SearchEvent {
    private String query;
    private String userId;
    private String sessionId;
    private Integer resultCount;
    private String category;
    private String timestamp;

    public SearchEvent() {
    }

    public SearchEvent(String query, String userId, String sessionId, Integer resultCount, String category, String timestamp) {
        this.query = query;
        this.userId = userId;
        this.sessionId = sessionId;
        this.resultCount = resultCount;
        this.category = category;
        this.timestamp = timestamp;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
