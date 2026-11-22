package com.ishwinder.expense_tracker.expense.dto;

import java.math.BigDecimal;

public record CategoryTotal(Long categoryId, BigDecimal total) { }