package com.nextcloud.tasks.data.caldav.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DavMultistatusParserTest {
    private val parser = DavMultistatusParser()

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun xml(body: String) =
        """<d:multistatus xmlns:d="DAV:" xmlns:c="urn:ietf:params:xml:ns:caldav"
               xmlns:i="http://apple.com/ns/ical/" xmlns:oc="http://owncloud.org/ns">
$body
</d:multistatus>"""

    private fun calendarResponse(
        href: String = "/calendars/admin/tasks/",
        displayName: String = "My Tasks",
        privilegeSet: String = "<d:privilege><d:read/></d:privilege><d:privilege><d:write/></d:privilege>",
        extraProps: String = "",
        extra404Props: String = "",
    ) = xml(
        """
  <d:response>
    <d:href>$href</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>$displayName</d:displayname>
        <d:resourcetype><d:collection/><c:calendar/></d:resourcetype>
        <c:supported-calendar-component-set><c:comp name="VTODO"/></c:supported-calendar-component-set>
        <d:current-user-privilege-set>$privilegeSet</d:current-user-privilege-set>
        $extraProps
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
    ${if (extra404Props.isNotEmpty()) {
            """
    <d:propstat>
      <d:prop>$extra404Props</d:prop>
      <d:status>HTTP/1.1 404 Not Found</d:status>
    </d:propstat>"""
        } else {
            ""
        }}
  </d:response>""",
    )

    // ── basic parsing ─────────────────────────────────────────────────────────

    @Test
    fun `parseMultistatus returns empty list for empty multistatus`() {
        val result = parser.parseMultistatus(xml(""))
        assertTrue(result.responses.isEmpty())
    }

    @Test
    fun `parseCalendarCollections returns display name and href`() {
        val multistatus =
            parser.parseMultistatus(
                calendarResponse(href = "/calendars/admin/tasks/", displayName = "Work"),
            )
        val collections = parser.parseCalendarCollections(multistatus)

        assertEquals(1, collections.size)
        assertEquals("Work", collections[0].displayName)
        assertEquals("/calendars/admin/tasks/", collections[0].href)
    }

    @Test
    fun `parseCalendarCollections skips non-calendar resources`() {
        val response =
            xml(
                """
  <d:response>
    <d:href>/calendars/admin/contacts/</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>Contacts</d:displayname>
        <d:resourcetype><d:collection/></d:resourcetype>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>""",
            )
        val collections = parser.parseCalendarCollections(parser.parseMultistatus(response))
        assertTrue(collections.isEmpty())
    }

    @Test
    fun `parseCalendarCollections skips calendars without VTODO support`() {
        val response =
            xml(
                """
  <d:response>
    <d:href>/calendars/admin/personal/</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>Personal</d:displayname>
        <d:resourcetype><d:collection/><c:calendar/></d:resourcetype>
        <c:supported-calendar-component-set><c:comp name="VEVENT"/></c:supported-calendar-component-set>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>""",
            )
        val collections = parser.parseCalendarCollections(parser.parseMultistatus(response))
        assertTrue(collections.isEmpty())
    }

    // ── hasWriteAccess / privilege set ────────────────────────────────────────

    @Test
    fun `hasWriteAccess is true when privilege set contains write`() {
        val privileges =
            "<d:privilege><d:read/></d:privilege>" +
                "<d:privilege><d:write/></d:privilege>" +
                "<d:privilege><d:write-properties/></d:privilege>"
        val multistatus = parser.parseMultistatus(calendarResponse(privilegeSet = privileges))
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertTrue(collection.hasWriteAccess == true)
    }

    @Test
    fun `hasWriteAccess is false when privilege set contains only write-properties`() {
        // Regression: write-properties is granted even to read-only sharees and
        // must not be mistaken for write access.
        val privileges =
            "<d:privilege><d:read/></d:privilege>" +
                "<d:privilege><d:write-properties/></d:privilege>"
        val multistatus = parser.parseMultistatus(calendarResponse(privilegeSet = privileges))
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertFalse(collection.hasWriteAccess == true)
    }

    @Test
    fun `hasWriteAccess is true when privilege set contains write-content`() {
        val privileges =
            "<d:privilege><d:read/></d:privilege>" +
                "<d:privilege><d:write-content/></d:privilege>"
        val multistatus = parser.parseMultistatus(calendarResponse(privilegeSet = privileges))
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertTrue(collection.hasWriteAccess == true)
    }

    @Test
    fun `hasWriteAccess is null when no privilege set present`() {
        val response =
            xml(
                """
  <d:response>
    <d:href>/calendars/admin/tasks/</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>Tasks</d:displayname>
        <d:resourcetype><d:collection/><c:calendar/></d:resourcetype>
        <c:supported-calendar-component-set><c:comp name="VTODO"/></c:supported-calendar-component-set>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>""",
            )
        val collection = parser.parseCalendarCollections(parser.parseMultistatus(response))[0]

        assertNull(collection.hasWriteAccess)
    }

    // ── organizer / P2 regression ─────────────────────────────────────────────

    @Test
    fun `organizerHref is preserved when a trailing 404 propstat follows 200`() {
        // Regression: a 404 propstat for unsupported properties must not wipe
        // the organizer info parsed from the preceding 200 propstat.
        val inviteBlock =
            """<oc:invite>
          <oc:organizer><d:href>principals/users/admin</d:href></oc:organizer>
        </oc:invite>"""
        val multistatus =
            parser.parseMultistatus(
                calendarResponse(
                    extraProps = inviteBlock,
                    extra404Props = "<d:owner-principal/>",
                ),
            )
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertEquals("principals/users/admin", collection.organizerHref)
    }

    @Test
    fun `organizerHref is null when invite has no organizer`() {
        val multistatus = parser.parseMultistatus(calendarResponse())
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertNull(collection.organizerHref)
    }

    // ── sharee invite parsing ─────────────────────────────────────────────────

    @Test
    fun `invite block with one sharee is parsed correctly`() {
        val inviteBlock =
            """<oc:invite>
          <oc:user>
            <d:href>principals/users/john</d:href>
            <d:prop><d:displayname>John Doe</d:displayname></d:prop>
            <oc:access><oc:read-write/></oc:access>
          </oc:user>
        </oc:invite>"""
        val multistatus = parser.parseMultistatus(calendarResponse(extraProps = inviteBlock))
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertEquals(1, collection.invites.size)
        assertEquals("principals/users/john", collection.invites[0].href)
        assertEquals("John Doe", collection.invites[0].commonName)
        assertEquals("read-write", collection.invites[0].access)
    }

    @Test
    fun `invite block with multiple sharees is parsed correctly`() {
        val inviteBlock =
            """<oc:invite>
          <oc:user>
            <d:href>principals/users/alice</d:href>
            <d:prop><d:displayname>Alice</d:displayname></d:prop>
            <oc:access><oc:read-write/></oc:access>
          </oc:user>
          <oc:user>
            <d:href>principals/users/bob</d:href>
            <d:prop><d:displayname>Bob</d:displayname></d:prop>
            <oc:access><oc:read/></oc:access>
          </oc:user>
        </oc:invite>"""
        val multistatus = parser.parseMultistatus(calendarResponse(extraProps = inviteBlock))
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertEquals(2, collection.invites.size)
        assertEquals("principals/users/alice", collection.invites[0].href)
        assertEquals("principals/users/bob", collection.invites[1].href)
        assertEquals("read", collection.invites[1].access)
    }

    @Test
    fun `sharees from non-200 propstat are ignored`() {
        val response =
            xml(
                """
  <d:response>
    <d:href>/calendars/admin/tasks/</d:href>
    <d:propstat>
      <d:prop>
        <d:displayname>Tasks</d:displayname>
        <d:resourcetype><d:collection/><c:calendar/></d:resourcetype>
        <c:supported-calendar-component-set><c:comp name="VTODO"/></c:supported-calendar-component-set>
        <d:current-user-privilege-set><d:privilege><d:write/></d:privilege></d:current-user-privilege-set>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
    <d:propstat>
      <d:prop>
        <oc:invite>
          <oc:user>
            <d:href>principals/users/ghost</d:href>
            <oc:access><oc:read/></oc:access>
          </oc:user>
        </oc:invite>
      </d:prop>
      <d:status>HTTP/1.1 404 Not Found</d:status>
    </d:propstat>
  </d:response>""",
            )
        val collection = parser.parseCalendarCollections(parser.parseMultistatus(response))[0]

        assertTrue(collection.invites.isEmpty())
    }

    // ── share-access ──────────────────────────────────────────────────────────

    @Test
    fun `share-access shared-owner is parsed`() {
        val shareAccessBlock = "<d:share-access><d:shared-owner/></d:share-access>"
        val multistatus = parser.parseMultistatus(calendarResponse(extraProps = shareAccessBlock))
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertEquals("shared-owner", collection.shareAccess)
    }

    @Test
    fun `share-access read is parsed`() {
        val shareAccessBlock = "<d:share-access><d:read/></d:share-access>"
        val multistatus = parser.parseMultistatus(calendarResponse(extraProps = shareAccessBlock))
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertEquals("read", collection.shareAccess)
    }

    // ── calendar colour and order ─────────────────────────────────────────────

    @Test
    fun `color and order are parsed`() {
        val extra = "<i:calendar-color>#0082c9</i:calendar-color><i:calendar-order>3</i:calendar-order>"
        val multistatus = parser.parseMultistatus(calendarResponse(extraProps = extra))
        val collection = parser.parseCalendarCollections(multistatus)[0]

        assertEquals("#0082c9", collection.color)
        assertEquals(3, collection.order)
    }

    // ── principal / calendar-home discovery ───────────────────────────────────

    @Test
    fun `parsePrincipalUrl extracts current-user-principal href`() {
        val response =
            xml(
                """
  <d:response>
    <d:href>/</d:href>
    <d:propstat>
      <d:prop>
        <d:current-user-principal><d:href>/principals/users/admin</d:href></d:current-user-principal>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>""",
            )
        val multistatus = parser.parseMultistatus(response)
        val principal = parser.parsePrincipalUrl(multistatus)

        assertNotNull(principal)
        assertEquals("/principals/users/admin", principal.principalUrl)
    }

    @Test
    fun `parseCalendarHomeUrl extracts calendar-home-set href`() {
        val response =
            xml(
                """
  <d:response>
    <d:href>/principals/users/admin</d:href>
    <d:propstat>
      <d:prop>
        <d:calendar-home-set><d:href>/calendars/admin/</d:href></d:calendar-home-set>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>""",
            )
        val multistatus = parser.parseMultistatus(response)
        val home = parser.parseCalendarHomeUrl(multistatus)

        assertNotNull(home)
        assertEquals("/calendars/admin/", home.calendarHomeUrl)
    }

    // ── calendar objects ──────────────────────────────────────────────────────

    @Test
    fun `parseCalendarObjects extracts calendar data and etag`() {
        val icalData = "BEGIN:VCALENDAR\nBEGIN:VTODO\nUID:123\nEND:VTODO\nEND:VCALENDAR"
        val response =
            xml(
                """
  <d:response>
    <d:href>/calendars/admin/tasks/task1.ics</d:href>
    <d:propstat>
      <d:prop>
        <d:getetag>"etag-abc"</d:getetag>
        <c:calendar-data>$icalData</c:calendar-data>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>""",
            )
        val objects = parser.parseCalendarObjects(parser.parseMultistatus(response))

        assertEquals(1, objects.size)
        assertEquals("/calendars/admin/tasks/task1.ics", objects[0].href)
        assertEquals("etag-abc", objects[0].etag)
        assertEquals(icalData, objects[0].calendarData)
    }

    @Test
    fun `parseCalendarObjects skips entries without calendar data`() {
        val response =
            xml(
                """
  <d:response>
    <d:href>/calendars/admin/tasks/</d:href>
    <d:propstat>
      <d:prop>
        <d:getetag>"etag-abc"</d:getetag>
      </d:prop>
      <d:status>HTTP/1.1 200 OK</d:status>
    </d:propstat>
  </d:response>""",
            )
        val objects = parser.parseCalendarObjects(parser.parseMultistatus(response))
        assertTrue(objects.isEmpty())
    }
}
