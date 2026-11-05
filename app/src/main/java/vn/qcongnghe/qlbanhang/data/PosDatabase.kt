package vn.qcongnghe.qlbanhang.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products", indices = [Index(value = ["sku"], unique = true)])
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

@Entity(tableName = "customers", indices = [Index(value = ["phone"], unique = false)])
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val note: String = ""
)

@Entity(tableName = "suppliers")
data class Supplier(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val note: String = ""
)

@Entity(tableName = "app_users", indices = [Index(value = ["username"], unique = true)])
data class AppUser(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val displayName: String,
    val passwordHash: String,
    val passwordSalt: String,
    val role: String,
    val active: Boolean = true
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

@Entity(tableName = "sale_lines", indices = [Index("saleId")])
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
    @Query("DELETE FROM products WHERE id = :id") suspend fun deleteProduct(id: Long)
    @Query("UPDATE products SET stock = stock + :quantity WHERE id = :id AND :quantity > 0") suspend fun addStock(id: Long, quantity: Int): Int
    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :id AND stock >= :quantity AND :quantity > 0") suspend fun reduceStock(id: Long, quantity: Int): Int

    @Query("SELECT * FROM customers ORDER BY name") fun customers(): Flow<List<Customer>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveCustomer(customer: Customer): Long
    @Query("DELETE FROM customers WHERE id = :id") suspend fun deleteCustomer(id: Long)

    @Query("SELECT * FROM suppliers ORDER BY name") fun suppliers(): Flow<List<Supplier>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun saveSupplier(supplier: Supplier): Long
    @Query("DELETE FROM suppliers WHERE id = :id") suspend fun deleteSupplier(id: Long)

    @Query("SELECT * FROM app_users ORDER BY displayName") fun users(): Flow<List<AppUser>>
    @Query("SELECT COUNT(*) FROM app_users") suspend fun userCount(): Int
    @Query("SELECT * FROM app_users WHERE username = :username COLLATE NOCASE LIMIT 1") suspend fun userByUsername(username: String): AppUser?
    @Insert(onConflict = OnConflictStrategy.ABORT) suspend fun insertUser(user: AppUser): Long
    @Query("UPDATE app_users SET displayName = :displayName, role = :role, active = :active WHERE id = :id") suspend fun updateUser(id: Long, displayName: String, role: String, active: Boolean)
    @Query("UPDATE app_users SET passwordHash = :hash, passwordSalt = :salt WHERE id = :id") suspend fun updatePassword(id: Long, hash: String, salt: String)

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
        require(lines.isNotEmpty()) { "Giỏ hàng đang trống." }
        require(lines.all { it.quantity > 0 && it.unitPrice >= 0 }) { "Số lượng hoặc giá bán không hợp lệ." }
        lines.groupBy { it.productId }.forEach { (productId, grouped) ->
            val quantity = grouped.sumOf { it.quantity }
            val changed = reduceStock(productId, quantity)
            check(changed == 1) { "Sản phẩm ${grouped.first().productName} không đủ tồn kho." }
        }
        val saleId = insertSale(sale)
        insertSaleLines(lines.map { it.copy(saleId = saleId) })
    }
}

@Database(
    entities = [Product::class, Customer::class, Supplier::class, AppUser::class, Sale::class, SaleLine::class, ServiceTicket::class, CashEntry::class],
    version = 2,
    exportSchema = false
)
abstract class PosDatabase : RoomDatabase() {
    abstract fun dao(): PosDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS suppliers (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, phone TEXT NOT NULL, email TEXT NOT NULL, address TEXT NOT NULL, note TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS app_users (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, username TEXT NOT NULL, displayName TEXT NOT NULL, passwordHash TEXT NOT NULL, passwordSalt TEXT NOT NULL, role TEXT NOT NULL, active INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_app_users_username ON app_users(username)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_customers_phone ON customers(phone)")
            }
        }
        @Volatile private var INSTANCE: PosDatabase? = null
        fun getInstance(context: Context): PosDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(context, PosDatabase::class.java, "qlbh-pos.db")
                .addMigrations(MIGRATION_1_2).build().also { INSTANCE = it }
        }
    }
}
