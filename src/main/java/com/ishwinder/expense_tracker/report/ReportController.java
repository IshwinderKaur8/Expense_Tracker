package com.ishwinder.expense_tracker.report;

import com.ishwinder.expense_tracker.report.dto.MonthlyReport;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/monthly")
    public MonthlyReport monthly(@RequestParam("month") String month) {
        return service.monthly(month);
    }
}