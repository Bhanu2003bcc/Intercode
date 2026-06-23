package com.interview.platform.exception;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
@ResponseStatus(HttpStatus.CONFLICT)  
public class EmailAlreadyExistsException extends RuntimeException {
     public EmailAlreadyExistsException(String message) {
        super(message);
    }
    
}
