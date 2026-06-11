package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiClient
import com.example.api.ProviderDetails
import com.example.crypto.CryptoManager
import com.example.data.ApiKeyEntry
import com.example.data.ApiKeyRepository
import com.example.data.AppDatabase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class VaultUiState(
    val keysList: List<ApiKeyEntry> = emptyList(),
    val filteredKeysList: List<ApiKeyEntry> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val suggestedDetails: ProviderDetails? = null,
    val isFetchingSuggestions: Boolean = false,
    val alertMessage: String? = null,
    val showAddDialog: Boolean = false,
    val decryptKeyId: Int? = null, // Track which key's decryption is visible
    val searchProviderType: String = "ALL" // Filter: ALL, LLM, MCP
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ApiKeyRepository
    private val _uiState = MutableStateFlow(VaultUiState())
    val uiState: StateFlow<VaultUiState> = _uiState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = ApiKeyRepository(database.apiKeyDao())

        // Combine flow emissions of database list, searchQuery, and providerFilter live
        repository.allKeys.combine(_uiState.map { it.searchQuery }.distinctUntilChanged()) { keys, query ->
            Pair(keys, query)
        }.combine(_uiState.map { it.searchProviderType }.distinctUntilChanged()) { pair, type ->
            val (keys, query) = pair
            var list = keys
            if (query.isNotEmpty()) {
                list = list.filter {
                    it.providerName.contains(query, ignoreCase = true) ||
                    it.modelIdentifier.contains(query, ignoreCase = true) ||
                    it.notes.contains(query, ignoreCase = true)
                }
            }
            if (type != "ALL") {
                list = list.filter {
                    val isMcp = it.providerName.contains("mcp", ignoreCase = true) ||
                            it.modelIdentifier.contains("mcp", ignoreCase = true) ||
                            it.baseUrl.contains("mcp", ignoreCase = true)
                    if (type == "MCP") isMcp else !isMcp
                }
            }
            list
        }.onEach { filtered ->
            _uiState.update { it.copy(filteredKeysList = filtered) }
        }.launchIn(viewModelScope)

        repository.allKeys.onEach { keys ->
            _uiState.update { it.copy(keysList = keys) }
        }.launchIn(viewModelScope)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun updateProviderFilter(type: String) {
        _uiState.update { it.copy(searchProviderType = type) }
    }

    fun setShowAddDialog(show: Boolean) {
        _uiState.update {
            it.copy(
                showAddDialog = show,
                suggestedDetails = null,
                isFetchingSuggestions = false
            )
        }
    }

    fun setAlertMessage(message: String?) {
        _uiState.update { it.copy(alertMessage = message) }
    }

    fun toggleKeyVisibility(id: Int) {
        _uiState.update {
            val current = it.decryptKeyId
            it.copy(decryptKeyId = if (current == id) null else id)
        }
    }

    fun getDecryptedKey(encryptedKey: String): String {
        return CryptoManager.decryptString(encryptedKey)
    }

    fun fetchDocsSuggestions(providerName: String, modelIdentifier: String) {
        if (providerName.isBlank()) {
            setAlertMessage("Please enter a provider name first.")
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingSuggestions = true, suggestedDetails = null) }
            try {
                val details = GeminiClient.lookupProviderDocs(providerName, modelIdentifier)
                _uiState.update { it.copy(suggestedDetails = details) }
            } catch (e: Exception) {
                e.printStackTrace()
                val fallbackDetails = GeminiClient.getFallbackDocs(providerName, modelIdentifier)
                _uiState.update { it.copy(suggestedDetails = fallbackDetails) }
            } finally {
                _uiState.update { it.copy(isFetchingSuggestions = false) }
            }
        }
    }

    fun saveKey(
        providerName: String,
        modelIdentifier: String,
        baseUrl: String,
        endpointStructure: String,
        rawApiKey: String,
        notes: String
    ): Boolean {
        if (providerName.isBlank() || rawApiKey.isBlank()) {
            setAlertMessage("Provider Name and API Key cannot be empty.")
            return false
        }

        val encryptedKey = CryptoManager.encryptString(rawApiKey)
        if (encryptedKey.isEmpty()) {
            setAlertMessage("Encryption failure. Store setup corrupt.")
            return false
        }

        viewModelScope.launch {
            val entry = ApiKeyEntry(
                providerName = providerName.trim(),
                modelIdentifier = modelIdentifier.trim(),
                baseUrl = baseUrl.trim(),
                endpointStructure = endpointStructure.trim(),
                encryptedApiKey = encryptedKey,
                notes = notes.trim()
            )
            repository.insertKey(entry)
            setAlertMessage("API Key saved securely on-device.")
        }
        return true
    }

    fun deleteKey(id: Int) {
        viewModelScope.launch {
            repository.deleteKeyById(id)
            setAlertMessage("API Key removed securely from device.")
        }
    }
}
