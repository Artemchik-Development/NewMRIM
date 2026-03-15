package org.artemchik.newmrim.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import org.artemchik.newmrim.data.SettingsDataStore
import org.artemchik.newmrim.protocol.MrimClient
import org.artemchik.newmrim.protocol.data.ServerConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.artemchik.newmrim.R
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val isChecking: Boolean = true, // Состояние первоначальной проверки
    val error: String? = null,
    val isLoggedIn: Boolean = false,
    val rememberPassword: Boolean = false,
    val serverHost: String = ServerConfig.DEFAULT_HOST,
    val serverPort: String = ServerConfig.DEFAULT_PORT.toString(),
    val useRedirector: Boolean = true,
    val avatarHost: String = ServerConfig.DEFAULT_AVATAR_HOST,
    val showServerSettings: Boolean = false
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val mrimClient: MrimClient,
    private val settingsStore: SettingsDataStore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // 1. Слушаем состояние подключения
        viewModelScope.launch {
            mrimClient.connectionState.collect { state ->
                Log.d("LoginViewModel", "Connection state: $state")
                when (state) {
                    is MrimClient.ConnectionState.LoggedIn ->
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = true, isChecking = false) }
                    is MrimClient.ConnectionState.Error ->
                        _uiState.update { it.copy(isLoading = false, error = state.message, isChecking = false) }
                    is MrimClient.ConnectionState.Connecting,
                    is MrimClient.ConnectionState.LoggingIn ->
                        _uiState.update { it.copy(isLoading = true, error = null) }
                    is MrimClient.ConnectionState.Disconnected ->
                        _uiState.update { it.copy(isLoading = false, isLoggedIn = false) }
                    else -> {}
                }
            }
        }

        // 2. Загружаем настройки и проверяем необходимость автовхода
        viewModelScope.launch {
            val configFlow = settingsStore.serverConfig.first()
            val emailFlow = settingsStore.lastEmail.first()
            val rememberFlow = settingsStore.rememberPassword.first()
            val passwordFlow = settingsStore.savedPassword.first()
            val autoConnectFlow = settingsStore.autoConnect.first()

            _uiState.update {
                it.copy(
                    serverHost = configFlow.host,
                    serverPort = configFlow.port.toString(),
                    useRedirector = configFlow.useRedirector,
                    avatarHost = configFlow.avatarHost,
                    email = emailFlow,
                    rememberPassword = rememberFlow,
                    password = if (rememberFlow) passwordFlow else ""
                )
            }

            // Если уже залогинены — просто убираем экран проверки
            if (mrimClient.connectionState.value is MrimClient.ConnectionState.LoggedIn) {
                _uiState.update { it.copy(isLoggedIn = true, isChecking = false) }
            } else if (autoConnectFlow && rememberFlow && emailFlow.isNotBlank() && passwordFlow.isNotBlank()) {
                // Пытаемся войти автоматически только если включено автоподключение
                login()
            } else {
                // Нет данных для входа или автоподключение выключено — показываем форму
                _uiState.update { it.copy(isChecking = false) }
            }
        }
    }

    fun onEmailChanged(v: String) = _uiState.update { it.copy(email = v, error = null) }
    fun onPasswordChanged(v: String) = _uiState.update { it.copy(password = v, error = null) }
    fun onRememberPasswordChanged(v: Boolean) = _uiState.update { it.copy(rememberPassword = v) }
    fun toggleServerSettings() = _uiState.update { it.copy(showServerSettings = !it.showServerSettings) }
    fun onServerHostChanged(v: String) = _uiState.update { it.copy(serverHost = v) }
    fun onServerPortChanged(v: String) = _uiState.update { it.copy(serverPort = v.filter { c -> c.isDigit() }) }
    fun onUseRedirectorChanged(v: Boolean) = _uiState.update {
        it.copy(useRedirector = v, serverPort = if (v) ServerConfig.DEFAULT_PORT.toString() else ServerConfig.DEFAULT_DIRECT_PORT.toString())
    }
    fun onAvatarHostChanged(v: String) = _uiState.update { it.copy(avatarHost = v) }
    fun resetToDefaults() = _uiState.update {
        it.copy(serverHost = ServerConfig.DEFAULT_HOST, serverPort = ServerConfig.DEFAULT_PORT.toString(),
            useRedirector = true, avatarHost = ServerConfig.DEFAULT_AVATAR_HOST)
    }
    fun clearError() = _uiState.update { it.copy(error = null) }

    fun login() {
        val s = _uiState.value
        
        // Если уже в процессе — не мешаем
        if (mrimClient.connectionState.value is MrimClient.ConnectionState.Connecting ||
            mrimClient.connectionState.value is MrimClient.ConnectionState.LoggingIn) {
            return
        }

        if (s.email.isBlank() || s.password.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_fill_login_password), isChecking = false) }
            return
        }
        val port = s.serverPort.toIntOrNull()
        if (port == null || port !in 1..65535) {
            _uiState.update { it.copy(error = context.getString(R.string.error_invalid_port), isChecking = false) }
            return
        }
        if (s.serverHost.isBlank()) {
            _uiState.update { it.copy(error = context.getString(R.string.error_empty_host), isChecking = false) }
            return
        }

        val config = ServerConfig(s.serverHost.trim(), port, s.useRedirector,
            s.avatarHost.trim().ifEmpty { ServerConfig.DEFAULT_AVATAR_HOST })

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            settingsStore.saveServerConfig(config)
            settingsStore.saveCredentials(s.email.trim(), s.password, s.rememberPassword)
            mrimClient.login(s.email.trim(), s.password, config)
        }
    }
}
