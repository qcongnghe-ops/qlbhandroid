package vn.qcongnghe.qlbanhang.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products", indices = [androidx.room.Index(value = ["sku"], unique = true)])
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sku: String,
    val name: String,
    val category: String,
    val brand: String = "",
    val price: Long,
    val cost: Long = 0,
    val stock: Int = 0,
    val minStock: Int = 2
)

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val note: String = ""
)

@Entity(tableName = "sales")
data class Sale(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val total: Long,
    val discount: Long = 0,
    val paymentMethod: String = "Tiền mặt",
    val customerName: String = "Khách lẻ"
)

@Entity(tableName = "sale_lines", indices = [androidx.room.Index("saleId")])
data class SaleLine(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long
)

@Entity(tableName = "service_tickets")
data class ServiceTicket(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val customerName: String,
    val phone: String,
    val device: String,
    val issue: String,
    val status: String = "Mới tiếp nhận",
    val fee: Long = 0,
    val appointmentAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "cash_entries")
data class CashEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Long,
    val type: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface PosDao {
    @Query("SELECT * FROM products ORDER BY name") fun products(): Flow<List<Product>>
    @Query("SELECT * FROM products WHERE stock <= minStock ORDER BY stock") fun lowStock(): Flow<List<Product>>
    @Query("SELECT * FROM products WHERE id = :id LIMIT 1") suspend fun product(id: Long): Product?
    @Query("SELECT COUNT(*) FROM products") suspend fun productCount(): Int
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveProduct(product: Product): Long
    @Query("UPDATE products SET stock = stock + :quantity WHERE id = :id") suspend fun addStock(id: Long, quantity: Int)
    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :id AND stock >= :quantity") suspend fun reduceStock(id: Long, quantity: Int): Int

    @Query("SELECT * FROM customers ORDER BY name") fun customers(): Flow<List<Customer>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCustomer(customer: Customer): Long

    @Query("SELECT * FROM sales ORDER BY createdAt DESC") fun sales(): Flow<List<Sale>>
    @Insert suspend fun insertSale(sale: Sale): Long
    @Insert suspend fun insertSaleLines(lines: List<SaleLine>)

    @Query("SELECT * FROM service_tickets ORDER BY createdAt DESC") fun tickets(): Flow<List<ServiceTicket>>
    @Insert suspend fun insertTicket(ticket: ServiceTicket): Long
    @Query("UPDATE service_tickets SET status = :status WHERE id = :id") suspend fun updateTicketStatus(id: Long, status: String)

    @Query("SELECT * FROM cash_entries ORDER BY createdAt DESC") fun cashEntries(): Flow<List<CashEntry>>
    @Insert suspend fun insertCashEntry(entry: CashEntry): Long

    @Transaction
    suspend fun checkout(sale: Sale, lines: List<SaleLine>) {
        lines.forEach { line ->
            val changed = reduceStock(line.productId, line.quantity)
            check(changed == 1) { "Sản phẩm ${line.productName} không đủ tồn kho." }
        }
        val saleId = insertSale(sale)
        insertSaleLines(lines.map { it.copy(saleId = saleId) })
    }
}

@Database(
    entities = [Product::class, Customer::class, Sale::class, SaleLine::class, ServiceTicket::class, CashEntry::class],
    version = 1,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun dao(): PosDao

    companion object {
        @Volatile private var INSTANCE: PosDatabase? = null
        fun getInstance(context: Context): PosDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context, PosDatabase::class.java, "qlbh-pos.db")
                .build().also { INSTANCE = it }
        }
    }
}
