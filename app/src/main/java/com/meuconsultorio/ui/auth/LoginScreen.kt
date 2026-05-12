package com.meuconsultorio.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.meuconsultorio.R
import com.meuconsultorio.viewmodel.AuthViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isCreatingAccount by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun submit() {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Preencha email e senha."
            return
        }
        if (password.length < 6) {
            errorMessage = "A senha deve ter no mínimo 6 caracteres."
            return
        }
        isLoading = true
        errorMessage = null
        val onError: (String) -> Unit = { msg ->
            errorMessage = msg
            isLoading = false
        }
        if (isCreatingAccount) {
            viewModel.signUp(email, password, onError)
        } else {
            viewModel.signIn(email, password, onError)
        }
    }

    fun signInWithGoogle() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetSignInWithGoogleOption.Builder(
                    context.getString(R.string.default_web_client_id)
                ).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()
                val result = credentialManager.getCredential(context, request)
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
                viewModel.signInWithGoogle(googleIdTokenCredential.idToken) { msg ->
                    errorMessage = msg
                    isLoading = false
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "Erro ao entrar com Google."
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp).semantics { contentDescription = "login_screen" },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Filled.MedicalServices,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Meu Consultório",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            if (isCreatingAccount) "Criar conta" else "Entrar",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it; errorMessage = null },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Filled.Email, null) },
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "campo_email" },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            singleLine = true
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it; errorMessage = null },
            label = { Text("Senha") },
            leadingIcon = { Icon(Icons.Filled.Lock, null) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (passwordVisible) "Ocultar senha" else "Ver senha"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = "campo_senha" },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus(); submit() }),
            singleLine = true
        )

        errorMessage?.let { msg ->
            Spacer(Modifier.height(8.dp))
            Text(
                msg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { submit() },
            modifier = Modifier.fillMaxWidth().height(50.dp).semantics { contentDescription = "btn_entrar" },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    if (isCreatingAccount) "Criar conta" else "Entrar",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { signInWithGoogle() },
            modifier = Modifier.fillMaxWidth().height(50.dp).semantics { contentDescription = "btn_entrar_google" },
            enabled = !isLoading
        ) {
            Icon(
                Icons.Filled.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Entrar com Google", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(12.dp))

        TextButton(onClick = {
            isCreatingAccount = !isCreatingAccount
            errorMessage = null
        }) {
            Text(
                if (isCreatingAccount) "Já tenho conta — Entrar"
                else "Não tenho conta — Criar conta"
            )
        }
    }
}
