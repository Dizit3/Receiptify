package com.example.financetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val shopName: String,
    val date: String,
    val totalAmount: Double,
    val items: List<ReceiptItem>
)

data class ReceiptItem(
    val name: String,
    val price: Double
)

class Converters {
    @TypeConverter
    fun fromReceiptItemList(value: List<ReceiptItem>): String {
        val gson = Gson()
        val type = object : TypeToken<List<ReceiptItem>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toReceiptItemList(value: String): List<ReceiptItem> {
        val gson = Gson()
        val type = object : TypeToken<List<ReceiptItem>>() {}.type
        return gson.fromJson(value, type)
    }
}
