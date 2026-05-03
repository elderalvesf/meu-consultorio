package com.meuconsultorio.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.meuconsultorio.data.database.AppDatabase
import com.meuconsultorio.data.firebase.FirestoreSync
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestoreSync: FirestoreSync,
    private val db: AppDatabase
) : ViewModel() {

    sealed class AuthState {
        object Loading : AuthState()
        object Unauthenticated : AuthState()
        data class Authenticated(val email: String?) : AuthState()
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(user.email)
                viewModelScope.launch { firestoreSync.pullAll() }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    fun signIn(email: String, password: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(email.trim(), password).await()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Erro ao entrar. Verifique email e senha.")
            }
        }
    }

    fun signUp(email: String, password: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                auth.createUserWithEmailAndPassword(email.trim(), password).await()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Erro ao criar conta.")
            }
        }
    }

    fun signInWithGoogle(idToken: String, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Erro ao entrar com Google.")
            }
        }
    }

    fun signOut() {
        auth.signOut()
        viewModelScope.launch { withContext(Dispatchers.IO) { db.clearAllTables() } }
    }
}
