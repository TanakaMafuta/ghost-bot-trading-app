package com.ghostbot.trading.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ghostbot.trading.data.remote.websocket.DerivWebSocketManager
import com.ghostbot.trading.domain.model.AccountType
import com.ghostbot.trading.domain.model.LoginCredentials
import com.ghostbot.trading.domain.usecase.AuthenticationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authenticationUseCase: AuthenticationUseCase,
    private val webSocketManager: DerivWebSocketManager
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    
    fun updateLoginId(loginId: String) {
        _uiState.update {
            it.copy(
                loginId = loginId,
                errorMessage = null
            )
        }
    }
    
    fun updatePassword(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                errorMessage = null
            )
        }
    }
    
    fun updateAccountType(accountType: AccountType) {
        _uiState.update {
            it.copy(
                accountType = accountType,
                server = if (accountType == AccountType.DEMO) "Deriv-Demo" else "Deriv-Real"
            )
        }
    }
    
    fun login() {
        viewModelScope.launch {
            try {
                val currentState = _uiState.value
                
                // Validate inputs
                if (currentState.loginId.isBlank()) {
                    _uiState.update {
                        it.copy(errorMessage = "Login ID is required")
                    }
                    return@launch
                }
                
                if (currentState.password.isBlank()) {
                    _uiState.update {
                        it.copy(errorMessage = "Password is required")
                    }
                    return@launch
                }
                
                _uiState.update { it.copy(isLoading = true) }
                
                val credentials = LoginCredentials(
                    loginId = currentState.loginId,
                    password = currentState.password,
                    accountType = currentState.accountType,
                    server = currentState.server
                )
                
                // Authenticate with backend
                val loginResult = authenticationUseCase.login(credentials)
                
                if (loginResult.isSuccess) {
                    // Connect to WebSocket
                    val connected = webSocketManager.connect()
                    
                    if (connected) {
                        // Authorize WebSocket with token (simplified - in real app, get token from login)
                        val authorized = webSocketManager.authorize(currentState.loginId)
                        
                        if (authorized) {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    isLoggedIn = true,
                                    connectionStatus = ConnectionStatus.CONNECTED,
                                    errorMessage = null
                                )
                            }
                        } else {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    connectionStatus = ConnectionStatus.AUTHORIZATION_FAILED,
                                    errorMessage = "Failed to authorize with trading server"
                                )
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                connectionStatus = ConnectionStatus.CONNECTION_FAILED,
                                errorMessage = "Failed to connect to trading server"
                            )
                        }
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = loginResult.exceptionOrNull()?.message ?: "Login failed"
                        )
                    }
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Login failed")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Login failed"
                    )
                }
            }
        }
    }
    
    fun testConnection() {
        viewModelScope.launch {
            try {
                _uiState.update {
                    it.copy(
                        isTestingConnection = true,
                        connectionStatus = ConnectionStatus.TESTING
                    )
                }
                
                val connected = webSocketManager.connect()
                
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        connectionStatus = if (connected) {
                            ConnectionStatus.TEST_SUCCESS
                        } else {
                            ConnectionStatus.TEST_FAILED
                        }
                    )
                }
                
                // Disconnect after test
                if (connected) {
                    webSocketManager.disconnect()
                }
                
            } catch (e: Exception) {
                Timber.e(e, "Connection test failed")
                _uiState.update {
                    it.copy(
                        isTestingConnection = false,
                        connectionStatus = ConnectionStatus.TEST_FAILED,
                        errorMessage = "Connection test failed: ${e.message}"
                    )
                }
            }
        }
    }
    
    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    fun clearConnectionStatus() {
        _uiState.update {
            it.copy(connectionStatus = ConnectionStatus.IDLE)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            webSocketManager.disconnect()
        }
    }
}

data class LoginUiState(
    val isLoading: Boolean = false,
    val isTestingConnection: Boolean = false,
    val isLoggedIn: Boolean = false,
    val loginId: String = "",
    val password: String = "",
    val accountType: AccountType = AccountType.DEMO,
    val server: String = "Deriv-Demo",
    val broker: String = "Deriv",
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val errorMessage: String? = null
)

enum class ConnectionStatus {
    IDLE,
    TESTING,
    TEST_SUCCESS,
    TEST_FAILED,
    CONNECTING,
    CONNECTED,
    CONNECTION_FAILED,
    AUTHORIZATION_FAILED
}