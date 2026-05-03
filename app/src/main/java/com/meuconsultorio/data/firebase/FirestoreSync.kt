package com.meuconsultorio.data.firebase

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.meuconsultorio.data.dao.*
import com.meuconsultorio.data.entity.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirestoreSync @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth,
    private val patientDao: PatientDao,
    private val appointmentDao: AppointmentDao,
    private val treatmentDao: TreatmentDao,
    private val paymentDao: PaymentDao,
    private val prontuarioDao: ProntuarioDao
) {
    private val uid get() = auth.currentUser?.uid
    private val isLoggedIn get() = uid != null

    private fun userDoc() = firestore.collection("users").document(uid!!)
    private val colPatients get() = userDoc().collection("patients")
    private val colAppointments get() = userDoc().collection("appointments")
    private val colTreatments get() = userDoc().collection("treatments")
    private val colPayments get() = userDoc().collection("payments")
    private val colProntuario get() = userDoc().collection("prontuario_entries")

    fun pushPatient(patient: Patient) {
        if (!isLoggedIn) return
        colPatients.document(patient.id.toString()).set(mapOf(
            "name" to patient.name,
            "cpf" to patient.cpf,
            "phone" to patient.phone,
            "email" to patient.email,
            "birthDate" to patient.birthDate,
            "address" to patient.address,
            "notes" to patient.notes,
            "createdAt" to patient.createdAt
        ))
    }

    fun pushAppointment(appointment: Appointment) {
        if (!isLoggedIn) return
        colAppointments.document(appointment.id.toString()).set(mapOf(
            "patientId" to appointment.patientId,
            "dateTime" to appointment.dateTime,
            "durationMinutes" to appointment.durationMinutes,
            "procedureType" to appointment.procedureType,
            "status" to appointment.status.name,
            "notes" to appointment.notes,
            "createdAt" to appointment.createdAt
        ))
    }

    fun pushTreatment(treatment: Treatment) {
        if (!isLoggedIn) return
        colTreatments.document(treatment.id.toString()).set(mapOf(
            "patientId" to treatment.patientId,
            "procedure" to treatment.procedure,
            "tooth" to treatment.tooth,
            "description" to treatment.description,
            "cost" to treatment.cost,
            "price" to treatment.price,
            "date" to treatment.date,
            "status" to treatment.status.name
        ))
    }

    fun pushPayment(payment: Payment) {
        if (!isLoggedIn) return
        colPayments.document(payment.id.toString()).set(mapOf(
            "patientId" to payment.patientId,
            "description" to payment.description,
            "amount" to payment.amount,
            "method" to payment.method.name,
            "date" to payment.date,
            "notes" to payment.notes,
            "isPaid" to payment.isPaid
        ))
    }

    fun pushProntuarioEntry(entry: ProntuarioEntry) {
        if (!isLoggedIn) return
        colProntuario.document(entry.id.toString()).set(mapOf(
            "patientId" to entry.patientId,
            "appointmentId" to entry.appointmentId,
            "text" to entry.text,
            "imageUrl" to entry.imageUrl,
            "createdAt" to entry.createdAt
        ))
    }

    fun deletePatient(id: Long) { if (isLoggedIn) colPatients.document(id.toString()).delete() }
    fun deleteAppointment(id: Long) { if (isLoggedIn) colAppointments.document(id.toString()).delete() }
    fun deleteTreatment(id: Long) { if (isLoggedIn) colTreatments.document(id.toString()).delete() }
    fun deletePayment(id: Long) { if (isLoggedIn) colPayments.document(id.toString()).delete() }

    fun deleteProntuarioEntry(id: Long) {
        if (!isLoggedIn) return
        colProntuario.document(id.toString()).delete()
        storage.reference.child("prontuario/$id.jpg").delete()
    }

    suspend fun uploadProntuarioImage(entryId: Long, localPath: String): String? {
        if (!isLoggedIn) return null
        return try {
            val file = File(localPath)
            if (!file.exists()) return null
            val ref = storage.reference.child("prontuario/$entryId.jpg")
            ref.putFile(Uri.fromFile(file)).await()
            val url = ref.downloadUrl.await().toString()
            colProntuario.document(entryId.toString()).update("imageUrl", url)
            url
        } catch (e: Exception) {
            null
        }
    }

    suspend fun pullAll() {
        if (!isLoggedIn) return
        withContext(Dispatchers.IO) {
            try {
                colPatients.get().await().forEach { doc ->
                    doc.toPatient()?.let { patientDao.insertPatient(it) }
                }
            } catch (_: Exception) {}

            try {
                colAppointments.get().await().forEach { doc ->
                    doc.toAppointment()?.let { appointmentDao.insertAppointment(it) }
                }
            } catch (_: Exception) {}

            try {
                colTreatments.get().await().forEach { doc ->
                    doc.toTreatment()?.let { treatmentDao.insertTreatment(it) }
                }
            } catch (_: Exception) {}

            try {
                colPayments.get().await().forEach { doc ->
                    doc.toPayment()?.let { paymentDao.insertPayment(it) }
                }
            } catch (_: Exception) {}

            try {
                colProntuario.get().await().forEach { doc ->
                    doc.toProntuarioEntry()?.let { prontuarioDao.insert(it) }
                }
            } catch (_: Exception) {}
        }
    }

    private fun DocumentSnapshot.toPatient(): Patient? {
        val id = id.toLongOrNull() ?: return null
        return Patient(
            id = id,
            name = getString("name") ?: return null,
            cpf = getString("cpf") ?: "",
            phone = getString("phone") ?: "",
            email = getString("email") ?: "",
            birthDate = getString("birthDate") ?: "",
            address = getString("address") ?: "",
            notes = getString("notes") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toAppointment(): Appointment? {
        val id = id.toLongOrNull() ?: return null
        return Appointment(
            id = id,
            patientId = getLong("patientId") ?: return null,
            dateTime = getLong("dateTime") ?: return null,
            durationMinutes = getLong("durationMinutes")?.toInt() ?: 60,
            procedureType = getString("procedureType") ?: return null,
            status = try {
                AppointmentStatus.valueOf(getString("status") ?: "")
            } catch (_: Exception) { AppointmentStatus.AGENDADA },
            notes = getString("notes") ?: "",
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }

    private fun DocumentSnapshot.toTreatment(): Treatment? {
        val id = id.toLongOrNull() ?: return null
        return Treatment(
            id = id,
            patientId = getLong("patientId") ?: return null,
            procedure = getString("procedure") ?: return null,
            tooth = getString("tooth") ?: "",
            description = getString("description") ?: "",
            cost = getDouble("cost") ?: 0.0,
            price = getDouble("price") ?: 0.0,
            date = getLong("date") ?: System.currentTimeMillis(),
            status = try {
                TreatmentStatus.valueOf(getString("status") ?: "")
            } catch (_: Exception) { TreatmentStatus.EM_ANDAMENTO }
        )
    }

    private fun DocumentSnapshot.toPayment(): Payment? {
        val id = id.toLongOrNull() ?: return null
        return Payment(
            id = id,
            patientId = getLong("patientId") ?: return null,
            description = getString("description") ?: return null,
            amount = getDouble("amount") ?: return null,
            method = try {
                PaymentMethod.valueOf(getString("method") ?: "")
            } catch (_: Exception) { PaymentMethod.PIX },
            date = getLong("date") ?: System.currentTimeMillis(),
            notes = getString("notes") ?: "",
            isPaid = getBoolean("isPaid") ?: true
        )
    }

    private fun DocumentSnapshot.toProntuarioEntry(): ProntuarioEntry? {
        val id = id.toLongOrNull() ?: return null
        return ProntuarioEntry(
            id = id,
            patientId = getLong("patientId") ?: return null,
            appointmentId = getLong("appointmentId"),
            text = getString("text") ?: "",
            imagePath = null,
            imageUrl = getString("imageUrl"),
            createdAt = getLong("createdAt") ?: System.currentTimeMillis()
        )
    }
}
