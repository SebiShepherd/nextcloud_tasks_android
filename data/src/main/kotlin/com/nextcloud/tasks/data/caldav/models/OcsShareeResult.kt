package com.nextcloud.tasks.data.caldav.models

/**
 * Result from the OCS sharee search API
 */
data class OcsShareeResult(
    val id: String,
    val displayName: String,
    val type: String,
)
