package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GeminiService
import com.example.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

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
    private val geminiService = GeminiService()

    // Database flows
    val accounts: StateFlow<List<AccountEntity>> = repo.allAccounts.stateIn(
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
    var selectedCustomer: CustomerEntity? by mutableStateOf(null)

    // OCR Scanning temporary hold
    var scannedBitmap: Bitmap? by mutableStateOf(null)
    var isOcrProcessing by mutableStateOf(false)
    var ocrResultOcrJson by mutableStateOf<String?>(null)
    var ocrCustomerName by mutableStateOf("")
    var ocrPaymentType by mutableStateOf("CREDIT")
    var ocrItems = mutableStateOf<List<SaleItem>>(emptyList())
    var ocrTotal by mutableStateOf(0.0)

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
            preseedShoroomProductsAndCustomers()
        }
    }

    private suspend fun preseedShoroomProductsAndCustomers() {
        val currentPd = repo.allProducts.first()
        if (currentPd.isEmpty()) {
            // Seed 5 high-quality products
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

            // Seed Customers
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

            // Let's seed pre-existing transactions for these customers to show active outstanding bills beautifully!
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

            // A payment receipt too
            repo.recordPayment(
                customerId = c1Id.toInt(),
                customerName = "Ali Traders (Gulshan)",
                amount = 10000.0,
                methodAccountCode = "1000",
                notes = "Advance cash deposit at counter"
            )
        }
    }

    // Ledger Write Methods
    fun addNewCustomer(name: String, shopName: String, phone: String, address: String, creditLimit: Double, terms: Int) {
        viewModelScope.launch {
            repo.addCustomer(name, shopName, phone, address, creditLimit, terms)
        }
    }

    fun addNewProduct(sku: String, name: String, brand: String, category: String, variant: String, unitCost: Double, salePrice: Double, reorderLevel: Int, initShowroomQty: Int, initGodownQty: Int) {
        viewModelScope.launch {
            repo.addProduct(sku, name, brand, category, variant, unitCost, salePrice, reorderLevel, initShowroomQty, initGodownQty)
        }
    }

    fun executeSale(customerId: Int?, customerName: String?, items: List<SaleItem>, paymentType: String, paymentAccountCode: String, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            val invoiceNum = repo.recordSale(customerId, customerName, items, paymentType, paymentAccountCode)
            onFinished(invoiceNum)
        }
    }

    fun executePayment(customerId: Int, customerName: String, amount: Double, methodAccountCode: String, notes: String, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            val transNum = repo.recordPayment(customerId, customerName, amount, methodAccountCode, notes)
            onFinished(transNum)
        }
    }

    fun executeExpense(expenseAccountCode: String, debitAccountName: String, amount: Double, paymentAccountCode: String, description: String, onFinished: (String) -> Unit) {
        viewModelScope.launch {
            val transNum = repo.recordExpense(expenseAccountCode, debitAccountName, amount, paymentAccountCode, description)
            onFinished(transNum)
        }
    }

    fun executeStockTransfer(productSku: String, productName: String, quantity: Int, fromLocation: String, toLocation: String) {
        viewModelScope.launch {
            repo.recordStockTransfer(productSku, productName, quantity, fromLocation, toLocation)
        }
    }

    fun executeStockAdjustment(productSku: String, productName: String, quantity: Int, location: String, type: String, reason: String) {
        viewModelScope.launch {
            repo.recordManualAdjustment(productSku, productName, quantity, location, type, reason)
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

            val aiResponse = geminiService.getChatResponse(
                userInput = message,
                history = updatedHistory.drop(1), // ignore first greet item
                customers = custs,
                accounts = accs,
                products = prods
            )

            isChatLoading = false

            // Try to extract structured transaction recommendations if present
            tryParseActionJson(aiResponse)

            val parsedResponse = stripActionJson(aiResponse)
            val finalHistory = _chatHistory.value.toMutableList()
            finalHistory.add(Pair(parsedResponse, false))
            _chatHistory.value = finalHistory
        }
    }

    private fun tryParseActionJson(response: String) {
        try {
            // Find any valid JSON block at the bottom
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
            // ignored, no valid suggested action
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

                            // Check active product cost
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
                        val code = repo.recordSale(
                            customerId = if (isCredit) custId else null,
                            customerName = custName,
                            items = saleItemsList,
                            paymentType = if (isCredit) "CREDIT" else "CASH",
                            paymentAccountCode = "1000"
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
        }
    }

    // OCR trigger
    fun uploadReceiptAndTriggerOcr(bitmap: Bitmap) {
        scannedBitmap = bitmap
        isOcrProcessing = true
        ocrResultOcrJson = null

        viewModelScope.launch {
            val prods = repo.allProducts.first()
            val parsedResult = geminiService.parseParchiOcr(bitmap, prods)
            isOcrProcessing = false
            ocrResultOcrJson = parsedResult

            try {
                val json = JSONObject(parsedResult)
                ocrCustomerName = json.optString("customerName", "Cash Walk-In")
                ocrPaymentType = json.optString("paymentType", "CREDIT")
                ocrTotal = json.optDouble("grandTotal", 0.0)

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
                        val cost = p?.unitCost ?: (rate * 0.6) // estimate cost if missing SKU
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
                ocrItems.value = parsedItems
            } catch (e: Exception) {
                Log.e("HisabViewModel", "Failed to parse OCR return JSON: ${e.message}")
            }
        }
    }

    fun clearOcrState() {
        scannedBitmap = null
        ocrResultOcrJson = null
        ocrItems.value = emptyList()
        ocrCustomerName = ""
        ocrTotal = 0.0
    }
}
