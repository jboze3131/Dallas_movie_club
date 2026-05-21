package com.example.data

import android.graphics.Color
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull

class MovieBuddyRepository(private val dao: MovieBuddyDao) {

    val myProfile: Flow<UserProfile?> = dao.getMyProfileFlow()
    val otherBuddies: Flow<List<UserProfile>> = dao.getOtherBuddiesFlow()
    val activeOutings: Flow<List<OutingRequest>> = dao.getActiveOutingsFlow()
    val allMatches: Flow<List<MatchRecord>> = dao.getMatchesFlow()

    suspend fun saveProfile(profile: UserProfile) {
        dao.insertOrUpdateProfile(profile)
    }

    suspend fun addOuting(outing: OutingRequest) {
        dao.insertOuting(outing)
    }

    suspend fun removeOuting(outingId: Int) {
        dao.deleteOuting(outingId)
    }

    suspend fun addMatch(match: MatchRecord) {
        dao.insertMatch(match)
    }

    suspend fun updateMatch(match: MatchRecord) {
        dao.updateMatch(match)
    }

    // Pre-populates the database with real local profiles of people looking for movie buddies in Dallas.
    suspend fun initializeDatabaseIfEmpty() {
        val count = dao.getProfilesCount()
        if (count == 0) {
            // 1. Create default profile for the user themselves
            val me = UserProfile(
                id = "me_user",
                name = "Alex Vance",
                avatarEmoji = "🍿",
                avatarBgColor = 0xFF6366F1.toInt(), // Indigo
                neighborhood = "Lower Greenville",
                favoriteMovies = "La La Land, Everything Everywhere All At Once, Dune, Pulp Fiction",
                favoriteGenres = "Sci-Fi, Drama, Indie, Romance",
                letterboxdLink = "https://letterboxd.com/alexvance_m",
                age = 26,
                gender = "Male",
                relationshipStatus = "Single",
                isMe = true
            )
            dao.insertOrUpdateProfile(me)

            // 2. Prep some Dallas Movie Buddies
            val buddies = listOf(
                UserProfile(
                    id = "buddy_1",
                    name = "Sarah Jenkins",
                    avatarEmoji = "🦊",
                    avatarBgColor = 0xFFF43F5E.toInt(), // Rose
                    neighborhood = "Bishop Arts District",
                    favoriteMovies = "Past Lives, Parasite, Portrait of a Lady on Fire, Challengers",
                    favoriteGenres = "Indie, Drama, Romance",
                    letterboxdLink = "https://letterboxd.com/sara_j_cinema",
                    age = 28,
                    gender = "Female",
                    relationshipStatus = "Single",
                    isMe = false
                ),
                UserProfile(
                    id = "buddy_2",
                    name = "Marcus Thornton",
                    avatarEmoji = "🎞️",
                    avatarBgColor = 0xFF10B981.toInt(), // Emerald
                    neighborhood = "Uptown",
                    favoriteMovies = "The Dark Knight, Interstellar, Mad Max: Fury Road, Heat",
                    favoriteGenres = "Action, Sci-Fi, Thriller",
                    letterboxdLink = "https://letterboxd.com/marcus_t_reviews",
                    age = 31,
                    gender = "Male",
                    relationshipStatus = "Married",
                    isMe = false
                ),
                UserProfile(
                    id = "buddy_3",
                    name = "Chloe Garza",
                    avatarEmoji = "🌟",
                    avatarBgColor = 0xFFF59E0B.toInt(), // Amber
                    neighborhood = "Deep Ellum",
                    favoriteMovies = "Midsommar, Hereditary, Alien, Perfect Blue",
                    favoriteGenres = "Horror, Mystery, Indie",
                    letterboxdLink = "https://letterboxd.com/chloeg_horrorgirl",
                    age = 24,
                    gender = "Female",
                    relationshipStatus = "Single",
                    isMe = false
                ),
                UserProfile(
                    id = "buddy_4",
                    name = "Devon Miller",
                    avatarEmoji = "🎸",
                    avatarBgColor = 0xFF8B5CF6.toInt(), // Violet
                    neighborhood = "Oak Lawn",
                    favoriteMovies = "Whiplash, Baby Driver, Amadeus, Almost Famous",
                    favoriteGenres = "Drama, Comedy, Documentary",
                    letterboxdLink = "https://letterboxd.com/devon_m_sound",
                    age = 29,
                    gender = "Non-binary",
                    relationshipStatus = "In a Relationship",
                    isMe = false
                ),
                UserProfile(
                    id = "buddy_5",
                    name = "Elena Rostova",
                    avatarEmoji = "🎨",
                    avatarBgColor = 0xFF06B6D4.toInt(), // Cyan
                    neighborhood = "Lakewood",
                    favoriteMovies = "Spirited Away, Amélie, Grand Budapest Hotel, Anatomy of a Fall",
                    favoriteGenres = "Indie, Comedy, Drama, Animation",
                    letterboxdLink = "https://letterboxd.com/elena_lakewood",
                    age = 34,
                    gender = "Female",
                    relationshipStatus = "Married",
                    isMe = false
                ),
                UserProfile(
                    id = "buddy_6",
                    name = "Jordan Brooks",
                    avatarEmoji = "🦁",
                    avatarBgColor = 0xFFEC4899.toInt(), // Pink
                    neighborhood = "Knox-Henderson",
                    favoriteMovies = "Blade Runner 2049, Arrival, Matrix, Her",
                    favoriteGenres = "Sci-Fi, Mystery, Romance",
                    letterboxdLink = "https://letterboxd.com/jordanb_retro",
                    age = 27,
                    gender = "Male",
                    relationshipStatus = "Single",
                    isMe = false
                )
            )
            dao.insertProfiles(buddies)

            // 3. Post a couple of active buddy outings in Dallas theaters
            val outings = listOf(
                OutingRequest(
                    creatorId = "buddy_1",
                    creatorName = "Sarah Jenkins",
                    creatorAvatarEmoji = "🦊",
                    creatorAvatarBgColor = 0xFFF43F5E.toInt(),
                    movieTitle = "Anora",
                    theaterName = "Texas Theatre (Oak Cliff)",
                    showDate = "Friday, May 22",
                    showTime = "7:15 PM",
                    afterPlan = "We can head to Whitehall Lanes or a cafe around Bishop Arts right after!",
                    description = "Super excited to catch the new Sean Baker film at the iconic Texas Theatre. Let's grab central row seats!",
                    status = "Open"
                ),
                OutingRequest(
                    creatorId = "buddy_2",
                    creatorName = "Marcus Thornton",
                    creatorAvatarEmoji = "🎞️",
                    creatorAvatarBgColor = 0xFF10B981.toInt(),
                    movieTitle = "Dune: Part Two (IMAX Re-release)",
                    theaterName = "AMC NorthPark 15",
                    showDate = "Saturday, May 23",
                    showTime = "6:30 PM",
                    afterPlan = "Quick debrief outside the IMAX lobby and grab coffee at NorthPark",
                    description = "Watching Dune on the giant NorthPark IMAX screen. I have seat J14! Who is down to sit together?",
                    status = "Open"
                ),
                OutingRequest(
                    creatorId = "buddy_5",
                    creatorName = "Elena Rostova",
                    creatorAvatarEmoji = "🎨",
                    creatorAvatarBgColor = 0xFF06B6D4.toInt(),
                    movieTitle = "The Zone of Interest",
                    theaterName = "Angelika Film Center (Mockingbird Station)",
                    showDate = "Sunday, May 24",
                    showTime = "4:45 PM",
                    afterPlan = "Dinner at Mockingbird Station to discuss the profound editing and sound design.",
                    description = "Looking for a thoughtful buddy to watch and dissect this award-winning film. Mature and respectful only.",
                    status = "Open"
                )
            )
            for (outing in outings) {
                dao.insertOuting(outing)
            }
        }
    }
}
