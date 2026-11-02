package com.ishwinder.expense_tracker.common;

public class NotFoundException extends RuntimeException {
    public NotFoundException(String message) { super(message); }
}