package vn.qcongnghe.qlbanhang.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import vn.qcongnghe.qlbanhang.data.CashEntry
import vn.qcongnghe.qlbanhang.data.Customer
import vn.qcongnghe.qlbanhang.data.Product
import vn.qcongnghe.qlbanhang.data.Sale
import vn.qcongnghe.qlbanhang.data.SaleLine
import vn.qcongnghe.qlbanhang.data.ServiceTicket
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private enum class Page(val title: String, val icon: String) {
    HOME("Tổng quan", "⌂"), POS("Bán hàng", "＋"), STOCK("Kho hàng", "▤"), SERVICES("Dịch vụ", "⚙"), CUSTOMERS("Khách hàng", "♙"), REPORTS("Báo cáo", "▥")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosApp(viewModel: PosViewModel) {
    val products by viewModel.products.collectAsStateWithLifecycle(emptyList())
    val lowStock by viewModel.lowStock.collectAsStateWithLifecycle(emptyList())
    val customers by viewModel.customers.collectAsStateWithLifecycle(emptyList())
    val sales by viewModel.sales.collectAsStateWithLifecycle(emptyList())
    val tickets by viewModel.tickets.collectAsStateWithLifecycle(emptyList())
    val cashEntries by viewModel.cashEntries.collectAsStateWithLifecycle(emptyList())
    val message by viewModel.message.collectAsStateWithLifecycle()
    var page by remember { mutableStateOf(Page.HOME) }
    val cart = remember { mutableStateListOf<CartRow>() }
    var customerName by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        if (message != null) {
            kotlinx.coroutines.delay(2200)
            viewModel.clearMessage()
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = { TopAppBar(title = { Text("QLBH Điện tử", fontWeight = FontWeight.Bold) }) },
            bottomBar = {
                NavigationBar {
                    Page.entries.forEach { item ->
                        NavigationBarItem(selected = page == item, onClick = { page = item }, icon = { Text(item.icon) }, label = { Text(item.title) })
                    }
                }
            },
            floatingActionButton = {
                when (page) {
                    Page.STOCK -> AddProductButton { form -> viewModel.saveProduct(form) }
                    Page.SERVICES -> AddTicketButton { viewModel.addTicket(it) }
                    Page.CUSTOMERS -> AddCustomerButton { viewModel.saveCustomer(it) }
                    Page.REPORTS -> AddCashButton { viewModel.addCashEntry(it) }
                    else -> Unit
                }
            },
            snackbarHost = {}
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                when (page) {
                    Page.HOME -> HomeScreen(sales, products, lowStock, tickets)
                    Page.POS -> PosScreen(products, cart, customerName, { customerName = it }) {
                        val total = cart.sumOf { it.product.price * it.quantity }
                        viewModel.checkout(cart.map { SaleLine(saleId = 0, productId = it.product.id, productName = it.product.name, quantity = it.quantity, unitPrice = it.product.price) }, customerName, "Tiền mặt", total)
                        cart.clear(); customerName = ""
                    }
                    Page.STOCK -> StockScreen(products, viewModel)
                    Page.SERVICES -> ServiceScreen(tickets) { viewModel.advanceTicket(it.id, it.status) }
                    Page.CUSTOMERS -> CustomerScreen(customers)
                    Page.REPORTS -> ReportScreen(sales, cashEntries, lowStock)
                }
                if (message != null) Surface(color = MaterialTheme.colorScheme.inverseSurface, modifier = Modifier.fillMaxWidth()) {
                    Text(message.orEmpty(), color = MaterialTheme.colorScheme.inverseOnSurface, modifier = Modifier.padding(12.dp))
                }
            }
        }
    }
}

private data class CartRow(val product: Product, val quantity: Int)

@Composable
private fun HomeScreen(sales: List<Sale>, products: List<Product>, lowStock: List<Product>, tickets: List<ServiceTicket>) {
    val today = sales.filter { isToday(it.createdAt) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Xin chào! Đây là tình hình cửa hàng.", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Doanh thu hôm nay", money(today.sumOf { it.total }), Modifier.weight(1f))
            MetricCard("Hóa đơn hôm nay", "${today.size}", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard("Sản phẩm", "${products.size}", Modifier.weight(1f))
            MetricCard("Phiếu dịch vụ", "${tickets.count { it.status != "Hoàn tất" }}", Modifier.weight(1f))
        }
        SectionTitle("Sắp hết hàng (${lowStock.size})")
        if (lowStock.isEmpty()) Text("Chưa có sản phẩm nào dưới định mức tồn.")
        lowStock.take(5).forEach { Text("• ${it.name} — còn ${it.stock}") }
        SectionTitle("Dịch vụ đang xử lý")
        val active = tickets.filter { it.status != "Hoàn tất" }.take(4)
        if (active.isEmpty()) Text("Chưa có phiếu sửa chữa đang mở.")
        active.forEach { Text("• ${it.device}: ${it.status} — ${it.customerName}") }
    }
}

