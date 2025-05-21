package com.example.expensebuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.expensebuddy.data.ExpenseDatabase
import com.example.expensebuddy.data.ExpenseRepository
import com.example.expensebuddy.navigation.Screen
import com.example.expensebuddy.ui.screens.AddEditExpenseScreen
import com.example.expensebuddy.ui.screens.FilterScreen
import com.example.expensebuddy.ui.screens.HomeScreen
import com.example.expensebuddy.ui.theme.ExpenseBuddyTheme
import com.example.expensebuddy.ui.viewmodel.ExpenseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        setContent {
            ExpenseBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val database = remember { ExpenseDatabase.getDatabase(applicationContext) }
                    val repository = remember { ExpenseRepository(database.expenseDao()) }
                    val viewModel: ExpenseViewModel = viewModel(
                        factory = ExpenseViewModel.Factory(repository)
                    )

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = viewModel,
                                onAddExpense = {
                                    navController.navigate(Screen.AddExpense.route)
                                },
                                onEditExpense = { expenseId ->
                                    navController.navigate(Screen.EditExpense.createRoute(expenseId))
                                },
                                onOpenFilter = {
                                    navController.navigate(Screen.Filter.route)
                                }
                            )
                        }

                        composable(Screen.AddExpense.route) {
                            AddEditExpenseScreen(
                                expenseId = null,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = Screen.EditExpense.route,
                            arguments = listOf(
                                navArgument("expenseId") { type = NavType.LongType }
                            )
                        ) { backStackEntry ->
                            val expenseId = backStackEntry.arguments?.getLong("expenseId")
                            AddEditExpenseScreen(
                                expenseId = expenseId,
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Screen.Filter.route) {
                            FilterScreen(
                                viewModel = viewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onEditExpense = { expenseId ->
                                    navController.navigate(Screen.EditExpense.createRoute(expenseId))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}