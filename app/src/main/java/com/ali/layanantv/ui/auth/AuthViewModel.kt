package com.ali.layanantv.ui.auth

import androidx.lifecycle.ViewModel
import com.ali.layanantv.data.model.User
import com.ali.layanantv.data.repository.AuthRepository
import com.ali.layanantv.utils.ValidationUtils
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(email: String, password: String) {
        if (!validateLoginInput(email, password)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.login(email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    isLoginSuccessful = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Login failed"
                )
            }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        if (!validateRegisterInput(name, email, password, confirmPassword)) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = authRepository.register(name, email, password)
            if (result.isSuccess) {
                val user = result.getOrNull()!!
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    user = user,
                    isRegisterSuccessful = true
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }

    private fun validateLoginInput(email: String, password: String): Boolean {
        return when {
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Email tidak boleh kosong")
                false
            }
            !ValidationUtils.isValidEmail(email) -> {
                _uiState.value = _uiState.value.copy(error = "Format email tidak valid")
                false
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Password tidak boleh kosong")
                false
            }
            else -> true
        }
    }

    private fun validateRegisterInput(name: String, email: String, password: String, confirmPassword: String): Boolean {
        return when {
            name.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Nama tidak boleh kosong")
                false
            }
            !ValidationUtils.isValidName(name) -> {
                _uiState.value = _uiState.value.copy(error = "Nama minimal 2 karakter")
                false
            }
            email.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Email tidak boleh kosong")
                false
            }
            !ValidationUtils.isValidEmail(email) -> {
                _uiState.value = _uiState.value.copy(error = "Format email tidak valid")
                false
            }
            password.isBlank() -> {
                _uiState.value = _uiState.value.copy(error = "Password tidak boleh kosong")
                false
            }
            !ValidationUtils.isValidPassword(password) -> {
                _uiState.value = _uiState.value.copy(error = "Password minimal 6 karakter")
                false
            }
            password != confirmPassword -> {
                _uiState.value = _uiState.value.copy(error = "Password tidak cocok")
                false
            }
            else -> true
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null,
    val isLoginSuccessful: Boolean = false,
    val isRegisterSuccessful: Boolean = false
)