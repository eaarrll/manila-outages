package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.AddressAlertResult
import com.example.data.OutageEntity
import com.example.data.UserAddressEntity
import java.text.SimpleDateFormat
import java.util.*

// Direct hex colors to avoid slop and provide clean utility styling
object MetroColors {
    val DarkSlateDefault = Color(0xFF12141C)
    val DarkSlateSurface = Color(0xFF1E212E)
    val DarkSlateBorder = Color(0xFF2C3145)

    val PowerAmber = Color(0xFFFFB300)       // Meralco/Power Amber Accent
    val WaterBlue = Color(0xFF29B6F6)        // Maynilad/Water Light Blue Accent
    val ManilaWaterBlue = Color(0xFF0288D1)  // Manila Water Deep Blue Accent
    
    val AlertRed = Color(0xFFEF5350)         // Critical/Active Interruptions
    val SafeGreen = Color(0xFF66BB6A)        // Safe Sector Indicator
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB0B5C6)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutageAppScreen(
    viewModel: OutageViewModel = viewModel()
) {
    // Collect Flow States safely via collectAsStateWithLifecycle
    val scheduledOutages by viewModel.scheduledOutages.collectAsStateWithLifecycle()
    val crowdsourcedOutages by viewModel.crowdsourcedOutages.collectAsStateWithLifecycle()
    val userSavedAddresses by viewModel.allAddresses.collectAsStateWithLifecycle()
    val addressAlertStates by viewModel.addressAlertStates.collectAsStateWithLifecycle()
    val allOutagesList by viewModel.allOutages.collectAsStateWithLifecycle()

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedProviderFilter by viewModel.selectedProviderFilter.collectAsStateWithLifecycle()
    val selectedTypeFilter by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(0) } // 0 = Schedules, 1 = Crowdsourced, 2 = Saved Alerts

    // Action dialog states
    var showAiParseDialog by remember { mutableStateOf(false) }
    var showCrowdReportDialog by remember { mutableStateOf(false) }
    var showAddAddressDialog by remember { mutableStateOf(false) }

    // Scaffolding UI
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MetroColors.DarkSlateDefault,
        topBar = {
            Column(
                modifier = Modifier
                    .background(MetroColors.DarkSlateSurface)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CrisisAlert,
                        contentDescription = "App Logo",
                        tint = MetroColors.PowerAmber,
                        modifier = Modifier
                            .size(28.dp)
                            .padding(end = 6.dp)
                    )
                    Text(
                        text = "MANILA OUTAGE WATCH",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MetroColors.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MetroColors.DarkSlateBorder)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "METRO MANILA",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MetroColors.PowerAmber
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stats ticker overlay
                StatsTickerBanner(allOutagesList)
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = MetroColors.DarkSlateSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.navigationBarsPadding()
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Filled.CalendarMonth, contentDescription = "Schedules") },
                    label = { Text("Advisories") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MetroColors.PowerAmber,
                        selectedTextColor = MetroColors.PowerAmber,
                        indicatorColor = MetroColors.DarkSlateBorder,
                        unselectedIconColor = MetroColors.TextSecondary,
                        unselectedTextColor = MetroColors.TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Filled.Group, contentDescription = "Crowdsourced") },
                    label = { Text("Crowdsourced") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MetroColors.WaterBlue,
                        selectedTextColor = MetroColors.WaterBlue,
                        indicatorColor = MetroColors.DarkSlateBorder,
                        unselectedIconColor = MetroColors.TextSecondary,
                        unselectedTextColor = MetroColors.TextSecondary
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Filled.HomeWork, contentDescription = "My Alerts") },
                    label = { Text("Saved Alerts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MetroColors.SafeGreen,
                        selectedTextColor = MetroColors.SafeGreen,
                        indicatorColor = MetroColors.DarkSlateBorder,
                        unselectedIconColor = MetroColors.TextSecondary,
                        unselectedTextColor = MetroColors.TextSecondary
                    )
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Persistent search bar and filters for tabs 0 and 1
                if (activeTab == 0 || activeTab == 1) {
                    Column(
                        modifier = Modifier
                            .background(MetroColors.DarkSlateSurface)
                            .padding(bottom = 12.dp, start = 16.dp, end = 16.dp)
                    ) {
                        // Search bar
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.searchQuery.value = it },
                            placeholder = { Text("Search city, barangay, or street...", color = MetroColors.TextSecondary, fontSize = 14.sp) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search", tint = MetroColors.TextSecondary) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear", tint = MetroColors.TextSecondary)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MetroColors.TextPrimary,
                                unfocusedTextColor = MetroColors.TextPrimary,
                                focusedBorderColor = MetroColors.PowerAmber,
                                unfocusedBorderColor = MetroColors.DarkSlateBorder,
                                focusedContainerColor = MetroColors.DarkSlateDefault,
                                unfocusedContainerColor = MetroColors.DarkSlateDefault
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Chip selection filters
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Filter by Provider
                            Box(modifier = Modifier.weight(1f)) {
                                var providerExpanded by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { providerExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetroColors.DarkSlateBorder),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (selectedProviderFilter == "ALL") "All Providers" else selectedProviderFilter,
                                        fontSize = 11.sp,
                                        color = MetroColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MetroColors.TextSecondary, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = providerExpanded,
                                    onDismissRequest = { providerExpanded = false },
                                    modifier = Modifier.background(MetroColors.DarkSlateSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Providers", color = MetroColors.TextPrimary) },
                                        onClick = { viewModel.selectedProviderFilter.value = "ALL"; providerExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("MERALCO (Power)", color = MetroColors.TextPrimary) },
                                        onClick = { viewModel.selectedProviderFilter.value = "MERALCO"; providerExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("MAYNILAD (Water)", color = MetroColors.TextPrimary) },
                                        onClick = { viewModel.selectedProviderFilter.value = "MAYNILAD"; providerExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("MANILA WATER (Water)", color = MetroColors.TextPrimary) },
                                        onClick = { viewModel.selectedProviderFilter.value = "MANILA_WATER"; providerExpanded = false }
                                    )
                                }
                            }

                            // Filter by Type
                            Box(modifier = Modifier.weight(1f)) {
                                var typeExpanded by remember { mutableStateOf(false) }
                                Button(
                                    onClick = { typeExpanded = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MetroColors.DarkSlateBorder),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (selectedTypeFilter == "ALL") "All Types" else if (selectedTypeFilter == "POWER") "Power Outages" else "Water Interrupted",
                                        fontSize = 11.sp,
                                        color = MetroColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MetroColors.TextSecondary, modifier = Modifier.size(16.dp))
                                }
                                DropdownMenu(
                                    expanded = typeExpanded,
                                    onDismissRequest = { typeExpanded = false },
                                    modifier = Modifier.background(MetroColors.DarkSlateSurface)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("All Types", color = MetroColors.TextPrimary) },
                                        onClick = { viewModel.selectedTypeFilter.value = "ALL"; typeExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("⚡ Power Only", color = MetroColors.TextPrimary) },
                                        onClick = { viewModel.selectedTypeFilter.value = "POWER"; typeExpanded = false }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("💧 Water Only", color = MetroColors.TextPrimary) },
                                        onClick = { viewModel.selectedTypeFilter.value = "WATER"; typeExpanded = false }
                                    )
                                }
                            }

                            // Clear Filters Indicator Reset
                            if (selectedProviderFilter != "ALL" || selectedTypeFilter != "ALL") {
                                IconButton(
                                    onClick = {
                                        viewModel.selectedProviderFilter.value = "ALL"
                                        viewModel.selectedTypeFilter.value = "ALL"
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(Icons.Filled.FilterListOff, contentDescription = "Clear Filters", tint = MetroColors.AlertRed)
                                }
                            }
                        }
                    }
                }

                // Main screen view switcher based on active tab
                when (activeTab) {
                    0 -> ScheduledAdvisoriesTab(
                        outages = scheduledOutages,
                        onAiImportClick = { showAiParseDialog = true },
                        onDeleteOutage = { viewModel.removeOutage(it) }
                    )
                    1 -> CrowdsourcedLiveTab(
                        outages = crowdsourcedOutages,
                        onReportClick = { showCrowdReportDialog = true },
                        onUpvote = { viewModel.upvoteReport(it) },
                        userId = viewModel.userId,
                        onDeleteOutage = { viewModel.removeOutage(it) }
                    )
                    2 -> SavedAlertsTab(
                        addresses = userSavedAddresses,
                        alerts = addressAlertStates,
                        onAddAddressClick = { showAddAddressDialog = true },
                        onDeleteAddress = { viewModel.removeUserAddress(it) }
                    )
                }
            }

            // Dialog definitions
            if (showAiParseDialog) {
                val parsingState by viewModel.parsingState.collectAsStateWithLifecycle()
                AiCopilotParseDialog(
                    cities = viewModel.metroManilaCities,
                    parsingState = parsingState,
                    onParseClick = { viewModel.parseUtilityAdvisory(it) },
                    onTryAgainClick = { viewModel.resetParsingState() },
                    onSaveClick = {
                        viewModel.saveParsedOutage(it)
                        showAiParseDialog = false
                    },
                    onDismiss = {
                        viewModel.resetParsingState()
                        showAiParseDialog = false
                    }
                )
            }

            if (showCrowdReportDialog) {
                CrowdsourcedReportDialog(
                    cities = viewModel.metroManilaCities,
                    onSubmit = { type, provider, title, city, barangay, streets, details ->
                        viewModel.submitCrowdsourcedReport(type, provider, title, city, barangay, streets, details)
                        showCrowdReportDialog = false
                    },
                    onDismiss = { showCrowdReportDialog = false }
                )
            }

            if (showAddAddressDialog) {
                AddAddressDialog(
                    cities = viewModel.metroManilaCities,
                    onSave = { label, city, barangay, street ->
                        viewModel.addUserAddress(label, city, barangay, street)
                        showAddAddressDialog = false
                    },
                    onDismiss = { showAddAddressDialog = false }
                )
            }
        }
    }
}

