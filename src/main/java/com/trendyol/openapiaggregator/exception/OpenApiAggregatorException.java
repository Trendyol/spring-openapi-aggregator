package com.trendyol.openapiaggregator.exception;

public class OpenApiAggregatorException extends RuntimeException {
    public OpenApiAggregatorException(String message) {
        super(message);
    }

    public OpenApiAggregatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
