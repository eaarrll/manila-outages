package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class OutageRepository(private val context: Context) {

    private val database: AppDatabase by lazy {
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "manila_outages_db"
        ).build()
    }

    private val dao: OutageDao by lazy { database.outageDao() }

    val allOutagesFlow: Flow<List<OutageEntity>> = dao.getAllOutagesFlow()
    val scheduledOutagesFlow: Flow<List<OutageEntity>> = dao.getScheduledOutagesFlow()
    val crowdsourcedOutagesFlow: Flow<List<OutageEntity>> = dao.getCrowdsourcedOutagesFlow()
    val userAddressesFlow: Flow<List<UserAddressEntity>> = dao.getAllAddressesFlow()

    init {
        // Run pre-seeding asynchronously in IO scope
        CoroutineScope(Dispatchers.IO).launch {
            try {
                preseedDataIfNeeded()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private suspend fun preseedDataIfNeeded() {
        val currentOutages = dao.getAllOutagesFlow().first()
        if (currentOutages.isEmpty()) {
            val now = System.currentTimeMillis()
            val hour = 3600000L
            val day = 86400000L

            val seeds = listOf(
                // Meralco Scheduled Outages (Power Maintenance)
                OutageEntity(
                    type = "POWER",
                    provider = "MERALCO",
                    isScheduled = true,
                    title = "Line Reconstruction & Maintenance",
                    scheduledStart = now + 4 * hour, // Starts in 4 hours
                    scheduledEnd = now + 10 * hour, // Ends in 10 hours
                    city = "Quezon City",
                    barangay = "Loyola Heights",
                    streets = "Katipunan Avenue (Northbound), Esteban Abada St., F. Dela Rosa St., Loyola Subd.",
                    details = "Replacement of rotten wooden poles and installation of auxiliary line equipment along Katipunan Ave., near Ateneo Gate 3.",
                    reportedAt = now - 2 * day,
                    reportedBy = "Meralco Advisory",
                    isVerified = true
                ),
                OutageEntity(
                    type = "POWER",
                    provider = "MERALCO",
                    isScheduled = true,
                    title = "System Upgrading & Substation Work",
                    scheduledStart = now + 1 * day + 2 * hour, // Tomorrow morning
                    scheduledEnd = now + 1 * day + 8 * hour,
                    city = "Pasig",
                    barangay = "San Antonio",
                    streets = "Emerald Avenue, Onyx Street, Sapphire Road, portions of Meralco Avenue",
                    details = "Converting lines from 13.2kV to 34.5kV for better voltage performance in Ortigas Center business area.",
                    reportedAt = now - 1 * day,
                    reportedBy = "Meralco Advisory",
                    isVerified = true
                ),
                OutageEntity(
                    type = "POWER",
                    provider = "MERALCO",
                    isScheduled = true,
                    title = "Primary Sector Line Maintenance",
                    scheduledStart = now + 2 * day, // Day after tomorrow
                    scheduledEnd = now + 2 * day + 6 * hour,
                    city = "Taguig",
                    barangay = "Fort Bonifacio",
                    streets = "5th Avenue, 26th Street, BGC Corporate Center, Central Plaza Area",
                    details = "Preventive electrical equipment inspection and overhead wire upgrades to prevent line tripping in BGC commercial hub.",
                    reportedAt = now - 12 * hour,
                    reportedBy = "Meralco Advisory",
                    isVerified = true
                ),

                // Maynilad Water Scheduled Interruptions
                OutageEntity(
                    type = "WATER",
                    provider = "MAYNILAD",
                    isScheduled = true,
                    title = "Network Upgrades & Pressure Valve Maintenance",
                    scheduledStart = now + 6 * hour,
                    scheduledEnd = now + 14 * hour,
                    city = "Parañaque",
                    barangay = "BF Homes",
                    streets = "BF Homes Phase 3 (Placid Place, Joy Street), Phase 4 (Elizalde St., El Grande Ave.)",
                    details = "Installing new Pressure Reducing Valves (PRV) to streamline pressure levels and minimize underground distribution line ruptures.",
                    reportedAt = now - 1 * day,
                    reportedBy = "Maynilad Post",
                    isVerified = true
                ),
                OutageEntity(
                    type = "WATER",
                    provider = "MAYNILAD",
                    isScheduled = true,
                    title = "Interconnection of Newly Laid Pipeline",
                    scheduledStart = now + 26 * hour, // Tomorrow afternoon
                    scheduledEnd = now + 34 * hour,
                    city = "Manila",
                    barangay = "Malate",
                    streets = "Taft Avenue corridor, Leon Guinto St., Quirino Highway, parts of San Andres",
                    details = "Interconnecting the new 300mm distribution pipe along Leon Guinto corner Quirino Avenue to decommission leak-prone old primary mains.",
                    reportedAt = now - 18 * hour,
                    reportedBy = "Maynilad Water News",
                    isVerified = true
                ),

                // Manila Water Scheduled
                OutageEntity(
                    type = "WATER",
                    provider = "MANILA_WATER",
                    isScheduled = true,
                    title = "Emergency Pipe Layout Repair",
                    scheduledStart = now + 10 * hour,
                    scheduledEnd = now + 16 * hour,
                    city = "Marikina",
                    barangay = "Concepcion Uno",
                    streets = "Bayan-Bayanan Avenue, J. Molina Street, E. Santos St., Twin River Subd.",
                    details = "Sewer alignment inspection and direct main pipe leakage sealing to resolve low pressure outputs in local households.",
                    reportedAt = now - 3 * hour,
                    reportedBy = "Manila Water Advisory",
                    isVerified = true
                ),

                // Crowdsourced Unscheduled Reports
                OutageEntity(
                    type = "POWER",
                    provider = "MERALCO",
                    isScheduled = false,
                    title = "Sudden Power Interruption / Ground Explosion",
                    scheduledStart = 0L,
                    scheduledEnd = 0L,
                    city = "Makati",
                    barangay = "Poblacion",
                    streets = "Makati Avenue corner Kalayaan Ave, P. Burgos St, General Luna",
                    details = "Hear a loud bang from a utility electrical transformer near the local convenience store around 7 PM. Entire block lost absolute electrical power immediately! Appreciate updates.",
                    reportedAt = now - 45 * 60000, // 45 mins ago
                    reportedBy = "WFH_Pro_Ph",
                    upvotes = 12,
                    isVerified = true,
                    userUpvotedList = "system_pre_vote_1"
                ),
                OutageEntity(
                    type = "WATER",
                    provider = "MAYNILAD",
                    isScheduled = false,
                    title = "Total Water Interruption / No Flow",
                    scheduledStart = 0L,
                    scheduledEnd = 0L,
                    city = "Muntinlupa",
                    barangay = "Alabang",
                    streets = "Filinvest City corporate center, Civic Drive, Palms Pointe",
                    details = "Water supply completely slowed to an ultimate trickle and shut down. No primary official advisory from Maynilad page. Anyone else experiencing this right now around Civic Drive?",
                    reportedAt = now - 3 * hour,
                    reportedBy = "MuntinlupaLocal",
                    upvotes = 7,
                    isVerified = false,
                    userUpvotedList = "system_pre_vote_2"
                ),
                OutageEntity(
                    type = "POWER",
                    provider = "MERALCO",
                    isScheduled = false,
                    title = "Voltage Fluctuations & Sagging Lines",
                    scheduledStart = 0L,
                    scheduledEnd = 0L,
                    city = "Quezon City",
                    barangay = "Diliman",
                    streets = "Malingap Street, Maginhawa St., Sikatuna Village",
                    details = "Household lights are cycling dim & bright repeatedly. Afraid to turn on computers or refrigerators! This has been going on for over an hour. Already raised to hotline.",
                    reportedAt = now - 2 * hour,
                    reportedBy = "TechieInQC",
                    upvotes = 4,
                    isVerified = false,
                    userUpvotedList = ""
                )
            )

            seeds.forEach { dao.insertOutage(it) }
        }

        val currentAddresses = dao.getAllAddressesFlow().first()
        if (currentAddresses.isEmpty()) {
            val defaultAddresses = listOf(
                UserAddressEntity(
                    label = "Home 🏠",
                    city = "Quezon City",
                    barangay = "Loyola Heights",
                    street = "Esteban Abada St."
                ),
                UserAddressEntity(
                    label = "Office 🏢",
                    city = "Pasig",
                    barangay = "San Antonio",
                    street = "Sapphire Road"
                )
            )
            defaultAddresses.forEach { dao.insertAddress(it) }
        }
    }

    suspend fun insertOutage(outage: OutageEntity): Long = withContext(Dispatchers.IO) {
        dao.insertOutage(outage)
    }

    suspend fun deleteOutage(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteOutageById(id)
    }

    suspend fun upvoteOutage(id: Int, userId: String): Boolean = withContext(Dispatchers.IO) {
        val outage = dao.getOutageById(id) ?: return@withContext false
        val currentList = outage.userUpvotedList.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        if (currentList.contains(userId)) {
            // Already upvoted
            false
        } else {
            val newList = if (outage.userUpvotedList.isEmpty()) userId else "${outage.userUpvotedList},$userId"
            val updated = outage.copy(
                upvotes = outage.upvotes + 1,
                userUpvotedList = newList,
                // Automatically verify if a report gets 6 or more crowdsourced upvotes
                isVerified = outage.isVerified || (outage.upvotes + 1 >= 6)
            )
            dao.updateOutage(updated)
            true
        }
    }

    suspend fun getAlertStatusForAddress(city: String, barangay: String): AddressAlertResult = withContext(Dispatchers.IO) {
        val activeOutages = dao.getMatchingOutages(city, barangay)
        val now = System.currentTimeMillis()

        val scheduledUpcoming = activeOutages.filter {
            it.isScheduled && it.scheduledStart > now
        }
        val scheduledActive = activeOutages.filter {
            it.isScheduled && now in it.scheduledStart..it.scheduledEnd
        }
        val unscheduledActive = activeOutages.filter {
            !it.isScheduled && (now - it.reportedAt < 12 * 3600000L) // reported within last 12 hours
        }

        AddressAlertResult(
            scheduledUpcoming = scheduledUpcoming,
            scheduledActive = scheduledActive,
            unscheduledActive = unscheduledActive
        )
    }

    suspend fun insertUserAddress(address: UserAddressEntity) = withContext(Dispatchers.IO) {
        dao.insertAddress(address)
    }

    suspend fun deleteUserAddress(id: Int) = withContext(Dispatchers.IO) {
        dao.deleteAddressById(id)
    }
}

data class AddressAlertResult(
    val scheduledUpcoming: List<OutageEntity>,
    val scheduledActive: List<OutageEntity>,
    val unscheduledActive: List<OutageEntity>
) {
    val totalCount: Int
        get() = scheduledUpcoming.size + scheduledActive.size + unscheduledActive.size

    val maxSeverityState: String
        get() = when {
            scheduledActive.isNotEmpty() || unscheduledActive.isNotEmpty() -> "ACTIVE"
            scheduledUpcoming.isNotEmpty() -> "UPCOMING"
            else -> "SAFE"
        }
}
