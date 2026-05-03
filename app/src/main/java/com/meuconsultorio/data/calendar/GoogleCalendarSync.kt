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

data class CalendarInfo(val id: Long, val displayName: String)

@Singleton
class GoogleCalendarSync @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private fun findGoogleCalendar(): CalendarInfo? {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.SYNC_EVENTS,
            CalendarContract.Calendars.VISIBLE
        )
        return try {
            // Busca calendário Google com sync ativado, visível, prefere o primário
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.ACCOUNT_TYPE} = ?" +
                        " AND ${CalendarContract.Calendars.SYNC_EVENTS} = 1" +
                        " AND ${CalendarContract.Calendars.VISIBLE} = 1",
                arrayOf("com.google"),
                "${CalendarContract.Calendars.IS_PRIMARY} DESC, ${CalendarContract.Calendars._ID} ASC"
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID))
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME))
                    CalendarInfo(id, name ?: "Google Calendar")
                } else {
                    null
                }
            }
        } catch (e: SecurityException) {
            null
        }
    }

    fun insertEvent(appointment: Appointment, patientName: String): Pair<Long, String> {
        val calendar = findGoogleCalendar()
            ?: return Pair(-1L, "Nenhum calendário Google com sincronização ativa encontrado.\nVerifique se o Google Calendar está configurado no dispositivo e com sincronização ativada.")

        val endMillis = appointment.dateTime + appointment.durationMinutes * 60_000L
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, calendar.id)
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
            val eventId = uri?.lastPathSegment?.toLong() ?: -1L
            if (eventId > 0L)
                Pair(eventId, "Adicionado em \"${calendar.displayName}\"")
            else
                Pair(-1L, "Erro ao criar evento no calendário.")
        } catch (e: SecurityException) {
            Pair(-1L, "Permissão de calendário negada.")
        } catch (e: Exception) {
            Pair(-1L, "Erro inesperado: ${e.message}")
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

    data class CalendarEventSnapshot(val dtStart: Long, val dtEnd: Long)

    fun readEvent(eventId: Long): CalendarEventSnapshot? {
        if (eventId <= 0L) return null
        val projection = arrayOf(
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DELETED
        )
        return try {
            val uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventId)
            context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                if (!cursor.moveToFirst()) return null
                val deleted = cursor.getInt(cursor.getColumnIndexOrThrow(CalendarContract.Events.DELETED)) != 0
                if (deleted) return null
                CalendarEventSnapshot(
                    dtStart = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)),
                    dtEnd = cursor.getLong(cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND))
                )
            }
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }
}