// Banner displaying quick live aggregate ticker
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StatsTickerBanner(allOutagesList: List<OutageEntity>) {
    val now = System.currentTimeMillis()
    val totalScheduledActive = allOutagesList.count { it.isScheduled && now in it.scheduledStart..it.scheduledEnd }
    val totalUnscheduled = allOutagesList.count { !it.isScheduled && (now - it.reportedAt < 12 * 3600000L) }
    val powerInterrupted = allOutagesList.count { it.type == "POWER" && (it.isScheduled && now in it.scheduledStart..it.scheduledEnd || !it.isScheduled && now - it.reportedAt < 6 * 3600000) }
    val waterInterrupted = allOutagesList.count { it.type == "WATER" && (it.isScheduled && now in it.scheduledStart..it.scheduledEnd || !it.isScheduled && now - it.reportedAt < 6 * 3600000) }

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        StatsBox(text = "⚡ $powerInterrupted Active Power Cuts", iconColor = MetroColors.PowerAmber)
        StatsBox(text = "💧 $waterInterrupted Active Water Interrupted", iconColor = MetroColors.WaterBlue)
        StatsBox(text = "📣 $totalUnscheduled Citizen Reports (12h)", iconColor = MetroColors.TextSecondary)
    }
}

@Composable
fun StatsBox(text: String, iconColor: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MetroColors.DarkSlateBorder.copy(alpha = 0.5f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MetroColors.TextPrimary)
    }
}

