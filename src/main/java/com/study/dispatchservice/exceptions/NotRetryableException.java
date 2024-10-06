package com.study.dispatchservice.exceptions;

public class NotRetryableException extends RuntimeException {

    public NotRetryableException(Exception exception) {
        super(exception);
    }
}
