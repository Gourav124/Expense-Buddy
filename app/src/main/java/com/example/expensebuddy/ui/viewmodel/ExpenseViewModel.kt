package com.example.expensebuddy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.expensebuddy.data.Expense
import com.example.expensebuddy.data.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

enum class TimePeriod {
    DAILY,
    MONTHLY,
    CUSTOM_MONTHLY
}

class ExpenseViewModel(private val repository: ExpenseRepository) : ViewModel() {
    private val _selectedTimePeriod = MutableStateFlow(TimePeriod.DAILY)
    val selectedTimePeriod = _selectedTimePeriod.asStateFlow()

    private val _currentFilter = MutableStateFlow<ExpenseFilter>(ExpenseFilter.All)
    val currentFilter = _currentFilter.asStateFlow()

    private val _selectedExpense = MutableStateFlow<Expense?>(null)
    val selectedExpense = _selectedExpense.asStateFlow()

    private val _filteredExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val filteredExpenses = _filteredExpenses.asStateFlow()

    private val _customMonthStartDate = MutableStateFlow<LocalDate?>(null)
    val customMonthStartDate = _customMonthStartDate.asStateFlow()

    private val _customMonthEndDate = MutableStateFlow<LocalDate?>(null)
    val customMonthEndDate = _customMonthEndDate.asStateFlow()

    init {
        // Check and update custom month period if needed
        viewModelScope.launch {
            updateCustomMonthPeriodIfNeeded()
        }
    }

    private fun updateCustomMonthPeriodIfNeeded() {
        val today = LocalDate.now()
        val startDate = _customMonthStartDate.value
        val endDate = _customMonthEndDate.value

        if (startDate != null && endDate != null && today.isAfter(endDate)) {
            // Current period has ended, move to next month period
            val nextStartDate = endDate.plusDays(1)
            val nextEndDate = nextStartDate.plusMonths(1).minusDays(1)
            _customMonthStartDate.value = nextStartDate
            _customMonthEndDate.value = nextEndDate
        }
    }

    val expenses = _selectedTimePeriod.flatMapLatest { period ->
        when (period) {
            TimePeriod.DAILY -> {
                val today = LocalDate.now()
                repository.getExpensesByDateRange(today, today)
            }
            TimePeriod.MONTHLY -> {
                val currentMonth = YearMonth.now()
                repository.getExpensesByDateRange(
                    currentMonth.atDay(1),
                    currentMonth.atEndOfMonth()
                )
            }
            TimePeriod.CUSTOM_MONTHLY -> {
                updateCustomMonthPeriodIfNeeded()
                val startDate = _customMonthStartDate.value ?: LocalDate.now()
                val endDate = startDate.plusMonths(1).minusDays(1)
                _customMonthEndDate.value = endDate
                repository.getExpensesByDateRange(startDate, endDate)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    val currentTotal = _selectedTimePeriod.flatMapLatest { period ->
        when (period) {
            TimePeriod.DAILY -> {
                val today = LocalDate.now()
                repository.getTotalExpenseForPeriod(today, today)
            }
            TimePeriod.MONTHLY -> {
                val currentMonth = YearMonth.now()
                repository.getTotalExpenseForPeriod(
                    currentMonth.atDay(1),
                    currentMonth.atEndOfMonth()
                )
            }
            TimePeriod.CUSTOM_MONTHLY -> {
                updateCustomMonthPeriodIfNeeded()
                val startDate = _customMonthStartDate.value ?: LocalDate.now()
                val endDate = _customMonthEndDate.value ?: startDate.plusMonths(1).minusDays(1)
                repository.getTotalExpenseForPeriod(startDate, endDate)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), 0.0)

    fun setTimePeriod(period: TimePeriod) {
        _selectedTimePeriod.value = period
    }

    fun setCustomMonthStartDate(date: LocalDate) {
        _customMonthStartDate.value = date
        _customMonthEndDate.value = date.plusMonths(1).minusDays(1)
        if (_selectedTimePeriod.value != TimePeriod.CUSTOM_MONTHLY) {
            _selectedTimePeriod.value = TimePeriod.CUSTOM_MONTHLY
        }
    }

    fun applyFilter(filter: ExpenseFilter) {
        viewModelScope.launch {
            _currentFilter.value = filter
            val expenses = when (filter) {
                is ExpenseFilter.All -> repository.getAllExpenses().first()
                is ExpenseFilter.ByDateRange -> repository.getExpensesByDateRange(
                    filter.startDate,
                    filter.endDate
                ).first()
                is ExpenseFilter.ByCategory -> repository.getExpensesByCategory(
                    filter.category
                ).first()
            }
            _filteredExpenses.value = expenses
        }
    }

    fun loadExpense(id: Long) {
        viewModelScope.launch {
            _selectedExpense.value = repository.getExpenseById(id)
        }
    }

    fun addExpense(expense: Expense) {
        viewModelScope.launch {
            repository.insertExpense(expense)
        }
    }

    fun updateExpense(expense: Expense) {
        viewModelScope.launch {
            repository.updateExpense(expense)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
        }
    }

    class Factory(private val repository: ExpenseRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ExpenseViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ExpenseViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}

sealed class ExpenseFilter {
    object All : ExpenseFilter()
    data class ByDateRange(val startDate: LocalDate, val endDate: LocalDate) : ExpenseFilter()
    data class ByCategory(val category: String) : ExpenseFilter()
} 