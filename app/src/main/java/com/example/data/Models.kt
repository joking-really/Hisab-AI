package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val code: String,
    val name: String,
    val type: String, // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
    val balance: Double = 0.0
)

@Entity(tableName = "customers")
data class CustomerEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val shopName: String,
    val phone: String,
    val address: String,
    val creditLimit: Double = 50000.0,
    val paymentTermsDays: Int = 30,
    val runningBalance: Double = 0.0,
    val riskScore: Double = 3.0 // 1-10 risk rating
)

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val sku: String,
    val name: String,
    val brand: String,
    val category: String,
    val variant: String,
    val unitCost: Double,
    val salePrice: Double,
    val reorderLevel: Int = 5,
    val showroomQty: Int = 0,
    val godownQty: Int = 0,
    val photoPath: String? = null
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entryNumber: String, // JE-YYYY-MM-#####
    val date: Long, // timestamp
    val description: String,
    val refType: String, // SALE, PAYMENT, EXPENSE, PURCHASE, STOCK_IN, TRANSFER
    val debitAccountCode: String,
    val creditAccountCode: String,
    val amount: Double,
    val customerId: Int? = null,
    val customerName: String? = null,
    val productSku: String? = null,
    val productName: String? = null,
    val quantity: Int? = null,
    val isSynced: Boolean = false
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val productSku: String,
    val productName: String,
    val date: Long,
    val type: String, // IN, OUT, TRANSFER, ADJUSTMENT
    val quantity: Int,
    val fromLocation: String, // SHOWROOM, GODOWN, NONE
    val toLocation: String, // SHOWROOM, GODOWN, NONE
    val reason: String
)