// Countdown time formatter helper
fun formatTimeDetails(isScheduled: Boolean, start: Long, end: Long): ScheduleTimeProgress {
    val now = System.currentTimeMillis()
    if (!isScheduled) return ScheduleTimeProgress("Crowdsourced", false, "N/A", "Active Unscheduled Outage")

    val dateFormat = SimpleDateFormat("MMM dd, yyyy (hh:mm a)", Locale.US)
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)

    val startDate = Date(start)
    val endDate = Date(end)

    val mainInterval = "${dateFormat.format(startDate)} to ${timeFormat.format(endDate)}"

    return when {
        now > end -> {
            ScheduleTimeProgress("Ended", false, mainInterval, "Completed/Power Restored")
        }
        now in start..end -> {
            val remainHours = ((end - now) / 3600000.0).coerceAtLeast(0.1)
            val timeString = if (remainHours < 1.0) {
                "${((end - now) / 60000).toInt()} mins"
            } else {
                "${String.format("%.1f", remainHours)} hrs"
            }
            ScheduleTimeProgress("Active Now ⚠️", true, mainInterval, "Expires in $timeString")
        }
        else -> {
            val waitHours = (start - now) / 3600000.0
            val badgeDescription = when {
                waitHours < 1.0 -> "Starts in ${((start - now) / 60000).toInt()} mins!"
                waitHours < 24.0 -> "Starts in ${String.format("%.1f", waitHours)} hrs"
                else -> "Starts in ${String.format("%.1f", waitHours / 24.0)} days"
            }
            ScheduleTimeProgress("Upcoming", false, mainInterval, badgeDescription)
        }
    }
}

data class ScheduleTimeProgress(
    val status: String,
    val isActiveNow: Boolean,
    val dateInterval: String,
    val badgeMessage: String
)

// --- CARD LAYOUTS FOR ADVISORIES ---

