package com.example.expensebuddy.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.expensebuddy.data.Expense
import com.example.expensebuddy.ui.components.AmountText
import com.example.expensebuddy.ui.components.ExpenseCard
import com.example.expensebuddy.ui.components.ExpenseTopBar
import com.example.expensebuddy.ui.viewmodel.ExpenseViewModel
import com.example.expensebuddy.ui.viewmodel.TimePeriod
import com.example.expensebuddy.util.PdfExporter
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: ExpenseViewModel,
    onAddExpense: () -> Unit,
    onEditExpense: (Long) -> Unit,
    onOpenFilter: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val expenses by viewModel.expenses.collectAsState()
    val currentTotal by viewModel.currentTotal.collectAsState()
    val selectedPeriod by viewModel.selectedTimePeriod.collectAsState()
    val customStartDate by viewModel.customMonthStartDate.collectAsState()
    val customEndDate by viewModel.customMonthEndDate.collectAsState()

    val formatter = DateTimeFormatter.ofPattern("dd MMM")

    Scaffold(
        topBar = {
            ExpenseTopBar(
                title = "Expense Buddy",
                actions = {
                    // PDF Export button
                    IconButton(
                        onClick = {
                            scope.launch {
                                try {
                                    val pdfExporter = PdfExporter()
                                    val currentYearMonth = when (selectedPeriod) {
                                        TimePeriod.DAILY -> YearMonth.now()
                                        TimePeriod.MONTHLY -> YearMonth.now()
                                        TimePeriod.CUSTOM_MONTHLY -> {
                                            customStartDate?.let { YearMonth.from(it) }
                                                ?: YearMonth.now()
                                        }
                                    }
                                    val file = pdfExporter.exportMonthlyReport(
                                        context = context,
                                        expenses = expenses,
                                        yearMonth = currentYearMonth
                                    )
                                    Toast.makeText(
                                        context,
                                        "PDF exported successfully: ${file.name}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Failed to export PDF: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF"
                        )
                    }
                    // Filter button
                    IconButton(onClick = onOpenFilter) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddExpense) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Time period selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedPeriod == TimePeriod.DAILY,
                    onClick = { viewModel.setTimePeriod(TimePeriod.DAILY) },
                    label = { Text("Daily") }
                )
                FilterChip(
                    selected = selectedPeriod == TimePeriod.MONTHLY,
                    onClick = { viewModel.setTimePeriod(TimePeriod.MONTHLY) },
                    label = { Text("Monthly") }
                )
            }

            // Total card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (selectedPeriod) {
                            TimePeriod.DAILY -> "Today's Total"
                            TimePeriod.MONTHLY -> "This Month's Total"
                            TimePeriod.CUSTOM_MONTHLY -> {
                                if (customStartDate != null && customEndDate != null) {
                                    "${formatter.format(customStartDate)} - ${formatter.format(customEndDate)} Total"
                                } else {
                                    "Custom Monthly Total"
                                }
                            }
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (selectedPeriod == TimePeriod.CUSTOM_MONTHLY) {
                        Text(
                            text = "(Current Period)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        
                        if (customEndDate != null) {
                            val nextStartDate = customEndDate!!.plusDays(1)
                            val nextEndDate = nextStartDate.plusMonths(1).minusDays(1)
                            Text(
                                text = "Next Period: ${formatter.format(nextStartDate)} - ${formatter.format(nextEndDate)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    AmountText(
                        amount = currentTotal ?: 0.0,
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            // Expense list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenses) { expense ->
                    ExpenseCard(
                        expense = expense,
                        onClick = { onEditExpense(expense.id) }
                    )
                }
            }
        }
    }
} 