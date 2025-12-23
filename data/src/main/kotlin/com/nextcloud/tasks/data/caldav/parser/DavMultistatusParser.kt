package com.nextcloud.tasks.data.caldav.parser

import android.util.Xml
import com.nextcloud.tasks.data.caldav.models.CalendarCollectionInfo
import com.nextcloud.tasks.data.caldav.models.CalendarHomeInfo
import com.nextcloud.tasks.data.caldav.models.CalendarObjectInfo
import com.nextcloud.tasks.data.caldav.models.DavMultistatus
import com.nextcloud.tasks.data.caldav.models.DavProperty
import com.nextcloud.tasks.data.caldav.models.DavResourceResponse
import com.nextcloud.tasks.data.caldav.models.PrincipalInfo
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.StringReader
import javax.inject.Inject

/**
 * Parser for CalDAV/WebDAV multistatus XML responses
 */
class DavMultistatusParser
    @Inject
    constructor() {
        fun parseMultistatus(xml: String): DavMultistatus {
            val parser = Xml.newPullParser()
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
            parser.setInput(StringReader(xml))

            val responses = mutableListOf<DavResourceResponse>()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && matchesTag(parser.name, "response")) {
                    responses.add(parseResponse(parser))
                }
                eventType = parser.next()
            }

            return DavMultistatus(responses)
        }

        /**
         * Check if a tag name matches, handling namespace prefixes
         * e.g., "d:response" or "response" both match "response"
         */
        private fun matchesTag(
            tagName: String,
            expectedName: String,
        ): Boolean = tagName == expectedName || tagName.endsWith(":$expectedName")

        private fun parseResponse(parser: XmlPullParser): DavResourceResponse {
            var href = ""
            val properties = mutableMapOf<String, String?>()
            var etag: String? = null

            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && matchesTag(parser.name, "response")) {
                    break
                }
                if (eventType != XmlPullParser.START_TAG) continue

                when {
                    matchesTag(parser.name, "href") -> href = parser.nextText()
                    matchesTag(parser.name, "propstat") -> {
                        val propstatProps = parsePropstat(parser)
                        properties.putAll(propstatProps)
                        propstatProps[DavProperty.GET_ETAG]?.let { etag = it }
                    }
                }
            }

            return DavResourceResponse(href, properties, etag)
        }

        private fun parsePropstat(parser: XmlPullParser): Map<String, String?> {
            val properties = mutableMapOf<String, String?>()
            var statusCode: String? = null

            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && matchesTag(parser.name, "propstat")) {
                    break
                }
                if (eventType != XmlPullParser.START_TAG) continue

                when {
                    matchesTag(parser.name, "status") -> {
                        statusCode = parser.nextText()
                    }
                    matchesTag(parser.name, "prop") -> {
                        parseProp(parser, properties)
                    }
                }
            }

            // Only return properties if status is 200 OK
            return if (statusCode?.contains("200") == true) properties else emptyMap()
        }

        private fun parseProp(
            parser: XmlPullParser,
            properties: MutableMap<String, String?>,
        ) {
            val depth = parser.depth
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
                if (eventType != XmlPullParser.START_TAG) continue

                when {
                    matchesTag(parser.name, "current-user-principal") -> {
                        properties[DavProperty.CURRENT_USER_PRINCIPAL] = parseHrefValue(parser)
                    }
                    matchesTag(parser.name, "calendar-home-set") -> {
                        properties[DavProperty.CALENDAR_HOME_SET] = parseHrefValue(parser)
                    }
                    matchesTag(parser.name, "displayname") -> {
                        properties[DavProperty.DISPLAY_NAME] = parser.nextText()
                    }
                    matchesTag(parser.name, "getetag") -> {
                        properties[DavProperty.GET_ETAG] = parser.nextText().trim('"')
                    }
                    matchesTag(parser.name, "resourcetype") -> {
                        properties[DavProperty.RESOURCE_TYPE] = parseResourceType(parser)
                    }
                    matchesTag(parser.name, "supported-calendar-component-set") -> {
                        properties[DavProperty.SUPPORTED_CALENDAR_COMPONENT_SET] =
                            parseSupportedComponents(parser)
                    }
                    matchesTag(parser.name, "calendar-data") -> {
                        properties[DavProperty.CALENDAR_DATA] = parser.nextText()
                    }
                    matchesTag(parser.name, "calendar-color") -> {
                        properties[DavProperty.CALENDAR_COLOR] = parser.nextText()
                    }
                    matchesTag(parser.name, "calendar-order") -> {
                        properties[DavProperty.CALENDAR_ORDER] = parser.nextText()
                    }
                }
            }
        }

        private fun parseHrefValue(parser: XmlPullParser): String? {
            val depth = parser.depth
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
                if (eventType == XmlPullParser.START_TAG && matchesTag(parser.name, "href")) {
                    return parser.nextText()
                }
            }
            return null
        }

        private fun parseResourceType(parser: XmlPullParser): String {
            val types = mutableListOf<String>()
            val depth = parser.depth

            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
                if (eventType == XmlPullParser.START_TAG) {
                    // Extract tag name without namespace prefix
                    val tagName = parser.name
                    val cleanName = if (tagName.contains(":")) {
                        tagName.substringAfter(":")
                    } else {
                        tagName
                    }
                    types.add(cleanName)
                }
            }

            return types.joinToString(",")
        }

        private fun parseSupportedComponents(parser: XmlPullParser): String {
            val components = mutableListOf<String>()
            val depth = parser.depth

            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
                if (eventType == XmlPullParser.START_TAG && matchesTag(parser.name, "comp")) {
                    parser.getAttributeValue(null, "name")?.let { components.add(it) }
                }
            }

            return components.joinToString(",")
        }

        /**
         * Extract principal URL from multistatus response
         */
        fun parsePrincipalUrl(multistatus: DavMultistatus): PrincipalInfo? {
            timber.log.Timber.d("Parsing principal from ${multistatus.responses.size} responses")
            multistatus.responses.forEach { response ->
                timber.log.Timber.d("Response href: ${response.href}, properties: ${response.properties}")
            }
            val principalHref =
                multistatus.responses
                    .firstOrNull()
                    ?.properties
                    ?.get(DavProperty.CURRENT_USER_PRINCIPAL)
            timber.log.Timber.d("Principal href found: $principalHref")
            return principalHref?.let { PrincipalInfo(it) }
        }

        /**
         * Extract calendar home URL from multistatus response
         */
        fun parseCalendarHomeUrl(multistatus: DavMultistatus): CalendarHomeInfo? {
            val calendarHomeHref =
                multistatus.responses
                    .firstOrNull()
                    ?.properties
                    ?.get(DavProperty.CALENDAR_HOME_SET)
            return calendarHomeHref?.let { CalendarHomeInfo(it) }
        }

        /**
         * Extract calendar collections from multistatus response
         */
        fun parseCalendarCollections(multistatus: DavMultistatus): List<CalendarCollectionInfo> =
            multistatus.responses.mapNotNull { response ->
                val resourceType = response.properties[DavProperty.RESOURCE_TYPE] ?: ""
                val supportedComponentsStr =
                    response.properties[DavProperty.SUPPORTED_CALENDAR_COMPONENT_SET] ?: ""

                // Only include collections that are calendars, support VTODO, and are not deleted
                val isCalendar = resourceType.contains("calendar")
                val supportsVTodo = supportedComponentsStr.contains("VTODO")
                val isDeleted = resourceType.contains("deleted-calendar")

                if (isCalendar && supportsVTodo && !isDeleted) {
                    CalendarCollectionInfo(
                        href = response.href,
                        displayName = response.properties[DavProperty.DISPLAY_NAME] ?: "Unnamed",
                        supportedComponents = supportedComponentsStr.split(","),
                        color = response.properties[DavProperty.CALENDAR_COLOR],
                        order = response.properties[DavProperty.CALENDAR_ORDER]?.toIntOrNull(),
                        etag = response.etag,
                    )
                } else {
                    null
                }
            }

        /**
         * Extract calendar objects (tasks) from multistatus response
         */
        fun parseCalendarObjects(multistatus: DavMultistatus): List<CalendarObjectInfo> =
            multistatus.responses.mapNotNull { response ->
                val calendarData = response.properties[DavProperty.CALENDAR_DATA]
                val etag = response.etag

                if (calendarData != null && etag != null) {
                    CalendarObjectInfo(
                        href = response.href,
                        etag = etag,
                        calendarData = calendarData,
                    )
                } else {
                    null
                }
            }
    }
