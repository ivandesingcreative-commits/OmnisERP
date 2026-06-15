package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MainAppWorkspace()
        }
    }
}

// ---------------------------------------------------------------------
// COLOR SYSTEM ACCORDING TO HIGH-CONTRAST PRINCIPLES: OLED Black vs Pure White
// ---------------------------------------------------------------------

val OledBackground = Color(0xFF000000)
val OledSurface = Color(0xFF121212)
val OledBorder = Color(0xFF2C2C2E)
val OledTextPrimary = Color(0xFFFFFFFF)
val OledTextSecondary = Color(0xFF8E8E93)
val OledActiveGreen = Color(0xFF30D158)
val OledActiveBlue = Color(0xFF0A84FF)
val OledAccentAlert = Color(0xFFFF453A)

val PureWhiteBackground = Color(0xFFFFFFFF)
val PureWhiteSurface = Color(0xFFF2F2F7)
val PureWhiteBorder = Color(0xFFE5E5EA)
val PureWhiteTextPrimary = Color(0xFF1C1C1E)
val PureWhiteTextSecondary = Color(0xFF636366)
val PureWhiteActiveGreen = Color(0xFF34C759)
val PureWhiteActiveBlue = Color(0xFF007AFF)
val PureWhiteAccentAlert = Color(0xFFFF3B30)

