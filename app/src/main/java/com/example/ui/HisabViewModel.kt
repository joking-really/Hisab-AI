package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GroqService
import com.example.data.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

// Navigation destinations inside State
enum class UIState {
    DASHBOARD,
    KHATA_LIST,
    CUSTOMER_DETAIL,
    PRODUCTS_GRID,
    ASSISTANT_CHAT,
    JOURNAL_ENTRIES,
    REPORTS,
    PARCHI_OCR
}

class HisabViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repo = AppRepository(db.appDao)
    private val groqService = GroqService()

    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents = _toastEvents.asSharedFlow()

    fun showToast(message: String) {
        viewModelScope.launch {
            _toastEvents.emit(message)
        }
    }

    // Database flows
    val accounts: StateFlow<List<AccountBalanceView>> = repo.getTrialBalance(null, null).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val customers: StateFlow<List<CustomerEntity>> = repo.allCustomers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val products: StateFlow<List<ProductEntity>> = repo.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val journalEntries: StateFlow<List<JournalEntryEntity>> = repo.allJournalEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val stockMovements: StateFlow<List<StockMovementEntity>> = repo.allStockMovements.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // UI state
    var currentScreen by mutableStateOf(UIState.DASHBOARD)
    var selectedCustomer by mutableStateOf<CustomerEntity?>(null)

    // OCR Scanning temporary hold
    var scannedBitmap by mutableStateOf<Bitmap?>(null)
    var isOcrProcessing by mutableStateOf(false)
    var ocrResultOcrJson by mutableStateOf<String?>(null)
    var ocrCustomerName by mutableStateOf("")
    var ocrPaymentType by mutableStateOf("CREDIT")
    var ocrItems by mutableStateOf<List<SaleItem>>(emptyList())
    var ocrTotal by mutableStateOf(0.0)
    var ocrConfidence by mutableStateOf(1.0)

    // Chatbot States
    var chatInputText by mutableStateOf("")
    private val _chatHistory = MutableStateFlow<List<Pair<String, Boolean>>>(
        listOf(
            Pair("Assalam-o-Alaikum! Main aapka **Hisab Assistant** hoon. \n\nMeri madad se aap udhar (Khata) record kar sakte hain, stock check kar sakte hain, ya aaj ka cash summary jaan sakte hain. \n\n*Puchiye jo aap chahein! Aur Roman Urdu use karein.*", false)
        )
    )
    val chatHistory: StateFlow<List<Pair<String, Boolean>>> = _chatHistory.asStateFlow()
    var isChatLoading by mutableStateOf(false)

    // Transaction action alert state (AI Function parsing)
    var showChatAddConfirmation by mutableStateOf(false)
    var chatSuggestedAction by mutableStateOf<JSONObject?>(null)

    // Quick transaction models
    var quickAddType by mutableStateOf<String?>(null) // "SALE", "PAYMENT", "EXPENSE", "TRANSFER"

    init {
        viewModelScope.launch {
            repo.preseedAccounts()
            preseedShowroomProductsAndCustomers()
        }
    }

    private suspend fun preseedShowroomProductsAndCustomers() {
        val currentPd = repo.allProducts.first()
        if (currentPd.isEmpty()) {
            repo.addProduct(
                sku = "TI-MARB-01",
                name = "Royal Glazed White Marble (12x12)",
                brand = "Master Tiles",
                category = "Tiles",
                variant = "Glazed White",
                unitCost = 800.0,
                salePrice = 1450.0,
                reorderLevel = 10,
                initShowroomQty = 15,
                initGodownQty = 80
            )
            repo.addProduct(
                sku = "TI-GOLD-02",
                name = "Century Gold Accent Tile Box",
                brand = "Century",
                category = "Tiles",
                variant = "Gold Floral",
                unitCost = 1500.0,
                salePrice = 2750.0,
                reorderLevel = 5,
                initShowroomQty = 5,
                initGodownQty = 35
            )
            repo.addProduct(
                sku = "SA-COMM-10",
                name = "Sonex Executive Commode Ivory",
                brand = "Sonex",
                category = "Sanitary",
                variant = "Ivory Ceramic",
                unitCost = 3500.0,
                salePrice = 7200.0,
                reorderLevel = 4,
                initShowroomQty = 3,
                initGodownQty = 18
            )
            repo.addProduct(
                sku = "SA-BASN-11",
                name = "Luxury Ceramic Wash Basin White",
                brand = "Master Tiles",
                category = "Sanitary",
                variant = "Standard",
                unitCost = 1800.0,
                salePrice = 3850.0,
                reorderLevel = 6,
                initShowroomQty = 8,
                initGodownQty = 24
            )
            repo.addProduct(
                sku = "SA-FITT-20",
                name = "SS Faisal Golden Mixer Set",
                brand = "Faisal Fittings",
                category = "Fittings",
                variant = "Gold Plated",
                unitCost = 2500.0,
                salePrice = 5200.0,
                reorderLevel = 5,
                initShowroomQty = 12,
                initGodownQty = 40
            )

            val c1Id = repo.addCustomer(
                name = "Ali Traders (Gulshan)",
                shopName = "Ali Tile Plaza",
                phone = "0321-4567890",
                address = "Block 4, Gulshan-e-Iqbal, Karachi",
                creditLimit = 80000.0,
                terms = 30
            )
            val c2Id = repo.addCustomer(
                name = "Karachi Sanitary Store (Saddar)",
                shopName = "Karachi Sanitary Emporium",
                phone = "0300-1234567",
                address = "Saddar Road, Saddar, Karachi",
                creditLimit = 150000.0,
                terms = 45
            )
            val c3Id = repo.addCustomer(
                name = "Imran & Sons (Liaquatabad)",
                shopName = "Imran Marble Mart",
                phone = "0312-9876543",
                address = "Super Market, Liaquatabad, Karachi",
                creditLimit = 60000.0,
                terms = 15
            )

            repo.recordSale(
                customerId = c1Id.toInt(),
                customerName = "Ali Traders (Gulshan)",
                items = listOf(
                    SaleItem("TI-MARB-01", "Royal Glazed White Marble (12x12)", 10, 1450.0, 800.0, "GODOWN"),
                    SaleItem("SA-BASN-11", "Luxury Ceramic Wash Basin White", 2, 3850.0, 1800.0, "SHOWROOM")
                ),
                paymentType = "CREDIT",
                paymentAccountCode = "1000"
            )

            repo.recordSale(
                customerId = c2Id.toInt(),
                customerName = "Karachi Sanitary Store (Saddar)",
                items = listOf(
                    SaleItem("SA-COMM-10", "Sonex Executive Commode Ivory", 4, 7200.0, 3500.0, "GODOWN"),
                    SaleItem("SA-FITT-20", "SS Faisal Golden Mixer Set", 5, 5200.0, 2500.0, "SHOWROOM")
                ),
                paymentType = "CREDIT",
                paymentAccountCode = "1000"
            )

            repo.recordPayment(
                customerId = c1Id.toInt(),
                customerName = "Ali Traders (Gulshan)",
                amount = 10000.0,
                methodAccountCode = "1000",
                notes = "Advance cash deposit at counter"
            )
        }
    }

    suspend fun executeTool(name: String, arguments: Map<String, Any>): String {
        return try {
            when (name) {
                "get_customer_balance" -> {
                    val nameQuery = arguments["name"] as? String ?: ""
                    val found = customers.value.find { 
                        it.name.contains(nameQuery, ignoreCase = true) || 
                        it.shopName.contains(nameQuery, ignoreCase = true) 
                    }
                    if (found == null) {
                        "Customer '$nameQuery' database mein nahi mila."
                    } else {
                        "Customer: ${found.name}\nShop: ${found.shopName}\nOutstanding Udhar: Rs. ${found.runningBalance}\nLimit: Rs. ${found.creditLimit}"
                    }
                }
                "get_stock_level" -> {
                    val nameQuery = arguments["name"] as? String ?: ""
                    val location = arguments["location"] as? String ?: ""
                    val found = products.value.find { 
                        it.name.contains(nameQuery, ignoreCase = true) || 
                        it.sku.equals(nameQuery, ignoreCase = true) 
                    }
                    if (found == null) {
                        "Product '$nameQuery' catalogues mein nahi mila."
                    } else {
                        val showroomVal = found.showroomQty
                        val godownVal = found.godownQty
                        if (location.lowercase() == "showroom") {
                            "${found.name} is available in Showroom: $showroomVal pcs (Price: Rs. ${found.salePrice})."
                        } else if (location.lowercase() == "godown") {
                            "${found.name} is available in Godown: $godownVal pcs (Price: Rs. ${found.salePrice})."
                        } else {
                            "${found.name} total stock:\n- Showroom: $showroomVal pcs\n- Godown: $godownVal pcs\n- Price: Rs. ${found.salePrice}"
                        }
                    }
                }
                "get_daily_summary" -> {
                    val salesList = repo.allSales.first()
                    val today = System.currentTimeMillis()
                    val calendarToday = Calendar.getInstance().apply { timeInMillis = today }
                    val salesToday = salesList.filter { sale ->
                        val calendarSale = Calendar.getInstance().apply { timeInMillis = sale.date }
                        calendarSale.get(Calendar.YEAR) == calendarToday.get(Calendar.YEAR) &&
                        calendarSale.get(Calendar.DAY_OF_YEAR) == calendarToday.get(Calendar.DAY_OF_YEAR)
                    }
                    val totalRevenue = salesToday.sumOf { it.totalAmount }
                    val count = salesToday.size
                    "Daily Sales Summary Today:\n- Total Sales Revenue: Rs. $totalRevenue\n- Quantity of Invoices issued today: $count"
                }
                "get_overdue_accounts" -> {
                    val list = customers.value.filter { it.runningBalance > 0 }
                    if (list.isEmpty()) {
                        "Sab clean hai! Koi overdue outstanding khata accounts nahi hain."
                    } else {
                        val sb = StringBuilder("Overdue Credit Customers:\n")
                        list.forEach { 
                            sb.append("- ${it.name}: Rs. ${it.runningBalance} [Max Limit: Rs. ${it.creditLimit}]\n")
                        }
                        sb.toString()
                    }
                }
                "get_cash_position" -> {
                    val accList = accounts.value
                    val cashOnHand = accList.find { it.code == "1000" }?.balance ?: 0.0
                    val bankAccount = accList.find { it.code == "1010" }?.balance ?: 0.0
                    val walletWallet = accList.find { it.code == "1020" }?.balance ?: 0.0
                    "Treasury Cash summary:\n- Vault (Cash in store): Rs. $cashOnHand\n- Bank Account: Rs. $bankAccount\n- Digital Registers (EasyPaisa/JazzCash): Rs. $walletWallet\n- Total Liquid Cash: Rs. ${cashOnHand + bankAccount + walletWallet}"
                }
                "suggest_sale" -> "Draft prepared for approval."
                "suggest_payment" -> "Draft prepared for client payment and transaction receipt logs."
                else -> "Query resolved."
            }
        } catch (e: Exception) {
            "Technical query resolution failed: ${e.message}"
        }
    }

    fun safeLaunch(block: suspend kotlinx.coroutines.CoroutineScope.() -> Unit) {
        viewModelScope.launch {
            try {
                block()
            } catch (e: Exception) {
                showToast("Urgent Error: ${e.message}")
            } catch (e: Throwable) {
                showToast("Critical System Error: ${e.message}")
            }
        }
    }

    fun exportBackup(uri: android.net.Uri) {
        safeLaunch {
            getApplication<Application>().contentResolver.openOutputStream(uri)?.use { output ->
                val dbFile = getApplication<Application>().getDatabasePath("app_database")
                if (dbFile.exists()) {
                    dbFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                    showToast("Backups exported successfully!")
                } else {
                    showToast("Active database not found for backing up")
                }
            }
        }
    }

    // Ledger Write Methods
    fun addNewCustomer(name: String, shopName: String, phone: String, address: String, creditLimit: Double, terms: Int) {
        safeLaunch {
            repo.addCustomer(name, shopName, phone, address, creditLimit, terms)
            showToast("Mister $name registered successfully!")
        }
    }

    fun addNewProduct(sku: String, name: String, brand: String, category: String, variant: String, unitCost: Double, salePrice: Double, reorderLevel: Int, initShowroomQty: Int, initGodownQty: Int) {
        safeLaunch {
            repo.addProduct(sku, name, brand, category, variant, unitCost, salePrice, reorderLevel, initShowroomQty, initGodownQty)
            showToast("Product $name ($sku) registered successfully!")
        }
    }

    fun executeSale(customerId: Int?, customerName: String?, items: List<SaleItem>, paymentType: String, paymentAccountCode: String, onFinished: (String) -> Unit) {
        safeLaunch {
            val invoiceNum = repo.recordSale(customerId, customerName, items, paymentType, paymentAccountCode)
            onFinished(invoiceNum)
        }
    }

    fun executePayment(customerId: Int, customerName: String, amount: Double, methodAccountCode: String, notes: String, onFinished: (String) -> Unit) {
        safeLaunch {
            val transNum = repo.recordPayment(customerId, customerName, amount, methodAccountCode, notes)
            onFinished(transNum)
        }
    }

    fun executeExpense(expenseAccountCode: String, debitAccountName: String, amount: Double, paymentAccountCode: String, description: String, onFinished: (String) -> Unit) {
        safeLaunch {
            val transNum = repo.recordExpense(expenseAccountCode, debitAccountName, amount, paymentAccountCode, description)
            onFinished(transNum)
        }
    }

    fun executeStockTransfer(productSku: String, productName: String, quantity: Int, fromLocation: String, toLocation: String) {
        safeLaunch {
            repo.recordStockTransfer(productSku, productName, quantity, fromLocation, toLocation)
            showToast("Stock transfer complete: $quantity pcs of $productName")
        }
    }

    fun executeStockAdjustment(productSku: String, productName: String, quantity: Int, location: String, type: String, reason: String) {
        safeLaunch {
            repo.recordManualAdjustment(productSku, productName, quantity, location, type, reason)
            showToast("Stock adjustment logged: $quantity pcs at $location")
        }
    }

    // Chat assistant execution
    fun sendMessageToAssistant() {
        val message = chatInputText.trim()
        if (message.isEmpty()) return

        chatInputText = ""
        val updatedHistory = _chatHistory.value.toMutableList()
        updatedHistory.add(Pair(message, true))
        _chatHistory.value = updatedHistory

        isChatLoading = true

        viewModelScope.launch {
            val custs = customers.value
            val accs = accounts.value
            val prods = products.value

            val aiResponse = groqService.getChatResponse(
                userInput = message,
                history = updatedHistory.drop(1),
                customers = custs,
                accounts = accs,
                products = prods
            ) { toolName, argsMap ->
                executeTool(toolName, argsMap)
            }

            isChatLoading = false

            tryParseActionJson(aiResponse)

            val parsedResponse = stripActionJson(aiResponse)
            val finalHistory = _chatHistory.value.toMutableList()
            finalHistory.add(Pair(parsedResponse, false))
            _chatHistory.value = finalHistory
        }
    }

    private fun tryParseActionJson(response: String) {
        try {
            val startIndex = response.lastIndexOf("{")
            val endIndex = response.lastIndexOf("}")
            if (startIndex in 0 until endIndex) {
                val jsonPart = response.substring(startIndex, endIndex + 1)
                val json = JSONObject(jsonPart)
                val suggestedAction = json.optString("suggestedAction", "NONE")
                if (suggestedAction != "NONE") {
                    chatSuggestedAction = json
                    showChatAddConfirmation = true
                }
            }
        } catch (e: Exception) {
            Log.d("HisabViewModel", "No suggested action JSON or failed to parse: ${e.message}")
        }
    }

    private fun stripActionJson(response: String): String {
        return try {
            val index = response.lastIndexOf("{")
            if (index != -1 && response.contains("suggestedAction")) {
                response.substring(0, index).trim()
            } else {
                response
            }
        } catch (e: Exception) {
            response
        }
    }

    fun confirmChatSuggestedAction(onNotification: (String) -> Unit) {
        val suggested = chatSuggestedAction ?: return
        showChatAddConfirmation = false
        chatSuggestedAction = null

        viewModelScope.launch {
            try {
                val actionType = suggested.optString("suggestedAction")
                val details = suggested.optString("details", "")
                val amount = suggested.optDouble("amount", 0.0)

                when (actionType) {
                    "RECORD_SALE" -> {
                        val custName = suggested.optString("customerName", "Cash Walk-In")
                        val custId = suggested.optInt("customerId", -1)
                        val jsonItems = suggested.optJSONArray("items")
                        val saleItemsList = mutableListOf<SaleItem>()

                        if (jsonItems != null) {
                            for (i in 0 until jsonItems.length()) {
                                val jo = jsonItems.getJSONObject(i)
                                val sku = jo.getString("sku")
                                val name = jo.optString("productName", "Product")
                                val qty = jo.getInt("quantity")
                                val rate = jo.optDouble("rate", 0.0)
                                val loc = jo.optString("fromLocation", "GODOWN")

                                val cost = repo.getProductBySku(sku)?.unitCost ?: 0.0
                                saleItemsList.add(
                                    SaleItem(
                                        productSku = sku,
                                        productName = name,
                                        quantity = qty,
                                        salePriceOnParchi = rate,
                                        costPriceAtTime = cost,
                                        fromLocation = loc
                                    )
                                )
                            }
                        }

                        if (saleItemsList.isNotEmpty()) {
                            val isCredit = custId != -1
                            val payAccount = if (isCredit) "1200" else suggested.optString("paymentAccountCode", "1000")
                            val code = repo.recordSale(
                                customerId = if (isCredit) custId else null,
                                customerName = custName,
                                items = saleItemsList,
                                paymentType = if (isCredit) "CREDIT" else "CASH",
                                paymentAccountCode = payAccount
                            )
                            onNotification("Parchi generated successfully: $code")
                        } else {
                            onNotification("Could not record sale: No valid items found in suggestion.")
                        }
                    }
                    "RECORD_PAYMENT" -> {
                        val custId = suggested.optInt("customerId", -1)
                        val custName = suggested.optString("customerName", "Customer")
                        val payMethod = suggested.optString("paymentAccountCode", "1000")
                        if (custId != -1 && amount > 0) {
                            val code = repo.recordPayment(
                                customerId = custId,
                                customerName = custName,
                                amount = amount,
                                methodAccountCode = payMethod,
                                notes = "AI Auto-recorded: $details"
                            )
                            onNotification("Payment receipt logged: $code")
                        }
                    }
                    "RECORD_EXPENSE" -> {
                        val expCode = suggested.optString("expenseCode", "5200")
                        val payMethod = suggested.optString("paymentAccountCode", "1000")
                        val label = when(expCode) {
                            "5100" -> "Shop Rent"
                            "5200" -> "Utilities"
                            "5300" -> "Transport/Delivery"
                            "5400" -> "Staff Salaries"
                            else -> "Shop Expense"
                        }
                        if (amount > 0) {
                            val code = repo.recordExpense(
                                expenseAccountCode = expCode,
                                debitAccountName = label,
                                amount = amount,
                                paymentAccountCode = payMethod,
                                description = details
                            )
                            onNotification("Expense recorded: $code")
                        }
                    }
                }
            } catch (e: Exception) {
                onNotification("AI Action application failed: ${e.message}")
            }
        }
    }

    // OCR trigger
    fun uploadReceiptAndTriggerOcr(bitmap: Bitmap) {
        scannedBitmap = bitmap
        isOcrProcessing = true
        ocrResultOcrJson = null

        viewModelScope.launch {
            val prods = repo.allProducts.first()
            val parsedResult = groqService.parseParchiOcr(bitmap, prods)
            isOcrProcessing = false
            ocrResultOcrJson = parsedResult

            try {
                val json = JSONObject(parsedResult)
                ocrCustomerName = json.optString("customerName", "Cash Walk-In")
                ocrPaymentType = json.optString("paymentType", "CREDIT")
                ocrTotal = json.optDouble("grandTotal", 0.0)
                ocrConfidence = json.optDouble("confidence", 1.0)

                val itemsArray = json.optJSONArray("items")
                val parsedItems = mutableListOf<SaleItem>()
                if (itemsArray != null) {
                    for (i in 0 until itemsArray.length()) {
                        val io = itemsArray.getJSONObject(i)
                        val sku = io.optString("sku", "TI-MARB-01")
                        val pName = io.optString("productName", "Product")
                        val qty = io.optInt("quantity", 1)
                        val rate = io.optDouble("rate", 0.0)

                        val p = repo.getProductBySku(sku)
                        val cost = p?.unitCost ?: (rate * 0.6)
                        parsedItems.add(
                            SaleItem(
                                productSku = sku,
                                productName = pName,
                                quantity = qty,
                                salePriceOnParchi = rate,
                                costPriceAtTime = cost,
                                fromLocation = "GODOWN"
                            )
                        )
                    }
                }
                ocrItems = parsedItems
            } catch (e: Exception) {
                Log.e("HisabViewModel", "Failed to parse OCR return JSON: ${e.message}")
            }
        }
    }

    fun clearOcrState() {
        scannedBitmap = null
        ocrResultOcrJson = null
        ocrItems = emptyList<SaleItem>()
        ocrCustomerName = ""
        ocrTotal = 0.0
        ocrConfidence = 1.0
    }
}
