package com.example

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.MatchRecord
import com.example.data.OutingRequest
import com.example.data.UserProfile
import com.example.ui.MovieBuddyViewModel
import com.example.ui.ViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MovieBuddyApplication
        val viewModel: MovieBuddyViewModel by viewModels {
            ViewModelFactory(application, app.repository)
        }

        setContent {
            // Force premium cinematic dark theme colors for true theater feel
            val cinemaColorScheme = darkColorScheme(
                primary = Color(0xFFE50914), // Velvet red
                onPrimary = Color.White,
                primaryContainer = Color(0xFF8C0009),
                secondary = Color(0xFFFFB300), // Neon golden/marquee yellow
                onSecondary = Color.Black,
                background = Color(0xFF121214), // Midnight charcoal
                onBackground = Color(0xFFECEFF1),
                surface = Color(0xFF1E1E22), // Lighter charcoal for cards
                onSurface = Color(0xFFECEFF1),
                surfaceVariant = Color(0xFF2A2A2E),
                onSurfaceVariant = Color(0xFFCFD8DC)
            )

            MaterialTheme(
                colorScheme = cinemaColorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: MovieBuddyViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val showCreateOutingDialog by viewModel.showCreateOutingDialog.collectAsStateWithLifecycle()
    val activeChatMatch by viewModel.activeChatMatch.collectAsStateWithLifecycle()
    val selectedBuddy by viewModel.selectedBuddyForDetail.collectAsStateWithLifecycle()

    // Handle system back press
    BackHandler(enabled = activeChatMatch != null || selectedBuddy != null) {
        if (activeChatMatch != null) {
            viewModel.closeChat()
        } else if (selectedBuddy != null) {
            viewModel.selectBuddy(null)
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBarCinema()
        },
        bottomBar = {
            NavigationBarCinema(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) }
            )
        },
        floatingActionButton = {
            if (currentTab == "outings") {
                FloatingActionButton(
                    onClick = { viewModel.setShowOutingDialog(true) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("create_outing_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Post Outing Plan")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Display main view content depending on selected tab
            when (currentTab) {
                "buddies" -> BuddiesTab(viewModel)
                "outings" -> OutingsTab(viewModel)
                "matches" -> MatchesTab(viewModel)
                "profile" -> ProfileTab(viewModel)
            }

            // Buddy Detail Sheet (Overlay Dialog)
            selectedBuddy?.let { buddy ->
                BuddyDetailOverlay(
                    buddy = buddy,
                    onClose = { viewModel.selectBuddy(null) },
                    onStartMatch = { viewModel.startDirectMatch(buddy) }
                )
            }

            // Active Chat screen (Full screen/Overlay)
            activeChatMatch?.let { match ->
                ChatRoomOverlay(
                    match = match,
                    viewModel = viewModel,
                    onClose = { viewModel.closeChat() }
                )
            }

            // Create Outing dialog overlay
            if (showCreateOutingDialog) {
                CreateOutingDialog(viewModel)
            }
        }
    }
}

// ==========================================
// SHARED UI COMPONENTS
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopAppBarCinema() {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color(0xFF0A0A0C),
            titleContentColor = Color.White
        ),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "📽️ CINEMA CLUB",
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        letterSpacing = 1.5.sp,
                        color = Color(0xFFFFB300) // Marquee yellow
                    )
                }
                Text(
                    text = "DALLAS, TEXAS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    )
}

@Composable
fun NavigationBarCinema(
    currentTab: String,
    onTabSelected: (String) -> Unit
) {
    NavigationBar(
        containerColor = Color(0xFF0A0A0C),
        tonalElevation = 8.dp
    ) {
        val items = listOf(
            Triple("buddies", "Dallas Buddies", Icons.Default.Group),
            Triple("outings", "Showtimes", Icons.Default.Theaters),
            Triple("matches", "Matches", Icons.Default.Forum),
            Triple("profile", "My Profile", Icons.Default.Person)
        )

        items.forEach { (tab, label, icon) ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onTabSelected(tab) },
                icon = {
                    Icon(
                        imageVector = icon,
                        contentDescription = label,
                        modifier = Modifier.testTag("nav_tab_$tab")
                    )
                },
                label = {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    indicatorColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

@Composable
fun AvatarView(
    emoji: String,
    bgColorInt: Int,
    size: Int = 50,
    fontSize: Int = 22
) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Color(bgColorInt)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = emoji,
            fontSize = fontSize.sp
        )
    }
}

