
package com.ishwinder.expense_tracker.report;

import com.ishwinder.expense_tracker.report.dto.MonthlyReport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService service;
    private final CsvExportService csvExportService;
    private final PdfExportService pdfExportService;

    public ReportController(
            ReportService service,
            CsvExportService csvExportService,
            PdfExportService pdfExportService) {
        this.service = service;
        this.csvExportService = csvExportService;
        this.pdfExportService = pdfExportService;
    }

    @GetMapping("/monthly")
    public MonthlyReport monthly(@RequestParam("month") String month) {
        return service.monthly(month);
    }

    @GetMapping("/monthly/export/csv")
    public ResponseEntity<byte[]> exportCsv(@RequestParam("month") String month) {
        byte[] csv = csvExportService.exportMonthly(month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"expenses-" + month + ".csv\"")
                .contentType(new MediaType("text", "csv"))
                .body(csv);
    }

    @GetMapping("/monthly/export/pdf")
    public ResponseEntity<byte[]> exportPdf(@RequestParam("month") String month) {
        byte[] pdf = pdfExportService.exportMonthly(month);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"report-" + month + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
