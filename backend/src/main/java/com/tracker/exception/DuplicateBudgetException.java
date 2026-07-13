package com.tracker.exception;

public class DuplicateBudgetException extends RuntimeException {

    public DuplicateBudgetException(String message) {
        super(message);
    }

    public DuplicateBudgetException(Integer month, Integer year) {
        super("Budget already exists for month " + month + "/" + year);
    }
}
