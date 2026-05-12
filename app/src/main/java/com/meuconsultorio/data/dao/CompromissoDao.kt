package com.meuconsultorio.data.dao

import androidx.room.*
import com.meuconsultorio.data.entity.Compromisso
import kotlinx.coroutines.flow.Flow

@Dao
interface CompromissoDao {

    @Query("SELECT * FROM compromissos ORDER BY date ASC")
    fun getAllCompromissos(): Flow<List<Compromisso>>

    @Query("SELECT * FROM compromissos WHERE date >= :startOfDay AND date <= :endOfDay ORDER BY date ASC")
    fun getCompromissosByDay(startOfDay: Long, endOfDay: Long): Flow<List<Compromisso>>

    @Query("SELECT * FROM compromissos WHERE id = :id")
    fun getCompromissoById(id: Long): Flow<Compromisso?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompromisso(compromisso: Compromisso): Long

    @Update
    suspend fun updateCompromisso(compromisso: Compromisso)

    @Delete
    suspend fun deleteCompromisso(compromisso: Compromisso)
}