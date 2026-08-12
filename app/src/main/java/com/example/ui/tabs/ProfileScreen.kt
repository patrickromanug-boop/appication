package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.AppViewModel
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryBlue
import com.example.data.supabase.UserProfile
import com.example.data.supabase.UserDocument
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class EducationItem(
    val school: String,
    val degree: String,
    val startYear: String,
    val endYear: String
)

data class ExperienceItem(
    val company: String,
    val role: String,
    val startYear: String,
    val endYear: String,
    val achievements: String
)

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: AppViewModel,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val userSubscription by viewModel.userSubscription.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val userDocuments by viewModel.userDocuments.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()

    // Screen navigation state inside Profile: "main", "completion_flow", "document_vault", "subscription_comparison"
    val activeSubScreen = viewModel.profileSubScreen
    var hasSkippedProfileFlow by remember { mutableStateOf(false) }

    // Dialog overlays
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showActivationFilterDialog by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(
        enabled = activeSubScreen != "main" || showChangePasswordDialog || showDeleteAccountDialog || showActivationFilterDialog
    ) {
        when {
            showActivationFilterDialog -> showActivationFilterDialog = false
            showChangePasswordDialog -> showChangePasswordDialog = false
            showDeleteAccountDialog -> showDeleteAccountDialog = false
            activeSubScreen != "main" -> viewModel.profileSubScreen = "main"
        }
    }


    // Redirect to profile flow automatically ONCE on first install/login if profile is incomplete
    LaunchedEffect(isLoggedIn, userProfile) {
        if (isLoggedIn && userProfile != null && !viewModel.hasShownCvOnboarding) {
            viewModel.markCvOnboardingShown()
            val isComplete = userProfile?.fullName?.isNotEmpty() == true && 
                             userProfile?.phone?.isNotEmpty() == true &&
                             userProfile?.education != null && userProfile?.education != "[]" &&
                             userProfile?.experience != null && userProfile?.experience != "[]" &&
                             userProfile?.skills?.isNotEmpty() == true &&
                             userProfile?.preferredCategories?.isNotEmpty() == true &&
                             userProfile?.preferredLocations?.isNotEmpty() == true
            if (!isComplete && activeSubScreen == "main") {
                viewModel.profileSubScreen = "completion_flow"
            }
        }
    }

    // Load documents if screen shifts to vault
    LaunchedEffect(activeSubScreen) {
        if (activeSubScreen == "document_vault" && isLoggedIn) {
            viewModel.loadUserDocuments()
        }
    }

    // Clear Toast messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(successMessage) {
        successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearSuccess()
        }
    }

    when (activeSubScreen) {
        "completion_flow" -> {
            ProfileCompletionFlowScreen(
                viewModel = viewModel,
                userProfile = userProfile,
                onBack = {
                    hasSkippedProfileFlow = true
                    viewModel.profileSubScreen = "main"
                }
            )
        }
        "document_vault" -> {
            DocumentVaultScreen(
                viewModel = viewModel,
                userDocuments = userDocuments,
                onBack = { viewModel.profileSubScreen = "main" }
            )
        }
        "subscription_comparison" -> {
            PlanComparisonScreen(
                viewModel = viewModel,
                onBack = { viewModel.profileSubScreen = "main" }
            )
        }
        "referral_program" -> {
            ReferralScreen(
                viewModel = viewModel,
                onBack = { viewModel.profileSubScreen = "main" }
            )
        }
        "other_services" -> {
            OtherServicesScreen(
                onBack = { viewModel.profileSubScreen = "main" }
            )
        }
        "onboarding_tour" -> {
            OnboardingScreen(
                viewModel = viewModel,
                onFinish = { viewModel.profileSubScreen = "main" }
            )
        }
        "legal_and_about" -> {
            LegalAndAboutScreen(
                onBack = { viewModel.profileSubScreen = "main" }
            )
        }
        else -> {
            // Main Settings SubScreen
            val scrollState = rememberScrollState()

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(scrollState)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "My Profile Hub",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (!isLoggedIn) {
                        // Guest Profile Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = "Guest",
                                    tint = PrimaryBlue.copy(alpha = 0.3f),
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Guest Account",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Unlock CV templates, personalized job matches, offline bookmarks, auto-apply, and Ugandan referral rewards by creating a free account.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(24.dp))
                                Button(
                                    onClick = onNavigateToLogin,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Text("Sign In or Register Now", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        // Logged In User Profile Block
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(56.dp)
                                            .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(100)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = (userProfile?.fullName ?: "U").take(1).uppercase(),
                                            color = PrimaryBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = userProfile?.fullName ?: "Anonymous Member",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (userProfile?.role == "admin") AccentOrange else PrimaryBlue,
                                                        RoundedCornerShape(4.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = userProfile?.role?.uppercase() ?: "USER",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                            if (userSubscription != null) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "${userSubscription?.planTier?.uppercase()} PLAN",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF2E7D32)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                                Spacer(modifier = Modifier.height(12.dp))

                                // Calculate Completion Rate
                                val compSteps = remember(userProfile) {
                                    val steps = mutableListOf<String>()
                                    if (userProfile?.fullName?.isNotEmpty() == true) steps.add("Name")
                                    if (userProfile?.phone?.isNotEmpty() == true) steps.add("Phone")
                                    val edList = try { JSONArray(userProfile?.education ?: "[]") } catch(e: Exception) { JSONArray() }
                                    if (edList.length() > 0) steps.add("Education")
                                    val expList = try { JSONArray(userProfile?.experience ?: "[]") } catch(e: Exception) { JSONArray() }
                                    if (expList.length() > 0) steps.add("Experience")
                                    if (userProfile?.skills?.isNotEmpty() == true) steps.add("Skills")
                                    if (userProfile?.preferredCategories?.isNotEmpty() == true) steps.add("Categories")
                                    if (userProfile?.preferredLocations?.isNotEmpty() == true) steps.add("Locations")
                                    steps
                                }
                                val completionPercent = (compSteps.size * 100) / 7
                                val isComplete = completionPercent >= 100

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "CV Profile Status: $completionPercent%",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isComplete) Color(0xFF2E7D32) else AccentOrange
                                        )
                                        Text(
                                            text = if (isComplete) "Profile 100% completed and synced!" else "Complete all 7 fields to enable Auto-Apply",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    if (isComplete) {
                                        Icon(Icons.Default.CheckCircle, "Complete", tint = Color(0xFF2E7D32))
                                    } else {
                                        Icon(Icons.Default.Warning, "Incomplete", tint = AccentOrange)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = completionPercent / 100f,
                                    color = if (isComplete) Color(0xFF2E7D32) else PrimaryBlue,
                                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                                )

                                Spacer(modifier = Modifier.height(14.dp))
                                Button(
                                    onClick = { viewModel.profileSubScreen = "completion_flow" },
                                    colors = ButtonDefaults.buttonColors(containerColor = if (isComplete) PrimaryBlue else AccentOrange),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, "Edit")
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isComplete) "Edit Structured CV Profile" else "Resume Completion Flow Now",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Subscription & Billing Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.profileSubScreen = "subscription_comparison" }
                                .testTag("subscription_billing_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(AccentOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Star, "Subscription", tint = AccentOrange)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Subscription & Billing", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    val planName = userSubscription?.planTier?.replace("_", " ")?.uppercase() ?: "FREE"
                                    Text("Current Plan: $planName", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Icon(Icons.Default.ArrowForward, "Open", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Documents Vault Button
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.profileSubScreen = "document_vault" },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(PrimaryBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Folder, "Vault", tint = PrimaryBlue)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("My Document Vault", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("Store ID, Transcripts, CV drafts securely", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                }
                                Icon(Icons.Default.ArrowForward, "Open", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // LS Referral Rewards Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.profileSubScreen = "referral_program" }
                                .testTag("referral_program_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CardGiftcard, contentDescription = "Referrals", tint = AccentOrange)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "LS Referral Program",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = "Open",
                                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Share your link — when a friend registers and completes their profile, you both get 7 days of Premium free!",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "CODE: ${userProfile?.referralCode ?: "REF-LS-UG"}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Black,
                                        color = PrimaryBlue
                                    )
                                    Text(
                                        text = "Invite Friends →",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentOrange
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Other LS Services & Corporate Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.profileSubScreen = "other_services" }
                                .testTag("other_services_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.BusinessCenter, contentDescription = "Other Services", tint = PrimaryBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Other LS Services",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Text(
                                        text = "NSSF, Business Registration, TIN, Bulk SMS & NIRA",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Open",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "System Settings",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Theme Selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Palette, contentDescription = "Theme", tint = PrimaryBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("App Theme Mode", fontWeight = FontWeight.Bold)
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val choices = listOf("light", "dark", "system")
                                choices.forEach { mode ->
                                    val isSelected = themeMode == mode
                                    Button(
                                        onClick = { viewModel.setTheme(mode) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(38.dp)
                                            .testTag("theme_btn_$mode")
                                    ) {
                                        Text(
                                            text = mode.uppercase(),
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (isLoggedIn) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Notification Preferences Card
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("notification_settings_card"),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = PrimaryBlue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Notification Preferences", fontWeight = FontWeight.Bold)
                                }

                                val isNotifGranted = viewModel.isNotificationPermissionGranted(context)
                                if (isNotifGranted) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.NotificationsActive, "Active", tint = Color(0xFF2E7D32))
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text("Phone Notifications Active", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                                                Text("You will receive instant alerts when jobs are published.", fontSize = 11.sp, color = Color(0xFF2E7D32))
                                            }
                                        }
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                            .padding(12.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.NotificationsOff, "Disabled", tint = MaterialTheme.colorScheme.error)
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("System Notifications Blocked", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
                                                    Text("Allow notifications in phone settings to get instant job updates.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Button(
                                                    onClick = {
                                                        viewModel.checkAndRequestNotificationPermission(context)
                                                    },
                                                    modifier = Modifier.weight(1f).height(36.dp),
                                                    shape = RoundedCornerShape(8.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                                ) {
                                                    Text("Enable Alerts", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        viewModel.openPhoneNotificationSettings(context)
                                                    },
                                                    modifier = Modifier.weight(1f).height(36.dp),
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("Phone Settings", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                // Toggle 1: Notify All Jobs
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("All New Jobs", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Instant alerts for any newly posted Ugandan vacancy.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                    }
                                    Switch(
                                        checked = userProfile?.notifyAllJobs ?: true,
                                        onCheckedChange = { viewModel.updateNotifyAllJobs(it) },
                                        modifier = Modifier.testTag("toggle_notify_all_jobs")
                                    )
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                // Toggle 2: Notify Matching Preferences
                                val isFree = userSubscription?.planTier == "free" && userSubscription?.status != "trial"
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("Targeted Job Matches", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = if (isFree) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface)
                                            if (isFree) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .background(Color(0xFFFFF3E0), RoundedCornerShape(4.dp))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text("PREMIUM", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AccentOrange)
                                                }
                                            }
                                        }
                                        Text(
                                            text = if (isFree) "Upgrade to unlock notifications matching preferred categories & locations." else "Alerts customized to your category and location fields.",
                                            fontSize = 11.sp,
                                            color = if (isFree) AccentOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                        )
                                    }
                                    Switch(
                                        checked = if (isFree) false else (userProfile?.notifyMatchingPreferences ?: false),
                                        onCheckedChange = { isChecked ->
                                            if (isFree) {
                                                viewModel.profileSubScreen = "subscription_comparison"
                                            } else if (isChecked) {
                                                showActivationFilterDialog = true
                                            } else {
                                                viewModel.updateNotifyMatchingPreferences(false)
                                            }
                                        },
                                        enabled = true, // We allow clicking even if free so it triggers the Upgrade prompt!
                                        modifier = Modifier.testTag("toggle_notify_matching_preferences")
                                    )
                                }

                                if (!isFree && userProfile?.notifyMatchingPreferences == true) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(Color(0xFFE3F2FD), RoundedCornerShape(10.dp))
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.FilterList, "Filter", tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Active Interest Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = PrimaryBlue)
                                                }
                                                Surface(
                                                    onClick = { showActivationFilterDialog = true },
                                                    color = PrimaryBlue.copy(alpha = 0.1f),
                                                    shape = RoundedCornerShape(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Icon(Icons.Default.Edit, "Edit", tint = PrimaryBlue, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Edit Filters", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                                    }
                                                }
                                            }
                                            val cats = userProfile?.preferredCategories?.joinToString(", ").takeIf { !it.isNullOrBlank() } ?: "All Categories"
                                            val locs = userProfile?.preferredLocations?.joinToString(", ").takeIf { !it.isNullOrBlank() } ?: "Any Location"
                                            val skills = userProfile?.skills?.joinToString(", ").takeIf { !it.isNullOrBlank() } ?: "None set"

                                             Text("• Target Categories: $cats", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("• Target Locations: $locs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                            Text("• Skills Keywords: $skills", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedButton(
                                    onClick = { viewModel.sendTestNotification() },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Send Test Push Notification", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Account operations Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showChangePasswordDialog = true }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Lock, "Pass", tint = AccentOrange)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Change Security Password", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowForward, "Open", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.profileSubScreen = "onboarding_tour" }
                                        .padding(vertical = 12.dp)
                                        .testTag("onboarding_tour_row"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.HelpOutline, "App Tour", tint = PrimaryBlue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("App Tour & Onboarding Guide", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowForward, "Open", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.profileSubScreen = "legal_and_about" }
                                        .padding(vertical = 12.dp)
                                        .testTag("legal_about_row"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Gavel, "Legal", tint = PrimaryBlue)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Privacy Policy, Terms & About LS", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowForward, "Open", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }

                                Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showDeleteAccountDialog = true }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Delete Account Permanently", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                                    Spacer(modifier = Modifier.weight(1f))
                                    Icon(Icons.Default.ArrowForward, "Open", tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Future placeholders
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Upcoming Features (Beta)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AccentOrange)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.Star, "Star", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Auto-Generate PDF CV Templates", fontSize = 12.sp, color = Color.Gray)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(Icons.Default.Star, "Star", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Offline AI Recommendation Agent", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isLoggedIn) {
                        Button(
                            onClick = { viewModel.logout() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("logout_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout icon")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign Out Account", fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(60.dp))
                }

                // Global loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            }
        }
    }

    // A. CHANGE PASSWORD DIALOG
    if (showChangePasswordDialog) {
        var newPassInput by remember { mutableStateOf("") }
        var confirmPassInput by remember { mutableStateOf("") }
        var passError by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showChangePasswordDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Change Password", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(12.dp))

                    if (passError != null) {
                        Text(passError!!, color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = newPassInput,
                        onValueChange = { newPassInput = it; passError = null },
                        label = { Text("New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = confirmPassInput,
                        onValueChange = { confirmPassInput = it; passError = null },
                        label = { Text("Confirm New Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showChangePasswordDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (newPassInput.length < 6) {
                                    passError = "Password must be at least 6 characters"
                                } else if (newPassInput != confirmPassInput) {
                                    passError = "Passwords do not match"
                                } else {
                                    viewModel.changePassword(newPassInput) { success ->
                                        if (success) {
                                            showChangePasswordDialog = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Password")
                        }
                    }
                }
            }
        }
    }

    // B. DELETE ACCOUNT DIALOG
    if (showDeleteAccountDialog) {
        var verifyInput by remember { mutableStateOf("") }
        var isVerifyError by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showDeleteAccountDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Delete Account Permanently?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Warning: This action is irreversible. All of your saved documents, applications, and subscription details will be deleted permanently from our databases.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Type \"DELETE\" below to authorize cascade deletion:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = verifyInput,
                        onValueChange = { verifyInput = it; isVerifyError = false },
                        placeholder = { Text("DELETE") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = isVerifyError,
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showDeleteAccountDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (verifyInput.trim() == "DELETE") {
                                    viewModel.deleteAccount { success ->
                                        if (success) {
                                            showDeleteAccountDialog = false
                                        }
                                    }
                                } else {
                                    isVerifyError = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Permanently Delete", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // C. ACTIVATION FILTER DIALOG
    if (showActivationFilterDialog) {
        val currentProfile = userProfile ?: UserProfile(
            id = "user_${System.currentTimeMillis()}",
            fullName = "User"
        )

        var selectedCategories by remember {
            mutableStateOf(currentProfile.preferredCategories.toSet())
        }
        var selectedLocations by remember {
            mutableStateOf(currentProfile.preferredLocations.toSet())
        }
        var skillKeywordsInput by remember {
            mutableStateOf(currentProfile.skills.joinToString(", "))
        }

        val defaultCategories = listOf("Engineering & IT", "Healthcare", "Sales & Marketing", "Education", "Finance", "Agriculture", "Other")
        val defaultLocations = listOf("Kampala", "Mbarara", "Jinja", "Entebbe", "Gulu", "Arua", "Remote")

        Dialog(onDismissRequest = { showActivationFilterDialog = false }) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, "Alerts", tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Activation Filter Criteria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Set target criteria for instant job alerts", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Section 1: Target Categories
                    Text("1. Target Job Categories", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                    Text("Select categories you want notifications for:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))

                    val allCategories = (defaultCategories + selectedCategories).distinct()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allCategories.forEach { cat ->
                            val isSelected = selectedCategories.contains(cat)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedCategories = if (isSelected) {
                                        selectedCategories - cat
                                    } else {
                                        selectedCategories + cat
                                    }
                                },
                                label = { Text(cat, fontSize = 11.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Section 2: Target Locations
                    Text("2. Target Locations", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                    Text("Select preferred cities/regions for job alerts:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))

                    val allLocations = (defaultLocations + selectedLocations).distinct()
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allLocations.forEach { loc ->
                            val isSelected = selectedLocations.contains(loc)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedLocations = if (isSelected) {
                                        selectedLocations - loc
                                    } else {
                                        selectedLocations + loc
                                    }
                                },
                                label = { Text(loc, fontSize = 11.sp) },
                                leadingIcon = if (isSelected) {
                                    { Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp)) }
                                } else null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Section 3: Skill / Keyword Triggers
                    Text("3. Skill & Job Keywords", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
                    Text("Enter comma-separated keywords (e.g., Nurse, Accounting, Driver, Graphic Design):", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = skillKeywordsInput,
                        onValueChange = { skillKeywordsInput = it },
                        placeholder = { Text("e.g. Accounting, Software, Driver, Nursing", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = false,
                        maxLines = 3
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showActivationFilterDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val parsedSkills = skillKeywordsInput
                                    .split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }

                                val updatedProfile = currentProfile.copy(
                                    preferredCategories = selectedCategories.toList(),
                                    preferredLocations = selectedLocations.toList(),
                                    skills = parsedSkills
                                )

                                viewModel.updateUserProfile(updatedProfile) { _ ->
                                    viewModel.updateNotifyMatchingPreferences(true)
                                    showActivationFilterDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.NotificationsActive, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save & Activate Alerts", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 1. PROFILE COMPLETION FLOW SUB-SCREEN (MULTISTEP)
// ==========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCompletionFlowScreen(
    viewModel: AppViewModel,
    userProfile: UserProfile?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var currentStep by remember { mutableStateOf(1) }

    // Step 1: Basic Info
    var fullNameInput by remember(userProfile) { mutableStateOf(userProfile?.fullName ?: "") }
    var phoneInput by remember(userProfile) { mutableStateOf(userProfile?.phone ?: "") }

    // Step 2: Education (List of school objects)
    val initialEducation = remember(userProfile) {
        val list = mutableListOf<EducationItem>()
        try {
            val arr = JSONArray(userProfile?.education ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    EducationItem(
                        school = obj.optString("school", ""),
                        degree = obj.optString("degree", ""),
                        startYear = obj.optString("start_year", ""),
                        endYear = obj.optString("end_year", "")
                    )
                )
            }
        } catch (e: Exception) {}
        list
    }
    val educationList = remember { mutableStateListOf<EducationItem>().apply { addAll(initialEducation) } }

    var tempSchool by remember { mutableStateOf("") }
    var tempDegree by remember { mutableStateOf("") }
    var tempStartYear by remember { mutableStateOf("") }
    var tempEndYear by remember { mutableStateOf("") }
    var showAddEducationDialog by remember { mutableStateOf(false) }

    // Step 3: Skills
    val initialSkills = remember(userProfile) { userProfile?.skills ?: emptyList() }
    val selectedSkills = remember { mutableStateListOf<String>().apply { addAll(initialSkills) } }
    var tempSkillInput by remember { mutableStateOf("") }

    val presetSkills = listOf(
        "Android (Kotlin)", "Jetpack Compose", "Git & GitHub", "Project Management",
        "SQL / Supabase", "Sales & Marketing", "Accounting (QBO)", "UI/UX Design",
        "Public Speaking", "Data Analytics", "Customer Support", "HTML/CSS/JS"
    )

    // Step 4: Work Experience (List of experience objects)
    val initialExperience = remember(userProfile) {
        val list = mutableListOf<ExperienceItem>()
        try {
            val arr = JSONArray(userProfile?.experience ?: "[]")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    ExperienceItem(
                        company = obj.optString("company", ""),
                        role = obj.optString("role", ""),
                        startYear = obj.optString("start_year", ""),
                        endYear = obj.optString("end_year", ""),
                        achievements = obj.optString("achievements", "")
                    )
                )
            }
        } catch (e: Exception) {}
        list
    }
    val experienceList = remember { mutableStateListOf<ExperienceItem>().apply { addAll(initialExperience) } }

    var tempCompany by remember { mutableStateOf("") }
    var tempRole by remember { mutableStateOf("") }
    var tempExpStartYear by remember { mutableStateOf("") }
    var tempExpEndYear by remember { mutableStateOf("") }
    var tempAchievements by remember { mutableStateOf("") }
    var showAddExperienceDialog by remember { mutableStateOf(false) }

    // Step 5: Preferences
    val initialCategories = remember(userProfile) { userProfile?.preferredCategories ?: emptyList() }
    val selectedCategories = remember { mutableStateListOf<String>().apply { addAll(initialCategories) } }

    val initialLocations = remember(userProfile) { userProfile?.preferredLocations ?: emptyList() }
    val selectedLocations = remember { mutableStateListOf<String>().apply { addAll(initialLocations) } }

    val categoriesList = listOf("Engineering & IT", "Healthcare", "Sales & Marketing", "Education", "Finance", "Agriculture", "Other")
    val locationsList = listOf("Kampala", "Mbarara", "Jinja", "Entebbe", "Gulu", "Arua", "Remote")

    // Validation alerts
    var stepValidationError by remember { mutableStateOf<String?>(null) }

    // Helper to validate current step before proceeding
    fun validateAndNext() {
        stepValidationError = null
        when (currentStep) {
            1 -> {
                if (fullNameInput.trim().isEmpty()) {
                    stepValidationError = "Full Name is required"
                    return
                }
                // Validate Ugandan phone format roughly
                val cleanedPhone = phoneInput.trim().replace(" ", "")
                if (cleanedPhone.isEmpty()) {
                    stepValidationError = "Phone Number is required"
                    return
                }
                if (!cleanedPhone.all { it.isDigit() || it == '+' } || cleanedPhone.length < 9) {
                    stepValidationError = "Please enter a valid phone number (e.g., 0770000000)"
                    return
                }
                currentStep = 2
            }
            2 -> {
                if (educationList.isEmpty()) {
                    stepValidationError = "Please add at least one Education entry"
                    return
                }
                currentStep = 3
            }
            3 -> {
                if (selectedSkills.size < 3) {
                    stepValidationError = "Please add at least 3 skills to customize your CV"
                    return
                }
                currentStep = 4
            }
            4 -> {
                if (experienceList.isEmpty()) {
                    stepValidationError = "Please add at least one Work Experience entry"
                    return
                }
                currentStep = 5
            }
            5 -> {
                if (selectedCategories.isEmpty()) {
                    stepValidationError = "Please select at least one Job Category preference"
                    return
                }
                if (selectedLocations.isEmpty()) {
                    stepValidationError = "Please select at least one Job Location preference"
                    return
                }
                currentStep = 6
            }
        }
    }

    // Save final profile back to Supabase
    fun saveCompletedProfile() {
        if (userProfile == null) return

        // Format Education list as JSON Array string
        val edArr = JSONArray()
        educationList.forEach { ed ->
            val obj = JSONObject().apply {
                put("school", ed.school)
                put("degree", ed.degree)
                put("start_year", ed.startYear)
                put("end_year", ed.endYear)
            }
            edArr.put(obj)
        }

        // Format Experience list as JSON Array string
        val expArr = JSONArray()
        experienceList.forEach { ex ->
            val obj = JSONObject().apply {
                put("company", ex.company)
                put("role", ex.role)
                put("start_year", ex.startYear)
                put("end_year", ex.endYear)
                put("achievements", ex.achievements)
            }
            expArr.put(obj)
        }

        val updatedProfile = userProfile.copy(
            fullName = fullNameInput.trim(),
            phone = phoneInput.trim(),
            education = edArr.toString(),
            skills = selectedSkills.toList(),
            experience = expArr.toString(),
            preferredCategories = selectedCategories.toList(),
            preferredLocations = selectedLocations.toList()
        )

        viewModel.updateUserProfile(updatedProfile) { success ->
            if (success) {
                onBack() // return to main Profile
            }
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Form Header with progress
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Onboarding CV Builder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    TextButton(onClick = onBack) {
                        Text("Skip for Now", color = AccentOrange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Custom Step dots
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (i in 1..6) {
                        val active = i <= currentStep
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(if (active) PrimaryBlue else Color.LightGray.copy(alpha = 0.5f))
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                val stepNames = listOf("", "Basic Info", "Education", "My Skills", "Experience", "Preferences", "Review & Sync")
                Text(
                    text = "Step $currentStep of 6: ${stepNames[currentStep]}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        // Error message banner
        if (stepValidationError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.errorContainer)
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, "Err", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stepValidationError!!,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (stepValidationError!!.contains("limit reached", ignoreCase = true)) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "👉 Click here to Upgrade Subscription now",
                                color = PrimaryBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable {
                                    onBack() // Takes the user back to the main settings list
                                    viewModel.profileSubScreen = "subscription_comparison"
                                }
                            )
                        }
                    }
                }
            }
        }

        // Step Contents Column
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            when (currentStep) {
                1 -> {
                    // STEP 1: BASIC INFO
                    Text("Introduce Yourself", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Used by the automatic dispatch engine to fill out application forms.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = fullNameInput,
                        onValueChange = { fullNameInput = it; stepValidationError = null },
                        label = { Text("Full Legal Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it; stepValidationError = null },
                        label = { Text("Mobile Phone Number") },
                        placeholder = { Text("E.g., 0770000000") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Provide a working number for WhatsApp or direct employer calls.", fontSize = 11.sp, color = Color.Gray)
                }
                2 -> {
                    // STEP 2: EDUCATION (LIST)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Academic History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Highest certificate or degree earned", fontSize = 12.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = { showAddEducationDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (educationList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No academic background added yet. Add school or training centers.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        educationList.forEachIndexed { index, ed ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.School, "School", tint = PrimaryBlue, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ed.degree, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(ed.school, fontSize = 12.sp, color = Color.Gray)
                                        Text("${ed.startYear} - ${ed.endYear}", fontSize = 11.sp, color = PrimaryBlue)
                                    }
                                    IconButton(
                                        onClick = { educationList.removeAt(index) }
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
                3 -> {
                    // STEP 3: SKILLS (CHIPS)
                    Text("My Top Core Skills", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Select from suggestions or add custom skills. (Add at least 3)", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(14.dp))

                    // Custom add row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = tempSkillInput,
                            onValueChange = { tempSkillInput = it },
                            label = { Text("Custom Skill Name") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (tempSkillInput.trim().isNotEmpty()) {
                                    val skill = tempSkillInput.trim()
                                    if (!selectedSkills.contains(skill)) {
                                        selectedSkills.add(skill)
                                    }
                                    tempSkillInput = ""
                                    stepValidationError = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text("Add")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current Added Skills Title
                    Text("Active Skills (${selectedSkills.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Added Skills chip list
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        selectedSkills.forEach { skill ->
                            InputChip(
                                selected = true,
                                onClick = { selectedSkills.remove(skill) },
                                label = { Text(skill, fontSize = 11.sp) },
                                trailingIcon = { Icon(Icons.Default.Close, "Remove", modifier = Modifier.size(12.dp)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Preset Suggestions List
                    Text("Suggested for You (Tap to add)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presetSkills.filter { !selectedSkills.contains(it) }.forEach { skill ->
                            SuggestionChip(
                                onClick = {
                                    selectedSkills.add(skill)
                                    stepValidationError = null
                                },
                                label = { Text(skill, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                4 -> {
                    // STEP 4: EXPERIENCE (LIST)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Work Experience", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("List previous jobs or volunteer service.", fontSize = 12.sp, color = Color.Gray)
                        }
                        Button(
                            onClick = { showAddExperienceDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (experienceList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No professional background added yet. Add your career history.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        experienceList.forEachIndexed { index, ex ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Work, "Work", tint = AccentOrange, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(ex.role, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(ex.company, fontSize = 12.sp, color = Color.Gray)
                                        Text("${ex.startYear} - ${ex.endYear}", fontSize = 11.sp, color = AccentOrange)
                                        if (ex.achievements.isNotEmpty()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(ex.achievements, fontSize = 11.sp, color = Color.DarkGray, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                    IconButton(
                                        onClick = { experienceList.removeAt(index) }
                                    ) {
                                        Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }
                }
                5 -> {
                    // STEP 5: PREFERENCES (CATEGORIES & LOCATIONS)
                    val userSubscription by viewModel.userSubscription.collectAsState()
                    val categoriesLimit = userSubscription?.categoriesLimit ?: 2
                    val isMatchingNotifEnabled = userProfile?.notifyMatchingPreferences == true

                    Text("Job Preferences", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("We will only match you with jobs fitting these filters.", fontSize = 12.sp, color = Color.Gray)
                    
                    if (isMatchingNotifEnabled) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Matching Notifications Active: Limit is $categoriesLimit selections per filter on your current plan tier.",
                            fontSize = 11.sp,
                            color = PrimaryBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    // Categories Selector
                    Text("Target Job Sectors", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categoriesList.forEach { cat ->
                            val selected = selectedCategories.contains(cat)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) {
                                        selectedCategories.remove(cat)
                                        stepValidationError = null
                                    } else {
                                        if (isMatchingNotifEnabled && selectedCategories.size >= categoriesLimit) {
                                            stepValidationError = "Category limit reached ($categoriesLimit max for your plan). Upgrade to Premium to add more!"
                                        } else {
                                            selectedCategories.add(cat)
                                            stepValidationError = null
                                        }
                                    }
                                },
                                label = { Text(cat, fontSize = 11.sp) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Locations Selector
                    Text("Preferred Working Locations", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        locationsList.forEach { loc ->
                            val selected = selectedLocations.contains(loc)
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    if (selected) {
                                        selectedLocations.remove(loc)
                                        stepValidationError = null
                                    } else {
                                        // Sharing categoriesLimit for preferred locations as well to protect notify filters
                                        if (isMatchingNotifEnabled && selectedLocations.size >= categoriesLimit) {
                                            stepValidationError = "Location limit reached ($categoriesLimit max for your plan). Upgrade to Premium to add more!"
                                        } else {
                                            selectedLocations.add(loc)
                                            stepValidationError = null
                                        }
                                    }
                                },
                                label = { Text(loc, fontSize = 11.sp) }
                            )
                        }
                    }
                }
                6 -> {
                    // STEP 6: REVIEW & SUBMIT
                    Text("Review Profile CV Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Double check details below before synchronizing to auto-dispatch servers.", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Section 1: Basic
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccountCircle, "Profile", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Personal Details", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Name: $fullNameInput", fontSize = 12.sp)
                            Text("Mobile: $phoneInput", fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Section 2: Education
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, "School", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Academic Background (${educationList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            educationList.forEach { ed ->
                                Text("• ${ed.degree} - ${ed.school} (${ed.startYear}-${ed.endYear})", fontSize = 11.sp, color = Color.DarkGray)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Section 3: Work
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Work, "Work", tint = AccentOrange, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Work Experience (${experienceList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            experienceList.forEach { ex ->
                                Text("• ${ex.role} at ${ex.company} (${ex.startYear}-${ex.endYear})", fontSize = 11.sp, color = Color.DarkGray)
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            Spacer(modifier = Modifier.height(8.dp))

                            // Section 4: Skills
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, "Skills", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("CV Skills Chipsets", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                            Text(selectedSkills.joinToString(", "), fontSize = 11.sp, color = Color.DarkGray)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }

        // Stepper Navigation Footer Buttons (Fixed Bottom)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStep > 1) {
                    OutlinedButton(
                        onClick = { currentStep -= 1; stepValidationError = null },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.ArrowBack, "Back")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Back")
                    }
                } else {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("Later")
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                if (currentStep < 6) {
                    Button(
                        onClick = { validateAndNext() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f).height(48.dp)
                    ) {
                        Text("Continue", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.Default.ArrowForward, "Next")
                    }
                } else {
                    Button(
                        onClick = { saveCompletedProfile() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1.5f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Check, "Save")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save & Sync Profile", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // STEP 2 DIALOG: ADD EDUCATION ITEM
    if (showAddEducationDialog) {
        var schoolVal by remember { mutableStateOf("") }
        var degreeVal by remember { mutableStateOf("") }
        var startYVal by remember { mutableStateOf("") }
        var endYVal by remember { mutableStateOf("") }
        var dialogErr by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showAddEducationDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Add Academic Background", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (dialogErr != null) {
                        Text(dialogErr!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    OutlinedTextField(
                        value = degreeVal,
                        onValueChange = { degreeVal = it; dialogErr = null },
                        label = { Text("Degree / Certification") },
                        placeholder = { Text("E.g., Bachelor of Computer Science") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = schoolVal,
                        onValueChange = { schoolVal = it; dialogErr = null },
                        label = { Text("School / University / Center") },
                        placeholder = { Text("E.g., Makerere University") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startYVal,
                            onValueChange = { startYVal = it; dialogErr = null },
                            label = { Text("Start Year") },
                            placeholder = { Text("YYYY") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = endYVal,
                            onValueChange = { endYVal = it; dialogErr = null },
                            label = { Text("End Year") },
                            placeholder = { Text("YYYY or Present") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddEducationDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val sYr = startYVal.trim()
                                val eYr = endYVal.trim()
                                if (schoolVal.trim().isEmpty() || degreeVal.trim().isEmpty() || sYr.isEmpty() || eYr.isEmpty()) {
                                    dialogErr = "All fields are required"
                                } else if (sYr.length != 4 || !sYr.all { it.isDigit() }) {
                                    dialogErr = "Start year must be a 4-digit number"
                                } else if (eYr != "Present" && (eYr.length != 4 || !eYr.all { it.isDigit() })) {
                                    dialogErr = "End year must be a 4-digit number or 'Present'"
                                } else if (eYr != "Present" && eYr.toInt() < sYr.toInt()) {
                                    dialogErr = "End year cannot be before start year"
                                } else {
                                    educationList.add(
                                        EducationItem(
                                            school = schoolVal.trim(),
                                            degree = degreeVal.trim(),
                                            startYear = sYr,
                                            endYear = eYr
                                        )
                                    )
                                    showAddEducationDialog = false
                                    stepValidationError = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add Entry")
                        }
                    }
                }
            }
        }
    }

    // STEP 4 DIALOG: ADD EXPERIENCE ITEM
    if (showAddExperienceDialog) {
        var compVal by remember { mutableStateOf("") }
        var roleVal by remember { mutableStateOf("") }
        var startYVal by remember { mutableStateOf("") }
        var endYVal by remember { mutableStateOf("") }
        var achVal by remember { mutableStateOf("") }
        var dialogErr by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showAddExperienceDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Add Career Experience", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    if (dialogErr != null) {
                        Text(dialogErr!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    OutlinedTextField(
                        value = roleVal,
                        onValueChange = { roleVal = it; dialogErr = null },
                        label = { Text("Job Position / Title") },
                        placeholder = { Text("E.g., Senior Officer") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = compVal,
                        onValueChange = { compVal = it; dialogErr = null },
                        label = { Text("Employer / Company Name") },
                        placeholder = { Text("E.g., MTN Uganda") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = startYVal,
                            onValueChange = { startYVal = it; dialogErr = null },
                            label = { Text("Start Year") },
                            placeholder = { Text("YYYY") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = endYVal,
                            onValueChange = { endYVal = it; dialogErr = null },
                            label = { Text("End Year") },
                            placeholder = { Text("YYYY or Present") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = achVal,
                        onValueChange = { achVal = it; dialogErr = null },
                        label = { Text("Key Achievements (Optional)") },
                        placeholder = { Text("E.g., increased regional targets by 20%") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showAddExperienceDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val sYr = startYVal.trim()
                                val eYr = endYVal.trim()
                                if (compVal.trim().isEmpty() || roleVal.trim().isEmpty() || sYr.isEmpty() || eYr.isEmpty()) {
                                    dialogErr = "First four fields are required"
                                } else if (sYr.length != 4 || !sYr.all { it.isDigit() }) {
                                    dialogErr = "Start year must be a 4-digit number"
                                } else if (eYr != "Present" && (eYr.length != 4 || !eYr.all { it.isDigit() })) {
                                    dialogErr = "End year must be a 4-digit number or 'Present'"
                                } else if (eYr != "Present" && eYr.toInt() < sYr.toInt()) {
                                    dialogErr = "End year cannot be before start year"
                                } else {
                                    experienceList.add(
                                        ExperienceItem(
                                            company = compVal.trim(),
                                            role = roleVal.trim(),
                                            startYear = sYr,
                                            endYear = eYr,
                                            achievements = achVal.trim()
                                        )
                                    )
                                    showAddExperienceDialog = false
                                    stepValidationError = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Add Entry")
                        }
                    }
                }
            }
        }
    }
}

// ==========================================================
// 2. DOCUMENT VAULT SUB-SCREEN
// ==========================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocumentVaultScreen(
    viewModel: AppViewModel,
    userDocuments: List<UserDocument>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var showUploadDialog by remember { mutableStateOf(false) }

    // Helper map for beautiful display names
    val typeLabels = mapOf(
        "id_card" to "National ID Card",
        "academic_transcript" to "Academic Transcript",
        "passport_photo" to "Passport Size Photo",
        "recommendation_letter" to "Recommendation Letter",
        "cover_letter" to "Cover Letter",
        "other" to "Custom Document File"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Vault Header
        TopAppBar(
            title = {
                Column {
                    Text("My Document Vault", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Secure storage for automatic dispatch engine mapping", fontSize = 11.sp, color = Color.Gray)
                }
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, "Back")
                }
            },
            actions = {
                Button(
                    onClick = { showUploadDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Icon(Icons.Default.Upload, "Add", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Document", fontSize = 12.sp)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Content Area
        if (userDocuments.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.FolderOpen,
                        contentDescription = "Empty Folder",
                        tint = PrimaryBlue.copy(alpha = 0.2f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your document vault is empty",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Upload high quality copies of your transcripts, ID, and portfolio. Our auto-apply engines use them directly for final job submissions.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showUploadDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Upload Your First Document")
                    }
                }
            }
        } else {
            // Document List
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
            ) {
                userDocuments.forEach { doc ->
                    val displayType = doc.documentType.substringBefore(":")
                    val label = typeLabels[displayType] ?: doc.documentType.substringAfter(":")
                    
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .background(AccentOrange.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.FilePresent, "Doc", tint = AccentOrange)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = label,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = doc.fileUrl.substringAfterLast("/"),
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Uploaded at: " + doc.uploadedAt.take(10),
                                    fontSize = 10.sp,
                                    color = PrimaryBlue
                                )
                            }
                            
                            // Download/View trigger
                            IconButton(
                                onClick = {
                                    try {
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(doc.fileUrl))
                                        context.startActivity(intent)
                                    } catch(e: Exception) {
                                        Toast.makeText(context, "Cannot open document URL in browser", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            ) {
                                Icon(Icons.Default.Download, "Download File", tint = PrimaryBlue)
                            }

                            // Delete trigger
                            IconButton(
                                onClick = {
                                    viewModel.deleteUserDocument(doc.id, doc.fileUrl)
                                }
                            ) {
                                Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // UPLOAD SIMULATOR FORM DIALOG
    if (showUploadDialog) {
        var selectedType by remember { mutableStateOf("id_card") }
        var customTypeLabel by remember { mutableStateOf("") }
        var mockFileNameInput by remember { mutableStateOf("") }
        var showPresetDropdown by remember { mutableStateOf(false) }
        var uploadErr by remember { mutableStateOf<String?>(null) }

        val typeOptions = listOf(
            "id_card" to "National ID Card",
            "academic_transcript" to "Academic Transcript",
            "passport_photo" to "Passport Size Photo",
            "recommendation_letter" to "Recommendation Letter",
            "cover_letter" to "Cover Letter",
            "other" to "Custom Other Document"
        )

        Dialog(onDismissRequest = { showUploadDialog = false }) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text("Upload New Document", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Selects preset or adds custom categories. Auto-generates simulated secure payload.", fontSize = 11.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(14.dp))

                    if (uploadErr != null) {
                        Text(uploadErr!!, color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    // Preset Selector
                    Text("Document Category Preset", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedCard(
                            onClick = { showPresetDropdown = !showPresetDropdown },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(typeOptions.firstOrNull { it.first == selectedType }?.second ?: "Select Preset")
                                Icon(Icons.Default.ArrowDropDown, "down")
                            }
                        }
                        DropdownMenu(
                            expanded = showPresetDropdown,
                            onDismissRequest = { showPresetDropdown = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            typeOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.second) },
                                    onClick = {
                                        selectedType = opt.first
                                        showPresetDropdown = false
                                        uploadErr = null
                                    }
                                )
                            }
                        }
                    }

                    if (selectedType == "other") {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = customTypeLabel,
                            onValueChange = { customTypeLabel = it; uploadErr = null },
                            label = { Text("Specify Document Type Label") },
                            placeholder = { Text("E.g., Birth Certificate, Portfolio PDF") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = mockFileNameInput,
                        onValueChange = { mockFileNameInput = it; uploadErr = null },
                        label = { Text("Source File Name") },
                        placeholder = { Text("E.g., national_id_scan.jpg") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Choose JPG, PNG, or PDF file matching the document.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showUploadDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                val cleanFileName = mockFileNameInput.trim()
                                if (cleanFileName.isEmpty()) {
                                    uploadErr = "File Name is required"
                                } else if (selectedType == "other" && customTypeLabel.trim().isEmpty()) {
                                    uploadErr = "Please specify custom document label"
                                } else {
                                    val finalType = if (selectedType == "other") "other:${customTypeLabel.trim()}" else selectedType
                                    
                                    // Generate a mock byte array payload representing file scans
                                    val mockPayloadText = "LS-Services Simulated File Scan Binary Payload for file $cleanFileName category $finalType"
                                    val mockBytes = mockPayloadText.toByteArray()
                                    val mimeType = when {
                                        cleanFileName.endsWith(".pdf", true) -> "application/pdf"
                                        cleanFileName.endsWith(".png", true) -> "image/png"
                                        else -> "image/jpeg"
                                    }

                                    viewModel.uploadAndSaveDocument(
                                        documentType = finalType,
                                        fileName = cleanFileName,
                                        fileBytes = mockBytes,
                                        mimeType = mimeType
                                    ) { success ->
                                        if (success) {
                                            showUploadDialog = false
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Check, "upload")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Upload Now")
                        }
                    }
                }
            }
        }
    }
}

/**
 * Fallback flow row helper for Chips in Jetpack Compose
 */
@Composable
fun FlowRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable () -> Unit
) {
    androidx.compose.ui.layout.Layout(
        content = content,
        modifier = modifier
    ) { measurables, constraints ->
        val tempPlaceables = measurables.map { measurable ->
            measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        }

        val rows = mutableListOf<List<androidx.compose.ui.layout.Placeable>>()
        var currentRow = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var currentRowWidth = 0

        tempPlaceables.forEach { placeable ->
            if (currentRowWidth + placeable.width > constraints.maxWidth) {
                rows.add(currentRow)
                currentRow = mutableListOf()
                currentRowWidth = 0
            }
            currentRow.add(placeable)
            currentRowWidth += placeable.width + 12
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        var totalHeight = 0
        rows.forEachIndexed { i, row ->
            val rowHeight = row.maxOfOrNull { it.height } ?: 0
            totalHeight += rowHeight + (if (i > 0) 12 else 0)
        }

        layout(constraints.maxWidth, totalHeight) {
            var y = 0
            rows.forEach { row ->
                var x = 0
                row.forEach { placeable ->
                    placeable.placeRelative(x, y)
                    x += placeable.width + 12
                }
                y += (row.maxOfOrNull { it.height } ?: 0) + 12
            }
        }
    }
}
