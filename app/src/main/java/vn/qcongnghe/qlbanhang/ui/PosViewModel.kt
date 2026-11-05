package vn.qcongnghe.qlbanhang.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.qcongnghe.qlbanhang.data.AppUser
import vn.qcongnghe.qlbanhang.data.AuthRepository
import vn.qcongnghe.qlbanhang.data.CashEntry
import vn.qcongnghe.qlbanhang.data.Customer
import vn.qcongnghe.qlbanhang.data.PosRepository
import vn.qcongnghe.qlbanhang.data.Product
import vn.qcongnghe.qlbanhang.data.Sale
import vn.qcongnghe.qlbanhang.data.SaleLine
import vn.qcongnghe.qlbanhang.data.ServiceTicket
import vn.qcongnghe.qlbanhang.data.Supplier

class PosViewModel(private val repository: PosRepository, private val auth: AuthRepository) : ViewModel() {
    val products = repository.products
    val lowStock = repository.lowStock
    val customers = repository.customers
    val suppliers = repository.suppliers
    val users = repository.users
    val sales = repository.sales
    val tickets = repository.tickets
    val cashEntries = repository.cashEntries

    private val _user = MutableStateFlow<AppUser?>(null)
    val user = _user.asStateFlow()
    private val _setupRequired = MutableStateFlow<Boolean?>(null)
    val setupRequired = _setupRequired.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init {
        viewModelScope.launch {
            repository.seedIfEmpty()
            _setupRequired.value = !auth.hasUsers()
        }
    }

    fun login(username: String, password: String) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { auth.login(username, password) } }
            .onSuccess { result -> if (result != null) _user.value = result else _message.value = "Tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản đã khóa." }
            .onFailure { _message.value = it.message ?: "Không thể đăng nhập." }
    }
    fun createFirstAdmin(username: String, displayName: String, password: String) = viewModelScope.launch {
        runCatching { withContext(Dispatchers.IO) { auth.createFirstAdmin(username, displayName, password) } }
            .onSuccess { _user.value = it; _setupRequired.value = false; _message.value = "Đã tạo tài khoản quản trị." }
            .onFailure { _message.value = it.message ?: "Không thể tạo tài khoản." }
    }
    fun logout() { _user.value = null }
    fun createEmployee(username: String, name: String, password: String, role: String) = viewModelScope.launch {
        if (_user.value?.role != "ADMIN") { _message.value = "Chỉ quản trị viên mới được quản lý nhân viên."; return@launch }
        runCatching { withContext(Dispatchers.IO) { auth.createEmployee(username, name, password, role) } }
            .onSuccess { _message.value = "Đã tạo nhân viên." }
            .onFailure { _message.value = it.message ?: "Không thể tạo nhân viên (có thể trùng tên đăng nhập)." }
    }
    fun updateUser(user: AppUser, active: Boolean = user.active, role: String = user.role) = viewModelScope.launch {
        if (_user.value?.role != "ADMIN") return@launch
        if (user.id == _user.value?.id && (!active || role != "ADMIN")) { _message.value = "Không thể khóa hoặc hạ quyền tài khoản đang đăng nhập."; return@launch }
        repository.updateUser(user.id, user.displayName, role, active)
    }

    private fun canManageInventory() = _user.value?.role == "ADMIN"
    private fun canManageCustomers() = _user.value?.role in setOf("ADMIN", "CASHIER")
    private fun canManageService() = _user.value?.role in setOf("ADMIN", "TECHNICIAN")
    private fun canSell() = _user.value?.role in setOf("ADMIN", "CASHIER")
    private fun canManageCash() = _user.value?.role == "ADMIN"

    fun saveProduct(product: Product) = viewModelScope.launch {
        if (!canManageInventory()) return@launch
        runCatching { repository.saveProduct(product) }.onFailure { _message.value = "Không lưu được sản phẩm. SKU phải duy nhất." }
    }
    fun deleteProduct(id: Long) = viewModelScope.launch { if (canManageInventory()) repository.deleteProduct(id) }
    fun addStock(id: Long, quantity: Int) = viewModelScope.launch {
        if (!canManageInventory()) return@launch
        if (quantity <= 0) { _message.value = "Số lượng nhập phải lớn hơn 0."; return@launch }
        repository.addStock(id, quantity)
    }
    fun saveCustomer(customer: Customer) = viewModelScope.launch { if (canManageCustomers()) repository.saveCustomer(customer) }
    fun deleteCustomer(id: Long) = viewModelScope.launch { if (canManageCustomers()) repository.deleteCustomer(id) }
    fun saveSupplier(supplier: Supplier) = viewModelScope.launch { if (canManageInventory()) repository.saveSupplier(supplier) }
    fun deleteSupplier(id: Long) = viewModelScope.launch { if (canManageInventory()) repository.deleteSupplier(id) }
    fun addTicket(ticket: ServiceTicket) = viewModelScope.launch { if (canManageService()) repository.addTicket(ticket) }
    fun advanceTicket(id: Long, current: String) = viewModelScope.launch {
        if (!canManageService()) return@launch
        val next = when (current) { "Mới tiếp nhận" -> "Đang sửa chữa"; "Đang sửa chữa" -> "Chờ linh kiện"; "Chờ linh kiện" -> "Hoàn tất"; else -> "Mới tiếp nhận" }
        repository.updateTicketStatus(id, next)
    }
    fun addCashEntry(entry: CashEntry) = viewModelScope.launch { if (canManageCash()) repository.addCashEntry(entry) }
    fun checkout(lines: List<SaleLine>, customer: String, payment: String, total: Long) = viewModelScope.launch {
        if (!canSell()) { _message.value = "Tài khoản không có quyền bán hàng."; return@launch }
        if (lines.isEmpty()) { _message.value = "Giỏ hàng đang trống."; return@launch }
        runCatching { repository.checkout(Sale(total = total, customerName = customer.ifBlank { "Khách lẻ" }, paymentMethod = payment), lines) }
            .onSuccess { _message.value = "Thanh toán thành công! Đã tạo hóa đơn." }
            .onFailure { _message.value = it.message ?: "Thanh toán thất bại." }
    }
    fun clearMessage() { _message.value = null }
}

class PosViewModelFactory(private val repository: PosRepository, private val auth: AuthRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PosViewModel::class.java))
        return PosViewModel(repository, auth) as T
    }
}
