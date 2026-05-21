package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.MovieBuddyApplication
import com.example.data.MatchRecord
import com.example.data.MovieBuddyRepository
import com.example.data.OutingRequest
import com.example.data.UserProfile
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.random.Random

class MovieBuddyViewModel(
    application: Application,
    private val repository: MovieBuddyRepository
) : AndroidViewModel(application) {

    // Main Flows from DB
    val myProfile: StateFlow<UserProfile?> = repository.myProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    private val _otherBuddies = repository.otherBuddies.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeOutings: StateFlow<List<OutingRequest>> = repository.activeOutings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allMatches: StateFlow<List<MatchRecord>> = repository.allMatches.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Navigation and UI state
    private val _currentTab = MutableStateFlow("buddies") // "buddies", "outings", "matches", "profile"
    val currentTab: StateFlow<String> = _currentTab.asStateFlow()

    // Filters for finding buddies
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _selectedGenre = MutableStateFlow("All")
    val selectedGenre = _selectedGenre.asStateFlow()

    private val _selectedNeighborhood = MutableStateFlow("All")
    val selectedNeighborhood = _selectedNeighborhood.asStateFlow()

    private val _selectedGender = MutableStateFlow("All")
    val selectedGender = _selectedGender.asStateFlow()

    private val _selectedStatus = MutableStateFlow("All")
    val selectedStatus = _selectedStatus.asStateFlow()

    private val _ageRange = MutableStateFlow(18f..60f)
    val ageRange = _ageRange.asStateFlow()

    // Filtered buddies flow
    val filteredBuddies: StateFlow<List<UserProfile>> = combine(
        _otherBuddies,
        _searchQuery,
        _selectedGenre,
        _selectedNeighborhood,
        _selectedGender,
        _selectedStatus,
        _ageRange
    ) { flowsArray ->
        @Suppress("UNCHECKED_CAST")
        val buddies = flowsArray[0] as List<UserProfile>
        val query = flowsArray[1] as String
        val genre = flowsArray[2] as String
        val neighborhood = flowsArray[3] as String
        val gender = flowsArray[4] as String
        val status = flowsArray[5] as String
        @Suppress("UNCHECKED_CAST")
        val ageRange = flowsArray[6] as ClosedFloatingPointRange<Float>

        buddies.filter { buddy ->
            val matchesQuery = query.isBlank() || buddy.name.contains(query, ignoreCase = true) || 
                            buddy.favoriteMovies.contains(query, ignoreCase = true)
            val matchesGenre = genre == "All" || buddy.getGenresList().any { it.equals(genre, ignoreCase = true) }
            val matchesNeighborhood = neighborhood == "All" || buddy.neighborhood.equals(neighborhood, ignoreCase = true)
            val matchesGender = gender == "All" || buddy.gender.equals(gender, ignoreCase = true)
            val matchesStatus = status == "All" || buddy.relationshipStatus.equals(status, ignoreCase = true)
            val matchesAge = buddy.age >= ageRange.start && buddy.age <= ageRange.endInclusive

            matchesQuery && matchesGenre && matchesNeighborhood && matchesGender && matchesStatus && matchesAge
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Current focused buddy for details
    private val _selectedBuddyForDetail = MutableStateFlow<UserProfile?>(null)
    val selectedBuddyForDetail = _selectedBuddyForDetail.asStateFlow()

    // Create Outing Dialog Fields
    private val _showCreateOutingDialog = MutableStateFlow(false)
    val showCreateOutingDialog = _showCreateOutingDialog.asStateFlow()

    var nMovieTitle = MutableStateFlow("")
    var nTheaterName = MutableStateFlow("Texas Theatre (Oak Cliff)")
    var nShowDate = MutableStateFlow("Tonight")
    var nShowTime = MutableStateFlow("7:30 PM")
    var nAfterPlan = MutableStateFlow("Sit together & discuss over food")
    var nDescription = MutableStateFlow("")

    // Active Chat overlay
    private val _activeChatMatch = MutableStateFlow<MatchRecord?>(null)
    val activeChatMatch = _activeChatMatch.asStateFlow()

    private val _chatInputText = MutableStateFlow("")
    val chatInputText = _chatInputText.asStateFlow()

    // Profile Edits State
    private val _isEditingProfile = MutableStateFlow(false)
    val isEditingProfile = _isEditingProfile.asStateFlow()

    var editName = MutableStateFlow("")
    var editNeighborhood = MutableStateFlow("Lower Greenville")
    var editAge = MutableStateFlow(25)
    var editGender = MutableStateFlow("Male")
    var editRelationshipStatus = MutableStateFlow("Single")
    var editFavoriteMovies = MutableStateFlow("")
    var editFavoriteGenres = MutableStateFlow("")
    var editLetterboxd = MutableStateFlow("")
    var editAvatarEmoji = MutableStateFlow("🍿")
    var editAvatarBgColor = MutableStateFlow(0xFF6366F1.toInt())

    fun selectTab(tab: String) {
        _currentTab.value = tab
    }

    fun selectBuddy(buddy: UserProfile?) {
        _selectedBuddyForDetail.value = buddy
    }

    fun search(q: String) {
        _searchQuery.value = q
    }

    fun filterGenre(g: String) {
        _selectedGenre.value = g
    }

    fun filterNeighborhood(n: String) {
        _selectedNeighborhood.value = n
    }

    fun filterGender(g: String) {
        _selectedGender.value = g
    }

    fun filterStatus(s: String) {
        _selectedStatus.value = s
    }

    fun filterAgeRange(range: ClosedFloatingPointRange<Float>) {
        _ageRange.value = range
    }

    // Outing dialog control
    fun setShowOutingDialog(show: Boolean) {
        _showCreateOutingDialog.value = show
        if (show) {
            // clear form default values
            nMovieTitle.value = ""
            nDescription.value = ""
        }
    }

    fun postOuting() {
        val user = myProfile.value ?: return
        if (nMovieTitle.value.isBlank()) return

        viewModelScope.launch {
            val newOuting = OutingRequest(
                creatorId = user.id,
                creatorName = user.name,
                creatorAvatarEmoji = user.avatarEmoji,
                creatorAvatarBgColor = user.avatarBgColor,
                movieTitle = nMovieTitle.value,
                theaterName = nTheaterName.value,
                showDate = nShowDate.value,
                showTime = nShowTime.value,
                afterPlan = nAfterPlan.value,
                description = nDescription.value,
                status = "Open"
            )
            repository.addOuting(newOuting)
            setShowOutingDialog(false)
            _currentTab.value = "outings"
        }
    }

    // Dynamic Matching & Outing requests
    fun requestJoinOuting(outing: OutingRequest) {
        viewModelScope.launch {
            // Check if match already exists
            val existing = allMatches.value.any { it.outingId == outing.id }
            if (existing) return@launch

            // Compile initial message welcoming the user
            val initialChat = "buddy|Hey there! Thanks for requested to join me. I'm looking forward to watching \"${outing.movieTitle}\"! Are you ready to meet up at the ${outing.theaterName}?\n"

            val match = MatchRecord(
                outingId = outing.id,
                buddyId = outing.creatorId,
                buddyName = outing.creatorName,
                buddyAvatarEmoji = outing.creatorAvatarEmoji,
                buddyAvatarBgColor = outing.creatorAvatarBgColor,
                movieTitle = outing.movieTitle,
                theaterName = outing.theaterName,
                showDate = outing.showDate,
                showTime = outing.showTime,
                plan = outing.afterPlan,
                status = "Confirmed",
                chatHistory = initialChat
            )
            repository.addMatch(match)
            _currentTab.value = "matches"
        }
    }

    fun startDirectMatch(buddy: UserProfile) {
        viewModelScope.launch {
            val existing = allMatches.value.find { it.buddyId == buddy.id && it.outingId == 0 }
            if (existing != null) {
                openChat(existing)
                _currentTab.value = "matches"
                return@launch
            }

            val initialChat = "buddy|Hi Alex! I saw on Cinema Club that we both love ${buddy.getGenresList().firstOrNull() ?: "movies"}. Down to catch a movie in Dallas sometime soon? 🎬🍿\n"

            val match = MatchRecord(
                outingId = 0,
                buddyId = buddy.id,
                buddyName = buddy.name,
                buddyAvatarEmoji = buddy.avatarEmoji,
                buddyAvatarBgColor = buddy.avatarBgColor,
                movieTitle = "Upcoming Movie Title",
                theaterName = "TBD Dallas Theater",
                showDate = "TBD Date",
                showTime = "TBD Time",
                plan = "Grab food or drinks nearby and chat about movies",
                status = "Confirmed",
                chatHistory = initialChat
            )
            repository.addMatch(match)
            _selectedBuddyForDetail.value = null
            _currentTab.value = "matches"
        }
    }

    fun openChat(match: MatchRecord) {
        _activeChatMatch.value = match
        _chatInputText.value = ""
    }

    fun closeChat() {
        _activeChatMatch.value = null
    }

    fun setChatInput(text: String) {
        _chatInputText.value = text
    }

    fun sendChatMessage() {
        val message = _chatInputText.value.trim()
        val currentMatch = _activeChatMatch.value ?: return
        if (message.isBlank()) return

        viewModelScope.launch {
            val updatedHistory = currentMatch.chatHistory + "me|$message\n"
            val updatedMatch = currentMatch.copy(chatHistory = updatedHistory)
            
            repository.updateMatch(updatedMatch)
            _activeChatMatch.value = updatedMatch
            _chatInputText.value = ""

            // Trigger a realistic friendly delayed buddy reply
            delay(1200)
            
            val replies = listOf(
                "That sounds great! AMC NorthPark works perfectly for me. See you there!",
                "Love that movie! I am so glad I found someone who shares this passion. What row are we sitting in?",
                "Texas Theatre has the best popcorn in Dallas, absolutely down to sit together! Let's get burgers at Crater's or something afterwards too.",
                "Haha agree, the ending is definitely going to require a long discussion. Let's make sure we get drinks at West Village!",
                "Perfect! We are matching up nicely. I will book my ticket nearby, thanks Alex!",
                "Amazing! See you in Uptown Dallas soon!"
            )
            val randomReply = replies[Random.nextInt(replies.size)]
            val finalHistory = updatedHistory + "buddy|$randomReply\n"
            val finalMatch = updatedMatch.copy(chatHistory = finalHistory)
            
            repository.updateMatch(finalMatch)
            // Only update current chat is still focused on the same match
            if (_activeChatMatch.value?.id == currentMatch.id) {
                _activeChatMatch.value = finalMatch
            }
        }
    }

    // Profile Actions
    fun enterProfileEdit() {
        val user = myProfile.value ?: return
        editName.value = user.name
        editNeighborhood.value = user.neighborhood
        editAge.value = user.age
        editGender.value = user.gender
        editRelationshipStatus.value = user.relationshipStatus
        editFavoriteMovies.value = user.favoriteMovies
        editFavoriteGenres.value = user.favoriteGenres
        editLetterboxd.value = user.letterboxdLink
        editAvatarEmoji.value = user.avatarEmoji
        editAvatarBgColor.value = user.avatarBgColor
        _isEditingProfile.value = true
    }

    fun cancelProfileEdit() {
        _isEditingProfile.value = false
    }

    fun saveProfileEdit() {
        if (editName.value.isBlank()) return
        viewModelScope.launch {
            val user = myProfile.value ?: return@launch
            val updated = user.copy(
                name = editName.value.trim(),
                neighborhood = editNeighborhood.value,
                age = editAge.value,
                gender = editGender.value,
                relationshipStatus = editRelationshipStatus.value,
                favoriteMovies = editFavoriteMovies.value.trim(),
                favoriteGenres = editFavoriteGenres.value.trim(),
                letterboxdLink = editLetterboxd.value.trim(),
                avatarEmoji = editAvatarEmoji.value,
                avatarBgColor = editAvatarBgColor.value
            )
            repository.saveProfile(updated)
            _isEditingProfile.value = false
        }
    }

    fun setEditAvatar(emoji: String, color: Int) {
        editAvatarEmoji.value = emoji
        editAvatarBgColor.value = color
    }
}

class ViewModelFactory(
    private val application: Application,
    private val repository: MovieBuddyRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MovieBuddyViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MovieBuddyViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
