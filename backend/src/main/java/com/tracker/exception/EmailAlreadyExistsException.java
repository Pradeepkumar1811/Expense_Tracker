package com.tracker.exception;

public class EmailAlreadyExistsException extends RuntimeException {

    public EmailAlreadyExistsException(String message) {
        super(message);
    }

    public EmailAlreadyExistsException(String email, boolean isEmail) {
        super("Email already exists: " + email);
    }
}
