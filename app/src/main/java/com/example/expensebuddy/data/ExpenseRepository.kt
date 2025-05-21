package com.example.expensebuddy.data

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    fun getAllExpenses(): Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByDateRange(startDate: LocalDate, endDate: LocalDate): Flow<List<Expense>> =
        expenseDao.getExpensesByDateRange(startDate, endDate)

    fun getExpensesByCategory(category: String): Flow<List<Expense>> =
        expenseDao.getExpensesByCategory(category)

    fun getTotalExpenseForPeriod(startDate: LocalDate, endDate: LocalDate): Flow<Double?> =
        expenseDao.getTotalExpenseForPeriod(startDate, endDate)

    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun getExpenseById(id: Long): Expense? = expenseDao.getExpenseById(id)
} 