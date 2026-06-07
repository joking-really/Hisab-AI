package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class SaleItem(
    val productSku: String,
    val productName: String,
    val quantity: Int,
    val salePriceOnParchi: Double,
    val costPriceAtTime: Double,
    val fromLocation: String // SHOWROOM, GODOWN
)

class AppRepository(private val appDao: AppDao) {

    val allAccounts: Flow<List<AccountEntity>> = appDao.getAllAccounts()
    val allCustomers: Flow<List<CustomerEntity>> = appDao.getAllCustomersWithBalance()
    val allProducts: Flow<List<ProductEntity>> = appDao.getAllProducts()
    val allJournalEntries: Flow<List<JournalEntryEntity>> = appDao.getAllJournalEntriesWithLineSummaries()
    val allStockMovements: Flow<List<StockMovementEntity>> = appDao.getAllStockMovements()
    val allSales: Flow<List<SaleEntity>> = appDao.getAllSales()
    val allPayments: Flow<List<PaymentEntity>> = appDao.getAllPayments()

    private suspend fun validateTrialBalance(message: String) {
        val accounts = appDao.getTrialBalance(null, null).first()
        val totalDr = accounts.filter { it.type in listOf("ASSET", "EXPENSE") }.sumOf { it.balance }
        val totalCr = accounts.filter { it.type in listOf("LIABILITY", "EQUITY", "INCOME") }.sumOf { it.balance }
        val diff = kotlin.math.abs(totalDr - totalCr)
        if (diff > 0.01) {
            throw IllegalStateException("CRITICAL [$message]: Trial balance unbalanced! Dr=$totalDr Cr=$totalCr Diff=$diff")
        }
    }

    suspend fun getProductBySku(sku: String): ProductEntity? = appDao.getProductBySku(sku)

    fun getTrialBalance(fromDate: Long?, toDate: Long?): Flow<List<AccountBalanceView>> = 
        appDao.getTrialBalance(fromDate, toDate)

    suspend fun preseedAccounts() {
        val current = allAccounts.first()
        if (current.isEmpty()) {
            val systemAccounts = listOf(
                // ASSETS
                AccountEntity("1000", "Cash on Hand", "ASSET", "نقد کیش"),
                AccountEntity("1010", "Bank Account", "ASSET", "بینک اکاؤنٹ"),
                AccountEntity("1020", "Easypaisa/JazzCash Wallet", "ASSET", "ایزی پیسہ والٹ"),
                AccountEntity("1100", "Inventory — Showroom", "ASSET", "انوینٹری شو روم"),
                AccountEntity("1110", "Inventory — Godown", "ASSET", "انوینٹری گودام"),
                AccountEntity("1200", "Accounts Receivable (Khata)", "ASSET", "کھاتہ وصولی"),
                
                // LIABILITIES
                AccountEntity("2000", "Accounts Payable", "LIABILITY", "کھاتہ ادائیگی"),
                AccountEntity("2100", "Short-term Loans", "LIABILITY", "مختصر قرضے"),
                
                // EQUITY
                AccountEntity("3000", "Owner's Capital", "EQUITY", "سرمایہ مالک"),
                AccountEntity("3100", "Retained Earnings", "EQUITY", "منافع جمع شدہ"),
                AccountEntity("3900", "Stock Adjustment Reserve", "EQUITY", "اسٹاک ایڈجسٹمنٹ"),
                
                // INCOME
                AccountEntity("4000", "Sales Revenue", "INCOME", "آمدنی فروخت"),
                AccountEntity("4100", "Sales Returns", "INCOME", "واپسی فروخت"),
                
                // EXPENSES
                AccountEntity("5000", "Cost of Goods Sold (COGS)", "EXPENSE", "فروخت شدہ مال کی لاگت"),
                AccountEntity("5100", "Shop Rent", "EXPENSE", "دکان کا کرایہ"),
                AccountEntity("5200", "Utilities", "EXPENSE", "بل بجلی و گیس"),
                AccountEntity("5300", "Transport/Delivery", "EXPENSE", "ٹرانسپورٹ و ترسیل"),
                AccountEntity("5400", "Staff Salaries", "EXPENSE", "تنخواہ ملازمین")
            )
            appDao.insertAccounts(systemAccounts)
            
            // Seed initial journal lines to represent opening balances in line-based DB
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val jePrefix = "JE-${year}-${month.toString().padStart(2, '0')}"
            val jeNum = "00001"
            val entryNumber = "$jePrefix-$jeNum"

            val entry = JournalEntryEntity(
                entryNumber = entryNumber,
                date = System.currentTimeMillis(),
                description = "Opening Balance Setup",
                refType = "ADJUSTMENT"
            )
            
            // Setting up opening cash, bank, inventory on Dr versus capital on Cr
            val lines = listOf(
                JournalLineEntity(journalEntryId = 0, accountCode = "1000", debit = 50000.0, credit = 0.0, description = "Opening cash asset"),
                JournalLineEntity(journalEntryId = 0, accountCode = "1010", debit = 150000.0, credit = 0.0, description = "Opening bank asset"),
                JournalLineEntity(journalEntryId = 0, accountCode = "1020", debit = 15000.0, credit = 0.0, description = "Opening wallet asset"),
                JournalLineEntity(journalEntryId = 0, accountCode = "1100", debit = 200000.0, credit = 0.0, description = "Opening showroom stock value"),
                JournalLineEntity(journalEntryId = 0, accountCode = "1110", debit = 500000.0, credit = 0.0, description = "Opening godown stock value"),
                JournalLineEntity(journalEntryId = 0, accountCode = "3000", debit = 0.0, credit = 915000.0, description = "Opening equity capital")
            )

            appDao.insertGenericTransaction(entry, lines)
        }
    }

