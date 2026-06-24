package com.interview.platform.exception;

public class EmailAlreadyExistsException extends RuntimeException {
     public EmailAlreadyExistsException(String message) {
        super(message);
    }
    
}
