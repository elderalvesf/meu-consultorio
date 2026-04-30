package com.meuconsultorio.data.dao

import androidx.room.*
import com.meuconsultorio.data.entity.ProntuarioEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface ProntuarioDao {

    @Query("SELECT * FROM prontuario_entries WHERE patientId = :patientId ORDER BY createdAt DESC")
    fun getEntriesByPatient(patientId: Long): Flow<List<ProntuarioEntry>>

    @Query("SELECT * FROM prontuario_entries WHERE appointmentId = :appointmentId ORDER BY createdAt DESC")
    fun getEntriesByAppointment(appointmentId: Long): Flow<List<ProntuarioEntry>>

    @Query("SELECT * FROM prontuario_entries WHERE id = :id")
    fun getEntryById(id: Long): Flow<ProntuarioEntry?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: ProntuarioEntry): Long

    @Update
    suspend fun update(entry: ProntuarioEntry)

    @Delete
    suspend fun delete(entry: ProntuarioEntry)
}
