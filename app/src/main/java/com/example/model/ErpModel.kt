package com.example.model

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// ---------------------------------------------------------------------
// 1. DATA MODELS & SCHEMAS
// ---------------------------------------------------------------------

data class Employee(
    val pin: String,
    val name: String,
    val role: String, // "Root (Admin Maestro)", "Nivel 2 (Gerencia)", "Nivel 1 (Operaciones/Logística)"
    val biometricRegistered: Boolean = false
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("pin", pin)
            put("name", name)
            put("role", role)
            put("biometricRegistered", biometricRegistered)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): Employee {
            return Employee(
                pin = json.optString("pin", "123456"),
                name = json.optString("name", "Usuario"),
                role = json.optString("role", "Nivel 1 (Operaciones/Logística)"),
                biometricRegistered = json.optBoolean("biometricRegistered", false)
            )
        }
    }
}

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String, // e.g., "Alimentos", "Bebidas", "Cocina", "Servicios"
    val itemType: String, // "Physical" (Exige control de almacén), "Service" (Servicio / stock infinito)
    val stockWarehouse: Double, // Almacén Central
    val stockFloor: Double,     // Piso de Venta
    val price: Double,
    val isSoftDeleted: Boolean = false
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("category", category)
            put("itemType", itemType)
            put("stockWarehouse", stockWarehouse)
            put("stockFloor", stockFloor)
            put("price", price)
            put("isSoftDeleted", isSoftDeleted)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): InventoryItem {
            return InventoryItem(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", "Producto"),
                category = json.optString("category", "General"),
                itemType = json.optString("itemType", "Physical"),
                stockWarehouse = json.optDouble("stockWarehouse", 0.0),
                stockFloor = json.optDouble("stockFloor", 0.0),
                price = json.optDouble("price", 0.0),
                isSoftDeleted = json.optBoolean("isSoftDeleted", false)
            )
        }
    }
}

data class Recipe(
    val id: String = UUID.randomUUID().toString(),
    val parentItemId: String,      // Item that is produced/serviced
    val ingredientItemId: String,  // Child inventory item discountable
    val quantityRequired: Double
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("parentItemId", parentItemId)
            put("ingredientItemId", ingredientItemId)
            put("quantityRequired", quantityRequired)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): Recipe {
            return Recipe(
                id = json.optString("id", UUID.randomUUID().toString()),
                parentItemId = json.optString("parentItemId", ""),
                ingredientItemId = json.optString("ingredientItemId", ""),
                quantityRequired = json.optDouble("quantityRequired", 1.0)
            )
        }
    }
}

data class CashSession(
    val id: String = UUID.randomUUID().toString(),
    val tokenDevice: String,
    val openTime: String,
    val closeTime: String? = null,
    val initialAmount: Double,
    val currentAmount: Double,
    val openedBy: String,
    val isOpen: Boolean = false
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("tokenDevice", tokenDevice)
            put("openTime", openTime)
            put("closeTime", closeTime ?: JSONObject.NULL)
            put("initialAmount", initialAmount)
            put("currentAmount", currentAmount)
            put("openedBy", openedBy)
            put("isOpen", isOpen)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): CashSession {
            return CashSession(
                id = json.optString("id", UUID.randomUUID().toString()),
                tokenDevice = json.optString("tokenDevice", "DEV-DEFAULT"),
                openTime = json.optString("openTime", ""),
                closeTime = if (json.isNull("closeTime")) null else json.optString("closeTime"),
                initialAmount = json.optDouble("initialAmount", 0.0),
                currentAmount = json.optDouble("currentAmount", 0.0),
                openedBy = json.optString("openedBy", ""),
                isOpen = json.optBoolean("isOpen", false)
            )
        }
    }
}

