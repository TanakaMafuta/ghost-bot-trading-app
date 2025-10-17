package com.ghostbot.trading.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostbot.trading.domain.usecase.AuthenticationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SecurityPinViewModel @Inject constructor(
    private val authenticationUseCase: AuthenticationUseCase
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SecurityPinUiState())
    val uiState: StateFlow<SecurityPinUiState> = _uiState.asStateFlow()
    
    init {
        checkPinSetup()
    }
    
    private fun checkPinSetup() {
        viewModelScope.launch {
            try {
                val isPinSet = authenticationUseCase.isPinSet()
                val isLockedOut = authenticationUseCase.isLockedOut()
                val failedAttempts = authenticationUseCase.getFailedAttempts()
                val lockoutEndTime = authenticationUseCase.getLockoutEndTime()
                
                _uiState.update {
                    it.copy(
                        isPinSet = isPinSet,
                        isLockedOut = isLockedOut,
                        failedAttempts = failedAttempts,
                        lockoutEndTime = lockoutEndTime,
                        isFirstTime = !isPinSet
                    )
                }
                
                if (isLockedOut) {
                    startLockoutTimer()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to check PIN setup")
                _uiState.update {
                    it.copy(errorMessage = "Failed to initialize security: ${e.message}")
                }
            }
        }
    }
    
    fun onPinEntered(pin: String) {
        if (_uiState.value.isLockedOut) {
            _uiState.update {
                it.copy(errorMessage = "Account is locked. Please wait.")
            }
            return
        }
        
        if (_uiState.value.isFirstTime) {
            setNewPin(pin)
        } else {
            verifyPin(pin)
        }
    }
    
    private fun setNewPin(pin: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = authenticationUseCase.setPin(pin)
                
                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isPinSet = true,
                            isFirstTime = false,
                            isVerified = true,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "Failed to set PIN"
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to set PIN")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to set PIN"
                    )
                }
            }
        }
    }
    
    private fun verifyPin(pin: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                
                val result = authenticationUseCase.verifyPin(pin)
                
                if (result.isSuccess) {
                    val isValid = result.getOrNull() ?: false
                    
                    if (isValid) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isVerified = true,
                                failedAttempts = 0,
                                errorMessage = null
                            )
                        }
                    } else {
                        val failedAttempts = authenticationUseCase.getFailedAttempts()
                        val isLockedOut = authenticationUseCase.isLockedOut()
                        
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                failedAttempts = failedAttempts,
                                isLockedOut = isLockedOut,
                                errorMessage = "Incorrect PIN. ${3 - failedAttempts} attempts remaining."
                            )
                        }
                        
                        if (isLockedOut) {
                            startLockoutTimer()
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.exceptionOrNull()?.message ?: "PIN verification failed"
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Failed to verify PIN")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "PIN verification failed"
                    )
                }
            }
        }
    }
    
    fun onPinChanged(pin: String) {
        _uiState.update {
            it.copy(
                currentPin = pin,
                errorMessage = null
            )
        }
    }
    
    fun clearPin() {
        _uiState.update {
            it.copy(currentPin = "")
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    private fun startLockoutTimer() {
        viewModelScope.launch {
            val lockoutEndTime = authenticationUseCase.getLockoutEndTime()
            
            while (System.currentTimeMillis() < lockoutEndTime) {
                val remainingTime = lockoutEndTime - System.currentTimeMillis()
                _uiState.update {
                    it.copy(
                        isLockedOut = true,
                        lockoutRemainingTime = remainingTime
                    )
                }
                kotlinx.coroutines.delay(1000) // Update every second
            }
            
            // Lockout ended
            _uiState.update {
                it.copy(
                    isLockedOut = false,
                    lockoutRemainingTime = 0,
                    failedAttempts = 0,
                    errorMessage = null
                )
            }
        }
    }
}

data class SecurityPinUiState(
    val isLoading: Boolean = false,
    val isPinSet: Boolean = false,
    val isFirstTime: Boolean = true,
    val isVerified: Boolean = false,
    val isLockedOut: Boolean = false,
    val currentPin: String = "",
    val failedAttempts: Int = 0,
    val lockoutEndTime: Long = 0,
    val lockoutRemainingTime: Long = 0,
    val errorMessage: String? = null
)