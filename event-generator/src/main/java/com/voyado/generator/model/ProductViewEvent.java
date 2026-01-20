package com.voyado.generator.model;

public class ProductViewEvent {
    private String userId;
    private String productId;
    private Long timestamp;
    private String sessionId;
    private Double price;
    private String category;

    public ProductViewEvent() {
    }

    public ProductViewEvent(String userId, String productId, Long timestamp, String sessionId, Double price, String category) {
        this.userId = userId;
        this.productId = productId;
        this.timestamp = timestamp;
        this.sessionId = sessionId;
        this.price = price;
        this.category = category;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
