package vn.qcongnghe.qlbanhang.data

import kotlinx.coroutines.flow.Flow

class PosRepository(private val database: PosDatabase) {
    private val dao = database.dao()
    val products: Flow<List<Product>> = dao.products()
    val lowStock: Flow<List<Product>> = dao.lowStock()
    val customers: Flow<List<Customer>> = dao.customers()
    val sales: Flow<List<Sale>> = dao.sales()
    val tickets: Flow<List<ServiceTicket>> = dao.tickets()
    val cashEntries: Flow<List<CashEntry>> = dao.cashEntries()

    suspend fun seedIfEmpty() {
        if (dao.productCount() == 0) {
            listOf(
                Product(sku = "RAM-16", name = "RAM DDR4 16GB", category = "Linh kiện PC", brand = "Kingston", price = 850_000, cost = 680_000, stock = 12),
                Product(sku = "SSD-500", name = "SSD NVMe 500GB", category = "Linh kiện PC", brand = "WD", price = 1_150_000, cost = 900_000, stock = 8),
                Product(sku = "MOUSE-01", name = "Chuột không dây", category = "Phụ kiện", brand = "Logitech", price = 350_000, cost = 250_000, stock = 15),
                Product(sku = "PHONE-01", name = "Cáp sạc Type-C", category = "Phụ kiện điện thoại", brand = "Anker", price = 180_000, cost = 110_000, stock = 20),
                Product(sku = "MON-24", name = "Màn hình 24 inch", category = "Màn hình", brand = "ViewSonic", price = 2_650_000, cost = 2_200_000, stock = 4),
                Product(sku = "PRN-01", name = "Máy in laser", category = "Máy in", brand = "Canon", price = 3_200_000, cost = 2_750_000, stock = 2)
            ).forEach { dao.saveProduct(it) }
        }
    }

    suspend fun saveProduct(product: Product) = dao.saveProduct(product)
    suspend fun addStock(id: Long, quantity: Int) = dao.addStock(id, quantity)
    suspend fun saveCustomer(customer: Customer) = dao.saveCustomer(customer)
    suspend fun checkout(sale: Sale, lines: List<SaleLine>) = dao.checkout(sale, lines)
    suspend fun addTicket(ticket: ServiceTicket) = dao.insertTicket(ticket)
    suspend fun updateTicketStatus(id: Long, status: String) = dao.updateTicketStatus(id, status)
    suspend fun addCashEntry(entry: CashEntry) = dao.insertCashEntry(entry)
}
