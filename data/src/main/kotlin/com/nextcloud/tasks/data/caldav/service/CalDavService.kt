package com.nextcloud.tasks.data.caldav.service

import com.nextcloud.tasks.data.caldav.models.CalendarCollectionInfo
import com.nextcloud.tasks.data.caldav.models.CalendarHomeInfo
import com.nextcloud.tasks.data.caldav.models.CalendarObjectInfo
import com.nextcloud.tasks.data.caldav.models.DavSharee
import com.nextcloud.tasks.data.caldav.models.OcsShareeResult
import com.nextcloud.tasks.data.caldav.models.PrincipalInfo
import com.nextcloud.tasks.data.caldav.parser.DavMultistatusParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.util.UUID
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
                            <d:share-access/>
                            <d:invite/>
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
                    throw CalDavHttpException(
                        response.code,
                        "Failed to create todo: ${response.code} - ${response.message}",
                    )
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
                    throw CalDavHttpException(
                        response.code,
                        "Failed to update todo: ${response.code} - ${response.message}",
                    )
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
                    throw CalDavHttpException(
                        response.code,
                        "Failed to delete todo: ${response.code} - ${response.message}",
                    )
                }
            }

        /**
         * Create a new calendar collection (task list) via MKCOL
         */
        suspend fun createCalendarCollection(
            baseUrl: String,
            calendarHomeUrl: String,
            displayName: String,
            color: String? = null,
        ): Result<String> =
            runCatching {
                val collectionSlug = UUID.randomUUID().toString()
                val collectionHref = "${calendarHomeUrl.trimEnd('/')}/$collectionSlug/"
                val collectionUrl = buildFullUrl(baseUrl, collectionHref)

                val escapedName =
                    displayName
                        .replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&apos;")

                val requestBody =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <d:mkcol xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav">
                        <d:set>
                            <d:prop>
                                <d:displayname>$escapedName</d:displayname>
                                <d:resourcetype>
                                    <d:collection/>
                                    <c:calendar/>
                                </d:resourcetype>
                                <c:supported-calendar-component-set>
                                    <c:comp name="VTODO"/>
                                </c:supported-calendar-component-set>
                            </d:prop>
                        </d:set>
                    </d:mkcol>
                    """.trimIndent().toRequestBody(XML_MEDIA_TYPE)

                val request =
                    Request
                        .Builder()
                        .url(collectionUrl)
                        .method("MKCOL", requestBody)
                        .header("Content-Type", "application/xml; charset=utf-8")
                        .build()

                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw CalDavHttpException(
                            response.code,
                            "Failed to create calendar collection: ${response.code} - ${response.message}",
                        )
                    }
                }

                // After collection creation, set color via PROPPATCH if provided
                if (color != null) {
                    setCalendarColor(baseUrl, collectionHref, color)
                }

                collectionHref
            }

        /**
         * Set the calendar color via PROPPATCH on an existing collection.
         * Failures are logged but not propagated — the list was created successfully.
         */
        fun setCalendarColor(
            baseUrl: String,
            collectionHref: String,
            color: String,
        ) {
            try {
                val collectionUrl = buildFullUrl(baseUrl, collectionHref)
                val requestBody =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <d:propertyupdate xmlns:d="DAV:" xmlns:i="http://apple.com/ns/ical/">
                        <d:set>
                            <d:prop>
                                <i:calendar-color>$color</i:calendar-color>
                            </d:prop>
                        </d:set>
                    </d:propertyupdate>
                    """.trimIndent().toRequestBody(XML_MEDIA_TYPE)
                val request =
                    Request
                        .Builder()
                        .url(collectionUrl)
                        .method("PROPPATCH", requestBody)
                        .header("Content-Type", "application/xml; charset=utf-8")
                        .build()
                okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Timber.w("PROPPATCH calendar-color returned %d for %s", response.code, collectionHref)
                    }
                }
            } catch (e: IOException) {
                Timber.w(e, "Failed to set calendar color via PROPPATCH")
            } catch (e: IllegalStateException) {
                Timber.w(e, "Failed to set calendar color via PROPPATCH")
            }
        }

        /**
         * Share or update sharing for a calendar collection.
         * POST with application/davsharing+xml
         *
         * @param principalHrefs list of sharee principal URIs
         *   (e.g., "principal:principals/users/john" or "principal:principals/groups/team")
         * @param access one of "read-write", "read", or "no-access" (to remove)
         */
        suspend fun shareResource(
            baseUrl: String,
            collectionHref: String,
            principalHref: String,
            access: String,
        ): Result<Unit> =
            runCatching {
                val fullUrl = buildFullUrl(baseUrl, collectionHref)

                val accessElement =
                    when (access) {
                        "no-access" -> "<D:no-access/>"
                        "read" -> "<D:read/>"
                        else -> "<D:read-write/>"
                    }

                val requestBody =
                    """
                    <?xml version="1.0" encoding="utf-8" ?>
                    <D:share-resource xmlns:D="DAV:">
                        <D:sharee>
                            <D:href>$principalHref</D:href>
                            <D:share-access>
                                $accessElement
                            </D:share-access>
                        </D:sharee>
                    </D:share-resource>
                    """.trimIndent()
                        .toRequestBody("application/davsharing+xml; charset=utf-8".toMediaType())

                val request =
                    Request
                        .Builder()
                        .url(fullUrl)
                        .post(requestBody)
                        .header("Content-Type", "application/davsharing+xml; charset=utf-8")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw CalDavHttpException(
                        response.code,
                        "Failed to share resource: ${response.code} - ${response.message}",
                    )
                }
            }

        /**
         * Get current invites (sharees) for a calendar collection via PROPFIND.
         */
        suspend fun getInvites(
            baseUrl: String,
            collectionHref: String,
        ): Result<List<DavSharee>> =
            runCatching {
                val fullUrl = buildFullUrl(baseUrl, collectionHref)

                val requestBody =
                    """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <d:propfind xmlns:d="DAV:">
                        <d:prop>
                            <d:invite/>
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
                    throw CalDavHttpException(
                        response.code,
                        "Failed to get invites: ${response.code}",
                    )
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                val multistatus = parser.parseMultistatus(responseBody)
                multistatus.responses.firstOrNull()?.invites ?: emptyList()
            }

        /**
         * Search for sharees (users and groups) using the OCS API.
         */
        suspend fun searchSharees(
            baseUrl: String,
            query: String,
        ): Result<List<OcsShareeResult>> =
            runCatching {
                val cleanBaseUrl = baseUrl.trimEnd('/')
                val url =
                    "$cleanBaseUrl/ocs/v1.php/apps/files_sharing/api/v1/sharees" +
                        "?search=${java.net.URLEncoder.encode(query, "UTF-8")}" +
                        "&itemType=file&perPage=20&format=json"

                val request =
                    Request
                        .Builder()
                        .url(url)
                        .get()
                        .header("OCS-APIREQUEST", "true")
                        .header("Accept", "application/json")
                        .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw CalDavHttpException(
                        response.code,
                        "Failed to search sharees: ${response.code}",
                    )
                }

                val body = response.body?.string() ?: throw IOException("Empty response")
                parseOcsShareeResponse(body)
            }

        /**
         * Parse the OCS sharee search JSON response.
         * Response structure: { ocs: { data: { exact: { users: [...], groups: [...] }, users: [...], groups: [...] } } }
         */
        private fun parseOcsShareeResponse(json: String): List<OcsShareeResult> {
            val results = mutableListOf<OcsShareeResult>()
            try {
                val root = org.json.JSONObject(json)
                val data = root.getJSONObject("ocs").getJSONObject("data")

                // Parse users from both exact and non-exact matches
                parseShareeArray(data, "users", "USER", results)
                parseShareeArray(data, "groups", "GROUP", results)

                // Also check exact matches
                if (data.has("exact")) {
                    val exact = data.getJSONObject("exact")
                    parseShareeArray(exact, "users", "USER", results)
                    parseShareeArray(exact, "groups", "GROUP", results)
                }
            } catch (e: org.json.JSONException) {
                Timber.w(e, "Failed to parse OCS sharee response")
            }
            return results.distinctBy { it.id + it.type }
        }

        private fun parseShareeArray(
            parent: org.json.JSONObject,
            key: String,
            type: String,
            results: MutableList<OcsShareeResult>,
        ) {
            if (!parent.has(key)) return
            val array = parent.getJSONArray(key)
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val label = item.optString("label", "")
                val value = item.optJSONObject("value")
                val shareWith = value?.optString("shareWith", "") ?: ""
                if (shareWith.isNotEmpty()) {
                    results.add(OcsShareeResult(id = shareWith, displayName = label, type = type))
                }
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
