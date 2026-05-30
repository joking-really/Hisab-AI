package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Accounts
    @Query("SELECT * FROM accounts ORDER BY code ASC")
    fun getAllAccounts(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE code = :code")
    suspend fun getAccountByCode(code: String): AccountEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    @Query("UPDATE accounts SET balance = balance + :amount WHERE code = :code")
    suspend fun updateAccountBalance(code: String, amount: Double)

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    @Query("UPDATE customers SET runningBalance = runningBalance + :amount WHERE id = :id")
    suspend fun updateCustomerBalance(id: Int, amount: Double)

    // Products
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE sku = :sku")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)

    @Query("UPDATE products SET showroomQty = showroomQty + :showroomDelta, godownQty = godownQty + :godownDelta WHERE sku = :sku")
    suspend fun updateProductStock(sku: String, showroomDelta: Int, godownDelta: Int)

    // Journal Entries
    @Query("SELECT * FROM journal_entries ORDER BY date DESC, id DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    // Stock Movements
    @Query("SELECT * FROM stock_movements ORDER BY date DESC")
    fun getAllStockMovements(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)
}
