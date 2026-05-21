package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val id: String,
    val name: String,
    val avatarEmoji: String, // e.g. "🍿", "🦊", "🎞️"
    val avatarBgColor: Int, // Hex integer color
    val neighborhood: String, // Uptown, Oak Lawn, Deep Ellum, Bishop Arts, Lakewood, Lower Greenville
    val favoriteMovies: String, // Comma separated list of films
    val favoriteGenres: String, // Comma separated list (e.g. "Action, Drama, Sci-Fi")
    val letterboxdLink: String, // e.g. "https://letterboxd.com/username"
    val age: Int,
    val gender: String, // "Male", "Female", "Non-binary", etc.
    val relationshipStatus: String, // "Single", "Married", "In a Relationship"
    val isMe: Boolean = false // Marker for the logged-in user profile
) {
    fun getGenresList(): List<String> {
        if (favoriteGenres.isBlank()) return emptyList()
        return favoriteGenres.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Entity(tableName = "outing_requests")
data class OutingRequest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val creatorId: String,
    val creatorName: String,
    val creatorAvatarEmoji: String,
    val creatorAvatarBgColor: Int,
    val movieTitle: String,
    val theaterName: String, // AMC NorthPark 15, Texas Theatre, Landmark Inwood, Angelika, Violet Crown
    val showDate: String, // e.g. "Fri, May 22", "Sat, May 23"
    val showTime: String, // e.g. "7:30 PM", "9:00 PM"
    val afterPlan: String, // "Discussion at theater", "Grab burgers nearby", "Coffee & movie talk"
    val description: String = "", // Custom note
    val status: String = "Open" // "Open", "Matched", "Completed"
)

@Entity(tableName = "match_records")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val outingId: Int, // Reference to outing, or 0 if direct invite
    val buddyId: String, // The buddy's user profile ID
    val buddyName: String,
    val buddyAvatarEmoji: String,
    val buddyAvatarBgColor: Int,
    val movieTitle: String,
    val theaterName: String,
    val showDate: String,
    val showTime: String,
    val plan: String,
    val status: String = "Confirmed", // "Pending Confirmation", "Confirmed", "Completed"
    val chatHistory: String = "" // String format for local mock chat (sender|message\n...)
) {
    fun getMessages(): List<Pair<Boolean, String>> { // Boolean: true if sent by user, false if sent by buddy
        if (chatHistory.isBlank()) return emptyList()
        return chatHistory.trim().split("\n").mapNotNull {
            val parts = it.split("|", limit = 2)
            if (parts.size == 2) {
                val isMe = parts[0] == "me"
                isMe to parts[1]
            } else {
                null
            }
        }
    }
}
