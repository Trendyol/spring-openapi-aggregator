package com.trendyol.openapiaggregator.exception;

public class OpenApiTransformException extends RuntimeException {
    public OpenApiTransformException(String message, Exception e) {
        super(message, e);
    }
}