// ==========================================
// TAB 1: BUDDIES SEARCH & FILTER SCREEN
// ==========================================

@Composable
fun BuddiesTab(viewModel: MovieBuddyViewModel) {
    val buddies by viewModel.filteredBuddies.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedGenre by viewModel.selectedGenre.collectAsStateWithLifecycle()
    val selectedNeighborhood by viewModel.selectedNeighborhood.collectAsStateWithLifecycle()
    val selectedGender by viewModel.selectedGender.collectAsStateWithLifecycle()
    val selectedStatus by viewModel.selectedStatus.collectAsStateWithLifecycle()
    val ageRange by viewModel.ageRange.collectAsStateWithLifecycle()

    var showAdvancedFilters by remember { mutableStateOf(false) }

    val genres = listOf("All", "Action", "Drama", "Sci-Fi", "Comedy", "Indie", "Horror", "Mystery", "Romance", "Animation")
    val neighborhoods = listOf("All", "Lower Greenville", "Bishop Arts District", "Uptown", "Deep Ellum", "Lakewood", "Oak Lawn", "Knox-Henderson")
    val genders = listOf("All", "Male", "Female", "Non-binary")
    val statuses = listOf("All", "Single", "Married", "In a Relationship")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Search text field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.search(it) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_field"),
            placeholder = { Text("Search by name, favorite movies...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { showAdvancedFilters = !showAdvancedFilters }) {
                    Icon(
                        imageVector = if (showAdvancedFilters) Icons.Default.FilterListOff else Icons.Default.FilterList,
                        contentDescription = "Toggle Filters",
                        tint = if (showAdvancedFilters) MaterialTheme.colorScheme.primary else Color.White
                    )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
            )
        )

        // Advanced filter expander
        AnimatedVisibility(
            visible = showAdvancedFilters,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "FILTER FINDERS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Gender filter Row
                    Text("Gender:", fontSize = 12.sp, color = Color.Gray)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(genders) { g ->
                            val isSelected = selectedGender == g
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f))
                                    .clickable { viewModel.filterGender(g) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = g,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Status filter Row
                    Text("Relationship Status:", fontSize = 12.sp, color = Color.Gray)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(statuses) { s ->
                            val isSelected = selectedStatus == s
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.4f))
                                    .clickable { viewModel.filterStatus(s) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = s,
                                    fontSize = 11.sp,
                                    color = Color.White,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Age slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Age Range: ${ageRange.start.toInt()} - ${ageRange.endInclusive.toInt()}", fontSize = 12.sp)
                    }
                    RangeSlider(
                        value = ageRange,
                        onValueChange = { viewModel.filterAgeRange(it) },
                        valueRange = 18f..70f,
                        colors = SliderDefaults.colors(
                            thumbColor = MaterialTheme.colorScheme.primary,
                            activeTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal list of Genre scroll filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(genres) { genre ->
                val isSelected = selectedGenre == genre
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(32.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1E1E22))
                        .clickable { viewModel.filterGenre(genre) }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = genre,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Horizontal list of Dallas Neighborhood scroll filters
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(neighborhoods) { n ->
                val isSelected = selectedNeighborhood == n
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.secondary else Color(0xFF2A2A2E))
                        .clickable { viewModel.filterNeighborhood(n) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (n == "All") "📍 All Dallas" else "📍 $n",
                        color = if (isSelected) Color.Black else Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Buddies List
        if (buddies.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍿", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No movie buddies match your filters.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Text(
                        "Try expanding your neighborhood or age settings!",
                        color = Color.Gray.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(buddies) { buddy ->
                    BuddyListItem(
                        buddy = buddy,
                        onClick = { viewModel.selectBuddy(buddy) }
                    )
                }
            }
        }
    }
}

@Composable
fun BuddyListItem(
    buddy: UserProfile,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("buddy_card_${buddy.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(emoji = buddy.avatarEmoji, bgColorInt = buddy.avatarBgColor, size = 52)
            
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = buddy.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "${buddy.age} • ${buddy.gender}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Dallas Area",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = buddy.neighborhood,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${buddy.relationshipStatus}",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "Likes: ${buddy.favoriteMovies}",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Genres tags
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    buddy.getGenresList().take(3).forEach { g ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color.White.copy(alpha = 0.1f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(g, fontSize = 9.sp, color = Color.LightGray)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}

// ==========================================
// TAB 2: ACTIVE MOVIE OUTINGS IN DALLAS
// ==========================================

@Composable
fun OutingsTab(viewModel: MovieBuddyViewModel) {
    val outings by viewModel.activeOutings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Dallas Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🇺🇸", fontSize = 24.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "DALLAS CINEMA COMPANIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Browse postings from buddies wanting to see specific films this week.",
                    fontSize = 11.sp,
                    color = Color.LightGray
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (outings.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🎟️", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No movie outings scheduled right now.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Tap the button below to schedule yours first!",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(outings) { outing ->
                    OutingCardItem(
                        outing = outing,
                        onJoin = { viewModel.requestJoinOuting(outing) }
                    )
                }
            }
        }
    }
}

@Composable
fun OutingCardItem(
    outing: OutingRequest,
    onJoin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("outing_card_${outing.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Publisher profile header
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarView(
                    emoji = outing.creatorAvatarEmoji,
                    bgColorInt = outing.creatorAvatarBgColor,
                    size = 36,
                    fontSize = 14
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = outing.creatorName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Posted a movie plan",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }

                // Status tag
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "OPEN",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Movie & Theater detail block
            Text(
                text = outing.movieTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.secondary
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 2.dp)
            ) {
                Icon(Icons.Filled.Theaters, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = outing.theaterName,
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${outing.showDate} @ ${outing.showTime}",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }

            if (outing.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"${outing.description}\"",
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Post-movie hangout details
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "PLAN AFTER MOVIE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = outing.afterPlan,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Request Join trigger
            Button(
                onClick = onJoin,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("join_outing_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ConfirmationNumber,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Request to Meet Up & Sit Together",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

// ==========================================
// TAB 3: CONNECTIONS & CHATS
// ==========================================

@Composable
fun MatchesTab(viewModel: MovieBuddyViewModel) {
    val matches by viewModel.allMatches.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "YOUR ACTIVE OUTINGS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 1.sp
        )
        Text(
            text = "Tread carefully & be friendly. Meet inside public lobbies!",
            fontSize = 11.sp,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(10.dp))

        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🍿", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No matches or buddy meetups yet.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Join outstanding outings, or tap direct message on Dallas profiles!",
                        color = Color.Gray.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(matches) { match ->
                    MatchRowItem(
                        match = match,
                        onClick = { viewModel.openChat(match) }
                    )
                }
            }
        }
    }
}

@Composable
fun MatchRowItem(
    match: MatchRecord,
    onClick: () -> Unit
) {
    val messages = match.getMessages()
    val latestMessage = messages.lastOrNull()?.second ?: "No messages yet."
    val latestByMe = messages.lastOrNull()?.first ?: false

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("match_item_${match.id}"),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AvatarView(
                emoji = match.buddyAvatarEmoji,
                bgColorInt = match.buddyAvatarBgColor,
                size = 46,
                fontSize = 18
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = match.buddyName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.White
                    )
                    Text(
                        text = "Confirmed",
                        color = Color(0xFF00E676),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "Seeing: ${match.movieTitle}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = if (latestByMe) "You: $latestMessage" else "${match.buddyName}: $latestMessage",
                    fontSize = 11.sp,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ChatBubble,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==========================================
// TAB 4: MY PROFILE & EDIT SCREEN
// ==========================================

@Composable
fun ProfileTab(viewModel: MovieBuddyViewModel) {
    val meProfile by viewModel.myProfile.collectAsStateWithLifecycle()
    val isEditing by viewModel.isEditingProfile.collectAsStateWithLifecycle()

    val editName by viewModel.editName.collectAsStateWithLifecycle()
    val editNeighborhood by viewModel.editNeighborhood.collectAsStateWithLifecycle()
    val editAge by viewModel.editAge.collectAsStateWithLifecycle()
    val editGender by viewModel.editGender.collectAsStateWithLifecycle()
    val editRelationshipStatus by viewModel.editRelationshipStatus.collectAsStateWithLifecycle()
    val editFavoriteMovies by viewModel.editFavoriteMovies.collectAsStateWithLifecycle()
    val editFavoriteGenres by viewModel.editFavoriteGenres.collectAsStateWithLifecycle()
    val editLetterboxd by viewModel.editLetterboxd.collectAsStateWithLifecycle()
    val editAvatarEmoji by viewModel.editAvatarEmoji.collectAsStateWithLifecycle()
    val editAvatarBgColor by viewModel.editAvatarBgColor.collectAsStateWithLifecycle()

    val neighborhoods = listOf("Lower Greenville", "Bishop Arts District", "Uptown", "Deep Ellum", "Lakewood", "Oak Lawn", "Knox-Henderson")
    val genders = listOf("Male", "Female", "Non-binary", "Prefer not to say")
    val statuses = listOf("Single", "Married", "In a Relationship")
    val context = LocalContext.current

    if (meProfile == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val user = meProfile!!

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        if (!isEditing) {
            // Profile display state
            item {
                Spacer(modifier = Modifier.height(12.dp))
                AvatarView(
                    emoji = user.avatarEmoji,
                    bgColorInt = user.avatarBgColor,
                    size = 100,
                    fontSize = 44
                )
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = user.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = "📍 ${user.neighborhood}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "${user.age} yrs • ${user.gender} • ${user.relationshipStatus}",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Detail display box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "🎬 FAVORITE GENRES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            user.getGenresList().forEach { g ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(g, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "🍿 FAVORITE MOVIES",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = user.favoriteMovies,
                            fontSize = 13.sp,
                            color = Color.White
                        )

                        if (user.letterboxdLink.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "🗄️ LETTERBOXD ACCOUNT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                letterSpacing = 1.sp
                            )
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(user.letterboxdLink))
                                            context.startActivity(browserIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Invalid link address", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = user.letterboxdLink,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("letterboxd_link_label")
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = { viewModel.enterProfileEdit() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("edit_profile_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Modify Profile Details")
                }
            }
        } else {
            // PROFILE EDIT FORM STATE
            item {
                Text(
                    text = "EDIT PROFILE DETAILS",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Avatar Customizer
                Text("Select Icon and Card Color Style:", fontSize = 12.sp, color = Color.Gray, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(6.dp))
                
                val avatars = listOf("🍿", "🎞️", "🦊", "🦁", "🌟", "🎸", "🎨", "👽", "🦉")
                val colors = listOf(0xFFE50914.toInt(), 0xFF6366F1.toInt(), 0xFF10B981.toInt(), 0xFFF59E0B.toInt(), 0xFF8B5CF6.toInt(), 0xFF06B6D4.toInt())

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(avatars) { emo ->
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(
                                    if (editAvatarEmoji == emo) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f)
                                )
                                .clickable { viewModel.setEditAvatar(emo, editAvatarBgColor) }
                                .padding(4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(emo, fontSize = 20.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(colors) { col ->
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(Color(col))
                                .clickable { viewModel.setEditAvatar(editAvatarEmoji, col) }
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (editAvatarBgColor == col) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Profile Fields Inputs
                OutlinedTextField(
                    value = editName,
                    onValueChange = { viewModel.editName.value = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_name_input"),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Neighborhood Select Row / Box
                var showNeighborDropdown by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showNeighborDropdown = !showNeighborDropdown }
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Selected Dallas Neighborhood", fontSize = 11.sp, color = Color.Gray)
                        Text(editNeighborhood, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    DropdownMenu(
                        expanded = showNeighborDropdown,
                        onDismissRequest = { showNeighborDropdown = false }
                    ) {
                        neighborhoods.forEach { nb ->
                            DropdownMenuItem(
                                text = { Text(nb) },
                                onClick = {
                                    viewModel.editNeighborhood.value = nb
                                    showNeighborDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Age & Gender
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editAge.toString(),
                        onValueChange = {
                            val v = it.toIntOrNull()
                            if (v != null) viewModel.editAge.value = v
                        },
                        label = { Text("Age") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )

                    var showGenderDropdown by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .weight(1.2f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .clickable { showGenderDropdown = !showGenderDropdown }
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Gender", fontSize = 10.sp, color = Color.Gray)
                            Text(editGender, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        DropdownMenu(
                            expanded = showGenderDropdown,
                            onDismissRequest = { showGenderDropdown = false }
                        ) {
                            genders.forEach { gen ->
                                DropdownMenuItem(
                                    text = { Text(gen) },
                                    onClick = {
                                        viewModel.editGender.value = gen
                                        showGenderDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Relationship Status Dropdown
                var showStatusDropdown by remember { mutableStateOf(false) }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showStatusDropdown = !showStatusDropdown }
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Relationship Status", fontSize = 11.sp, color = Color.Gray)
                        Text(editRelationshipStatus, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    DropdownMenu(
                        expanded = showStatusDropdown,
                        onDismissRequest = { showStatusDropdown = false }
                    ) {
                        statuses.forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st) },
                                onClick = {
                                    viewModel.editRelationshipStatus.value = st
                                    showStatusDropdown = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editFavoriteMovies,
                    onValueChange = { viewModel.editFavoriteMovies.value = it },
                    label = { Text("Favorite Movies (comma separated)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editFavoriteGenres,
                    onValueChange = { viewModel.editFavoriteGenres.value = it },
                    label = { Text("Favorite Genres (e.g. Action, Indie, Sci-Fi)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = editLetterboxd,
                    onValueChange = { viewModel.editLetterboxd.value = it },
                    label = { Text("Letterboxd Profile URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Save buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.cancelProfileEdit() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = { viewModel.saveProfileEdit() },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_profile_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Save Details")
                    }
                }
            }
        }
    }
}

// ==========================================
// OVERLAYS & MODALS UI
// ==========================================

@Composable
fun BuddyDetailOverlay(
    buddy: UserProfile,
    onClose: () -> Unit,
    onStartMatch: () -> Unit
) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("buddy_detail_modal"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUDDY DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Close details")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AvatarView(emoji = buddy.avatarEmoji, bgColorInt = buddy.avatarBgColor, size = 80, fontSize = 36)
                
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = buddy.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Text(
                    text = "📍 Neighborhood: ${buddy.neighborhood}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )

                Text(
                    text = "${buddy.age} years old • ${buddy.gender} • ${buddy.relationshipStatus}",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 14.dp)
                )

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("🎬 FAVORITE GENRES", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = buddy.favoriteGenres,
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Text("🍿 FILMS THEY LOVE", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text(
                            text = buddy.favoriteMovies,
                            fontSize = 13.sp,
                            color = Color.White,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                if (buddy.letterboxdLink.isNotBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            try {
                                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(buddy.letterboxdLink))
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Cannot open Letterboxd address", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Link, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("See reviews on Letterboxd", color = Color.LightGray, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onStartMatch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("init_match_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Connect as Movie Buddy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatRoomOverlay(
    match: MatchRecord,
    viewModel: MovieBuddyViewModel,
    onClose: () -> Unit
) {
    val chatText by viewModel.chatInputText.collectAsStateWithLifecycle()
    val messages = match.getMessages()
    val listState = rememberLazyListState()

    // Always scroll to the latest message whenever messages flow updates
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header bar
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0C)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 8.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Close chat")
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    AvatarView(
                        emoji = match.buddyAvatarEmoji,
                        bgColorInt = match.buddyAvatarBgColor,
                        size = 40,
                        fontSize = 16
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = match.buddyName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (match.outingId == 0) "Direct Chat" else "Outimate: ${match.theaterName}",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Surface(
                        color = Color(0xFF00E676).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "ACTIVE PLAN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Info bar showing plans details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "🎟️ Watch \"${match.movieTitle}\" at ${match.theaterName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "📅 Schedule: ${match.showDate} (${match.showTime})",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                    Text(
                        text = "🍽️ Afterwards: ${match.plan}",
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }
            }

            // Chat Messages log
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 20.dp)
            ) {
                items(messages) { (isMe, text) ->
                    ChatBubble(isMe = isMe, text = text)
                }
            }

            // Message box input row
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0C)),
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Row(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatText,
                        onValueChange = { viewModel.setChatInput(it) },
                        placeholder = { Text("Write your movie plan message...") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_text_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { viewModel.sendChatMessage() }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.5f)
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { viewModel.sendChatMessage() },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .testTag("chat_send_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send plan",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(isMe: Boolean, text: String) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Column(
            horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isMe) 16.dp else 2.dp,
                            bottomEnd = if (isMe) 2.dp else 16.dp
                        )
                    )
                    .background(
                        if (isMe) MaterialTheme.colorScheme.primaryContainer else Color(0xFF2A2A2E)
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = text,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
            Text(
                text = if (isMe) "You" else "Buddy",
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 2.dp, start = 4.dp, end = 4.dp)
            )
        }
    }
}

@Composable
fun CreateOutingDialog(viewModel: MovieBuddyViewModel) {
    val movieTitle by viewModel.nMovieTitle.collectAsStateWithLifecycle()
    val theaterName by viewModel.nTheaterName.collectAsStateWithLifecycle()
    val showDate by viewModel.nShowDate.collectAsStateWithLifecycle()
    val showTime by viewModel.nShowTime.collectAsStateWithLifecycle()
    val afterPlan by viewModel.nAfterPlan.collectAsStateWithLifecycle()
    val description by viewModel.nDescription.collectAsStateWithLifecycle()

    val theaters = listOf(
        "Texas Theatre (Oak Cliff)",
        "AMC NorthPark 15",
        "Landmark Inwood Theatre",
        "Angelika Film Center (Mockingbird)",
        "Violet Crown Dallas (Uptown)",
        "Alamo Drafthouse Lake Highlands"
    )

    val dates = listOf(
        "Tonight",
        "Tomorrow",
        "Friday, May 22",
        "Saturday, May 23",
        "Sunday, May 24"
    )

    Dialog(onDismissRequest = { viewModel.setShowOutingDialog(false) }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("create_outing_modal"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "POST NEW OUTING",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary,
                            letterSpacing = 1.sp
                        )
                        IconButton(onClick = { viewModel.setShowOutingDialog(false) }) {
                            Icon(Icons.Default.Close, contentDescription = "Close dialog")
                        }
                    }
                    Text(
                        text = "Share what movie you want to watch and where, so locals can sit with you!",
                        fontSize = 11.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                item {
                    // Movie title
                    OutlinedTextField(
                        value = movieTitle,
                        onValueChange = { viewModel.nMovieTitle.value = it },
                        label = { Text("What Movie?") },
                        placeholder = { Text("e.g. Kingdom of the Planet of the Apes") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("new_outing_movie_input"),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                item {
                    // Theater selection dropdown box
                    var showTheaterDropdown by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTheaterDropdown = !showTheaterDropdown }
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Which Dallas Theater?", fontSize = 11.sp, color = Color.Gray)
                            Text(theaterName, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        DropdownMenu(
                            expanded = showTheaterDropdown,
                            onDismissRequest = { showTheaterDropdown = false }
                        ) {
                            theaters.forEach { th ->
                                DropdownMenuItem(
                                    text = { Text(th) },
                                    onClick = {
                                        viewModel.nTheaterName.value = th
                                        showTheaterDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Show Date select row
                    var showDateDropdown by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDateDropdown = !showDateDropdown }
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Which Day?", fontSize = 11.sp, color = Color.Gray)
                            Text(showDate, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                        DropdownMenu(
                            expanded = showDateDropdown,
                            onDismissRequest = { showDateDropdown = false }
                        ) {
                            dates.forEach { dt ->
                                DropdownMenuItem(
                                    text = { Text(dt) },
                                    onClick = {
                                        viewModel.nShowDate.value = dt
                                        showDateDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                item {
                    // Show Time text input
                    OutlinedTextField(
                        value = showTime,
                        onValueChange = { viewModel.nShowTime.value = it },
                        label = { Text("What Time?") },
                        placeholder = { Text("e.g. 7:30 PM") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                item {
                    // After movie hanging options
                    OutlinedTextField(
                        value = afterPlan,
                        onValueChange = { viewModel.nAfterPlan.value = it },
                        label = { Text("What's the plan afterwards?") },
                        placeholder = { Text("e.g. Grab food at Lower Greenville/drinks at Uptown") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                item {
                    // Short description note
                    OutlinedTextField(
                        value = description,
                        onValueChange = { viewModel.nDescription.value = it },
                        label = { Text("Optional message / Notes") },
                        placeholder = { Text("e.g. I already booked row G. Let me know if you want to sit close!") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary)
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.setShowOutingDialog(false) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Dismiss")
                        }

                        Button(
                            onClick = { viewModel.postOuting() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("submit_outing_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            enabled = movieTitle.isNotBlank()
                        ) {
                            Text("Post Outing")
                        }
                    }
                }
            }
        }
    }
}
