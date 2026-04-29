package com.meuconsultorio.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.meuconsultorio.data.dao.AppointmentDao
import com.meuconsultorio.data.dao.PatientDao
import com.meuconsultorio.data.dao.PaymentDao
import com.meuconsultorio.data.dao.ProntuarioDao
import com.meuconsultorio.data.dao.TreatmentDao
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.Patient
import com.meuconsultorio.data.entity.Payment
import com.meuconsultorio.data.entity.ProntuarioEntry
import com.meuconsultorio.data.entity.Treatment

@Database(
    entities = [Patient::class, Appointment::class, Treatment::class, Payment::class, ProntuarioEntry::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun paymentDao(): PaymentDao
    abstract fun prontuarioDao(): ProntuarioDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `prontuario_entries` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `patientId` INTEGER NOT NULL,
                        `appointmentId` INTEGER,
                        `text` TEXT NOT NULL DEFAULT '',
                        `imagePath` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`patientId`) REFERENCES `patients`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`appointmentId`) REFERENCES `appointments`(`id`) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_prontuario_entries_patientId` ON `prontuario_entries` (`patientId`)"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_prontuario_entries_appointmentId` ON `prontuario_entries` (`appointmentId`)"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `prontuario_entries` ADD COLUMN `imageUrl` TEXT")
            }
        }
    }
}