@Composable
fun OutageItemCard(
    outage: OutageEntity,
    onVoteClick: (() -> Unit)? = null,
    onDeleteOutage: ((Int) -> Unit)? = null,
    userId: String = ""
) {
    val now = System.currentTimeMillis()
    val timeProgress = formatTimeDetails(outage.isScheduled, outage.scheduledStart, outage.scheduledEnd)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MetroColors.DarkSlateSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row: Provider, Utility Logo & Badge Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Provider label Indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (outage.provider) {
                                "MERALCO" -> MetroColors.PowerAmber.copy(alpha = 0.15f)
                                "MAYNILAD" -> MetroColors.WaterBlue.copy(alpha = 0.15f)
                                else -> MetroColors.ManilaWaterBlue.copy(alpha = 0.15f)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = outage.provider,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (outage.provider) {
                            "MERALCO" -> MetroColors.PowerAmber
                            "MAYNILAD" -> MetroColors.WaterBlue
                            else -> MetroColors.ManilaWaterBlue
                        }
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Outage type (Power / Water) label mini
                Text(
                    text = if (outage.type == "POWER") "⚡ Power" else "💧 Water",
                    fontSize = 11.sp,
                    color = MetroColors.TextSecondary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.weight(1f))

                // Action status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                !outage.isScheduled -> MetroColors.AlertRed.copy(alpha = 0.15f)
                                timeProgress.isActiveNow -> MetroColors.AlertRed.copy(alpha = 0.15f)
                                timeProgress.status == "Upcoming" -> MetroColors.PowerAmber.copy(alpha = 0.15f)
                                else -> MetroColors.DarkSlateBorder
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (!outage.isScheduled) "LIVE REPORT" else timeProgress.badgeMessage.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            !outage.isScheduled -> MetroColors.AlertRed
                            timeProgress.isActiveNow -> MetroColors.AlertRed
                            timeProgress.status == "Upcoming" -> MetroColors.PowerAmber
                            else -> MetroColors.TextSecondary
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Body: Outage Title & Primary Description
            Text(
                text = outage.title,
                fontSize = 15.sp,
                color = MetroColors.TextPrimary,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(4.dp))

            // City + Barangay highlighted sector label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = MetroColors.AlertRed,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${outage.city}, ${outage.barangay}",
                    fontSize = 12.sp,
                    color = MetroColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Streets affected (collapsible-styled box)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MetroColors.DarkSlateDefault.copy(alpha = 0.5f))
                    .padding(8.dp)
            ) {
                Column {
                    Text(
                        text = "AFFECTED AREA / STREETS:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MetroColors.PowerAmber
                    )
                    Text(
                        text = outage.streets.ifBlank { "Unspecified block range" },
                        fontSize = 12.sp,
                        color = MetroColors.TextSecondary
                    )
                }
            }

            // Details/Instructions description
            Text(
                text = outage.details,
                fontSize = 12.sp,
                color = MetroColors.TextSecondary,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Footer Row: Timestamps, reporting, upvoting, deletion
            Divider(color = MetroColors.DarkSlateBorder.copy(alpha = 0.5f), thickness = 1.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (outage.isScheduled) "SCHEDULE DATE:" else "REPORTED AT:",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = MetroColors.TextSecondary
                    )
                    Text(
                        text = if (outage.isScheduled) timeProgress.dateInterval else {
                            val sdf = SimpleDateFormat("MMM dd, hh:mm a (12h ago)", Locale.US)
                            val elapsedMinutes = (now - outage.reportedAt) / 60000
                            val elapsedText = when {
                                elapsedMinutes < 1 -> "just now"
                                elapsedMinutes < 60 -> "$elapsedMinutes mins ago"
                                else -> "${elapsedMinutes / 60} hrs ago"
                            }
                            "${SimpleDateFormat("MMM dd, hh:mm a", Locale.US).format(Date(outage.reportedAt))} ($elapsedText)"
                        },
                        fontSize = 11.sp,
                        color = MetroColors.TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // If non-scheduled (crowdsourced), show upvote action
                if (!outage.isScheduled && onVoteClick != null) {
                    val userUpvotedList = outage.userUpvotedList.split(",").map { it.trim() }
                    val isUpvotedByMe = userUpvotedList.contains(userId)

                    Button(
                        onClick = onVoteClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isUpvotedByMe) MetroColors.SafeGreen.copy(alpha = 0.2f) else MetroColors.DarkSlateBorder
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = if (isUpvotedByMe) Icons.Filled.CheckCircle else Icons.Filled.ThumbUp,
                            contentDescription = "Upvote",
                            tint = if (isUpvotedByMe) MetroColors.SafeGreen else MetroColors.TextPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${outage.upvotes} Me Too",
                            fontSize = 11.sp,
                            color = if (isUpvotedByMe) MetroColors.SafeGreen else MetroColors.TextPrimary
                        )

                        if (outage.isVerified) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = "High Confidence Report",
                                tint = MetroColors.WaterBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }

                // Temporary Local Delete (For testing & clean interface)
                if (onDeleteOutage != null && (outage.reportedBy == "AI Copilot Parser" || outage.reportedBy == "User_$userId")) {
                    IconButton(
                        onClick = { onDeleteOutage(outage.id) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Remove outage",
                            tint = MetroColors.AlertRed.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

// --- TAB IMPLEMENTATIONS ---

// Tab 0: Scheduled Advisories Tab
@Composable
fun ScheduledAdvisoriesTab(
    outages: List<OutageEntity>,
    onAiImportClick: () -> Unit,
    onDeleteOutage: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (outages.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.LightbulbCircle,
                    contentDescription = null,
                    tint = MetroColors.DarkSlateBorder,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Scheduled Outages Match Filters",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MetroColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Adjust your search parameters, or paste a new Facebook/PDF advisory using our AI Copilot below to add it to the schedule.",
                    fontSize = 13.sp,
                    color = MetroColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {
                item {
                    // Small banner header about AI import
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MetroColors.DarkSlateBorder.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Psychology,
                                contentDescription = "AI Copilot",
                                tint = MetroColors.PowerAmber,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Have a raw Facebook/PDF advisory?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MetroColors.TextPrimary
                               )
                                Text(
                                    text = "Our AI Copilot extracts streets and dates in seconds.",
                                    fontSize = 10.sp,
                                    color = MetroColors.TextSecondary
                                )
                            }
                            Button(
                                onClick = onAiImportClick,
                                colors = ButtonDefaults.buttonColors(containerColor = MetroColors.PowerAmber),
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("A.I. Import", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }

                items(outages) { outage ->
                    OutageItemCard(
                        outage = outage,
                        onDeleteOutage = onDeleteOutage
                    )
                }
            }
        }

        // Floating Action Button as a secondary indicator for importing advisory
        FloatingActionButton(
            onClick = onAiImportClick,
            containerColor = MetroColors.PowerAmber,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.PostAdd, contentDescription = "AI Import", tint = Color.Black)
        }
    }
}

// Tab 1: Crowdsourced Feed Hub
@Composable
fun CrowdsourcedLiveTab(
    outages: List<OutageEntity>,
    onReportClick: () -> Unit,
    onUpvote: (Int) -> Unit,
    userId: String,
    onDeleteOutage: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Citizen Report quick alert panel
            Card(
                colors = CardDefaults.cardColors(containerColor = MetroColors.DarkSlateSurface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.WifiChannel,
                        contentDescription = "Crowd Alert",
                        tint = MetroColors.WaterBlue,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Unscheduled Sudden Outage?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetroColors.TextPrimary
                        )
                        Text(
                            text = "Report standard blackout or water cut and help your adjacent neighborhoods.",
                            fontSize = 11.sp,
                            color = MetroColors.TextSecondary
                        )
                    }
                    Button(
                        onClick = onReportClick,
                        colors = ButtonDefaults.buttonColors(containerColor = MetroColors.WaterBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Report +", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    }
                }
            }

            if (outages.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Filled.ReportGmailerrorred,
                        contentDescription = null,
                        tint = MetroColors.DarkSlateBorder,
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Sudden Outages Reported Nearby",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MetroColors.TextPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Everything looks smooth! If you are losing power or water, press the 'Report +' button to notify users.",
                        fontSize = 13.sp,
                        color = MetroColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)
                ) {
                    items(outages) { outage ->
                        OutageItemCard(
                            outage = outage,
                            onVoteClick = { onUpvote(outage.id) },
                            userId = userId,
                            onDeleteOutage = onDeleteOutage
                        )
                    }
                }
            }
        }

        // Floating button for direct report
        FloatingActionButton(
            onClick = onReportClick,
            containerColor = MetroColors.WaterBlue,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Filled.AddAlert, contentDescription = "Report Outage", tint = Color.Black)
        }
    }
}

