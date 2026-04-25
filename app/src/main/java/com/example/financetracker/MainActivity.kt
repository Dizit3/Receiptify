package com.example.financetracker

import android.net.Uri
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.financetracker.data.AppDatabase
import com.example.financetracker.data.Transaction
import com.example.financetracker.data.TransactionDao
import com.example.financetracker.ml.DownloadStatus
import com.example.financetracker.ml.ModelManager
import com.example.financetracker.ui.AIStatusBar
import com.example.financetracker.ui.ReviewScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

enum class Screen {
    History, Review
}

class MainViewModel(private val transactionDao: TransactionDao) : ViewModel() {
    val transactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    fun onImageSelected(uri: Uri, context: Context, onBitmapLoaded: (Bitmap) -> Unit) {
        try {
            val bitmap: Bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
            Log.i("MainViewModel", "Successfully loaded bitmap from URI: $uri, dimensions: ${bitmap.width}x${bitmap.height}")
            onBitmapLoaded(bitmap)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error loading bitmap from URI: $uri", e)
        }
    }
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
                            onResultReady = { transaction ->
                                pendingTransaction = transaction
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
                                        transactionDao.insertTransaction(updatedTransaction)
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
    onResultReady: (Transaction) -> Unit
) {
    val transactions by viewModel.transactions.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Model Management State
    val modelManager = remember { ModelManager(context) }
    val isModelDownloaded = remember { mutableStateOf(modelManager.isModelDownloaded()) }
    val showDownloadOverlay = remember { mutableStateOf(false) }
    val downloadStatus by modelManager.downloadStatus.collectAsState(initial = DownloadStatus.Idle)

    // Image Selection State
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var tempImageUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var tempImageUri = tempImageUriString?.let { Uri.parse(it) }

    var isAnalyzing by remember { mutableStateOf(false) }

    val handleImage = { uri: Uri ->
        viewModel.onImageSelected(uri, context) { bitmap ->
            coroutineScope.launch {
                isAnalyzing = true
                try {
                    val modelPath = modelManager.getModelFile().absolutePath
                    val analyzer = com.example.financetracker.ml.ReceiptAnalyzer(context, modelPath)
                    val jsonResult = analyzer.analyzeReceipt(bitmap)
                    analyzer.close()

                    if (jsonResult != null) {
                        try {
                            // Parse JSON result using Gson
                            val gson = com.google.gson.Gson()
                            val cleanedJson = jsonResult.trim().removeSurrounding("```json", "```").trim()
                            val analyzedTransaction = gson.fromJson(cleanedJson, Transaction::class.java)
                            onResultReady(analyzedTransaction)
                        } catch (e: Exception) {
                            Log.e("MainActivity", "Error parsing AI JSON response: $jsonResult", e)
                            onResultReady(Transaction(shopName = "Parse Error", date = "", totalAmount = 0.0, items = emptyList()))
                        }
                    } else {
                        Log.e("MainActivity", "AI analysis returned null")
                        onResultReady(Transaction(shopName = "Analysis Failed", date = "", totalAmount = 0.0, items = emptyList()))
                    }
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error during AI analysis", e)
                } finally {
                    isAnalyzing = false
                }
            }
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            handleImage(uri)
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempImageUri != null) {
            handleImage(tempImageUri!!)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val storageDir = context.cacheDir
            val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
            tempImageUriString = uri.toString()
            cameraLauncher.launch(uri)
        } else {
            Log.w("MainActivity", "Camera permission denied")
        }
    }

    // Add Transaction Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Transaction") },
            text = { Text("Choose a method to add a receipt.") },
            confirmButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    permissionLauncher.launch(android.Manifest.permission.CAMERA)
                }) {
                    Text("Take Photo")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddDialog = false
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                }) {
                    Text("Gallery")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Finance Tracker") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.primary,
                    )
                )
                AIStatusBar(
                    downloadStatus = downloadStatus,
                    isAnalyzing = isAnalyzing
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                if (isModelDownloaded.value) {
                    showAddDialog = true 
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

            // Download Overlay UI
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
                            is DownloadStatus.Downloaded -> { /* Handled in LaunchedEffect */ }
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

            // AI Analysis Overlay
            if (isAnalyzing) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Analyzing Receipt with AI...",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TransactionList(transactions: List<Transaction>, modifier: Modifier = Modifier) {
    if (transactions.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
