package vn.qcongnghe.qlbanhang.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.qcongnghe.qlbanhang.data.AppUser
import vn.qcongnghe.qlbanhang.data.CashEntry
import vn.qcongnghe.qlbanhang.data.Customer
import vn.qcongnghe.qlbanhang.data.Product
import vn.qcongnghe.qlbanhang.data.Sale
import vn.qcongnghe.qlbanhang.data.SaleLine
import vn.qcongnghe.qlbanhang.data.ServiceTicket
import vn.qcongnghe.qlbanhang.data.Supplier
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class Page(val title: String, val icon: String) {
    HOME("Tổng quan", "⌂"), POS("Bán hàng", "＋"), STOCK("Kho hàng", "▤"), CUSTOMERS("Khách hàng", "♙"), SUPPLIERS("Nhà cung cấp", "▣"), SERVICES("Sửa chữa", "⚙"), REPORTS("Báo cáo", "▥"), USERS("Nhân viên", "♧")
}
private data class CartRow(val product: Product, val quantity: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosApp(viewModel: PosViewModel) {
    val user by viewModel.user.collectAsStateWithLifecycle()
    val setupRequired by viewModel.setupRequired.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle(emptyList())
    val lowStock by viewModel.lowStock.collectAsStateWithLifecycle(emptyList())
    val customers by viewModel.customers.collectAsStateWithLifecycle(emptyList())
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle(emptyList())
    val users by viewModel.users.collectAsStateWithLifecycle(emptyList())
    val sales by viewModel.sales.collectAsStateWithLifecycle(emptyList())
    val tickets by viewModel.tickets.collectAsStateWithLifecycle(emptyList())
    val cashEntries by viewModel.cashEntries.collectAsStateWithLifecycle(emptyList())
    val message by viewModel.message.collectAsStateWithLifecycle()
    var page by remember(user?.id) { mutableStateOf(Page.HOME) }
    val cart = remember { mutableStateListOf<CartRow>() }
    var customerName by remember { mutableStateOf("") }

    LaunchedEffect(message) { if (message != null) { kotlinx.coroutines.delay(2800); viewModel.clearMessage() } }
    MaterialTheme {
        if (setupRequired == null) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) { Text("Đang khởi tạo dữ liệu…") }
        } else if (user == null) {
            LoginScreen(setupRequired == true, message, viewModel::createFirstAdmin, viewModel::login)
        } else {
            val allowed = when (user?.role) {
                "ADMIN" -> Page.entries.toList()
                "CASHIER" -> listOf(Page.HOME, Page.POS, Page.CUSTOMERS)
                "TECHNICIAN" -> listOf(Page.HOME, Page.SERVICES)
                else -> listOf(Page.HOME)
            }
            if (page !in allowed) page = allowed.first()
            Scaffold(
                topBar = { TopAppBar(title = { Text("QLBH Điện tử · ${user?.displayName}", fontWeight = FontWeight.Bold) }, actions = { TextButton(onClick = viewModel::logout) { Text("Đăng xuất") } }) },
                bottomBar = { NavigationBar { allowed.forEach { item -> NavigationBarItem(selected = page == item, onClick = { page = item }, icon = { Text(item.icon) }, label = { Text(item.title) }) } } },
                floatingActionButton = {
                    when (page) {
                        Page.STOCK -> AddProductButton(null, viewModel::saveProduct)
                        Page.CUSTOMERS -> AddCustomerButton(null, viewModel::saveCustomer)
                        Page.SUPPLIERS -> AddSupplierButton(null, viewModel::saveSupplier)
                        Page.SERVICES -> AddTicketButton(viewModel::addTicket)
                        Page.REPORTS -> AddCashButton(viewModel::addCashEntry)
                        Page.USERS -> AddUserButton(viewModel::createEmployee)
                        else -> Unit
                    }
                }
            ) { padding ->
                Column(Modifier.fillMaxSize().padding(padding)) {
                    when (page) {
                        Page.HOME -> HomeScreen(sales, products, lowStock, tickets, user?.displayName.orEmpty())
                        Page.POS -> PosScreen(products, cart, customerName, { customerName = it }) {
                            val total = cart.sumOf { it.product.price * it.quantity }
                            viewModel.checkout(cart.map { SaleLine(saleId = 0, productId = it.product.id, productName = it.product.name, quantity = it.quantity, unitPrice = it.product.price) }, customerName, "Tiền mặt", total)
                            cart.clear(); customerName = ""
                        }
                        Page.STOCK -> ProductScreen(products, viewModel::saveProduct, viewModel::addStock, viewModel::deleteProduct)
                        Page.CUSTOMERS -> CustomerScreen(customers, viewModel::saveCustomer, viewModel::deleteCustomer)
                        Page.SUPPLIERS -> SupplierScreen(suppliers, viewModel::saveSupplier, viewModel::deleteSupplier)
                        Page.SERVICES -> ServiceScreen(tickets, viewModel::advanceTicket)
                        Page.REPORTS -> ReportScreen(sales, cashEntries, lowStock, tickets)
                        Page.USERS -> UserScreen(users, user!!, viewModel::updateUser)
                    }
                    if (message != null) Text(message.orEmpty(), Modifier.fillMaxWidth().padding(10.dp), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(setup: Boolean, message: String?, onSetup: (String, String, String) -> Unit, onLogin: (String, String) -> Unit) {
    var username by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("QLBH Điện tử", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(if (setup) "Tạo tài khoản quản trị đầu tiên trên thiết bị này." else "Đăng nhập để tiếp tục.")
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(username, { username = it }, label = { Text("Tên đăng nhập") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        if (setup) OutlinedTextField(name, { name = it }, label = { Text("Tên hiển thị") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu (ít nhất 10 ký tự)") }, modifier = Modifier.fillMaxWidth(), visualTransformation = PasswordVisualTransformation(), singleLine = true)
        if (message != null) Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = { if (setup) onSetup(username, name, password) else onLogin(username, password) }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(if (setup) "Tạo tài khoản Admin" else "Đăng nhập") }
        if (setup) Text("Tài khoản được lưu cục bộ trên thiết bị. Hãy giữ an toàn thông tin đăng nhập.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 12.dp))
    }
}

@Composable
private fun HomeScreen(sales: List<Sale>, products: List<Product>, low: List<Product>, tickets: List<ServiceTicket>, name: String) {
    val today = sales.filter { isToday(it.createdAt) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Xin chào, $name", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Metric("Doanh thu hôm nay", money(today.sumOf { it.total }), Modifier.weight(1f)); Metric("Hóa đơn", "${today.size}", Modifier.weight(1f)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { Metric("Sản phẩm", "${products.size}", Modifier.weight(1f)); Metric("Phiếu đang xử lý", "${tickets.count { it.status != "Hoàn tất" }}", Modifier.weight(1f)) }
        Heading("Sắp hết hàng (${low.size})"); low.take(6).forEach { Text("• ${it.name}: còn ${it.stock}") }
        Heading("Lịch sửa chữa đang mở"); tickets.filter { it.status != "Hoàn tất" }.take(5).forEach { Text("• ${it.device} — ${it.status} — ${it.customerName}") }
    }
}

@Composable
private fun PosScreen(products: List<Product>, cart: MutableList<CartRow>, customerName: String, changeCustomer: (String) -> Unit, checkout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        OutlinedTextField(customerName, changeCustomer, label = { Text("Khách hàng (không bắt buộc)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Heading("Chạm sản phẩm để thêm vào giỏ")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(products.filter { it.stock > 0 }, key = { it.id }) { p -> Card(onClick = { val i = cart.indexOfFirst { it.product.id == p.id }; if (i >= 0) { if (cart[i].quantity < p.stock) cart[i] = cart[i].copy(quantity = cart[i].quantity + 1) } else cart.add(CartRow(p, 1)) }) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.SemiBold); Text("${p.sku} · Tồn ${p.stock}") }; Text(money(p.price)) } } }
        }
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp)) { Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Giỏ hàng (${cart.sumOf { it.quantity }})", fontWeight = FontWeight.Bold)
            cart.forEach { row -> Text("${row.product.name} × ${row.quantity} = ${money(row.product.price * row.quantity)}") }
            Text("Tổng: ${money(cart.sumOf { it.product.price * it.quantity })}", fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = cart::clear, modifier = Modifier.weight(1f)) { Text("Xóa") }; Button(onClick = checkout, enabled = cart.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Thanh toán tiền mặt") } }
        } }
    }
}

@Composable
private fun ProductScreen(products: List<Product>, save: (Product) -> Unit, addStock: (Long, Int) -> Unit, delete: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Heading("Kho hàng · ${products.size} sản phẩm"); Text("Nhấn + để thêm; bút chì để sửa; nhập nhanh số lượng.") }
        items(products, key = { it.id }) { p -> Card { Column(Modifier.fillMaxWidth().padding(12.dp)) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(p.name, fontWeight = FontWeight.Bold); Text("${p.sku} · ${p.category} · ${p.brand}"); Text("Giá vốn ${money(p.cost)} · Giá bán ${money(p.price)}"); Text("Tồn ${p.stock} · báo thấp ${p.minStock}") }; TextButton(onClick = { ProductDialog(p, save) { } }) { Text("Sửa") } }; Row { TextButton(onClick = { addStock(p.id, 1) }) { Text("+ Nhập 1") }; TextButton(onClick = { delete(p.id) }) { Text("Xóa") } } } } }
    }
}

@Composable
private fun CustomerScreen(rows: List<Customer>, save: (Customer) -> Unit, delete: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Heading("Khách hàng (${rows.size})") }; items(rows, key = { it.id }) { c -> Card { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(c.name, fontWeight = FontWeight.Bold); Text(c.phone); Text(c.note) }; TextButton(onClick = { CustomerDialog(c, save) }) { Text("Sửa") }; TextButton(onClick = { delete(c.id) }) { Text("Xóa") } } } } }
}

