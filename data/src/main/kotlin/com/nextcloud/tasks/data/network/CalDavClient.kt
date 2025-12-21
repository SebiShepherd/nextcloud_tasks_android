@file:Suppress("TooManyFunctions")

package com.nextcloud.tasks.data.network

import com.nextcloud.tasks.data.auth.AuthTokenProvider
import com.nextcloud.tasks.data.api.dto.TaskDto
import com.nextcloud.tasks.domain.model.Task
import com.nextcloud.tasks.domain.model.TaskDraft
import java.io.ByteArrayInputStream
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.xml.parsers.DocumentBuilderFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.internal.closeQuietly
import okio.IOException
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList

data class CalDavCalendar(
    val href: String,
    val displayName: String,
    val color: String?,
    val updatedAt: Long,
)

@Singleton
class CalDavClient
    @Inject
    constructor(
        @Named("authenticated") private val okHttpClient: OkHttpClient,
        private val authTokenProvider: AuthTokenProvider,
    ) {
        private val xmlMediaType = "application/xml; charset=utf-8".toMediaType()
        private val icalMediaType = "text/calendar; charset=utf-8".toMediaType()
        private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmssX")
        private val dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")

        suspend fun fetchCalendars(): List<CalDavCalendar> {
            val (serverUrl, username) = activeEndpoint()
            val url = normalizeBase(serverUrl) + "remote.php/dav/calendars/$username/"
            val request =
                Request
                    .Builder()
                    .url(url)
                    .method("PROPFIND", propfindBody().toRequestBody(xmlMediaType))
                    .header("Depth", "1")
                    .header("Content-Type", "application/xml; charset=utf-8")
                    .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("PROPFIND calendars failed: ${response.code}")
                val body = response.body?.string() ?: return emptyList()
                return parseCalendars(body)
            }
        }

        suspend fun fetchTasks(calendar: CalDavCalendar): List<TaskDto> {
            val (serverUrl, _) = activeEndpoint()
            val calendarUrl = absoluteUrl(serverUrl, calendar.href)
            val request =
                Request
                    .Builder()
                    .url(calendarUrl)
                    .method("REPORT", reportBody().toRequestBody(xmlMediaType))
                    .header("Depth", "1")
                    .header("Content-Type", "application/xml; charset=utf-8")
                    .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("REPORT tasks failed: ${response.code}")
                val body = response.body?.string() ?: return emptyList()
                return parseTasks(body, calendar.href)
            }
        }

        suspend fun createTask(calendar: CalDavCalendar, draft: TaskDraft): TaskDto {
            val (serverUrl, _) = activeEndpoint()
            val calendarUrl = absoluteUrl(serverUrl, calendar.href)
            val uid = UUID.randomUUID().toString()
            val resourceHref = buildResourceHref(calendar.href, uid)
            val putUrl = absoluteUrl(serverUrl, resourceHref)
            val now = Instant.now()
            val ical = buildICal(uid, draft.title, draft.description, draft.completed, draft.priority, draft.due, draft.tagIds)

            val request =
                Request
                    .Builder()
                    .url(putUrl)
                    .put(ical.toRequestBody(icalMediaType))
                    .header("If-None-Match", "*")
                    .header("Content-Type", "text/calendar; charset=utf-8")
                    .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("PUT VTODO failed: ${response.code}")
            }

            return TaskDto(
                id = resourceHref,
                listId = calendar.href,
                title = draft.title,
                description = draft.description,
                completed = draft.completed,
                priority = draft.priority,
                due = draft.due?.toEpochMilli(),
                updatedAt = now.toEpochMilli(),
                tagIds = draft.tagIds,
            )
        }

        suspend fun updateTask(task: Task): TaskDto {
            val (serverUrl, _) = activeEndpoint()
            val resourceUrl = absoluteUrl(serverUrl, task.id)
            val uid = taskUidFromHref(task.id)
            val ical =
                buildICal(
                    uid = uid,
                    title = task.title,
                    description = task.description,
                    completed = task.completed,
                    priority = task.priority,
                    due = task.due,
                    tags = task.tags.map { it.id },
                )
            val request =
                Request
                    .Builder()
                    .url(resourceUrl)
                    .put(ical.toRequestBody(icalMediaType))
                    .header("Content-Type", "text/calendar; charset=utf-8")
                    .build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("PUT VTODO failed: ${response.code}")
            }
            return TaskDto(
                id = task.id,
                listId = task.listId,
                title = task.title,
                description = task.description,
                completed = task.completed,
                priority = task.priority,
                due = task.due?.toEpochMilli(),
                updatedAt = Instant.now().toEpochMilli(),
                tagIds = task.tags.map { it.id },
            )
        }

        suspend fun deleteTask(taskId: String) {
            val (serverUrl, _) = activeEndpoint()
            val url = absoluteUrl(serverUrl, taskId)
            val request = Request.Builder().url(url).delete().build()
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("DELETE VTODO failed: ${response.code}")
            }
        }

        private fun activeEndpoint(): Pair<String, String> {
            val serverUrl =
                requireNotNull(authTokenProvider.activeServerUrl()) {
                    "Active server URL is missing; please log in."
                }
            val username =
                requireNotNull(authTokenProvider.activeUsername()) {
                    "Active username is missing; please log in."
                }
            return serverUrl to username
        }

        private fun propfindBody(): String =
            """
            <d:propfind xmlns:d="DAV:" xmlns:cal="urn:ietf:params:xml:ns:caldav">
              <d:prop>
                <d:displayname />
                <cal:supported-calendar-component-set />
                <cal:calendar-color />
                <d:resourcetype />
              </d:prop>
            </d:propfind>
            """.trimIndent()

        private fun reportBody(): String =
            """
            <c:calendar-query xmlns:c="urn:ietf:params:xml:ns:caldav" xmlns:d="DAV:">
              <d:prop>
                <d:getetag />
                <c:calendar-data />
              </d:prop>
              <c:filter>
                <c:comp-filter name="VCALENDAR">
                  <c:comp-filter name="VTODO" />
                </c:comp-filter>
              </c:filter>
            </c:calendar-query>
            """.trimIndent()

        private fun parseCalendars(xml: String): List<CalDavCalendar> {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xml.toByteArray()))
            val responses = doc.getElementsByTagNameNS("DAV:", "response")
            val calendars = mutableListOf<CalDavCalendar>()
            for (i in 0 until responses.length) {
                val response = responses.item(i) as? Element ?: continue
                val href = response.getElementsByTagNameNS("DAV:", "href").firstText() ?: continue
                if (href.trimEnd('/').endsWith("/calendars")) continue
                val supportedSet = response.getElementsByTagNameNS("urn:ietf:params:xml:ns:caldav", "supported-calendar-component-set")
                val hasVtodo = supportedSet.any { it.textContent.contains("VTODO", ignoreCase = true) }
                if (!hasVtodo) continue
                val displayName = response.getElementsByTagNameNS("DAV:", "displayname").firstText() ?: href.substringAfterLast('/').trimEnd('/')
                val color = response.getElementsByTagNameNS("urn:ietf:params:xml:ns:caldav", "calendar-color").firstText()
                calendars += CalDavCalendar(
                    href = href,
                    displayName = displayName,
                    color = color,
                    updatedAt = Instant.now().toEpochMilli(),
                )
            }
            return calendars
        }

        private fun parseTasks(xml: String, calendarHref: String): List<TaskDto> {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xml.toByteArray()))
            val responses = doc.getElementsByTagNameNS("DAV:", "response")
            val tasks = mutableListOf<TaskDto>()
            for (i in 0 until responses.length) {
                val response = responses.item(i) as? Element ?: continue
                val href = response.getElementsByTagNameNS("DAV:", "href").firstText() ?: continue
                val calendarData =
                    response
                        .getElementsByTagNameNS("urn:ietf:params:xml:ns:caldav", "calendar-data")
                        .firstText()
                        ?: continue
                parseVtodo(calendarData, href, calendarHref)?.let(tasks::add)
            }
            return tasks
        }

        private fun parseVtodo(
            ical: String,
            href: String,
            calendarHref: String,
        ): TaskDto? {
            val unfolded = unfoldIcs(ical)
            var inVtodo = false
            var summary: String? = null
            var description: String? = null
            var status: String? = null
            var priority: Int = 0
            var due: Instant? = null
            var lastModified: Instant? = null
            var categories: List<String> = emptyList()
            var uid: String? = null
            unfolded.forEach { line ->
                when {
                    line.startsWith("BEGIN:VTODO") -> inVtodo = true
                    line.startsWith("END:VTODO") -> inVtodo = false
                    inVtodo -> {
                        when {
                            line.startsWith("UID:", ignoreCase = true) -> uid = line.substringAfter("UID:", "")
                            line.startsWith("SUMMARY:", ignoreCase = true) -> summary = line.substringAfter("SUMMARY:", "")
                            line.startsWith("DESCRIPTION:", ignoreCase = true) -> description = line.substringAfter("DESCRIPTION:", "")
                            line.startsWith("STATUS:", ignoreCase = true) -> status = line.substringAfter("STATUS:", "")
                            line.startsWith("PRIORITY:", ignoreCase = true) -> priority = line.substringAfter("PRIORITY:", "0").toIntOrNull() ?: 0
                            line.startsWith("DUE", ignoreCase = true) -> due = parseDue(line)
                            line.startsWith("LAST-MODIFIED:", ignoreCase = true) -> lastModified = parseDateTime(line.substringAfter("LAST-MODIFIED:", ""))
                            line.startsWith("DTSTAMP:", ignoreCase = true) -> if (lastModified == null) lastModified = parseDateTime(line.substringAfter("DTSTAMP:", ""))
                            line.startsWith("CATEGORIES:", ignoreCase = true) -> categories = line.substringAfter("CATEGORIES:", "").split(',').map { it.trim() }.filter { it.isNotBlank() }
                        }
                    }
                }
            }
            val title = summary ?: return null
            val updated = lastModified ?: Instant.now()
            val resourceId = href
            return TaskDto(
                id = resourceId,
                listId = calendarHref,
                title = title,
                description = description,
                completed = status.equals("COMPLETED", ignoreCase = true),
                priority = priority,
                due = due?.toEpochMilli(),
                updatedAt = updated.toEpochMilli(),
                tagIds = categories,
            )
        }

        private fun unfoldIcs(ical: String): List<String> {
            val lines = ical.lines()
            val unfolded = mutableListOf<String>()
            lines.forEach { line ->
                if (line.startsWith(" ") || line.startsWith("\t")) {
                    val last = unfolded.removeLastOrNull()
                    unfolded += (last ?: "") + line.trimStart()
                } else {
                    unfolded += line
                }
            }
            return unfolded
        }

        private fun parseDue(line: String): Instant? {
            val value = line.substringAfter(':', "")
            val cleaned = value.substringAfter('=' , value)
            return parseDateTime(cleaned)
        }

        private fun parseDateTime(value: String): Instant? {
            return runCatching { ZonedDateTime.parse(value, dateTimeFormatter).toInstant() }.getOrNull()
                ?: runCatching { LocalDateTime.parse(value, dateTimeFormatter).atZone(ZoneId.systemDefault()).toInstant() }.getOrNull()
                ?: runCatching { LocalDate.parse(value, dateFormatter).atStartOfDay(ZoneId.systemDefault()).toInstant() }.getOrNull()
        }

        private fun buildICal(
            uid: String,
            title: String,
            description: String?,
            completed: Boolean,
            priority: Int,
            due: Instant?,
            tags: List<String>,
        ): String {
            val status = if (completed) "COMPLETED" else "NEEDS-ACTION"
            val utcZone = ZoneId.of("UTC")
            val builder =
                StringBuilder()
                    .appendLine("BEGIN:VCALENDAR")
                    .appendLine("VERSION:2.0")
                    .appendLine("PRODID:-//Nextcloud Tasks Android//EN")
                    .appendLine("BEGIN:VTODO")
                    .appendLine("UID:$uid")
                    .appendLine("SUMMARY:$title")
                    .appendLine("STATUS:$status")
            if (description != null) {
                builder.appendLine("DESCRIPTION:$description")
            }
            builder.appendLine("PRIORITY:$priority")
            due?.let { builder.appendLine("DUE:${dateTimeFormatter.format(it.atZone(utcZone))}") }
            builder.appendLine("DTSTAMP:${dateTimeFormatter.format(Instant.now().atZone(utcZone))}")
            if (tags.isNotEmpty()) {
                builder.appendLine("CATEGORIES:${tags.joinToString(",")}")
            }
            builder.appendLine("END:VTODO")
            builder.appendLine("END:VCALENDAR")
            return builder.toString()
        }

        private fun absoluteUrl(
            serverUrl: String,
            href: String,
        ): String {
            if (href.startsWith("http")) return href
            val base = normalizeBase(serverUrl)
            return if (href.startsWith("/")) "$base${href.removePrefix("/")}" else "$base$href"
        }

        private fun normalizeBase(serverUrl: String): String = serverUrl.trimEnd('/') + "/"

        private fun buildResourceHref(
            calendarHref: String,
            uid: String,
        ): String {
            val base = calendarHref.trimEnd('/')
            return "$base/$uid.ics"
        }

        private fun taskUidFromHref(href: String): String = href.substringAfterLast('/').removeSuffix(".ics")
    }

private fun NodeList.firstText(): String? = (0 until length)
    .asSequence()
    .mapNotNull { item(it)?.textContent }
    .firstOrNull { it.isNotBlank() }

private fun NodeList.any(predicate: (Node) -> Boolean): Boolean {
    for (i in 0 until length) {
        if (predicate(item(i))) return true
    }
    return false
}
