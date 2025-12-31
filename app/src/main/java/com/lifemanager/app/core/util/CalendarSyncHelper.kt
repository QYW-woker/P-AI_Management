package com.lifemanager.app.core.util

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import com.lifemanager.app.core.database.entity.TodoEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 日历同步帮助类
 *
 * 用于将待办事项同步到系统日历，实现：
 * 1. 在日历中显示待办事项
 * 2. 利用系统日历的提醒功能
 */
@Singleton
class CalendarSyncHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val CALENDAR_NAME = "LifeManager"
        private const val CALENDAR_ACCOUNT_NAME = "lifemanager@local"
        private const val CALENDAR_ACCOUNT_TYPE = "LOCAL"
        private const val CALENDAR_DISPLAY_NAME = "生活管家"
        private const val CALENDAR_COLOR = 0xFF4CAF50.toInt() // 绿色
    }

    /**
     * 检查是否有日历权限
     */
    fun hasCalendarPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.WRITE_CALENDAR
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取需要的权限列表
     */
    fun getRequiredPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.READ_CALENDAR,
            Manifest.permission.WRITE_CALENDAR
        )
    }

    /**
     * 获取或创建应用专属日历
     */
    fun getOrCreateCalendarId(): Long? {
        if (!hasCalendarPermission()) return null

        // 先尝试查找已存在的日历
        val existingId = findExistingCalendarId()
        if (existingId != null) return existingId

        // 创建新日历
        return createCalendar()
    }

    private fun findExistingCalendarId(): Long? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME
        )

        val selection = "${CalendarContract.Calendars.ACCOUNT_NAME} = ? AND " +
                "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?"
        val selectionArgs = arrayOf(CALENDAR_ACCOUNT_NAME, CALENDAR_ACCOUNT_TYPE)

        var cursor: Cursor? = null
        try {
            cursor = context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )
            if (cursor?.moveToFirst() == true) {
                return cursor.getLong(0)
            }
        } finally {
            cursor?.close()
        }
        return null
    }

    private fun createCalendar(): Long? {
        val values = ContentValues().apply {
            put(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            put(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
            put(CalendarContract.Calendars.NAME, CALENDAR_NAME)
            put(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CALENDAR_DISPLAY_NAME)
            put(CalendarContract.Calendars.CALENDAR_COLOR, CALENDAR_COLOR)
            put(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL, CalendarContract.Calendars.CAL_ACCESS_OWNER)
            put(CalendarContract.Calendars.OWNER_ACCOUNT, CALENDAR_ACCOUNT_NAME)
            put(CalendarContract.Calendars.VISIBLE, 1)
            put(CalendarContract.Calendars.SYNC_EVENTS, 1)
            put(CalendarContract.Calendars.CALENDAR_TIME_ZONE, TimeZone.getDefault().id)
        }

        val uri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, CALENDAR_ACCOUNT_NAME)
            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, CALENDAR_ACCOUNT_TYPE)
            .build()

        return try {
            val resultUri = context.contentResolver.insert(uri, values)
            resultUri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 添加或更新日历事件
     */
    fun syncTodoToCalendar(todo: TodoEntity): Long? {
        if (!hasCalendarPermission()) return null

        val calendarId = getOrCreateCalendarId() ?: return null

        // 如果已有事件ID，先删除旧事件
        todo.calendarEventId?.let { eventId ->
            deleteCalendarEvent(eventId)
        }

        // 计算事件时间
        val dueDate = todo.dueDate ?: return null
        val localDate = LocalDate.ofEpochDay(dueDate.toLong())
        val zoneId = ZoneId.systemDefault()

        val startMillis: Long
        val endMillis: Long

        if (todo.isAllDay) {
            // 全天事件
            startMillis = localDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
            endMillis = localDate.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        } else {
            // 有具体时间的事件
            val startTime = todo.startTime?.let { parseTime(it) } ?: LocalTime.of(9, 0)
            val endTime = todo.endTime?.let { parseTime(it) } ?: startTime.plusHours(1)

            startMillis = localDate.atTime(startTime).atZone(zoneId).toInstant().toEpochMilli()
            endMillis = localDate.atTime(endTime).atZone(zoneId).toInstant().toEpochMilli()
        }

        // 创建事件
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, todo.title)
            put(CalendarContract.Events.DESCRIPTION, buildEventDescription(todo))
            put(CalendarContract.Events.DTSTART, startMillis)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.ALL_DAY, if (todo.isAllDay) 1 else 0)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            todo.location?.let { put(CalendarContract.Events.EVENT_LOCATION, it) }
        }

        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            val eventId = uri?.lastPathSegment?.toLongOrNull()

            // 添加提醒
            if (eventId != null && todo.reminderMinutesBefore > 0) {
                addReminder(eventId, todo.reminderMinutesBefore)
            }

            eventId
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 删除日历事件
     */
    fun deleteCalendarEvent(eventId: Long): Boolean {
        if (!hasCalendarPermission()) return false

        return try {
            val deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.delete(deleteUri, null, null) > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 添加提醒
     */
    private fun addReminder(eventId: Long, minutesBefore: Int) {
        val values = ContentValues().apply {
            put(CalendarContract.Reminders.EVENT_ID, eventId)
            put(CalendarContract.Reminders.MINUTES, minutesBefore)
            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
        }

        try {
            context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, values)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 解析时间字符串
     */
    private fun parseTime(timeStr: String): LocalTime? {
        return try {
            val parts = timeStr.split(":")
            LocalTime.of(parts[0].toInt(), parts.getOrElse(1) { "0" }.toInt())
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 构建事件描述
     */
    private fun buildEventDescription(todo: TodoEntity): String {
        val parts = mutableListOf<String>()

        if (todo.description.isNotBlank()) {
            parts.add(todo.description)
        }

        todo.priority.takeIf { it != "NONE" }?.let {
            val priorityText = when (it) {
                "HIGH" -> "高优先级"
                "MEDIUM" -> "中优先级"
                "LOW" -> "低优先级"
                else -> ""
            }
            if (priorityText.isNotEmpty()) parts.add("优先级: $priorityText")
        }

        todo.quadrant?.let {
            val quadrantText = when (it) {
                "IMPORTANT_URGENT" -> "重要且紧急"
                "IMPORTANT_NOT_URGENT" -> "重要不紧急"
                "NOT_IMPORTANT_URGENT" -> "不重要但紧急"
                "NOT_IMPORTANT_NOT_URGENT" -> "不重要不紧急"
                else -> ""
            }
            if (quadrantText.isNotEmpty()) parts.add("四象限: $quadrantText")
        }

        parts.add("——由生活管家创建")

        return parts.joinToString("\n")
    }

    /**
     * 查询指定日期的日历事件
     */
    fun getEventsForDate(date: LocalDate): List<CalendarEvent> {
        if (!hasCalendarPermission()) return emptyList()

        val zoneId = ZoneId.systemDefault()
        val startMillis = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
        val endMillis = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.EVENT_LOCATION
        )

        val selection = "(${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?) " +
                "OR (${CalendarContract.Events.DTEND} > ? AND ${CalendarContract.Events.DTEND} <= ?)"
        val selectionArgs = arrayOf(
            startMillis.toString(),
            endMillis.toString(),
            startMillis.toString(),
            endMillis.toString()
        )

        val events = mutableListOf<CalendarEvent>()
        var cursor: Cursor? = null

        try {
            cursor = context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                "${CalendarContract.Events.DTSTART} ASC"
            )

            while (cursor?.moveToNext() == true) {
                events.add(
                    CalendarEvent(
                        id = cursor.getLong(0),
                        title = cursor.getString(1) ?: "",
                        startTime = cursor.getLong(2),
                        endTime = cursor.getLong(3),
                        isAllDay = cursor.getInt(4) == 1,
                        location = cursor.getString(5)
                    )
                )
            }
        } finally {
            cursor?.close()
        }

        return events
    }
}

/**
 * 日历事件数据类
 */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val isAllDay: Boolean,
    val location: String?
)
