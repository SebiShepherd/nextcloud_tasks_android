package com.nextcloud.tasks.data.caldav.models

/**
 * Models for CalDAV/WebDAV responses
 */

/**
 * A multistatus response containing multiple resource responses
 */
data class DavMultistatus(
    val responses: List<DavResourceResponse>,
)

/**
 * A response for a single resource
 */
data class DavResourceResponse(
    val href: String,
    val properties: Map<String, String?>,
    val etag: String? = null,
)

/**
 * Result of principal discovery
 */
data class PrincipalInfo(
    val principalUrl: String,
)

/**
 * Result of calendar-home-set discovery
 */
data class CalendarHomeInfo(
    val calendarHomeUrl: String,
)

/**
 * Information about a calendar collection
 */
data class CalendarCollectionInfo(
    val href: String,
    val displayName: String,
    val supportedComponents: List<String>,
    val color: String? = null,
    val order: Int? = null,
    val etag: String? = null,
)

/**
 * Information about a calendar object (task/event)
 */
data class CalendarObjectInfo(
    val href: String,
    val etag: String,
    val calendarData: String,
)
