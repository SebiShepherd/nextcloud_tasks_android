package com.nextcloud.tasks.data.caldav.parser

import android.util.Xml
import com.nextcloud.tasks.data.caldav.models.CalendarCollectionInfo
import com.nextcloud.tasks.data.caldav.models.CalendarHomeInfo
import com.nextcloud.tasks.data.caldav.models.CalendarObjectInfo
import com.nextcloud.tasks.data.caldav.models.DavMultistatus
import com.nextcloud.tasks.data.caldav.models.DavProperty
import com.nextcloud.tasks.data.caldav.models.DavResourceResponse
import com.nextcloud.tasks.data.caldav.models.DavSharee
import com.nextcloud.tasks.data.caldav.models.PrincipalInfo
import org.xmlpull.v1.XmlPullParser
import timber.log.Timber
import java.io.StringReader
import javax.inject.Inject

/**
 * Parser for CalDAV/WebDAV multistatus XML responses
 */
@Suppress("TooManyFunctions")
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
            val invites = mutableListOf<DavSharee>()
            val organizerRef = mutableListOf<String>()

            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && matchesTag(parser.name, "response")) {
                    break
                }
                if (eventType != XmlPullParser.START_TAG) continue

                when {
                    matchesTag(parser.name, "href") -> href = parser.nextText()
                    matchesTag(parser.name, "propstat") -> {
                        val parsedInvites = mutableListOf<DavSharee>()
                        val propstatProps = parsePropstat(parser, parsedInvites, organizerRef)
                        properties.putAll(propstatProps)
                        invites.addAll(parsedInvites)
                        propstatProps[DavProperty.GET_ETAG]?.let { etag = it }
                    }
                }
            }

            return DavResourceResponse(
                href,
                properties,
                etag,
                invites,
                organizerRef.firstOrNull(),
                ownerHref = properties[DavProperty.OWNER],
                hasWriteAccess =
                    properties[DavProperty.CURRENT_USER_PRIVILEGE_SET]?.let { privilegeStr ->
                        // Split into individual privilege names and check for exact write access.
                        // "write-properties" is granted even for read-only sharees (for display
                        // preferences) and must not be mistaken for actual write access.
                        privilegeStr.split(",").any { it in listOf("write", "write-content") }
                    },
            )
        }

        private fun parsePropstat(
            parser: XmlPullParser,
            invites: MutableList<DavSharee>,
            organizerRef: MutableList<String>,
        ): Map<String, String?> {
            val properties = mutableMapOf<String, String?>()
            var statusCode: String? = null
            // Use a local list so a non-200 propstat (e.g. the common 404 block for
            // unsupported properties) doesn't wipe organizer info from a prior 200 block.
            val localOrganizerRef = mutableListOf<String>()

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
                        parseProp(parser, properties, invites, localOrganizerRef)
                    }
                }
            }

            // Only propagate data from 200 OK propstats.
            if (statusCode?.contains("200") != true) {
                invites.clear()
                return emptyMap()
            }
            organizerRef.addAll(localOrganizerRef)
            return properties
        }

        private data class InviteParseResult(
            val sharees: List<DavSharee>,
            val organizerHref: String? = null,
        )

        @Suppress("CyclomaticComplexMethod")
        private fun parseProp(
            parser: XmlPullParser,
            properties: MutableMap<String, String?>,
            invites: MutableList<DavSharee>,
            organizerRef: MutableList<String>,
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
                    matchesTag(parser.name, "share-access") -> {
                        properties[DavProperty.SHARE_ACCESS] = parseShareAccess(parser)
                    }
                    matchesTag(parser.name, "owner-principal") -> {
                        properties[DavProperty.OWNER_PRINCIPAL] = parseHrefValue(parser)
                    }
                    matchesTag(parser.name, "owner") -> {
                        properties[DavProperty.OWNER] = parseHrefValue(parser)
                    }
                    matchesTag(parser.name, "current-user-privilege-set") -> {
                        properties[DavProperty.CURRENT_USER_PRIVILEGE_SET] =
                            parsePrivilegeSet(parser)
                    }
                    matchesTag(parser.name, "invite") -> {
                        val result = parseInviteElement(parser)
                        invites.addAll(result.sharees)
                        result.organizerHref?.let { organizerRef.add(it) }
                    }
                }
            }
        }

        /**
         * Parse the share-access element to extract the access level.
         * Expected structure: <d:share-access><d:shared-owner/></d:share-access>
         */
        private fun parseShareAccess(parser: XmlPullParser): String? {
            val depth = parser.depth
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    val cleanName =
                        if (tagName.contains(":")) tagName.substringAfter(":") else tagName
                    // Skip to end of the child element
                    skipElement(parser)
                    return cleanName
                }
            }
            return null
        }

        /**
         * Parse the invite element containing a list of sharees.
         * Expected structure:
         * <d:invite>
         *   <d:sharee>
         *     <d:href>principal:principals/users/john</d:href>
         *     <d:prop><d:displayname>John</d:displayname></d:prop>
         *     <d:share-access><d:read-write/></d:share-access>
         *   </d:sharee>
         * </d:invite>
         */
        private fun parseInviteElement(parser: XmlPullParser): InviteParseResult {
            val sharees = mutableListOf<DavSharee>()
            var organizerHref: String? = null
            val depth = parser.depth

            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
                if (eventType != XmlPullParser.START_TAG) continue

                when {
                    // DAV format: <d:sharee>, Nextcloud format: <oc:user>
                    matchesTag(parser.name, "sharee") || matchesTag(parser.name, "user") -> {
                        parseShareeElement(parser)?.let { sharees.add(it) }
                    }
                    // Parse organizer to extract owner principal href
                    matchesTag(parser.name, "organizer") -> {
                        organizerHref = parseOrganizerHref(parser)
                    }
                }
            }

            return InviteParseResult(sharees, organizerHref)
        }

        private fun parseOrganizerHref(parser: XmlPullParser): String? {
            val depth = parser.depth
            var href: String? = null
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
                if (eventType == XmlPullParser.START_TAG && matchesTag(parser.name, "href")) {
                    href = parser.nextText()
                }
            }
            return href
        }

        private fun parseShareeElement(parser: XmlPullParser): DavSharee? {
            var href: String? = null
            var commonName: String? = null
            var access: String? = null
            val depth = parser.depth

            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
                if (eventType != XmlPullParser.START_TAG) continue

                when {
                    matchesTag(parser.name, "href") -> href = parser.nextText()
                    matchesTag(parser.name, "common-name") -> commonName = parser.nextText()
                    // DAV format: <d:share-access>, Nextcloud format: <oc:access>
                    matchesTag(parser.name, "share-access") || matchesTag(parser.name, "access") -> {
                        access = parseShareAccess(parser)
                    }
                    matchesTag(parser.name, "prop") -> {
                        commonName = parseShareeProp(parser) ?: commonName
                    }
                    // Skip invite-accepted/invite-noresponse status elements
                    matchesTag(parser.name, "invite-accepted") ||
                        matchesTag(parser.name, "invite-noresponse") -> {
                        skipElement(parser)
                    }
                }
            }

            return href?.let { DavSharee(it, commonName, access) }
        }

        /**
         * Parse prop element inside a sharee to extract displayname.
         */
        private fun parseShareeProp(parser: XmlPullParser): String? {
            val depth = parser.depth
            var displayName: String? = null
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
                if (eventType == XmlPullParser.START_TAG && matchesTag(parser.name, "displayname")) {
                    displayName = parser.nextText()
                }
            }
            return displayName
        }

        private fun skipElement(parser: XmlPullParser) {
            val depth = parser.depth
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) {
                    break
                }
            }
        }

        private fun parsePrivilegeSet(parser: XmlPullParser): String {
            val privileges = mutableListOf<String>()
            val depth = parser.depth
            while (true) {
                val eventType = parser.next()
                if (eventType == XmlPullParser.END_TAG && parser.depth == depth) break
                if (eventType == XmlPullParser.START_TAG) {
                    val tagName = parser.name
                    val cleanName =
                        if (tagName.contains(":")) tagName.substringAfter(":") else tagName
                    if (cleanName == "privilege") continue
                    privileges.add(cleanName)
                }
            }
            return privileges.joinToString(",")
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
                    val cleanName =
                        if (tagName.contains(":")) {
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
            val principalHref =
                multistatus.responses
                    .firstOrNull()
                    ?.properties
                    ?.get(DavProperty.CURRENT_USER_PRINCIPAL)
            Timber.v("Parsed principal href: %s", principalHref)
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
                    val shareAccess = response.properties[DavProperty.SHARE_ACCESS]
                    val ownerPrincipal = response.properties[DavProperty.OWNER_PRINCIPAL]
                    CalendarCollectionInfo(
                        href = response.href,
                        displayName = response.properties[DavProperty.DISPLAY_NAME] ?: "Unnamed",
                        supportedComponents = supportedComponentsStr.split(","),
                        color = response.properties[DavProperty.CALENDAR_COLOR],
                        order = response.properties[DavProperty.CALENDAR_ORDER]?.toIntOrNull(),
                        etag = response.etag,
                        shareAccess = shareAccess,
                        ownerPrincipalHref = ownerPrincipal,
                        organizerHref = response.organizerHref,
                        ownerHref = response.ownerHref,
                        hasWriteAccess = response.hasWriteAccess,
                        invites = response.invites,
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
