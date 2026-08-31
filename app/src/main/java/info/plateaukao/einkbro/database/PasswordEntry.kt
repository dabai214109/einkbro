package info.plateaukao.einkbro.database

import kotlinx.serialization.Serializable

/**
 * One saved credential. Stored in PasswordStore's JSON file; the password
 * column is CryptoUnit ciphertext, never written in clear.
 */
@Serializable
data class PasswordEntry(
    val host: String,
    val username: String,
    val encryptedPassword: String,
    val updatedAt: Long,
)
