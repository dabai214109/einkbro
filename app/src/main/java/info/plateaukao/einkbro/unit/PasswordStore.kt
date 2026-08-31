package info.plateaukao.einkbro.unit

import android.content.Context
import info.plateaukao.einkbro.database.PasswordEntry
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Device-local credential store: a JSON file in app-private storage whose
 * password column is Keystore-encrypted. Survives browser cache clears; only
 * uninstalling (or clearing app data) removes it.
 */
class PasswordStore(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }

    private val file: File get() = File(context.filesDir, "shark_passwords.json")

    @Volatile
    private var cache: List<PasswordEntry>? = null

    private fun load(): List<PasswordEntry> {
        cache?.let { return it }
        val list = runCatching {
            if (!file.exists()) emptyList()
            else json.decodeFromString(ListSerializer(PasswordEntry.serializer()), file.readText())
        }.getOrDefault(emptyList())
        cache = list
        return list
    }

    private fun save(list: List<PasswordEntry>) {
        cache = list
        runCatching {
            file.writeText(json.encodeToString(ListSerializer(PasswordEntry.serializer()), list))
        }
    }

    fun all(): List<PasswordEntry> = load().sortedBy { it.host }

    /** Most recently saved credential for the page origin, if any. */
    fun find(origin: String): PasswordEntry? {
        val host = hostOf(origin)
        return load().filter { it.host == host }.maxByOrNull { it.updatedAt }
    }

    fun contains(origin: String, username: String, plainPassword: String): Boolean =
        load().any {
            it.host == hostOf(origin) && it.username == username &&
                    CryptoUnit.decrypt(it.encryptedPassword) == plainPassword
        }

    fun put(origin: String, username: String, plainPassword: String) {
        val host = hostOf(origin)
        val list = load().toMutableList()
        list.removeAll { it.host == host && it.username == username }
        list.add(
            PasswordEntry(host, username, CryptoUnit.encrypt(plainPassword), System.currentTimeMillis())
        )
        save(list)
    }

    fun remove(host: String, username: String) {
        save(load().filterNot { it.host == host && it.username == username })
    }

    fun removeHost(host: String) {
        save(load().filterNot { it.host == host })
    }

    fun plainPassword(entry: PasswordEntry): String = CryptoUnit.decrypt(entry.encryptedPassword)

    private fun hostOf(origin: String): String =
        runCatching { java.net.URI(origin).host?.lowercase() ?: origin.lowercase() }
            .getOrDefault(origin.lowercase())
            .removePrefix("www.")
}