@Composable
fun MainAppWorkspace() {
    val context = LocalContext.current
    val stateFile = remember { File(context.filesDir, "erp_pos_state.json") }

    // Init state from local file or template defaults
    var erpState by remember {
        mutableStateOf(
            if (stateFile.exists()) {
                try {
                    ErpState.fromJSONString(stateFile.readText())
                } catch (e: Exception) {
                    ErpState.createInitialDefaultState()
                }
            } else {
                ErpState.createInitialDefaultState()
            }
        )
    }

    // Save State to local storage helper
    fun saveState(newState: ErpState) {
        erpState = newState
        try {
            stateFile.writeText(newState.toJSONString())
        } catch (e: Exception) {
            Toast.makeText(context, "Error al guardar localmente", Toast.LENGTH_SHORT).show()
        }
    }

    // Session controls
    var activeUser by remember { mutableStateOf<Employee?>(null) }
    var selectedOperationMode by remember { mutableStateOf("POS") } // "POS" or "Cliente"
    var activeSubView by remember { mutableStateOf("CATALOG") } // Inside Client: "DASHBOARD", "INVENTORY", "RECIPES", "EMPLOYEES", "TOPOLOGY", "PRINTERS", "SETTINGS", "JSON_WORKSPACE"
    
    // Flow parameters
    var activeCart by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var showTimeClock by remember { mutableStateOf(false) }
    var activeCategoryFilter by remember { mutableStateOf("Todos") }

    // Navigation and sub-dialog states
    var checkoutTicketToShow by remember { mutableStateOf<Order?>(null) }
    var cashDialogOpen by remember { mutableStateOf<String?>(null) } // "OPEN", "ARQUEO", "CLOSE"

    val isDark = erpState.isDarkTheme
    val bgColor = if (isDark) OledBackground else PureWhiteBackground
    val surfaceColor = if (isDark) OledSurface else PureWhiteSurface
    val borderColor = if (isDark) OledBorder else PureWhiteBorder
    val textPrimary = if (isDark) OledTextPrimary else PureWhiteTextPrimary
    val textSecondary = if (isDark) OledTextSecondary else PureWhiteTextSecondary
    val activeColor = if (isDark) OledActiveGreen else PureWhiteActiveGreen
    val accentColor = if (isDark) OledActiveBlue else PureWhiteActiveBlue
    val alertColor = if (isDark) OledAccentAlert else PureWhiteAccentAlert

    // Verify Active Cash register Status (MANDATORY REQUIREMENT)
    val activeSession = erpState.cashSessions.find { it.isOpen && it.tokenDevice == erpState.deviceTokenId }
    val isCashRegisterOpen = activeSession != null

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // 1. TOP HEADER APP BAR
            TopWorkspaceBar(
                state = erpState,
                activeUser = activeUser,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                borderColor = borderColor,
                surfaceColor = surfaceColor,
                activeColor = activeColor,
                onThemeToggle = {
                    saveState(erpState.copy(isDarkTheme = !isDark))
                },
                onLogOut = {
                    activeUser = null
                    activeCart = emptyList()
                    showTimeClock = false
                    Toast.makeText(context, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                }
            )

            // 2. MAIN HUB WORKSPACE WHEN LOGGED IN / ACCESS GATEWAY BEFORE
            if (activeUser == null) {
                // Access gateway screen (View_Login)
                ViewLoginScreen(
                    state = erpState,
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    activeColor = activeColor,
                    accentColor = accentColor,
                    alertColor = alertColor,
                    selectedOperationMode = selectedOperationMode,
                    onModeChange = { selectedOperationMode = it },
                    onLoginSuccess = { user ->
                        activeUser = user
                        // Configure initial active sub-view based on logged user constraints
                        if (user.role.contains("Root")) {
                            activeSubView = "DASHBOARD"
                        } else if (user.role.contains("Nivel 2")) {
                            activeSubView = "INVENTORY"
                        } else {
                            activeSubView = "INVENTORY"
                        }
                        Toast.makeText(context, "Bienvenido: ${user.name}", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // Logged In: Dual Panel ERP Dashboard
                Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                    
                    // Left Side Menu - Hamburger representation
                    SidebarNavMenu(
                        user = activeUser!!,
                        selectedMode = selectedOperationMode,
                        activeSubView = activeSubView,
                        isDark = isDark,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        borderColor = borderColor,
                        surfaceColor = surfaceColor,
                        activeColor = activeColor,
                        accentColor = accentColor,
                        onModeSelect = { selectedOperationMode = it },
                        onSubViewSelect = { activeSubView = it },
                        onInitiateCashRegisterAction = { action ->
                            cashDialogOpen = action
                        },
                        isCashRegisterOpen = isCashRegisterOpen,
                        activeSessionAmount = activeSession?.currentAmount ?: 0.0
                    )

                    // Main Active Interface Viewport
                    Box(modifier = Modifier.fillMaxHeight().weight(1f).border(1.dp, borderColor)) {
                        when (selectedOperationMode) {
                            "POS" -> {
                                if (erpState.assignedDeviceRole == "Pantalla de Pedidos") {
                                    // Pantalla de Pedidos (KDS) Screen
                                    KdsScreen(
                                        state = erpState,
                                        isDark = isDark,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        surfaceColor = surfaceColor,
                                        borderColor = borderColor,
                                        activeColor = activeColor,
                                        onUpdateStatus = { orderId, nextStatus ->
                                            val updatedOrders = erpState.orders.map {
                                                if (it.id == orderId) it.copy(status = nextStatus) else it
                                            }
                                            saveState(erpState.copy(orders = updatedOrders))
                                            Toast.makeText(context, "Pedido actualizado a $nextStatus", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } else {
                                    // Main Point of Sale Workspace
                                    ViewPosWorkspace(
                                        state = erpState,
                                        isDark = isDark,
                                        textPrimary = textPrimary,
                                        textSecondary = textSecondary,
                                        surfaceColor = surfaceColor,
                                        borderColor = borderColor,
                                        activeColor = activeColor,
                                        accentColor = accentColor,
                                        alertColor = alertColor,
                                        activeCart = activeCart,
                                        activeCategoryFilter = activeCategoryFilter,
                                        isCashRegisterOpen = isCashRegisterOpen,
                                        onCategoryFilterChange = { activeCategoryFilter = it },
                                        onAddToCart = { item ->
                                            if (!isCashRegisterOpen) {
                                                Toast.makeText(context, "Caja bloqueada. Abra sesión primero", Toast.LENGTH_LONG).show()
                                                return@ViewPosWorkspace
                                            }
                                            // Handle recipe parent or physical limits check
                                            val currentCartItem = activeCart.find { it.itemId == item.id }
                                            val nextQty = (currentCartItem?.quantity ?: 0) + 1
                                            
                                            // Validate Physical inventory on floor if physical item
                                            if (item.itemType == "Physical") {
                                                val stockNeeded = nextQty.toDouble()
                                                // If item has recipes:
                                                val associatedRecipes = erpState.recipes.filter { it.parentItemId == item.id }
                                                var ingredientError: String? = null
                                                for (recipe in associatedRecipes) {
                                                    val ingredient = erpState.inventory.find { it.id == recipe.ingredientItemId }
                                                    if (ingredient != null) {
                                                        val neededQty = recipe.quantityRequired * stockNeeded
                                                        if (neededQty > ingredient.stockFloor) {
                                                            ingredientError = "Insumo insuficiente: ${ingredient.name}"
                                                            break
                                                        }
                                                    }
                                                }

                                                if (ingredientError != null) {
                                                    Toast.makeText(context, "Alerta: $ingredientError", Toast.LENGTH_LONG).show()
                                                }

                                                if (stockNeeded > item.stockFloor && associatedRecipes.isEmpty()) {
                                                    Toast.makeText(context, "Alerta: Stock insuficiente en piso de venta", Toast.LENGTH_SHORT).show()
                                                    // Soft allowance or block based on strict check
                                                }
                                            }

                                            // Proceed to add to cart
                                            if (currentCartItem != null) {
                                                activeCart = activeCart.map {
                                                    if (it.itemId == item.id) it.copy(quantity = it.quantity + 1) else it
                                                }
                                            } else {
                                                activeCart = activeCart + OrderItem(item.id, item.name, item.price, 1)
                                            }
                                        },
                                        onRemoveFromCart = { item ->
                                            val existing = activeCart.find { it.itemId == item.itemId }
                                            if (existing != null) {
                                                if (existing.quantity > 1) {
                                                    activeCart = activeCart.map {
                                                        if (it.itemId == item.itemId) it.copy(quantity = it.quantity - 1) else it
                                                    }
                                                } else {
                                                    activeCart = activeCart.filter { it.itemId != item.itemId }
                                                }
                                            }
                                        },
                                        onClearCart = { activeCart = emptyList() },
                                        onCheckout = { cashAmount ->
                                            if (activeCart.isEmpty()) return@ViewPosWorkspace
                                            val total = activeCart.sumOf { it.price * it.quantity }
                                            if (cashAmount < total) {
                                                Toast.makeText(context, "Monto recibido insuficiente", Toast.LENGTH_SHORT).show()
                                                return@ViewPosWorkspace
                                            }

                                            // Process inventory deductions (recipes & floor stock)
                                            val updatedInventory = erpState.inventory.map { invItem ->
                                                var matchingItem = invItem
                                                // 1. Direct Item quantity discount if sold matching item is Physical
                                                activeCart.forEach { cartItem ->
                                                    if (cartItem.itemId == invItem.id && invItem.itemType == "Physical") {
                                                        matchingItem = matchingItem.copy(
                                                            stockFloor = (matchingItem.stockFloor - cartItem.quantity).coerceAtLeast(0.0)
                                                        )
                                                    }
                                                    
                                                    // 2. Ingredients deduction via Recipes
                                                    val recipesForThis = erpState.recipes.filter { it.parentItemId == cartItem.itemId }
                                                    recipesForThis.forEach { recipe ->
                                                        if (recipe.ingredientItemId == invItem.id) {
                                                            val totalDeduction = recipe.quantityRequired * cartItem.quantity
                                                            matchingItem = matchingItem.copy(
                                                                stockFloor = (matchingItem.stockFloor - totalDeduction).coerceAtLeast(0.0)
                                                            )
                                                        }
                                                    }
                                                }
                                                matchingItem
                                            }

                                            // Update cash register session amount
                                            val updatedSessions = erpState.cashSessions.map { s ->
                                                if (s.isOpen && s.tokenDevice == erpState.deviceTokenId) {
                                                    s.copy(currentAmount = s.currentAmount + total)
                                                } else s
                                            }

                                            // Simulate cooking printer routing
                                            val printersUsed = mutableListOf<String>()
                                            activeCart.forEach { item ->
                                                val invSource = erpState.inventory.find { it.id == item.itemId }
                                                val category = invSource?.category ?: "General"
                                                val route = erpState.printerRoutes.find { it.category == category }
                                                val printer = erpState.printers.find { it.id == route?.printerId }
                                                if (printer != null && !printersUsed.contains(printer.name)) {
                                                    printersUsed.add(printer.name)
                                                }
                                            }

                                            // If Solo POS device, order state is saved but restricted for transaction close.
                                            // Here we handle terminal rules:
                                            val finalStatus = if (erpState.assignedDeviceRole == "Solo POS") "Pendiente" else "Completado"

                                            val newOrder = Order(
                                                tokenDevice = erpState.deviceTokenId,
                                                timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                                                items = activeCart,
                                                totalAmount = total,
                                                cashReceived = cashAmount,
                                                changeReturned = cashAmount - total,
                                                openedSessionId = activeSession?.id ?: "OFFLINE-TRANS",
                                                printedPrinters = if (printersUsed.isEmpty()) listOf("Impresora Principal Termica") else printersUsed,
                                                status = finalStatus
                                            )

                                            saveState(erpState.copy(
                                                inventory = updatedInventory,
                                                cashSessions = updatedSessions,
                                                orders = erpState.orders + newOrder
                                            ))

                                            activeCart = emptyList()
                                            checkoutTicketToShow = newOrder
                                            Toast.makeText(context, "Transacción Procesada!", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            }
                            "Cliente" -> {
                                // Master ERP Control Viewport
                                ViewClientWorkspace(
                                    state = erpState,
                                    activeSubView = activeSubView,
                                    isDark = isDark,
                                    textPrimary = textPrimary,
                                    textSecondary = textSecondary,
                                    surfaceColor = surfaceColor,
                                    borderColor = borderColor,
                                    activeColor = activeColor,
                                    accentColor = accentColor,
                                    alertColor = alertColor,
                                    activeUser = activeUser!!,
                                    onSaveRawMatrix = { rawJson ->
                                        try {
                                            val parsed = ErpState.fromJSONString(rawJson)
                                            saveState(parsed)
                                            Toast.makeText(context, "¡Matriz de datos restaurada correctamente!", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Formato JSON de matriz inválido", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    onInventoryUpdate = { updatedList ->
                                        saveState(erpState.copy(inventory = updatedList))
                                    },
                                    onRecipesUpdate = { updatedRecipes ->
                                        saveState(erpState.copy(recipes = updatedRecipes))
                                    },
                                    onEmployeesUpdate = { updatedEmployees ->
                                        saveState(erpState.copy(employees = updatedEmployees))
                                    },
                                    onPrintersUpdate = { updatedPrinters, updatedRoutes ->
                                        saveState(erpState.copy(printers = updatedPrinters, printerRoutes = updatedRoutes))
                                    },
                                    onConfigUpdate = { updatedConfigState ->
                                        saveState(updatedConfigState)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. MANDATORY FLOATING ACCELERATOR: RELOJ CHECADOR (TIME STAMP CLOCK)
        // Strictly hidden under Cliente screens, available at Root landing & POS views
        if (selectedOperationMode == "POS" || activeUser == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                FloatingActionButton(
                    onClick = { showTimeClock = true },
                    containerColor = accentColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Reloj Checador",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // --- SUB-DIALOG: TIME CLOCK STAMP DIALOG ---
        if (showTimeClock) {
            TimeClockDialog(
                state = erpState,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surfaceColor = surfaceColor,
                borderColor = borderColor,
                activeColor = activeColor,
                accentColor = accentColor,
                alertColor = alertColor,
                onClose = { showTimeClock = false },
                onPunchRegistered = { pin, punchType ->
                    val employee = erpState.employees.find { it.pin == pin }
                    if (employee != null) {
                        val newPunch = TimePunch(
                            employeePin = employee.pin,
                            employeeName = employee.name,
                            punchTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                            punchType = punchType
                        )
                        saveState(erpState.copy(timePunches = erpState.timePunches + newPunch))
                        true // Punch accepted
                    } else {
                        false // PIN mismatch
                    }
                }
            )
        }

        // --- SUB-DIALOG: CHECKOUT SUCCESS EXPORTER ---
        checkoutTicketToShow?.let { order ->
            CheckoutTicketModal(
                order = order,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surfaceColor = surfaceColor,
                borderColor = borderColor,
                activeColor = activeColor,
                accentColor = accentColor,
                alertColor = alertColor,
                isSupabaseMirrorActive = erpState.isSupabaseMirrorActive,
                onDismiss = { checkoutTicketToShow = null }
            )
        }

        // --- SUB-DIALOGS FOR CASH SESSION MANAGEMENTS ---
        cashDialogOpen?.let { step ->
            CashSessionConfigModal(
                step = step,
                deviceToken = erpState.deviceTokenId,
                activeUser = activeUser,
                isDark = isDark,
                textPrimary = textPrimary,
                textSecondary = textSecondary,
                surfaceColor = surfaceColor,
                borderColor = borderColor,
                activeColor = activeColor,
                accentColor = accentColor,
                alertColor = alertColor,
                currentSession = activeSession,
                ordersInSession = erpState.orders.filter { it.openedSessionId == (activeSession?.id ?: "") },
                onConfirm = { initialAmount, auditAmount ->
                    val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    when (step) {
                        "OPEN" -> {
                            val newSession = CashSession(
                                tokenDevice = erpState.deviceTokenId,
                                openTime = nowStr,
                                initialAmount = initialAmount,
                                currentAmount = initialAmount,
                                openedBy = activeUser?.name ?: "Gerente",
                                isOpen = true
                            )
                            saveState(erpState.copy(cashSessions = erpState.cashSessions + newSession))
                            Toast.makeText(context, "Caja Abierta Exitosamente ($$initialAmount)", Toast.LENGTH_SHORT).show()
                        }
                        "ARQUEO" -> {
                            val target = activeSession?.currentAmount ?: 0.0
                            val difference = auditAmount - target
                            val msg = if (difference == 0.0) "¡Caja Balanceada Perfectamente!" else "Diferencia de Arqueo: $${String.format("%.2f", difference)}"
                            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                        }
                        "CLOSE" -> {
                            if (activeSession != null) {
                                val closedSession = activeSession.copy(
                                    isOpen = false,
                                    closeTime = nowStr,
                                    currentAmount = activeSession.currentAmount
                                )
                                val updatedList = erpState.cashSessions.map {
                                    if (it.id == activeSession.id) closedSession else it
                                }
                                
                                // Clean active cart on register close
                                activeCart = emptyList()

                                saveState(erpState.copy(cashSessions = updatedList))
                                Toast.makeText(context, "Caja Cerrada. Resumen Generado.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                    cashDialogOpen = null
                },
                onDismiss = { cashDialogOpen = null }
            )
        }
    }
}

// ---------------------------------------------------------------------
// 4. VIEW_LOGIN SCREEN: Numeric Keyboard Toggle Alphanumeric
// ---------------------------------------------------------------------

@Composable
fun ViewLoginScreen(
    state: ErpState,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    selectedOperationMode: String,
    onModeChange: (String) -> Unit,
    onLoginSuccess: (Employee) -> Unit
) {
    val context = LocalContext.current
    val bgColor = if (isDark) OledBackground else PureWhiteBackground
    var inputPin by remember { mutableStateOf("") }
    var inputEmail by remember { mutableStateOf("") }
    var inputPassword by remember { mutableStateOf("") }
    var keypadTypeIsNumeric by remember { mutableStateOf(true) } // Mode switcher keyboard

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Logo minimal restrict layout
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(surfaceColor)
                .border(2.dp, borderColor, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Logo ERP SaaS",
                tint = accentColor,
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "ERP + POS SaaS Local-First",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = "Sincronizador SQLite con Espejo Supabase",
            fontSize = 12.sp,
            color = textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle Operating Mode
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(12.dp))
                .background(surfaceColor)
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            listOf("POS", "Cliente").forEach { mode ->
                val active = selectedOperationMode == mode
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (active) accentColor else Color.Transparent)
                        .clickable {
                            onModeChange(mode)
                            keypadTypeIsNumeric = (mode == "POS")
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (mode == "POS") "Punto de Venta (POS)" else "Panel de Control",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (active) Color.White else textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // PIN / Credential Form Box
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(surfaceColor, RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Acceso de Seguridad",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
                
                // Graphical Toggle Button for Keyboard Style
                IconButton(onClick = { keypadTypeIsNumeric = !keypadTypeIsNumeric }) {
                    Icon(
                        imageVector = if (keypadTypeIsNumeric) Icons.Default.Check else Icons.Default.Build,
                        contentDescription = "Cambiar Teclado",
                        tint = accentColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (keypadTypeIsNumeric) {
                // Pin Numeric display (6 Digit)
                Text(
                    text = if (inputPin.isEmpty()) "• • • • • •" else inputPin.map { "•" }.joinToString(" "),
                    fontSize = 28.sp,
                    color = textPrimary,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Interactive 6-Digit Graphic Pin Keyboard
                val keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "Borrar", "0", "Acceso")
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..2) {
                                val index = row * 3 + col
                                if (index < keys.size) {
                                    val key = keys[index]
                                    val isAction = key == "Borrar" || key == "Acceso"
                                    Button(
                                        onClick = {
                                            if (key == "Borrar") {
                                                if (inputPin.isNotEmpty()) inputPin = inputPin.dropLast(1)
                                            } else if (key == "Acceso") {
                                                // Validate biometric pin or default PIN
                                                val found = state.employees.find { it.pin == inputPin }
                                                if (found != null) {
                                                    onLoginSuccess(found)
                                                } else {
                                                    Toast.makeText(context, "PIN incorrecto. Intente nuevamente", Toast.LENGTH_SHORT).show()
                                                    inputPin = ""
                                                }
                                            } else {
                                                if (inputPin.length < 6) inputPin += key
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAction) borderColor else bgColor,
                                            contentColor = textPrimary
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(52.dp)
                                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                            .padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = key,
                                            fontSize = if (isAction) 11.sp else 18.sp,
                                            color = if (key == "Acceso" && inputPin.length >= 4) activeColor else textPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Alphanumeric Standard Form for customers
                OutlinedTextField(
                    value = inputEmail,
                    onValueChange = { inputEmail = it },
                    label = { Text("Correo Electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = textSecondary,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = inputPassword,
                    onValueChange = { inputPassword = it },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = borderColor,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = textSecondary,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val clientEmployee = state.employees.find { it.pin == "abcd1234@_" } ?: Employee("abcd1234@_", "Cliente Demo", "Cliente Portal", false)
                        val isClientCredential = inputEmail.trim() == "abcd1234@_" || inputPassword.trim() == "abcd1234@_"
                        if (isClientCredential) {
                            onLoginSuccess(clientEmployee)
                        } else {
                            val found = state.employees.find { it.pin == inputEmail.trim() || it.name.contains(inputEmail.trim(), ignoreCase = true) }
                            if (found != null) {
                                onLoginSuccess(found)
                            } else {
                                Toast.makeText(context, "Credencial incorrecta. Use abcd1234@_ para cliente", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Autenticarse", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive simulation guide to avoid data invention and ease evaluation
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor, RoundedCornerShape(8.dp))
                    .padding(10.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "GUÍA DE CREDENCIALES (MODO SIMULACIÓN):",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Cliente Alfanumérico: abcd1234@_\n" +
                           "• PIN Cajero: 123456\n" +
                           "• PIN Gerente: 090909\n" +
                           "• Reloj Checador: 1369 y 0000",
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    color = textSecondary,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// 5. SIDEBAR NAVIGATION CONTROLLERS representation
// ---------------------------------------------------------------------

@Composable
fun SidebarNavMenu(
    user: Employee,
    selectedMode: String,
    activeSubView: String,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color,
    surfaceColor: Color,
    activeColor: Color,
    accentColor: Color,
    onModeSelect: (String) -> Unit,
    onSubViewSelect: (String) -> Unit,
    onInitiateCashRegisterAction: (String) -> Unit,
    isCashRegisterOpen: Boolean,
    activeSessionAmount: Double
) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(220.dp)
            .background(surfaceColor)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = borderColor,
                    start = Offset(size.width - strokeWidth / 2, 0f),
                    end = Offset(size.width - strokeWidth / 2, size.height),
                    strokeWidth = strokeWidth
                )
            }
            .padding(12.dp)
    ) {
        // Active Staff Badge
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFE5E5EA), RoundedCornerShape(10.dp))
                .padding(12.dp)
        ) {
            Text(user.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
            Text(user.role, fontSize = 10.sp, color = textSecondary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Device Oper Role Label Mode Toggle Drawer
        Text("ÁREAS DISPONIBLES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(6.dp))

        // Mode Switching Toggles
        SidebarItem(
            label = "Punto de Venta (POS)",
            icon = Icons.Default.ShoppingCart,
            selected = selectedMode == "POS",
            activeColor = accentColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = { onModeSelect("POS") }
        )

        SidebarItem(
            label = "Panel General (ERP)",
            icon = Icons.Default.Home,
            selected = selectedMode == "Cliente",
            activeColor = accentColor,
            textPrimary = textPrimary,
            textSecondary = textSecondary,
            onClick = { onModeSelect("Cliente") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Divider(color = borderColor)
        Spacer(modifier = Modifier.height(16.dp))

        // SubViews relative to active Mode choice
        if (selectedMode == "Cliente") {
            Text("MÓDULOS DE CONTROL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))

            val items = if (user.role.contains("Root")) {
                listOf(
                    Triple("Métricas de Venta", "DASHBOARD", Icons.Default.Star),
                    Triple("Control de Almacén", "INVENTORY", Icons.Default.List),
                    Triple("Recetario", "RECIPES", Icons.Default.Edit),
                    Triple("Plantilla Personal", "EMPLOYEES", Icons.Default.Person),
                    Triple("Topología de Red", "TOPOLOGY", Icons.Default.Share),
                    Triple("Impresoras", "PRINTERS", Icons.Default.Build),
                    Triple("Soporte Supabase", "SETTINGS", Icons.Default.Refresh),
                    Triple("Matriz Estado JSON", "JSON_WORKSPACE", Icons.Default.Send)
                )
            } else if (user.role.contains("Nivel 2")) {
                listOf(
                    Triple("Control de Almacén", "INVENTORY", Icons.Default.List),
                    Triple("Recetario", "RECIPES", Icons.Default.Edit),
                    Triple("Plantilla Personal", "EMPLOYEES", Icons.Default.Person)
                )
            } else {
                listOf(
                    Triple("Control de Almacén", "INVENTORY", Icons.Default.List)
                )
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(items) { item ->
                    SidebarItem(
                        label = item.first,
                        icon = item.third,
                        selected = activeSubView == item.second,
                        activeColor = activeColor,
                        textPrimary = textPrimary,
                        textSecondary = textSecondary,
                        onClick = { onSubViewSelect(item.second) }
                    )
                }
            }
        } else {
            // Cash session controller widgets inside POS view
            Text("ESTADO DE CAJA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
            Spacer(modifier = Modifier.height(8.dp))

            if (isCashRegisterOpen) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, activeColor, RoundedCornerShape(8.dp))
                        .background(activeColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Column {
                        Text("Caja Abierta", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Efectivo: $${String.format("%.2f", activeSessionAmount)}", color = textPrimary, fontWeight = FontWeight.Medium, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onInitiateCashRegisterAction("ARQUEO") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = textPrimary),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Arqueo Caja", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))

                Button(
                    onClick = { onInitiateCashRegisterAction("CLOSE") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = activeColor),
                    border = BorderStroke(1.dp, activeColor),
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Cerrar Turno", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Text("Caja Cerrada\nNo se permite venta", fontSize = 11.sp, color = textSecondary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { onInitiateCashRegisterAction("OPEN") },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                    modifier = Modifier.fillMaxWidth().height(38.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Apertura Caja", fontSize = 11.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    activeColor: Color,
    textPrimary: Color,
    textSecondary: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) activeColor.copy(alpha = 0.12f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) activeColor else textSecondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) textPrimary else textSecondary
        )
    }
}

// ---------------------------------------------------------------------
// 6. VIEW_POS: ribbon with products, responsive screen structure
// ---------------------------------------------------------------------

@Composable
fun ViewPosWorkspace(
    state: ErpState,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    activeCart: List<OrderItem>,
    activeCategoryFilter: String,
    isCashRegisterOpen: Boolean,
    onCategoryFilterChange: (String) -> Unit,
    onAddToCart: (InventoryItem) -> Unit,
    onRemoveFromCart: (OrderItem) -> Unit,
    onClearCart: () -> Unit,
    onCheckout: (Double) -> Unit
) {
    var rawCashReceivedInput by remember { mutableStateOf("") }
    
    // Auto-calculates total order amount
    val totalAmountOfOrder = activeCart.sumOf { it.price * it.quantity }

    Row(modifier = Modifier.fillMaxSize()) {
        
        // Product Catalog area
        Column(modifier = Modifier.weight(1.4f).fillMaxHeight().padding(12.dp)) {
            
            // Ribbon list of category tags
            val categories = listOf("Todos", "Alimentos", "Bebidas", "Cocina", "Servicios")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                items(categories) { cat ->
                    val isSel = activeCategoryFilter == cat
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSel) accentColor else surfaceColor)
                            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                            .clickable { onCategoryFilterChange(cat) }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = cat,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) Color.White else textSecondary
                        )
                    }
                }
            }

            // Products ribbon grid
            val filteredInventory = state.inventory.filter {
                !it.isSoftDeleted && (activeCategoryFilter == "Todos" || it.category == activeCategoryFilter)
            }

            if (filteredInventory.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay productos disponibles.", color = textSecondary, fontSize = 13.sp)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredInventory) { item ->
                        val hasStock = item.itemType == "Service" || item.stockFloor > 0
                        Box(
                            modifier = Modifier
                                .height(105.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(surfaceColor)
                                .border(1.dp, if (hasStock) borderColor else alertColor.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { onAddToCart(item) }
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                                Column {
                                    Text(
                                        text = item.name,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary,
                                        maxLines = 2
                                    )
                                    Text(
                                        text = item.category,
                                        fontSize = 9.sp,
                                        color = textSecondary
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$${String.format("%.2f", item.price)}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = activeColor
                                    )
                                    
                                    // Stock quantity pill
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (hasStock) Color.Transparent else alertColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (item.itemType == "Service") "∞" else "S:${item.stockFloor.toInt()}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (hasStock) textSecondary else alertColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Active Cart Workspace (Collapsable checkout sidebar)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(surfaceColor)
                .drawBehind {
                    val strokeWidth = 1.dp.toPx()
                    drawLine(
                        color = borderColor,
                        start = Offset(strokeWidth / 2, 0f),
                        end = Offset(strokeWidth / 2, size.height),
                        strokeWidth = strokeWidth
                    )
                }
                .padding(12.dp)
        ) {
            Text(
                text = "TICKET ACTUAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Cart Items List
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(activeCart) { cartItem ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(cartItem.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text(
                                "${cartItem.quantity} x $${String.format("%.2f", cartItem.price)}",
                                fontSize = 10.sp,
                                color = textSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "$${String.format("%.2f", cartItem.price * cartItem.quantity)}",
                                fontSize = 11.sp,
                                color = textPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            IconButton(
                                onClick = { onRemoveFromCart(cartItem) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = alertColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Divider(color = borderColor, modifier = Modifier.padding(vertical = 8.dp))

            // Totals and cash intake fields
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total a Pagar:", color = textSecondary, fontSize = 12.sp)
                    Text("$${String.format("%.2f", totalAmountOfOrder)}", color = activeColor, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input cash payment amount
                OutlinedTextField(
                    value = rawCashReceivedInput,
                    onValueChange = { rawCashReceivedInput = it },
                    label = { Text("Efectivo Recibido ($)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = borderColor,
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = textSecondary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val amount = rawCashReceivedInput.toDoubleOrNull() ?: totalAmountOfOrder
                        onCheckout(amount)
                        rawCashReceivedInput = ""
                    },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                    shape = RoundedCornerShape(8.dp),
                    enabled = isCashRegisterOpen && activeCart.isNotEmpty()
                ) {
                    Text(
                        text = if (!isCashRegisterOpen) "CAJA CERRADA" else "COBRAR TICKET",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// 7. VIEW_CLIENT: MASTER ERP CONTROLLER WORKSPACE
// ---------------------------------------------------------------------

@Composable
fun ViewClientWorkspace(
    state: ErpState,
    activeSubView: String,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    activeUser: Employee,
    onSaveRawMatrix: (String) -> Unit,
    onInventoryUpdate: (List<InventoryItem>) -> Unit,
    onRecipesUpdate: (List<Recipe>) -> Unit,
    onEmployeesUpdate: (List<Employee>) -> Unit,
    onPrintersUpdate: (List<HardwarePrinter>, List<PrinterRoute>) -> Unit,
    onConfigUpdate: (ErpState) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color.Transparent)
    ) {
        when (activeSubView) {
            "DASHBOARD" -> {
                // Main Analytics & Metricas
                Text("MÉTRICAS DE VENTA Y DESEMPEÑO", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // KPI Total Transacted
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(surfaceColor, RoundedCornerShape(10.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("Ingresos Totales", color = textSecondary, fontSize = 11.sp)
                            Text("$${String.format("%.2f", state.orders.sumOf { it.totalAmount })}", color = activeColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // KPI Orders Count
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(surfaceColor, RoundedCornerShape(10.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        Column {
                            Text("Transacciones", color = textSecondary, fontSize = 11.sp)
                            Text("${state.orders.size} órdenes", color = textPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // KPI Average ticket
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(surfaceColor, RoundedCornerShape(10.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                            .padding(16.dp)
                    ) {
                        val avg = if (state.orders.isNotEmpty()) state.orders.sumOf { it.totalAmount } / state.orders.size else 0.0
                        Column {
                            Text("Ticket Promedio", color = textSecondary, fontSize = 11.sp)
                            Text("$${String.format("%.2f", avg)}", color = accentColor, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Order history
                Text("HISTORIAL DE FACTURACIÓN LOCAL", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.orders.reversed()) { o ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(surfaceColor, RoundedCornerShape(8.dp))
                                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Venta ID: ${o.id.take(8).uppercase()}", fontSize = 11.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                                Text("${o.timestamp} • ${o.items.sumOf { it.quantity }} prod.", fontSize = 10.sp, color = textSecondary)
                            }
                            Text("$${String.format("%.2f", o.totalAmount)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = activeColor)
                        }
                    }
                }
            }

            "INVENTORY" -> {
                // Stock logistics warehouse / floor (Nivel 2/1)
                InventoryLogisticsPanel(
                    state = state,
                    user = activeUser,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    activeColor = activeColor,
                    accentColor = accentColor,
                    alertColor = alertColor,
                    onUpdateInventory = onInventoryUpdate
                )
            }

            "RECIPES" -> {
                // Recipe constructor creator
                RecipesCreatorBuilder(
                    state = state,
                    user = activeUser,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    activeColor = activeColor,
                    accentColor = accentColor,
                    alertColor = alertColor,
                    onUpdateRecipes = onRecipesUpdate
                )
            }

            "EMPLOYEES" -> {
                // Staff control & Pin management (Nivel 2)
                EmployeeStaffPanel(
                    state = state,
                    isDark = isDark,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    activeColor = activeColor,
                    accentColor = accentColor,
                    alertColor = alertColor,
                    onUpdateEmployees = onEmployeesUpdate
                )
            }

            "TOPOLOGY" -> {
                // Network devices & Token IDs mapping
                DeviceNetworkTopologyPanel(
                    state = state,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    accentColor = accentColor,
                    onConfigUpdate = onConfigUpdate
                )
            }

            "PRINTERS" -> {
                // Printers config & routing category paths (Cooking hot printers)
                HardwarePrinterPanel(
                    state = state,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    activeColor = activeColor,
                    accentColor = accentColor,
                    alertColor = alertColor,
                    onPrintersUpdate = onPrintersUpdate
                )
            }

            "SETTINGS" -> {
                // Supabase credentials configuration mirror
                SupabaseMirrorPanel(
                    state = state,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    activeColor = activeColor,
                    accentColor = accentColor,
                    onConfigUpdate = onConfigUpdate
                )
            }

            "JSON_WORKSPACE" -> {
                // PLAIN-TEXT MATRIX RAW DATABASE SWITCHER (ZERO LOSS RECOVERY)
                DeveloperJsonWorkspace(
                    state = state,
                    textPrimary = textPrimary,
                    textSecondary = textSecondary,
                    surfaceColor = surfaceColor,
                    borderColor = borderColor,
                    activeColor = activeColor,
                    accentColor = accentColor,
                    onSave = onSaveRawMatrix
                )
            }
        }
    }
}

// ---------------------------------------------------------------------
// 8. HIGH POLISH SUB-MODULES
// ---------------------------------------------------------------------

@Composable
fun InventoryLogisticsPanel(
    state: ErpState,
    user: Employee,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    onUpdateInventory: (List<InventoryItem>) -> Unit
) {
    var rawSearch by remember { mutableStateOf("") }
    
    // Add product parameters
    var showAddDialog by remember { mutableStateOf(false) }
    var prodName by remember { mutableStateOf("") }
    var prodCategory by remember { mutableStateOf("Alimentos") }
    var prodType by remember { mutableStateOf("Physical") } // Physical / Service
    var prodPrice by remember { mutableStateOf("") }
    var prodWarehouse by remember { mutableStateOf("") }
    var prodFloor by remember { mutableStateOf("") }

    // Logistic Transfer parameters
    var showTransferDialog by remember { mutableStateOf<InventoryItem?>(null) }
    var transferQty by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ALMACÉN E INVENTARIO LOGÍSTICO", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text(
                    text = "No se permiten borrados físicos. Restringido a borrado lógico.",
                    fontSize = 10.sp,
                    color = textSecondary
                )
            }

            // Standard Permission control: Only Root or Level 2 can create products
            if (user.role.contains("Root") || user.role.contains("Nivel 2")) {
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("+ Nuevo Elemento", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search engine
        OutlinedTextField(
            value = rawSearch,
            onValueChange = { rawSearch = it },
            label = { Text("Buscar por código o descripción...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = borderColor,
                focusedTextColor = textPrimary,
                unfocusedTextColor = textPrimary,
                focusedLabelColor = accentColor,
                unfocusedLabelColor = textSecondary
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Inventory list showing Warehouse vs Floor (Piso de Venta) stock
        val filtered = state.inventory.filter {
            !it.isSoftDeleted && (rawSearch.isEmpty() || it.name.contains(rawSearch, ignoreCase = true))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filtered) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor, RoundedCornerShape(10.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (item.itemType == "Physical") accentColor.copy(alpha = 0.15f) else activeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (item.itemType == "Physical") "Stock" else "Servicio",
                                    fontSize = 8.sp,
                                    color = if (item.itemType == "Physical") accentColor else activeColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text("${item.category} • Precio: $${String.format("%.2f", item.price)}", fontSize = 10.sp, color = textSecondary)
                        
                        if (item.itemType == "Physical") {
                            Text(
                                "Almacén Central: ${item.stockWarehouse.toInt()} pzas | Piso de Venta: ${item.stockFloor.toInt()} pzas",
                                fontSize = 10.sp,
                                color = textPrimary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Logistics floor transfer button (Nivel 1 capacity)
                        if (item.itemType == "Physical") {
                            IconButton(
                                onClick = { showTransferDialog = item },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(borderColor)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Traspasar", tint = accentColor, modifier = Modifier.size(16.dp))
                            }
                        }

                        // Soft delete icon: Root / Nivel 2 restricted
                        if (user.role.contains("Root") || user.role.contains("Nivel 2")) {
                            IconButton(
                                onClick = {
                                    // Soft delete (Soft Delete exclusive option)
                                    val updated = state.inventory.map {
                                        if (it.id == item.id) it.copy(isSoftDeleted = true) else it
                                    }
                                    onUpdateInventory(updated)
                                },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(borderColor)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Baja", tint = alertColor, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal dialog to add Item
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nuevo Artículo ERP", fontSize = 14.sp) },
            containerColor = surfaceColor,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = prodName,
                        onValueChange = { prodName = it },
                        label = { Text("Nombre del Producto") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Type Select selector
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Physical", "Service").forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (prodType == type) accentColor else borderColor, RoundedCornerShape(8.dp))
                                    .clickable { prodType = type }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(if (type == "Physical") "Físico (Con Stock)" else "Servicio", fontSize = 11.sp, color = textPrimary)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = prodCategory,
                        onValueChange = { prodCategory = it },
                        label = { Text("Categoría (e.g. Alimentos, Bebidas)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = prodPrice,
                        onValueChange = { prodPrice = it },
                        label = { Text("Precio de Venta ($)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (prodType == "Physical") {
                        OutlinedTextField(
                            value = prodWarehouse,
                            onValueChange = { prodWarehouse = it },
                            label = { Text("Cantidad en Almacén") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = prodFloor,
                            onValueChange = { prodFloor = it },
                            label = { Text("Cantidad en Piso Venta") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (prodName.isNotEmpty()) {
                            val priceValue = prodPrice.toDoubleOrNull() ?: 10.0
                            val pWarehouse = prodWarehouse.toDoubleOrNull() ?: 0.0
                            val pFloor = prodFloor.toDoubleOrNull() ?: 0.0

                            val newItem = InventoryItem(
                                name = prodName,
                                category = prodCategory,
                                itemType = prodType,
                                stockWarehouse = if (prodType == "Physical") pWarehouse else 0.0,
                                stockFloor = if (prodType == "Physical") pFloor else 0.0,
                                price = priceValue
                            )

                            onUpdateInventory(state.inventory + newItem)

                            // Clear
                            prodName = ""
                            prodCategory = "Alimentos"
                            prodPrice = ""
                            prodWarehouse = ""
                            prodFloor = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("Registrar", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cancelar", color = textSecondary) }
            }
        )
    }

    // Modal dialog for Logistics warehouse floor transfers
    showTransferDialog?.let { item ->
        AlertDialog(
            onDismissRequest = { showTransferDialog = null },
            title = { Text("Traspasar de Almacén a Piso de Venta", fontSize = 13.sp) },
            containerColor = surfaceColor,
            text = {
                Column {
                    Text(item.name, fontWeight = FontWeight.Bold, color = textPrimary, fontSize = 12.sp)
                    Text("Disponible en Almacén: ${item.stockWarehouse.toInt()} pzas", fontSize = 11.sp, color = textSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = transferQty,
                        onValueChange = { transferQty = it },
                        label = { Text("Cantidad a Traspasar") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qty = transferQty.toDoubleOrNull() ?: 0.0
                        if (qty > 0 && qty <= item.stockWarehouse) {
                            val updated = state.inventory.map {
                                if (it.id == item.id) {
                                    it.copy(
                                        stockWarehouse = it.stockWarehouse - qty,
                                        stockFloor = it.stockFloor + qty
                                    )
                                } else it
                            }
                            onUpdateInventory(updated)
                            transferQty = ""
                            showTransferDialog = null
                        } else {
                            // Transfer amount too high
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Confirmar Traspaso", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = null }) { Text("Cancelar", color = textSecondary) }
            }
        )
    }
}

// ---------------------------------------------------------------------
// 9. RECIPE BUILDER SYSTEM
// ---------------------------------------------------------------------

@Composable
fun RecipesCreatorBuilder(
    state: ErpState,
    user: Employee,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    onUpdateRecipes: (List<Recipe>) -> Unit
) {
    var showCreator by remember { mutableStateOf(false) }
    var selectedParentId by remember { mutableStateOf("") }
    var selectedIngredientId by remember { mutableStateOf("") }
    var inputAmountNeeded by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("CREACIÓN DE RECETAS (MATERIAS PRIMAS)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("El sistema descuenta del stock de insumos al vuelo durante la venta", fontSize = 10.sp, color = textSecondary)
            }

            if (user.role.contains("Root") || user.role.contains("Nivel 2")) {
                Button(
                    onClick = { showCreator = true },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("+ Nueva Receta", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Existing recipes list
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.recipes) { recipe ->
                val parentItem = state.inventory.find { it.id == recipe.parentItemId }
                val ingredientItem = state.inventory.find { it.id == recipe.ingredientItemId }

                if (parentItem != null && ingredientItem != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(surfaceColor, RoundedCornerShape(8.dp))
                            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Producto: ${parentItem.name}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                            Text("Insumo Requerido: ${ingredientItem.name}", fontSize = 11.sp, color = textSecondary)
                            Text("Cantidad por Unidad: ${recipe.quantityRequired} unidades", fontSize = 10.sp, color = activeColor, fontWeight = FontWeight.Medium)
                        }

                        // Recipe delete (Soft Deleted logic helper / deletion)
                        if (user.role.contains("Root") || user.role.contains("Nivel 2")) {
                            IconButton(
                                onClick = {
                                    val list = state.recipes.filter { it.id != recipe.id }
                                    onUpdateRecipes(list)
                                }
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = alertColor)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreator) {
        val eligibleParents = state.inventory.filter { !it.isSoftDeleted }
        val eligibleIngredients = state.inventory.filter { !it.isSoftDeleted && it.id != selectedParentId }

        AlertDialog(
            onDismissRequest = { showCreator = false },
            title = { Text("Estructura de Receta", fontSize = 13.sp) },
            containerColor = surfaceColor,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Paso 1: Seleccione producto padre a producir:", fontSize = 10.sp, color = textSecondary)
                    
                    // Simple Dropdown lists simulator inside alert
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(borderColor).padding(6.dp)) {
                        LazyColumn {
                            items(eligibleParents) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedParentId = p.id }
                                        .background(if (selectedParentId == p.id) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .padding(6.dp)
                                ) {
                                    Text(p.name, fontSize = 11.sp, color = textPrimary)
                                }
                            }
                        }
                    }

                    Text("Paso 2: Seleccione ingrediente de descuento:", fontSize = 10.sp, color = textSecondary)
                    Box(modifier = Modifier.fillMaxWidth().height(120.dp).background(borderColor).padding(6.dp)) {
                        LazyColumn {
                            items(eligibleIngredients) { ing ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedIngredientId = ing.id }
                                        .background(if (selectedIngredientId == ing.id) accentColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .padding(6.dp)
                                ) {
                                    Text(ing.name, fontSize = 11.sp, color = textPrimary)
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = inputAmountNeeded,
                        onValueChange = { inputAmountNeeded = it },
                        label = { Text("Cantidad Requerida (g, ml o pieza)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = inputAmountNeeded.toDoubleOrNull() ?: 1.0
                        if (selectedParentId.isNotEmpty() && selectedIngredientId.isNotEmpty()) {
                            val newRecipe = Recipe(
                                parentItemId = selectedParentId,
                                ingredientItemId = selectedIngredientId,
                                quantityRequired = amt
                            )
                            onUpdateRecipes(state.recipes + newRecipe)

                            // Clear
                            selectedParentId = ""
                            selectedIngredientId = ""
                            inputAmountNeeded = ""
                            showCreator = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("Registrar Receta", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreator = false }) { Text("Cerrar", color = textSecondary) }
            }
        )
    }
}

// ---------------------------------------------------------------------
// 10. STAFF PIN & EMPLOYEE LIST (Nivel 2)
// ---------------------------------------------------------------------

@Composable
fun EmployeeStaffPanel(
    state: ErpState,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    onUpdateEmployees: (List<Employee>) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }
    var empName by remember { mutableStateOf("") }
    var empPin by remember { mutableStateOf("") }
    var empRole by remember { mutableStateOf("Nivel 1 (Operaciones/Logística)") }
    var bioSupport by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("EQUIPO DE TRABAJO Y BIOMETRÍA", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("Registro de operarios con PIN numérico de acceso rápido", fontSize = 11.sp, color = textSecondary)
            }

            Button(
                onClick = { showDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("+ Nuevo Empleado", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Employees List
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.employees) { emp ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor, RoundedCornerShape(10.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(emp.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text(emp.role, fontSize = 10.sp, color = textSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Código PIN: ${emp.pin}", fontSize = 10.sp, color = textSecondary)
                            Spacer(modifier = Modifier.width(8.dp))
                            if (emp.biometricRegistered) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(activeColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text("Huella Registrada", fontSize = 8.sp, color = activeColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Soft Delete Rule (Do not remove Root)
                    IconButton(
                        onClick = {
                            if (emp.pin != "000000") {
                                val list = state.employees.filter { it.pin != emp.pin }
                                onUpdateEmployees(list)
                            }
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Baja", tint = alertColor)
                    }
                }
            }
        }
    }

    if (showDialog) {
        val roles = listOf("Root (Admin Maestro)", "Nivel 2 (Gerencia)", "Nivel 1 (Operaciones/Logística)")
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Registrar Empleado", fontSize = 13.sp) },
            containerColor = surfaceColor,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = empName,
                        onValueChange = { empName = it },
                        label = { Text("Nombre Completo") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = empPin,
                        onValueChange = { empPin = it },
                        label = { Text("PIN Numérico (6 dígitos sugeridos)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text("Rango Jerárquico:", fontSize = 10.sp, color = textSecondary)
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(borderColor).padding(4.dp)) {
                        LazyColumn {
                            items(roles) { r ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { empRole = r }
                                        .background(if (empRole == r) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                                        .padding(6.dp)
                                ) {
                                    Text(r, fontSize = 11.sp, color = textPrimary)
                                }
                            }
                        }
                    }

                    // Simulated Biometric Registration check
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = bioSupport,
                            onCheckedChange = { bioSupport = it },
                            colors = CheckboxDefaults.colors(checkedColor = activeColor)
                        )
                        Text("Activar Lector de Huella Dactilar", fontSize = 11.sp, color = textPrimary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (empName.isNotEmpty() && empPin.isNotEmpty()) {
                            val newEmp = Employee(
                                name = empName,
                                pin = empPin,
                                role = empRole,
                                biometricRegistered = bioSupport
                            )
                            onUpdateEmployees(state.employees + newEmp)

                            // Clear
                            empName = ""
                            empPin = ""
                            empRole = "Nivel 1 (Operaciones/Logística)"
                            bioSupport = false
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("Guardar Ficha", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cerrar", color = textSecondary) }
            }
        )
    }
}

// ---------------------------------------------------------------------
// 11. TOPOLOGY & TOKEN DEVICES MAPPING
// ---------------------------------------------------------------------

@Composable
fun DeviceNetworkTopologyPanel(
    state: ErpState,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    accentColor: Color,
    onConfigUpdate: (ErpState) -> Unit
) {
    val rolesList = listOf("Caja + POS", "Solo POS", "Pantalla de Pedidos")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("TOPOLOGÍA DE TERMINALES Y ROLES", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text("Control de periféricos y nodo operador en la red local", fontSize = 11.sp, color = textSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        // Token ID representation
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor, RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(16.dp)
        ) {
            Column {
                Text("Identificador Token del Dispositivo (ID Único)", color = textSecondary, fontSize = 11.sp)
                Text(state.deviceTokenId, color = accentColor, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Cualquier transacción o comanda emitida por este nodo se marcará con este Token ID.", fontSize = 10.sp, color = textSecondary)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Role Designation
        Text("Asignar Comportamiento del Nodo", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text("Define la interfaz y restricciones del operador local.", fontSize = 11.sp, color = textSecondary)

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rolesList.forEach { r ->
                val active = state.assignedDeviceRole == r
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (active) accentColor else surfaceColor)
                        .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                        .clickable { onConfigUpdate(state.copy(assignedDeviceRole = r)) }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = r,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (active) Color.White else textPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Explanation card of roles capabilities
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor, RoundedCornerShape(8.dp))
                .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CAPACIDADES DE LA RED LOCAL", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("• Caja + POS: Nodo transaccional maestro. Autoriza flujo monetario.", fontSize = 10.sp, color = textSecondary)
                Text("• Solo POS: Captura órdenes. El cobro final se delega a la caja principal.", fontSize = 10.sp, color = textSecondary)
                Text("• Pantalla de Pedidos: Comandero KDS lectura y despacho para cocina.", fontSize = 10.sp, color = textSecondary)
            }
        }
    }
}

// ---------------------------------------------------------------------
// 12. HARDWARE PRINTER ROUTING MODULE
// ---------------------------------------------------------------------

@Composable
fun HardwarePrinterPanel(
    state: ErpState,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    onPrintersUpdate: (List<HardwarePrinter>, List<PrinterRoute>) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var printerName by remember { mutableStateOf("") }
    var printerAddress by remember { mutableStateOf("") }
    var printType by remember { mutableStateOf("IP Network") } // IP Network / Bluetooth MAC

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("ENRUTADOR DE IMPRESORAS TÉRMICAS", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("Mapeo IP para comandas de cocina y tickets de corte de caja", fontSize = 11.sp, color = textSecondary)
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor)
            ) {
                Text("+ Agregar Impresora", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Printers Catalog
        Text("IMPRESORAS CONFIGURADAS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.height(160.dp)) {
            items(state.printers) { prn ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor, RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(prn.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("${prn.connectionType} • ${prn.address}", fontSize = 10.sp, color = textSecondary)
                    }

                    IconButton(
                        onClick = {
                            val list = state.printers.filter { it.id != prn.id }
                            onPrintersUpdate(list, state.printerRoutes.filter { it.printerId != prn.id })
                        }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Baja", tint = alertColor)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Categories Printer routing
        Text("BIFURCACIÓN DE PEDIDOS (IMPRESIONES INTENT)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text("Relaciona categorías específicas a impresoras térmicas destinadas.", fontSize = 10.sp, color = textSecondary)
        Spacer(modifier = Modifier.height(8.dp))

        val productCategories = listOf("Alimentos", "Bebidas", "Cocina", "Servicios", "General")

        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(productCategories) { cat ->
                val currentRoute = state.printerRoutes.find { it.category == cat }
                val destPrn = state.printers.find { it.id == currentRoute?.printerId }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(surfaceColor, RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Categoría: $cat", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)

                    // Printer selection dropdown simulator
                    var expandedChoiceBlock by remember { mutableStateOf(false) }

                    Box {
                        Text(
                            text = destPrn?.name ?: "Sin Enrutamiento (Omitido)",
                            color = if (destPrn != null) activeColor else textSecondary,
                            fontSize = 11.sp,
                            modifier = Modifier
                                .border(1.dp, borderColor, RoundedCornerShape(4.dp))
                                .clickable { expandedChoiceBlock = !expandedChoiceBlock }
                                .padding(8.dp)
                        )

                        if (expandedChoiceBlock) {
                            Column(
                                modifier = Modifier
                                    .width(180.dp)
                                    .background(surfaceColor)
                                    .border(1.dp, borderColor)
                                    .padding(4.dp)
                            ) {
                                state.printers.forEach { p ->
                                    Text(
                                        text = p.name,
                                        fontSize = 11.sp,
                                        color = textPrimary,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                val remainingRoutes = state.printerRoutes.filter { it.category != cat }
                                                val newRoute = PrinterRoute(category = cat, printerId = p.id)
                                                onPrintersUpdate(state.printers, remainingRoutes + newRoute)
                                                expandedChoiceBlock = false
                                            }
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Registrar Impresora Térmica", fontSize = 13.sp) },
            containerColor = surfaceColor,
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = printerName,
                        onValueChange = { printerName = it },
                        label = { Text("Nombre Referencia (ej. Cocina Principal)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("IP Network", "Bluetooth MAC").forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, if (printType == type) accentColor else borderColor, RoundedCornerShape(8.dp))
                                    .clickable { printType = type }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(type, fontSize = 11.sp, color = textPrimary)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = printerAddress,
                        onValueChange = { printerAddress = it },
                        label = { Text(if (printType == "IP Network") "Dirección IP (ej. 192.168.1.100)" else "MAC Address (ej. 00:0A:95:9D:68:16)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (printerName.isNotEmpty()) {
                            val newPrn = HardwarePrinter(
                                name = printerName,
                                connectionType = printType,
                                address = printerAddress
                            )
                            onPrintersUpdate(state.printers + newPrn, state.printerRoutes)

                            printerName = ""
                            printerAddress = ""
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = activeColor)
                ) {
                    Text("Conectar", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) { Text("Cerrar", color = textSecondary) }
            }
        )
    }
}

// ---------------------------------------------------------------------
// 13. SUPABASE CREDS PRESETS PANEL
// ---------------------------------------------------------------------

@Composable
fun SupabaseMirrorPanel(
    state: ErpState,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    onConfigUpdate: (ErpState) -> Unit
) {
    var rawUrl by remember { mutableStateOf(state.supabaseUrl) }
    var rawKey by remember { mutableStateOf(state.supabaseAnonKey) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text("CONEXIÓN DE MOTOR DUAL COMPARTIDA (SUPABASE)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text("Activa el espejo de sincronización cuando la sesión root inyecte las llaves.", fontSize = 11.sp, color = textSecondary)

        Spacer(modifier = Modifier.height(16.dp))

        // Active State Switcher
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(surfaceColor, RoundedCornerShape(10.dp))
                .border(1.dp, borderColor, RoundedCornerShape(10.dp))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Espejo en la Nube Supabase Activo", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("Sincroniza transacciones al cerrar caja.", fontSize = 10.sp, color = textSecondary)
            }

            Switch(
                checked = state.isSupabaseMirrorActive,
                onCheckedChange = { onConfigUpdate(state.copy(isSupabaseMirrorActive = it)) },
                colors = SwitchDefaults.colors(checkedThumbColor = activeColor)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (state.isSupabaseMirrorActive) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = rawUrl,
                    onValueChange = { rawUrl = it },
                    label = { Text("Supabase Proyecto URL") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = borderColor
                    )
                )

                OutlinedTextField(
                    value = rawKey,
                    onValueChange = { rawKey = it },
                    label = { Text("Supabase API Anon Key") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textPrimary,
                        unfocusedTextColor = textPrimary,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = borderColor
                    )
                )

                Button(
                    onClick = {
                        onConfigUpdate(state.copy(supabaseUrl = rawUrl, supabaseAnonKey = rawKey))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("Inyectar Llaves", color = Color.White, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulated diagnostics status
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, activeColor, RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text("MÉTRICAS DEL COORDINADOR DUAL", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textPrimary)
                        Text("• Socket Sincronizador: OK (Conectado)", fontSize = 10.sp, color = textSecondary)
                        Text("• Transacciones Espejo: ${state.orders.map { if (it.isSynced) 1 else 0 }.sum()} / ${state.orders.size} logs", fontSize = 10.sp, color = textSecondary)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// 14. RAW DATABASE JSON MATRICES WORKSPACE
// ---------------------------------------------------------------------

@Composable
fun DeveloperJsonWorkspace(
    state: ErpState,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    onSave: (String) -> Unit
) {
    var textInputState by remember { mutableStateOf(state.toJSONString(pretty = true)) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("MATRIZ DE DATOS JSON EN TEXTO PLANO", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text(
            text = "Copie y pegue esta matriz para exportación/importación sin pérdida de contexto entre iteraciones.",
            fontSize = 11.sp,
            color = textSecondary
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = textInputState,
            onValueChange = { textInputState = it },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(surfaceColor),
            textStyle = androidx.compose.ui.text.TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = textPrimary
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = borderColor
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    onSave(textInputState)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = activeColor)
            ) {
                Text("Cargar Matriz de Estado", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    textInputState = state.toJSONString(pretty = true)
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = borderColor, contentColor = textPrimary)
            ) {
                Text("Actualizar Matriz")
            }
        }
    }
}

// ---------------------------------------------------------------------
// 15. KITCHEN PREPARATION QUEUE (KDS Screen - Read Only Screen)
// ---------------------------------------------------------------------

@Composable
fun KdsScreen(
    state: ErpState,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    onUpdateStatus: (String, String) -> Unit
) {
    // Show orders that has "Pendiente", "Preparado", "Entregado" status
    val ordersToPrep = state.orders.filter { it.status != "Completado" }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("PANTALLA DE PEDIDOS Y COMANDAS KDS (KITCHEN DISPATCH)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textPrimary)
        Text("Módulo de solo lectura para el control y despacho de órdenes preparatorias en cocina", fontSize = 11.sp, color = textSecondary)

        Spacer(modifier = Modifier.height(12.dp))

        if (ordersToPrep.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Cocina Despejada • No hay pedidos pendientes", color = activeColor, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ordersToPrep) { order ->
                    Box(
                        modifier = Modifier
                            .background(surfaceColor, RoundedCornerShape(8.dp))
                            .border(1.dp, if (order.status == "Pendiente") borderColor else activeColor, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Comanda: #${order.id.take(4).uppercase()}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (order.status == "Pendiente") Color.Yellow.copy(alpha = 0.2f) else activeColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                ) {
                                    Text(order.status, fontSize = 8.sp, color = textPrimary, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Order elements
                            order.items.forEach { item ->
                                Text("• ${item.quantity}x ${item.name}", fontSize = 11.sp, color = textPrimary)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action buttons
                            if (order.status == "Pendiente") {
                                Button(
                                    onClick = { onUpdateStatus(order.id, "Preparado") },
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Listo Cocina", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Button(
                                    onClick = { onUpdateStatus(order.id, "Completado") },
                                    modifier = Modifier.fillMaxWidth().height(32.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = activeColor),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Entregar Despacho", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------
// 16. DETAILED MODAL VIEWS
// ---------------------------------------------------------------------

@Composable
fun TimeClockDialog(
    state: ErpState,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    onClose: () -> Unit,
    onPunchRegistered: (String, String) -> Boolean
) {
    var rawPin by remember { mutableStateOf("") }
    var actionTypeSelected by remember { mutableStateOf("Punch In") } // Punch In / Punch Out
    var biometricScanInAction by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onClose,
        title = {
            Text(
                "RELOJ CHECADOR OPERARIOS",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = textPrimary,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        containerColor = surfaceColor,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Selector punch action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(borderColor)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (actionTypeSelected == "Punch In") activeColor else Color.Transparent)
                            .clickable { actionTypeSelected = "Punch In" }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Reg. Entrada", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (actionTypeSelected == "Punch In") Color.Black else textSecondary)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (actionTypeSelected == "Punch Out") alertColor else Color.Transparent)
                            .clickable { actionTypeSelected = "Punch Out" }
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Reg. Salida", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (actionTypeSelected == "Punch Out") Color.White else textSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Pin output indicator
                Text(
                    text = if (rawPin.isEmpty()) "• • • • • •" else rawPin.map { "•" }.joinToString(" "),
                    fontSize = 24.sp,
                    color = textPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Simulated Biometric Hub Touch Pad
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(if (biometricScanInAction) activeColor.copy(alpha = 0.2f) else borderColor)
                        .border(2.dp, if (biometricScanInAction) activeColor else textSecondary, CircleShape)
                        .clickable {
                            if (rawPin.isNotEmpty()) {
                                biometricScanInAction = true
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Simulador Biometría",
                        tint = if (biometricScanInAction) activeColor else textSecondary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Text("Simulando Lector Dactilar (Presione PIN + Huella)", fontSize = 10.sp, color = textSecondary)

                if (biometricScanInAction) {
                    // Quick processing simulation
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(1200)
                        val success = onPunchRegistered(rawPin, actionTypeSelected)
                        biometricScanInAction = false
                        if (success) {
                            rawPin = ""
                            onClose()
                        } else {
                            rawPin = ""
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Numbers keypad layout 
                val numbers = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "C", "0", "Aceptar")
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (row in 0..3) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (col in 0..2) {
                                val label = numbers[row * 3 + col]
                                Button(
                                    onClick = {
                                        if (label == "C") {
                                            rawPin = ""
                                        } else if (label == "Aceptar") {
                                            val valid = onPunchRegistered(rawPin, actionTypeSelected)
                                            if (valid) {
                                                rawPin = ""
                                                onClose()
                                            } else {
                                                rawPin = ""
                                            }
                                        } else {
                                            if (rawPin.length < 6) rawPin += label
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = borderColor, contentColor = textPrimary),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .padding(vertical = 1.dp)
                                ) {
                                    Text(label, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onClose) { Text("Cerrar", color = textSecondary) }
        }
    )
}

@Composable
fun CheckoutTicketModal(
    order: Order,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    isSupabaseMirrorActive: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Comprobante Simplificado ERP", fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        },
        containerColor = surfaceColor,
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("TRANSACCIÓN CON CLAVE UUID V4:", fontSize = 9.sp, color = textSecondary, fontFamily = FontFamily.Monospace)
                Text(order.id, fontSize = 9.sp, color = accentColor, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)

                Spacer(modifier = Modifier.height(10.dp))

                // Print category routing status
                order.printedPrinters.forEach { prn ->
                    Text("✓ Enrutado automático: $prn [Impreso]", color = activeColor, fontSize = 9.sp, fontWeight = FontWeight.Medium)
                }

                Divider(color = borderColor)

                order.items.forEach { line ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${line.quantity}x ${line.name}", fontSize = 11.sp, color = textPrimary)
                        Text("$${String.format("%.2f", line.price * line.quantity)}", fontSize = 11.sp, color = textPrimary)
                    }
                }

                Divider(color = borderColor)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("TOTAL FACTURADO:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textPrimary)
                    Text("$${String.format("%.2f", order.totalAmount)}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = activeColor)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Efectivo Recibido:", fontSize = 11.sp, color = textSecondary)
                    Text("$${String.format("%.2f", order.cashReceived)}", fontSize = 11.sp, color = textPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Cambio:", fontSize = 11.sp, color = textSecondary)
                    Text("$${String.format("%.2f", order.changeReturned)}", fontSize = 11.sp, color = textPrimary)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Shared report structure template
                Text("ACELERADORES DE EXPEDICIÓN:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textSecondary)
                
                Button(
                    onClick = {
                        // CFDI dispatch simulate
                    },
                    modifier = Modifier.fillMaxWidth().height(36.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = borderColor, contentColor = textPrimary),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Generar Petición Timbrado CFDI", fontSize = 10.sp)
                }

                if (isSupabaseMirrorActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, activeColor, RoundedCornerShape(4.dp))
                            .background(activeColor.copy(alpha = 0.05f))
                            .padding(8.dp)
                    ) {
                        Text("✓ Transacción resguardada exitosamente en el espejo Supabase remoto.", color = activeColor, fontSize = 9.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = accentColor)) {
                Text("Cerrar", color = Color.White)
            }
        }
    )
}

@Composable
fun CashSessionConfigModal(
    step: String,
    deviceToken: String,
    activeUser: Employee?,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    surfaceColor: Color,
    borderColor: Color,
    activeColor: Color,
    accentColor: Color,
    alertColor: Color,
    currentSession: CashSession?,
    ordersInSession: List<Order>,
    onConfirm: (Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var rawLimitValue by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (step) {
                    "OPEN" -> "Apertura de Caja Inicial"
                    "ARQUEO" -> "Arqueo Auditivo de Caja"
                    else -> "Cierre de Caja y Remisión"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        },
        containerColor = surfaceColor,
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Token Terminal: $deviceToken", fontSize = 10.sp, color = accentColor, fontFamily = FontFamily.Monospace)

                when (step) {
                    "OPEN" -> {
                        Text("Especifique fondo de efectivo inicial (Corte de caja):", fontSize = 11.sp, color = textSecondary)
                        OutlinedTextField(
                            value = rawLimitValue,
                            onValueChange = { rawLimitValue = it },
                            label = { Text("Efectivo Inicial ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "ARQUEO" -> {
                        Text("Especifique efectivo contado a mano en gaveta:", fontSize = 11.sp, color = textSecondary)
                        OutlinedTextField(
                            value = rawLimitValue,
                            onValueChange = { rawLimitValue = it },
                            label = { Text("Monto Contado ($)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "CLOSE" -> {
                        val salesSum = ordersInSession.sumOf { it.totalAmount }
                        val endSum = (currentSession?.initialAmount ?: 0.0) + salesSum

                        Text("CONFIRMACIÓN DE CONCORDANCIA MONETARIA:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                        Text("• Monto de Apertura: $${currentSession?.initialAmount}", fontSize = 11.sp, color = textSecondary)
                        Text("• Ingresos Recopilados: $$salesSum (Sobre ${ordersInSession.size} ventas)", fontSize = 11.sp, color = textSecondary)
                        Text("• Saldo Esperado en Caja: $$endSum", fontSize = 12.sp, color = activeColor, fontWeight = FontWeight.Bold)

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("Se sincronizarán todos los datos pendientes localmente con Supabase.", fontSize = 10.sp, color = textSecondary)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val limit = rawLimitValue.toDoubleOrNull() ?: 0.0
                    onConfirm(limit, limit)
                    rawLimitValue = ""
                },
                colors = ButtonDefaults.buttonColors(containerColor = activeColor)
            ) {
                Text(if (step == "CLOSE") "Cerrar Cierre" else "Confirmar", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = textSecondary) }
        }
    )
}

@Composable
fun TopWorkspaceBar(
    state: ErpState,
    activeUser: Employee?,
    isDark: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    borderColor: Color,
    surfaceColor: Color,
    activeColor: Color,
    onThemeToggle: () -> Unit,
    onLogOut: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceColor)
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                drawLine(
                    color = borderColor,
                    start = Offset(0f, size.height - strokeWidth / 2),
                    end = Offset(size.width, size.height - strokeWidth / 2),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(activeColor)
                    .padding(end = 10.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ERP + POS SaaS",
                color = textPrimary,
                fontWeight = FontWeight.Black,
                fontSize = 15.sp,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(borderColor)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "Local-First",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = textSecondary,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Theme selector (Exclusive Pure White / OLED Black)
            IconButton(onClick = onThemeToggle) {
                Icon(
                    imageVector = if (isDark) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = "Cambiar Tema",
                    tint = textPrimary
                )
            }

            if (activeUser != null) {
                // Logout action
                IconButton(onClick = onLogOut) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Cerrar Sesión",
                        tint = textPrimary
                    )
                }
            }
        }
    }
}
