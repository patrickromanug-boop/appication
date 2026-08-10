package com.example.ui.tabs

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import com.example.ui.AppViewModel
import com.example.ui.MockJob
import com.example.ui.components.LSLogoLockup
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun OrgInitialsBadge(orgName: String, size: Int = 40) {
    val initials = orgName.split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .map { it.first().uppercase() }
        .joinToString("")

    // Rotate through light brand-tinted colors based on organization name hash
    val colors = listOf(
        Color(0xFFE8F0FE), // Light Blue
        Color(0xFFFFF3E0), // Light Orange
        Color(0xFFE8F5E9), // Light Green
        Color(0xFFF3E5F5), // Light Purple
        Color(0xFFE0F7FA)  // Light Cyan
    )
    val textColors = listOf(
        Color(0xFF1A73E8), // Blue
        Color(0xFFE65100), // Orange
        Color(0xFF2E7D32), // Green
        Color(0xFF7B1FA2), // Purple
        Color(0xFF00838F)  // Cyan
    )
    val index = Math.abs(orgName.hashCode()) % colors.size
    val bgColor = colors[index]
    val textColor = textColors[index]

    Box(
        modifier = Modifier
            .size(size.dp)
            .background(bgColor, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = (size * 0.38f).sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun DeadlinePill(deadlineStr: String) {
    val daysRemaining = remember(deadlineStr) {
        try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            val deadlineDate = LocalDate.parse(deadlineStr, formatter)
            val today = LocalDate.now()
            val referenceDate = if (today.isAfter(LocalDate.of(2026, 7, 1))) today else LocalDate.of(2026, 7, 21)
            ChronoUnit.DAYS.between(referenceDate, deadlineDate)
        } catch (e: Exception) {
            -1L
        }
    }

    val isUrgent = daysRemaining in 0..3
    val isExpired = daysRemaining < 0

    val bgColor = when {
        isExpired -> Color(0xFFF5F5F5)
        isUrgent -> Color(0xFFFFEBEE)
        else -> Color(0xFFECEFF1)
    }

    val textColor = when {
        isExpired -> Color(0xFF757575)
        isUrgent -> Color(0xFFC62828)
        else -> Color(0xFF455A64)
    }

    val text = when {
        isExpired -> "Expired"
        daysRemaining == 0L -> "Deadline TODAY"
        daysRemaining == 1L -> "1 day remaining"
        else -> "$daysRemaining days left"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AppViewModel,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val activeJobs by viewModel.jobs.collectAsState()
    val bookmarkedJobIds by viewModel.bookmarks.collectAsState()
    val appliedJobIds by viewModel.appliedJobs.collectAsState()

    // Theme mode state
    val themeMode by viewModel.themeMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemDark
    }

    // Search and filter queries
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf("All") }
    var selectedLocation by remember { mutableStateOf("All") }
    var selectedJobType by remember { mutableStateOf("All") }

    // Active filters applied state
    var appliedCategory by remember { mutableStateOf("All") }
    var appliedLocation by remember { mutableStateOf("All") }
    var appliedJobType by remember { mutableStateOf("All") }

    // Dropdowns and UI expansion
    var isFiltersExpanded by remember { mutableStateOf(false) }

    // Welcome Banner state: appears ONCE per app session on launch, auto-disappears after 6 seconds
    var showWelcomeBanner by remember { mutableStateOf(!viewModel.hasShownWelcomeBannerThisSession) }
    LaunchedEffect(Unit) {
        if (!viewModel.hasShownWelcomeBannerThisSession) {
            viewModel.hasShownWelcomeBannerThisSession = true
            showWelcomeBanner = true
            delay(6000L)
            showWelcomeBanner = false
        } else {
            showWelcomeBanner = false
        }
    }

    val categories = listOf("All", "Engineering & IT", "Healthcare", "Sales & Marketing", "Education", "Finance")
    val locations = listOf("All", "Kampala", "Mbarara", "Jinja", "Entebbe", "Remote")
    val jobTypes = listOf("All", "Full-time", "Part-time", "Contract", "Remote", "Internship", "Volunteer")

    // Filter jobs client-side (memoized & automatically excludes expired listings)
    val filteredJobs = remember(activeJobs, searchQuery, appliedCategory, appliedLocation, appliedJobType) {
        val today = java.time.LocalDate.now()
        val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE

        activeJobs.filter { job ->
            val matchesSearch = searchQuery.isBlank() || 
                                job.title.contains(searchQuery, ignoreCase = true) || 
                                job.organization.contains(searchQuery, ignoreCase = true) ||
                                job.location.contains(searchQuery, ignoreCase = true) ||
                                job.category.contains(searchQuery, ignoreCase = true)

            val matchesCategory = appliedCategory == "All" || 
                                  job.category.equals(appliedCategory, ignoreCase = true) || 
                                  job.category.contains(appliedCategory, ignoreCase = true) ||
                                  appliedCategory.contains(job.category, ignoreCase = true) ||
                                  (appliedCategory == "Engineering & IT" && (
                                      job.category.contains("IT", ignoreCase = true) || 
                                      job.category.contains("Tech", ignoreCase = true) || 
                                      job.category.contains("Engineer", ignoreCase = true) || 
                                      job.category.contains("Software", ignoreCase = true) || 
                                      job.category.contains("Developer", ignoreCase = true)
                                  )) ||
                                  (appliedCategory == "Sales & Marketing" && (
                                      job.category.contains("Sales", ignoreCase = true) || 
                                      job.category.contains("Marketing", ignoreCase = true) || 
                                      job.category.contains("Business", ignoreCase = true)
                                  )) ||
                                  (appliedCategory == "Finance" && (
                                      job.category.contains("Accounting", ignoreCase = true) || 
                                      job.category.contains("Audit", ignoreCase = true) || 
                                      job.category.contains("Banking", ignoreCase = true) || 
                                      job.category.contains("Finance", ignoreCase = true)
                                  )) ||
                                  (appliedCategory == "Healthcare" && (
                                      job.category.contains("Medical", ignoreCase = true) || 
                                      job.category.contains("Health", ignoreCase = true) || 
                                      job.category.contains("Nurse", ignoreCase = true) || 
                                      job.category.contains("Doctor", ignoreCase = true)
                                  ))

            val matchesLocation = appliedLocation == "All" || 
                                  job.location.contains(appliedLocation, ignoreCase = true) ||
                                  appliedLocation.contains(job.location, ignoreCase = true)

            val matchesJobType = appliedJobType == "All" || 
                                 job.jobType.contains(appliedJobType, ignoreCase = true) ||
                                 appliedJobType.contains(job.jobType, ignoreCase = true)

            val isActive = job.status.isBlank() || 
                           job.status.equals("active", ignoreCase = true) || 
                           job.status.equals("open", ignoreCase = true) || 
                           job.status.equals("published", ignoreCase = true)

            val isNotExpired = if (job.status.equals("active", ignoreCase = true) || job.status.equals("open", ignoreCase = true) || job.status.equals("published", ignoreCase = true) || job.status.isBlank()) {
                true
            } else {
                try {
                    if (job.deadline.isBlank() || job.deadline.equals("Negotiable", ignoreCase = true)) {
                        true
                    } else {
                        val deadlineClean = job.deadline.take(10)
                        val deadlineDate = java.time.LocalDate.parse(deadlineClean, formatter)
                        !deadlineDate.isBefore(today)
                    }
                } catch (e: Exception) { 
                    true 
                }
            }

            matchesSearch && matchesCategory && matchesLocation && matchesJobType && isActive && isNotExpired
        }
    }


    // Modal view details states
    var selectedJobForDetail by remember { mutableStateOf<MockJob?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadJobsFromSupabase()
    }

    LaunchedEffect(viewModel.globalJobDetailToShow) {
        viewModel.globalJobDetailToShow?.let { job ->
            selectedJobForDetail = job
            viewModel.globalJobDetailToShow = null
        }
    }

    var reportReasonInput by remember { mutableStateOf("") }
    var showReportDialogForJob by remember { mutableStateOf<MockJob?>(null) }

    // Web view state
    var showWebViewUrl by remember { mutableStateOf<String?>(null) }
    var webViewTitle by remember { mutableStateOf("") }

    androidx.activity.compose.BackHandler(
        enabled = selectedJobForDetail != null || showWebViewUrl != null || showReportDialogForJob != null || isFiltersExpanded
    ) {
        when {
            showWebViewUrl != null -> showWebViewUrl = null
            selectedJobForDetail != null -> selectedJobForDetail = null
            showReportDialogForJob != null -> showReportDialogForJob = null
            isFiltersExpanded -> isFiltersExpanded = false
        }
    }


    // Auto apply state engine
    var activeAutoApplyingJob by remember { mutableStateOf<MockJob?>(null) }
    var autoApplyProgressText by remember { mutableStateOf("Analyzing CV fields...") }
    var autoApplyProgress by remember { mutableStateOf(0.1f) }
    var showAutoApplySuccess by remember { mutableStateOf<MockJob?>(null) }
    var showProfileIncompletePrompt by remember { mutableStateOf<MockJob?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LSLogoLockup(logoSize = 24f, showPill = true)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { isSearchVisible = !isSearchVisible },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("home_search_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                            contentDescription = "Search",
                            tint = if (searchQuery.isNotEmpty()) PrimaryBlue else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Refresh Jobs Button
                    val isRefreshing by viewModel.isRefreshing.collectAsState()
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (isRefreshing) 360f else 0f,
                        animationSpec = if (isRefreshing) infiniteRepeatable(
                            animation = tween(800, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ) else snap(),
                        label = "refresh_rotation"
                    )

                    IconButton(
                        onClick = { viewModel.loadJobsFromSupabase(showToastOnComplete = true) },
                        enabled = !isRefreshing,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("home_refresh_jobs_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Jobs",
                            tint = if (isRefreshing) AccentOrange else PrimaryBlue,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer {
                                    rotationZ = rotationAngle
                                }
                        )
                    }

                    // Notification Bell Button
                    val isNotifActive = viewModel.isNotificationPermissionGranted(context)
                    IconButton(
                        onClick = {
                            if (isNotifActive) {
                                viewModel.sendTestNotification()
                            } else {
                                viewModel.checkAndRequestNotificationPermission(context)
                            }
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isNotifActive) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                                shape = CircleShape
                            )
                            .testTag("home_notification_bell_button")
                    ) {
                        Icon(
                            imageVector = if (isNotifActive) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            contentDescription = "Notification Settings",
                            tint = if (isNotifActive) Color(0xFF2E7D32) else Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Theme Toggle Button
                    IconButton(
                        onClick = {
                            viewModel.setTheme(if (isDark) "light" else "dark")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = if (isDark) Color(0xFF2C2C2E) else PrimaryBlue.copy(alpha = 0.1f),
                                shape = CircleShape
                            )
                            .testTag("home_theme_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isDark) Icons.Default.WbSunny else Icons.Default.DarkMode,
                            contentDescription = if (isDark) "Switch to Light Theme" else "Switch to Dark Theme",
                            tint = if (isDark) Color(0xFFFFD60A) else PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Initial / Profile Avatar Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape)
                            .clickable { viewModel.currentTab = "profile" }
                            .testTag("home_profile_avatar_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLoggedIn) (userProfile?.fullName ?: "U").take(1).uppercase() else "G",
                            color = PrimaryBlue,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            // Welcome Banner Card (appears on app launch or login, auto-disappears in 6 seconds)
            AnimatedVisibility(
                visible = showWelcomeBanner,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isLoggedIn) "Hello, ${userProfile?.fullName ?: "Candidate"}!" else "Welcome to LS Services!",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Browse, filter and tap-apply to Uganda's leading jobs. Automated CV drafts included.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                        }
                        IconButton(
                            onClick = { showWelcomeBanner = false },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss welcome banner",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Collapsible Search Input Barspace (compact single-line rectangle)
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search icon",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { isSearchVisible = false }),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("job_search_input"),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Search title, location or company...",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                            maxLines = 1
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .height(38.dp)
                            .background(PrimaryBlue, RoundedCornerShape(8.dp))
                            .clickable { isSearchVisible = false }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Search", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Active Search Filter Chip (shown when search is active but barspace is closed)
            if (searchQuery.isNotEmpty() && !isSearchVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                ) {
                    AssistChip(
                        onClick = { isSearchVisible = true },
                        label = { Text("Search: \"$searchQuery\"", maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        trailingIcon = {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Clear search query",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { searchQuery = "" }
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = PrimaryBlue.copy(alpha = 0.1f),
                            labelColor = PrimaryBlue
                        )
                    )
                }
            }

            // Filters Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Outlined Filters pill button matching screenshot
                OutlinedButton(
                    onClick = { isFiltersExpanded = !isFiltersExpanded },
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(42.dp)
                        .testTag("home_filter_pill_button"),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.5.dp, if (appliedCategory != "All" || appliedLocation != "All" || appliedJobType != "All") AccentOrange else PrimaryBlue),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (appliedCategory != "All" || appliedLocation != "All" || appliedJobType != "All") AccentOrange else PrimaryBlue
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filters icon",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (appliedCategory != "All" || appliedLocation != "All" || appliedJobType != "All") "Filters (Active)" else "Filters",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            // Collapsible Expanded Filters Panel with scroll bar and scrollable options
            AnimatedVisibility(
                visible = isFiltersExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    val filterDetailScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .drawVerticalScrollbar(filterDetailScrollState)
                            .verticalScroll(filterDetailScrollState)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Filter Listings",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Dropdowns for Category and Location
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Category Select
                            var catExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1.2f)) {
                                OutlinedTextField(
                                    value = if (selectedCategory == "All") "All Categories" else selectedCategory,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Sector/Category", fontSize = 10.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "dropdown") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue)
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { catExpanded = true })
                                DropdownMenu(expanded = catExpanded, onDismissRequest = { catExpanded = false }) {
                                    categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat) },
                                            onClick = {
                                                selectedCategory = cat
                                                catExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Location Select
                            var locExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedTextField(
                                    value = if (selectedLocation == "All") "All Locations" else selectedLocation,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Location", fontSize = 10.sp) },
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, "dropdown") },
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue)
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { locExpanded = true })
                                DropdownMenu(expanded = locExpanded, onDismissRequest = { locExpanded = false }) {
                                    locations.forEach { loc ->
                                        DropdownMenuItem(
                                            text = { Text(loc) },
                                            onClick = {
                                                selectedLocation = loc
                                                locExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Job Type Select
                        var typeExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = if (selectedJobType == "All") "All Contract Types" else selectedJobType,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Job Contract Type", fontSize = 10.sp) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, "dropdown") },
                                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue)
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { typeExpanded = true })
                            DropdownMenu(expanded = typeExpanded, onDismissRequest = { typeExpanded = false }) {
                                jobTypes.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type) },
                                        onClick = {
                                            selectedJobType = type
                                            typeExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Filter Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    selectedCategory = "All"
                                    selectedLocation = "All"
                                    selectedJobType = "All"
                                    appliedCategory = "All"
                                    appliedLocation = "All"
                                    appliedJobType = "All"
                                    isFiltersExpanded = false
                                }
                            ) {
                                Text("Clear All", color = AccentOrange, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    appliedCategory = selectedCategory
                                    appliedLocation = selectedLocation
                                    appliedJobType = selectedJobType
                                    isFiltersExpanded = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Apply Filters", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Available Job count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Available Jobs · ${filteredJobs.size} found",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                // Inline quick indicator if any filters applied
                if (appliedCategory != "All" || appliedLocation != "All" || appliedJobType != "All" || searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(AccentOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                            .clickable {
                                searchQuery = ""
                                selectedCategory = "All"
                                selectedLocation = "All"
                                selectedJobType = "All"
                                appliedCategory = "All"
                                appliedLocation = "All"
                                appliedJobType = "All"
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Clear", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.Close, "clear", tint = AccentOrange, modifier = Modifier.size(10.dp))
                        }
                    }
                }
            }

            // Main Active Listings list or Empty State
            if (filteredJobs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Empty state icon",
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No jobs match your filters",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Try refining your search query, choosing different dropdown combinations, or resetting filters.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            searchQuery = ""
                            selectedCategory = "All"
                            selectedLocation = "All"
                            selectedJobType = "All"
                            appliedCategory = "All"
                            appliedLocation = "All"
                            appliedJobType = "All"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Reset Filters", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredJobs, key = { it.id }) { job ->
                        JobCard(
                            job = job,
                            isBookmarked = bookmarkedJobIds.contains(job.id),
                            isApplied = appliedJobIds.contains(job.id),
                            onToggleBookmark = { viewModel.toggleBookmark(job.id) },
                            onCardClicked = {
                                viewModel.incrementViewsCount(job.id)
                                selectedJobForDetail = job
                            }
                        )
                    }
                }
            }
        }

        LaunchedEffect(selectedJobForDetail) {
            if (selectedJobForDetail != null) {
                viewModel.onJobDetailViewed()
            }
        }

        // ==========================================
        // 1. DETAILED POPUP / MODAL OVERLAY (ANIMATED)
        // ==========================================
        AnimatedVisibility(
            visible = selectedJobForDetail != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            selectedJobForDetail?.let { job ->
                // Ensure dynamic state overrides are loaded correctly on card open
                val currentJobState = activeJobs.firstOrNull { it.id == job.id } ?: job
                val isCurrentBookmarked = bookmarkedJobIds.contains(currentJobState.id)
                val isCurrentApplied = appliedJobIds.contains(currentJobState.id)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable { selectedJobForDetail = null }
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.92f)
                            .align(Alignment.BottomCenter)
                            .clickable(enabled = false) {}, // Prevent dismiss click inside card
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Sticky Top Header Row (Close, Bookmark, Report, Share)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(onClick = { selectedJobForDetail = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close Detail modal")
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Bookmark button (Guest local vs Registered synced)
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleBookmark(currentJobState.id)
                                            if (!isLoggedIn) {
                                                Toast.makeText(context, "Saved locally (Guest Mode). Register to sync across devices!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                val msg = if (isCurrentBookmarked) "Removed from saved jobs" else "Added to saved jobs"
                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                            contentDescription = "Bookmark button",
                                            tint = if (isCurrentBookmarked) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    // Share button (Native share sheet)
                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Check out this vacancy: ${currentJobState.title} at ${currentJobState.organization} (${currentJobState.location}).\nApply via LS Services or directly here: ${currentJobState.officialLink}"
                                                )
                                                type = "text/plain"
                                            }
                                            val shareIntent = Intent.createChooser(sendIntent, "Share Job Vacancy")
                                            context.startActivity(shareIntent)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share button",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    // Report button
                                    IconButton(
                                        onClick = { showReportDialogForJob = currentJobState }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Report button",
                                            tint = if (viewModel.reportedJobs.value.contains(currentJobState.id)) AccentOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            // Middle Scrollable Section
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 60.dp, bottom = 80.dp) // Leave padding for sticky bottom buttons
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp)
                            ) {
                                // Title Block
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    OrgInitialsBadge(orgName = currentJobState.organization, size = 52)
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = currentJobState.title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = currentJobState.organization,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Tags row (Deadline Pill & Salary Tag)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    DeadlinePill(deadlineStr = currentJobState.deadline)
                                    Box(
                                        modifier = Modifier
                                            .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = currentJobState.category,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                    }
                                }

                                Divider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                // Meta indicators (Location, Contract Type, Views)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Location indicator
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(Icons.Default.LocationOn, "Location", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(currentJobState.location, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    // Contract indicator
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1.1f)) {
                                        Icon(Icons.Default.Business, "Type", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(currentJobState.jobType, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    // Views indicator
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(0.9f)) {
                                        Icon(Icons.Default.Visibility, "Views", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("${currentJobState.viewsCount} views", fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }

                                Divider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                // Job Purpose Section
                                Text("Position Purpose", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentJobState.purpose,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Job Requirements Section
                                Text("Key Requirements", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentJobState.requirements,
                                    fontSize = 14.sp,
                                    lineHeight = 22.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                Spacer(modifier = Modifier.height(18.dp))

                                // Other details section
                                Text("Compensation & Other Details", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = currentJobState.otherDetails,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )

                                Divider(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(vertical = 16.dp)
                                )

                                // ==========================================
                                // "SIMILAR JOBS" STRIP (SCROLLABLE ROW)
                                // ==========================================
                                Text(
                                    text = "Similar Open Vacancies",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(10.dp))

                                val similarList = remember(currentJobState.id, appliedJobIds) {
                                    viewModel.getSimilarJobs(currentJobState)
                                }

                                if (similarList.isEmpty()) {
                                    Text(
                                        text = "No similar jobs found in this sector. Try checking other listings.",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )
                                } else {
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        items(similarList, key = { it.id }) { simJob ->
                                            Card(
                                                modifier = Modifier
                                                    .width(180.dp)
                                                    .clickable {
                                                        viewModel.incrementViewsCount(simJob.id)
                                                        selectedJobForDetail = simJob
                                                    },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                        OrgInitialsBadge(orgName = simJob.organization, size = 26)
                                                        Text(
                                                            text = simJob.organization,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                    Spacer(modifier = Modifier.height(6.dp))
                                                    Text(
                                                        text = simJob.title,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = simJob.location,
                                                        fontSize = 10.sp,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(32.dp))
                            }

                            // Sticky Action Row (Fixed Bottom)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 12.dp,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Button A: Apply on Official Site
                                    Button(
                                        onClick = {
                                            val isExpired = try {
                                                java.time.LocalDate.parse(currentJobState.deadline).isBefore(java.time.LocalDate.now())
                                            } catch (e: Exception) {
                                                false
                                            }
                                            if (!isOnline) {
                                                Toast.makeText(context, "You're offline. Connect to the internet to apply for this job.", Toast.LENGTH_LONG).show()
                                            } else if (isExpired) {
                                                Toast.makeText(context, "This job listing has expired.", Toast.LENGTH_LONG).show()
                                            } else {
                                                // Load inside in-app WebView
                                                webViewTitle = currentJobState.title
                                                showWebViewUrl = currentJobState.officialLink
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isOnline) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (!isOnline) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f) else PrimaryBlue
                                        ),
                                        border = BorderStroke(1.5.dp, if (!isOnline) Color.Gray.copy(alpha = 0.3f) else PrimaryBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            text = if (!isOnline) "Official Site (Offline)" else "Official Site",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }

                                    // Button B: Apply automatically
                                    Button(
                                        onClick = {
                                            val isExpired = try {
                                                java.time.LocalDate.parse(currentJobState.deadline).isBefore(java.time.LocalDate.now())
                                            } catch (e: Exception) {
                                                false
                                            }
                                            if (!isOnline) {
                                                Toast.makeText(context, "You're offline. Connect to the internet to apply for this job.", Toast.LENGTH_LONG).show()
                                            } else if (isExpired) {
                                                Toast.makeText(context, "This job listing has expired.", Toast.LENGTH_LONG).show()
                                            } else if (currentJobState.applicationMethod == "requires_personal_account") {
                                                Toast.makeText(
                                                    context,
                                                    "This employer requires a manual login on their portal. Please use 'Official Site' instead.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                            } else if (!isLoggedIn) {
                                                // Trigger register onboarding dialog
                                                showAutoApplySuccess = null // close any older
                                                activeAutoApplyingJob = currentJobState // triggers register CTA
                                            } else {
                                                // Registered auto-apply flow
                                                viewModel.activeApplyFlowJob = currentJobState
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1.3f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when {
                                                !isOnline -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                                                isCurrentApplied -> Color(0xFF2E7D32)
                                                else -> PrimaryBlue
                                            }
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        enabled = !isCurrentApplied
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (isCurrentApplied) {
                                                Icon(Icons.Default.CheckCircle, "applied", tint = Color.White, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Applied", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            } else if (!isOnline) {
                                                Icon(Icons.Default.WifiOff, "offline", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f), modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Offline", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f))
                                            } else {
                                                Text("Auto-Apply Tap", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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

        // ==========================================
        // 2. IN-APP WEBVIEW OVERLAY DIALOG
        // ==========================================
        if (showWebViewUrl != null) {
            Dialog(
                onDismissRequest = { showWebViewUrl = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var webViewProgressLoading by remember { mutableStateOf(true) }
                    var hasWebViewError by remember { mutableStateOf(false) }
                    var reloadKey by remember { mutableStateOf(0) }

                    // Safety progress indicator timeout
                    LaunchedEffect(showWebViewUrl, reloadKey) {
                        webViewProgressLoading = true
                        hasWebViewError = false
                        delay(10000)
                        if (webViewProgressLoading) {
                            webViewProgressLoading = false
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header bar with status and exit buttons
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                IconButton(onClick = { showWebViewUrl = null }) {
                                    Icon(Icons.Default.ArrowBack, "Close")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = webViewTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = showWebViewUrl ?: "",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        reloadKey++
                                    }
                                ) {
                                    Icon(Icons.Default.Refresh, "Refresh", tint = PrimaryBlue)
                                }
                                IconButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(showWebViewUrl))
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Could not open browser", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, "Open in Browser", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        if (webViewProgressLoading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = PrimaryBlue)
                        }

                        if (hasWebViewError) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Error, "error", tint = AccentOrange, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Could not load page. Check network connection.", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        reloadKey++
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Retry Loading")
                                }
                            }
                        } else {
                            // Real Android WebView rendering
                            key(reloadKey) {
                                AndroidView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    factory = { ctx ->
                                        WebView(ctx).apply {
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.useWideViewPort = true
                                            settings.loadWithOverviewMode = true
                                            webViewClient = object : WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                                    return false // Keep inside in-app WebView
                                                }

                                                override fun onPageFinished(view: WebView?, url: String?) {
                                                    webViewProgressLoading = false
                                                }

                                                override fun onReceivedError(
                                                    view: WebView?,
                                                    errorCode: Int,
                                                    description: String?,
                                                    failingUrl: String?
                                                ) {
                                                    hasWebViewError = true
                                                    webViewProgressLoading = false
                                                }
                                            }
                                            loadUrl(showWebViewUrl ?: "https://google.com")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. AUTO-APPLY ENGINE OVERLAY DIALOGS
        // ==========================================
        // A. Active Processing Loading Indicator
        if (activeAutoApplyingJob != null && isLoggedIn) {
            Dialog(onDismissRequest = {}) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(progress = autoApplyProgress, color = PrimaryBlue, strokeWidth = 5.dp)
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "LS Auto-Apply Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = autoApplyProgressText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // B. Guest Registration Encouragement Prompt
        if (activeAutoApplyingJob != null && !isLoggedIn) {
            Dialog(onDismissRequest = { activeAutoApplyingJob = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(AccentOrange.copy(alpha = 0.1f), RoundedCornerShape(100)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Lock, "Lock", tint = AccentOrange, modifier = Modifier.size(28.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Register to Unlock Auto-Apply",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To allow the LS Services automated engine to map your skills, customize professional CV drafts, and register applications, please create a free secure profile.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { activeAutoApplyingJob = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Button(
                                onClick = {
                                    activeAutoApplyingJob = null
                                    selectedJobForDetail = null
                                    onNavigateToLogin()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Sign In / Join", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // C. Success Animation Modal Dialog
        if (showAutoApplySuccess != null) {
            Dialog(onDismissRequest = { showAutoApplySuccess = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(100)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.CheckCircle, "Success", tint = Color(0xFF2E7D32), modifier = Modifier.size(36.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Tap-Apply Submitted!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Your application for ${showAutoApplySuccess?.title} has been recorded. LS Services dispatch staff will double-check your custom CV draft and complete the final submission for you.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { showAutoApplySuccess = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Done", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3D. PROFILE INCOMPLETE PROMPT DIALOG
        // ==========================================
        if (showProfileIncompletePrompt != null) {
            Dialog(onDismissRequest = { showProfileIncompletePrompt = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(AccentOrange.copy(alpha = 0.1f), RoundedCornerShape(100)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountCircle, "Profile incomplete", tint = AccentOrange, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Complete Your Profile First",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "To auto-apply, the engine needs structured details (Education, Experience, Skills) to generate a professional CV. Complete your profile to proceed.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { showProfileIncompletePrompt = null },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Later", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                            Button(
                                onClick = {
                                    showProfileIncompletePrompt = null
                                    selectedJobForDetail = null
                                    viewModel.currentTab = "profile"
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.weight(1.5f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Complete Now", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // 4. REPORT VA_CANCY DIALOG FORM
        // ==========================================
        if (showReportDialogForJob != null) {
            Dialog(onDismissRequest = { showReportDialogForJob = null }) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Report Suspicious Listing",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Help LS Services keep our job board 100% verified and free from scams. Why are you reporting this vacancy?",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = reportReasonInput,
                            onValueChange = { reportReasonInput = it },
                            placeholder = { Text("E.g., duplicate listing, misleading description, suspicious fee demand, incorrect info...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AccentOrange)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = {
                                    showReportDialogForJob = null
                                    reportReasonInput = ""
                                }
                            ) {
                                Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (reportReasonInput.trim().isNotEmpty()) {
                                        viewModel.reportJob(showReportDialogForJob!!.id, reportReasonInput)
                                        Toast.makeText(context, "Thank you! Our safety staff will review this listing within 2 hours.", Toast.LENGTH_LONG).show()
                                        showReportDialogForJob = null
                                        reportReasonInput = ""
                                    } else {
                                        Toast.makeText(context, "Please enter a reason", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Submit Report", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun JobCard(
    job: MockJob,
    isBookmarked: Boolean,
    isApplied: Boolean,
    onToggleBookmark: () -> Unit,
    onCardClicked: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClicked() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: Initials Badge, Title & Org, Deadline pill on Top Right
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    OrgInitialsBadge(orgName = job.organization, size = 44)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = job.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = job.organization,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Deadline pill top right (matching screenshot)
                DeadlinePill(deadlineStr = job.deadline)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Row 2: Location, Job Type tags & Views count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = job.location,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Text(
                    text = job.jobType,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    text = "•",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Text(
                    text = "${job.viewsCount}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Solid Black View details button (matching screenshot)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    onClick = { onCardClicked() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = if (isApplied) "View details (Applied)" else "View details",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(
                    onClick = { onToggleBookmark() },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Bookmark button",
                        tint = if (isBookmarked) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

fun Modifier.drawVerticalScrollbar(
    scrollState: androidx.compose.foundation.ScrollState,
    color: Color = PrimaryBlue.copy(alpha = 0.5f),
    width: androidx.compose.ui.unit.Dp = 4.dp
): Modifier = drawWithContent {
    drawContent()
    if (scrollState.maxValue > 0) {
        val elementHeight = size.height
        val scrollOffset = scrollState.value.toFloat()
        val maxOffset = scrollState.maxValue.toFloat()
        val scrollbarHeight = (elementHeight * elementHeight / (elementHeight + maxOffset)).coerceAtLeast(24.dp.toPx())
        val scrollbarY = (scrollOffset / maxOffset) * (elementHeight - scrollbarHeight)

        drawRoundRect(
            color = color,
            topLeft = Offset(size.width - width.toPx(), scrollbarY),
            size = Size(width.toPx(), scrollbarHeight),
            cornerRadius = CornerRadius(width.toPx() / 2, width.toPx() / 2)
        )
    }
}
