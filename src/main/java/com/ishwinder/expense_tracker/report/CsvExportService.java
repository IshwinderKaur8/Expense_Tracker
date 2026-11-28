
package com.ishwinder.expense_tracker.report;

import com.ishwinder.expense_tracker.category.CategoryService;
import com.ishwinder.expense_tracker.category.dto.CategoryResponse;
import com.ishwinder.expense_tracker.common.CurrentUserProvider;
import com.ishwinder.expense_tracker.common.Months;
import com.ishwinder.expense_tracker.expense.Expense;
import com.ishwinder.expense_tracker.expense.ExpenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CsvExportService {

    private final ExpenseRepository expenseRepository;
    private final CategoryService categoryService;
    private final CurrentUserProvider currentUser;

    public CsvExportService(ExpenseRepository expenseRepository,
                           CategoryService categoryService,
                           CurrentUserProvider currentUser) {
        this.expenseRepository = expenseRepository;
        this.categoryService = categoryService;
        this.currentUser = currentUser;
    }

    @Transactional(readOnly = true)
    public byte[] exportMonthly(String month) {
        YearMonth yearMonth = Months.parse(month);

        List<Expense> expenses = expenseRepository.findAllForExport(
                currentUser.currentUserId(),
                yearMonth.atDay(1),
                yearMonth.atEndOfMonth());

        Set<Long> categoryIds = expenses.stream()
                .map(Expense::getCategoryId)
                .collect(Collectors.toSet());

        Map<Long, CategoryResponse> categories = categoryService.findByIds(categoryIds);

        StringBuilder csv = new StringBuilder();
        csv.append("Expense ID,Date,Category,Amount,Description\r\n");

        for (Expense expense : expenses) {
            CategoryResponse category = categories.get(expense.getCategoryId());
            String categoryName = category != null ? category.name() : "Unknown";

            csv.append(expense.getId()).append(',');
            csv.append(csvCell(expense.getExpenseDate().toString())).append(',');
            csv.append(csvCell(categoryName)).append(',');
            csv.append(expense.getAmount().toPlainString()).append(',');
            csv.append(csvCell(expense.getDescription()));
            csv.append("\r\n");
        }

        // UTF-8 BOM helps spreadsheet software recognize UTF-8 CSV files.
        byte[] content = csv.toString().getBytes(StandardCharsets.UTF_8);
        byte[] bom = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};

        byte[] result = new byte[bom.length + content.length];
        System.arraycopy(bom, 0, result, 0, bom.length);
        System.arraycopy(content, 0, result, bom.length, content.length);

        return result;
    }

    private String csvCell(String value) {
        if (value == null) {
            return "";
        }

        // Prevent spreadsheet formula injection in text fields.
        String safe = value.replaceAll("^[\\s]*([=+\\-@])", "'$1");

        // Escape double quotes and wrap the cell in quotes.
        return "\"" + safe.replace("\"", "\"\"") + "\"";
    }
}
