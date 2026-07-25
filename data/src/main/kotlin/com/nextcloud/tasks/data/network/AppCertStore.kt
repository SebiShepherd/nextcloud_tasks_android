package com.nextcloud.tasks.data.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
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
 */
@Singleton
class AppCertStore
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val prefs by lazy {
            val masterKey =
                MasterKey
                    .Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        }

        private val rejected = Collections.synchronizedSet(mutableSetOf<String>())

        fun isTrusted(cert: X509Certificate): Boolean {
            val fingerprint = sha256Hex(cert)
            if (fingerprint in rejected) return false
            return prefs.getBoolean(fingerprint, false)
        }

        fun trust(cert: X509Certificate) {
            val fingerprint = sha256Hex(cert)
            rejected.remove(fingerprint)
            prefs.edit().putBoolean(fingerprint, true).apply()
        }

        /** Session-only rejection; the user is re-prompted after an app restart. */
        fun reject(cert: X509Certificate) {
            rejected.add(sha256Hex(cert))
        }

        fun clear() {
            prefs.edit().clear().apply()
            rejected.clear()
        }

        companion object {
            private const val PREFS_NAME = "trusted_certificates"
        }
    }
