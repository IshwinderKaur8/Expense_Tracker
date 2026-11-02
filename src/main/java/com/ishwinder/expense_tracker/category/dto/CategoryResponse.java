package com.ishwinder.expense_tracker.category.dto;

import com.ishwinder.expense_tracker.category.Category;

public record CategoryResponse(Long id, String name, String color) {

    public static CategoryResponse from(Category c) {
        return new CategoryResponse(c.getId(), c.getName(), c.getColor());
    }
}