package com.nextcloud.tasks.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nextcloud.tasks.domain.model.TaskSort
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

/**
 * App-level UI preferences persisted with DataStore: the global sort, whether sort is remembered per
 * list, and each list's remembered sort. Survives process death (unlike the transient ViewModel state).
 */
@Singleton
class AppPreferences
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) {
        private val store = context.appDataStore

        val globalSort: Flow<TaskSort> =
            store.data.map { it[GLOBAL_SORT]?.toTaskSort() ?: DEFAULT_SORT }

        val perListSortEnabled: Flow<Boolean> =
            store.data.map { it[PER_LIST_ENABLED] ?: false }

        /** The remembered sort for [listId], or null if that list has none yet. */
        fun listSort(listId: String): Flow<TaskSort?> = store.data.map { it[listSortKey(listId)]?.toTaskSort() }

        suspend fun setGlobalSort(sort: TaskSort) {
            store.edit { it[GLOBAL_SORT] = sort.name }
        }

        suspend fun setPerListSortEnabled(enabled: Boolean) {
            store.edit { it[PER_LIST_ENABLED] = enabled }
        }

        suspend fun setListSort(
            listId: String,
            sort: TaskSort,
        ) {
            store.edit { it[listSortKey(listId)] = sort.name }
        }

        private fun String.toTaskSort(): TaskSort? = TaskSort.entries.firstOrNull { it.name == this }

        private companion object {
            val DEFAULT_SORT = TaskSort.DUE_DATE
            val GLOBAL_SORT = stringPreferencesKey("global_sort")
            val PER_LIST_ENABLED = booleanPreferencesKey("per_list_sort_enabled")

            fun listSortKey(listId: String) = stringPreferencesKey("list_sort_$listId")
        }
    }
