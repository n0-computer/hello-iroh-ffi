package computer.iroh.pong.identity

import android.content.Context
import android.util.Base64
import computer.iroh.SecretKey

/**
 * Persistent identity for this device. Mirrors the Swift IdentityStore:
 * generate a SecretKey on first launch, persist the 32 raw bytes as
 * base64 in SharedPreferences, reload it on subsequent launches so the
 * endpoint id is stable across sessions.
 *
 * The API secret for iroh-services is persisted alongside in the same
 * preferences file (default empty string = "use bundled default").
 */
class IdentityStore private constructor(
    val secretKey: SecretKey,
    val endpointId: String,
    private val prefs: android.content.SharedPreferences,
) {
    var apiSecret: String
        get() = prefs.getString(KEY_API_SECRET, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_API_SECRET, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "iroh.pong.identity"
        private const val KEY_SECRET = "secretKey"
        private const val KEY_API_SECRET = "apiSecret"

        fun loadOrCreate(context: Context): IdentityStore {
            val prefs = context.applicationContext
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val secret = loadOrGenerate(prefs)
            return IdentityStore(
                secretKey = secret,
                endpointId = secret.public().toString(),
                prefs = prefs,
            )
        }

        private fun loadOrGenerate(prefs: android.content.SharedPreferences): SecretKey {
            val existing = prefs.getString(KEY_SECRET, null)
            if (existing != null) {
                try {
                    val bytes = Base64.decode(existing, Base64.NO_WRAP)
                    return SecretKey.fromBytes(bytes)
                } catch (_: Throwable) {
                    // fall through to generate a fresh key
                }
            }
            val generated = SecretKey.generate()
            val encoded = Base64.encodeToString(generated.toBytes(), Base64.NO_WRAP)
            prefs.edit().putString(KEY_SECRET, encoded).apply()
            return generated
        }
    }
}
