package com.ecold.exception;

public class TemplateValidationException extends RuntimeException {
    public TemplateValidationException(String message) {
        super(message);
    }
    
    public TemplateValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}