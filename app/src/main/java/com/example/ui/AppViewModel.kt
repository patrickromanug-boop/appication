package com.example.ui

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preference.PreferencesManager
import com.example.data.supabase.SupabaseClient
import com.example.data.supabase.UserProfile
import com.example.data.supabase.UserSubscription
import com.example.data.supabase.UserDocument
import com.example.data.supabase.UserApplication
import com.example.data.supabase.ReferralRecord
import com.example.util.CacheUtils.formatRelativeTime
import com.example.util.CacheUtils.parseMockJobList
import com.example.util.CacheUtils.parseUserApplicationList
import com.example.util.CacheUtils.parseUserProfile
import com.example.util.CacheUtils.toJsonString
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

enum class OfflineBannerState {
    HIDDEN,
    OFFLINE,
    BACK_ONLINE
}

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val client = SupabaseClient(application)

    companion object {
        private const val TAG = "AppViewModel"

        fun evaluateJobMatchStatic(job: MockJob, profile: UserProfile?): JobMatchResult {
            if (profile == null) return JobMatchResult(isMatch = true, matchedReason = "All Vacancies")

            val categories = profile.preferredCategories.map { it.trim() }.filter { it.isNotBlank() }
            val locations = profile.preferredLocations.map { it.trim() }.filter { it.isNotBlank() }
            val skills = profile.skills.map { it.trim() }.filter { it.isNotBlank() }

            // If user hasn't set any specific filter criteria, treat as matching all jobs
            if (categories.isEmpty() && locations.isEmpty() && skills.isEmpty()) {
                return JobMatchResult(isMatch = true, matchedReason = "All Vacancies (No restrictive filters set)")
            }

            val matchedCat = categories.find { cat ->
                cat.equals("All", ignoreCase = true) ||
                job.category.contains(cat, ignoreCase = true) ||
                cat.contains(job.category, ignoreCase = true) ||
                cat.split("&", "/", ",", " ", "-").any { token ->
                    val clean = token.trim().lowercase()
                    clean.length >= 3 && job.category.lowercase().contains(clean)
                } ||
                job.category.split("&", "/", ",", " ", "-").any { jToken ->
                    val clean = jToken.trim().lowercase()
                    clean.length >= 3 && cat.lowercase().contains(clean)
                }
            }

            val matchedLoc = locations.find { loc ->
                loc.equals("All", ignoreCase = true) ||
                loc.equals("Any Location", ignoreCase = true) ||
                job.location.contains(loc, ignoreCase = true) ||
                loc.contains(job.location, ignoreCase = true) ||
                loc.split(",", "/", " ", "-").any { token ->
                    val clean = token.trim().lowercase()
                    clean.length >= 3 && job.location.lowercase().contains(clean)
                }
            }

            val matchedSkill = skills.find { skill ->
                val clean = skill.lowercase()
                clean.isNotBlank() && (
                    job.title.lowercase().contains(clean) ||
                    job.requirements.lowercase().contains(clean) ||
                    job.purpose.lowercase().contains(clean) ||
                    job.otherDetails.lowercase().contains(clean) ||
                    job.category.lowercase().contains(clean) ||
                    job.organization.lowercase().contains(clean) ||
                    job.location.lowercase().contains(clean)
                )
            }

            return when {
                matchedCat != null && matchedLoc != null -> JobMatchResult(true, "Category '$matchedCat' & Location '$matchedLoc'")
                matchedCat != null -> JobMatchResult(true, "Category '$matchedCat'")
                matchedLoc != null -> JobMatchResult(true, "Location '$matchedLoc'")
                matchedSkill != null -> JobMatchResult(true, "Keyword '$matchedSkill'")
                else -> JobMatchResult(false, null)
            }
        }
    }

    // UI States
    var currentTab by mutableStateOf("home")
    val tabHistory = mutableStateListOf("home")
    var hasShownWelcomeBannerThisSession by mutableStateOf(false)
    var showExitConfirmDialog by mutableStateOf(false)
    
    var activeApplyFlowJob by mutableStateOf<MockJob?>(null)
    var showNotificationExplanationDialog by mutableStateOf(false)
    var showUpgradePrompt by mutableStateOf(false)
    var globalJobDetailToShow by mutableStateOf<MockJob?>(null)
    var profileSubScreen by mutableStateOf("main") // "main", "completion_flow", "document_vault", "subscription_comparison"
    var hasShownCvOnboarding by mutableStateOf(prefs.hasShownCvOnboarding)
    var dailyJobDetailViews by mutableStateOf(0)

    fun markCvOnboardingShown() {
        prefs.hasShownCvOnboarding = true
        hasShownCvOnboarding = true
    }
    var showPremiumBannerToday by mutableStateOf(false)
    var isPremiumBannerDismissedToday by mutableStateOf(false)

    fun selectTab(tabId: String) {
        if (currentTab != tabId) {
            currentTab = tabId
            if (tabHistory.lastOrNull() != tabId) {
                tabHistory.add(tabId)
            }
        }
    }

    fun popTabHistory(): Boolean {
        if (tabHistory.size > 1) {
            tabHistory.removeAt(tabHistory.size - 1)
            currentTab = tabHistory.last()
            return true
        }
        return false
    }

    
    private val _themeMode = MutableStateFlow(prefs.themePreference) // "light", "dark", "system"
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.isLoggedIn)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _userSubscription = MutableStateFlow<UserSubscription?>(null)
    val userSubscription: StateFlow<UserSubscription?> = _userSubscription.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _userDocuments = MutableStateFlow<List<UserDocument>>(emptyList())
    val userDocuments: StateFlow<List<UserDocument>> = _userDocuments.asStateFlow()

    private val _userApplications = MutableStateFlow<List<UserApplication>>(emptyList())
    val userApplications: StateFlow<List<UserApplication>> = _userApplications.asStateFlow()

    // Referral Program, Services Popup & Onboarding state
    private val _pendingReferralCode = MutableStateFlow<String?>(prefs.pendingReferralCode)
    val pendingReferralCode: StateFlow<String?> = _pendingReferralCode.asStateFlow()

    private val _userReferrals = MutableStateFlow<List<ReferralRecord>>(emptyList())
    val userReferrals: StateFlow<List<ReferralRecord>> = _userReferrals.asStateFlow()

    private val _jobViewsInSession = MutableStateFlow(0)

    private val _shouldShowServicesPopup = MutableStateFlow(false)
    val shouldShowServicesPopup: StateFlow<Boolean> = _shouldShowServicesPopup.asStateFlow()

    private val _hasCompletedOnboarding = MutableStateFlow(prefs.hasCompletedOnboarding)
    val hasCompletedOnboarding: StateFlow<Boolean> = _hasCompletedOnboarding.asStateFlow()

    // Network & Offline Caching State
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val _offlineBannerState = MutableStateFlow(OfflineBannerState.HIDDEN)
    val offlineBannerState: StateFlow<OfflineBannerState> = _offlineBannerState.asStateFlow()
    private var hasShownOfflineBannerInSession = false

    private val _lastCacheFormattedTime = MutableStateFlow(
        formatRelativeTime(if (prefs.cachedJobsTimestamp > 0) prefs.cachedJobsTimestamp else System.currentTimeMillis())
    )
    val lastCacheFormattedTime: StateFlow<String> = _lastCacheFormattedTime.asStateFlow()

    // Flag indicating Supabase status
    val isRealSupabaseConnected: Boolean = client.isRealConfigActive

    // Job states
    private val _jobs = MutableStateFlow<List<MockJob>>(
        listOf(
            MockJob(
                id = "1",
                title = "Software Engineer",
                organization = "LS Services IT Division",
                location = "Kampala",
                jobType = "Full-time",
                category = "Engineering & IT",
                salary = "UGX 3.5M - 4.5M",
                deadline = "2026-07-23",
                purpose = "To lead the development of enterprise web applications and maintain API integrations for local businesses.",
                requirements = "• 3+ years experience with Kotlin or Java\n• Proficiency with Spring Boot or Node.js\n• Experience with relational databases like PostgreSQL\n• Good team player with communication skills",
                otherDetails = "Office located in Nakasero, Kampala. Free lunch and medical insurance included.",
                opensExternally = false,
                officialLink = "https://lsrecruitingservices.com/jobs/software-engineer",
                applicationMethod = "auto_apply",
                viewsCount = 142,
                createdAt = "2026-07-21T09:00:00Z"
            ),
            MockJob(
                id = "2",
                title = "Clinical Research Nurse",
                organization = "Mbarara Regional Hospital",
                location = "Mbarara",
                jobType = "Full-time",
                category = "Healthcare",
                salary = "UGX 2.2M - 3.0M",
                deadline = "2026-07-24",
                purpose = "To coordinate clinical trials, gather participant data, and ensure compliance with medical protocols.",
                requirements = "• Registered Nurse with valid Uganda Nurses and Midwives Council license\n• 2+ years of clinical research experience\n• High attention to detail and record keeping\n• Knowledge of GCP (Good Clinical Practice) guidelines",
                otherDetails = "This is a 2-year renewable contract position based in Mbarara city.",
                opensExternally = true,
                officialLink = "https://mbararahospital.go.ug/careers/nurse-researcher",
                applicationMethod = "requires_personal_account",
                viewsCount = 89,
                createdAt = "2026-07-21T08:00:00Z"
            ),
            MockJob(
                id = "3",
                title = "Regional Marketing Officer",
                organization = "Nile Breweries",
                location = "Jinja",
                jobType = "Contract",
                category = "Sales & Marketing",
                salary = "UGX 1.8M - 2.5M",
                deadline = "2026-07-31",
                purpose = "To spearhead promotional campaigns, direct local distribution channels, and scale sales volume in the Eastern region.",
                requirements = "• Bachelor's Degree in Marketing, Business Administration or related\n• Proven track record of field sales success in Eastern Uganda\n• Ability to speak Lusoga and Luganda fluently\n• Valid driving permit is mandatory",
                otherDetails = "Field vehicle, fuel allowance, and generous commissions are provided.",
                opensExternally = false,
                officialLink = "https://nilebreweries.com/jobs/marketing-officer",
                applicationMethod = "auto_apply",
                viewsCount = 215,
                createdAt = "2026-07-20T14:30:00Z"
            ),
            MockJob(
                id = "4",
                title = "Assistant IT Lecturer",
                organization = "Makerere University",
                location = "Kampala",
                jobType = "Part-time",
                category = "Education",
                salary = "UGX 1.5M - 2.0M",
                deadline = "2026-08-15",
                purpose = "To conduct undergraduate labs, grade assignments, and tutor students in database design and mobile programming.",
                requirements = "• Master's Degree in Computer Science or Information Technology\n• Previous teaching assistant or mentoring experience\n• Deep expertise in SQL and Android SDK\n• Dedicated and passionate about teaching",
                otherDetails = "Teaching hours are flexible, mainly evening and weekend classes.",
                opensExternally = true,
                officialLink = "https://mak.ac.ug/jobs/assistant-lecturer-it",
                applicationMethod = "requires_personal_account",
                viewsCount = 310,
                createdAt = "2026-07-19T10:00:00Z"
            ),
            MockJob(
                id = "5",
                title = "Senior Accountant",
                organization = "Stanbic Bank Uganda",
                location = "Entebbe",
                jobType = "Full-time",
                category = "Finance",
                salary = "UGX 4.0M - 5.5M",
                deadline = "2026-08-01",
                purpose = "To oversee financial reporting, prepare tax computations, and coordinate internal and external audit operations.",
                requirements = "• CPA Uganda or ACCA fully qualified\n• 5+ years of banking or corporate accounting experience\n• Advanced knowledge of IFRS standards and Excel auditing\n• Integrity-driven and analytical",
                otherDetails = "Based at our Entebbe Main Branch. Comprehensive banking benefits package included.",
                opensExternally = false,
                officialLink = "https://stanbicbank.co.ug/careers/senior-accountant",
                applicationMethod = "auto_apply",
                viewsCount = 184,
                createdAt = "2026-07-18T11:00:00Z"
            ),
            MockJob(
                id = "6",
                title = "Mobile App Developer (Kotlin)",
                organization = "LS Services Tech",
                location = "Remote",
                jobType = "Remote",
                category = "Engineering & IT",
                salary = "UGX 3.0M - 4.0M",
                deadline = "2026-07-29",
                purpose = "To refine and build native Android application features using Jetpack Compose, Coroutines, and MVVM patterns.",
                requirements = "• Strong expertise in native Android development with Kotlin\n• Deep understanding of Jetpack Compose and modern architecture components\n• Experience consuming RESTful APIs and Supabase integrations\n• Independent, self-directed working style",
                otherDetails = "100% remote job with monthly internet stipends and learning allowances.",
                opensExternally = false,
                officialLink = "https://lsrecruitingservices.com/jobs/kotlin-developer",
                applicationMethod = "auto_apply",
                viewsCount = 420,
                createdAt = "2026-07-17T16:00:00Z"
            ),
            MockJob(
                id = "7",
                title = "Graduate Finance Intern",
                organization = "Centenary Bank",
                location = "Kampala",
                jobType = "Internship",
                category = "Finance",
                salary = "UGX 600K - 800K",
                deadline = "2026-07-22",
                purpose = "To assist the treasury team with daily reconciliations, data entry, and report drafting.",
                requirements = "• Recent graduate with a first-class or second-class upper degree in Finance or Economics\n• Basic knowledge of accounting principles\n• Eager to learn and highly motivated\n• Good team player",
                otherDetails = "This is a 6-month internship with potential for full-time conversion based on performance.",
                opensExternally = false,
                officialLink = "https://centenarybank.co.ug/jobs/finance-intern",
                applicationMethod = "auto_apply",
                viewsCount = 95,
                createdAt = "2026-07-15T09:00:00Z"
            ),
            MockJob(
                id = "8",
                title = "Agricultural Officer (Expired Demo)",
                organization = "Kasese District LG",
                location = "Remote",
                jobType = "Full-time",
                category = "Healthcare",
                salary = "UGX 1.2M - 1.5M",
                deadline = "2026-07-19",
                purpose = "To coordinate farming programs and advise farmers on modern crop management.",
                requirements = "• Degree in Agriculture or related field\n• Familiarity with local farming challenges\n• Willing to work in rural areas",
                otherDetails = "Government pension scheme included.",
                opensExternally = false,
                officialLink = "https://kasese.go.ug/jobs/agricultural-officer",
                applicationMethod = "auto_apply",
                viewsCount = 38,
                createdAt = "2026-07-10T12:00:00Z",
                status = "active"
            )
        ).sortedByDescending { it.createdAt }
    )
    val jobs: StateFlow<List<MockJob>> = _jobs.asStateFlow()

    // Bookmarks state
    private val _bookmarks = MutableStateFlow<Set<String>>(prefs.guestBookmarks)
    val bookmarks: StateFlow<Set<String>> = _bookmarks.asStateFlow()

    // Reported jobs
    private val _reportedJobs = MutableStateFlow<Set<String>>(prefs.reportedJobs)
    val reportedJobs: StateFlow<Set<String>> = _reportedJobs.asStateFlow()

    // Applied jobs
    private val _appliedJobs = MutableStateFlow<Set<String>>(prefs.appliedJobs)
    val appliedJobs: StateFlow<Set<String>> = _appliedJobs.asStateFlow()

    // Admin classification dynamic options
    private val _locations = MutableStateFlow<List<String>>(
        listOf("Kampala", "Mbarara", "Jinja", "Entebbe", "Gulu", "Fort Portal", "Kasese", "Arua", "Remote")
    )
    val locations: StateFlow<List<String>> = _locations.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(
        listOf("Engineering & IT", "Healthcare", "Sales & Marketing", "Education", "Finance", "Administration", "Human Resources", "Agriculture")
    )
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _jobTypes = MutableStateFlow<List<String>>(
        listOf("Full-time", "Part-time", "Contract", "Remote", "Internship", "Volunteer")
    )
    val jobTypes: StateFlow<List<String>> = _jobTypes.asStateFlow()

    // Admin Applications Queue across all users
    private val _allApplications = MutableStateFlow<List<UserApplication>>(
        listOf(
            UserApplication(
                id = "app_101",
                userId = "user_001",
                jobId = "1",
                generatedCvUrl = "https://mock-supabase.co/storage/v1/object/public/generated-cvs/patrick-cv.pdf",
                documentsAttached = listOf("National ID", "Academic Transcript"),
                status = "pending",
                appliedAt = "2026-07-21T11:30:00Z",
                jobTitle = "Software Engineer",
                jobOrganization = "LS Services IT Division",
                candidateName = "Patrick Roman",
                candidateEmail = "patrickromanug@gmail.com",
                candidatePhone = "+256 771 234 567",
                adminNotes = null
            ),
            UserApplication(
                id = "app_102",
                userId = "user_002",
                jobId = "2",
                generatedCvUrl = "https://mock-supabase.co/storage/v1/object/public/generated-cvs/sarah-cv.pdf",
                documentsAttached = listOf("Nursing License", "Passport Photo"),
                status = "pending",
                appliedAt = "2026-07-21T10:15:00Z",
                jobTitle = "Clinical Research Nurse",
                jobOrganization = "Mbarara Regional Hospital",
                candidateName = "Sarah Akello",
                candidateEmail = "sarah.akello@gmail.com",
                candidatePhone = "+256 782 990 112",
                adminNotes = null
            ),
            UserApplication(
                id = "app_103",
                userId = "user_003",
                jobId = "3",
                generatedCvUrl = "https://mock-supabase.co/storage/v1/object/public/generated-cvs/david-cv.pdf",
                documentsAttached = listOf("Driving Permit", "Recommendation Letter"),
                status = "Forwarded to Employer",
                appliedAt = "2026-07-20T16:00:00Z",
                jobTitle = "Regional Marketing Officer",
                jobOrganization = "Nile Breweries",
                candidateName = "David Mukasa",
                candidateEmail = "david.m@gmail.com",
                candidatePhone = "+256 701 443 221",
                adminNotes = "Screened and forwarded to HR Manager on July 20th."
            ),
            UserApplication(
                id = "app_104",
                userId = "user_004",
                jobId = "5",
                generatedCvUrl = "https://mock-supabase.co/storage/v1/object/public/generated-cvs/joan-cv.pdf",
                documentsAttached = listOf("CPA Certificate", "National ID"),
                status = "Accepted",
                appliedAt = "2026-07-19T14:20:00Z",
                jobTitle = "Senior Accountant",
                jobOrganization = "Stanbic Bank Uganda",
                candidateName = "Joan Katusiime",
                candidateEmail = "joan.kat@gmail.com",
                candidatePhone = "+256 755 889 001",
                adminNotes = "Candidate selected for 2nd round interviews."
            )
        )
    )
    val allApplications: StateFlow<List<UserApplication>> = _allApplications.asStateFlow()

    // Admin Reported Jobs Queue
    private val _reportedJobItems = MutableStateFlow<List<ReportedJobItem>>(
        listOf(
            ReportedJobItem(
                id = "rep_01",
                jobId = "8",
                jobTitle = "Agricultural Officer (Expired Demo)",
                organization = "Kasese District LG",
                reporterEmail = "applicant256@gmail.com",
                reason = "Application deadline has passed and portal link returns 404.",
                reportedAt = "2026-07-21T08:30:00Z"
            ),
            ReportedJobItem(
                id = "rep_02",
                jobId = "3",
                jobTitle = "Regional Marketing Officer",
                organization = "Nile Breweries",
                reporterEmail = "alex.k@gmail.com",
                reason = "Requirements mention Lusoga fluency, please clarify if Luganda is mandatory.",
                reportedAt = "2026-07-20T19:00:00Z"
            )
        )
    )
    val reportedJobItems: StateFlow<List<ReportedJobItem>> = _reportedJobItems.asStateFlow()

    private val _companyAds = MutableStateFlow<List<com.example.data.supabase.CompanyAd>>(
        listOf(
            com.example.data.supabase.CompanyAd(
                id = "ad_01",
                companyName = "LS Services & Corporate Placement",
                headline = "Empowering Ugandan Professionals Nationwide",
                description = "Get verified CV drafting, direct employer referrals, and career growth assistance with LS Services Uganda.",
                photoUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=800&q=80",
                websiteUrl = "https://lsservices.ug",
                contactPhone = "+256 771 234 567",
                category = "Career Services",
                isActive = true
            ),
            com.example.data.supabase.CompanyAd(
                id = "ad_02",
                companyName = "MTN Uganda Tech Careers",
                headline = "Innovate & Build the Future of Telecoms",
                description = "Join MTN Uganda's digital innovation team. Explore tech, engineering, and digital marketing placements.",
                photoUrl = "https://images.unsplash.com/photo-1573164713988-8665fc963095?w=800&q=80",
                websiteUrl = "https://mtn.co.ug/careers",
                contactPhone = "+256 772 000 000",
                category = "Telecom & Tech",
                isActive = true
            )
        )
    )
    val companyAds: StateFlow<List<com.example.data.supabase.CompanyAd>> = _companyAds.asStateFlow()

    fun addLocation(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_locations.value.contains(trimmed)) {
            _locations.value = _locations.value + trimmed
        }
    }

    fun addCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_categories.value.contains(trimmed)) {
            _categories.value = _categories.value + trimmed
        }
    }

    fun addJobType(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotBlank() && !_jobTypes.value.contains(trimmed)) {
            _jobTypes.value = _jobTypes.value + trimmed
        }
    }

    fun postJob(job: MockJob, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            saveCustomUploadedJob(job)
            val merged = mergeUploadedAndFilterDeleted(_jobs.value)
            _jobs.value = merged
            saveJobsToCache(merged)
            checkAndDispatchPendingNotifications(merged)
            com.example.util.JobAlertWorker.checkNowWhenOnline(getApplication())

            if (isRealSupabaseConnected) {
                val res = client.insertJob(job)
                if (res is SupabaseClient.ApiResult.Success) {
                    loadJobsFromSupabase()
                } else if (res is SupabaseClient.ApiResult.Error) {
                    Log.w(TAG, "Post job Supabase insert returned: ${res.message}. Retained locally.")
                }
            }
            _successMessage.value = "Job '${job.title}' posted successfully!"
            _isLoading.value = false
            onComplete(true)
        }
    }

    fun updateJob(updatedJob: MockJob) {
        saveCustomUploadedJob(updatedJob)
        val merged = mergeUploadedAndFilterDeleted(_jobs.value)
        _jobs.value = merged
        saveJobsToCache(merged)
        if (isRealSupabaseConnected) {
            viewModelScope.launch(Dispatchers.IO) {
                client.insertJob(updatedJob)
                loadJobsFromSupabase()
            }
        }
        _successMessage.value = "Job '${updatedJob.title}' updated."
    }

    fun updateApplicationStatus(appId: String, newStatus: String, notes: String? = null) {
        val list = _allApplications.value.toMutableList()
        val idx = list.indexOfFirst { it.id == appId }
        if (idx != -1) {
            val old = list[idx]
            list[idx] = old.copy(
                status = newStatus,
                adminNotes = notes ?: old.adminNotes
            )
            _allApplications.value = list
            _successMessage.value = "Application status updated to '$newStatus'."
        }
    }

    fun dismissReportedJob(reportId: String) {
        val list = _reportedJobItems.value.toMutableList()
        list.removeAll { it.id == reportId }
        _reportedJobItems.value = list
        _successMessage.value = "Report dismissed."
    }

    fun deactivateJob(jobId: String) {
        // Deactivate job status
        val jobList = _jobs.value.toMutableList()
        val idx = jobList.indexOfFirst { it.id == jobId }
        if (idx != -1) {
            val old = jobList[idx]
            jobList[idx] = old.copy(status = "archived")
            _jobs.value = jobList
        }
        // Also clear associated reports
        val reportList = _reportedJobItems.value.toMutableList()
        reportList.removeAll { it.jobId == jobId }
        _reportedJobItems.value = reportList
        _successMessage.value = "Job listing deactivated and removed from active search."
    }

    fun deleteJob(jobId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val jobTitle = _jobs.value.find { it.id == jobId }?.title ?: "Vacant Role"
            deleteCustomUploadedJob(jobId)
            val merged = mergeUploadedAndFilterDeleted(_jobs.value)
            _jobs.value = merged

            if (isRealSupabaseConnected) {
                client.deleteJob(jobId)
                loadJobsFromSupabase()
            }
            saveJobsToCache(merged)

            // Clear associated reports
            val reportList = _reportedJobItems.value.toMutableList()
            reportList.removeAll { it.jobId == jobId }
            _reportedJobItems.value = reportList

            _successMessage.value = "Job listing '$jobTitle' deleted permanently."
            _isLoading.value = false
        }
    }

    fun addCompanyAd(ad: com.example.data.supabase.CompanyAd) {
        val current = _companyAds.value.toMutableList()
        current.add(0, ad)
        _companyAds.value = current
        _successMessage.value = "Company advertisement published successfully."
    }

    fun updateCompanyAd(ad: com.example.data.supabase.CompanyAd) {
        val current = _companyAds.value.toMutableList()
        val idx = current.indexOfFirst { it.id == ad.id }
        if (idx != -1) {
            current[idx] = ad
            _companyAds.value = current
            _successMessage.value = "Company advertisement updated."
        }
    }

    fun deleteCompanyAd(adId: String) {
        val current = _companyAds.value.toMutableList()
        current.removeAll { it.id == adId }
        _companyAds.value = current
        _successMessage.value = "Company ad removed."
    }

    fun toggleCompanyAdStatus(adId: String) {
        val current = _companyAds.value.toMutableList()
        val idx = current.indexOfFirst { it.id == adId }
        if (idx != -1) {
            val old = current[idx]
            current[idx] = old.copy(isActive = !old.isActive)
            _companyAds.value = current
            _successMessage.value = if (!old.isActive) "Company ad activated." else "Company ad paused."
        }
    }

    private fun saveCustomUploadedJob(job: MockJob) {
        val currentJson = prefs.customUploadedJobsJson
        val currentList = if (!currentJson.isNullOrBlank()) {
            try { parseMockJobList(currentJson).toMutableList() } catch (_: Exception) { mutableListOf() }
        } else {
            mutableListOf()
        }
        val index = currentList.indexOfFirst { it.id == job.id }
        if (index >= 0) {
            currentList[index] = job
        } else {
            currentList.add(0, job)
        }
        prefs.customUploadedJobsJson = currentList.toJsonString()

        val deleted = prefs.deletedJobIds.toMutableSet()
        if (deleted.remove(job.id)) {
            prefs.deletedJobIds = deleted
        }
    }

    private fun deleteCustomUploadedJob(jobId: String) {
        val deleted = prefs.deletedJobIds.toMutableSet()
        deleted.add(jobId)
        prefs.deletedJobIds = deleted

        val currentJson = prefs.customUploadedJobsJson
        if (!currentJson.isNullOrBlank()) {
            try {
                val currentList = parseMockJobList(currentJson).toMutableList()
                currentList.removeAll { it.id == jobId }
                prefs.customUploadedJobsJson = currentList.toJsonString()
            } catch (_: Exception) {}
        }
    }

    private fun mergeUploadedAndFilterDeleted(sourceList: List<MockJob>): List<MockJob> {
        val deletedIds = prefs.deletedJobIds
        val filtered = sourceList.filter { it.id !in deletedIds }.toMutableList()

        val customJson = prefs.customUploadedJobsJson
        if (!customJson.isNullOrBlank()) {
            try {
                val customJobs = parseMockJobList(customJson)
                for (customJob in customJobs) {
                    if (customJob.id in deletedIds) continue
                    val existingIndex = filtered.indexOfFirst { it.id == customJob.id }
                    if (existingIndex >= 0) {
                        filtered[existingIndex] = customJob
                    } else {
                        filtered.add(0, customJob)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing custom uploaded jobs: ${e.message}")
            }
        }
        return filtered.sortedByDescending { it.createdAt }
    }

    private fun checkAndDispatchPendingNotifications(jobsList: List<MockJob>) {
        val seedJobIds = setOf("1", "2", "3", "4", "5", "6", "7", "8")
        val notifiedSet = prefs.notifiedJobIds.toMutableSet()

        if (!prefs.hasCompletedInitialJobSync) {
            seedJobIds.forEach { notifiedSet.add(it) }
            prefs.notifiedJobIds = notifiedSet
            prefs.hasCompletedInitialJobSync = true
        }

        val newJobsToNotify = mutableListOf<MockJob>()
        jobsList.forEach { job ->
            if (job.status != "inactive" && !notifiedSet.contains(job.id)) {
                newJobsToNotify.add(job)
            }
        }

        if (newJobsToNotify.isNotEmpty()) {
            val profile = _userProfile.value
            val notifyAll = profile?.notifyAllJobs ?: true
            val notifyMatching = profile?.notifyMatchingPreferences ?: false

            var dispatchedCount = 0
            newJobsToNotify.forEach { job ->
                val matchResult = evaluateJobMatch(job, profile)
                val isTargeted = (profile != null) && notifyMatching && matchResult.isMatch
                val shouldNotify = if (notifyMatching) {
                    matchResult.isMatch
                } else {
                    notifyAll
                }

                if (shouldNotify) {
                    val dispatched = com.example.util.NotificationHelper.showJobAlertNotification(
                        getApplication(),
                        jobId = job.id,
                        title = job.title,
                        organization = job.organization,
                        location = job.location,
                        isTargetedMatch = isTargeted,
                        matchedReason = if (isTargeted) matchResult.matchedReason else null
                    )
                    if (dispatched) {
                        notifiedSet.add(job.id)
                        dispatchedCount++
                    }
                } else {
                    notifiedSet.add(job.id)
                }
            }

            if (dispatchedCount >= 2) {
                com.example.util.NotificationHelper.showOfflineSummaryNotification(
                    getApplication(),
                    dispatchedCount
                )
            }

            prefs.notifiedJobIds = notifiedSet
        }
    }

    fun refreshJobsForCurrentSession() {
        viewModelScope.launch(Dispatchers.IO) {
            val merged = mergeUploadedAndFilterDeleted(_jobs.value)
            _jobs.value = merged
            saveJobsToCache(merged)
            checkAndDispatchPendingNotifications(merged)
        }
    }

    fun saveJobsToCache(jobList: List<MockJob>) {
        viewModelScope.launch(Dispatchers.IO) {
            val json = jobList.toJsonString()
            prefs.cachedJobsJson = json
            val now = System.currentTimeMillis()
            prefs.cachedJobsTimestamp = now
            _lastCacheFormattedTime.value = formatRelativeTime(now)
        }
    }

    suspend fun loadJobsFromCache(): Boolean = withContext(Dispatchers.IO) {
        val cachedJson = prefs.cachedJobsJson
        val timestamp = prefs.cachedJobsTimestamp
        if (!cachedJson.isNullOrBlank()) {
            val list = parseMockJobList(cachedJson)
            if (list.isNotEmpty()) {
                val merged = mergeUploadedAndFilterDeleted(list)
                _jobs.value = merged
                _lastCacheFormattedTime.value = formatRelativeTime(timestamp)
                checkAndDispatchPendingNotifications(merged)
                return@withContext true
            }
        }
        return@withContext false
    }

    fun saveProfileToCache(profile: UserProfile) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.cachedProfileJson = profile.toJsonString()
            prefs.cachedProfileTimestamp = System.currentTimeMillis()
        }
    }

    fun loadProfileFromCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val cached = parseUserProfile(prefs.cachedProfileJson)
            if (cached != null) {
                _userProfile.value = cached
            }
        }
    }

    fun saveApplicationsToCache(applications: List<UserApplication>) {
        viewModelScope.launch(Dispatchers.IO) {
            prefs.cachedApplicationsJson = applications.toJsonString()
            prefs.cachedApplicationsTimestamp = System.currentTimeMillis()
        }
    }

    fun loadApplicationsFromCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val cached = parseUserApplicationList(prefs.cachedApplicationsJson)
            if (cached.isNotEmpty()) {
                _userApplications.value = cached
            }
        }
    }

    init {
        Log.d(TAG, "ViewModel Init. LoggedIn: ${prefs.isLoggedIn}, Connected: $isRealSupabaseConnected")
        setupNetworkMonitoring()
        com.example.util.JobAlertWorker.schedulePeriodic(getApplication())
        com.example.util.JobAlertWorker.checkNowWhenOnline(getApplication())
        viewModelScope.launch(Dispatchers.IO) {
            val hasCache = loadJobsFromCache()
            if (!hasCache) {
                val merged = mergeUploadedAndFilterDeleted(_jobs.value)
                _jobs.value = merged
                saveJobsToCache(merged)
                checkAndDispatchPendingNotifications(merged)
            }
            archiveExpiredJobs(silent = true)
            loadJobsFromSupabase()
            if (prefs.isLoggedIn) {
                loadUserProfileAndSubscription()
            }

            // Periodic auto-sync loop to check every 10 seconds for newly uploaded or deleted jobs
            while (coroutineContext[kotlinx.coroutines.Job]?.isActive != false) {
                kotlinx.coroutines.delay(10000)
                if (isRealSupabaseConnected) {
                    loadJobsFromSupabase()
                } else {
                    val currentJobs = _jobs.value
                    val merged = mergeUploadedAndFilterDeleted(currentJobs)
                    if (merged != currentJobs) {
                        _jobs.value = merged
                        saveJobsToCache(merged)
                    }
                    checkAndDispatchPendingNotifications(merged)
                }
            }
        }
    }

    fun loadJobsFromSupabase(showToastOnComplete: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                if (isRealSupabaseConnected) {
                    when (val result = client.fetchJobs()) {
                        is SupabaseClient.ApiResult.Success -> {
                            _isOnline.value = true
                            prefs.hasCompletedInitialJobSync = true
                            val remoteJobs = result.data
                            val mergedList = mergeUploadedAndFilterDeleted(remoteJobs)
                            _jobs.value = mergedList
                            saveJobsToCache(mergedList)
                            checkAndDispatchPendingNotifications(mergedList)

                            if (showToastOnComplete) {
                                withContext(Dispatchers.Main) {
                                    _successMessage.value = "🔄 Job listings refreshed! (${mergedList.size} vacancies loaded)"
                                }
                            }
                        }
                        is SupabaseClient.ApiResult.Error -> {
                            Log.e(TAG, "Error fetching remote jobs: ${result.message}")
                            if (showToastOnComplete) {
                                withContext(Dispatchers.Main) {
                                    _successMessage.value = "🔄 Offline cache active (${_jobs.value.size} vacancies loaded)"
                                }
                            }
                        }
                    }
                } else {
                    // Local / Demo mode refresh
                    loadJobsFromCache()
                    if (showToastOnComplete) {
                        withContext(Dispatchers.Main) {
                            _successMessage.value = "🔄 Job listings refreshed! (${_jobs.value.size} vacancies loaded)"
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during refresh: ${e.message}")
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    data class JobMatchResult(
        val isMatch: Boolean,
        val matchedReason: String?
    )

    fun evaluateJobMatch(job: MockJob, profile: UserProfile?): JobMatchResult {
        return evaluateJobMatchStatic(job, profile)
    }

    private fun showOfflineBannerBriefly() {
        if (!hasShownOfflineBannerInSession) {
            hasShownOfflineBannerInSession = true
            _offlineBannerState.value = OfflineBannerState.OFFLINE
            viewModelScope.launch(Dispatchers.Main) {
                kotlinx.coroutines.delay(4000)
                if (_offlineBannerState.value == OfflineBannerState.OFFLINE) {
                    _offlineBannerState.value = OfflineBannerState.HIDDEN
                }
            }
        }
    }

    private fun setupNetworkMonitoring() {
        val cm = getApplication<Application>().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm != null) {
            val activeNet = cm.activeNetwork
            val caps = cm.getNetworkCapabilities(activeNet)
            val initialConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
            _isOnline.value = initialConnected
            if (!initialConnected) {
                showOfflineBannerBriefly()
                viewModelScope.launch(Dispatchers.IO) {
                    loadJobsFromCache()
                    loadProfileFromCache()
                    loadApplicationsFromCache()
                }
            }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            try {
                cm.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        val wasOffline = !_isOnline.value
                        _isOnline.value = true
                        if (wasOffline) {
                            _offlineBannerState.value = OfflineBannerState.BACK_ONLINE
                            syncWithSupabaseOnReconnect()
                            viewModelScope.launch(Dispatchers.IO) {
                                kotlinx.coroutines.delay(2500)
                                if (_offlineBannerState.value == OfflineBannerState.BACK_ONLINE) {
                                    _offlineBannerState.value = OfflineBannerState.HIDDEN
                                }
                            }
                        }
                    }

                    override fun onLost(network: Network) {
                        _isOnline.value = false
                        showOfflineBannerBriefly()
                        viewModelScope.launch(Dispatchers.IO) {
                            loadJobsFromCache()
                            loadProfileFromCache()
                            loadApplicationsFromCache()
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e(TAG, "Failed to register network callback", e)
            }
        }
    }

    fun syncWithSupabaseOnReconnect() {
        viewModelScope.launch(Dispatchers.IO) {
            com.example.util.JobAlertWorker.checkNowWhenOnline(getApplication())
            if (isRealSupabaseConnected) {
                loadJobsFromSupabase()
            } else {
                loadJobsFromCache()
                val merged = mergeUploadedAndFilterDeleted(_jobs.value)
                _jobs.value = merged
                saveJobsToCache(merged)
                checkAndDispatchPendingNotifications(merged)
            }
            if (prefs.isLoggedIn) {
                loadUserProfileAndSubscription()
            }
        }
    }

    fun setTheme(theme: String) {
        prefs.themePreference = theme
        _themeMode.value = theme
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSuccess() {
        _successMessage.value = null
    }

    /**
     * Load Profile and Subscription for logged-in user
     */
    private fun loadUserProfileAndSubscription() {
        val uid = prefs.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _isLoading.value = true
            
            // Try fetching profile from Supabase first
            when (val profileRes = client.fetchProfile(uid)) {
                is SupabaseClient.ApiResult.Success -> {
                    var profile = profileRes.data
                    val cached = com.example.util.CacheUtils.parseUserProfile(prefs.cachedProfileJson)
                    var needsRemoteUpdate = false
                    if (cached != null) {
                        profile = profile.copy(
                            preferredCategories = if (profile.preferredCategories.isEmpty()) { needsRemoteUpdate = true; cached.preferredCategories } else profile.preferredCategories,
                            preferredLocations = if (profile.preferredLocations.isEmpty()) { needsRemoteUpdate = true; cached.preferredLocations } else profile.preferredLocations,
                            skills = if (profile.skills.isEmpty()) { needsRemoteUpdate = true; cached.skills } else profile.skills,
                            education = if (profile.education == "[]" || profile.education.isBlank()) { needsRemoteUpdate = true; cached.education } else profile.education,
                            experience = if (profile.experience == "[]" || profile.experience.isBlank()) { needsRemoteUpdate = true; cached.experience } else profile.experience,
                            themePreference = if (profile.themePreference == "system" && cached.themePreference != "system") { needsRemoteUpdate = true; cached.themePreference } else profile.themePreference,
                            notifyAllJobs = cached.notifyAllJobs,
                            notifyMatchingPreferences = cached.notifyMatchingPreferences
                        )
                    }
                    if (profile.referralCode.isNullOrBlank()) {
                        val genCode = client.ensureReferralCode(profile.id, profile.fullName)
                        profile = profile.copy(referralCode = genCode)
                        needsRemoteUpdate = true
                    }
                    if (needsRemoteUpdate) {
                        client.updateProfile(profile)
                    }
                    _userProfile.value = profile
                    saveProfileToCache(profile)
                    prefs.userRole = profile.role
                    prefs.userName = profile.fullName
                }
                is SupabaseClient.ApiResult.Error -> {
                    Log.e(TAG, "Profile Fetch Error: ${profileRes.message}")
                    val fallbackCode = client.ensureReferralCode(uid, prefs.userName ?: "User")
                    val cached = com.example.util.CacheUtils.parseUserProfile(prefs.cachedProfileJson)
                    val fallbackProfile = if (cached != null) {
                        cached.copy(
                            id = uid,
                            fullName = prefs.userName ?: cached.fullName,
                            role = prefs.userRole,
                            referralCode = cached.referralCode ?: fallbackCode
                        )
                    } else {
                        UserProfile(
                            id = uid,
                            fullName = prefs.userName ?: "User",
                            phone = null,
                            role = prefs.userRole,
                            referralCode = fallbackCode
                        )
                    }
                    _userProfile.value = fallbackProfile
                    saveProfileToCache(fallbackProfile)
                }
            }

            // 2. Fetch Subscription
            when (val subRes = client.fetchSubscription(uid)) {
                is SupabaseClient.ApiResult.Success -> {
                    _userSubscription.value = subRes.data
                }
                is SupabaseClient.ApiResult.Error -> {
                    Log.e(TAG, "Subscription Fetch Error: ${subRes.message}")
                    // Create fallback local subscription
                    _userSubscription.value = UserSubscription(
                        id = "sub_local",
                        userId = uid,
                        planTier = "trial",
                        status = "trial",
                        trialEndsAt = "2026-08-04T12:00:00Z",
                        renewalDate = "2026-08-04T12:00:00Z"
                    )
                }
            }
            
            loadUserDocuments()
            loadUserApplications()
            loadUserReferrals()
            _isLoading.value = false
            registerPushToken()
            loadJobsFromSupabase()
        }
    }

    /**
     * Fetch referral status records for active user
     */
    fun loadUserReferrals() {
        val uid = prefs.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = client.fetchReferrals(uid)) {
                is SupabaseClient.ApiResult.Success -> {
                    _userReferrals.value = res.data
                }
                is SupabaseClient.ApiResult.Error -> {
                    Log.e(TAG, "Fetch referrals error: ${res.message}")
                }
            }
        }
    }

    /**
     * Handle incoming referral code from deep link or user input
     */
    fun handleReferralCodeReceived(code: String) {
        val clean = code.trim().uppercase()
        if (clean.isNotBlank()) {
            prefs.pendingReferralCode = clean
            _pendingReferralCode.value = clean
            _successMessage.value = "Referral code $clean applied! Complete signup to receive 7 days of Premium free."
        }
    }

    /**
     * Increment job view count and check if Other Services popup should trigger
     */
    fun onJobDetailViewed() {
        _jobViewsInSession.value += 1
        val profile = _userProfile.value
        val isHiddenPermanently = profile?.hideServicesPopup == true
        val isDismissedTemporarily = prefs.servicesPopupDismissedUntil > System.currentTimeMillis()

        if (_jobViewsInSession.value >= 3 && !isHiddenPermanently && !isDismissedTemporarily) {
            _shouldShowServicesPopup.value = true
        }
    }

    /**
     * Dismiss Other Services popup for a few days
     */
    fun dismissServicesPopupLater() {
        _shouldShowServicesPopup.value = false
        prefs.servicesPopupDismissedUntil = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L // 3 days
    }

    /**
     * Dismiss Other Services popup permanently by updating profile hide_services_popup
     */
    fun dismissServicesPopupPermanently() {
        _shouldShowServicesPopup.value = false
        val uid = prefs.userId ?: return
        viewModelScope.launch {
            val current = _userProfile.value
            if (current != null) {
                _userProfile.value = current.copy(hideServicesPopup = true)
            }
            client.updateHideServicesPopup(uid, true)
        }
    }

    /**
     * Mark app onboarding as completed
     */
    fun completeOnboarding() {
        prefs.hasCompletedOnboarding = true
        _hasCompletedOnboarding.value = true
    }

    /**
     * Check profile completeness and complete referral reward if user was referred
     */
    fun completeReferralRewardIfEligible(userId: String) {
        viewModelScope.launch {
            when (val res = client.completeReferralReward(userId)) {
                is SupabaseClient.ApiResult.Success -> {
                    if (res.data) {
                        _successMessage.value = "🎉 Referral Reward Activated! 7 extra days of Premium have been added to your account."
                        loadUserProfileAndSubscription()
                    }
                }
                is SupabaseClient.ApiResult.Error -> {
                    Log.e(TAG, "Complete referral reward error: ${res.message}")
                }
            }
        }
    }

    /**
     * Load user documents from vault
     */
    fun loadUserDocuments() {
        val uid = prefs.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            when (val res = client.fetchUserDocuments(uid)) {
                is SupabaseClient.ApiResult.Success -> {
                    _userDocuments.value = res.data
                }
                is SupabaseClient.ApiResult.Error -> {
                    Log.e(TAG, "Fetch user docs error: ${res.message}")
                }
            }
        }
    }

    /**
     * Load user applications
     */
    fun loadUserApplications() {
        val uid = prefs.userId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (!_isOnline.value) {
                loadApplicationsFromCache()
                return@launch
            }
            when (val res = client.fetchUserApplications(uid)) {
                is SupabaseClient.ApiResult.Success -> {
                    val sorted = res.data.sortedByDescending { it.appliedAt }
                    _userApplications.value = sorted
                    saveApplicationsToCache(sorted)
                }
                is SupabaseClient.ApiResult.Error -> {
                    Log.e(TAG, "Fetch user applications error: ${res.message}")
                    loadApplicationsFromCache()
                }
            }
        }
    }

    /**
     * Update User Profile details
     */
    fun updateUserProfile(profile: UserProfile, onComplete: (Boolean) -> Unit) {
        _userProfile.value = profile
        saveProfileToCache(profile)
        if (profile.fullName.isNotBlank()) {
            prefs.userName = profile.fullName
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            if (_isOnline.value) {
                when (val res = client.updateProfile(profile)) {
                    is SupabaseClient.ApiResult.Success -> {
                        _userProfile.value = res.data
                        saveProfileToCache(res.data)
                        if (res.data.fullName.isNotBlank()) {
                            prefs.userName = res.data.fullName
                        }
                        _successMessage.value = "Profile updated successfully."
                    }
                    is SupabaseClient.ApiResult.Error -> {
                        Log.w(TAG, "Remote profile update failed: ${res.message}. Retained locally.")
                        _successMessage.value = "Profile updated successfully."
                    }
                }
            } else {
                _successMessage.value = "Profile updated successfully."
            }
            _isLoading.value = false
            onComplete(true)
        }
    }

    /**
     * Upload a document to storage and save its metadata row
     */
    fun uploadAndSaveDocument(
        documentType: String,
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String,
        onComplete: (Boolean) -> Unit
    ) {
        if (!_isOnline.value) {
            _errorMessage.value = "You're offline. Connect to the internet to upload documents to your vault."
            onComplete(false)
            return
        }
        val uid = prefs.userId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val path = "$uid/${UUID.randomUUID()}_$fileName"
            when (val uploadRes = client.uploadFile("user-documents", path, fileBytes, mimeType)) {
                is SupabaseClient.ApiResult.Success -> {
                    val fileUrl = uploadRes.data
                    when (val insertRes = client.insertUserDocument(uid, documentType, fileUrl)) {
                        is SupabaseClient.ApiResult.Success -> {
                            loadUserDocuments()
                            _successMessage.value = "Document successfully added to your vault."
                            onComplete(true)
                        }
                        is SupabaseClient.ApiResult.Error -> {
                            _errorMessage.value = "Failed to register document metadata: ${insertRes.message}"
                            onComplete(false)
                        }
                    }
                }
                is SupabaseClient.ApiResult.Error -> {
                    _errorMessage.value = "File upload failed: ${uploadRes.message}"
                    onComplete(false)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Delete a document from the vault
     */
    fun deleteUserDocument(docId: String, fileUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val res = client.deleteUserDocument(docId, fileUrl)) {
                is SupabaseClient.ApiResult.Success -> {
                    _userDocuments.value = _userDocuments.value.filter { it.id != docId }
                    _successMessage.value = "Document deleted successfully."
                }
                is SupabaseClient.ApiResult.Error -> {
                    _errorMessage.value = res.message
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Change Password
     */
    fun changePassword(newPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val res = client.changePassword(newPassword)) {
                is SupabaseClient.ApiResult.Success -> {
                    _successMessage.value = res.data
                    onComplete(true)
                }
                is SupabaseClient.ApiResult.Error -> {
                    _errorMessage.value = res.message
                    onComplete(false)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Permanent Account Deletion
     */
    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        val uid = prefs.userId ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            when (val res = client.deleteAccount(uid)) {
                is SupabaseClient.ApiResult.Success -> {
                    logout()
                    _successMessage.value = "Your account and all associated documents/applications have been deleted permanently."
                    onComplete(true)
                }
                is SupabaseClient.ApiResult.Error -> {
                    _errorMessage.value = res.message
                    onComplete(false)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Email / Password Login Action
     */
    fun login(email: String, password: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = client.login(email, password)) {
                is SupabaseClient.AuthResult.Success -> {
                    _isLoggedIn.value = true
                    prefs.userId = result.profile.id
                    _userProfile.value = result.profile
                    loadUserProfileAndSubscription()
                    _successMessage.value = "Successfully logged in as ${result.profile.fullName}!"
                    if (!hasShownCvOnboarding) {
                        markCvOnboardingShown()
                        profileSubScreen = "completion_flow"
                        selectTab("profile")
                    }
                    refreshJobsForCurrentSession()
                    onComplete(true)
                }
                is SupabaseClient.AuthResult.Error -> {
                    _errorMessage.value = result.message
                    onComplete(false)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Google Sign In Action
     */
    fun loginWithGoogle(
        email: String = "patrickromanug@gmail.com",
        name: String = "Patrick Roman",
        onComplete: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = client.loginWithGoogle(email, name)) {
                is SupabaseClient.AuthResult.Success -> {
                    _isLoggedIn.value = true
                    prefs.userId = result.profile.id
                    _userProfile.value = result.profile
                    loadUserProfileAndSubscription()
                    _successMessage.value = "Successfully signed in as ${result.user.email}!"
                    if (!hasShownCvOnboarding) {
                        markCvOnboardingShown()
                        profileSubScreen = "completion_flow"
                        selectTab("profile")
                    }
                    refreshJobsForCurrentSession()
                    onComplete(true)
                }
                is SupabaseClient.AuthResult.Error -> {
                    _errorMessage.value = result.message
                    onComplete(false)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Email / Password Signup Action
     */
    fun signUp(email: String, password: String, fullName: String, phone: String, referralCode: String? = null, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = client.signUp(email, password, fullName, phone)) {
                is SupabaseClient.AuthResult.Success -> {
                    _isLoggedIn.value = true
                    prefs.userId = result.profile.id
                    _userProfile.value = result.profile

                    val refCodeToUse = referralCode?.takeIf { it.isNotBlank() } ?: _pendingReferralCode.value
                    if (!refCodeToUse.isNullOrBlank()) {
                        client.recordPendingReferral(result.user.id, refCodeToUse)
                        prefs.pendingReferralCode = null
                        _pendingReferralCode.value = null
                    }

                    loadUserProfileAndSubscription()
                    _successMessage.value = "Welcome to LS Services, $fullName! Your 14-day free trial subscription is active."
                    if (!hasShownCvOnboarding) {
                        markCvOnboardingShown()
                        profileSubScreen = "completion_flow"
                        selectTab("profile")
                    }
                    onComplete(true)
                }
                is SupabaseClient.AuthResult.Error -> {
                    _errorMessage.value = result.message
                    onComplete(false)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Forgot Password Action
     */
    fun forgotPassword(email: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            when (val result = client.forgotPassword(email)) {
                is SupabaseClient.ApiResult.Success -> {
                    _successMessage.value = result.data
                    onComplete(true)
                }
                is SupabaseClient.ApiResult.Error -> {
                    _errorMessage.value = result.message
                    onComplete(false)
                }
            }
            _isLoading.value = false
        }
    }

    /**
     * Log Out Action
     */
    fun logout() {
        client.logout()
        _isLoggedIn.value = false
        _userProfile.value = null
        _userSubscription.value = null
        currentTab = "home"
        profileSubScreen = "main"
        prefs.hasShownCvOnboarding = false
        hasShownCvOnboarding = false
        _successMessage.value = "Logged out successfully."
    }

    fun toggleBookmark(jobId: String) {
        val current = _bookmarks.value.toMutableSet()
        if (current.contains(jobId)) {
            current.remove(jobId)
        } else {
            current.add(jobId)
        }
        _bookmarks.value = current
        prefs.guestBookmarks = current
        
        // TODO: For logged-in users, synchronize with the Supabase bookmarks table
    }

    fun reportJob(jobId: String, reason: String) {
        if (!_isOnline.value) {
            _errorMessage.value = "You're offline. Connect to the internet to report job listings."
            return
        }
        val current = _reportedJobs.value.toMutableSet()
        current.add(jobId)
        _reportedJobs.value = current
        prefs.reportedJobs = current
        
        // TODO: Insert a row into the Supabase reported_jobs table
    }

    fun applyJob(jobId: String) {
        val current = _appliedJobs.value.toMutableSet()
        current.add(jobId)
        _appliedJobs.value = current
        prefs.appliedJobs = current
        
        // TODO: In a later prompt, this will navigate through ApplyFlow and post to the Supabase applications table
    }

    fun getApplicationsSubmittedThisMonth(): Int {
        val now = LocalDate.now()
        val yearMonthStr = String.format("%04d-%02d", now.year, now.monthValue)
        return _userApplications.value.count { app ->
            app.appliedAt.startsWith(yearMonthStr)
        }
    }

    fun submitApplication(
        jobId: String,
        jobTitle: String,
        jobOrganization: String,
        generatedCvBytes: ByteArray,
        documentsAttached: List<String>,
        onResult: (Boolean, String) -> Unit
    ) {
        val uid = prefs.userId ?: run {
            onResult(false, "User not logged in.")
            return
        }
        
        val subscription = _userSubscription.value
        val monthlyLimit = subscription?.appliesMonthlyLimit ?: 5
        val submittedThisMonth = getApplicationsSubmittedThisMonth()
        if (submittedThisMonth >= monthlyLimit) {
            onResult(false, "LIMIT_REACHED")
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            val fileName = "cv_${System.currentTimeMillis()}.pdf"
            val cvUploadRes = client.uploadFile(
                bucketName = "generated-cvs",
                path = "$uid/$fileName",
                fileBytes = generatedCvBytes,
                mimeType = "application/pdf"
            )

            val cvUrl = when (cvUploadRes) {
                is SupabaseClient.ApiResult.Success -> cvUploadRes.data
                is SupabaseClient.ApiResult.Error -> {
                    _isLoading.value = false
                    onResult(false, "CV Upload Failed: ${cvUploadRes.message}")
                    return@launch
                }
            }

            val insertRes = client.insertApplication(
                userId = uid,
                jobId = jobId,
                generatedCvUrl = cvUrl,
                documentsAttached = documentsAttached,
                jobTitle = jobTitle,
                jobOrganization = jobOrganization
            )

            when (insertRes) {
                is SupabaseClient.ApiResult.Success -> {
                    val currentList = _userApplications.value.toMutableList()
                    currentList.add(0, insertRes.data)
                    _userApplications.value = currentList
                    
                    val applied = _appliedJobs.value.toMutableSet()
                    applied.add(jobId)
                    _appliedJobs.value = applied
                    prefs.appliedJobs = applied

                    _successMessage.value = "Application for $jobTitle submitted successfully!"
                    _isLoading.value = false
                    onResult(true, "Success")
                }
                is SupabaseClient.ApiResult.Error -> {
                    _isLoading.value = false
                    onResult(false, "Database Insert Failed: ${insertRes.message}")
                }
            }
        }
    }

    fun incrementViewsCount(jobId: String) {
        val currentJobs = _jobs.value.map { job ->
            if (job.id == jobId) {
                job.copy(viewsCount = job.viewsCount + 1)
            } else {
                job
            }
        }
        _jobs.value = currentJobs
        
        // If Supabase is active, increment in Supabase
        if (isRealSupabaseConnected) {
            viewModelScope.launch {
                // TODO: Perform Supabase RPC or UPDATE to increment views_count
            }
        }
    }

    fun getSimilarJobs(targetJob: MockJob): List<MockJob> {
        val applied = _appliedJobs.value
        return _jobs.value
            .filter { candidate ->
                candidate.id != targetJob.id && 
                (candidate.status.isBlank() || candidate.status.equals("active", ignoreCase = true) || candidate.status.equals("open", ignoreCase = true) || candidate.status.equals("published", ignoreCase = true)) && 
                !applied.contains(candidate.id)
            }
            .map { candidate ->
                var score = 0
                if (candidate.category.equals(targetJob.category, ignoreCase = true) || candidate.category.contains(targetJob.category, ignoreCase = true) || targetJob.category.contains(candidate.category, ignoreCase = true)) score += 3
                if (candidate.location.equals(targetJob.location, ignoreCase = true) || candidate.location.contains(targetJob.location, ignoreCase = true)) score += 2
                if (candidate.jobType.equals(targetJob.jobType, ignoreCase = true) || candidate.jobType.contains(targetJob.jobType, ignoreCase = true)) score += 1
                Pair(candidate, score)
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
            .take(4)
    }

    /**
     * Automatic scheduled auto-expiry of jobs.
     * Marks any active job where deadline has passed to 'archived'.
     */
    fun archiveExpiredJobs(silent: Boolean = true): Int {
        var count = 0
        val today = LocalDate.now()
        val referenceDate = if (today.isAfter(LocalDate.of(2026, 7, 1))) today else LocalDate.of(2026, 7, 21)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        
        val updatedJobs = _jobs.value.map { job ->
            try {
                val deadlineDate = LocalDate.parse(job.deadline, formatter)
                if (deadlineDate.isBefore(referenceDate) && job.status == "active") {
                    count++
                    job.copy(status = "archived")
                } else {
                    job
                }
            } catch (e: Exception) {
                job
            }
        }
        _jobs.value = updatedJobs
        if (!silent && count > 0) {
            _successMessage.value = "Automated Clean: $count expired listings archived successfully!"
        }
        return count
    }

    // ==========================================
    // NOTIFICATION SYSTEM PROPERTIES & FUNCTIONS
    // ==========================================

    fun isNotificationPermissionGranted(context: android.content.Context): Boolean {
        return com.example.util.NotificationHelper.areNotificationsEnabled(context)
    }

    fun openPhoneNotificationSettings(context: android.content.Context) {
        com.example.util.NotificationHelper.openNotificationSettings(context)
    }

    fun checkAndRequestNotificationPermission(context: android.content.Context) {
        if (isNotificationPermissionGranted(context)) {
            registerPushToken()
            _successMessage.value = "🔔 Notifications are active! Job alerts enabled."
        } else {
            showNotificationExplanationDialog = true
        }
    }

    fun onUserAcceptedNotifExplanation(
        context: android.content.Context,
        launcher: androidx.activity.result.ActivityResultLauncher<String>
    ) {
        showNotificationExplanationDialog = false
        prefs.hasShownNotifPermissionExplanation = true
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (isNotificationPermissionGranted(context)) {
                registerPushToken()
                _successMessage.value = "🔔 Notifications are active! Job alerts enabled."
            } else {
                try {
                    launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to launch notification permission request: ${e.message}")
                    openPhoneNotificationSettings(context)
                }
            }
        } else {
            registerPushToken()
            _successMessage.value = "🔔 Notifications are active! Job alerts enabled."
        }
    }

    fun onNotificationPermissionResult(context: android.content.Context, isGranted: Boolean) {
        if (isGranted) {
            registerPushToken()
            _successMessage.value = "🎉 Push notifications enabled! Job alerts active."
            viewModelScope.launch(Dispatchers.IO) {
                val merged = mergeUploadedAndFilterDeleted(_jobs.value)
                checkAndDispatchPendingNotifications(merged)
            }
        } else {
            _errorMessage.value = "⚠️ Permission denied. Opening phone notification settings..."
            openPhoneNotificationSettings(context)
        }
    }

    fun onUserDeclinedNotifExplanation() {
        showNotificationExplanationDialog = false
        prefs.hasShownNotifPermissionExplanation = true
    }

    fun registerPushToken() {
        val uid = prefs.userId ?: return
        val token = prefs.pushToken ?: return
        viewModelScope.launch {
            Log.d(TAG, "Registering push token: $token for user: $uid")
            client.upsertPushToken(uid, token)
        }
    }

    fun showToast(message: String) {
        _successMessage.value = message
    }

    fun handleNotificationTap(jobId: String) {
        viewModelScope.launch {
            val job = _jobs.value.find { it.id == jobId }
            if (job != null) {
                globalJobDetailToShow = job
                currentTab = "home"
            } else {
                Log.e(TAG, "Job with id $jobId not found in local list")
            }
        }
    }

    fun updateNotifyAllJobs(enabled: Boolean) {
        val current = _userProfile.value ?: UserProfile(
            id = prefs.userId ?: "guest",
            fullName = prefs.userName ?: "User",
            role = prefs.userRole
        )
        val updatedProfile = current.copy(
            notifyAllJobs = enabled,
            notifyMatchingPreferences = if (enabled) false else current.notifyMatchingPreferences
        )
        _userProfile.value = updatedProfile
        saveProfileToCache(updatedProfile)

        viewModelScope.launch {
            _isLoading.value = true
            when (val res = client.updateProfile(updatedProfile)) {
                is SupabaseClient.ApiResult.Success -> {
                    _userProfile.value = res.data
                    saveProfileToCache(res.data)
                    _successMessage.value = "Notification settings updated successfully."
                }
                is SupabaseClient.ApiResult.Error -> {
                    Log.w(TAG, "Remote profile update failed: ${res.message}. Retained locally.")
                    _successMessage.value = "Notification settings saved."
                }
            }
            _isLoading.value = false
        }
    }

    fun updateNotifyMatchingPreferences(enabled: Boolean) {
        val current = _userProfile.value ?: UserProfile(
            id = prefs.userId ?: "guest",
            fullName = prefs.userName ?: "User",
            role = prefs.userRole
        )
        val currentSub = _userSubscription.value
        val isFreeTier = currentSub?.planTier == "free" && currentSub.status != "trial"
        if (isFreeTier && enabled) {
            showUpgradePrompt = true
            return
        }

        val updatedProfile = current.copy(
            notifyMatchingPreferences = enabled,
            notifyAllJobs = if (enabled) false else current.notifyAllJobs
        )
        _userProfile.value = updatedProfile
        saveProfileToCache(updatedProfile)

        viewModelScope.launch {
            _isLoading.value = true
            when (val res = client.updateProfile(updatedProfile)) {
                is SupabaseClient.ApiResult.Success -> {
                    _userProfile.value = res.data
                    saveProfileToCache(res.data)
                    _successMessage.value = "Targeted notification settings updated successfully."
                }
                is SupabaseClient.ApiResult.Error -> {
                    Log.w(TAG, "Remote profile update failed: ${res.message}. Retained locally.")
                    _successMessage.value = "Targeted notification settings saved."
                }
            }
            _isLoading.value = false
        }
    }

    fun sendTestNotification() {
        val context = getApplication<android.app.Application>()
        if (!isNotificationPermissionGranted(context)) {
            checkAndRequestNotificationPermission(context)
            _errorMessage.value = "⚠️ Notification permission is required. Please enable notifications in the prompt."
            return
        }
        val profile = _userProfile.value
        val cats = profile?.preferredCategories?.firstOrNull() ?: "Engineering & IT"
        val loc = profile?.preferredLocations?.firstOrNull() ?: "Kampala"
        val dispatched = com.example.util.NotificationHelper.showJobAlertNotification(
            context,
            jobId = "test_job_${System.currentTimeMillis()}",
            title = "Test Job Alert ($cats)",
            organization = "LS Services System Test",
            location = loc,
            isTargetedMatch = true,
            matchedReason = "Category '$cats' & Location '$loc'"
        )
        if (dispatched) {
            _successMessage.value = "🔔 Test push notification dispatched to your phone status bar!"
        } else {
            _errorMessage.value = "⚠️ Unable to post notification. Please check phone Settings -> Notifications -> LS Services."
        }
    }

    fun mockUpgradeToTier(tier: String) {
        val current = _userSubscription.value ?: return
        val calculatedNotifLimit = when (tier) {
            "basic" -> 10
            "premium" -> 50
            else -> null // premium_pro / trial
        }
        val calculatedAppliesLimit = when (tier) {
            "basic" -> 3
            "premium" -> 15
            else -> null // premium_pro / trial
        }
        val calculatedCategoriesLimit = when (tier) {
            "basic" -> 2
            "premium" -> 5
            else -> null // premium_pro / trial
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val updatedSub = current.copy(
                planTier = tier,
                status = "active",
                notifDailyLimit = calculatedNotifLimit,
                appliesMonthlyLimit = calculatedAppliesLimit,
                categoriesLimit = calculatedCategoriesLimit
            )
            when (val res = client.updateSubscription(updatedSub)) {
                is SupabaseClient.ApiResult.Success -> {
                    _userSubscription.value = res.data
                    _successMessage.value = "Successfully upgraded to ${tier.replace("_", " ").uppercase()} Plan!"
                }
                is SupabaseClient.ApiResult.Error -> {
                    _userSubscription.value = updatedSub
                    _successMessage.value = "Successfully upgraded to ${tier.replace("_", " ").uppercase()} Plan locally!"
                }
            }
            _isLoading.value = false
        }
    }
}

data class MockJob(
    val id: String,
    val title: String,
    val organization: String,
    val location: String,
    val jobType: String,
    val category: String,
    val salary: String = "Negotiable",
    val deadline: String = "2026-08-15",
    val purpose: String = "To contribute to organizational growth and deliver excellent services.",
    val requirements: String = "• Strong communication skills\n• Relevant background or degree\n• Driven, integrity-focused and detail-oriented",
    val otherDetails: String = "Competitive package with medical cover and local allowances.",
    val opensExternally: Boolean = false,
    val officialLink: String = "https://lsrecruitingservices.com",
    val applicationMethod: String = "auto_apply", // "auto_apply", "requires_personal_account", "email_only"
    val requiredDocuments: List<String> = emptyList(),
    val postedBy: String? = null,
    val viewsCount: Int = 10,
    val status: String = "active", // "active", "archived"
    val createdAt: String = "2026-07-21T00:00:00Z"
)

data class ReportedJobItem(
    val id: String,
    val jobId: String,
    val jobTitle: String,
    val organization: String,
    val reporterEmail: String,
    val reason: String,
    val reportedAt: String
)
