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
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financetracker.data.AppDatabase
import com.example.financetracker.data.Transaction
import com.example.financetracker.data.TransactionDao
import com.example.financetracker.ml.DownloadStatus
import com.example.financetracker.ml.ModelManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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
                FinanceTrackerApp(factory)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceTrackerApp(factory: MainViewModelFactory, viewModel: MainViewModel = viewModel(factory = factory)) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val modelManager = remember { ModelManager(context) }
    val isModelDownloaded = remember { androidx.compose.runtime.mutableStateOf(modelManager.isModelDownloaded()) }
    val showDownloadOverlay = remember { androidx.compose.runtime.mutableStateOf(false) }
    val downloadStatus by modelManager.downloadStatus.collectAsState(initial = DownloadStatus.Idle)
    val coroutineScope = rememberCoroutineScope()

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
            FloatingActionButton(onClick = {
                if (isModelDownloaded.value) {
                    /* TODO: Implement add transaction */
                } else {
                    showDownloadOverlay.value = true
                }
            }) {
                Icon(Icons.Filled.Add, contentDescription = "Add Transaction")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            TransactionList(
                transactions = transactions,
                modifier = Modifier.fillMaxSize()
            )

            if (showDownloadOverlay.value && !isModelDownloaded.value) {
                LaunchedEffect(downloadStatus) {
                    if (downloadStatus is DownloadStatus.Downloaded) {
                        isModelDownloaded.value = true
                        showDownloadOverlay.value = false
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            "Downloading AI Model (~450MB). Please stay on this screen...",
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        when (val status = downloadStatus) {
                            is DownloadStatus.Idle -> {
                                Button(onClick = {
                                    coroutineScope.launch {
                                        modelManager.downloadModel()
                                    }
                                }) {
                                    Text("Start Download")
                                }
                            }
                            is DownloadStatus.Downloading -> {
                                LinearProgressIndicator(
                                    progress = status.progress,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${(status.progress * 100).toInt()}%")
                            }
                            is DownloadStatus.Downloaded -> {
                                // Handled in LaunchedEffect
                            }
                            is DownloadStatus.Error -> {
                                Text("Error: ${status.message}", color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(onClick = {
                                    coroutineScope.launch {
                                        modelManager.downloadModel()
                                    }
                                }) {
                                    Text("Retry Download")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedButton(onClick = { showDownloadOverlay.value = false }) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
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
