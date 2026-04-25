package com.example.financetracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @SerializedName("shop_name") val shopName: String,
    @SerializedName("date") val date: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("items") val items: List<ReceiptItem>
)

data class ReceiptItem(
    @SerializedName("name") val name: String,
    @SerializedName("price") val price: Double
)

class Converters {
    companion object {
        private val gson = Gson()
        private val type = object : TypeToken<List<ReceiptItem>>() {}.type
    }

    @TypeConverter
    fun fromReceiptItemList(value: List<ReceiptItem>): String {
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toReceiptItemList(value: String): List<ReceiptItem> {
        return gson.fromJson(value, type)
    }
}
