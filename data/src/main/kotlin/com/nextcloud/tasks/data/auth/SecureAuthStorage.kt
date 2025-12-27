package com.nextcloud.tasks.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("TooGenericExceptionCaught", "SwallowedException")
@Singleton
class SecureAuthStorage
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        moshi: Moshi,
    ) {
        private val prefs =
            try {
                EncryptedSharedPreferences.create(
                    context,
                    "auth_store",
                    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            } catch (e: Exception) {
                // If encrypted preferences are corrupted, delete them and recreate
                context
                    .getSharedPreferences("auth_store", Context.MODE_PRIVATE)
                    .edit()
                    .clear()
                    .commit()
                context.deleteSharedPreferences("auth_store")

                EncryptedSharedPreferences.create(
                    context,
                    "auth_store",
                    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
                )
            }

        private val adapter =
            moshi.adapter<List<StoredAccount>>(
                Types.newParameterizedType(List::class.java, StoredAccount::class.java),
            )

        private val accountsState = MutableStateFlow(loadAccounts())
        private val activeAccountIdState =
            MutableStateFlow(
                try {
                    prefs.getString(KEY_ACTIVE_ACCOUNT, null)
                } catch (e: Exception) {
                    null
                },
            )

        fun observeAccounts(): Flow<List<StoredAccount>> = accountsState.asStateFlow()

        fun observeActiveAccountId(): Flow<String?> = activeAccountIdState.asStateFlow()

        fun activeAccount(): StoredAccount? =
            activeAccountIdState.value?.let { id ->
                accountsState.value.firstOrNull { it.id == id }
            }

        fun setActiveAccount(id: String?) {
            try {
                prefs.edit().putString(KEY_ACTIVE_ACCOUNT, id).apply()
                activeAccountIdState.value = id
            } catch (e: Exception) {
                // If encryption fails, at least update the state
                activeAccountIdState.value = id
            }
        }

        fun saveAccount(account: StoredAccount) {
            val updated = accountsState.value.filterNot { it.id == account.id } + account
            persistAccounts(updated)
        }

        fun removeAccount(accountId: String) {
            val updated = accountsState.value.filterNot { it.id == accountId }
            if (updated.size != accountsState.value.size) {
                persistAccounts(updated)
                if (activeAccountIdState.value == accountId) {
                    setActiveAccount(updated.firstOrNull()?.id)
                }
            }
        }

        fun findAccount(accountId: String): StoredAccount? = accountsState.value.firstOrNull { it.id == accountId }

        private fun persistAccounts(accounts: List<StoredAccount>) {
            try {
                val serialized = adapter.toJson(accounts)
                prefs.edit().putString(KEY_ACCOUNTS, serialized).apply()
                accountsState.value = accounts
            } catch (e: Exception) {
                // If encryption fails, at least update the state
                // (data won't be persisted but app won't crash)
                accountsState.value = accounts
            }
        }

        private fun loadAccounts(): List<StoredAccount> {
            return try {
                val serialized = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
                adapter.fromJson(serialized) ?: emptyList()
            } catch (e: Exception) {
                // If decryption fails, clear corrupted data and return empty list
                try {
                    prefs.edit().clear().apply()
                } catch (ignored: Exception) {
                    // Ignore errors during cleanup
                }
                emptyList()
            }
        }

        companion object {
            private const val KEY_ACCOUNTS = "accounts"
            private const val KEY_ACTIVE_ACCOUNT = "active_account"
        }
    }

@JsonClass(generateAdapter = true)
data class StoredAccount(
    val id: String,
    val serverUrl: String,
    val username: String,
    val displayName: String,
    val authType: String,
    val accessToken: String? = null,
    val refreshToken: String? = null,
    val appPassword: String? = null,
)
