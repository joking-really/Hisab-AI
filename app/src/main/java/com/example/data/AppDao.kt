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
            CASE 
                WHEN a.type IN ('ASSET', 'EXPENSE') THEN (COALESCE(SUM(jl.debit), 0.0) - COALESCE(SUM(jl.credit), 0.0))
                ELSE (COALESCE(SUM(jl.credit), 0.0) - COALESCE(SUM(jl.debit), 0.0))
            END as balance,
            a.nameUrdu
        FROM accounts a
        LEFT JOIN journal_lines jl ON a.code = jl.accountCode
        LEFT JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE (:fromDate IS NULL OR je.date IS NULL OR je.date >= :fromDate)
        AND (:toDate IS NULL OR je.date IS NULL OR je.date <= :toDate)
        GROUP BY a.code
    """)
    fun getTrialBalance(fromDate: Long?, toDate: Long?): Flow<List<AccountBalanceView>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccount(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAccounts(accounts: List<AccountEntity>)

    // Customers
    @Query("SELECT * FROM customers ORDER BY name ASC")
    fun getAllCustomers(): Flow<List<CustomerEntity>>

    @Query("""
        SELECT c.*, 
            COALESCE(
                (SELECT SUM(jl2.debit) - SUM(jl2.credit)
                 FROM journal_lines jl2
                 JOIN journal_entries je2 ON jl2.journalEntryId = je2.id
                 WHERE je2.customerId = c.id AND jl2.accountCode = '1200'
                ), 0.0
            ) as runningBalance
        FROM customers c
        ORDER BY c.name ASC
    """)
    fun getAllCustomersWithBalance(): Flow<List<CustomerEntity>>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getCustomerById(id: Int): CustomerEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertCustomer(customer: CustomerEntity): Long

    // Products
    @Query("SELECT * FROM products ORDER BY name ASC")
    fun getAllProducts(): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE sku = :sku")
    suspend fun getProductBySku(sku: String): ProductEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProduct(product: ProductEntity)

    @Query("UPDATE products SET showroomQty = showroomQty + :showroomDelta, godownQty = godownQty + :godownDelta WHERE sku = :sku")
    suspend fun updateProductStock(sku: String, showroomDelta: Int, godownDelta: Int)

    // Journal Entries
    @Query("SELECT * FROM journal_entries ORDER BY date DESC, id DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("""
        SELECT 
            je.*,
            COALESCE(SUM(jl.debit), 0.0) as amount,
            (SELECT accountCode FROM journal_lines WHERE journalEntryId = je.id AND debit > 0 LIMIT 1) as debitAccountCode,
            (SELECT accountCode FROM journal_lines WHERE journalEntryId = je.id AND credit > 0 LIMIT 1) as creditAccountCode
        FROM journal_entries je
        LEFT JOIN journal_lines jl ON jl.journalEntryId = je.id
        GROUP BY je.id
        ORDER BY je.date DESC, je.id DESC
    """)
    fun getAllJournalEntriesWithLineSummaries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getJournalEntryById(id: Long): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Query("SELECT COUNT(*) FROM journal_entries WHERE entryNumber LIKE :prefix || '-%'")
    suspend fun countJournalEntriesInMonth(prefix: String): Int

    // Journal Lines
    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :entryId")
    suspend fun getJournalLinesByEntryId(entryId: Long): List<JournalLineEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalLine(line: JournalLineEntity): Long

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertJournalLines(lines: List<JournalLineEntity>)

    // Sales
    @Query("SELECT * FROM sales ORDER BY date DESC")
    fun getAllSales(): Flow<List<SaleEntity>>

    @Query("SELECT * FROM sales WHERE id = :id")
    suspend fun getSaleById(id: Long): SaleEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSale(sale: SaleEntity): Long

    @Query("SELECT COUNT(*) FROM sales WHERE parchiNumber LIKE :prefix || '-%'")
    suspend fun countSalesInMonth(prefix: String): Int

    @Query("SELECT * FROM sales WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getSalesInRange(startDate: Long, endDate: Long): Flow<List<SaleEntity>>

    // Sale Items
    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    suspend fun getSaleItemsBySaleId(saleId: Long): List<SaleItemEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSaleItems(items: List<SaleItemEntity>)

    // Payments
    @Query("SELECT * FROM payments ORDER BY date DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Query("SELECT * FROM payments WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getPaymentsInRange(startDate: Long, endDate: Long): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertPayment(payment: PaymentEntity): Long

    @Query("SELECT COUNT(*) FROM payments WHERE paymentNumber LIKE :prefix || '-%'")
    suspend fun countPaymentsInMonth(prefix: String): Int

    @Query("""
        SELECT 
            COALESCE(SUM(jl.debit), 0.0) - COALESCE(SUM(jl.credit), 0.0) as balance
        FROM journal_lines jl
        JOIN journal_entries je ON jl.journalEntryId = je.id
        WHERE je.customerId = :customerId 
        AND jl.accountCode = '1200'
    """)
    fun getCustomerBalanceFromJournal(customerId: Int): Double

    @Query("SELECT * FROM journal_lines WHERE journalEntryId = :entryId")
    fun getJournalLinesForEntry(entryId: Long): List<JournalLineEntity>

    // Stock Movements
    @Query("SELECT * FROM stock_movements ORDER BY date DESC")
    fun getAllStockMovements(): Flow<List<StockMovementEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertStockMovement(movement: StockMovementEntity)

    // Atomic sequence counter for invoice numbering
    @Query("SELECT next_value FROM sequence_counters WHERE prefix = :prefix")
    suspend fun getNextSequence(prefix: String): Int?

    @Query("INSERT OR REPLACE INTO sequence_counters (prefix, next_value) VALUES (:prefix, COALESCE((SELECT next_value + 1 FROM sequence_counters WHERE prefix = :prefix), 1))")
    suspend fun incrementSequence(prefix: String)

    // Transaction Wrappers
    @Transaction
    suspend fun insertSaleTransaction(
        sale: SaleEntity,
        items: List<SaleItemEntity>,
        journalEntry: JournalEntryEntity,
        journalLines: List<JournalLineEntity>,
        stockMovements: List<StockMovementEntity>,
        stockUpdates: List<StockUpdate>  // NEW parameter
    ): Long {
        val entryId = insertJournalEntry(journalEntry)
        val finalSale = sale.copy(journalEntryId = entryId)
        val saleId = insertSale(finalSale)
        
        insertSaleItems(items.map { it.copy(saleId = saleId) })
        insertJournalLines(journalLines.map { it.copy(journalEntryId = entryId) })
        stockMovements.forEach { insertStockMovement(it) }
        
        // Apply stock updates INSIDE the transaction
        for (update in stockUpdates) {
            updateProductStock(update.sku, update.showroomDelta, update.godownDelta)
        }
        
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
