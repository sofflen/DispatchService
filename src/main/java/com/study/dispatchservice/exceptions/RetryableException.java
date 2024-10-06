package com.study.dispatchservice.exceptions;

public class RetryableException extends RuntimeException {

    public RetryableException() {
        super();
    }

    public RetryableException(Exception exception) {
        super(exception);
    }
}
