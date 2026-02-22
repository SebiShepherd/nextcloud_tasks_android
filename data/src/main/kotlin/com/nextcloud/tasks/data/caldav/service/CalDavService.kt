package com.nextcloud.tasks.data.caldav.service

import com.nextcloud.tasks.data.caldav.models.CalendarCollectionInfo
import com.nextcloud.tasks.data.caldav.models.CalendarHomeInfo
import com.nextcloud.tasks.data.caldav.models.CalendarObjectInfo
import com.nextcloud.tasks.data.caldav.models.PrincipalInfo
import com.nextcloud.tasks.data.caldav.parser.DavMultistatusParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * IOException subclass that carries the HTTP status code from a failed CalDAV request.
 */
class CalDavHttpException(
    val statusCode: Int,
    message: String,
) : IOException(message)

/**
 * Service for CalDAV operations
 */
class CalDavService
    @Inject
    constructor(
        @Named("authenticated") private val okHttpClient: OkHttpClient,
        private val parser: DavMultistatusParser,
    ) {
        companion object {
            private const val DAV_ROOT = "/remote.php/dav"
            private val XML_MEDIA_TYPE = "application/xml; charset=utf-8".toMediaType()
        }

        /**
         * Discover the current user's principal URL
         */
        suspend fun discoverPrincipal(baseUrl: String): Result<PrincipalInfo> =
            runCatching {
                val davUrl = buildDavUrl(baseUrl)

                val requestBody =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <d:propfind xmlns:d="DAV:">
                        <d:prop>
                            <d:current-user-principal/>
                        </d:prop>
                    </d:propfind>
                    """.trimIndent().toRequestBody(XML_MEDIA_TYPE)

                val request =
                    Request
                        .Builder()
                        .url(davUrl)
                        .method("PROPFIND", requestBody)
                        .header("Depth", "0")
                        .header("Content-Type", "application/xml; charset=utf-8")
                        .header("Accept", "application/xml")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw CalDavHttpException(response.code, "Failed to discover principal: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                timber.log.Timber.v("Principal discovery response: %s", responseBody)
                val multistatus = parser.parseMultistatus(responseBody)
                parser.parsePrincipalUrl(multistatus) ?: throw IOException("Principal URL not found")
            }

        /**
         * Discover the calendar home set URL for a principal
         */
        suspend fun discoverCalendarHome(
            baseUrl: String,
            principalUrl: String,
        ): Result<CalendarHomeInfo> =
            runCatching {
                val fullUrl = buildFullUrl(baseUrl, principalUrl)

                val requestBody =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                        <d:prop>
                            <c:calendar-home-set/>
                        </d:prop>
                    </d:propfind>
                    """.trimIndent().toRequestBody(XML_MEDIA_TYPE)

                val request =
                    Request
                        .Builder()
                        .url(fullUrl)
                        .method("PROPFIND", requestBody)
                        .header("Depth", "0")
                        .header("Content-Type", "application/xml; charset=utf-8")
                        .header("Accept", "application/xml")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw CalDavHttpException(response.code, "Failed to discover calendar home: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                val multistatus = parser.parseMultistatus(responseBody)
                parser.parseCalendarHomeUrl(multistatus)
                    ?: throw IOException("Calendar home URL not found")
            }

        /**
         * Enumerate calendar collections from the calendar home set
         */
        suspend fun enumerateCalendarCollections(
            baseUrl: String,
            calendarHomeUrl: String,
        ): Result<List<CalendarCollectionInfo>> =
            runCatching {
                val fullUrl = buildFullUrl(baseUrl, calendarHomeUrl)

                val requestBody =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <d:propfind xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav" xmlns:i="http://apple.com/ns/ical/">
                        <d:prop>
                            <d:displayname/>
                            <d:resourcetype/>
                            <c:supported-calendar-component-set/>
                            <i:calendar-color/>
                            <i:calendar-order/>
                            <d:getetag/>
                        </d:prop>
                    </d:propfind>
                    """.trimIndent().toRequestBody(XML_MEDIA_TYPE)

                val request =
                    Request
                        .Builder()
                        .url(fullUrl)
                        .method("PROPFIND", requestBody)
                        .header("Depth", "1")
                        .header("Content-Type", "application/xml; charset=utf-8")
                        .header("Accept", "application/xml")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw CalDavHttpException(response.code, "Failed to enumerate collections: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                val multistatus = parser.parseMultistatus(responseBody)
                parser.parseCalendarCollections(multistatus)
            }

        /**
         * Fetch all VTODOs from a calendar collection
         */
        suspend fun fetchTodosFromCollection(
            baseUrl: String,
            collectionHref: String,
        ): Result<List<CalendarObjectInfo>> =
            runCatching {
                val fullUrl = buildFullUrl(baseUrl, collectionHref)

                val requestBody =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <c:calendar-query xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                        <d:prop>
                            <d:getetag/>
                            <c:calendar-data/>
                        </d:prop>
                        <c:filter>
                            <c:comp-filter name="VCALENDAR">
                                <c:comp-filter name="VTODO"/>
                            </c:comp-filter>
                        </c:filter>
                    </c:calendar-query>
                    """.trimIndent().toRequestBody(XML_MEDIA_TYPE)

                val request =
                    Request
                        .Builder()
                        .url(fullUrl)
                        .method("REPORT", requestBody)
                        .header("Depth", "1")
                        .header("Content-Type", "application/xml; charset=utf-8")
                        .header("Accept", "application/xml")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw CalDavHttpException(response.code, "Failed to fetch todos: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                val multistatus = parser.parseMultistatus(responseBody)
                parser.parseCalendarObjects(multistatus)
            }

        /**
         * Create a new VTODO in a calendar collection
         */
        suspend fun createTodo(
            baseUrl: String,
            collectionHref: String,
            filename: String,
            icalData: String,
        ): Result<String> =
            runCatching {
                val todoUrl = buildFullUrl(baseUrl, "$collectionHref/$filename")

                val requestBody = icalData.toRequestBody("text/calendar; charset=utf-8".toMediaType())

                val request =
                    Request
                        .Builder()
                        .url(todoUrl)
                        .put(requestBody)
                        .header("Content-Type", "text/calendar; charset=utf-8")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw CalDavHttpException(response.code, "Failed to create todo: ${response.code} - ${response.message}")
                }

                // Extract ETag from response
                response.header("ETag")?.trim('"') ?: ""
            }

        /**
         * Update an existing VTODO
         */
        suspend fun updateTodo(
            baseUrl: String,
            todoHref: String,
            icalData: String,
            etag: String?,
        ): Result<String> =
            runCatching {
                val todoUrl = buildFullUrl(baseUrl, todoHref)

                val requestBody = icalData.toRequestBody("text/calendar; charset=utf-8".toMediaType())

                val requestBuilder =
                    Request
                        .Builder()
                        .url(todoUrl)
                        .put(requestBody)
                        .header("Content-Type", "text/calendar; charset=utf-8")

                // Add If-Match header for optimistic locking
                etag?.let {
                    requestBuilder.header("If-Match", "\"$it\"")
                }

                val request = requestBuilder.build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    if (response.code == 412) {
                        throw IOException("Conflict: Task was modified on server (ETag mismatch)")
                    }
                    throw CalDavHttpException(response.code, "Failed to update todo: ${response.code} - ${response.message}")
                }

                // Return new ETag
                response.header("ETag")?.trim('"') ?: etag ?: ""
            }

        /**
         * Delete a VTODO
         */
        suspend fun deleteTodo(
            baseUrl: String,
            todoHref: String,
            etag: String?,
        ): Result<Unit> =
            runCatching {
                val todoUrl = buildFullUrl(baseUrl, todoHref)

                val requestBuilder =
                    Request
                        .Builder()
                        .url(todoUrl)
                        .delete()

                // Add If-Match header for optimistic locking
                etag?.let {
                    requestBuilder.header("If-Match", "\"$it\"")
                }

                val request = requestBuilder.build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    if (response.code == 412) {
                        throw IOException("Conflict: Task was modified on server (ETag mismatch)")
                    }
                    if (response.code == 404) {
                        // Already deleted, consider success
                        return@runCatching
                    }
                    throw CalDavHttpException(response.code, "Failed to delete todo: ${response.code} - ${response.message}")
                }
            }

        /**
         * Build the DAV root URL
         */
        private fun buildDavUrl(baseUrl: String): String {
            val cleanBaseUrl = baseUrl.trimEnd('/')
            return "$cleanBaseUrl$DAV_ROOT"
        }

        /**
         * Build a full URL from base URL and a relative or absolute href
         */
        private fun buildFullUrl(
            baseUrl: String,
            href: String,
        ): String {
            val cleanBaseUrl = baseUrl.trimEnd('/')
            return if (href.startsWith("http://") || href.startsWith("https://")) {
                href
            } else if (href.startsWith("/")) {
                "$cleanBaseUrl$href"
            } else {
                "$cleanBaseUrl/$href"
            }
        }
    }
