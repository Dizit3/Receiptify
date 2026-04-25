package com.example.financetracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financetracker.data.AppDatabase
import com.example.financetracker.data.Transaction
import com.example.financetracker.data.TransactionDao
import com.example.financetracker.ui.ReviewScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Screen {
    History, Review
}

class MainViewModel(private val transactionDao: TransactionDao) : ViewModel() {
    val transactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()
}

class MainViewModelFactory(private val transactionDao: TransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(transactionDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = AppDatabase.getDatabase(this)
        val transactionDao = database.transactionDao()
        val factory = MainViewModelFactory(transactionDao)

        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf(Screen.History) }
                var pendingTransaction by remember { mutableStateOf<Transaction?>(null) }
                val scope = rememberCoroutineScope()

                when (currentScreen) {
                    Screen.History -> {
                        FinanceTrackerApp(
                            factory = factory,
                            onAddTransaction = {
                                // Placeholder: In real app, this triggers AI analysis
                                // For now, we mock a transition to ReviewScreen
                                pendingTransaction = com.example.financetracker.data.Transaction(
                                    shopName = "New Store",
                                    date = "2024-04-25",
                                    totalAmount = 0.0,
                                    items = emptyList()
                                )
                                currentScreen = Screen.Review
                            }
                        )
                    }
                    Screen.Review -> {
                        pendingTransaction?.let { transaction ->
                            ReviewScreen(
                                initialTransaction = transaction,
                                onSave = { updatedTransaction ->
                                    scope.launch {
                                        transactionDao.insert(updatedTransaction)
                                        currentScreen = Screen.History
                                    }
                                },
                                onCancel = {
                                    currentScreen = Screen.History
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceTrackerApp(
    factory: MainViewModelFactory, 
    viewModel: MainViewModel = viewModel(factory = factory),
    onAddTransaction: () -> Unit
) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Finance Tracker") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.primary,
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTransaction) {
                Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        TransactionList(
            transactions = transactions,
            modifier = Modifier.padding(paddingValues)
        )
    }
}

@Composable
fun TransactionList(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    if (transactions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
            Text(text = "No transactions yet", style = MaterialTheme.typography.bodyLarge)
        }
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions) { transaction ->
                TransactionItem(transaction)
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = transaction.shopName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = String.format("$%.2f", transaction.totalAmount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = transaction.date,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (transaction.items.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "${transaction.items.size} items",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
