package com.nextcloud.tasks.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.nextcloud.tasks.domain.model.Account
import com.nextcloud.tasks.domain.model.AuthType
import com.nextcloud.tasks.domain.repository.AccountRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

@Singleton
class AccountRepositoryImpl @Inject constructor(@ApplicationContext context: Context) : AccountRepository {
    private val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    private val preferences: SharedPreferences =
        EncryptedSharedPreferences.create(
            context,
            PREF_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )

    private val accountsState = MutableStateFlow(loadAccounts())
    private val activeAccountIdState = MutableStateFlow(preferences.getString(KEY_ACTIVE_ACCOUNT_ID, null))

    override val accounts: Flow<List<Account>> = accountsState.asStateFlow()

    override val activeAccount: Flow<Account?> =
        combine(accountsState, activeAccountIdState) { accounts, activeId ->
            accounts.firstOrNull { it.id == activeId }
        }

    override suspend fun saveAccount(account: Account) {
        accountsState.update { accounts ->
            accounts.filterNot { it.id == account.id } + account
        }
        persistAccounts()
        setActiveAccount(account.id)
    }

    override suspend fun setActiveAccount(accountId: String) {
        if (accountsState.value.none { it.id == accountId }) {
            throw IllegalStateException("Account not found")
        }
        activeAccountIdState.value = accountId
        preferences.edit().putString(KEY_ACTIVE_ACCOUNT_ID, accountId).apply()
    }

    override suspend fun removeAccount(accountId: String) {
        val removedAccount = accountsState.value.firstOrNull { it.id == accountId }
        accountsState.update { accounts -> accounts.filterNot { it.id == accountId } }
        persistAccounts()
        if (removedAccount != null && activeAccountIdState.value == accountId) {
            clearActiveAccount()
        }
    }

    override suspend fun clearActiveAccount() {
        activeAccountIdState.value = null
        preferences.edit().remove(KEY_ACTIVE_ACCOUNT_ID).apply()
    }

    private fun persistAccounts() {
        val json = JSONArray()
        accountsState.value.forEach { account -> json.put(account.toStored().toJson()) }
        preferences.edit().putString(KEY_ACCOUNTS, json.toString()).apply()
    }

    private fun loadAccounts(): List<Account> {
        val raw = preferences.getString(KEY_ACCOUNTS, null) ?: return emptyList()
        val array = runCatching { JSONArray(raw) }.getOrNull() ?: return emptyList()
        return buildList {
            for (index in 0 until array.length()) {
                val entry = array.optJSONObject(index) ?: continue
                entry.toStored()?.let { add(it.toDomain()) }
            }
        }
    }

    private fun Account.toStored(): StoredAccount =
        StoredAccount(
            id = id,
            serverUrl = serverUrl,
            displayName = displayName,
            username = username,
            authType = authType,
            accessToken = accessToken,
            refreshToken = refreshToken,
            appPassword = appPassword,
        )

    private data class StoredAccount(
        val id: String,
        val serverUrl: String,
        val displayName: String,
        val username: String?,
        val authType: AuthType,
        val accessToken: String?,
        val refreshToken: String?,
        val appPassword: String?,
    ) {
        fun toDomain(): Account =
            Account(
                id = id,
                serverUrl = serverUrl,
                displayName = displayName,
                username = username,
                authType = authType,
                accessToken = accessToken,
                refreshToken = refreshToken,
                appPassword = appPassword,
            )

        fun toJson(): JSONObject =
            JSONObject()
                .put("id", id)
                .put("serverUrl", serverUrl)
                .put("displayName", displayName)
                .put("username", username)
                .put("authType", authType.name)
                .put("accessToken", accessToken)
                .put("refreshToken", refreshToken)
                .put("appPassword", appPassword)
    }

    private fun JSONObject.toStored(): StoredAccount? {
        val id = optString("id").takeIf { it.isNotBlank() } ?: return null
        val serverUrl = optString("serverUrl").takeIf { it.isNotBlank() } ?: return null
        val displayName = optString("displayName").takeIf { it.isNotBlank() } ?: return null
        val authTypeName = optString("authType").takeIf { it.isNotBlank() } ?: return null
        val authType = runCatching { AuthType.valueOf(authTypeName) }.getOrNull() ?: return null

        return StoredAccount(
            id = id,
            serverUrl = serverUrl,
            displayName = displayName,
            username = optString("username").takeIf { it.isNotBlank() },
            authType = authType,
            accessToken = optString("accessToken").takeIf { it.isNotBlank() },
            refreshToken = optString("refreshToken").takeIf { it.isNotBlank() },
            appPassword = optString("appPassword").takeIf { it.isNotBlank() },
        )
    }

    companion object {
        private const val PREF_NAME = "nextcloud_accounts"
        private const val KEY_ACCOUNTS = "accounts"
        private const val KEY_ACTIVE_ACCOUNT_ID = "activeAccountId"
    }
}
