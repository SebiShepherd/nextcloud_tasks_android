package com.nextcloud.tasks.data.network

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Trust-on-first-use store for certificates the user explicitly accepted.
 *
 * Accepted certificates are pinned by their SHA-256 fingerprint in an EncryptedSharedPreferences
 * (tamper-resistant, so a "trust" decision can't be forged by editing the file on disk).
 * Rejections are kept in memory for the session only, so the user is re-prompted on a fresh start.
 *
 * The encrypted store can become undecryptable when the Android keystore master key is invalidated
 * (app reinstall, data restore from backup, OS upgrade, lock-screen change). Every read then throws
 * [javax.crypto.AEADBadTagException] / KeyStoreException, which previously propagated out of the
 * TLS trust check and made login to a self-signed server fail *without* showing the trust prompt.
 * This class recovers per Android best practice — delete the master key and the file, recreate —
 * and, if even that fails, degrades to session-only in-memory trust so a TLS check never crashes.
 */
@Singleton
class AppCertStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        // Persistent encrypted store, or null when it could not be opened even after recovery.
        // `by lazy` is synchronized, giving thread-safe one-time creation (EncryptedSharedPreferences
        // creation is not thread-safe).
        private val prefs: SharedPreferences? by lazy {
            openResilient(create = ::createEncryptedPrefs, wipe = ::wipeStore)
        }

        // In-memory fallback used when the persistent store is unavailable (session-only trust).
        private val sessionTrusted = Collections.synchronizedSet(mutableSetOf<String>())
        private val rejected = Collections.synchronizedSet(mutableSetOf<String>())

        fun isTrusted(cert: X509Certificate): Boolean {
            val fingerprint = sha256Hex(cert)
            if (fingerprint in rejected) return false
            val store = prefs ?: return fingerprint in sessionTrusted
            return try {
                store.getBoolean(fingerprint, false)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                // A read must never break the TLS handshake; fall back to session trust.
                Timber.w(e, "Trusted-cert store read failed; using session-only trust")
                fingerprint in sessionTrusted
            }
        }

        fun trust(cert: X509Certificate) {
            val fingerprint = sha256Hex(cert)
            rejected.remove(fingerprint)
            sessionTrusted.add(fingerprint)
            val store = prefs ?: return
            try {
                store.edit().putBoolean(fingerprint, true).apply()
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.w(e, "Trusted-cert store write failed; kept in session only")
            }
        }

        /** Session-only rejection; the user is re-prompted after an app restart. */
        fun reject(cert: X509Certificate) {
            rejected.add(sha256Hex(cert))
        }

        fun clear() {
            sessionTrusted.clear()
            rejected.clear()
            val store = prefs ?: return
            try {
                store.edit().clear().apply()
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.w(e, "Trusted-cert store clear failed")
            }
        }

        private fun createEncryptedPrefs(): SharedPreferences {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            return EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        /** Deletes the master key and the encrypted file so the next create() starts clean. */
        private fun wipeStore() {
            try {
                KeyStore
                    .getInstance(ANDROID_KEYSTORE)
                    .apply { load(null) }
                    .deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            } catch (
                @Suppress("TooGenericExceptionCaught") e: Exception,
            ) {
                Timber.w(e, "Could not delete cert-store master key")
            }
            context.deleteSharedPreferences(PREFS_NAME)
        }

        companion object {
            private const val PREFS_NAME = "trusted_certificates"
            private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        }
    }

/**
 * Opens the encrypted store, recovering once from an undecryptable keystore by [wipe]-ing the
 * master key and file and recreating. Returns null if even that fails, so the caller can fall back
 * to in-memory trust instead of letting the exception crash every TLS check. Extracted (no Context)
 * so the recovery flow is unit-testable without the Android keystore.
 */
internal fun openResilient(
    create: () -> SharedPreferences,
    wipe: () -> Unit,
): SharedPreferences? {
    repeat(2) { attempt ->
        try {
            if (attempt > 0) wipe()
            return create()
        } catch (e: GeneralSecurityException) {
            Timber.w(e, "Trusted-cert store open failed (attempt ${attempt + 1}); recovering")
        } catch (e: IOException) {
            Timber.w(e, "Trusted-cert store open failed (attempt ${attempt + 1}); recovering")
        }
    }
    Timber.e("Trusted-cert store unrecoverable; using session-only trust")
    return null
}
