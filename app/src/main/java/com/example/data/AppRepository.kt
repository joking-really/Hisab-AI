package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.UUID

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
    val allCustomers: Flow<List<CustomerEntity>> = appDao.getAllCustomers()
    val allProducts: Flow<List<ProductEntity>> = appDao.getAllProducts()
    val allJournalEntries: Flow<List<JournalEntryEntity>> = appDao.getAllJournalEntries()
    val allStockMovements: Flow<List<StockMovementEntity>> = appDao.getAllStockMovements()

    suspend fun getProductBySku(sku: String): ProductEntity? = appDao.getProductBySku(sku)

    suspend fun preseedAccounts() {
        val current = allAccounts.first()
        if (current.isEmpty()) {
            val systemAccounts = listOf(
                // ASSETS
                AccountEntity("1000", "Cash on Hand", "ASSET", 50000.0),
                AccountEntity("1010", "Bank Account", "ASSET", 150000.0),
                AccountEntity("1020", "Easypaisa/JazzCash Wallet", "ASSET", 15000.0),
                AccountEntity("1100", "Inventory — Showroom", "ASSET", 200000.0),
                AccountEntity("1110", "Inventory — Godown", "ASSET", 500000.0),
                AccountEntity("1200", "Accounts Receivable (Khata)", "ASSET", 0.0),
                
                // LIABILITIES
                AccountEntity("2000", "Accounts Payable", "LIABILITY", 0.0),
                AccountEntity("2100", "Short-term Loans", "LIABILITY", 0.0),
                
                // EQUITY
                AccountEntity("3000", "Owner's Capital", "EQUITY", 915000.0),
                AccountEntity("3100", "Retained Earnings", "EQUITY", 0.0),
                
                // INCOME
                AccountEntity("4000", "Sales Revenue", "INCOME", 0.0),
                AccountEntity("4100", "Sales Returns", "INCOME", 0.0),
                
                // EXPENSES
                AccountEntity("5000", "Cost of Goods Sold (COGS)", "EXPENSE", 0.0),
                AccountEntity("5100", "Shop Rent", "EXPENSE", 0.0),
                AccountEntity("5200", "Utilities", "EXPENSE", 0.0),
                AccountEntity("5300", "Transport/Delivery", "EXPENSE", 0.0),
                AccountEntity("5400", "Staff Salaries", "EXPENSE", 0.0)
            )
            appDao.insertAccounts(systemAccounts)
        }
    }

    suspend fun addCustomer(name: String, shopName: String, phone: String, address: String, creditLimit: Double, terms: Int): Long {
        val customer = CustomerEntity(
            name = name,
            shopName = shopName,
            phone = phone,
            address = address,
            creditLimit = creditLimit,
            paymentTermsDays = terms,
            runningBalance = 0.0
        )
        return appDao.insertCustomer(customer)
    }

    suspend fun addProduct(sku: String, name: String, brand: String, category: String, variant: String, unitCost: Double, salePrice: Double, reorderLevel: Int, initShowroomQty: Int, initGodownQty: Int) {
        val product = ProductEntity(
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
            appDao.updateAccountBalance("1100", costTotalShowroom)
            val jeCode = "ST-${System.currentTimeMillis() % 10000}"
            appDao.insertJournalEntry(
                JournalEntryEntity(
                    entryNumber = jeCode,
                    date = System.currentTimeMillis(),
                    description = "Initial showroom stock for ${name}",
                    refType = "STOCK_IN",
                    debitAccountCode = "1100",
                    creditAccountCode = "3000", // owner capital investment
                    amount = costTotalShowroom,
                    productSku = sku,
                    productName = name,
                    quantity = initShowroomQty
                )
            )
        }
        val costTotalGodown = initGodownQty * unitCost
        if (costTotalGodown > 0) {
            appDao.updateAccountBalance("1110", costTotalGodown)
            val jeCode = "ST-${System.currentTimeMillis() % 10000}"
            appDao.insertJournalEntry(
                JournalEntryEntity(
                    entryNumber = jeCode,
                    date = System.currentTimeMillis(),
                    description = "Initial godown stock for ${name}",
                    refType = "STOCK_IN",
                    debitAccountCode = "1110",
                    creditAccountCode = "3000",
                    amount = costTotalGodown,
                    productSku = sku,
                    productName = name,
                    quantity = initGodownQty
                )
            )
        }
    }

    suspend fun recordSale(
        customerId: Int?,
        customerName: String?,
        items: List<SaleItem>,
        paymentType: String, // CASH, CREDIT
        paymentAccountCode: String // 1000, 1010, 1020 (used if CASH)
    ): String {
        val totalRevenue = items.sumOf { it.quantity * it.salePriceOnParchi }
        val uniqueTag = System.currentTimeMillis() % 100000
        val parchiNumber = "SL-2026-${String.format("%05d", uniqueTag)}"

        // DEBIT account: 1200 if Credit (accounts receivable), or Cash / Bank if CASH
        val debitAccount = if (paymentType == "CREDIT") "1200" else paymentAccountCode
        val creditAccount = "4000" // Sales Revenue

        // 1. Log Sales Journal Entry
        val salesJournalId = appDao.insertJournalEntry(
            JournalEntryEntity(
                entryNumber = parchiNumber,
                date = System.currentTimeMillis(),
                description = "Revenue sale to ${customerName ?: "Cash Walk-In"} | Parchi: $parchiNumber",
                refType = "SALE",
                debitAccountCode = debitAccount,
                creditAccountCode = creditAccount,
                amount = totalRevenue,
                customerId = customerId,
                customerName = customerName
            )
        )

        // Update account balances
        appDao.updateAccountBalance(debitAccount, totalRevenue)
        appDao.updateAccountBalance(creditAccount, totalRevenue)

        if (paymentType == "CREDIT" && customerId != null) {
            appDao.updateCustomerBalance(customerId, totalRevenue)
        }

        // 2. Perform inventory deduction & Record Expense COGS
        for (item in items) {
            val productCost = item.quantity * item.costPriceAtTime
            val invAccount = if (item.fromLocation == "SHOWROOM") "1100" else "1110"
            
            // Log COGS debit & Inventory credit journal entry
            appDao.insertJournalEntry(
                JournalEntryEntity(
                    entryNumber = parchiNumber,
                    date = System.currentTimeMillis(),
                    description = "COGS entry for ${item.productName} (${item.quantity} pcs from ${item.fromLocation})",
                    refType = "SALE",
                    debitAccountCode = "5000", // COGS Expense
                    creditAccountCode = invAccount, // Showroom/Godown Inventory Asset
                    amount = productCost,
                    productSku = item.productSku,
                    productName = item.productName,
                    quantity = item.quantity
                )
            )

            // Update product stock balance
            val showroomDelta = if (item.fromLocation == "SHOWROOM") -item.quantity else 0
            val godownDelta = if (item.fromLocation == "GODOWN") -item.quantity else 0
            appDao.updateProductStock(item.productSku, showroomDelta, godownDelta)

            // Log corresponding stock movement
            appDao.insertStockMovement(
                StockMovementEntity(
                    productSku = item.productSku,
                    productName = item.productName,
                    date = System.currentTimeMillis(),
                    type = "OUT",
                    quantity = item.quantity,
                    fromLocation = item.fromLocation,
                    toLocation = "NONE",
                    reason = "Sales Parchi $parchiNumber"
                )
            )

            // Update Account Balances
            appDao.updateAccountBalance("5000", productCost) // Increases expense
            appDao.updateAccountBalance(invAccount, -productCost) // Decreases asset
        }

        return parchiNumber
    }

    suspend fun recordPayment(
        customerId: Int,
        customerName: String,
        amount: Double,
        methodAccountCode: String, // 1000 Cash, 1010 Bank, 1020 Esc
        notes: String
    ): String {
        val uniqueTag = System.currentTimeMillis() % 100000
        val paymentNumber = "PM-2026-${String.format("%05d", uniqueTag)}"

        // DEBIT cash/bank (+asset), CREDIT accounts receivable (-asset)
        appDao.insertJournalEntry(
            JournalEntryEntity(
                entryNumber = paymentNumber,
                date = System.currentTimeMillis(),
                description = "Received Payment from $customerName via ${getAccountLabelLabel(methodAccountCode)} | Notes: $notes",
                refType = "PAYMENT",
                debitAccountCode = methodAccountCode,
                creditAccountCode = "1200",
                amount = amount,
                customerId = customerId,
                customerName = customerName
            )
        )

        // Balances adjustments
        appDao.updateAccountBalance(methodAccountCode, amount) // Increases asset
        appDao.updateAccountBalance("1200", -amount) // Decreases outstanding A/R
        appDao.updateCustomerBalance(customerId, -amount) // Decreases customer debt

        return paymentNumber
    }

    suspend fun recordExpense(
        expenseAccountCode: String, // 5100, 5200, 5300, 5400
        debitAccountName: String,
        amount: Double,
        paymentAccountCode: String, // 1000, 1010, 1020
        description: String
    ): String {
        val uniqueTag = System.currentTimeMillis() % 100000
        val code = "EXP-2026-${String.format("%05d", uniqueTag)}"

        // DEBIT Expense (+Expense), CREDIT Cash (-Asset)
        appDao.insertJournalEntry(
            JournalEntryEntity(
                entryNumber = code,
                date = System.currentTimeMillis(),
                description = "Paid $debitAccountName | $description",
                refType = "EXPENSE",
                debitAccountCode = expenseAccountCode,
                creditAccountCode = paymentAccountCode,
                amount = amount
            )
        )

        appDao.updateAccountBalance(expenseAccountCode, amount) // Increases expense
        appDao.updateAccountBalance(paymentAccountCode, -amount) // Decreases asset

        return code
    }

    suspend fun recordStockTransfer(
        productSku: String,
        productName: String,
        quantity: Int,
        fromLocation: String, // GODOWN
        toLocation: String // SHOWROOM
    ) {
        val currentProduct = appDao.getProductBySku(productSku) ?: return
        val transferCost = quantity * currentProduct.unitCost

        val fromAccount = if (fromLocation == "SHOWROOM") "1100" else "1110"
        val toAccount = if (toLocation == "SHOWROOM") "1100" else "1110"

        val uniqueTag = System.currentTimeMillis() % 100000
        val code = "TR-2026-${String.format("%05d", uniqueTag)}"

        // DEBIT toAccount (+Asset), CREDIT fromAccount (-Asset)
        appDao.insertJournalEntry(
            JournalEntryEntity(
                entryNumber = code,
                date = System.currentTimeMillis(),
                description = "Stock Transfer of $quantity pcs $productName from $fromLocation to $toLocation",
                refType = "TRANSFER",
                debitAccountCode = toAccount,
                creditAccountCode = fromAccount,
                amount = transferCost,
                productSku = productSku,
                productName = productName,
                quantity = quantity
            )
        )

        // Adjust stock variables
        val fromShowroomDelta = if (fromLocation == "SHOWROOM") -quantity else 0
        val fromGodownDelta = if (fromLocation == "GODOWN") -quantity else 0
        val toShowroomDelta = if (toLocation == "SHOWROOM") quantity else 0
        val toGodownDelta = if (toLocation == "GODOWN") quantity else 0

        appDao.updateProductStock(productSku, fromShowroomDelta + toShowroomDelta, fromGodownDelta + toGodownDelta)

        // Adjust Accounts value
        appDao.updateAccountBalance(toAccount, transferCost)
        appDao.updateAccountBalance(fromAccount, -transferCost)

        // Log double movement entries or transfer log
        appDao.insertStockMovement(
            StockMovementEntity(
                productSku = productSku,
                productName = productName,
                date = System.currentTimeMillis(),
                type = "TRANSFER",
                quantity = quantity,
                fromLocation = fromLocation,
                toLocation = toLocation,
                reason = "Internal Stock Transfer $code"
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
        val account = if (location == "SHOWROOM") "1100" else "1110"

        val uniqueTag = System.currentTimeMillis() % 100000
        val code = "ADJ-2026-${String.format("%05d", uniqueTag)}"

        if (type == "IN") {
            // DEBIT Inventory Asset, CREDIT Owner Equity (injection)
            appDao.insertJournalEntry(
                JournalEntryEntity(
                    entryNumber = code,
                    date = System.currentTimeMillis(),
                    description = "Stock Adjustment IN: $reason",
                    refType = "ADJUSTMENT",
                    debitAccountCode = account,
                    creditAccountCode = "3000",
                    amount = cost,
                    productSku = productSku,
                    productName = productName,
                    quantity = quantity
                )
            )
            appDao.updateAccountBalance(account, cost)
            appDao.updateAccountBalance("3000", cost)

            val showroomDelta = if (location == "SHOWROOM") quantity else 0
            val godownDelta = if (location == "GODOWN") quantity else 0
            appDao.updateProductStock(productSku, showroomDelta, godownDelta)
        } else {
            // Loss! DEBIT Expense COGS (or custom loss account if mapped, let's use COGS 5000), CREDIT Inventory Asset
            appDao.insertJournalEntry(
                JournalEntryEntity(
                    entryNumber = code,
                    date = System.currentTimeMillis(),
                    description = "Stock Adjustment OUT (Loss): $reason",
                    refType = "ADJUSTMENT",
                    debitAccountCode = "5000",
                    creditAccountCode = account,
                    amount = cost,
                    productSku = productSku,
                    productName = productName,
                    quantity = quantity
                )
            )
            appDao.updateAccountBalance("5000", cost)
            appDao.updateAccountBalance(account, -cost)

            val showroomDelta = if (location == "SHOWROOM") -quantity else 0
            val godownDelta = if (location == "GODOWN") -quantity else 0
            appDao.updateProductStock(productSku, showroomDelta, godownDelta)
        }

        appDao.insertStockMovement(
            StockMovementEntity(
                productSku = productSku,
                productName = productName,
                date = System.currentTimeMillis(),
                type = "ADJUSTMENT",
                quantity = quantity,
                fromLocation = if (type == "OUT") location else "NONE",
                toLocation = if (type == "IN") location else "NONE",
                reason = "Inventory Adjustment: $reason"
            )
        )
    }

    private fun getAccountLabelLabel(code: String): String {
        return when (code) {
            "1000" -> "Cash on Hand"
            "1010" -> "Bank Account"
            "1020" -> "Easypaisa/JazzCash"
            else -> "Account $code"
        }
    }
}
