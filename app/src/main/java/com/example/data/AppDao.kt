package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieBuddyDao {
    // Profiling Queries
    @Query("SELECT * FROM user_profiles WHERE isMe = 1 LIMIT 1")
    fun getMyProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE isMe = 1 LIMIT 1")
    suspend fun getMyProfileDirect(): UserProfile?

    @Query("SELECT * FROM user_profiles WHERE isMe = 0")
    fun getOtherBuddiesFlow(): Flow<List<UserProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProfile(profile: UserProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(profiles: List<UserProfile>)

    // Outing Queries
    @Query("SELECT * FROM outing_requests WHERE status = 'Open' ORDER BY id DESC")
    fun getActiveOutingsFlow(): Flow<List<OutingRequest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOuting(outing: OutingRequest)

    @Query("DELETE FROM outing_requests WHERE id = :outingId")
    suspend fun deleteOuting(outingId: Int)

    // Match Record Queries
    @Query("SELECT * FROM match_records ORDER BY id DESC")
    fun getMatchesFlow(): Flow<List<MatchRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMatch(match: MatchRecord)

    @Update
    suspend fun updateMatch(match: MatchRecord)

    @Query("SELECT COUNT(*) FROM user_profiles")
    suspend fun getProfilesCount(): Int
}
