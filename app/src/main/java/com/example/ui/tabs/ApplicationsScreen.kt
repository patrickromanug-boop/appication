package com.example.ui.tabs

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Assignment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.supabase.UserApplication
import com.example.ui.AppViewModel
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationsScreen(
    viewModel: AppViewModel,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userApplications by viewModel.userApplications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var selectedFilter by remember { mutableStateOf("all") } // "all", "pending", "submitted", "needs_more_info"
    var selectedAppForDetail by remember { mutableStateOf<UserApplication?>(null) }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            viewModel.loadUserApplications()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Applications", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                actions = {
                    if (isLoggedIn) {
                        IconButton(onClick = {
                            viewModel.loadUserApplications()
                            Toast.makeText(context, "Applications updated", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh applications queue")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (!isLoggedIn) {
                // Guest Onboarding Screen
                GuestApplicationsOnboarding(onNavigateToLogin)
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Filter Chips Bar (2 rows so no horizontal scroll is needed)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val filterOptions = listOf(
                            "all" to "All Submissions",
                            "pending" to "Pending",
                            "submitted" to "Submitted",
                            "needs_more_info" to "Action Required"
                        )

                        // Row 1: All Submissions & Pending
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filterOptions.take(2).forEach { (filterKey, label) ->
                                val isSelected = selectedFilter == filterKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = filterKey },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                                        selectedLabelColor = PrimaryBlue,
                                        selectedLeadingIconColor = PrimaryBlue
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = Color.LightGray,
                                        selectedBorderColor = PrimaryBlue,
                                        selectedBorderWidth = 1.5.dp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Row 2: Submitted & Action Required
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filterOptions.drop(2).forEach { (filterKey, label) ->
                                val isSelected = selectedFilter == filterKey
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedFilter = filterKey },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.12f),
                                        selectedLabelColor = PrimaryBlue,
                                        selectedLeadingIconColor = PrimaryBlue
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = isSelected,
                                        borderColor = Color.LightGray,
                                        selectedBorderColor = PrimaryBlue,
                                        selectedBorderWidth = 1.5.dp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Filtered applications list (memoized to prevent re-filtering on every recomposition)
                    val filteredApps = remember(userApplications, selectedFilter) {
                        userApplications.filter { app ->
                            selectedFilter == "all" || app.status.equals(selectedFilter, ignoreCase = true)
                        }
                    }

                    if (filteredApps.isEmpty()) {
                        EmptyApplicationsState(selectedFilter)
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 20.dp)
                        ) {
                            items(filteredApps, key = { it.id }) { app ->
                                ApplicationItemCard(
                                    application = app,
                                    onClick = { selectedAppForDetail = app }
                                )
                            }
                        }
                    }
                }
            }

            // Application details Bottom Sheet Dialog
            selectedAppForDetail?.let { app ->
                ApplicationDetailBottomSheet(
                    application = app,
                    onDismiss = { selectedAppForDetail = null }
                )
            }
        }
    }
}

@Composable
fun GuestApplicationsOnboarding(onNavigateToLogin: () -> Unit) {
    val scrollState = rememberScrollState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = "Authenticated section",
            tint = AccentOrange,
            modifier = Modifier.size(72.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Sign In to Track Applications",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "To auto-generate CVs, apply with a single tap, and coordinate applications with LS Services staff, please establish a free secure account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Button(
            onClick = onNavigateToLogin,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Register / Sign In",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun EmptyApplicationsState(filter: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Assignment,
            contentDescription = "No applications matching filter",
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "No Submissions Found",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(8.dp))

        val textDesc = when (filter) {
            "pending" -> "You don't have any pending automatic applications currently being processed."
            "submitted" -> "Once employer portals confirm receipt of your dispatched details, they appear under 'Submitted'."
            "needs_more_info" -> "All your submissions are healthy! None require supplementary info from staff."
            else -> "Any jobs you apply for using LS Services' automated tap-apply system will appear here, monitored by our local staff."
        }

        Text(
            text = textDesc,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun ApplicationItemCard(
    application: UserApplication,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("application_item_card_${application.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = application.jobTitle ?: "Job Application",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = application.jobOrganization ?: "Organization Link",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ApplicationStatusBadge(status = application.status)
            }

            Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Applied date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "Applied Date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val cleanDate = application.appliedAt.substringBefore("T")
                    Text(
                        text = "Applied on $cleanDate",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Documents attachment counter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Docs",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${application.documentsAttached.size} supplementary docs",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = PrimaryBlue
                    )
                }
            }
        }
    }
}

@Composable
fun ApplicationStatusBadge(status: String) {
    val (bgColor, textColor, label) = when (status.lowercase()) {
        "pending" -> Triple(Color(0xFFFFF3E0), Color(0xFFE65100), "Pending")
        "submitted" -> Triple(Color(0xFFE8F5E9), Color(0xFF1B5E20), "Submitted")
        "needs_more_info" -> Triple(Color(0xFFFEEBEE), Color(0xFFC62828), "Action Required")
        else -> Triple(Color(0xFFF5F5F5), Color(0xFF616161), status.uppercase())
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(textColor)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationDetailBottomSheet(
    application: UserApplication,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Text(
                text = application.jobTitle ?: "Job Detail Record",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = application.jobOrganization ?: "Organization Link",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (application.status.lowercase()) {
                        "pending" -> Color(0xFFFFF3E0)
                        "submitted" -> Color(0xFFE8F5E9)
                        "needs_more_info" -> Color(0xFFFEEBEE)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (application.status.lowercase()) {
                                "pending" -> Icons.Default.Info
                                "submitted" -> Icons.Default.CheckCircle
                                else -> Icons.Default.Warning
                            },
                            contentDescription = "Status",
                            tint = when (application.status.lowercase()) {
                                "pending" -> Color(0xFFE65100)
                                "submitted" -> Color(0xFF1B5E20)
                                else -> Color(0xFFC62828)
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "STATUS: ${application.status.uppercase()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = when (application.status.lowercase()) {
                                "pending" -> Color(0xFFE65100)
                                "submitted" -> Color(0xFF1B5E20)
                                else -> Color(0xFFC62828)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    val statusExplanation = when (application.status.lowercase()) {
                        "pending" -> "Our dispatch bot and staff are organizing and compiling this submission. We will submit it to the employer shortly."
                        "submitted" -> "This application was successfully received by the employer's screening platform. Keep an eye on your email!"
                        "needs_more_info" -> "The employer has requested additional document verification or clarification. Please contact LS Services support."
                        else -> ""
                    }
                    Text(
                        text = statusExplanation,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Generated CV
            Text("Generated CV Document", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Menu, "PDF", tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Automatic_CV.pdf", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Standard PDF Format", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Button(
                        onClick = {
                            if (application.generatedCvUrl.isNotEmpty()) {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(application.generatedCvUrl))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Cannot open PDF link", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "CV URL not available", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("Open", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section: Attached Documents
            Text("Attached Supplementary Documents", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
            Spacer(modifier = Modifier.height(8.dp))

            if (application.documentsAttached.isEmpty()) {
                Text("No additional documents were attached to this application.", fontStyle = FontStyle.Italic, fontSize = 12.sp, color = Color.Gray)
            } else {
                application.documentsAttached.forEachIndexed { idx, docUrl ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Share, "Attachment", tint = AccentOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Attached_File_${idx + 1}.pdf", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(docUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open attachment", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "View doc", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Close Button
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Close Details", fontWeight = FontWeight.Bold)
            }
        }
    }
}
