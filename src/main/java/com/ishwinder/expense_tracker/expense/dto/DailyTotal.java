package com.ishwinder.expense_tracker.expense.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTotal(LocalDate date, BigDecimal total) { }