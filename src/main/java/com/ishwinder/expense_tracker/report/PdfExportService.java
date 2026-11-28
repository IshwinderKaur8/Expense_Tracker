
package com.ishwinder.expense_tracker.report;

import com.ishwinder.expense_tracker.report.dto.MonthlyReport;
import com.ishwinder.expense_tracker.report.dto.MonthlyReport.CategoryLine;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfExportService {

    private final ReportService reportService;

    public PdfExportService(ReportService reportService) {
        this.reportService = reportService;
    }

    public byte[] exportMonthly(String month) {
        MonthlyReport report = reportService.monthly(month);

        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document();
            PdfWriter.getInstance(document, output);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("Monthly Expense Report", titleFont));
            document.add(new Paragraph("Month: " + month, normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Summary", headingFont));
            document.add(new Paragraph(
                    "Total spent: " + report.totalSpent(), normalFont));
            document.add(new Paragraph(
                    "Overall budget: " +
                            (report.overallBudget() == null
                                    ? "Not set"
                                    : report.overallBudget()),
                    normalFont));
            document.add(new Paragraph(
                    "Remaining: " +
                            (report.remaining() == null
                                    ? "Not set"
                                    : report.remaining()),
                    normalFont));
            document.add(new Paragraph(" "));

            document.add(new Paragraph("Category Breakdown", headingFont));
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);

            table.addCell("Category");
            table.addCell("Spent");
            table.addCell("Share");
            table.addCell("Budget");
            table.addCell("Status");

            for (CategoryLine line : report.byCategory()) {
                table.addCell(line.name());
                table.addCell(String.valueOf(line.spent()));
                table.addCell(String.format("%.2f%%", line.percentOfTotal()));
                table.addCell(
                        line.budget() == null
                                ? "Not set"
                                : String.valueOf(line.budget()));
                table.addCell(
                        line.budgetStatus() == null
                                ? "NONE"
                                : line.budgetStatus().toString());
            }

            document.add(table);
            document.close();

            return output.toByteArray();
        } catch (DocumentException | IOException exception) {
            throw new IllegalStateException("Could not generate monthly PDF.", exception);
        }
    }
}
