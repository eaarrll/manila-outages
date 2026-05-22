package com.example.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiParserClient
import com.example.data.AddressAlertResult
import com.example.data.OutageEntity
import com.example.data.OutageRepository
import com.example.data.UserAddressEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

sealed interface ParsingState {
    object Idle : ParsingState
    object Loading : ParsingState
    data class Success(val parsedOutage: OutageEntity) : ParsingState
    data class Error(val message: String) : ParsingState
}

class OutageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = OutageRepository(application)

    // A unique user installations ID to limit double voting
    val userId: String by lazy {
        val sharedPrefs = application.getSharedPreferences("outage_tracker_prefs", Application.MODE_PRIVATE)
        var id = sharedPrefs.getString("user_id", null)
        if (id == null) {
            id = UUID.randomUUID().toString().substring(0, 8)
            sharedPrefs.edit().putString("user_id", id).apply()
        }
        id
    }

    // Filter and search states
    val searchQuery = MutableStateFlow("")
    val selectedProviderFilter = MutableStateFlow("ALL") // "ALL", "MERALCO", "MAYNILAD", "MANILA_WATER"
    val selectedTypeFilter = MutableStateFlow("ALL") // "ALL", "POWER", "WATER"

    // Raw streams from DB
    val allAddresses: StateFlow<List<UserAddressEntity>> = repository.userAddressesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOutages: StateFlow<List<OutageEntity>> = repository.allOutagesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamically calculate alert statuses for all saved user addresses
    val addressAlertStates = combine(allAddresses, allOutages) { addresses, _ ->
        addresses.associateWith { addr ->
            repository.getAlertStatusForAddress(addr.city, addr.barangay)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // Filtered lists for Scheduled
    val scheduledOutages: StateFlow<List<OutageEntity>> = combine(
        repository.scheduledOutagesFlow,
        searchQuery,
        selectedProviderFilter,
        selectedTypeFilter
    ) { list, query, provider, type ->
        list.filter { outage ->
            val matchesQuery = query.isEmpty() || 
                    outage.city.contains(query, ignoreCase = true) ||
                    outage.barangay.contains(query, ignoreCase = true) ||
                    outage.streets.contains(query, ignoreCase = true) ||
                    outage.title.contains(query, ignoreCase = true)
            
            val matchesProvider = provider == "ALL" || outage.provider == provider
            val matchesType = type == "ALL" || outage.type == type

            matchesQuery && matchesProvider && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered lists for Crowdsourced Outages
    val crowdsourcedOutages: StateFlow<List<OutageEntity>> = combine(
        repository.crowdsourcedOutagesFlow,
        searchQuery,
        selectedProviderFilter,
        selectedTypeFilter
    ) { list, query, provider, type ->
        list.filter { outage ->
            val matchesQuery = query.isEmpty() || 
                    outage.city.contains(query, ignoreCase = true) ||
                    outage.barangay.contains(query, ignoreCase = true) ||
                    outage.streets.contains(query, ignoreCase = true) ||
                    outage.title.contains(query, ignoreCase = true)
            
            val matchesProvider = provider == "ALL" || outage.provider == provider
            val matchesType = type == "ALL" || outage.type == type

            matchesQuery && matchesProvider && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI Parsing Flow State
    private val _parsingState = MutableStateFlow<ParsingState>(ParsingState.Idle)
    val parsingState: StateFlow<ParsingState> = _parsingState.asStateFlow()

    // Metro Manila Cities List
    val metroManilaCities = listOf(
        "Caloocan", "Las Piñas", "Makati", "Malabon", "Mandaluyong", "Manila",
        "Marikina", "Muntinlupa", "Navotas", "Parañaque", "Pasay", "Pasig",
        "Pateros", "Quezon City", "San Juan", "Taguig", "Valenzuela"
    )

    fun resetParsingState() {
        _parsingState.value = ParsingState.Idle
    }

    // Parsing Advisory via Gemini Copilot
    fun parseUtilityAdvisory(rawText: String) {
        if (rawText.isBlank()) {
            _parsingState.value = ParsingState.Error("Please paste some advisory content first.")
            return
        }
        _parsingState.value = ParsingState.Loading
        viewModelScope.launch {
            try {
                val parsedOutage = GeminiParserClient.parseAnnouncement(rawText, System.currentTimeMillis())
                if (parsedOutage != null) {
                    _parsingState.value = ParsingState.Success(parsedOutage)
                } else {
                    _parsingState.value = ParsingState.Error("Failed to extract outage details. Try pasting a cleaner version or key sentences.")
                }
            } catch (e: Exception) {
                _parsingState.value = ParsingState.Error(e.message ?: "An unexpected service error occurred during AI analysis.")
            }
        }
    }

    // Save final parsed outage
    fun saveParsedOutage(outage: OutageEntity) {
        viewModelScope.launch {
            repository.insertOutage(outage)
            _parsingState.value = ParsingState.Idle
        }
    }

    // Add normal User Crowdsourced Report
    fun submitCrowdsourcedReport(
        type: String,
        provider: String,
        title: String,
        city: String,
        barangay: String,
        streets: String,
        details: String
    ) {
        viewModelScope.launch {
            val crowdsourcedReport = OutageEntity(
                type = type,
                provider = provider,
                isScheduled = false,
                title = title,
                scheduledStart = 0L,
                scheduledEnd = 0L,
                city = city,
                barangay = barangay,
                streets = streets,
                details = details,
                reportedAt = System.currentTimeMillis(),
                reportedBy = "User_${userId}",
                upvotes = 1,
                isVerified = false,
                userUpvotedList = userId
            )
            repository.insertOutage(crowdsourcedReport)
        }
    }

    // Upvoting Crowdsourced
    fun upvoteReport(outageId: Int) {
        viewModelScope.launch {
            repository.upvoteOutage(outageId, userId)
        }
    }

    // Delete Outage Report (Admins/Creator fallback or simple local remove)
    fun removeOutage(outageId: Int) {
        viewModelScope.launch {
            repository.deleteOutage(outageId)
        }
    }

    // User Addresses Actions
    fun addUserAddress(label: String, city: String, barangay: String, street: String) {
        viewModelScope.launch {
            val address = UserAddressEntity(
                label = label.ifBlank { "Location" },
                city = city,
                barangay = barangay,
                street = street
            )
            repository.insertUserAddress(address)
        }
    }

    fun removeUserAddress(id: Int) {
        viewModelScope.launch {
            repository.deleteUserAddress(id)
        }
    }
}