@Composable
private fun PosScreen(products: List<Product>, cart: MutableList<CartRow>, customerName: String, onCustomerChange: (String) -> Unit, onCheckout: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(customerName, onCustomerChange, label = { Text("Tên khách hàng (không bắt buộc)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        SectionTitle("Sản phẩm · ${products.size}")
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(products, key = { it.id }) { product ->
                Card(onClick = {
                    val index = cart.indexOfFirst { it.product.id == product.id }
                    if (index >= 0) cart[index] = cart[index].copy(quantity = cart[index].quantity + 1)
                    else cart.add(CartRow(product, 1))
                }) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text("${product.sku} · Tồn ${product.stock}", style = MaterialTheme.typography.bodySmall) }
                        Text(money(product.price), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Giỏ hàng (${cart.sumOf { it.quantity }} sản phẩm)", fontWeight = FontWeight.Bold)
                cart.take(3).forEach { Text("${it.product.name} × ${it.quantity} · ${money(it.product.price * it.quantity)}") }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tổng cộng", fontWeight = FontWeight.Bold)
                    Text(money(cart.sumOf { it.product.price * it.quantity }), fontWeight = FontWeight.Bold)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { cart.clear() }, modifier = Modifier.weight(1f)) { Text("Xóa giỏ") }
                    Button(onClick = onCheckout, enabled = cart.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("Thanh toán") }
                }
            }
        }
    }
}

@Composable
private fun StockScreen(products: List<Product>, viewModel: PosViewModel) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Danh mục hàng hóa", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Chạm + để thêm sản phẩm. SKU cần duy nhất.") }
        items(products, key = { it.id }) { product ->
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) { Text(product.name, fontWeight = FontWeight.SemiBold); Text("${product.category} · ${product.brand} · ${product.sku}", style = MaterialTheme.typography.bodySmall) }
                        Text(money(product.price), fontWeight = FontWeight.Bold)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Tồn: ${product.stock} · Định mức: ${product.minStock}")
                        TextButton(onClick = { viewModel.addStock(product.id, 1) }) { Text("+ Nhập 1") }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceScreen(tickets: List<ServiceTicket>, onAdvance: (ServiceTicket) -> Unit) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Sửa chữa · bảo hành · lịch hẹn", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (tickets.isEmpty()) item { Text("Chưa có phiếu dịch vụ. Nhấn + để tiếp nhận thiết bị.") }
        items(tickets, key = { it.id }) { ticket ->
            Card {
                Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${ticket.device} · ${ticket.status}", fontWeight = FontWeight.Bold)
                    Text("${ticket.customerName} · ${ticket.phone}")
                    Text("Lỗi: ${ticket.issue}")
                    Text("Phí dự kiến: ${money(ticket.fee)}${ticket.appointmentAt?.let { " · Hẹn ${date(it)}" } ?: ""}")
                    TextButton(onClick = { onAdvance(ticket) }) { Text(if (ticket.status == "Hoàn tất") "Mở lại phiếu" else "Chuyển trạng thái") }
                }
            }
        }
    }
}

@Composable
private fun CustomerScreen(customers: List<Customer>) {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Text("Khách hàng", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        if (customers.isEmpty()) item { Text("Chưa có khách hàng. Nhấn + để thêm hồ sơ.") }
        items(customers, key = { it.id }) { c -> Card { Column(Modifier.padding(12.dp)) { Text(c.name, fontWeight = FontWeight.Bold); Text(c.phone); if (c.note.isNotBlank()) Text(c.note) } } }
    }
}

@Composable
private fun ReportScreen(sales: List<Sale>, entries: List<CashEntry>, lowStock: List<Product>) {
    val now = Calendar.getInstance()
    val month = sales.filter { isSameMonth(it.createdAt, now) }
    val today = sales.filter { isToday(it.createdAt) }
    val monthExpense = entries.filter { it.type == "Chi" && isSameMonth(it.createdAt, now) }.sumOf { it.amount }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Báo cáo kinh doanh", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        MetricCard("Doanh thu hôm nay", money(today.sumOf { it.total }))
        MetricCard("Doanh thu tháng này", money(month.sumOf { it.total }))
        MetricCard("Phiếu bán trong tháng", "${month.size}")
        MetricCard("Chi phí tháng này", money(monthExpense))
        MetricCard("Tồn kho cần chú ý", "${lowStock.size} sản phẩm")
        SectionTitle("Giao dịch gần đây")
        sales.take(10).forEach { Text("${dateTime(it.createdAt)} · ${it.customerName} · ${money(it.total)} · ${it.paymentMethod}") }
        SectionTitle("Thu / chi gần đây")
        entries.take(10).forEach { Text("${dateTime(it.createdAt)} · ${it.type}: ${it.title} · ${money(it.amount)}") }
    }
}

@Composable
private fun AddProductButton(onSave: (Product) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FloatingActionButton(onClick = { open = true }) { Text("+") }
    if (open) ProductDialog(onDismiss = { open = false }, onSave = { onSave(it); open = false })
}