@Composable
private fun SupplierScreen(rows: List<Supplier>, save: (Supplier) -> Unit, delete: (Long) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Heading("Nhà cung cấp (${rows.size})") }; items(rows, key = { it.id }) { s -> Card { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(s.name, fontWeight = FontWeight.Bold); Text("${s.phone} · ${s.email}"); Text(s.address) }; TextButton(onClick = { SupplierDialog(s, save) }) { Text("Sửa") }; TextButton(onClick = { delete(s.id) }) { Text("Xóa") } } } } }
}

@Composable
private fun ServiceScreen(rows: List<ServiceTicket>, advance: (Long, String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Heading("Sửa chữa · bảo hành · lịch hẹn") }; if (rows.isEmpty()) item { Text("Chưa có phiếu. Nhấn + để tiếp nhận.") }; items(rows, key = { it.id }) { t -> Card { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text("${t.device} · ${t.status}", fontWeight = FontWeight.Bold); Text("${t.customerName} · ${t.phone}"); Text("Lỗi: ${t.issue}"); Text("Phí dự kiến: ${money(t.fee)}${t.appointmentAt?.let { " · Hẹn ${date(it)}" } ?: ""}"); if (t.status != "Hoàn tất") TextButton(onClick = { advance(t.id, t.status) }) { Text("Chuyển trạng thái") } } } } }
}