data class OrderItem(
    val itemId: String,
    val name: String,
    val price: Double,
    val quantity: Int
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("itemId", itemId)
            put("name", name)
            put("price", price)
            put("quantity", quantity)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): OrderItem {
            return OrderItem(
                itemId = json.optString("itemId", ""),
                name = json.optString("name", ""),
                price = json.optDouble("price", 0.0),
                quantity = json.optInt("quantity", 1)
            )
        }
    }
}

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val tokenDevice: String,
    val timestamp: String,
    val items: List<OrderItem>,
    val totalAmount: Double,
    val cashReceived: Double,
    val changeReturned: Double,
    val openedSessionId: String,
    val isSynced: Boolean = false,
    val printedPrinters: List<String> = emptyList(),
    val customerName: String = "Venta General",
    val status: String = "Completado" // "Pendiente", "Preparado", "Entregado", "Completado"
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("tokenDevice", tokenDevice)
            put("timestamp", timestamp)
            put("totalAmount", totalAmount)
            put("cashReceived", cashReceived)
            put("changeReturned", changeReturned)
            put("openedSessionId", openedSessionId)
            put("isSynced", isSynced)
            put("customerName", customerName)
            put("status", status)
            
            val arr = JSONArray()
            items.forEach { arr.put(it.toJSONObject()) }
            put("items", arr)

            val pArr = JSONArray()
            printedPrinters.forEach { pArr.put(it) }
            put("printedPrinters", pArr)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): Order {
            val list = mutableListOf<OrderItem>()
            val arr = json.optJSONArray("items")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    list.add(OrderItem.fromJSONObject(arr.getJSONObject(i)))
                }
            }

            val pList = mutableListOf<String>()
            val pArr = json.optJSONArray("printedPrinters")
            if (pArr != null) {
                for (i in 0 until pArr.length()) {
                    pList.add(pArr.getString(i))
                }
            }

            return Order(
                id = json.optString("id", UUID.randomUUID().toString()),
                tokenDevice = json.optString("tokenDevice", "DEV-DEFAULT"),
                timestamp = json.optString("timestamp", ""),
                items = list,
                totalAmount = json.optDouble("totalAmount", 0.0),
                cashReceived = json.optDouble("cashReceived", 0.0),
                changeReturned = json.optDouble("changeReturned", 0.0),
                openedSessionId = json.optString("openedSessionId", ""),
                isSynced = json.optBoolean("isSynced", false),
                printedPrinters = pList,
                customerName = json.optString("customerName", "Venta General"),
                status = json.optString("status", "Completado")
            )
        }
    }
}

data class HardwarePrinter(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val connectionType: String, // "IP Network", "Bluetooth MAC"
    val address: String          // IP or MAC
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("connectionType", connectionType)
            put("address", address)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): HardwarePrinter {
            return HardwarePrinter(
                id = json.optString("id", UUID.randomUUID().toString()),
                name = json.optString("name", ""),
                connectionType = json.optString("connectionType", "IP Network"),
                address = json.optString("address", "127.0.0.1")
            )
        }
    }
}

data class PrinterRoute(
    val id: String = UUID.randomUUID().toString(),
    val category: String,         // e.g. "Alimentos", "Bebidas", "General"
    val printerId: String         // link to HardwarePrinter.id
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("category", category)
            put("printerId", printerId)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): PrinterRoute {
            return PrinterRoute(
                id = json.optString("id", UUID.randomUUID().toString()),
                category = json.optString("category", "General"),
                printerId = json.optString("printerId", "")
            )
        }
    }
}

data class TimePunch(
    val id: String = UUID.randomUUID().toString(),
    val employeePin: String,
    val employeeName: String,
    val punchTime: String,
    val punchType: String // "Punch In" (Entrada), "Punch Out" (Salida)
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("employeePin", employeePin)
            put("employeeName", employeeName)
            put("punchTime", punchTime)
            put("punchType", punchType)
        }
    }

    companion object {
        fun fromJSONObject(json: JSONObject): TimePunch {
            return TimePunch(
                id = json.optString("id", UUID.randomUUID().toString()),
                employeePin = json.optString("employeePin", ""),
                employeeName = json.optString("employeeName", ""),
                punchTime = json.optString("punchTime", ""),
                punchType = json.optString("punchType", "Punch In")
            )
        }
    }
}

// ---------------------------------------------------------------------
// 2. ROOT ERPSATE STATE CONTAINER
// ---------------------------------------------------------------------

