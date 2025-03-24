package com.williamfq.xhat.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.williamfq.xhat.data.UserPreferencesRepository
import com.williamfq.xhat.ui.screens.settings.model.SettingsGroup
import com.williamfq.xhat.ui.screens.settings.model.SettingItem
import com.williamfq.xhat.ui.screens.settings.model.SubSettingItem
import com.williamfq.xhat.utils.CrashReporter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val crashReporter: CrashReporter,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _settingsGroups = MutableStateFlow<List<SettingsGroup>>(emptyList())
    val settingsGroups: StateFlow<List<SettingsGroup>> = _settingsGroups.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _expandedGroupTitle = MutableStateFlow<String?>(null)
    val expandedGroupTitle: StateFlow<String?> = _expandedGroupTitle.asStateFlow()

    val crashReportingEnabled: StateFlow<Boolean> = userPreferencesRepository.crashReportingEnabledFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    init {
        initializeSettings()
    }

    private fun initializeSettings() {
        viewModelScope.launch {
            try {
                val isCrashReportingEnabled = userPreferencesRepository.isCrashReportingEnabled()
                crashReporter.enableCrashReporting(isCrashReportingEnabled)
                _settingsGroups.value = createSettingsGroups()
                _uiState.update { it.copy(isLoading = false) }
                Timber.tag(TAG).d("Configuraciones inicializadas correctamente")
            } catch (e: Exception) {
                val errorMessage = "Fallo al inicializar las configuraciones: ${e.localizedMessage}"
                _uiState.update { it.copy(error = errorMessage) }
                Timber.tag(TAG).e(e, errorMessage)
            }
        }
    }

    fun refreshSettings() {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true) }
                _settingsGroups.value = createSettingsGroups()
                _uiState.update { it.copy(isLoading = false) }
                Timber.tag(TAG).d("Configuraciones refrescadas exitosamente")
            } catch (e: Exception) {
                val errorMessage = "Error al refrescar configuraciones: ${e.localizedMessage}"
                _uiState.update { it.copy(error = errorMessage, isLoading = false) }
                Timber.tag(TAG).e(e, errorMessage)
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        filterGroups()
    }

    fun onGroupExpand(groupTitle: String) {
        _expandedGroupTitle.value = if (_expandedGroupTitle.value == groupTitle) null else groupTitle
        Timber.tag(TAG).d("Grupo ${if (_expandedGroupTitle.value == null) "colapsado" else "expandido"}: $groupTitle")
    }

    private fun filterGroups() {
        viewModelScope.launch {
            try {
                val query = _searchQuery.value
                if (query.isEmpty()) {
                    _settingsGroups.value = createSettingsGroups()
                } else {
                    val filteredGroups = _settingsGroups.value.map { group ->
                        group.copy(
                            items = group.items.filter { item ->
                                item.title.contains(query, ignoreCase = true) ||
                                        item.subItems.any { it.title.contains(query, ignoreCase = true) }
                            }
                        )
                    }.filter { it.items.isNotEmpty() }
                    _settingsGroups.value = filteredGroups
                }
                Timber.tag(TAG).d("Grupos filtrados con consulta: '$query'")
            } catch (e: Exception) {
                val errorMessage = "Fallo al filtrar grupos de configuración: ${e.localizedMessage}"
                _uiState.update { it.copy(error = errorMessage) }
                Timber.tag(TAG).e(e, errorMessage)
            }
        }
    }

    private fun createSettingsGroups(): List<SettingsGroup> = listOf(
        SettingsGroup(
            title = "Cuenta y Perfil",
            items = listOf(
                SettingItem(
                    title = "Perfil",
                    icon = Icons.Default.Person,
                    route = "settings/profile",
                    subItems = listOf(
                        SubSettingItem("Foto de perfil", "settings/profile/photo"),
                        SubSettingItem("Información personal", "settings/profile/info"),
                        SubSettingItem("Estado y humor", "settings/profile/status"),
                        SubSettingItem("Enlaces y redes sociales", "settings/profile/links"),
                        SubSettingItem("Insignias y logros", "settings/profile/badges"),
                        SubSettingItem("Configuración profesional", "settings/profile/professional")
                    )
                ),
                SettingItem(
                    title = "Privacidad",
                    icon = Icons.Default.Lock,
                    route = "settings/privacy",
                    subItems = listOf(
                        SubSettingItem("Reportar crashes", "settings/privacy/crash_reporting"),
                        SubSettingItem("Bloqueo por huella", "settings/privacy/fingerprint")
                    )
                )
            )
        )
    )

    fun onSettingItemClick(route: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(lastClickedRoute = route) }
                when (route) {
                    "settings/privacy/crash_reporting" -> toggleCrashReporting()
                    else -> Timber.tag(TAG).d("Navegación iniciada a ruta: $route")
                }
            } catch (e: Exception) {
                val errorMessage = "Error al procesar clic en configuración: ${e.localizedMessage}"
                _uiState.update { it.copy(error = errorMessage) }
                Timber.tag(TAG).e(e, errorMessage)
            }
        }
    }

    private fun toggleCrashReporting() {
        viewModelScope.launch {
            try {
                val currentEnabled = userPreferencesRepository.isCrashReportingEnabled()
                val newEnabled = !currentEnabled
                userPreferencesRepository.setCrashReportingEnabled(newEnabled)
                crashReporter.enableCrashReporting(newEnabled)
                Timber.tag(TAG).d("Reporte de crashes ${if (newEnabled) "activado" else "desactivado"}")
            } catch (e: Exception) {
                val errorMessage = "Fallo al alternar el reporte de crashes: ${e.localizedMessage}"
                _uiState.update { it.copy(error = errorMessage) }
                Timber.tag(TAG).e(e, errorMessage)
            }
        }
    }

    fun toggleCrashReportingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                userPreferencesRepository.setCrashReportingEnabled(enabled)
                crashReporter.enableCrashReporting(enabled)
                Timber.tag(TAG).i("Reporte de crashes establecido a: $enabled")
            } catch (e: Exception) {
                val errorMessage = "Error al configurar reporte de crashes: ${e.localizedMessage}"
                _uiState.update { it.copy(error = errorMessage) }
                Timber.tag(TAG).e(e, errorMessage)
            }
        }
    }

    fun resetLastClickedRoute() {
        _uiState.update { it.copy(lastClickedRoute = null) }
        Timber.tag(TAG).d("Ruta clicada reseteada")
    }

    fun toggleItemExpansion(itemTitle: String) {
        _uiState.update { currentState ->
            currentState.copy(
                expandedItems = currentState.expandedItems.toMutableSet().apply {
                    if (contains(itemTitle)) remove(itemTitle) else add(itemTitle)
                }
            )
        }
        Timber.tag(TAG).d("Item ${if (isItemExpanded(itemTitle)) "expandido" else "colapsado"}: $itemTitle")
    }

    fun isItemExpanded(itemTitle: String): Boolean {
        return _uiState.value.expandedItems.contains(itemTitle)
    }

    data class SettingsUiState(
        val isLoading: Boolean = true,
        val lastClickedRoute: String? = null,
        val expandedItems: Set<String> = emptySet(),
        val error: String? = null
    )

    companion object {
        private const val TAG = "SettingsViewModel"
    }
}