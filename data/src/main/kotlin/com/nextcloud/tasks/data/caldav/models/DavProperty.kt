package com.nextcloud.tasks.data.caldav.models

/**
 * CalDAV/WebDAV property names and namespaces
 */
object DavProperty {
    const val DAV_NAMESPACE = "DAV:"
    const val CALDAV_NAMESPACE = "urn:ietf:params:xml:ns:caldav"
    const val ICAL_NAMESPACE = "http://apple.com/ns/ical/"
    const val NEXTCLOUD_NAMESPACE = "http://nextcloud.com/ns"

    // DAV properties
    const val CURRENT_USER_PRINCIPAL = "current-user-principal"
    const val DISPLAY_NAME = "displayname"
    const val RESOURCE_TYPE = "resourcetype"
    const val GET_ETAG = "getetag"

    // CalDAV properties
    const val CALENDAR_HOME_SET = "calendar-home-set"
    const val SUPPORTED_CALENDAR_COMPONENT_SET = "supported-calendar-component-set"
    const val CALENDAR_DATA = "calendar-data"
    const val COMP = "comp"

    // Resource types
    const val COLLECTION = "collection"
    const val CALENDAR = "calendar"

    // iCal properties
    const val CALENDAR_COLOR = "calendar-color"
    const val CALENDAR_ORDER = "calendar-order"

    // Components
    const val VTODO = "VTODO"
}
