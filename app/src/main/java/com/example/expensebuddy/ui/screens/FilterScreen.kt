package com.example.expensebuddy.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.expensebuddy.data.Expense
import com.example.expensebuddy.ui.components.AmountText
import com.example.expensebuddy.ui.components.ExpenseCard
import com.example.expensebuddy.ui.components.ExpenseTopBar
import com.example.expensebuddy.ui.viewmodel.ExpenseFilter
import com.example.expensebuddy.ui.viewmodel.ExpenseViewModel
import com.maxkeppeker.sheets.core.models.base.rememberUseCaseState
import com.maxkeppeler.sheets.calendar.CalendarDialog
import com.maxkeppeler.sheets.calendar.models.CalendarConfig
import com.maxkeppeler.sheets.calendar.models.CalendarSelection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

data class DateRangeFilterState(
    var startDate: LocalDate = LocalDate.now(),
    var endDate: LocalDate = LocalDate.now()
)

data class CategoryFilterState(
    var selectedCategory: String = EXPENSE_CATEGORIES[0]
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterScreen(
    viewModel: ExpenseViewModel,
    onNavigateBack: () -> Unit,
    onEditExpense: (Long) -> Unit
) {
    var selectedFilterType by remember { mutableIntStateOf(0) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var displayedExpenses by remember { mutableStateOf<List<Expense>>(emptyList()) }
    
    var dateRangeState by remember { mutableStateOf(DateRangeFilterState()) }
    var categoryState by remember { mutableStateOf(CategoryFilterState()) }

    // Remember the last applied filter for each type
    var lastDateRangeFilter by remember { mutableStateOf<ExpenseFilter.ByDateRange?>(null) }
    var lastCategoryFilter by remember { mutableStateOf<ExpenseFilter.ByCategory?>(null) }
    
    // Track if a filter has been applied for the current type
    var hasAppliedCurrentFilter by remember { mutableStateOf(false) }

    val scrollState = rememberLazyListState()
    val isScrolling by remember {
        derivedStateOf {
            scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0
        }
    }

    val filteredExpenses by viewModel.filteredExpenses.collectAsState()
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    // Initialize with All filter
    LaunchedEffect(Unit) {
        viewModel.applyFilter(ExpenseFilter.All)
        hasAppliedCurrentFilter = true
        displayedExpenses = filteredExpenses
    }

    // Handle date picker dialogs
    if (showStartDatePicker) {
        CalendarDialog(
            state = rememberUseCaseState(
                visible = true,
                onDismissRequest = { showStartDatePicker = false }
            ),
            config = CalendarConfig(
                monthSelection = true,
                yearSelection = true
            ),
            selection = CalendarSelection.Date { newDate ->
                dateRangeState = dateRangeState.copy(startDate = newDate)
                showStartDatePicker = false
                // Apply the filter immediately when date changes
                val filter = ExpenseFilter.ByDateRange(newDate, dateRangeState.endDate)
                viewModel.applyFilter(filter)
                lastDateRangeFilter = filter
                hasAppliedCurrentFilter = true
            }
        )
    }

    if (showEndDatePicker) {
        CalendarDialog(
            state = rememberUseCaseState(onDismissRequest = { showEndDatePicker = false }),
            config = CalendarConfig(
                monthSelection = true,
                yearSelection = true
            ),
            selection = CalendarSelection.Date { newDate ->
                dateRangeState = dateRangeState.copy(endDate = newDate)
                showEndDatePicker = false
                // Apply the filter immediately when date changes
                val filter = ExpenseFilter.ByDateRange(dateRangeState.startDate, newDate)
                viewModel.applyFilter(filter)
                lastDateRangeFilter = filter
                hasAppliedCurrentFilter = true
            }
        )
    }

    // Reset displayed expenses when switching filter types
    LaunchedEffect(selectedFilterType) {
        when (selectedFilterType) {
            0 -> {
                viewModel.applyFilter(ExpenseFilter.All)
                hasAppliedCurrentFilter = true
                displayedExpenses = filteredExpenses
            }
            1 -> {
                hasAppliedCurrentFilter = false
                displayedExpenses = emptyList()
            }

            2 -> {
                hasAppliedCurrentFilter = false
                displayedExpenses = emptyList()
            }
        }
    }

    // Update displayed expenses only when filter is applied
    LaunchedEffect(filteredExpenses) {
        if (hasAppliedCurrentFilter) {
            displayedExpenses = filteredExpenses
        }
    }

    Scaffold(
        topBar = {
            ExpenseTopBar(
                title = "Filter Expenses",
                onBackClick = onNavigateBack
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filter type selection
                Text(
                    text = "Filter Type",
                    style = MaterialTheme.typography.titleMedium
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFilterType == 0,
                        onClick = { 
                            selectedFilterType = 0
                            viewModel.applyFilter(ExpenseFilter.All)
                            hasAppliedCurrentFilter = true
                            displayedExpenses = filteredExpenses
                        },
                        label = { Text("All") }
                    )
                    FilterChip(
                        selected = selectedFilterType == 1,
                        onClick = { 
                            selectedFilterType = 1
                            hasAppliedCurrentFilter = false
                            displayedExpenses = emptyList()
                            // Restore last date range filter if exists
                            lastDateRangeFilter?.let {
                                viewModel.applyFilter(it)
                                hasAppliedCurrentFilter = true
                                displayedExpenses = filteredExpenses
                            }
                        },
                        label = { Text("Date Range") }
                    )
                    FilterChip(
                        selected = selectedFilterType == 2,
                        onClick = { 
                            selectedFilterType = 2
                            hasAppliedCurrentFilter = false
                            displayedExpenses = emptyList()
                            // Restore last category filter if exists
                            lastCategoryFilter?.let {
                                viewModel.applyFilter(it)
                                hasAppliedCurrentFilter = true
                                displayedExpenses = filteredExpenses
                            }
                        },
                        label = { Text("Category") }
                    )
                }

                // Filter options with button visibility
                if (selectedFilterType > 0 && !isScrolling) {
                    when (selectedFilterType) {
                        1 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Select Date Range",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                OutlinedTextField(
                                    value = dateRangeState.startDate.format(dateFormatter),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Start Date") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { 
                                            showStartDatePicker = true 
                                        },
                                    trailingIcon = {
                                        IconButton(
                                            onClick = { 
                                                showStartDatePicker = true 
                                            }
                                        ) {
                                            Icon(
                                                Icons.Default.FilterList,
                                                contentDescription = "Select start date"
                                            )
                                        }
                                    }
                                )
                                OutlinedTextField(
                                    value = dateRangeState.endDate.format(dateFormatter),
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("End Date") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showEndDatePicker = true }
                                )
                                
                                if (dateRangeState.endDate.isBefore(dateRangeState.startDate)) {
                                    Text(
                                        text = "End date must be after start date",
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                Button(
                                    onClick = {
                                        if (!dateRangeState.endDate.isBefore(dateRangeState.startDate)) {
                                            val filter = ExpenseFilter.ByDateRange(
                                                dateRangeState.startDate,
                                                dateRangeState.endDate
                                            )
                                            viewModel.applyFilter(filter)
                                            lastDateRangeFilter = filter
                                            hasAppliedCurrentFilter = true
                                            displayedExpenses = filteredExpenses
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp),
                                    enabled = !dateRangeState.endDate.isBefore(dateRangeState.startDate)
                                ) {
                                    Text("Apply Date Filter")
                                }
                            }
                        }
                        2 -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Select Category",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                ExposedDropdownMenuBox(
                                    expanded = showCategoryDropdown,
                                    onExpandedChange = { showCategoryDropdown = it }
                                ) {
                                    OutlinedTextField(
                                        value = categoryState.selectedCategory,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Category") },
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor()
                                    )
                                    ExposedDropdownMenu(
                                        expanded = showCategoryDropdown,
                                        onDismissRequest = { showCategoryDropdown = false }
                                    ) {
                                        EXPENSE_CATEGORIES.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(option) },
                                                onClick = {
                                                    categoryState = categoryState.copy(selectedCategory = option)
                                                    showCategoryDropdown = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Button(
                                    onClick = {
                                        val filter = ExpenseFilter.ByCategory(categoryState.selectedCategory)
                                        viewModel.applyFilter(filter)
                                        lastCategoryFilter = filter
                                        hasAppliedCurrentFilter = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp)
                                ) {
                                    Text("Apply Category Filter")
                                }
                            }
                        }
                    }
                }

                // Show filtered results only when click on apply filter button
                if (hasAppliedCurrentFilter && displayedExpenses.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = when (selectedFilterType) {
                                    0 -> "All Expenses"
                                    1 -> "${dateRangeState.startDate.format(dateFormatter)} - ${dateRangeState.endDate.format(dateFormatter)}"
                                    2 -> "Category: ${categoryState.selectedCategory}"
                                    else -> "Total"
                                },
                                style = MaterialTheme.typography.titleSmall
                            )
                            AmountText(
                                amount = displayedExpenses.sumOf { it.amount },
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    LazyColumn(
                        state = scrollState,
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(displayedExpenses) { expense ->
                            ExpenseCard(
                                expense = expense,
                                onClick = { onEditExpense(expense.id) }
                            )
                        }
                    }
                } else if (hasAppliedCurrentFilter && displayedExpenses.isEmpty()) {
                    // Show no results message
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = when (selectedFilterType) {
                                0 -> "No expenses found"
                                1 -> "No expenses found in selected date range"
                                2 -> "No expenses found in category: ${categoryState.selectedCategory}"
                                else -> "No expenses found"
                            },
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
} 