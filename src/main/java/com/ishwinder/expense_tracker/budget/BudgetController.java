package com.ishwinder.expense_tracker.budget;

import com.ishwinder.expense_tracker.budget.dto.BudgetRequest;
import com.ishwinder.expense_tracker.budget.dto.BudgetResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/budgets")
public class BudgetController {

    private final BudgetService service;

    public BudgetController(BudgetService service) {
        this.service = service;
    }

    @PutMapping
    public BudgetResponse set(@Valid @RequestBody BudgetRequest request) {
        return service.set(request);
    }

    @GetMapping
    public List<BudgetResponse> list(@RequestParam("month") String month) {
        return service.list(month);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}