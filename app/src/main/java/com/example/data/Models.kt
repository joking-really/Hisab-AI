package com.example.data

import androidx.room.*

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val code: String,
    val name: String,
    val type: String, // ASSET, LIABILITY, EQUITY, INCOME, EXPENSE
    val nameUrdu: String? = null
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
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entryNumber: String, // JE-YYYY-MM-#####
    val date: Long = System.currentTimeMillis(),
    val description: String,
    val refType: String, // SALE, PAYMENT, EXPENSE, PURCHASE, STOCK_IN, TRANSFER, ADJUSTMENT
    val amount: Double = 0.0,
    val debitAccountCode: String = "",
    val creditAccountCode: String = "",
    val refId: Long? = null,
    val customerId: Int? = null
)

@Entity(
    tableName = "journal_lines",
    foreignKeys = [
        ForeignKey(
            entity = JournalEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["journalEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("journalEntryId"), Index("accountCode")]
)
data class JournalLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val journalEntryId: Long,
    val accountCode: String,      // e.g., "1000", "1200", "4000"
    val debit: Double,            // 0.0 if credit
    val credit: Double,           // 0.0 if debit
    val description: String? = null
)

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val parchiNumber: String,            // "PAR-2026-05-00001"
    val customerId: Int? = null,         // null = walk-in cash
    val date: Long = System.currentTimeMillis(),
    val totalAmount: Double,
    val paymentType: String,             // "cash" or "credit"
    val status: String = "current",     // current, paid, partial, overdue
    val dueDate: Long? = null,
    val journalEntryId: Long? = null,   // FK to journal_entries
    val notes: String? = null
)

@Entity(
    tableName = "sale_items",
    foreignKeys = [
        ForeignKey(
            entity = SaleEntity::class,
            parentColumns = ["id"],
            childColumns = ["saleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("saleId"), Index("productSku")]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productSku: String,
    val quantity: Int,
    val unitPrice: Double,
    val totalPrice: Double,
    val location: String               // "showroom" or "godown"
)

@Entity(tableName = "payments")
data class PaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerId: Int,
    val saleId: Long? = null,          // Optional: payment against specific sale
    val amount: Double,
    val method: String,                // "cash", "bankTransfer", "easypaisa", "jazzcash", "cheque"
    val date: Long = System.currentTimeMillis(),
    val notes: String? = null,
    val journalEntryId: Long? = null
)

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productSku: String,
    val productName: String,
    val date: Long,
    val type: String, // IN, OUT, TRANSFER, ADJUSTMENT
    val quantity: Int,
    val fromLocation: String, // SHOWROOM, GODOWN, NONE
    val toLocation: String, // SHOWROOM, GODOWN, NONE
    val reason: String
)

data class AccountBalanceView(
    val code: String,
    val name: String,
    val type: String,
    val totalDebit: Double,
    val totalCredit: Double,
    val balance: Double
)
