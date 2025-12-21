package com.nextcloud.tasks.data.caldav.service

import com.nextcloud.tasks.data.caldav.models.CalendarCollectionInfo
import com.nextcloud.tasks.data.caldav.models.CalendarHomeInfo
import com.nextcloud.tasks.data.caldav.models.CalendarObjectInfo
import com.nextcloud.tasks.data.caldav.models.DavProperty
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
                    throw IOException("Failed to discover principal: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
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
                    throw IOException("Failed to discover calendar home: ${response.code}")
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
                    throw IOException("Failed to enumerate collections: ${response.code}")
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
                    throw IOException("Failed to fetch todos: ${response.code}")
                }

                val responseBody = response.body?.string() ?: throw IOException("Empty response")
                val multistatus = parser.parseMultistatus(responseBody)
                parser.parseCalendarObjects(multistatus)
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
