package com.meuconsultorio.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.meuconsultorio.data.dao.AppointmentDao
import com.meuconsultorio.data.dao.PatientDao
import com.meuconsultorio.data.dao.PaymentDao
import com.meuconsultorio.data.dao.TreatmentDao
import com.meuconsultorio.data.entity.Appointment
import com.meuconsultorio.data.entity.Patient
import com.meuconsultorio.data.entity.Payment
import com.meuconsultorio.data.entity.Treatment

@Database(
    entities = [Patient::class, Appointment::class, Treatment::class, Payment::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun patientDao(): PatientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun treatmentDao(): TreatmentDao
    abstract fun paymentDao(): PaymentDao
}
