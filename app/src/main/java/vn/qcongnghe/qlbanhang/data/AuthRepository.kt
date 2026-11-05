package vn.qcongnghe.qlbanhang.data

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** Local, per-device authentication. Not a cloud identity provider. */
class AuthRepository(private val repository: PosRepository) {
    suspend fun hasUsers(): Boolean = repository.userCount() > 0

    suspend fun createFirstAdmin(username: String, displayName: String, password: String): AppUser {
        require(!hasUsers()) { "Tài khoản quản trị đã được tạo." }
        require(username.trim().length >= 3) { "Tên đăng nhập cần ít nhất 3 ký tự." }
        require(password.length >= 10) { "Mật khẩu cần ít nhất 10 ký tự." }
        val (hash, salt) = hashPassword(password)
        val user = AppUser(username = username.trim(), displayName = displayName.trim().ifBlank { username.trim() }, passwordHash = hash, passwordSalt = salt, role = "ADMIN")
        val id = repository.insertUser(user)
        return user.copy(id = id)
    }

    suspend fun login(username: String, password: String): AppUser? {
        val user = repository.userByUsername(username.trim()) ?: return null
        if (!user.active || !verifyPassword(password, user.passwordSalt, user.passwordHash)) return null
        return user.copy(passwordHash = "", passwordSalt = "")
    }

    suspend fun createEmployee(username: String, displayName: String, password: String, role: String): Long {
        require(role in setOf("ADMIN", "CASHIER", "TECHNICIAN"))
        require(username.trim().length >= 3) { "Tên đăng nhập cần ít nhất 3 ký tự." }
        require(password.length >= 10) { "Mật khẩu cần ít nhất 10 ký tự." }
        val (hash, salt) = hashPassword(password)
        return repository.insertUser(AppUser(username = username.trim(), displayName = displayName.trim().ifBlank { username.trim() }, passwordHash = hash, passwordSalt = salt, role = role))
    }

    private fun hashPassword(password: String): Pair<String, String> {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return derive(password, salt) to Base64.encodeToString(salt, Base64.NO_WRAP)
    }

    private fun verifyPassword(password: String, saltText: String, expected: String): Boolean {
        val salt = Base64.decode(saltText, Base64.NO_WRAP)
        val actual = Base64.decode(derive(password, salt), Base64.NO_WRAP)
        val target = Base64.decode(expected, Base64.NO_WRAP)
        return java.security.MessageDigest.isEqual(actual, target)
    }

    private fun derive(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
        return try {
            Base64.encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded, Base64.NO_WRAP)
        } finally { spec.clearPassword() }
    }
}
