package com.nextcloud.tasks.di

import javax.inject.Qualifier

/**
 * Qualifies an application-lifetime [kotlinx.coroutines.CoroutineScope] for work that must outlive
 * the component that started it (e.g. persisting an edit while the user navigates away and the
 * ViewModel — and its viewModelScope — is being cleared).
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