@Composable
private fun ProductDialog(onDismiss: () -> Unit, onSave: (Product) -> Unit) {
    var sku by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Linh kiện PC") }; var brand by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }; var cost by remember { mutableStateOf("") }; var stock by remember { mutableStateOf("0") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Thêm sản phẩm") }, text = {
        Column(Modifier.height(430.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Field(sku, { sku = it }, "Mã SKU*"); Field(name, { name = it }, "Tên sản phẩm*")
            Field(category, { category = it }, "Danh mục"); Field(brand, { brand = it }, "Thương hiệu")
            Field(price, { price = it }, "Giá bán (đ)*", numeric = true); Field(cost, { cost = it }, "Giá vốn (đ)", numeric = true); Field(stock, { stock = it }, "Tồn kho", numeric = true)
        }
    }, confirmButton = { TextButton(onClick = { if (sku.isNotBlank() && name.isNotBlank() && (price.toLongOrNull() ?: 0) > 0) onSave(Product(sku = sku.trim(), name = name.trim(), category = category.trim(), brand = brand.trim(), price = price.toLong(), cost = cost.toLongOrNull() ?: 0, stock = stock.toIntOrNull() ?: 0)) }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}

@Composable
private fun AddTicketButton(onSave: (ServiceTicket) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FloatingActionButton(onClick = { open = true }) { Text("+") }
    if (open) TicketDialog({ open = false }) { onSave(it); open = false }
}

@Composable
private fun TicketDialog(onDismiss: () -> Unit, onSave: (ServiceTicket) -> Unit) {
    var customer by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }
    var device by remember { mutableStateOf("") }; var issue by remember { mutableStateOf("") }; var fee by remember { mutableStateOf("0") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tiếp nhận sửa chữa") }, text = {
        Column(Modifier.height(350.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Field(customer, { customer = it }, "Tên khách hàng*"); Field(phone, { phone = it }, "Số điện thoại")
            Field(device, { device = it }, "Thiết bị (máy tính/điện thoại...)*"); Field(issue, { issue = it }, "Mô tả lỗi*"); Field(fee, { fee = it }, "Phí dự kiến (đ)", numeric = true)
        }
    }, confirmButton = { TextButton(onClick = { if (customer.isNotBlank() && device.isNotBlank() && issue.isNotBlank()) onSave(ServiceTicket(customerName = customer, phone = phone, device = device, issue = issue, fee = fee.toLongOrNull() ?: 0)) }) { Text("Tạo phiếu") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } })
}

@Composable
private fun AddCustomerButton(onSave: (Customer) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FloatingActionButton(onClick = { open = true }) { Text("+") }
    if (open) {
        var name by remember { mutableStateOf("") }; var phone by remember { mutableStateOf("") }; var note by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { open = false }, title = { Text("Thêm khách hàng") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Field(name, { name = it }, "Họ tên*"); Field(phone, { phone = it }, "Điện thoại"); Field(note, { note = it }, "Ghi chú") } }, confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { onSave(Customer(name = name, phone = phone, note = note)); open = false } }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Hủy") } })
    }
}

@Composable
private fun AddCashButton(onSave: (CashEntry) -> Unit) {
    var open by remember { mutableStateOf(false) }
    FloatingActionButton(onClick = { open = true }) { Text("+") }
    if (open) {
        var title by remember { mutableStateOf("") }; var amount by remember { mutableStateOf("") }; var type by remember { mutableStateOf("Chi") }
        AlertDialog(onDismissRequest = { open = false }, title = { Text("Ghi nhận thu / chi") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Field(title, { title = it }, "Nội dung*"); Field(amount, { amount = it }, "Số tiền (đ)*", numeric = true); Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { AssistChip(onClick = { type = "Thu" }, label = { Text("Thu${if (type == "Thu") " ✓" else ""}") }); AssistChip(onClick = { type = "Chi" }, label = { Text("Chi${if (type == "Chi") " ✓" else ""}") }) } } }, confirmButton = { TextButton(onClick = { val value = amount.toLongOrNull() ?: 0; if (title.isNotBlank() && value > 0) { onSave(CashEntry(title = title, amount = value, type = type)); open = false } }) { Text("Lưu") } }, dismissButton = { TextButton(onClick = { open = false }) { Text("Hủy") } })
    }
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String, numeric: Boolean = false) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) { Column(Modifier.padding(14.dp)) { Text(title, style = MaterialTheme.typography.bodySmall); Spacer(Modifier.size(4.dp)); Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun SectionTitle(title: String) {
    Spacer(Modifier.height(8.dp)); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

private fun money(amount: Long): String = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(amount) + " ₫"
private fun isToday(timestamp: Long): Boolean {
    val item = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    return item.get(Calendar.YEAR) == today.get(Calendar.YEAR) && item.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
}
private fun isSameMonth(timestamp: Long, now: Calendar): Boolean {
    val item = Calendar.getInstance().apply { timeInMillis = timestamp }
    return item.get(Calendar.YEAR) == now.get(Calendar.YEAR) && item.get(Calendar.MONTH) == now.get(Calendar.MONTH)
}
private fun date(timestamp: Long) = SimpleDateFormat("dd/MM/yyyy", Locale("vi", "VN")).format(Date(timestamp))
private fun dateTime(timestamp: Long) = SimpleDateFormat("dd/MM HH:mm", Locale("vi", "VN")).format(Date(timestamp))