@Composable
private fun ReportScreen(sales: List<Sale>, cash: List<CashEntry>, low: List<Product>, tickets: List<ServiceTicket>) {
    val now = Calendar.getInstance(); val today = sales.filter { isToday(it.createdAt) }; val month = sales.filter { sameMonth(it.createdAt, now) }
    val monthIn = cash.filter { it.type == "Thu" && sameMonth(it.createdAt, now) }.sumOf { it.amount }; val monthOut = cash.filter { it.type == "Chi" && sameMonth(it.createdAt, now) }.sumOf { it.amount }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Heading("Báo cáo doanh thu · ${now.get(Calendar.MONTH) + 1}/${now.get(Calendar.YEAR)}")
        Metric("Doanh thu hôm nay", money(today.sumOf { it.total })); Metric("Doanh thu tháng", money(month.sumOf { it.total })); Metric("Hóa đơn tháng", "${month.size}")
        Metric("Thu khác tháng", money(monthIn)); Metric("Chi tháng", money(monthOut)); Metric("Thu - chi khác", money(monthIn - monthOut))
        Metric("Hàng sắp hết", "${low.size} sản phẩm"); Metric("Phiếu dịch vụ mở", "${tickets.count { it.status != "Hoàn tất" }}")
        Heading("Hóa đơn gần đây"); sales.take(15).forEach { Text("${dateTime(it.createdAt)} · ${it.customerName} · ${money(it.total)} · ${it.paymentMethod}") }
        Heading("Sổ thu chi"); cash.take(15).forEach { Text("${dateTime(it.createdAt)} · ${it.type} · ${it.title} · ${money(it.amount)}") }
    }
}

