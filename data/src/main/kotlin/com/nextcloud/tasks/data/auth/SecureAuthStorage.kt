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

@Singleton
class SecureAuthStorage
    @Inject
    constructor(
        @ApplicationContext context: Context,
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
                // If encrypted preferences fail to initialize (e.g., corrupted keystore),
                // delete the file and try again with a fresh instance
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
        private val activeAccountIdState = MutableStateFlow(prefs.getString(KEY_ACTIVE_ACCOUNT, null))

        fun observeAccounts(): Flow<List<StoredAccount>> = accountsState.asStateFlow()

        fun observeActiveAccountId(): Flow<String?> = activeAccountIdState.asStateFlow()

        fun activeAccount(): StoredAccount? =
            activeAccountIdState.value?.let { id ->
                accountsState.value.firstOrNull { it.id == id }
            }

        fun setActiveAccount(id: String?) {
            prefs.edit().putString(KEY_ACTIVE_ACCOUNT, id).commit()
            activeAccountIdState.value = id
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
            val serialized = adapter.toJson(accounts)
            prefs.edit().putString(KEY_ACCOUNTS, serialized).commit()
            accountsState.value = accounts
        }

        private fun loadAccounts(): List<StoredAccount> {
            val serialized = prefs.getString(KEY_ACCOUNTS, null) ?: return emptyList()
            return adapter.fromJson(serialized) ?: emptyList()
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
