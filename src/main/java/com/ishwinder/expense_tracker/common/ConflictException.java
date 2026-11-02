package com.ishwinder.expense_tracker.common;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) { super(message); }
}