    suspend fun addCustomer(name: String, shopName: String, phone: String, address: String, creditLimit: Double, terms: Int): Long {
        val customer = CustomerEntity(
            name = name,
            shopName = shopName,
            phone = phone,
            address = address,
            creditLimit = creditLimit,
            paymentTermsDays = terms
        )
        return appDao.insertCustomer(customer)
    }

    suspend fun addProduct(
        sku: String,
        name: String,
        brand: String,
        category: String,
        variant: String,
        unitCost: Double,
        salePrice: Double,
        reorderLevel: Int,
        initShowroomQty: Int,
        initGodownQty: Int
    ) {
        require(salePrice >= unitCost) { 
            "Warning: Sale price (Rs. $salePrice) is below unit cost (Rs. $unitCost). This product will sell at a loss." 
        }

        val product = ProductEntity(
            id = 0,
            sku = sku,
            name = name,
            brand = brand,
            category = category,
            variant = variant,
            unitCost = unitCost,
            salePrice = salePrice,
            reorderLevel = reorderLevel,
            showroomQty = initShowroomQty,
            godownQty = initGodownQty
        )
        appDao.insertProduct(product)

        // Seed assets logic if quantities are supplied
        val costTotalShowroom = initShowroomQty * unitCost
        if (costTotalShowroom > 0) {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val jePrefix = "JE-${year}-${month.toString().padStart(2, '0')}"
            appDao.incrementSequence(jePrefix)
            val jeNum = appDao.getNextSequence(jePrefix)?.toString()?.padStart(5, '0') ?: "00001"
            val entryNumber = "$jePrefix-$jeNum"

            val entry = JournalEntryEntity(
                entryNumber = entryNumber,
                date = System.currentTimeMillis(),
                description = "Initial showroom stock for $name",
                refType = "STOCK_IN"
            )
            val lines = listOf(
                JournalLineEntity(journalEntryId = 0, accountCode = "1100", debit = costTotalShowroom, credit = 0.0, description = "Showroom inventory expansion for SKU: $sku"),
                JournalLineEntity(journalEntryId = 0, accountCode = "3000", debit = 0.0, credit = costTotalShowroom, description = "Capital investment injection for SKU: $sku")
            )
            appDao.insertGenericTransaction(entry, lines)
        }
        val costTotalGodown = initGodownQty * unitCost
        if (costTotalGodown > 0) {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH) + 1
            val jePrefix = "JE-${year}-${month.toString().padStart(2, '0')}"
            appDao.incrementSequence(jePrefix)
            val jeNum = appDao.getNextSequence(jePrefix)?.toString()?.padStart(5, '0') ?: "00001"
            val entryNumber = "$jePrefix-$jeNum"

            val entry = JournalEntryEntity(
                entryNumber = entryNumber,
                date = System.currentTimeMillis(),
                description = "Initial godown stock for $name",
                refType = "STOCK_IN"
            )
            val lines = listOf(
                JournalLineEntity(journalEntryId = 0, accountCode = "1110", debit = costTotalGodown, credit = 0.0, description = "Godown inventory expansion for SKU: $sku"),
                JournalLineEntity(journalEntryId = 0, accountCode = "3000", debit = 0.0, credit = costTotalGodown, description = "Capital investment injection for SKU: $sku")
            )
            appDao.insertGenericTransaction(entry, lines)
        }
        validateTrialBalance("AddProduct")
    }

    suspend fun recordSale(
        customerId: Int?,
        customerName: String?,
        items: List<SaleItem>,
        paymentType: String, // CASH or CREDIT
        paymentAccountCode: String // ONLY used when paymentType is CASH. Ignored for CREDIT (uses "1200")
    ): String {
        // Validation Layer
        require(items.isNotEmpty()) { "Sale must contain at least 1 item" }
        val totalRevenue = items.sumOf { it.quantity * it.salePriceOnParchi }
        require(totalRevenue > 0) { "Sale total must be greater than 0" }

        if (paymentType.uppercase() == "CREDIT") {
            require(customerId != null) { "Credit sale requires a customer" }
            val customer = appDao.getCustomerById(customerId)
            require(customer != null) { "Customer not found in database" }
            val currentBalance = appDao.getCustomerBalanceFromJournal(customerId)
            val projectedBalance = currentBalance + totalRevenue
            require(projectedBalance <= customer.creditLimit) { 
                "Credit limit exceeded. Limit: Rs. ${customer.creditLimit}, Projected: Rs. $projectedBalance" 
            }
        }

        // Validate stock beforehand
        for (item in items) {
            val product = appDao.getProductBySku(item.productSku)
            require(product != null) { "Product SKU ${item.productSku} not found" }
            val available = if (item.fromLocation.uppercase() == "SHOWROOM") product.showroomQty else product.godownQty
            require(available >= item.quantity) { 
                "Insufficient stock for ${product.name} at ${item.fromLocation}. Available: $available, Requested: ${item.quantity}" 
            }
        }

        // Double-check: no negative stock after this sale
        for (item in items) {
            val product = appDao.getProductBySku(item.productSku) ?: continue
            val finalShowroom = product.showroomQty + (if (item.fromLocation.uppercase() == "SHOWROOM") -item.quantity else 0)
            val finalGodown = product.godownQty + (if (item.fromLocation.uppercase() == "GODOWN") -item.quantity else 0)
            require(finalShowroom >= 0) { "Showroom stock for ${product.name} would go negative ($finalShowroom)" }
            require(finalGodown >= 0) { "Godown stock for ${product.name} would go negative ($finalGodown)" }
        }

        // Unique Parchi & Journal Entry numbers (collision-safe, sequential monthly)
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        
        val sPrefix = "PAR-${year}-${month.toString().padStart(2, '0')}"
        appDao.incrementSequence(sPrefix)
        val sNum = appDao.getNextSequence(sPrefix)?.toString()?.padStart(5, '0') ?: "00001"
        val parchiNumber = "$sPrefix-$sNum"

        val jePrefix = "JE-${year}-${month.toString().padStart(2, '0')}"
        appDao.incrementSequence(jePrefix)
        val jeNum = appDao.getNextSequence(jePrefix)?.toString()?.padStart(5, '0') ?: "00001"
        val journalEntryNumber = "$jePrefix-$jeNum"

        // Generate entities for saving
        val saleEntity = SaleEntity(
            parchiNumber = parchiNumber,
            customerId = customerId,
            date = System.currentTimeMillis(),
            totalAmount = totalRevenue,
            paymentType = paymentType.lowercase(),
            status = if (paymentType.uppercase() == "CREDIT") "current" else "paid"
        )

        val saleItemEntities = items.map { item ->
            SaleItemEntity(
                saleId = 0, // Filled in transaction
                productSku = item.productSku,
                quantity = item.quantity,
                unitPrice = item.salePriceOnParchi,
                totalPrice = item.quantity * item.salePriceOnParchi,
                location = item.fromLocation.lowercase()
            )
        }

        val debitAccount = if (paymentType.uppercase() == "CREDIT") "1200" else paymentAccountCode

        val journalEntry = JournalEntryEntity(
            entryNumber = journalEntryNumber,
            date = System.currentTimeMillis(),
            description = "Revenue sale to ${customerName ?: "Cash Walk-In"} | Parchi: $parchiNumber",
            refType = "SALE",
            customerId = customerId
        )

        // Make double entry ledger lines
        val journalLines = mutableListOf<JournalLineEntity>()
        
        // 1. Sale amount (Dr Cash/Receivable, Cr Revenue)
        journalLines.add(JournalLineEntity(
            journalEntryId = 0,
            accountCode = debitAccount,
            debit = totalRevenue,
            credit = 0.0,
            description = "Sales revenue billing: $parchiNumber"
        ))
        
        journalLines.add(JournalLineEntity(
            journalEntryId = 0,
            accountCode = "4000",
            debit = 0.0,
            credit = totalRevenue,
            description = "Sales revenue credit: $parchiNumber"
        ))

        // 2. Cost of Goods Sold & Inventory deduction (Dr COGS, Cr Inventory Asset)
        val totalCost = items.sumOf { it.quantity * it.costPriceAtTime }
        if (totalCost > 0) {
            journalLines.add(JournalLineEntity(
                journalEntryId = 0,
                accountCode = "5000",
                debit = totalCost,
                credit = 0.0,
                description = "COGS entry for Parchi: $parchiNumber"
            ))

            items.forEach { item ->
                val invAccount = if (item.fromLocation.uppercase() == "SHOWROOM") "1100" else "1110"
                journalLines.add(JournalLineEntity(
                    journalEntryId = 0,
                    accountCode = invAccount,
                    debit = 0.0,
                    credit = item.quantity * item.costPriceAtTime,
                    description = "Inventory Cr release: ${item.productName} [${item.quantity} pcs]"
                ))
            }
        }

        val stockMovements = items.map { item ->
            StockMovementEntity(
                productSku = item.productSku,
                productName = item.productName,
                date = System.currentTimeMillis(),
                type = "OUT",
                quantity = item.quantity,
                fromLocation = item.fromLocation.uppercase(),
                toLocation = "NONE",
                reason = "Sales Parchi $parchiNumber"
            )
        }

        val stockUpdates = items.map { item ->
            val showroomDelta = if (item.fromLocation.uppercase() == "SHOWROOM") -item.quantity else 0
            val godownDelta = if (item.fromLocation.uppercase() == "GODOWN") -item.quantity else 0
            StockUpdate(item.productSku, showroomDelta, godownDelta)
        }

        // All-or-nothing transaction: journal + sale + items + stock movements + stock updates
        appDao.insertSaleTransaction(
            sale = saleEntity,
            items = saleItemEntities,
            journalEntry = journalEntry,
            journalLines = journalLines,
            stockMovements = stockMovements,
            stockUpdates = stockUpdates
        )

        validateTrialBalance("Sale")

        return parchiNumber
    }

    suspend fun recordPayment(
        customerId: Int,
        customerName: String,
        amount: Double,
        methodAccountCode: String, // 1000 Cash, 1010 Bank, 1020 Esc
        notes: String
    ): String {
        require(amount > 0) { "Payment amount must be positive" }

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        val pmPrefix = "PM-$year-${month.toString().padStart(2, '0')}"
        appDao.incrementSequence(pmPrefix)
        val pmNum = appDao.getNextSequence(pmPrefix)?.toString()?.padStart(5, '0') ?: "00001"
        val paymentNumber = "$pmPrefix-$pmNum"

        val jePrefix = "JE-${year}-${month.toString().padStart(2, '0')}"
        appDao.incrementSequence(jePrefix)
        val jeNum = appDao.getNextSequence(jePrefix)?.toString()?.padStart(5, '0') ?: "00001"
        val journalEntryNumber = "$jePrefix-$jeNum"

        val paymentEntity = PaymentEntity(
            paymentNumber = paymentNumber,
            customerId = customerId,
            amount = amount,
            method = when (methodAccountCode) {
                "1000" -> "cash"
                "1010" -> "bankTransfer"
                "1020" -> "easypaisa"
                else -> "cash"
            },
            date = System.currentTimeMillis(),
            notes = notes
        )

        val journalEntry = JournalEntryEntity(
            entryNumber = journalEntryNumber,
            date = System.currentTimeMillis(),
            description = "Received Payment from $customerName | Notes: $notes",
            refType = "PAYMENT",
            customerId = customerId
        )

        // Dr Cash / Bank account, Cr Customer Accounts Receivable
        val journalLines = listOf(
            JournalLineEntity(
                journalEntryId = 0,
                accountCode = methodAccountCode,
                debit = amount,
                credit = 0.0,
                description = "Cash asset Dr via customer payment from $customerName"
            ),
            JournalLineEntity(
                journalEntryId = 0,
                accountCode = "1200",
                debit = 0.0,
                credit = amount,
                description = "Khata Cr balance deduction for customer $customerName"
            )
        )

        appDao.insertPaymentTransaction(paymentEntity, journalEntry, journalLines)

        validateTrialBalance("Payment")

        return paymentNumber
    }

    suspend fun recordExpense(
        expenseAccountCode: String, // 5100, 5200, 5300, 5400
        debitAccountName: String,
        amount: Double,
        paymentAccountCode: String, // 1000, 1010, 1020
        description: String
    ): String {
        require(amount > 0) { "Expense amount must be positive" }

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        val jePrefix = "JE-${year}-${month.toString().padStart(2, '0')}"
        appDao.incrementSequence(jePrefix)
        val jeNum = appDao.getNextSequence(jePrefix)?.toString()?.padStart(5, '0') ?: "00001"
        val journalEntryNumber = "$jePrefix-$jeNum"

        val journalEntry = JournalEntryEntity(
            entryNumber = journalEntryNumber,
            date = System.currentTimeMillis(),
            description = "Paid $debitAccountName | $description",
            refType = "EXPENSE"
        )

        val journalLines = listOf(
            JournalLineEntity(
                journalEntryId = 0,
                accountCode = expenseAccountCode,
                debit = amount,
                credit = 0.0,
                description = "Paid Expense Dr: $debitAccountName"
            ),
            JournalLineEntity(
                journalEntryId = 0,
                accountCode = paymentAccountCode,
                debit = 0.0,
                credit = amount,
                description = "Cash asset Cr reduction: $description"
            )
        )

        appDao.insertGenericTransaction(journalEntry, journalLines)

        validateTrialBalance("Expense")

        return journalEntryNumber
    }

    suspend fun recordStockTransfer(
        productSku: String,
        productName: String,
        quantity: Int,
        fromLocation: String,
        toLocation: String
    ) {
        val currentProduct = appDao.getProductBySku(productSku) ?: return
        val available = if (fromLocation.uppercase() == "SHOWROOM") currentProduct.showroomQty else currentProduct.godownQty
        require(available >= quantity) { 
            "Insufficient stock for $productName at $fromLocation to transfer $quantity. Available: $available" 
        }

        // Apply physical stock adjustments ONLY — no journal entry for physical moves
        val fromShowroomDelta = if (fromLocation.uppercase() == "SHOWROOM") -quantity else 0
        val fromGodownDelta = if (fromLocation.uppercase() == "GODOWN") -quantity else 0
        val toShowroomDelta = if (toLocation.uppercase() == "SHOWROOM") quantity else 0
        val toGodownDelta = if (toLocation.uppercase() == "GODOWN") quantity else 0

        appDao.updateProductStock(productSku, fromShowroomDelta + toShowroomDelta, fromGodownDelta + toGodownDelta)

        appDao.insertStockMovement(
            StockMovementEntity(
                productSku = productSku,
                productName = productName,
                date = System.currentTimeMillis(),
                type = "TRANSFER",
                quantity = quantity,
                fromLocation = fromLocation.uppercase(),
                toLocation = toLocation.uppercase(),
                reason = "Stock Transfer: $fromLocation → $toLocation"
            )
        )
    }

    suspend fun recordManualAdjustment(
        productSku: String,
        productName: String,
        quantity: Int,
        location: String, // SHOWROOM, GODOWN
        type: String, // IN, OUT
        reason: String
    ) {
        val currentProduct = appDao.getProductBySku(productSku) ?: return
        val cost = quantity * currentProduct.unitCost
        val account = if (location.uppercase() == "SHOWROOM") "1100" else "1110"

        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1

        val jePrefix = "JE-${year}-${month.toString().padStart(2, '0')}"
        appDao.incrementSequence(jePrefix)
        val jeNum = appDao.getNextSequence(jePrefix)?.toString()?.padStart(5, '0') ?: "00001"
        val journalEntryNumber = "$jePrefix-$jeNum"

        val journalEntry = JournalEntryEntity(
            entryNumber = journalEntryNumber,
            date = System.currentTimeMillis(),
            description = "Stock Adjustment $type: $reason",
            refType = "ADJUSTMENT"
        )

        val journalLines = if (type == "IN") {
            // GAIN (Dr Inventory, Cr Stock Adjustment equity — does NOT affect P&L)
            listOf(
                JournalLineEntity(journalEntryId = 0, accountCode = account, debit = cost, credit = 0.0, description = "Stock Adjust GAIN Dr: $location"),
                JournalLineEntity(journalEntryId = 0, accountCode = "3900", debit = 0.0, credit = cost, description = "Stock adjustment reserve Cr")
            )
        } else {
            // LOSS (Dr COGS expense, Cr Inventory reduction)
            val available = if (location.uppercase() == "SHOWROOM") currentProduct.showroomQty else currentProduct.godownQty
            require(available >= quantity) { "Cannot adjust out more than available stock ($available pcs)." }

            listOf(
                JournalLineEntity(journalEntryId = 0, accountCode = "5000", debit = cost, credit = 0.0, description = "COGS stock loss Dr: $reason"),
                JournalLineEntity(journalEntryId = 0, accountCode = account, debit = 0.0, credit = cost, description = "Stock Adjust LOSS Cr: $location")
            )
        }

        appDao.insertGenericTransaction(journalEntry, journalLines)

        val showroomDelta = if (location.uppercase() == "SHOWROOM") {
            if (type == "IN") quantity else -quantity
        } else 0
        val godownDelta = if (location.uppercase() == "GODOWN") {
            if (type == "IN") quantity else -quantity
        } else  0

        appDao.updateProductStock(productSku, showroomDelta, godownDelta)

        appDao.insertStockMovement(
            StockMovementEntity(
                productSku = productSku,
                productName = productName,
                date = System.currentTimeMillis(),
                type = "ADJUSTMENT",
                quantity = quantity,
                fromLocation = if (type == "OUT") location.uppercase() else "NONE",
                toLocation = if (type == "IN") location.uppercase() else "NONE",
                reason = "Inventory Adjustment: $reason"
            )
        )

        validateTrialBalance("Adjustment")
    }
}
