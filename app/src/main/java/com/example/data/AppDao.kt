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

    @Query("""
        SELECT 
            a.code, a.name, a.type,
            COALESCE(SUM(jl.debit), 0.0) as totalDebit,
            COALESCE(SUM(jl.credit), 0.0) as totalCredit,
            (COALESCE(SUM(jl.debit), 0.0) - COALESCE(SUM(jl.credit), 0.0)) as balance,
            a.nameUrdu
        FROM accounts a
        LEFT JOIN journal_lines jl ON a.code = jl.accountCode
        LEFT JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE (:fromDate IS NULL OR je.date >= :fromDate)
        AND (:toDate IS NULL OR je.date <= :toDate)
        GROUP BY a.code
    """)
    fun getTrialBalance(fromDate: Long?, toDate: Long?): Flow<List<AccountBalanceView>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomer(customer: CustomerEntity): Long

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

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getJournalEntryById(id: Long): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Query("SELECT COUNT(*) FROM journal_entries WHERE entryNumber LIKE :prefix || '-%'")
    fun countJournalEntriesInMonth(prefix: String): Int

    // Journal Lines
    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :entryId")
    suspend fun getJournalLinesByEntryId(entryId: Long): List<JournalLineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLine(line: JournalLineEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalLines(lines: List<JournalLineEntity>)

    // Sales
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSale(sale: SaleEntity): Long

    @Query("SELECT COUNT(*) FROM sales WHERE parchiNumber LIKE :prefix || '-%'")
    fun countSalesInMonth(prefix: String): Int

    // Sale Items
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsBySaleId(saleId: Long): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    // Payments
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT COUNT(*) FROM payments WHERE paymentNumber LIKE :prefix || '-%'")
    fun countPaymentsInMonth(prefix: String): Int

    @Query("""
        SELECT 
            COALESCE(SUM(jl.debit), 0.0) - COALESCE(SUM(jl.credit), 0.0) as balance
        FROM journal_lines jl
        JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE je.customerId = :customerId 
        AND jl.accountCode = '1200'
        AND je.refType IN ('SALE', 'PAYMENT')
    """)
    fun getCustomerBalanceFromJournal(customerId: Int): Double

    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :entryId")
    fun getJournalLinesForEntry(entryId: Long): List<JournalLineEntity>

    // Stock Movements
    @Query("SELECT * FROM stock_movements ORDER BY date DESC")
    fun getAllStockMovements(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    // Transaction Wrappers
    @Transaction
    suspend fun insertSaleTransaction(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        journalEntry: JournalEntryEntity,
        journalLines: List<JournalLineEntity>,
        stockMovements: List<StockMovementEntity>
    ): Long {
        val entryId = insertJournalEntry(journalEntry)
        val finalSale = sale.copy(journalEntryId = entryId)
        val saleId = insertSale(finalSale)
        
        insertSaleItems(items.map { it.copy(saleId = saleId) })
        insertJournalLines(journalLines.map { it.copy(journalEntryId = entryId) })
        stockMovements.forEach { insertStockMovement(it) }
        
        return saleId
    }

    @Transaction
    suspend fun insertPaymentTransaction(
        payment: PaymentEntity,
        journalEntry: JournalEntryEntity,
        journalLines: List<JournalLineEntity>
    ): Long {
        val entryId = insertJournalEntry(journalEntry)
        val finalPayment = payment.copy(journalEntryId = entryId)
        val paymentId = insertPayment(finalPayment)
        
        insertJournalLines(journalLines.map { it.copy(journalEntryId = entryId) })
        return paymentId
    }

    @Transaction
    suspend fun insertGenericTransaction(
        journalEntry: JournalEntryEntity,
        journalLines: List<JournalLineEntity>
    ): Long {
        val entryId = insertJournalEntry(journalEntry)
        insertJournalLines(journalLines.map { it.copy(journalEntryId = entryId) })
        return entryId
    }
}
