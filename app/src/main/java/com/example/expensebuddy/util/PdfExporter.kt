package com.example.expensebuddy.util

import android.content.Context
import android.os.Environment
import com.example.expensebuddy.data.Expense
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.*

class PdfExporter {
    fun exportMonthlyReport(context: Context, expenses: List<Expense>, yearMonth: YearMonth): File {
        val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val fileName = "expense_report_${yearMonth.format(DateTimeFormatter.ofPattern("yyyy_MM"))}.pdf"
        val file = File(downloadDir, fileName)

        PdfWriter(file).use { writer ->
            val pdfDoc = PdfDocument(writer)
            Document(pdfDoc).use { document ->
                // Add title
                val title = Paragraph("Expense Report - ${yearMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy"))}")
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontSize(20f)
                document.add(title)

                // Add summary
                val total = expenses.sumOf { it.amount }
                val summary = Paragraph("Total Expenses: ${formatCurrency(total)}")
                    .setTextAlignment(TextAlignment.RIGHT)
                    .setFontSize(14f)
                document.add(summary)

                // Add expenses table
                val table = Table(UnitValue.createPercentArray(floatArrayOf(20f, 20f, 30f, 30f)))
                    .useAllAvailableWidth()

                // Add headers
                listOf("Date", "Amount", "Category", "Note").forEach { header ->
                    table.addCell(
                        Cell().add(
                            Paragraph(header)
                                .setFontSize(12f)
                                .setBold()
                        )
                    )
                }

                // Add expense rows
                expenses.sortedByDescending { it.date }.forEach { expense ->
                    table.addCell(Cell().add(Paragraph(formatDate(expense.date))))
                    table.addCell(Cell().add(Paragraph(formatCurrency(expense.amount))))
                    table.addCell(Cell().add(Paragraph(expense.category)))
                    table.addCell(Cell().add(Paragraph(expense.note)))
                }

                document.add(table)

                // Add category summary
                document.add(Paragraph("\nCategory Summary").setFontSize(14f))
                val categoryTable = Table(UnitValue.createPercentArray(floatArrayOf(70f, 30f)))
                    .useAllAvailableWidth()

                val categoryTotals = expenses.groupBy { it.category }
                    .mapValues { it.value.sumOf { expense -> expense.amount } }
                    .toList()
                    .sortedByDescending { it.second }

                categoryTotals.forEach { (category, amount) ->
                    categoryTable.addCell(Cell().add(Paragraph(category)))
                    categoryTable.addCell(Cell().add(Paragraph(formatCurrency(amount))))
                }

                document.add(categoryTable)
            }
        }

        return file
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount)
    }

    private fun formatDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
    }
} 