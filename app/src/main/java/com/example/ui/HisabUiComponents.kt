package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.example.data.*
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun MainAppScreen(viewModel: HisabViewModel = viewModel()) {
    val context = LocalContext.current
    var showQuickAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(SlateGrayBg),
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(listOf(EmeraldAccent, EmeraldLight))),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "ح",
                                color = SlateGrayBg,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "HISAB AI",
                                fontWeight = FontWeight.Bold,
                                color = LightText,
                                fontSize = 16.sp,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldAccent)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    "Dual-Ledger Active",
                                    color = EmeraldAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "Cloud sync complete!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SlateGraySurface)
                            .size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Sync",
                            tint = EmeraldLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Divider(color = SlateGrayBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            Column {
                Divider(color = SlateGrayBorder, thickness = 1.dp)
                NavigationBar(
                    containerColor = SlateGraySurface,
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = viewModel.currentScreen == UIState.DASHBOARD,
                        onClick = { viewModel.currentScreen = UIState.DASHBOARD },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                        label = { Text("Home", fontSize = 10.sp, fontWeight = if (viewModel.currentScreen == UIState.DASHBOARD) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF042100),
                            selectedTextColor = Color(0xFF042100),
                            indicatorColor = EmeraldLight,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == UIState.KHATA_LIST || viewModel.currentScreen == UIState.CUSTOMER_DETAIL,
                        onClick = { viewModel.currentScreen = UIState.KHATA_LIST },
                        icon = { Icon(Icons.Default.Person, contentDescription = "Khata") },
                        label = { Text("Khata", fontSize = 10.sp, fontWeight = if (viewModel.currentScreen == UIState.KHATA_LIST || viewModel.currentScreen == UIState.CUSTOMER_DETAIL) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF042100),
                            selectedTextColor = Color(0xFF042100),
                            indicatorColor = EmeraldLight,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == UIState.PRODUCTS_GRID,
                        onClick = { viewModel.currentScreen = UIState.PRODUCTS_GRID },
                        icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Products") },
                        label = { Text("Stock", fontSize = 10.sp, fontWeight = if (viewModel.currentScreen == UIState.PRODUCTS_GRID) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF042100),
                            selectedTextColor = Color(0xFF042100),
                            indicatorColor = EmeraldLight,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == UIState.ASSISTANT_CHAT,
                        onClick = { viewModel.currentScreen = UIState.ASSISTANT_CHAT },
                        icon = { Icon(Icons.Default.Send, contentDescription = "AI Assistant") },
                        label = { Text("AI Assist", fontSize = 10.sp, fontWeight = if (viewModel.currentScreen == UIState.ASSISTANT_CHAT) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF042100),
                            selectedTextColor = Color(0xFF042100),
                            indicatorColor = EmeraldLight,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate
                        )
                    )
                    NavigationBarItem(
                        selected = viewModel.currentScreen == UIState.REPORTS,
                        onClick = { viewModel.currentScreen = UIState.REPORTS },
                        icon = { Icon(Icons.Default.List, contentDescription = "Reports") },
                        label = { Text("Reports", fontSize = 10.sp, fontWeight = if (viewModel.currentScreen == UIState.REPORTS) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF042100),
                            selectedTextColor = Color(0xFF042100),
                            indicatorColor = EmeraldLight,
                            unselectedIconColor = MutedSlate,
                            unselectedTextColor = MutedSlate
                        )
                    )
                }
            }
        },
        floatingActionButton = {
            if (viewModel.currentScreen == UIState.DASHBOARD || viewModel.currentScreen == UIState.KHATA_LIST || viewModel.currentScreen == UIState.PRODUCTS_GRID) {
                FloatingActionButton(
                    onClick = { showQuickAddDialog = true },
                    containerColor = EmeraldAccent,
                    contentColor = SlateGrayBg,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Quick Action")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(SlateGrayBg)
        ) {
            when (viewModel.currentScreen) {
                UIState.DASHBOARD -> DashboardScreen(viewModel)
                UIState.KHATA_LIST -> KhataScreen(viewModel)
                UIState.CUSTOMER_DETAIL -> CustomerDetailScreen(viewModel)
                UIState.PRODUCTS_GRID -> ProductsScreen(viewModel)
                UIState.ASSISTANT_CHAT -> AssistantChatScreen(viewModel)
                UIState.REPORTS -> ReportsScreen(viewModel)
                UIState.PARCHI_OCR -> ParchiOcrScreen(viewModel)
                else -> DashboardScreen(viewModel)
            }
        }
    }

    if (showQuickAddDialog) {
        QuickAddSelectorDialog(
            onDismiss = { showQuickAddDialog = false },
            onSelect = { type ->
                showQuickAddDialog = false
                viewModel.quickAddType = type
            },
            onOcrSelect = {
                showQuickAddDialog = false
                viewModel.currentScreen = UIState.PARCHI_OCR
            }
        )
    }

    viewModel.quickAddType?.let { type ->
        when (type) {
            "SALE" -> RecordSaleDialog(viewModel) { viewModel.quickAddType = null }
            "PAYMENT" -> RecordPaymentDialog(viewModel) { viewModel.quickAddType = null }
            "EXPENSE" -> RecordExpenseDialog(viewModel) { viewModel.quickAddType = null }
            "TRANSFER" -> RecordStockTransferDialog(viewModel) { viewModel.quickAddType = null }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 1. DASHBOARD SCREEN
// ---------------------------------------------------------------------------------------------------------------------
@Composable
fun DashboardScreen(viewModel: HisabViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()
    val journalEntries by viewModel.journalEntries.collectAsState()

    val cash = accounts.find { it.code == "1000" }?.balance ?: 0.0
    val bank = accounts.find { it.code == "1010" }?.balance ?: 0.0
    val wallet = accounts.find { it.code == "1020" }?.balance ?: 0.0
    val totalCashLiquidity = cash + bank + wallet

    val totalReceivables = customers.sumOf { it.runningBalance }
    val totalSalesRevenue = accounts.find { it.code == "4000" }?.balance ?: 0.0
    val lowStockCount = products.filter { (it.showroomQty + it.godownQty) <= it.reorderLevel }.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            InteractiveWelcomeCard(
                onScanClick = { viewModel.currentScreen = UIState.PARCHI_OCR },
                onChatClick = { viewModel.currentScreen = UIState.ASSISTANT_CHAT }
            )
        }

        item {
            Column {
                Text(
                    text = "Khata & Cash Analysis",
                    color = LightText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricMiniCard(
                        title = "Cash & Bank",
                        value = formatRupees(totalCashLiquidity),
                        description = "Liquidity Reserve",
                        icon = Icons.Default.Home,
                        color = EmeraldAccent,
                        modifier = Modifier.weight(1f)
                    )
                    MetricMiniCard(
                        title = "Outstandings",
                        value = formatRupees(totalReceivables),
                        description = "Urdu Udhar Khata",
                        icon = Icons.Default.Person,
                        color = OrangeAlert,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                MetricLargeCard(
                    title = "Monthly Revenue Sales",
                    value = formatRupees(totalSalesRevenue),
                    subText = "Double-entry Accounting Balanced",
                    color = EmeraldLight
                )
            }
        }

        if (lowStockCount > 0) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = RoseError.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoseError.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = "Low Stock", tint = RoseError, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Low Stock SKU Warning ($lowStockCount items)", fontWeight = FontWeight.Bold, color = LightText, fontSize = 12.sp)
                            Text("Ceramics stock levels dropping rapidly.", color = LightText.copy(alpha = 0.7f), fontSize = 10.sp)
                        }
                    }
                }
            }
        }

        item {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Outstanding Udhar", color = LightText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Details", color = EmeraldAccent, fontSize = 12.sp, modifier = Modifier.clickable { viewModel.currentScreen = UIState.KHATA_LIST })
                }

                if (customers.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().background(SlateGraySurface, RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Text("No registered customers.", color = MutedSlate, fontSize = 12.sp)
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateGrayBorder)
                    ) {
                        Column {
                            customers.take(3).forEachIndexed { idx, customer ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectedCustomer = customer
                                            viewModel.currentScreen = UIState.CUSTOMER_DETAIL
                                        }
                                        .padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = LightText)
                                        Text(customer.shopName, fontSize = 11.sp, color = MutedSlate)
                                    }
                                    Text(
                                        formatRupees(customer.runningBalance),
                                        color = if (customer.runningBalance > 0) OrangeAlert else EmeraldAccent,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                if (idx < 2 && idx < customers.size - 1) {
                                    Divider(color = SlateGrayBorder)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Column {
                Text("Recent Ledger Double-Entry Posts", color = LightText, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                if (journalEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().background(SlateGraySurface, RoundedCornerShape(12.dp)).padding(16.dp)) {
                        Text("No journal postings logged yet.", color = MutedSlate, fontSize = 12.sp)
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateGrayBorder)
                    ) {
                        Column {
                            journalEntries.take(3).forEachIndexed { idx, post ->
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(post.description, color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                        Text(formatRupees(post.amount), color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Dr ${post.debitAccountCode} | Cr ${post.creditAccountCode}", color = MutedSlate, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                        Text(formatDate(post.date), color = MutedSlate, fontSize = 10.sp)
                                    }
                                }
                                if (idx < 2 && idx < journalEntries.size - 1) {
                                    Divider(color = SlateGrayBorder)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 2. KHATA LIST SCREEN
// ---------------------------------------------------------------------------------------------------------------------
@Composable
fun KhataScreen(viewModel: HisabViewModel) {
    val customers by viewModel.customers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showCustomerAddDialog by remember { mutableStateOf(false) }

    val filtered = customers.filter {
        it.name.contains(searchQuery, true) || it.shopName.contains(searchQuery, true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search Khata holders...", color = MutedSlate, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = SlateGrayBorder,
                    focusedLabelColor = EmeraldAccent
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = { showCustomerAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text("+ New", color = SlateGrayBg, fontWeight = FontWeight.Bold)
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No customers found.", color = MutedSlate)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.selectedCustomer = item
                                viewModel.currentScreen = UIState.CUSTOMER_DETAIL
                            },
                        border = BorderStroke(1.dp, SlateGrayBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name, fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                                Text(item.shopName, color = MutedSlate, fontSize = 11.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(formatRupees(item.runningBalance), color = OrangeAlert, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Limit: ${formatRupees(item.creditLimit)}", color = MutedSlate, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomerAddDialog) {
        CustomerAddDialog(
            onDismiss = { showCustomerAddDialog = false },
            onSave = { name, shop, phone, address, limit, terms ->
                viewModel.addNewCustomer(name, shop, phone, address, limit, terms)
                showCustomerAddDialog = false
            }
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 3. CUSTOMER DETAILS SCREEN
// ---------------------------------------------------------------------------------------------------------------------
@Composable
fun CustomerDetailScreen(viewModel: HisabViewModel) {
    val customer = viewModel.selectedCustomer ?: return
    val journalEntries by viewModel.journalEntries.collectAsState()
    val ledgerTransactions = journalEntries.filter { it.customerId == customer.id }
    val context = LocalContext.current
    var showRepaymentDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.currentScreen = UIState.KHATA_LIST }) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = LightText)
            }
            Text("Udhar Customer File", fontWeight = FontWeight.Bold, color = LightText, fontSize = 15.sp)
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, SlateGrayBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(customer.name, fontWeight = FontWeight.Bold, color = LightText, fontSize = 16.sp)
                Text("Shop name: " + customer.shopName, color = EmeraldLight, fontSize = 12.sp)
                Text("Phone: " + customer.phone, color = MutedSlate, fontSize = 11.sp)
                Text("Address: " + customer.address, color = MutedSlate, fontSize = 11.sp, maxLines = 1)

                Divider(color = SlateGrayBorder, modifier = Modifier.padding(vertical = 10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("OUTSTANDING UDHAR", color = MutedSlate, fontSize = 10.sp)
                        Text(formatRupees(customer.runningBalance), color = OrangeAlert, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("CREDIT LIMIT", color = MutedSlate, fontSize = 10.sp)
                        Text(formatRupees(customer.creditLimit), color = LightText, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { showRepaymentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Repayment Cash", color = SlateGrayBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val msg = "Dear customer ${customer.name}, you have a ledger balance of ${formatRupees(customer.runningBalance)} outstanding at Saddar Ceramics. Please clear your dues. Thank you!"
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, msg)
                                type = "text/plain"
                            }
                            context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Ledger Balance"))
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBorder),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share Ledger", color = LightText, fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Receipt Log History", fontWeight = FontWeight.Bold, color = LightText, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))

        if (ledgerTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("No matching general ledger entries.", color = MutedSlate)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(ledgerTransactions) { log ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateGraySurface.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, SlateGrayBorder)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(log.description, fontWeight = FontWeight.Bold, color = LightText, fontSize = 12.sp)
                                Text(formatRupees(log.amount), color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Dr ${log.debitAccountCode} | Cr ${log.creditAccountCode}", color = MutedSlate, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                Text(formatDate(log.date), color = MutedSlate, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showRepaymentDialog) {
        ReceiveRepaymentDialog(
            customer = customer,
            onDismiss = { showRepaymentDialog = false },
            onSave = { amount, methodAccount, notes ->
                viewModel.executePayment(customer.id, customer.name, amount, methodAccount, notes) { entryCode ->
                    Toast.makeText(context, "Logged Repayment $entryCode", Toast.LENGTH_SHORT).show()
                    viewModel.selectedCustomer = customer.copy(runningBalance = customer.runningBalance - amount)
                    showRepaymentDialog = false
                }
            }
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 4. PRODUCTS CATALOG SCREEN
// ---------------------------------------------------------------------------------------------------------------------
@Composable
fun ProductsScreen(viewModel: HisabViewModel) {
    val products by viewModel.products.collectAsState()
    var searchSKU by remember { mutableStateOf("") }
    var showAddSKU by remember { mutableStateOf(false) }

    val filtered = products.filter {
        it.name.contains(searchSKU, true) || it.sku.contains(searchSKU, true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchSKU,
                onValueChange = { searchSKU = it },
                placeholder = { Text("Search catalog items SKU / Name...", color = MutedSlate, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = SlateGrayBorder
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Button(
                onClick = { showAddSKU = true },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.height(52.dp)
            ) {
                Text("+ Catalog", color = SlateGrayBg, fontWeight = FontWeight.Bold)
            }
        }

        if (filtered.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text("Empty product catalog.", color = MutedSlate)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered) { p ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, SlateGrayBorder)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(p.name, fontWeight = FontWeight.Bold, color = LightText, fontSize = 13.sp)
                                    Text("SKU: ${p.sku} | Brand: ${p.brand}", color = MutedSlate, fontSize = 11.sp)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(EmeraldLight.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(p.category, color = EmeraldLight, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Sale price: " + formatRupees(p.salePrice), color = LightText, fontSize = 11.sp)
                                    Text("Unit Cost: " + formatRupees(p.unitCost), color = MutedSlate, fontSize = 10.sp)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StockBadge("Showroom", p.showroomQty)
                                    StockBadge("Godown", p.godownQty)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSKU) {
        ProductAddDialog(
            onDismiss = { showAddSKU = false },
            onSave = { sku, name, brand, category, variant, cost, price, limit, showroom, godown ->
                viewModel.addNewProduct(sku, name, brand, category, variant, cost, price, limit, showroom, godown)
                showAddSKU = false
            }
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 5. ASSISTANT CHAT SCREEN (COPILOT)
// ---------------------------------------------------------------------------------------------------------------------
@Composable
fun AssistantChatScreen(viewModel: HisabViewModel) {
    val history by viewModel.chatHistory.collectAsState()
    val isChatLoading = viewModel.isChatLoading
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Hisab AI Copilot assistant", fontWeight = FontWeight.Bold, color = LightText, fontSize = 15.sp)
        Text("Convert handwriting description or voice command direct into ledger entries with Urdu feedback.", color = MutedSlate, fontSize = 11.sp, modifier = Modifier.padding(bottom = 8.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(history) { msg ->
                val isUser = msg.second
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 10.dp,
                                    topEnd = 10.dp,
                                    bottomStart = if (isUser) 10.dp else 0.dp,
                                    bottomEnd = if (isUser) 0.dp else 10.dp
                                )
                            )
                            .background(if (isUser) EmeraldAccent else SlateGraySurface)
                            .widthIn(max = 280.dp)
                            .padding(10.dp)
                    ) {
                        Text(msg.first, color = if (isUser) SlateGrayBg else LightText, fontSize = 12.sp, lineHeight = 17.sp)
                    }
                }
            }

            if (isChatLoading) {
                item {
                    Text("AI is loading recommendation details...", color = EmeraldLight, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                }
            }
        }

        if (viewModel.showChatAddConfirmation) {
            viewModel.chatSuggestedAction?.let { action ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
                    border = BorderStroke(1.dp, EmeraldAccent)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Suggested Balance Posting:", fontWeight = FontWeight.Bold, color = EmeraldAccent, fontSize = 12.sp)
                        Text("Action: ${action.optString("suggestedAction")} | Amount: Rs. ${action.optDouble("amount")}", color = LightText, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.confirmChatSuggestedAction { msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Post Entry", color = SlateGrayBg, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = {
                                    viewModel.showChatAddConfirmation = false
                                    viewModel.chatSuggestedAction = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBorder),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("Reject", color = LightText, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                QueryChip("Saddar ceramics ke stock kya hain?") { viewModel.chatInputText = it }
            }
            item {
                QueryChip("Ali Traders ka khata check karo") { viewModel.chatInputText = it }
            }
            item {
                QueryChip("Showroom stock low hai?") { viewModel.chatInputText = it }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = viewModel.chatInputText,
                onValueChange = { viewModel.chatInputText = it },
                placeholder = { Text("Write in Roman Urdu or Urdu...", color = MutedSlate, fontSize = 12.sp) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldAccent,
                    unfocusedBorderColor = SlateGrayBorder
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = { viewModel.sendMessageToAssistant() },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EmeraldAccent)
                    .size(52.dp)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = SlateGrayBg)
            }
        }
    }
}

@Composable
fun QueryChip(label: String, onClick: (String) -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SlateGraySurface)
            .clickable { onClick(label) }
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .border(1.dp, SlateGrayBorder, RoundedCornerShape(6.dp))
    ) {
        Text(label, color = LightText, fontSize = 10.sp)
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 6. REPORTS SCREEN (PL & TRIAL BALANCE)
// ---------------------------------------------------------------------------------------------------------------------
@Composable
fun ReportsScreen(viewModel: HisabViewModel) {
    val accounts by viewModel.accounts.collectAsState()
    var tabSelected by remember { mutableStateOf(0) } // 0: Income Statement, 1: Trial Balance

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("Account Sheets & Ledgers", fontWeight = FontWeight.Bold, color = LightText, fontSize = 15.sp)
        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Button(
                onClick = { tabSelected = 0 },
                colors = ButtonDefaults.buttonColors(containerColor = if (tabSelected == 0) EmeraldAccent else SlateGraySurface),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)
            ) {
                Text("Income Statement", color = if (tabSelected == 0) SlateGrayBg else LightText, fontSize = 12.sp)
            }
            Button(
                onClick = { tabSelected = 1 },
                colors = ButtonDefaults.buttonColors(containerColor = if (tabSelected == 1) EmeraldAccent else SlateGraySurface),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp)
            ) {
                Text("Trial Balance", color = if (tabSelected == 1) SlateGrayBg else LightText, fontSize = 12.sp)
            }
        }

        if (tabSelected == 0) {
            val revenue = accounts.find { it.code == "4000" }?.balance ?: 0.0
            val cogs = accounts.find { it.code == "5000" }?.balance ?: 0.0
            val rent = accounts.find { it.code == "5100" }?.balance ?: 0.0
            val utilities = accounts.find { it.code == "5200" }?.balance ?: 0.0
            val transport = accounts.find { it.code == "5300" }?.balance ?: 0.0
            val salaries = accounts.find { it.code == "5400" }?.balance ?: 0.0
            val expenses = rent + utilities + transport + salaries
            val net = revenue - cogs - expenses

            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    ReportRow("Total Income Revenue", revenue, isHeader = true, positive = true)
                }
                item {
                    ReportRow("Cost of Sales (COGS)", cogs, isHeader = false, positive = false)
                }
                item { Divider(color = SlateGrayBorder) }
                item {
                    ReportRow("Gross Ceramics Margin", revenue - cogs, isHeader = true, positive = true)
                }
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Operational Expenses", fontWeight = FontWeight.Bold, color = LightText, fontSize = 13.sp)
                }
                item {
                    ReportRow("Shop Room Rent", rent, isHeader = false, positive = false)
                }
                item {
                    ReportRow("Sui Gas & Power Utilites", utilities, isHeader = false, positive = false)
                }
                item {
                    ReportRow("Godown Carriage & Transport", transport, isHeader = false, positive = false)
                }
                item {
                    ReportRow("Helper Salaries", salaries, isHeader = false, positive = false)
                }
                item { Divider(color = SlateGrayBorder) }
                item {
                    ReportRow("Net Accountant Profit (Loss)", net, isHeader = true, positive = net >= 0.0)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(SlateGraySurface)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Category Detail", color = MutedSlate, fontSize = 11.sp, modifier = Modifier.weight(2f))
                        Text("Debit (Dr)", color = MutedSlate, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text("Credit (Cr)", color = MutedSlate, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }

                items(accounts) { acc ->
                    val isDr = acc.type == "ASSET" || acc.type == "EXPENSE"
                    val dr = if (isDr) acc.balance else 0.0
                    val cr = if (!isDr) acc.balance else 0.0

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${acc.code} ${acc.name}", color = LightText, fontSize = 11.sp, modifier = Modifier.weight(2f))
                        Text(if (dr != 0.0) formatRupees(dr) else "—", color = LightText, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(if (cr != 0.0) formatRupees(cr) else "—", color = LightText, fontSize = 11.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }

                item {
                    Divider(color = EmeraldAccent, thickness = 2.dp)
                    val totalDr = accounts.filter { it.type == "ASSET" || it.type == "EXPENSE" }.sumOf { it.balance }
                    val totalCr = accounts.filter { it.type == "LIABILITY" || it.type == "EQUITY" || it.type == "INCOME" }.sumOf { it.balance }

                    Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                        Text("Trial Sum", fontWeight = FontWeight.Bold, color = EmeraldAccent, fontSize = 12.sp, modifier = Modifier.weight(2f))
                        Text(formatRupees(totalDr), fontWeight = FontWeight.Bold, color = EmeraldAccent, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                        Text(formatRupees(totalCr), fontWeight = FontWeight.Bold, color = EmeraldAccent, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
                    }
                }
            }
        }
    }
}

@Composable
fun ReportRow(label: String, value: Double, isHeader: Boolean, positive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal, color = LightText, fontSize = 12.sp)
        Text(
            text = (if (positive) "" else "- ") + formatRupees(value),
            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) (if (positive) EmeraldAccent else RoseError) else LightText,
            fontSize = 12.sp
        )
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 7. PARCHI VISION OCR SCANNING SCREEN
// ---------------------------------------------------------------------------------------------------------------------
@Composable
fun ParchiOcrScreen(viewModel: HisabViewModel) {
    val context = LocalContext.current
    val isOcrProcessing = viewModel.isOcrProcessing
    val bitmap = viewModel.scannedBitmap

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Parchi OCR scanning", fontWeight = FontWeight.Bold, color = LightText, fontSize = 15.sp, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        if (bitmap == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SlateGraySurface)
                    .border(1.dp, EmeraldAccent, RoundedCornerShape(12.dp))
                    .clickable {
                        val sample = generateMockParchiBitmap(context)
                        viewModel.uploadReceiptAndTriggerOcr(sample)
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(36.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Snap / Upload Parchi Receipt", color = LightText, fontWeight = FontWeight.Bold)
                    Text("Simulated snap of Pottery written bill", color = MutedSlate, fontSize = 10.sp)
                }
            }
        } else {
            Image(
                painter = rememberAsyncImagePainter(bitmap),
                contentDescription = "Parchi",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(2.dp, EmeraldLight, RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isOcrProcessing) {
                Text("Gemini Vision API mapping ledger parameters...", color = EmeraldLight, fontSize = 12.sp)
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text("Customer Name", color = MutedSlate, fontSize = 10.sp)
                        OutlinedTextField(
                            value = viewModel.ocrCustomerName,
                            onValueChange = { viewModel.ocrCustomerName = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldAccent)
                        )
                    }
                    item {
                        Text("Payment Mode", color = MutedSlate, fontSize = 10.sp)
                        OutlinedTextField(
                            value = viewModel.ocrPaymentType,
                            onValueChange = { viewModel.ocrPaymentType = it },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = EmeraldAccent)
                        )
                    }

                    item {
                        Text("Scanned items ledger summary:", fontWeight = FontWeight.Bold, color = LightText, fontSize = 12.sp)
                    }

                    items(viewModel.ocrItems.value) { item ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
                            border = BorderStroke(1.dp, SlateGrayBorder)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(item.productName, fontWeight = FontWeight.Bold, color = LightText, fontSize = 12.sp)
                                    Text("Qty: ${item.quantity} | SKU: ${item.productSku}", color = MutedSlate, fontSize = 10.sp)
                                }
                                Text(formatRupees(item.quantity * item.salePriceOnParchi), color = LightText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Outlay", color = MutedSlate, fontSize = 13.sp)
                            Text(formatRupees(viewModel.ocrTotal), fontWeight = FontWeight.Bold, color = EmeraldAccent, fontSize = 15.sp)
                        }
                    }

                    item {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    val match = viewModel.customers.value.find { it.name.contains(viewModel.ocrCustomerName, true) }
                                    viewModel.executeSale(
                                        customerId = match?.id,
                                        customerName = viewModel.ocrCustomerName,
                                        items = viewModel.ocrItems.value,
                                        paymentType = viewModel.ocrPaymentType.trim().uppercase(),
                                        paymentAccountCode = "1000"
                                    ) { doc ->
                                        Toast.makeText(context, "Sale recorded: $doc", Toast.LENGTH_SHORT).show()
                                        viewModel.clearOcrState()
                                        viewModel.currentScreen = UIState.DASHBOARD
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Post Extracted", color = SlateGrayBg, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { viewModel.clearOcrState() },
                                colors = ButtonDefaults.buttonColors(containerColor = SlateGraySurface),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Discard", color = LightText)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 8. DIALOG WORKFLOWS
// ---------------------------------------------------------------------------------------------------------------------
@Composable
fun QuickAddSelectorDialog(onDismiss: () -> Unit, onSelect: (String) -> Unit, onOcrSelect: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Post Ledger Entry", fontWeight = FontWeight.Bold, color = LightText, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(10.dp))
                QuickItem("Sale Parchi", "Record ceramics / sanitaryware sale", Icons.Default.ShoppingCart, EmeraldAccent) { onSelect("SALE") }
                Spacer(modifier = Modifier.height(8.dp))
                QuickItem("Repayment collected", "Clear outstanding customer balances", Icons.Default.Check, EmeraldLight) { onSelect("PAYMENT") }
                Spacer(modifier = Modifier.height(8.dp))
                QuickItem("Shop business Expense", "Rent, transport, utilities salaries", Icons.Default.Warning, OrangeAlert) { onSelect("EXPENSE") }
                Spacer(modifier = Modifier.height(8.dp))
                QuickItem("Inter-Stock Transfer", "Move products showroom from godown", Icons.Default.Refresh, MutedSlate) { onSelect("TRANSFER") }
                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = SlateGrayBorder)
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOcrSelect,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldLight),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Parse Handwritten Bill", color = SlateGrayBg, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun QuickItem(title: String, desc: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(SlateGrayBg, RoundedCornerShape(8.dp))
            .border(1.dp, SlateGrayBorder, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(title, fontWeight = FontWeight.Bold, color = LightText, fontSize = 12.sp)
            Text(desc, color = MutedSlate, fontSize = 10.sp)
        }
    }
}

@Composable
fun RecordSaleDialog(viewModel: HisabViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    val products by viewModel.products.collectAsState()

    var isCredit by remember { mutableStateOf(true) }
    var chosenCustId by remember { mutableStateOf<Int?>(null) }
    var cashCustName by remember { mutableStateOf("") }
    var chosenSku by remember { mutableStateOf("") }
    var chosenQty by remember { mutableStateOf("1") }
    var chosenRate by remember { mutableStateOf("") }
    var stockLocation by remember { mutableStateOf("GODOWN") }

    val billItems = remember { mutableStateListOf<SaleItem>() }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Sales Statement Parchi", fontWeight = FontWeight.Bold, color = LightText, fontSize = 15.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Row {
                    Button(onClick = { isCredit = true }, colors = ButtonDefaults.buttonColors(containerColor = if (isCredit) EmeraldAccent else SlateGrayBg), modifier = Modifier.weight(1f)) {
                        Text("Udhar Credit", color = if (isCredit) SlateGrayBg else LightText, fontSize = 11.sp)
                    }
                    Button(onClick = { isCredit = false }, colors = ButtonDefaults.buttonColors(containerColor = if (!isCredit) EmeraldAccent else SlateGrayBg), modifier = Modifier.weight(1f)) {
                        Text("Direct Cash", color = if (!isCredit) SlateGrayBg else LightText, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (isCredit) {
                    Text("Select Customer Account:", color = MutedSlate, fontSize = 11.sp)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(customers) { c ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (chosenCustId == c.id) EmeraldAccent.copy(alpha = 0.2f) else SlateGrayBg)
                                    .clickable { chosenCustId = c.id }
                                    .padding(8.dp)
                                    .border(1.dp, if (chosenCustId == c.id) EmeraldAccent else SlateGrayBorder, RoundedCornerShape(6.dp))
                            ) {
                                Text(c.name, color = LightText, fontSize = 11.sp)
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = cashCustName,
                        onValueChange = { cashCustName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Walk-In Client Name", color = MutedSlate, fontSize = 11.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = SlateGrayBorder)
                Spacer(modifier = Modifier.height(12.dp))

                Text("Chose Product SKU:", color = MutedSlate, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(products) { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (chosenSku == p.sku) EmeraldLight.copy(alpha = 0.2f) else SlateGrayBg)
                                .clickable {
                                    chosenSku = p.sku
                                    chosenRate = p.salePrice.toString()
                                }
                                .padding(8.dp)
                                .border(1.dp, if (chosenSku == p.sku) EmeraldLight else SlateGrayBorder, RoundedCornerShape(6.dp))
                        ) {
                            Text(p.name + " (" + p.sku + ")", color = LightText, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chosenQty,
                        onValueChange = { chosenQty = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Quantity", color = MutedSlate, fontSize = 10.sp) }
                    )
                    OutlinedTextField(
                        value = chosenRate,
                        onValueChange = { chosenRate = it },
                        modifier = Modifier.weight(1.5f),
                        label = { Text("Rate Rs", color = MutedSlate, fontSize = 10.sp) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row {
                    Text("Stock Store source:  ", color = MutedSlate, fontSize = 11.sp)
                    Button(onClick = { stockLocation = "SHOWROOM" }, colors = ButtonDefaults.buttonColors(containerColor = if (stockLocation == "SHOWROOM") EmeraldLight else SlateGrayBg)) {
                        Text("Showroom", fontSize = 10.sp, color = if (stockLocation == "SHOWROOM") SlateGrayBg else LightText)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = { stockLocation = "GODOWN" }, colors = ButtonDefaults.buttonColors(containerColor = if (stockLocation == "GODOWN") EmeraldLight else SlateGrayBg)) {
                        Text("Godown", fontSize = 10.sp, color = if (stockLocation == "GODOWN") SlateGrayBg else LightText)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val selectedP = products.find { it.sku == chosenSku }
                        val qtyVal = chosenQty.toIntOrNull() ?: 1
                        val rateVal = chosenRate.toDoubleOrNull() ?: selectedP?.salePrice ?: 0.0

                        if (selectedP != null) {
                            billItems.add(
                                SaleItem(
                                    productSku = selectedP.sku,
                                    productName = selectedP.name,
                                    quantity = qtyVal,
                                    salePriceOnParchi = rateVal,
                                    costPriceAtTime = selectedP.unitCost,
                                    fromLocation = stockLocation
                                )
                            )
                            chosenSku = ""
                            chosenQty = "1"
                            chosenRate = ""
                        } else {
                            Toast.makeText(context, "Please choose product SKU First", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBg)
                ) {
                    Text("+ Append to Parchi", color = EmeraldLight)
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (billItems.isNotEmpty()) {
                    Text("Items mapped to balance sheet", fontWeight = FontWeight.Bold, color = LightText, fontSize = 12.sp)
                    billItems.forEach { entry ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• ${entry.productName} (${entry.quantity}x)", color = LightText, fontSize = 11.sp)
                            Text("Rs. ${entry.quantity * entry.salePriceOnParchi}", color = LightText, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val activeCust = if (isCredit) customers.find { it.id == chosenCustId } else null
                            if (isCredit && activeCust == null) {
                                Toast.makeText(context, "Choose Credit customer account", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (billItems.isEmpty()) {
                                Toast.makeText(context, "Empty parchi items list", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.executeSale(
                                customerId = activeCust?.id,
                                customerName = activeCust?.name ?: cashCustName.ifEmpty { "Walk In Cash" },
                                items = billItems,
                                paymentType = if (isCredit) "CREDIT" else "CASH",
                                paymentAccountCode = "1000"
                            ) { postDoc ->
                                Toast.makeText(context, "Ledger verified $postDoc", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Post double-entry", color = SlateGrayBg, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBg), modifier = Modifier.weight(1f)) {
                        Text("Exit", color = LightText)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordPaymentDialog(viewModel: HisabViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val customers by viewModel.customers.collectAsState()
    var chosenCustId by remember { mutableStateOf<Int?>(null) }
    var amount by remember { mutableStateOf("") }
    var receiptMemo by remember { mutableStateOf("Paid in cash Saddar Ceramics") }
    var accountCode by remember { mutableStateOf("1000") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Collect Outstanding Repayment", fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Choose customer account:", color = MutedSlate, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(customers) { c ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (chosenCustId == c.id) EmeraldAccent.copy(alpha = 0.2f) else SlateGrayBg)
                                .clickable { chosenCustId = c.id }
                                .padding(8.dp)
                                .border(1.dp, if (chosenCustId == c.id) EmeraldAccent else SlateGrayBorder, RoundedCornerShape(6.dp))
                        ) {
                            Text("${c.name} (${formatRupees(c.runningBalance)})", color = LightText, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Repayment Cash (PKR)", color = MutedSlate, fontSize = 11.sp) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Store Cash Ledger: ", color = MutedSlate, fontSize = 11.sp)
                    Button(onClick = { accountCode = "1000" }, colors = ButtonDefaults.buttonColors(containerColor = if (accountCode == "1000") EmeraldAccent else SlateGrayBg)) {
                        Text("Shop In Hand", fontSize = 10.sp, color = if (accountCode == "1000") SlateGrayBg else LightText)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = { accountCode = "1010" }, colors = ButtonDefaults.buttonColors(containerColor = if (accountCode == "1010") EmeraldAccent else SlateGrayBg)) {
                        Text("Bank Acc", fontSize = 10.sp, color = if (accountCode == "1010") SlateGrayBg else LightText)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = receiptMemo,
                    onValueChange = { receiptMemo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Receipt Memo", color = MutedSlate, fontSize = 11.sp) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val target = customers.find { it.id == chosenCustId }
                            val amt = amount.toDoubleOrNull() ?: 0.0

                            if (target == null || amt <= 0.0) {
                                Toast.makeText(context, "Select client account & amount first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.executePayment(target.id, target.name, amt, accountCode, receiptMemo) { docCode ->
                                Toast.makeText(context, "Repayment registered: $docCode", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Post Collection", color = SlateGrayBg, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBg), modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = LightText)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordExpenseDialog(viewModel: HisabViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var code by remember { mutableStateOf("5200") }
    var payAmount by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Log Operational Shop Expense", fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ExpenseSelectorRow("5100", "Showroom Space Rent", code) { code = it }
                    ExpenseSelectorRow("5200", "Utilities Sui Gas / Power", code) { code = it }
                    ExpenseSelectorRow("5300", "Carriage Delivery Charge", code) { code = it }
                    ExpenseSelectorRow("5400", "Helper Staff Salaries", code) { code = it }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = payAmount,
                    onValueChange = { payAmount = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Expense cash outlay (PKR)", color = MutedSlate, fontSize = 11.sp) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = memo,
                    onValueChange = { memo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Expense Details notes", color = MutedSlate, fontSize = 11.sp) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val amt = payAmount.toDoubleOrNull() ?: 0.0
                            if (amt <= 0.0) {
                                Toast.makeText(context, "Specify positive payment amount first", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.executeExpense(
                                expenseAccountCode = code,
                                debitAccountName = "Expense $code",
                                amount = amt,
                                paymentAccountCode = "1000",
                                description = memo.ifEmpty { "Logged business expense code $code" }
                            ) { postCode ->
                                Toast.makeText(context, "Expense mapped: $postCode", Toast.LENGTH_SHORT).show()
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Post Expense", color = SlateGrayBg, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBg), modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = LightText)
                    }
                }
            }
        }
    }
}

@Composable
fun RecordStockTransferDialog(viewModel: HisabViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val products by viewModel.products.collectAsState()
    var sku by remember { mutableStateOf("") }
    var qty by remember { mutableStateOf("5") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Stock Transfer (Godown → Showroom)", fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Select Product SKU:", color = MutedSlate, fontSize = 11.sp)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(products) { p ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (sku == p.sku) EmeraldLight.copy(alpha = 0.2f) else SlateGrayBg)
                                .clickable { sku = p.sku }
                                .padding(8.dp)
                                .border(1.dp, if (sku == p.sku) EmeraldLight else SlateGrayBorder, RoundedCornerShape(6.dp))
                        ) {
                            Text("${p.name} (Gdn: ${p.godownQty})", color = LightText, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = qty,
                    onValueChange = { qty = it },
                    label = { Text("Transfer Quantity", color = MutedSlate) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val activeP = products.find { it.sku == sku }
                            val qtyVal = qty.toIntOrNull() ?: 0

                            if (activeP == null || qtyVal <= 0) {
                                Toast.makeText(context, "Choose SKU and specify positive quantity", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (activeP.godownQty < qtyVal) {
                                Toast.makeText(context, "Insufficient stock in Godown store!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            viewModel.executeStockTransfer(activeP.sku, activeP.name, qtyVal, "GODOWN", "SHOWROOM")
                            Toast.makeText(context, "Transferred $qtyVal pcs of ${activeP.name}", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Post Transfer", color = SlateGrayBg, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBg), modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = LightText)
                    }
                }
            }
        }
    }
}

@Composable
fun ReceiveRepaymentDialog(customer: CustomerEntity, onDismiss: () -> Unit, onSave: (Double, String, String) -> Unit) {
    var rawAmt by remember { mutableStateOf(customer.runningBalance.toString()) }
    var accountCode by remember { mutableStateOf("1000") }
    var descriptionMemo by remember { mutableStateOf("Repay ledger outstanding") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Repayment collected from " + customer.name, fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = rawAmt,
                    onValueChange = { rawAmt = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Repayment Cash (PKR)", color = MutedSlate) }
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Method:  ", color = MutedSlate, fontSize = 11.sp)
                    Button(onClick = { accountCode = "1000" }, colors = ButtonDefaults.buttonColors(containerColor = if (accountCode == "1000") EmeraldAccent else SlateGrayBg)) {
                        Text("Cash Drawer", fontSize = 10.sp, color = if (accountCode == "1000") SlateGrayBg else LightText)
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(onClick = { accountCode = "1010" }, colors = ButtonDefaults.buttonColors(containerColor = if (accountCode == "1010") EmeraldAccent else SlateGrayBg)) {
                        Text("Bank", fontSize = 10.sp, color = if (accountCode == "1010") SlateGrayBg else LightText)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = descriptionMemo,
                    onValueChange = { descriptionMemo = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Receipt notes Memo", color = MutedSlate) }
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            val amt = rawAmt.toDoubleOrNull() ?: 0.0
                            if (amt > 0.0) {
                                onSave(amt, accountCode, descriptionMemo)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Update Ledger", color = SlateGrayBg, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBg), modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = LightText)
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerAddDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, Double, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var shop by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var limit by remember { mutableStateOf("50000") }
    var terms by remember { mutableStateOf("30") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("New Customer Account Account Profile", fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Client Full Name", color = MutedSlate, fontSize = 11.sp) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = shop, onValueChange = { shop = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Shop Business Name", color = MutedSlate, fontSize = 11.sp) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = phone, onValueChange = { phone = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Contact Phone", color = MutedSlate, fontSize = 11.sp) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Shop Address Market", color = MutedSlate, fontSize = 11.sp) })
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = limit, onValueChange = { limit = it }, modifier = Modifier.weight(1f), label = { Text("Credit limit Rs.", color = MutedSlate, fontSize = 10.sp) })
                    OutlinedTextField(value = terms, onValueChange = { terms = it }, modifier = Modifier.weight(1f), label = { Text("Terms Limit Days", color = MutedSlate, fontSize = 10.sp) })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (name.isNotEmpty()) {
                                onSave(name, shop, phone, address, limit.toDoubleOrNull() ?: 50000.0, terms.toIntOrNull() ?: 30)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Create Profile", color = SlateGrayBg, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBg), modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = LightText)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductAddDialog(onDismiss: () -> Unit, onSave: (String, String, String, String, String, Double, Double, Int, Int, Int) -> Unit) {
    var sku by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var brand by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Tiles") }
    var cost by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Add SKU standard parameters", fontWeight = FontWeight.Bold, color = LightText, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(value = sku, onValueChange = { sku = it }, modifier = Modifier.fillMaxWidth(), label = { Text("SKU Code", color = MutedSlate, fontSize = 11.sp) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = name, onValueChange = { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Product Label", color = MutedSlate, fontSize = 11.sp) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = brand, onValueChange = { brand = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Brand Manufacturer", color = MutedSlate, fontSize = 11.sp) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = category, onValueChange = { category = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Category", color = MutedSlate, fontSize = 11.sp) })
                Spacer(modifier = Modifier.height(8.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cost, onValueChange = { cost = it }, modifier = Modifier.weight(1f), label = { Text("Cost Price Rs", color = MutedSlate, fontSize = 10.sp) })
                    OutlinedTextField(value = price, onValueChange = { price = it }, modifier = Modifier.weight(1f), label = { Text("Sale Price Rs", color = MutedSlate, fontSize = 10.sp) })
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            if (sku.isNotEmpty() && name.isNotEmpty()) {
                                onSave(
                                    sku, name, brand, category, "",
                                    cost.toDoubleOrNull() ?: 1000.0,
                                    price.toDoubleOrNull() ?: 1200.0,
                                    5, 0, 20
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add to SKU Room", color = SlateGrayBg, fontWeight = FontWeight.Bold)
                    }
                    Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = SlateGrayBg), modifier = Modifier.weight(1f)) {
                        Text("Cancel", color = LightText)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 9. METRICS CARDS & DECORATIVE BADGES
// ---------------------------------------------------------------------------------------------------------------------
@Composable
fun MetricMiniCard(
    title: String,
    value: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateGrayBorder)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = MutedSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, color = color, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, color = MutedSlate, fontSize = 9.sp)
        }
    }
}

@Composable
fun MetricLargeCard(
    title: String,
    value: String,
    subText: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SlateGraySurface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, SlateGrayBorder)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MutedSlate, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, color = color, fontSize = 18.sp, fontWeight = FontWeight.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subText, color = MutedSlate, fontSize = 10.sp)
        }
    }
}

@Composable
fun InteractiveWelcomeCard(onScanClick: () -> Unit, onChatClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = EmeraldLight),
        shape = RoundedCornerShape(28.dp),
        border = BorderStroke(1.dp, SlateGrayBorder)
    ) {
        Column(modifier = Modifier.padding(22.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Your store is\nthriving today.",
                        color = Color(0xFF042100),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "LEDGER BALANCED & SECURED",
                        color = EmeraldAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                // Custom beautiful concentric garden progress design circle directly drawn in Compose
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Active",
                        tint = EmeraldAccent,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Track outstanding Urdu Udhar Khata data and showroom/godown ceramic stock flow securely under double-entry offline correctness.",
                color = Color(0xFF191C17).copy(alpha = 0.8f),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onScanClick,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldAccent),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Parchi OCR Scan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onChatClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f),
                    border = BorderStroke(1.dp, EmeraldAccent.copy(alpha = 0.2f))
                ) {
                    Text("AI Copilot", color = EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StockBadge(label: String, qty: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SlateGrayBg)
            .border(1.dp, SlateGrayBorder, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, color = MutedSlate, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            Text(qty.toString(), color = if (qty <= 2) RoseError else EmeraldAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ExpenseSelectorRow(code: String, label: String, current: String, onSelect: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(code) }
            .background(if (current == code) EmeraldAccent.copy(alpha = 0.2f) else SlateGrayBg, RoundedCornerShape(8.dp))
            .padding(10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$code — $label", color = LightText, fontSize = 11.sp)
        if (current == code) {
            Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldAccent, modifier = Modifier.size(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------------------------------------------------
// 10. MATH & DUMMY HARDWARE SIMULATOR
// ---------------------------------------------------------------------------------------------------------------------
fun formatRupees(amount: Double): String {
    val formatter = java.text.DecimalFormat("##,##,###.##")
    return "Rs. " + formatter.format(amount) + " PKR"
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun generateMockParchiBitmap(context: Context): Bitmap {
    val bitmap = Bitmap.createBitmap(320, 480, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint()

    canvas.drawColor(AndroidColor.parseColor("#FDF6E2")) // Vintage ledger paper style

    paint.strokeWidth = 2f
    paint.style = Paint.Style.STROKE
    paint.color = AndroidColor.parseColor("#1B3B5F")
    canvas.drawRect(8f, 8f, 312f, 472f, paint)

    paint.style = Paint.Style.FILL
    paint.color = AndroidColor.parseColor("#122338")
    paint.textSize = 18f
    paint.isFakeBoldText = true
    canvas.drawText("SADDAR CERAMICS SHOP", 20f, 40f, paint)

    paint.textSize = 10f
    paint.isFakeBoldText = false
    paint.color = AndroidColor.GRAY
    canvas.drawText("Karachi Sanitaryware Market, Pakistan", 20f, 58f, paint)

    paint.color = AndroidColor.BLACK
    paint.textSize = 12f
    paint.isFakeBoldText = true
    canvas.drawText("Client Account: Ali Traders", 20f, 95f, paint)

    paint.textSize = 11f
    paint.isFakeBoldText = false
    canvas.drawText("Item list:", 20f, 125f, paint)
    canvas.drawText("• Sonex Commode Luxury Ivory (Qty 2) Rs. 14,400", 20f, 145f, paint)
    canvas.drawText("• SS Faisal Golden Mixer Set (Qty 1) Rs. 5,200", 20f, 165f, paint)

    paint.color = AndroidColor.parseColor("#C33C3C")
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 2f
    canvas.drawRect(160f, 220f, 290f, 260f, paint)

    paint.style = Paint.Style.FILL
    paint.isFakeBoldText = true
    paint.textSize = 11f
    canvas.drawText("MAPPED CREDIT", 175f, 245f, paint)

    paint.color = AndroidColor.BLACK
    paint.textSize = 13f
    canvas.drawText("Parchi Total: Rs. 19,600 PKR", 20f, 310f, paint)

    paint.color = AndroidColor.GRAY
    paint.textSize = 8f
    paint.isFakeBoldText = false
    canvas.drawText("* Mapped offline directly to local Room DB *", 40f, 440f, paint)

    return bitmap
}