// Tab 2: Saved Alerts / Sector watches
@Composable
fun SavedAlertsTab(
    addresses: List<UserAddressEntity>,
    alerts: Map<UserAddressEntity, AddressAlertResult>,
    onAddAddressClick: () -> Unit,
    onDeleteAddress: (Int) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (addresses.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.NotificationAdd,
                    contentDescription = null,
                    tint = MetroColors.DarkSlateBorder,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Track Your Sectors (Home, Office)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MetroColors.TextPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Save your workplace, house, or gym location. We will scan all current schedules + citizen reports to show visual alarm parameters for you.",
                    fontSize = 13.sp,
                    color = MetroColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onAddAddressClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MetroColors.SafeGreen)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Register Sector Watch", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 80.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "SECTOR WATCH REGISTRY",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetroColors.TextPrimary
                        )
                        OutlinedButton(
                            onClick = onAddAddressClick,
                            border = BorderStroke(1.dp, MetroColors.SafeGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MetroColors.SafeGreen),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Location", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                items(addresses) { address ->
                    val alertState = alerts[address]

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = when (alertState?.maxSeverityState) {
                                "ACTIVE" -> MetroColors.AlertRed
                                "UPCOMING" -> MetroColors.PowerAmber
                                else -> MetroColors.DarkSlateBorder
                            }
                        ),
                        colors = CardDefaults.cardColors(containerColor = MetroColors.DarkSlateSurface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Header Row: Label & Severity Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (address.label.contains("Office") || address.label.contains("🏢")) Icons.Filled.Business else Icons.Filled.Home,
                                        contentDescription = null,
                                        tint = MetroColors.PowerAmber,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = address.label,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MetroColors.TextPrimary
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (alertState?.maxSeverityState) {
                                                    "ACTIVE" -> MetroColors.AlertRed.copy(alpha = 0.15f)
                                                    "UPCOMING" -> MetroColors.PowerAmber.copy(alpha = 0.15f)
                                                    else -> MetroColors.SafeGreen.copy(alpha = 0.15f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = when (alertState?.maxSeverityState) {
                                                "ACTIVE" -> "🚨 DANGER / OUTAGE AREA"
                                                "UPCOMING" -> "⚠️ INTERRUPTS SCHEDULED"
                                                else -> "✓ SECTOR CLEAR"
                                            },
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (alertState?.maxSeverityState) {
                                                "ACTIVE" -> MetroColors.AlertRed
                                                "UPCOMING" -> MetroColors.PowerAmber
                                                else -> MetroColors.SafeGreen
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    IconButton(
                                        onClick = { onDeleteAddress(address.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete Location", tint = MetroColors.TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Address String Details
                            Text(
                                text = "${address.street}, Brgy. ${address.barangay}, ${address.city}",
                                fontSize = 13.sp,
                                color = MetroColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MetroColors.DarkSlateBorder.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(10.dp))

                            // Action and upcoming summary lists
                            if (alertState == null || alertState.totalCount == 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MetroColors.SafeGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "No active power maintenance or water pipe leaks registered in ${address.barangay} sector.",
                                        fontSize = 12.sp,
                                        color = MetroColors.TextSecondary
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (alertState.scheduledActive.isNotEmpty()) {
                                        AlertDetailRow(
                                            iconColor = MetroColors.AlertRed,
                                            title = "CRITICAL ADVISORY",
                                            desc = "${alertState.scheduledActive.size} scheduled provider interruption is currently live!"
                                        )
                                    }
                                    if (alertState.unscheduledActive.isNotEmpty()) {
                                        AlertDetailRow(
                                            iconColor = MetroColors.AlertRed,
                                            title = "LIVE CITIZEN REPORTS UNVERIFIED",
                                            desc = "${alertState.unscheduledActive.size} sudden blackouts/water issues reported within 12h."
                                        )
                                    }
                                    if (alertState.scheduledUpcoming.isNotEmpty()) {
                                        val nextOutage = alertState.scheduledUpcoming.first()
                                        val format = SimpleDateFormat("EEE, h:mm a", Locale.US)
                                        AlertDetailRow(
                                            iconColor = MetroColors.PowerAmber,
                                            title = "UPCOMING SCHEDULED",
                                            desc = "Next interruption: ${nextOutage.provider} (${nextOutage.type}) on ${format.format(Date(nextOutage.scheduledStart))}"
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlertDetailRow(iconColor: Color, title: String, desc: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = iconColor
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MetroColors.TextPrimary
            )
        }
    }
}

// --- FULL IMPLEMENTATIONS OF MODAL DIALOGS ---

// Modal Dialog 1: AI Copilot Parse advisory
@Composable
fun AiCopilotParseDialog(
    cities: List<String>,
    parsingState: ParsingState,
    onParseClick: (String) -> Unit,
    onTryAgainClick: () -> Unit,
    onSaveClick: (OutageEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var rawTextToParse by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MetroColors.DarkSlateSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Psychology,
                        contentDescription = "AI",
                        tint = MetroColors.PowerAmber,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI Advisory Copilot",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MetroColors.TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (val state = parsingState) {
                    is ParsingState.Idle -> {
                        Text(
                            text = "Paste standard utility text or Facebook post below. Gemini AI will analyze the affected City, Barangay, Streets, and Dates.",
                            fontSize = 12.sp,
                            color = MetroColors.TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = rawTextToParse,
                            onValueChange = { rawTextToParse = it },
                            placeholder = { Text("E.g., Maynilad: There will be testing along Leon Guinto malate on May 25, 9am to 5pm...", color = MetroColors.TextSecondary, fontSize = 12.sp) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MetroColors.TextPrimary,
                                unfocusedTextColor = MetroColors.TextPrimary,
                                focusedBorderColor = MetroColors.PowerAmber,
                                unfocusedBorderColor = MetroColors.DarkSlateBorder,
                                focusedContainerColor = MetroColors.DarkSlateDefault,
                                unfocusedContainerColor = MetroColors.DarkSlateDefault
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel", color = MetroColors.TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { onParseClick(rawTextToParse) },
                                colors = ButtonDefaults.buttonColors(containerColor = MetroColors.PowerAmber),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Analyze with Gemini", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    is ParsingState.Loading -> {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(color = MetroColors.PowerAmber)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Extracting structures & geocoding streets...",
                                fontSize = 13.sp,
                                color = MetroColors.TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    is ParsingState.Error -> {
                        // Error visual panel
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Error",
                                tint = MetroColors.AlertRed,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "AI Parsing Failed",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MetroColors.TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = state.message,
                                fontSize = 12.sp,
                                color = MetroColors.AlertRed,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            Row {
                                TextButton(onClick = onDismiss) {
                                    Text("Dismiss", color = MetroColors.TextSecondary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Button(
                                    onClick = onTryAgainClick,
                                    colors = ButtonDefaults.buttonColors(containerColor = MetroColors.DarkSlateBorder)
                                ) {
                                    Text("Try Again", color = MetroColors.TextPrimary)
                                }
                            }
                        }
                    }

                    is ParsingState.Success -> {
                        val outage = state.parsedOutage
                        val format = SimpleDateFormat("MMM dd, yyyy (hh:mm a)", Locale.US)

                        Text(
                            text = "✅ SUCCESS! Extracted Outage Elements",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetroColors.SafeGreen
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Preview parsed cards
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MetroColors.DarkSlateDefault)
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PreviewElementRow("PROVIDER", "${outage.provider} (${outage.type})")
                            PreviewElementRow("TITLE", outage.title)
                            PreviewElementRow("CITY", outage.city)
                            PreviewElementRow("BARANGAY", outage.barangay)
                            PreviewElementRow("STREETS", outage.streets)
                            PreviewElementRow("START", format.format(Date(outage.scheduledStart)))
                            PreviewElementRow("END", format.format(Date(outage.scheduledEnd)))
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = onDismiss) {
                                Text("Cancel", color = MetroColors.TextSecondary)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = { onSaveClick(outage) },
                                colors = ButtonDefaults.buttonColors(containerColor = MetroColors.SafeGreen),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Save & Publish", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PreviewElementRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = "$label:",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = MetroColors.PowerAmber,
            modifier = Modifier.width(76.dp)
        )
        Text(
            text = value,
            fontSize = 11.sp,
            color = MetroColors.TextPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// Modal Dialog 2: Citizen Crowdsourced Outage Report
@Composable
fun CrowdsourcedReportDialog(
    cities: List<String>,
    onSubmit: (type: String, provider: String, title: String, city: String, barangay: String, streets: String, details: String) -> Unit,
    onDismiss: () -> Unit
) {
    var type by remember { mutableStateOf("POWER") } // "POWER", "WATER"
    var provider by remember { mutableStateOf("MERALCO") } // "MERALCO", "MAYNILAD", "MANILA_WATER"
    var title by remember { mutableStateOf("") }
    var selectedCity by remember { mutableStateOf(cities.firstOrNull() ?: "Quezon City") }
    var barangay by remember { mutableStateOf("") }
    var streets by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }

    var cityDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MetroColors.DarkSlateSurface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.WifiChannel,
                            contentDescription = null,
                            tint = MetroColors.WaterBlue,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Report Sudden Outage",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MetroColors.TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Share details about current unannounced water or power cuts.",
                        fontSize = 11.sp,
                        color = MetroColors.TextSecondary
                    )
                }

                // Type selector
                item {
                    Text("OUTAGE TYPE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        FilterChip(
                            selected = type == "POWER",
                            onClick = {
                                type = "POWER"
                                provider = "MERALCO"
                            },
                            label = { Text("⚡ Power outage") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = type == "WATER",
                            onClick = {
                                type = "WATER"
                                provider = "MAYNILAD"
                            },
                            label = { Text("💧 Water Out") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Provider selector derived
                item {
                    Text("UTILITY PROVIDER", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (type == "POWER") {
                            FilterChip(
                                selected = provider == "MERALCO",
                                onClick = { provider = "MERALCO" },
                                label = { Text("Meralco") },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            FilterChip(
                                selected = provider == "MAYNILAD",
                                onClick = { provider = "MAYNILAD" },
                                label = { Text("Maynilad") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = provider == "MANILA_WATER",
                                onClick = { provider = "MANILA_WATER" },
                                label = { Text("Manila Water") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Title
                item {
                    Text("BRIEF ISSUE STATUS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        placeholder = { Text("E.g., Low pressure water, complete blackout", color = MetroColors.TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MetroColors.TextPrimary,
                            unfocusedTextColor = MetroColors.TextPrimary,
                            focusedBorderColor = MetroColors.WaterBlue,
                            unfocusedBorderColor = MetroColors.DarkSlateBorder,
                            focusedContainerColor = MetroColors.DarkSlateDefault,
                            unfocusedContainerColor = MetroColors.DarkSlateDefault
                        )
                    )
                }

                // City dropdown picker
                item {
                    Text("AFFECTED CITY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedCity,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.fillMaxWidth().clickable { cityDropdownExpanded = true },
                            shape = RoundedCornerShape(8.dp),
                            trailingIcon = {
                                IconButton(onClick = { cityDropdownExpanded = true }) {
                                    Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MetroColors.TextSecondary)
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = MetroColors.TextPrimary,
                                unfocusedTextColor = MetroColors.TextPrimary,
                                focusedBorderColor = MetroColors.WaterBlue,
                                unfocusedBorderColor = MetroColors.DarkSlateBorder,
                                focusedContainerColor = MetroColors.DarkSlateDefault,
                                unfocusedContainerColor = MetroColors.DarkSlateDefault
                            )
                        )
                        DropdownMenu(
                            expanded = cityDropdownExpanded,
                            onDismissRequest = { cityDropdownExpanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(260.dp)
                                .background(MetroColors.DarkSlateSurface)
                        ) {
                            cities.forEach { city ->
                                DropdownMenuItem(
                                    text = { Text(city, color = MetroColors.TextPrimary) },
                                    onClick = {
                                        selectedCity = city
                                        cityDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Barangay
                item {
                    Text("BARANGAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                    OutlinedTextField(
                        value = barangay,
                        onValueChange = { barangay = it },
                        placeholder = { Text("E.g., Loyola Heights, BF Homes", color = MetroColors.TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MetroColors.TextPrimary,
                            unfocusedTextColor = MetroColors.TextPrimary,
                            focusedBorderColor = MetroColors.WaterBlue,
                            unfocusedBorderColor = MetroColors.DarkSlateBorder,
                            focusedContainerColor = MetroColors.DarkSlateDefault,
                            unfocusedContainerColor = MetroColors.DarkSlateDefault
                        )
                    )
                }

                // Streets
                item {
                    Text("AFFECTED STREETS / SITES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                    OutlinedTextField(
                        value = streets,
                        onValueChange = { streets = it },
                        placeholder = { Text("E.g., Esteban Abada St., El Grande Ave", color = MetroColors.TextSecondary, fontSize = 13.sp) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MetroColors.TextPrimary,
                            unfocusedTextColor = MetroColors.TextPrimary,
                            focusedBorderColor = MetroColors.WaterBlue,
                            unfocusedBorderColor = MetroColors.DarkSlateBorder,
                            focusedContainerColor = MetroColors.DarkSlateDefault,
                            unfocusedContainerColor = MetroColors.DarkSlateDefault
                        )
                    )
                }

                // Details Textbox
                item {
                    Text("ADDITIONAL DETAILS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        placeholder = { Text("Describe what happened (loud explosive sound, or leaking pipe visible along road)...", color = MetroColors.TextSecondary, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth().height(90.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MetroColors.TextPrimary,
                            unfocusedTextColor = MetroColors.TextPrimary,
                            focusedBorderColor = MetroColors.WaterBlue,
                            unfocusedBorderColor = MetroColors.DarkSlateBorder,
                            focusedContainerColor = MetroColors.DarkSlateDefault,
                            unfocusedContainerColor = MetroColors.DarkSlateDefault
                        )
                    )
                }

                // Form Buttons
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel", color = MetroColors.TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank() && barangay.isNotBlank()) {
                                    onSubmit(type, provider, title, selectedCity, barangay, streets, details)
                                }
                            },
                            enabled = title.isNotBlank() && barangay.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MetroColors.WaterBlue,
                                disabledContainerColor = MetroColors.DarkSlateBorder
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Publish Report", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Modal Dialog 3: Register sector alert address
@Composable
fun AddAddressDialog(
    cities: List<String>,
    onSave: (label: String, city: String, barangay: String, street: String) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("Home 🏠") }
    var selectedCity by remember { mutableStateOf(cities.firstOrNull() ?: "Quezon City") }
    var barangay by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }

    var cityDropdownExpanded by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MetroColors.DarkSlateSurface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AddLocationAlt,
                        contentDescription = null,
                        tint = MetroColors.SafeGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Sector Watch",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MetroColors.TextPrimary
                    )
                }

                Text(
                    text = "A Sector Watch automatically monitors scheduled outages and live citizen reports for your customized area.",
                    fontSize = 11.sp,
                    color = MetroColors.TextSecondary
                )

                // Label selector (chips)
                Text("LOCATION DESCRIPTION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = label == "Home 🏠",
                        onClick = { label = "Home 🏠" },
                        label = { Text("Home 🏠") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = label == "Office 🏢",
                        onClick = { label = "Office 🏢" },
                        label = { Text("Office 🏢") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = label != "Home 🏠" && label != "Office 🏢",
                        onClick = { label = "Workplace ☕" },
                        label = { Text("Other ☕") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Cities Selection Dropdown
                Text("CITIES BOUNDARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedCity,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().clickable { cityDropdownExpanded = true },
                        shape = RoundedCornerShape(8.dp),
                        trailingIcon = {
                            IconButton(onClick = { cityDropdownExpanded = true }) {
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = MetroColors.TextSecondary)
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = MetroColors.TextPrimary,
                            unfocusedTextColor = MetroColors.TextPrimary,
                            focusedBorderColor = MetroColors.SafeGreen,
                            unfocusedBorderColor = MetroColors.DarkSlateBorder,
                            focusedContainerColor = MetroColors.DarkSlateDefault,
                            unfocusedContainerColor = MetroColors.DarkSlateDefault
                        )
                    )
                    DropdownMenu(
                        expanded = cityDropdownExpanded,
                        onDismissRequest = { cityDropdownExpanded = false },
                        modifier = Modifier
                            .fillMaxWidth(0.9f)
                            .height(260.dp)
                            .background(MetroColors.DarkSlateSurface)
                    ) {
                        cities.forEach { city ->
                            DropdownMenuItem(
                                text = { Text(city, color = MetroColors.TextPrimary) },
                                onClick = {
                                    selectedCity = city
                                    cityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                // Barangay
                Text("BARANGAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                OutlinedTextField(
                    value = barangay,
                    onValueChange = { barangay = it },
                    placeholder = { Text("E.g., Loyola Heights, BF Homes", color = MetroColors.TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MetroColors.TextPrimary,
                        unfocusedTextColor = MetroColors.TextPrimary,
                        focusedBorderColor = MetroColors.SafeGreen,
                        unfocusedBorderColor = MetroColors.DarkSlateBorder,
                        focusedContainerColor = MetroColors.DarkSlateDefault,
                        unfocusedContainerColor = MetroColors.DarkSlateDefault
                    )
                )

                // Optional Street
                Text("STREET DETAILS (OPTIONAL)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MetroColors.TextSecondary)
                OutlinedTextField(
                    value = street,
                    onValueChange = { street = it },
                    placeholder = { Text("E.g., Esteban Abada St.", color = MetroColors.TextSecondary, fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MetroColors.TextPrimary,
                        unfocusedTextColor = MetroColors.TextPrimary,
                        focusedBorderColor = MetroColors.SafeGreen,
                        unfocusedBorderColor = MetroColors.DarkSlateBorder,
                        focusedContainerColor = MetroColors.DarkSlateDefault,
                        unfocusedContainerColor = MetroColors.DarkSlateDefault
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MetroColors.TextSecondary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            if (barangay.isNotBlank()) {
                                onSave(label, selectedCity, barangay, street)
                            }
                        },
                        enabled = barangay.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MetroColors.SafeGreen,
                            disabledContainerColor = MetroColors.DarkSlateBorder
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Create Watcher", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