@Composable
private fun UserScreen(users: List<AppUser>, current: AppUser, update: (AppUser, Boolean, String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { item { Heading("Tài khoản nhân viên"); Text("Phân quyền áp dụng trong ứng dụng cục bộ trên thiết bị này.") }; items(users, key = { it.id }) { u -> Card { Column(Modifier.fillMaxWidth().padding(12.dp)) { Text("${u.displayName} (@${u.username})", fontWeight = FontWeight.Bold); Text("Vai trò: ${roleName(u.role)} · ${if (u.active) "Đang hoạt động" else "Đã khóa"}"); if (u.id != current.id) Row { TextButton(onClick = { update(u, !u.active, u.role) }) { Text(if (u.active) "Khóa tài khoản" else "Mở khóa") }; TextButton(onClick = { val next = when (u.role) { "ADMIN" -> "CASHIER"; "CASHIER" -> "TECHNICIAN"; else -> "ADMIN" }; update(u, u.active, next) }) { Text("Đổi quyền") } } } } } }
}

@Composable
private fun AddProductButton(initial: Product?, save: (Product) -> Unit) { var open by remember { mutableStateOf(false) }; FloatingActionButton(onClick = { open = true }) { Text("+") }; if (open) ProductDialog(initial, save) { open = false } }
@Composable
private fun ProductDialog(initial: Product?, save: (Product) -> Unit, close: () -> Unit) {
    var sku by remember(initial) { mutableStateOf(initial?.sku.orEmpty()) }; var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }; var category by remember(initial) { mutableStateOf(initial?.category ?: "Linh kiện PC") }; var brand by remember(initial) { mutableStateOf(initial?.brand.orEmpty()) }; var price by remember(initial) { mutableStateOf(initial?.price?.toString().orEmpty()) }; var cost by remember(initial) { mutableStateOf(initial?.cost?.toString().orEmpty()) }; var stock by remember(initial) { mutableStateOf(initial?.stock?.toString() ?: "0") }; var min by remember(initial) { mutableStateOf(initial?.minStock?.toString() ?: "2") }
    AlertDialog(onDismissRequest = close, title = { Text(if (initial == null) "Thêm sản phẩm" else "Sửa sản phẩm") }, text = { Column(Modifier.height(420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) { Field(sku, { sku = it }, "SKU*"); Field(name, { name = it }, "Tên*"); Field(category, { category = it }, "Danh mục"); Field(brand, { brand = it }, "Thương hiệu"); Field(price, { price = it }, "Giá bán (đ)*"); Field(cost, { cost = it }, "Giá vốn"); Field(stock, { stock = it }, "Tồn kho"); Field(min, { min = it }, "Ngưỡng cảnh báo") } }, confirmButton = { TextButton(onClick = { val p = price.toLongOrNull(); if (sku.isNotBlank() && name.isNotBlank() && p != null && p > 0) { save(Product(id = initial?.id ?: 0, sku = sku.trim(), name = name.trim(), category = category.trim(), brand = brand.trim(), price = p, cost = cost.toLongOrNull() ?: 0, stock = stock.toIntOrNull()?.coerceAtLeast(0) ?: 0, minStock = min.toIntOrNull()?.coerceAtLeast(0) ?: 2)); close() } }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = close) { Text("Hủy") } })
}

@Composable
private fun AddCustomerButton(initial: Customer?, save: (Customer) -> Unit) { var open by remember { mutableStateOf(false) }; FloatingActionButton(onClick = { open = true }) { Text("+") }; if (open) CustomerDialog(initial, save) { open = false } }
@Composable
private fun CustomerDialog(initial: Customer?, save: (Customer) -> Unit) { var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }; var phone by remember(initial) { mutableStateOf(initial?.phone.orEmpty()) }; var note by remember(initial) { mutableStateOf(initial?.note.orEmpty()) }; var open by remember { mutableStateOf(true) }; if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text("Khách hàng") }, text = { Column { Field(name, { name = it }, "Họ tên*"); Field(phone, { phone = it }, "Điện thoại"); Field(note, { note = it }, "Ghi chú") } }, confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { save(Customer(id = initial?.id ?: 0, name = name.trim(), phone = phone.trim(), note = note.trim())); open = false } }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Hủy") } }) }

@Composable
private fun AddSupplierButton(initial: Supplier?, save: (Supplier) -> Unit) { var open by remember { mutableStateOf(false) }; FloatingActionButton(onClick = { open = true }) { Text("+") }; if (open) SupplierDialog(initial, save) { open = false } }
@Composable
private fun SupplierDialog(initial: Supplier?, save: (Supplier) -> Unit, close: () -> Unit = {}) { var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }; var phone by remember(initial) { mutableStateOf(initial?.phone.orEmpty()) }; var email by remember(initial) { mutableStateOf(initial?.email.orEmpty()) }; var address by remember(initial) { mutableStateOf(initial?.address.orEmpty()) }; AlertDialog(onDismissRequest = close, title = { Text("Nhà cung cấp") }, text = { Column { Field(name, { name = it }, "Tên nhà cung cấp*"); Field(phone, { phone = it }, "Điện thoại"); Field(email, { email = it }, "Email"); Field(address, { address = it }, "Địa chỉ") } }, confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { save(Supplier(id = initial?.id ?: 0, name = name.trim(), phone = phone.trim(), email = email.trim(), address = address.trim())); close() } }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = close) { Text("Hủy") } }) }