data class ErpState(
    val employees: List<Employee> = emptyList(),
    val inventory: List<InventoryItem> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val cashSessions: List<CashSession> = emptyList(),
    val orders: List<Order> = emptyList(),
    val printers: List<HardwarePrinter> = emptyList(),
    val printerRoutes: List<PrinterRoute> = emptyList(),
    val timePunches: List<TimePunch> = emptyList(),
    val deviceTokenId: String = "DEV-" + UUID.randomUUID().toString().take(6).uppercase(),
    val assignedDeviceRole: String = "Caja + POS", // "Caja + POS", "Solo POS", "Pantalla de Pedidos"
    val isSupabaseMirrorActive: Boolean = false,
    val supabaseUrl: String = "https://your-project.supabase.co",
    val supabaseAnonKey: String = "your-anon-key-placeholder",
    val isDarkTheme: Boolean = true // OLED Black by default
) {
    fun toJSONString(pretty: Boolean = true): String {
        val root = JSONObject()
        
        val empsArr = JSONArray()
        employees.forEach { empsArr.put(it.toJSONObject()) }
        root.put("employees", empsArr)

        val invArr = JSONArray()
        inventory.forEach { invArr.put(it.toJSONObject()) }
        root.put("inventory", invArr)

        val recsArr = JSONArray()
        recipes.forEach { recsArr.put(it.toJSONObject()) }
        root.put("recipes", recsArr)

        val cashArr = JSONArray()
        cashSessions.forEach { cashArr.put(it.toJSONObject()) }
        root.put("cashSessions", cashArr)

        val ordArr = JSONArray()
        orders.forEach { ordArr.put(it.toJSONObject()) }
        root.put("orders", ordArr)

        val printArr = JSONArray()
        printers.forEach { printArr.put(it.toJSONObject()) }
        root.put("printers", printArr)

        val routesArr = JSONArray()
        printerRoutes.forEach { routesArr.put(it.toJSONObject()) }
        root.put("printerRoutes", routesArr)

        val punchArr = JSONArray()
        timePunches.forEach { punchArr.put(it.toJSONObject()) }
        root.put("timePunches", punchArr)

        root.put("deviceTokenId", deviceTokenId)
        root.put("assignedDeviceRole", assignedDeviceRole)
        root.put("isSupabaseMirrorActive", isSupabaseMirrorActive)
        root.put("supabaseUrl", supabaseUrl)
        root.put("supabaseAnonKey", supabaseAnonKey)
        root.put("isDarkTheme", isDarkTheme)

        // Metadata of folder structures as requested
        val nestedTree = JSONObject().apply {
            put("/apps/pos-client", "Frontend PWA / Capacitor web assets compiling to APK")
            put("/packages/core-db", "SQLite architecture, OPFS sync layer & synchronization filters")
            put("/supabase/functions", "Cloud relay migration, REST auth triggers, and CFDI mock adapters")
            put("/.github/workflows", "Orchestrated CI/CD Android APK compiler & automatic static deployment pipelines")
        }
        root.put("monorepo_logical_tree", nestedTree)

        val packageJson = JSONObject().apply {
            put("name", "erp-pos-saas-superrepo")
            put("private", true)
            put("workspaces", JSONArray(listOf("apps/*", "packages/*")))
            val deps = JSONObject().apply {
                put("turbo", "^1.10.0")
                put("pnpm", "^8.6.0")
                put("typescript", "^5.1.3")
                put("capacitor-sqlite-wasm", "latest")
                put("@supabase/supabase-js", "latest")
            }
            put("devDependencies", deps)
        }
        root.put("root_package_json", packageJson)

        return if (pretty) root.toString(2) else root.toString()
    }

    companion object {
        fun fromJSONString(jsonStr: String): ErpState {
            try {
                val root = JSONObject(jsonStr)

                val emps = mutableListOf<Employee>()
                root.optJSONArray("employees")?.let { arr ->
                    for (i in 0 until arr.length()) emps.add(Employee.fromJSONObject(arr.getJSONObject(i)))
                }

                val inv = mutableListOf<InventoryItem>()
                root.optJSONArray("inventory")?.let { arr ->
                    for (i in 0 until arr.length()) inv.add(InventoryItem.fromJSONObject(arr.getJSONObject(i)))
                }

                val recs = mutableListOf<Recipe>()
                root.optJSONArray("recipes")?.let { arr ->
                    for (i in 0 until arr.length()) recs.add(Recipe.fromJSONObject(arr.getJSONObject(i)))
                }

                val cash = mutableListOf<CashSession>()
                root.optJSONArray("cashSessions")?.let { arr ->
                    for (i in 0 until arr.length()) cash.add(CashSession.fromJSONObject(arr.getJSONObject(i)))
                }

                val ords = mutableListOf<Order>()
                root.optJSONArray("orders")?.let { arr ->
                    for (i in 0 until arr.length()) ords.add(Order.fromJSONObject(arr.getJSONObject(i)))
                }

                val prnts = mutableListOf<HardwarePrinter>()
                root.optJSONArray("printers")?.let { arr ->
                    for (i in 0 until arr.length()) prnts.add(HardwarePrinter.fromJSONObject(arr.getJSONObject(i)))
                }

                val rtes = mutableListOf<PrinterRoute>()
                root.optJSONArray("printerRoutes")?.let { arr ->
                    for (i in 0 until arr.length()) rtes.add(PrinterRoute.fromJSONObject(arr.getJSONObject(i)))
                }

                val punches = mutableListOf<TimePunch>()
                root.optJSONArray("timePunches")?.let { arr ->
                    for (i in 0 until arr.length()) punches.add(TimePunch.fromJSONObject(arr.getJSONObject(i)))
                }

                return ErpState(
                    employees = emps,
                    inventory = inv,
                    recipes = recs,
                    cashSessions = cash,
                    orders = ords,
                    printers = prnts,
                    printerRoutes = rtes,
                    timePunches = punches,
                    deviceTokenId = root.optString("deviceTokenId", "DEV-MASTER"),
                    assignedDeviceRole = root.optString("assignedDeviceRole", "Caja + POS"),
                    isSupabaseMirrorActive = root.optBoolean("isSupabaseMirrorActive", false),
                    supabaseUrl = root.optString("supabaseUrl", "https://your-project.supabase.co"),
                    supabaseAnonKey = root.optString("supabaseAnonKey", "your-anon-key-placeholder"),
                    isDarkTheme = root.optBoolean("isDarkTheme", true)
                )
            } catch (e: Exception) {
                // Return default state with high-quality initial data on parsing crash
                return createInitialDefaultState()
            }
        }

        fun createInitialDefaultState(): ErpState {
            val initialEmployees = listOf(
                Employee("090909", "Margarita López (Gerente)", "Gerente (Administración)", true),
                Employee("123456", "Carlos Pérez (Cajero)", "Cajero (Operación)", false),
                Employee("1369", "Operario Turno Mañana (Reloj)", "Auxiliar General", false),
                Employee("0000", "Operario Turno Tarde (Reloj)", "Auxiliar General", false),
                Employee("abcd1234@_", "Cliente Demo", "Cliente Portal", false)
            )

            val initialInventory = listOf(
                // Services (stock null/infrito)
                InventoryItem(
                    id = "serv-001",
                    name = "Suscripción Premium Gimnasio",
                    category = "Servicios",
                    itemType = "Service",
                    stockWarehouse = 0.0,
                    stockFloor = 0.0,
                    price = 850.00
                ),
                InventoryItem(
                    id = "serv-002",
                    name = "Asesoría Contable / Financiera",
                    category = "Servicios",
                    itemType = "Service",
                    stockWarehouse = 0.0,
                    stockFloor = 0.0,
                    price = 500.00
                ),
                // Physical Assets
                InventoryItem(
                    id = "phys-001",
                    name = "Latas Refresco Cola 355ml",
                    category = "Bebidas",
                    itemType = "Physical",
                    stockWarehouse = 500.0,
                    stockFloor = 48.0,
                    price = 25.00
                ),
                InventoryItem(
                    id = "phys-002",
                    name = "Café Orgánico de Altura (Molido)",
                    category = "Alimentos",
                    itemType = "Physical",
                    stockWarehouse = 120.0,
                    stockFloor = 15.0,
                    price = 180.00
                ),
                // Ingredients
                InventoryItem(
                    id = "phys-ing-sugar",
                    name = "Azúcar Blanca Refinada (g)",
                    category = "Cocina",
                    itemType = "Physical",
                    stockWarehouse = 15000.0, // 15kg in Warehouse
                    stockFloor = 2000.0,       // 2kg on Floor
                    price = 0.05
                ),
                InventoryItem(
                    id = "phys-ing-milk",
                    name = "Leche Entera Modificada (ml)",
                    category = "Cocina",
                    itemType = "Physical",
                    stockWarehouse = 20000.0, // 20L
                    stockFloor = 3000.0,       // 3L
                    price = 0.03
                ),
                // Food Product with Recipe (Cappuccino que descuenta azúcar y leche)
                InventoryItem(
                    id = "phys-cappuccino",
                    name = "Cappuccino Italiano XL",
                    category = "Alimentos",
                    itemType = "Physical",
                    stockWarehouse = 0.0,
                    stockFloor = 100.0, // Available cups before checking ingredients
                    price = 65.00
                )
            )

            val initialRecipes = listOf(
                Recipe(parentItemId = "phys-cappuccino", ingredientItemId = "phys-ing-sugar", quantityRequired = 15.0), // 15g sugar
                Recipe(parentItemId = "phys-cappuccino", ingredientItemId = "phys-ing-milk", quantityRequired = 150.0) // 150ml milk
            )

            val initialPrinters = listOf(
                HardwarePrinter(id = "print-caja", name = "Impresora Principal Termica", connectionType = "IP Network", address = "192.168.1.100"),
                HardwarePrinter(id = "print-kitchen", name = "Comandas Cocina Caliente", connectionType = "IP Network", address = "192.168.1.250")
            )

            val initialPrinterRoutes = listOf(
                PrinterRoute(category = "Alimentos", printerId = "print-kitchen"),
                PrinterRoute(category = "Bebidas", printerId = "print-caja"),
                PrinterRoute(category = "Servicios", printerId = "print-caja"),
                PrinterRoute(category = "General", printerId = "print-caja")
            )

            return ErpState(
                employees = initialEmployees,
                inventory = initialInventory,
                recipes = initialRecipes,
                printers = initialPrinters,
                printerRoutes = initialPrinterRoutes
            )
        }
    }
}
