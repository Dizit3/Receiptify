package com.example.financetracker

import com.example.financetracker.data.Transaction
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ParsingTest {

    @Test
    fun testTransactionParsing() {
        val json = """
            {
                "shop_name": "Test Store",
                "date": "2023-10-27",
                "total_amount": 123.45,
                "items": [
                    {
                        "name": "Item 1",
                        "price": 10.0
                    },
                    {
                        "name": "Item 2",
                        "price": 113.45
                    }
                ]
            }
        """.trimIndent()

        val gson = Gson()
        val transaction = gson.fromJson(json, Transaction::class.java)

        assertEquals("Test Store", transaction.shopName)
        assertEquals("2023-10-27", transaction.date)
        assertEquals(123.45, transaction.totalAmount, 0.001)
        assertEquals(2, transaction.items.size)
        assertEquals("Item 1", transaction.items[0].name)
        assertEquals(10.0, transaction.items[0].price, 0.001)
    }
}
