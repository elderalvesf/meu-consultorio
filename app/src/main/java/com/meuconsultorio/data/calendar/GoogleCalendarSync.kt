package com.meuconsultorio.data.calendar

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.provider.CalendarContract
import com.meuconsultorio.data.entity.Appointment
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCalendarSync @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun findCalendarId(): Long? {
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.ACCOUNT_TYPE)
        return try {
            // Prefere conta Google, cai pro primeiro disponível se não encontrar
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?",
                arrayOf("com.google"),
                "${CalendarContract.Calendars._ID} ASC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            } ?: context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection, null, null,
                "${CalendarContract.Calendars._ID} ASC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
        } catch (e: SecurityException) {
            null
        }
    }

    fun insertEvent(appointment: Appointment, patientName: String): Long {
        val calendarId = findCalendarId() ?: return -1L
        val endMillis = appointment.dateTime + appointment.durationMinutes * 60_000L
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendarId)
            put(CalendarContract.Events.TITLE, "${appointment.procedureType} – $patientName")
            put(CalendarContract.Events.DESCRIPTION,
                appointment.notes.ifBlank { "Consulta via Meu Consultório" })
            put(CalendarContract.Events.DTSTART, appointment.dateTime)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
            put(CalendarContract.Events.HAS_ALARM, 1)
        }
        return try {
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLong() ?: -1L
        } catch (e: SecurityException) {
            -1L
        }
    }

    fun updateEvent(appointment: Appointment, calendarEventId: Long, patientName: String): Boolean {
        if (calendarEventId <= 0L) return false
        val endMillis = appointment.dateTime + appointment.durationMinutes * 60_000L
        val values = ContentValues().apply {
            put(CalendarContract.Events.TITLE, "${appointment.procedureType} – $patientName")
            put(CalendarContract.Events.DESCRIPTION,
                appointment.notes.ifBlank { "Consulta via Meu Consultório" })
            put(CalendarContract.Events.DTSTART, appointment.dateTime)
            put(CalendarContract.Events.DTEND, endMillis)
            put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
        }
        return try {
            val updateUri = ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI, calendarEventId
            )
            context.contentResolver.update(updateUri, values, null, null) > 0
        } catch (e: SecurityException) {
            false
        }
    }

    fun deleteEvent(calendarEventId: Long): Boolean {
        if (calendarEventId <= 0L) return false
        return try {
            val deleteUri = ContentUris.withAppendedId(
                CalendarContract.Events.CONTENT_URI, calendarEventId
            )
            context.contentResolver.delete(deleteUri, null, null) > 0
        } catch (e: SecurityException) {
            false
        }
    }
}
