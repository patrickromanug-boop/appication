package com.example.ui.tabs

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.example.data.supabase.CompanyAd
import com.example.data.supabase.UserApplication
import com.example.ui.AppViewModel
import com.example.ui.MockJob
import com.example.ui.ReportedJobItem

private val PrimaryBlue = Color(0xFF0D47A1)
private val AccentOrange = Color(0xFFFF6F00)
private val DangerRed = Color(0xFFD32F2F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPortalScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val jobs by viewModel.jobs.collectAsState()
    val applications by viewModel.allApplications.collectAsState()
    val reportedJobs by viewModel.reportedJobItems.collectAsState()
    val companyAds by viewModel.companyAds.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0: Jobs, 1: Post/Edit Job, 2: Company Ads, 3: Applications, 4: Reports

    // Edit Job state tracking
    var editingJob by remember { mutableStateOf<MockJob?>(null) }
    var jobToDelete by remember { mutableStateOf<MockJob?>(null) }

    // Job Search & Filter state
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") } // "all", "active", "archived"

    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://ls-admin-rbj9vypam-vision-african-child.vercel.app")
                                )
                                context.startActivity(intent)
                            }
                    ) {
                        Text(
                            text = "LS Services Web Admin Portal",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color.White
                        )
                        Text(
                            text = "ls-admin-rbj9vypam-vision-african-child.vercel.app",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("admin_portal_back_btn")) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://ls-admin-rbj9vypam-vision-african-child.vercel.app")
                            )
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Open Web Site", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Stats Row
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBadge("Total Jobs", jobs.size.toString(), PrimaryBlue)
                    StatBadge("Applications", applications.size.toString(), AccentOrange)
                    StatBadge("Company Ads", companyAds.size.toString(), Color(0xFF2E7D32))
                    StatBadge("Reports", reportedJobs.size.toString(), DangerRed)
                }
            }

            // Navigation Tabs Bar
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = PrimaryBlue
                        )
                    }
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("📋 Jobs (${jobs.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        editingJob = null
                        selectedTab = 1
                    },
                    text = { Text(if (editingJob == null) "➕ Post Vacancy" else "✏️ Edit Job", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("📢 Company Ads (${companyAds.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("📨 Applicants (${applications.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("🚩 Reports (${reportedJobs.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                )
            }

            // Tab Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> AdminJobListTab(
                        jobs = jobs,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        filterStatus = filterStatus,
                        onFilterChange = { filterStatus = it },
                        onEditJob = { job ->
                            editingJob = job
                            selectedTab = 1
                        },
                        onToggleDeactivate = { job ->
                            if (job.status == "active") {
                                viewModel.deactivateJob(job.id)
                            } else {
                                viewModel.updateJob(job.copy(status = "active"))
                            }
                        },
                        onDeleteJob = { job -> jobToDelete = job },
                        onPostNew = {
                            editingJob = null
                            selectedTab = 1
                        }
                    )

                    1 -> AdminPostEditJobTab(
                        viewModel = viewModel,
                        existingJob = editingJob,
                        onComplete = {
                            editingJob = null
                            selectedTab = 0
                        }
                    )

                    2 -> AdminCompanyAdsTab(
                        viewModel = viewModel,
                        ads = companyAds
                    )

                    3 -> AdminApplicationsTab(
                        applications = applications,
                        onUpdateStatus = { appId, status, notes ->
                            viewModel.updateApplicationStatus(appId, status, notes)
                        }
                    )

                    4 -> AdminReportedJobsTab(
                        reportedJobs = reportedJobs,
                        onDismiss = { reportId -> viewModel.dismissReportedJob(reportId) },
                        onDeleteJob = { jobId -> viewModel.deleteJob(jobId) }
                    )
                }
            }
        }
    }

    // Delete Job Confirmation Dialog
    jobToDelete?.let { job ->
        AlertDialog(
            onDismissRequest = { jobToDelete = null },
            icon = { Icon(Icons.Default.Warning, contentDescription = "Delete", tint = DangerRed) },
            title = { Text("Delete Job Listing Permanently?") },
            text = {
                Text(
                    "Are you sure you want to permanently delete '${job.title}' at '${job.organization}' from LS Services database?\n\nThis action cannot be undone."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteJob(job.id)
                        jobToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed)
                ) {
                    Text("Delete Permanently", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { jobToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatBadge(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Black, fontSize = 18.sp, color = color)
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

/* -------------------------------------------------------------------------
   TAB 1: MANAGED JOB POSTINGS LIST
   ------------------------------------------------------------------------- */
@Composable
private fun AdminJobListTab(
    jobs: List<MockJob>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    filterStatus: String,
    onFilterChange: (String) -> Unit,
    onEditJob: (MockJob) -> Unit,
    onToggleDeactivate: (MockJob) -> Unit,
    onDeleteJob: (MockJob) -> Unit,
    onPostNew: () -> Unit
) {
    val filteredJobs = remember(jobs, searchQuery, filterStatus) {
        jobs.filter { job ->
            val matchesSearch = searchQuery.isBlank() ||
                    job.title.contains(searchQuery, ignoreCase = true) ||
                    job.organization.contains(searchQuery, ignoreCase = true) ||
                    job.category.contains(searchQuery, ignoreCase = true)

            val matchesFilter = when (filterStatus) {
                "active" -> job.status.equals("active", ignoreCase = true)
                "archived" -> job.status.equals("archived", ignoreCase = true)
                else -> true
            }

            matchesSearch && matchesFilter
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search by title, org, or category...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, "Search", modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    { IconButton(onClick = { onSearchChange("") }) { Icon(Icons.Default.Clear, "Clear") } }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Button(
                onClick = onPostNew,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Add, "Add", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Post Job", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Status Filter Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all" to "All (${jobs.size})", "active" to "Active", "archived" to "Archived / Inactive").forEach { (key, label) ->
                val isSelected = filterStatus == key
                FilterChip(
                    selected = isSelected,
                    onClick = { onFilterChange(key) },
                    label = { Text(label, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                        selectedLabelColor = PrimaryBlue
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (filteredJobs.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.WorkOff, "No jobs", modifier = Modifier.size(48.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No vacancies matched your query.", fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredJobs, key = { it.id }) { job ->
                    AdminJobCardItem(
                        job = job,
                        onEdit = { onEditJob(job) },
                        onToggleDeactivate = { onToggleDeactivate(job) },
                        onDelete = { onDeleteJob(job) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminJobCardItem(
    job: MockJob,
    onEdit: () -> Unit,
    onToggleDeactivate: () -> Unit,
    onDelete: () -> Unit
) {
    val isActive = job.status.equals("active", ignoreCase = true)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (isActive) PrimaryBlue.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(job.title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Text("${job.organization} • ${job.location}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }

                Box(
                    modifier = Modifier
                        .background(if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isActive) "ACTIVE" else "ARCHIVED",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isActive) Color(0xFF2E7D32) else DangerRed
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .background(PrimaryBlue.copy(alpha = 0.08f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(job.category, fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    modifier = Modifier
                        .background(Color.Gray.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(job.jobType, fontSize = 10.sp, color = Color.DarkGray)
                }
                Text("• Deadline: ${job.deadline}", fontSize = 10.sp, color = Color.Gray)
            }

            if (job.purpose.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Summary: ${job.purpose}",
                    fontSize = 11.sp,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row (Edit, Toggle Archive, Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedButton(
                    onClick = onToggleDeactivate,
                    modifier = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(
                        if (isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        "Toggle",
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isActive) "Archive" else "Activate", fontSize = 11.sp)
                }

                Spacer(modifier = Modifier.width(8.dp))

                // DELETE BUTTON
                Button(
                    onClick = onDelete,
                    modifier = Modifier.height(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.White, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------
   TAB 2: POST / EDIT JOB FORM (NO SALARY FIELD, STRUCTURED SUMMARY & QUALIFICATIONS)
   ------------------------------------------------------------------------- */
@Composable
private fun AdminPostEditJobTab(
    viewModel: AppViewModel,
    existingJob: MockJob?,
    onComplete: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    val locations by viewModel.locations.collectAsState()
    val jobTypes by viewModel.jobTypes.collectAsState()

    var title by remember(existingJob) { mutableStateOf(existingJob?.title ?: "") }
    var organization by remember(existingJob) { mutableStateOf(existingJob?.organization ?: "") }
    var location by remember(existingJob) { mutableStateOf(existingJob?.location ?: locations.firstOrNull() ?: "Kampala, Uganda") }
    var category by remember(existingJob) { mutableStateOf(existingJob?.category ?: categories.firstOrNull() ?: "Engineering") }
    var jobType by remember(existingJob) { mutableStateOf(existingJob?.jobType ?: "Full Time") }
    var deadline by remember(existingJob) { mutableStateOf(existingJob?.deadline ?: "2026-12-31") }
    var purpose by remember(existingJob) { mutableStateOf(existingJob?.purpose ?: "") }
    var requirements by remember(existingJob) { mutableStateOf(existingJob?.requirements ?: "") }
    var otherDetails by remember(existingJob) { mutableStateOf(existingJob?.otherDetails ?: "") }
    var officialLink by remember(existingJob) { mutableStateOf(existingJob?.officialLink ?: "") }
    var applicationMethod by remember(existingJob) { mutableStateOf(existingJob?.applicationMethod ?: "auto_apply_supported") }

    var formError by remember { mutableStateOf<String?>(null) }
    var isPosting by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
            ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.WorkHistory, "Job", tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (existingJob == null) "Post New Job Vacancy" else "Edit Job #${existingJob.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = PrimaryBlue
                        )
                        Text(
                            text = "Fill in the job summary, qualifications, and placement details. (Salary field excluded per configuration).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        formError?.let { err ->
            item {
                Text(err, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Section 1: Basic Position Info
        item {
            Text("1. Basic Role & Employer Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
        }

        item {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it; formError = null },
                label = { Text("Job Title *") },
                placeholder = { Text("e.g., Senior Civil Engineer / Accounts Assistant") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = organization,
                onValueChange = { organization = it; formError = null },
                label = { Text("Organization / Employer Name *") },
                placeholder = { Text("e.g., LS Construction / Ministry of Health") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = deadline,
                    onValueChange = { deadline = it },
                    label = { Text("Deadline") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = jobType,
                    onValueChange = { jobType = it },
                    label = { Text("Contract Type") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }

        // Section 2: Job Summary & Purpose
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("2. Job Summary & Role Purpose", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
        }

        item {
            OutlinedTextField(
                value = purpose,
                onValueChange = { purpose = it },
                label = { Text("Job Summary & Main Purpose *") },
                placeholder = { Text("Describe the primary responsibility, role overview, and team context...") },
                modifier = Modifier.fillMaxWidth().height(110.dp),
                maxLines = 4
            )
        }

        // Section 3: Qualifications & Requirements
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("3. Required Qualifications & Candidate Requirements", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
        }

        item {
            OutlinedTextField(
                value = requirements,
                onValueChange = { requirements = it },
                label = { Text("Qualifications & Key Criteria *") },
                placeholder = { Text("• Bachelor's Degree in related field\n• 3+ years relevant experience\n• Proficiency in required tools/skills") },
                modifier = Modifier.fillMaxWidth().height(140.dp),
                maxLines = 6
            )
        }

        // Section 4: External Application & Additional Info
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text("4. Application Protocol & Official Details", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
        }

        item {
            OutlinedTextField(
                value = officialLink,
                onValueChange = { officialLink = it },
                label = { Text("Official Employer Portal Link (Optional)") },
                placeholder = { Text("https://careers.company.com/apply") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
        }

        item {
            OutlinedTextField(
                value = otherDetails,
                onValueChange = { otherDetails = it },
                label = { Text("Additional Submission Instructions") },
                placeholder = { Text("e.g., Physical submissions accepted at LS Kampala Office...") },
                modifier = Modifier.fillMaxWidth().height(80.dp),
                maxLines = 3
            )
        }

        // Live Preview Toggle
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show Live Candidate Card Preview", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Switch(
                    checked = showPreview,
                    onCheckedChange = { showPreview = it }
                )
            }
        }

        if (showPreview) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("PREVIEW CARD FOR CANDIDATES", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(title.ifBlank { "Job Title Placeholder" }, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("${organization.ifBlank { "Organization" }} • ${location.ifBlank { "Location" }}", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(purpose.ifBlank { "Job summary content will appear here..." }, fontSize = 12.sp, maxLines = 3)
                    }
                }
            }
        }

        // Submit Button
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (title.isBlank() || organization.isBlank()) {
                        formError = "Please enter Job Title and Organization Name."
                        return@Button
                    }
                    isPosting = true

                    val newJob = MockJob(
                        id = existingJob?.id ?: java.util.UUID.randomUUID().toString(),
                        title = title.trim(),
                        organization = organization.trim(),
                        location = location.trim().ifBlank { "Kampala, Uganda" },
                        jobType = jobType.trim(),
                        category = category.trim().ifBlank { "General" },
                        salary = "Negotiable",
                        purpose = purpose.trim(),
                        requirements = requirements.trim(),
                        otherDetails = otherDetails.trim(),
                        deadline = deadline.trim().ifBlank { "2026-12-31" },
                        officialLink = officialLink.trim(),
                        status = existingJob?.status ?: "active"
                    )

                    if (existingJob == null) {
                        viewModel.postJob(newJob) { success ->
                            isPosting = false
                            if (success) onComplete()
                        }
                    } else {
                        viewModel.updateJob(newJob)
                        isPosting = false
                        onComplete()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                enabled = !isPosting
            ) {
                if (isPosting) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, "Publish", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (existingJob == null) "Publish Job Listing" else "Save Job Changes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------
   TAB 3: COMPANY ADVERTISING & PROMOTIONAL BANNERS
   ------------------------------------------------------------------------- */
@Composable
private fun AdminCompanyAdsTab(
    viewModel: AppViewModel,
    ads: List<CompanyAd>
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAd by remember { mutableStateOf<CompanyAd?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Company Advertising Hub", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Upload banners & spotlight employer brands in LS App", fontSize = 11.sp, color = Color.Gray)
            }

            Button(
                onClick = {
                    editingAd = null
                    showAddDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Campaign, "Ad", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Ad Banner", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (ads.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("No company advertisements published.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(ads, key = { it.id }) { ad ->
                    CompanyAdCardItem(
                        ad = ad,
                        onToggleActive = { viewModel.toggleCompanyAdStatus(ad.id) },
                        onEdit = {
                            editingAd = ad
                            showAddDialog = true
                        },
                        onDelete = { viewModel.deleteCompanyAd(ad.id) }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        CompanyAdFormDialog(
            existingAd = editingAd,
            onDismiss = { showAddDialog = false },
            onSave = { ad ->
                if (editingAd == null) {
                    viewModel.addCompanyAd(ad)
                } else {
                    viewModel.updateCompanyAd(ad)
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun CompanyAdCardItem(
    ad: CompanyAd,
    onToggleActive: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Photo Preview
            if (ad.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = ad.photoUrl,
                    contentDescription = ad.companyName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(ad.companyName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = PrimaryBlue)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (ad.isActive) "ACTIVE" else "PAUSED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ad.isActive) Color(0xFF2E7D32) else Color.Gray
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Switch(
                            checked = ad.isActive,
                            onCheckedChange = { onToggleActive() },
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }

                Text(ad.headline, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(ad.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 3)

                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (ad.websiteUrl.isNotBlank()) {
                        Text("🔗 ${ad.websiteUrl}", fontSize = 10.sp, color = PrimaryBlue)
                    }
                    if (ad.contactPhone.isNotBlank()) {
                        Text("📞 ${ad.contactPhone}", fontSize = 10.sp, color = Color.DarkGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(onClick = onEdit, modifier = Modifier.height(30.dp), contentPadding = PaddingValues(horizontal = 8.dp)) {
                        Icon(Icons.Default.Edit, "Edit", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Ad", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CompanyAdFormDialog(
    existingAd: CompanyAd?,
    onDismiss: () -> Unit,
    onSave: (CompanyAd) -> Unit
) {
    var companyName by remember { mutableStateOf(existingAd?.companyName ?: "") }
    var headline by remember { mutableStateOf(existingAd?.headline ?: "") }
    var description by remember { mutableStateOf(existingAd?.description ?: "") }
    var photoUrl by remember { mutableStateOf(existingAd?.photoUrl ?: "") }
    var websiteUrl by remember { mutableStateOf(existingAd?.websiteUrl ?: "") }
    var contactPhone by remember { mutableStateOf(existingAd?.contactPhone ?: "") }
    var category by remember { mutableStateOf(existingAd?.category ?: "Corporate") }

    val presetPhotoUrls = listOf(
        "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800&q=80",
        "https://images.unsplash.com/photo-1573164713988-8665fc963095?w=800&q=80",
        "https://images.unsplash.com/photo-1556761175-5973dc0f32e7?w=800&q=80"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existingAd == null) "Create Company Advertisement" else "Edit Company Ad") },
        text = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    OutlinedTextField(
                        value = companyName,
                        onValueChange = { companyName = it },
                        label = { Text("Company / Brand Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = headline,
                        onValueChange = { headline = it },
                        label = { Text("Ad Headline / Offer *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = photoUrl,
                        onValueChange = { photoUrl = it },
                        label = { Text("Banner Photo URL / Image Link *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }

                item {
                    Text("Or choose a preset corporate banner:", fontSize = 11.sp, color = Color.Gray)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        presetPhotoUrls.forEachIndexed { idx, url ->
                            OutlinedButton(
                                onClick = { photoUrl = url },
                                modifier = Modifier.height(28.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp)
                            ) {
                                Text("Preset ${idx + 1}", fontSize = 10.sp)
                            }
                        }
                    }
                }

                if (photoUrl.isNotBlank()) {
                    item {
                        Text("Photo Preview:", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Advertising Summary & Details") },
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        maxLines = 3
                    )
                }
                item {
                    OutlinedTextField(
                        value = websiteUrl,
                        onValueChange = { websiteUrl = it },
                        label = { Text("Website / Promotion Link") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                item {
                    OutlinedTextField(
                        value = contactPhone,
                        onValueChange = { contactPhone = it },
                        label = { Text("WhatsApp / Contact Phone") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (companyName.isNotBlank() && headline.isNotBlank()) {
                        onSave(
                            CompanyAd(
                                id = existingAd?.id ?: java.util.UUID.randomUUID().toString(),
                                companyName = companyName.trim(),
                                headline = headline.trim(),
                                description = description.trim(),
                                photoUrl = photoUrl.trim().ifBlank { presetPhotoUrls.first() },
                                websiteUrl = websiteUrl.trim(),
                                contactPhone = contactPhone.trim(),
                                category = category.trim(),
                                isActive = existingJobIsActive(existingAd)
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Publish Advertisement", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun existingJobIsActive(ad: CompanyAd?): Boolean {
    return ad?.isActive ?: true
}

/* -------------------------------------------------------------------------
   TAB 4: APPLICATIONS QUEUE
   ------------------------------------------------------------------------- */
@Composable
private fun AdminApplicationsTab(
    applications: List<UserApplication>,
    onUpdateStatus: (String, String, String?) -> Unit
) {
    if (applications.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No candidate submissions found.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(applications, key = { it.id }) { app ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(app.jobTitle ?: "Job Application", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Box(
                                modifier = Modifier
                                    .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(app.status, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Candidate: ${app.candidateName} • ${app.candidatePhone}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Text("Email: ${app.candidateEmail}", fontSize = 11.sp, color = Color.Gray)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("Forwarded to Employer", "Accepted", "Rejected").forEach { newStatus ->
                                OutlinedButton(
                                    onClick = { onUpdateStatus(app.id, newStatus, null) },
                                    modifier = Modifier.height(28.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp)
                                ) {
                                    Text(newStatus, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------
   TAB 5: REPORTED JOBS
   ------------------------------------------------------------------------- */
@Composable
private fun AdminReportedJobsTab(
    reportedJobs: List<ReportedJobItem>,
    onDismiss: (String) -> Unit,
    onDeleteJob: (String) -> Unit
) {
    if (reportedJobs.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No vacancy flag reports.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(reportedJobs, key = { it.id }) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Flagged Role: ${report.jobTitle}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Reported By: ${report.reporterEmail}", fontSize = 11.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Reason: ${report.reason}", fontSize = 12.sp, color = DangerRed)

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { onDismiss(report.id) }, modifier = Modifier.height(30.dp)) {
                                Text("Dismiss Report", fontSize = 11.sp)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    onDeleteJob(report.jobId)
                                    onDismiss(report.id)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text("Delete Job Post", fontSize = 11.sp, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