@Composable
private fun AddTicketButton(save: (ServiceTicket) -> Unit) { var open by remember { mutableStateOf(false) }; FloatingActionButton(onClick = { open = true }) { Text("+") }; if (open) { var customer by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var device by remember { mutableStateOf("") }; var issue by remember { mutableStateOf("") }; var fee by remember { mutableStateOf("0") }; var appointment by remember { mutableStateOf("") }; AlertDialog(onDismissRequest = { open = false }, title = { Text("Tiếp nhận sửa chữa / bảo hành") }, text = { Column(Modifier.height(400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(5.dp)) { Field(customer, { customer = it }, "Khách hàng*"); Field(phone, { phone = it }, "Điện thoại"); Field(device, { device = it }, "Thiết bị*"); Field(issue, { issue = it }, "Mô tả lỗi*"); Field(fee, { fee = it }, "Phí dự kiến"); Field(appointment, { appointment = it }, "Lịch hẹn dd/MM/yyyy") } }, confirmButton = { TextButton(onClick = { val at = runCatching { SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).parse(appointment)?.time }.getOrNull(); if (customer.isNotBlank() && device.isNotBlank() && issue.isNotBlank()) { save(ServiceTicket(customerName = customer.trim(), phone = phone.trim(), device = device.trim(), issue = issue.trim(), fee = fee.toLongOrNull() ?: 0, appointmentAt = at)); open = false } }) { Text("Tạo phiếu") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Hủy") } }) } }

@Composable
private fun AddCashButton(save: (CashEntry) -> Unit) { var open by remember { mutableStateOf(false) }; FloatingActionButton(onClick = { open = true }) { Text("+") }; if (open) { var title by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var type by remember { mutableStateOf("Chi") }; AlertDialog(onDismissRequest = { open = false }, title = { Text("Phiếu thu / chi") }, text = { Column { Field(title, { title = it }, "Nội dung*"); Field(amount, { amount = it }, "Số tiền*"); Row { AssistChip(onClick = { type = "Thu" }, label = { Text("Thu") }); AssistChip(onClick = { type = "Chi" }, label = { Text("Chi") }) } } }, confirmButton = { TextButton(onClick = { val value = amount.toLongOrNull() ?: 0; if (title.isNotBlank() && value > 0) { save(CashEntry(title = title, amount = value, type = type)); open = false } }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Hủy") } }) } }

@Composable
private fun AddUserButton(create: (String, String, String, String) -> Unit) { var open by remember { mutableStateOf(false) }; FloatingActionButton(onClick = { open = true }) { Text("+") }; if (open) { var username by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }; var password by remember { mutableStateOf("") }; var role by remember { mutableStateOf("CASHIER") }; AlertDialog(onDismissRequest = { open = false }, title = { Text("Thêm nhân viên") }, text = { Column { Field(username, { username = it }, "Tên đăng nhập"); Field(name, { name = it }, "Tên hiển thị"); OutlinedTextField(password, { password = it }, label = { Text("Mật khẩu (10+ ký tự)") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()); Row { listOf("CASHIER", "TECHNICIAN", "ADMIN").forEach { r -> AssistChip(onClick = { role = r }, label = { Text("${roleName(r)}${if (role == r) " ✓" else ""}") }) } } } }, confirmButton = { TextButton(onClick = { create(username, name, password, role); open = false }) { Text("Tạo") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Hủy") } }) } }

@Composable
private fun Field(value: String, change: (String) -> Unit, label: String) { OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true) }
@Composable
private fun Metric(title: String, value: String, modifier: Modifier = Modifier) { Card(modifier) { Column(Modifier.padding(14.dp)) { Text(title, style = MaterialTheme.typography.bodySmall); Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) } } }
@Composable
private fun Heading(value: String) { Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 6.dp)) }
private fun money(v: Long) = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(v) + " ₫"
private fun isToday(t: Long): Boolean { val a = Calendar.getInstance().apply { timeInMillis = t }; val b = Calendar.getInstance(); return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR) }
private fun sameMonth(t: Long, now: Calendar): Boolean { val a = Calendar.getInstance().apply { timeInMillis = t }; return a.get(Calendar.YEAR) == now.get(Calendar.YEAR) && a.get(Calendar.MONTH) == now.get(Calendar.MONTH) }
private fun date(t: Long) = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(t))
private fun dateTime(t: Long) = SimpleDateFormat("dd/MM HH:mm", Locale("vi", "VN")).format(Date(t))
private fun roleName(role: String) = when (role) { "ADMIN" -> "Quản trị"; "CASHIER" -> "Thu ngân"; else -> "Kỹ thuật" }
