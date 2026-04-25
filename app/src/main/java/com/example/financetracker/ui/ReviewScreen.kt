package com.example.financetracker.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.financetracker.data.ReceiptItem
import com.example.financetracker.data.Transaction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    initialTransaction: Transaction,
    onSave: (Transaction) -> Unit,
    onCancel: () -> Unit
) {
    var shopName by remember { mutableStateOf(initialTransaction.shopName) }
    var date by remember { mutableStateOf(initialTransaction.date) }
    var totalAmount by remember { mutableStateOf(initialTransaction.totalAmount.toString()) }
    var items by remember { mutableStateOf(initialTransaction.items) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Receipt") },
                actions = {
                    IconButton(onClick = {
                        val finalTransaction = initialTransaction.copy(
                            shopName = shopName,
                            date = date,
                            totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                            items = items
                        )
                        onSave(finalTransaction)
                    }) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("General Information", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Shop Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Date (YYYY-MM-DD)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = totalAmount,
                        onValueChange = { totalAmount = it },
                        label = { Text("Total") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Divider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Items", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            }

            itemsIndexed(items) { index, item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = item.name,
                        onValueChange = { newName ->
                            val newList = items.toMutableList()
                            newList[index] = item.copy(name = newName)
                            items = newList
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Item Name") }
                    )
                    OutlinedTextField(
                        value = item.price.toString(),
                        onValueChange = { newPrice ->
                            val priceValue = newPrice.toDoubleOrNull() ?: 0.0
                            val newList = items.toMutableList()
                            newList[index] = item.copy(price = priceValue)
                            items = newList
                        },
                        modifier = Modifier.width(80.dp),
                        label = { Text("Price") }
                    )
                    IconButton(onClick = {
                        items = items.toMutableList().apply { removeAt(index) }
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove")
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        items = items + ReceiptItem("New Item", 0.0)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Add Item")
                }
            }
        }
    }
}
