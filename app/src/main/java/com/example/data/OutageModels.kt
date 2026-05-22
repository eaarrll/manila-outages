package com.example.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "outages")
data class OutageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "POWER" or "WATER"
    val provider: String, // "MERALCO", "MAYNILAD", "MANILA_WATER"
    val isScheduled: Boolean, // true = announced schedule, false = unscheduled user-reported
    val title: String, // e.g. "Maintenance, Emergency leak repair"
    val scheduledStart: Long, // timestamp, 0 if unscheduled
    val scheduledEnd: Long, // timestamp, 0 if unscheduled
    val city: String, // e.g. "Quezon City", "Pasig"
    val barangay: String, // e.g. "Loyola Heights"
    val streets: String, // Specific streets/subdivisions affected
    val details: String, // Explanation of interruption
    val reportedAt: Long, // Timestamp of report/entry
    val reportedBy: String, // "Provider Announcement" or anonymized/user
    val upvotes: Int = 0, // Verification counter for crowdsourced reports
    val isVerified: Boolean = false, // True if utility confirmed or high upvotes
    val userUpvotedList: String = "" // Comma-separated user IDs/IPs to prevent double voting
)

@Entity(tableName = "user_addresses")
data class UserAddressEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val label: String, // e.g. "Home", "Office", "Café"
    val city: String, // Metro Manila city
    val barangay: String, // Barangay
    val street: String // Street details
)

@Dao
interface OutageDao {
    @Query("SELECT * FROM outages ORDER BY reportedAt DESC")
    fun getAllOutagesFlow(): Flow<List<OutageEntity>>

    @Query("SELECT * FROM outages WHERE isScheduled = 1 ORDER BY scheduledStart ASC")
    fun getScheduledOutagesFlow(): Flow<List<OutageEntity>>

    @Query("SELECT * FROM outages WHERE isScheduled = 0 ORDER BY reportedAt DESC")
    fun getCrowdsourcedOutagesFlow(): Flow<List<OutageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOutage(outage: OutageEntity): Long

    @Update
    suspend fun updateOutage(outage: OutageEntity)

    @Query("DELETE FROM outages WHERE id = :id")
    suspend fun deleteOutageById(id: Int)

    @Query("SELECT * FROM outages WHERE id = :id LIMIT 1")
    suspend fun getOutageById(id: Int): OutageEntity?

    // Address-specific direct alerts matching (simple string containment or exact matches)
    @Query("SELECT * FROM outages WHERE LOWER(city) = LOWER(:city) AND (LOWER(barangay) = LOWER(:barangay) OR :barangay = '' OR LOWER(streets) LIKE '%' || LOWER(:barangay) || '%')")
    suspend fun getMatchingOutages(city: String, barangay: String): List<OutageEntity>

    // Alert User Address queries
    @Query("SELECT * FROM user_addresses ORDER BY id ASC")
    fun getAllAddressesFlow(): Flow<List<UserAddressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAddress(address: UserAddressEntity): Long

    @Query("DELETE FROM user_addresses WHERE id = :id")
    suspend fun deleteAddressById(id: Int)
}

@Database(entities = [OutageEntity::class, UserAddressEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun outageDao(): OutageDao
}
