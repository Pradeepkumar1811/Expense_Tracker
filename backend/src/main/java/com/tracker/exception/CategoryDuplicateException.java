package com.tracker.exception;

public class CategoryDuplicateException extends RuntimeException {

    public CategoryDuplicateException(String message) {
        super(message);
    }

    public CategoryDuplicateException(String categoryName, Long userId) {
        super("Category '" + categoryName + "' already exists for user " + userId);
    }
}
