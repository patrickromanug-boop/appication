package com.example.ui.tabs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.AppViewModel
import com.example.ui.MockJob
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedScreen(
    viewModel: AppViewModel,
    onNavigateToHome: () -> Unit
) {
    val context = LocalContext.current
    val bookmarkedJobIds by viewModel.bookmarks.collectAsState()
    val allJobs by viewModel.jobs.collectAsState()
    val appliedJobIds by viewModel.appliedJobs.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    val savedJobs = remember(allJobs, bookmarkedJobIds) {
        allJobs.filter { it.id in bookmarkedJobIds }
    }

    var selectedJobForDetail by remember { mutableStateOf<MockJob?>(null) }
    var showReportDialogForJob by remember { mutableStateOf<MockJob?>(null) }
    var showWebViewUrl by remember { mutableStateOf<String?>(null) }
    var webViewTitle by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Saved Jobs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                        if (savedJobs.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .background(PrimaryBlue.copy(alpha = 0.12f), CircleShape)
                                    .padding(horizontal = 10.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${savedJobs.size}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (savedJobs.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                bookmarkedJobIds.toList().forEach { id ->
                                    viewModel.toggleBookmark(id)
                                }
                                Toast.makeText(context, "Cleared all saved jobs", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Clear All", fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )

            if (savedJobs.isEmpty()) {
                // Empty State
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.BookmarkBorder,
                                contentDescription = "No bookmarks",
                                tint = PrimaryBlue,
                                modifier = Modifier.size(44.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "No Saved Jobs Yet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Keep track of interesting job opportunities by bookmarking them on the Home tab. Saved jobs will appear here for instant quick access.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(28.dp))

                        Button(
                            onClick = onNavigateToHome,
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = "Explore Listings",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            } else {
                // Saved Jobs List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                ) {
                    items(savedJobs, key = { it.id }) { job ->
                        JobCard(
                            job = job,
                            isBookmarked = true,
                            isApplied = appliedJobIds.contains(job.id),
                            onToggleBookmark = {
                                viewModel.toggleBookmark(job.id)
                                Toast.makeText(context, "Removed from saved jobs", Toast.LENGTH_SHORT).show()
                            },
                            onCardClicked = {
                                viewModel.incrementViewsCount(job.id)
                                selectedJobForDetail = job
                            }
                        )
                    }
                }
            }
        }

        // Animated Job Detail Popup Modal
        AnimatedVisibility(
            visible = selectedJobForDetail != null,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            selectedJobForDetail?.let { job ->
                val isCurrentBookmarked = bookmarkedJobIds.contains(job.id)
                val isCurrentApplied = appliedJobIds.contains(job.id)

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
                            .clickable(enabled = false) {},
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Sticky Top Header Row
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
                                    IconButton(
                                        onClick = {
                                            viewModel.toggleBookmark(job.id)
                                            val msg = if (isCurrentBookmarked) "Removed from saved jobs" else "Added to saved jobs"
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isCurrentBookmarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                            contentDescription = "Bookmark button",
                                            tint = if (isCurrentBookmarked) PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(
                                                    Intent.EXTRA_TEXT,
                                                    "Check out this vacancy: ${job.title} at ${job.organization} (${job.location}).\nApply via LS Services or directly here: ${job.officialLink}"
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

                                    IconButton(
                                        onClick = { showReportDialogForJob = job }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = "Report button",
                                            tint = if (viewModel.reportedJobs.value.contains(job.id)) AccentOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }

                            // Content Details
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = 60.dp, bottom = 80.dp)
                                    .verticalScroll(rememberScrollState())
                                    .padding(horizontal = 24.dp)
                            ) {
                                Text(
                                    text = job.title,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = job.organization,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, "Location", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(job.location, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                )

                                Text("Job Summary & Purpose", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(job.purpose, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

                                Spacer(modifier = Modifier.height(16.dp))

                                Text("Key Requirements & Qualifications", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(job.requirements, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))

                                Spacer(modifier = Modifier.height(24.dp))
                            }

                            // Fixed Bottom Action Bar
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomCenter),
                                color = MaterialTheme.colorScheme.surface,
                                shadowElevation = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            if (!isOnline) {
                                                Toast.makeText(context, "You are offline. Connect to the internet to view official site.", Toast.LENGTH_SHORT).show()
                                            } else {
                                                webViewTitle = job.title
                                                showWebViewUrl = job.officialLink
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        border = BorderStroke(1.dp, PrimaryBlue),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text("Official Site", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Report Dialog
        if (showReportDialogForJob != null) {
            val jobToReport = showReportDialogForJob!!
            AlertDialog(
                onDismissRequest = { showReportDialogForJob = null },
                title = { Text("Report Job Listing") },
                text = { Text("Are you sure you want to report '${jobToReport.title}' for misleading or inappropriate content?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.reportJob(jobToReport.id, "Flagged from Saved tab")
                            showReportDialogForJob = null
                            Toast.makeText(context, "Report submitted for review", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Submit Report")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReportDialogForJob = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // In-App WebView Overlay Dialog
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

                    LaunchedEffect(showWebViewUrl, reloadKey) {
                        webViewProgressLoading = true
                        hasWebViewError = false
                        kotlinx.coroutines.delay(10000)
                        if (webViewProgressLoading) {
                            webViewProgressLoading = false
                        }
                    }

                    Column(modifier = Modifier.fillMaxSize()) {
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
                                    Icon(Icons.Default.Close, "Close")
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(
                                        text = webViewTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = showWebViewUrl ?: "",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { reloadKey++ }) {
                                    Icon(Icons.Default.Refresh, "Refresh", tint = PrimaryBlue)
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
                                Icon(Icons.Default.Warning, "error", tint = AccentOrange, modifier = Modifier.size(64.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Could not load page. Check network connection.", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { reloadKey++ },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Retry Loading")
                                }
                            }
                        } else {
                            key(reloadKey) {
                                androidx.compose.ui.viewinterop.AndroidView(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    factory = { ctx ->
                                        android.webkit.WebView(ctx).apply {
                                            settings.javaScriptEnabled = true
                                            settings.domStorageEnabled = true
                                            settings.useWideViewPort = true
                                            settings.loadWithOverviewMode = true
                                            webViewClient = object : android.webkit.WebViewClient() {
                                                override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                                    return false
                                                }

                                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                                    webViewProgressLoading = false
                                                }

                                                override fun onReceivedError(
                                                    view: android.webkit.WebView?,
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
    }
}
