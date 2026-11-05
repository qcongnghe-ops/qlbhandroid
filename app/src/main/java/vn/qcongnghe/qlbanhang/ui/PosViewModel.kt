package vn.qcongnghe.qlbanhang.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import vn.qcongnghe.qlbanhang.data.CashEntry
import vn.qcongnghe.qlbanhang.data.Customer
import vn.qcongnghe.qlbanhang.data.PosRepository
import vn.qcongnghe.qlbanhang.data.Product
import vn.qcongnghe.qlbanhang.data.Sale
import vn.qcongnghe.qlbanhang.data.SaleLine
import vn.qcongnghe.qlbanhang.data.ServiceTicket

class PosViewModel(private val repository: PosRepository) : ViewModel() {
    val products = repository.products
    val lowStock = repository.lowStock
    val customers = repository.customers
    val sales = repository.sales
    val tickets = repository.tickets
    val cashEntries = repository.cashEntries

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    init { viewModelScope.launch { repository.seedIfEmpty() } }

    fun saveProduct(product: Product) = viewModelScope.launch {
        runCatching { repository.saveProduct(product) }
            .onFailure { _message.value = "Không lưu được sản phẩm. Mã SKU có thể đã tồn tại." }
    }
    fun addStock(id: Long, quantity: Int) = viewModelScope.launch { repository.addStock(id, quantity) }
    fun saveCustomer(customer: Customer) = viewModelScope.launch { repository.saveCustomer(customer) }
    fun addTicket(ticket: ServiceTicket) = viewModelScope.launch { repository.addTicket(ticket) }
    fun advanceTicket(id: Long, current: String) = viewModelScope.launch {
        val next = when (current) {
            "Mới tiếp nhận" -> "Đang sửa chữa"
            "Đang sửa chữa" -> "Chờ linh kiện"
            "Chờ linh kiện" -> "Hoàn tất"
            else -> "Mới tiếp nhận"
        }
        repository.updateTicketStatus(id, next)
    }
    fun addCashEntry(entry: CashEntry) = viewModelScope.launch { repository.addCashEntry(entry) }

    fun checkout(lines: List<SaleLine>, customer: String, payment: String, total: Long) = viewModelScope.launch {
        if (lines.isEmpty()) { _message.value = "Giỏ hàng đang trống."; return@launch }
        runCatching { repository.checkout(Sale(total = total, customerName = customer.ifBlank { "Khách lẻ" }, paymentMethod = payment), lines) }
            .onSuccess { _message.value = "Thanh toán thành công! Đã tạo hóa đơn." }
            .onFailure { _message.value = it.message ?: "Thanh toán thất bại." }
    }
    fun clearMessage() { _message.value = null }
}

class PosViewModelFactory(private val repository: PosRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(PosViewModel::class.java))
        return PosViewModel(repository) as T
    }
}
