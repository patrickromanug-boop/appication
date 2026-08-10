package com.example.ui.tabs

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.MockJob
import com.example.data.supabase.UserProfile
import com.example.ui.AppViewModel
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.AccentOrange
import com.example.util.CvPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

// Extension property to map required documents for MockJob
val MockJob.requiredDocuments: List<String>
    get() = when (category.lowercase()) {
        "it & technology", "engineering" -> listOf("Resume", "Academic Transcript", "ID Passport")
        "healthcare", "medicine" -> listOf("Resume", "Medical License", "ID Passport")
        else -> listOf("Resume", "ID Passport")
    }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyFlowScreen(
    viewModel: AppViewModel,
    job: MockJob,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val userProfile by viewModel.userProfile.collectAsState()
    val userDocuments by viewModel.userDocuments.collectAsState()
    val subscription by viewModel.userSubscription.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val scope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }

    var currentStep by remember { mutableStateOf(1) } // 1 to 5

    // Step 1 State (Editable profile details for this CV)
    var draftName by remember { mutableStateOf("") }
    var draftPhone by remember { mutableStateOf("") }
    var draftSkills by remember { mutableStateOf<List<String>>(emptyList()) }
    var draftEducation by remember { mutableStateOf<List<EducationItem>>(emptyList()) }
    var draftExperience by remember { mutableStateOf<List<ExperienceItem>>(emptyList()) }

    var isEditingProfileInline by remember { mutableStateOf(false) }

    // Selected template
    var selectedTemplateId by remember { mutableStateOf("executive") } // "executive" or "modern"

    // Legal consent checkbox
    var isConsentChecked by remember { mutableStateOf(false) }

    // Load initial data from userProfile
    LaunchedEffect(userProfile) {
        userProfile?.let { prof ->
            draftName = prof.fullName ?: ""
            draftPhone = prof.phone ?: ""
            draftSkills = prof.skills ?: emptyList()
            
            // Parse education
            val edList = mutableListOf<EducationItem>()
            try {
                val arr = JSONArray(prof.education)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    edList.add(
                        EducationItem(
                            school = obj.optString("school", ""),
                            degree = obj.optString("degree", ""),
                            startYear = obj.optString("startYear", ""),
                            endYear = obj.optString("endYear", "")
                        )
                    )
                }
            } catch (e: Exception) {
                // Parse fallback
            }
            draftEducation = edList

            // Parse experience
            val expList = mutableListOf<ExperienceItem>()
            try {
                val arr = JSONArray(prof.experience)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    expList.add(
                        ExperienceItem(
                            company = obj.optString("company", ""),
                            role = obj.optString("role", ""),
                            startYear = obj.optString("startYear", ""),
                            endYear = obj.optString("endYear", ""),
                            achievements = obj.optString("achievements", "")
                        )
                    )
                }
            } catch (e: Exception) {
                // Parse fallback
            }
            draftExperience = expList
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Automatic Application", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1 && currentStep < 5) {
                            currentStep--
                        } else {
                            onDismiss()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Step Progress Indicator
                StepProgressHeader(currentStep = currentStep)
                
                Spacer(modifier = Modifier.height(20.dp))

                // Step Content
                Box(modifier = Modifier.weight(1f)) {
                    when (currentStep) {
                        1 -> StepProfileVerification(
                            name = draftName,
                            phone = draftPhone,
                            skills = draftSkills,
                            education = draftEducation,
                            experience = draftExperience,
                            isEditing = isEditingProfileInline,
                            onNameChange = { draftName = it },
                            onPhoneChange = { draftPhone = it },
                            onSkillsChange = { draftSkills = it },
                            onEducationChange = { draftEducation = it },
                            onExperienceChange = { draftExperience = it },
                            onToggleEdit = { isEditingProfileInline = !isEditingProfileInline }
                        )
                        2 -> StepDocumentCheck(
                            requiredDocs = job.requiredDocuments,
                            userDocs = userDocuments,
                            viewModel = viewModel
                        )
                        3 -> StepCvPreviewAndEdit(
                            name = draftName,
                            phone = draftPhone,
                            skills = draftSkills,
                            education = draftEducation,
                            experience = draftExperience,
                            selectedTemplateId = selectedTemplateId,
                            onTemplateSelected = { selectedTemplateId = it }
                        )
                        4 -> StepConsentAndSubmit(
                            job = job,
                            viewModel = viewModel,
                            isConsentChecked = isConsentChecked,
                            onConsentCheckedChange = { isConsentChecked = it }
                        )
                        5 -> StepConfirmation(
                            job = job,
                            onClose = {
                                viewModel.currentTab = "applications"
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Buttons (Bottom Sticky)
                if (currentStep < 5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentStep > 1) {
                            OutlinedButton(
                                onClick = { currentStep-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.5.dp, PrimaryBlue)
                            ) {
                                Text("Back", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Determine Next Button click handler & validation
                        val isNextEnabled = when (currentStep) {
                            1 -> draftName.isNotEmpty() && draftPhone.isNotEmpty() && !isEditingProfileInline
                            2 -> {
                                val missing = job.requiredDocuments.filter { req ->
                                    userDocuments.none { ud -> ud.documentType.equals(req, ignoreCase = true) }
                                }
                                missing.isEmpty()
                            }
                            3 -> true
                            4 -> isConsentChecked && (viewModel.getApplicationsSubmittedThisMonth() < (subscription?.appliesMonthlyLimit ?: 5))
                            else -> true
                        }

                        Button(
                            onClick = {
                                if (currentStep == 4) {
                                    scope.launch {
                                        isGeneratingPdf = true
                                        val pdfBytes = withContext(Dispatchers.IO) {
                                            CvPdfGenerator.generateCvPdf(
                                                fullName = draftName,
                                                phone = draftPhone,
                                                education = draftEducation,
                                                experience = draftExperience,
                                                skills = draftSkills,
                                                templateId = selectedTemplateId
                                            )
                                        }
                                        
                                        val attachedUrls = job.requiredDocuments.mapNotNull { req ->
                                            userDocuments.firstOrNull { ud -> ud.documentType.equals(req, ignoreCase = true) }?.fileUrl
                                        }

                                        viewModel.submitApplication(
                                            jobId = job.id,
                                            jobTitle = job.title,
                                            jobOrganization = job.organization,
                                            generatedCvBytes = pdfBytes,
                                            documentsAttached = attachedUrls,
                                            onResult = { success, msg ->
                                                isGeneratingPdf = false
                                                if (success) {
                                                    currentStep = 5
                                                } else {
                                                    val errorText = if (msg == "LIMIT_REACHED") {
                                                        "Monthly limit reached. Please upgrade."
                                                    } else {
                                                        msg
                                                    }
                                                    Toast.makeText(context, errorText, Toast.LENGTH_LONG).show()
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    currentStep++
                                }
                            },
                            enabled = isNextEnabled && !isLoading && !isGeneratingPdf,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                                .testTag("apply_flow_next_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            if (isLoading || isGeneratingPdf) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                val btnText = when (currentStep) {
                                    1 -> "Looks Correct"
                                    2 -> "Documents Ready"
                                    3 -> "Looks Perfect"
                                    4 -> "Submit Application"
                                    else -> "Next"
                                }
                                Text(btnText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepProgressHeader(currentStep: Int) {
    val steps = listOf("Verify", "Docs", "CV Setup", "Consent", "Finish")
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            steps.forEachIndexed { index, label ->
                val stepNum = index + 1
                val isActive = stepNum == currentStep
                val isCompleted = stepNum < currentStep

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                when {
                                    isActive -> PrimaryBlue
                                    isCompleted -> Color(0xFF2E7D32)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(Icons.Default.Check, "Done", tint = Color.White, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                text = stepNum.toString(),
                                color = if (isActive) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = label,
                        fontSize = 10.sp,
                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                        color = if (isActive) PrimaryBlue else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }

                if (index < steps.size - 1) {
                    Box(
                        modifier = Modifier
                            .weight(0.4f)
                            .height(2.dp)
                            .background(
                                if (stepNum < currentStep) Color(0xFF2E7D32) else MaterialTheme.colorScheme.surfaceVariant
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun StepProfileVerification(
    name: String,
    phone: String,
    skills: List<String>,
    education: List<EducationItem>,
    experience: List<ExperienceItem>,
    isEditing: Boolean,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onSkillsChange: (List<String>) -> Unit,
    onEducationChange: (List<EducationItem>) -> Unit,
    onExperienceChange: (List<ExperienceItem>) -> Unit,
    onToggleEdit: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Step 1: Profile Verification",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
        Text(
            text = "Please review your details. This information will be used to automatically format your CV.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (!isEditing) {
            // Display summary Cards
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Contact Information", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                        IconButton(onClick = onToggleEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile inline", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Full Name: $name", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Phone: $phone", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Education Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Education", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    if (education.isEmpty()) {
                        Text("No education entries listed.", fontStyle = FontStyle.Italic, fontSize = 12.sp)
                    } else {
                        education.forEachIndexed { i, edu ->
                            Text("${edu.degree} — ${edu.school}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Years: ${edu.startYear} - ${edu.endYear}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            if (i < education.size - 1) Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Experience Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Professional Experience", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    if (experience.isEmpty()) {
                        Text("No experience entries listed.", fontStyle = FontStyle.Italic, fontSize = 12.sp)
                    } else {
                        experience.forEachIndexed { i, exp ->
                            Text("${exp.role} at ${exp.company}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Years: ${exp.startYear} - ${exp.endYear}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            Text(exp.achievements, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                            if (i < experience.size - 1) Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Skills Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Core Skills", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = PrimaryBlue)
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    if (skills.isEmpty()) {
                        Text("No skills added.", fontStyle = FontStyle.Italic, fontSize = 12.sp)
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            skills.forEach { skill ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(PrimaryBlue.copy(alpha = 0.08f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(skill, color = PrimaryBlue, fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Inline Profile Editing Form
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Edit Information", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = AccentOrange)
                        IconButton(onClick = onToggleEdit) {
                            Icon(Icons.Default.Check, contentDescription = "Done editing", tint = Color(0xFF2E7D32))
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = onNameChange,
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = onPhoneChange,
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Skills (comma-separated)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    OutlinedTextField(
                        value = skills.joinToString(", "),
                        onValueChange = { input ->
                            val sList = input.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                            onSkillsChange(sList)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Kotlin, Java, Project Management") }
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onToggleEdit,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Save & Review", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun StepDocumentCheck(
    requiredDocs: List<String>,
    userDocs: List<com.example.data.supabase.UserDocument>,
    viewModel: AppViewModel
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Step 2: Required Documents Check",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
        Text(
            text = "We verify if your vault contains all the supplementary documents required by this employer.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        requiredDocs.forEach { req ->
            val userDoc = userDocs.firstOrNull { ud -> ud.documentType.equals(req, ignoreCase = true) }
            val hasDoc = userDoc != null

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (hasDoc) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (hasDoc) Color(0xFFC8E6C9) else Color(0xFFFFE0B2))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (hasDoc) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = if (hasDoc) "Present" else "Missing",
                            tint = if (hasDoc) Color(0xFF2E7D32) else AccentOrange,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = req,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (hasDoc) Color(0xFF1B5E20) else Color(0xFFE65100)
                            )
                            Text(
                                text = if (hasDoc) "Available in Vault" else "Missing from Vault",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (!hasDoc) {
                        Button(
                            onClick = {
                                // Real programmatic mock upload to simulate storage insertion
                                val mockFileName = "${req.lowercase().replace(" ", "_")}.pdf"
                                val mockFileBytes = "Mock PDF content stream for document: $req".toByteArray()
                                
                                viewModel.uploadAndSaveDocument(
                                    documentType = req,
                                    fileName = mockFileName,
                                    fileBytes = mockFileBytes,
                                    mimeType = "application/pdf",
                                    onComplete = { success ->
                                        if (success) {
                                            Toast.makeText(context, "$req uploaded to vault!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Failed to upload $req", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentOrange),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Upload Now", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        val allPresent = requiredDocs.all { req -> userDocs.any { ud -> ud.documentType.equals(req, ignoreCase = true) } }
        if (allPresent) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ThumbUp, "Cool", tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "All required documents are present in your vault!",
                        color = Color(0xFF1B5E20),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, "Warning", tint = AccentOrange, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        "Please upload all missing required documents to proceed.",
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StepCvPreviewAndEdit(
    name: String,
    phone: String,
    skills: List<String>,
    education: List<EducationItem>,
    experience: List<ExperienceItem>,
    selectedTemplateId: String,
    onTemplateSelected: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Step 3: CV Formatting & Starter Template",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
        Text(
            text = "Select one of our premium, print-ready starter templates below to layout your details.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Template Selection Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Template A: Executive
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTemplateSelected("executive") },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTemplateId == "executive") PrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    2.dp,
                    if (selectedTemplateId == "executive") PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        Icons.Default.Star,
                        "Exec",
                        tint = if (selectedTemplateId == "executive") PrimaryBlue else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Executive", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Formal, centered headers, clean print margins", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // Template B: Modern Left-Border
            Card(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTemplateSelected("modern") },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTemplateId == "modern") PrimaryBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    2.dp,
                    if (selectedTemplateId == "modern") PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Icon(
                        Icons.Default.Face,
                        "Modern",
                        tint = if (selectedTemplateId == "modern") PrimaryBlue else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Modern Lite", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("Left-aligned, high-contrast, modern layout", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // CV Render Mockup Preview
        Text("CV Live Preview Mapped", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = PrimaryBlue)
        Spacer(modifier = Modifier.height(6.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(1.dp, Color.LightGray)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (selectedTemplateId == "executive") {
                    // Executive Layout Mock
                    Text(
                        text = name.uppercase(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = PrimaryBlue,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Phone: $phone  |  Email: Available on Request",
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                } else {
                    // Modern Layout Mock
                    Text(
                        text = name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = PrimaryBlue,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = "Phone: $phone",
                        fontSize = 10.sp,
                        color = Color.DarkGray,
                        textAlign = TextAlign.Start
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Divider(color = PrimaryBlue, thickness = 1.5.dp)
                Spacer(modifier = Modifier.height(10.dp))

                // Education Block
                Text("EDUCATION", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue)
                Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 3.dp))
                if (education.isEmpty()) {
                    Text("No education entries.", fontSize = 9.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                } else {
                    education.forEach { edu ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${edu.degree} - ${edu.school}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
                            Text("${edu.startYear} - ${edu.endYear}", fontStyle = FontStyle.Italic, fontSize = 9.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Experience Block
                Text("PROFESSIONAL EXPERIENCE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue)
                Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 3.dp))
                if (experience.isEmpty()) {
                    Text("No experience entries.", fontSize = 9.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                } else {
                    experience.forEach { exp ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${exp.role} at ${exp.company}", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = Color.Black)
                            Text("${exp.startYear} - ${exp.endYear}", fontStyle = FontStyle.Italic, fontSize = 9.sp, color = Color.Gray)
                        }
                        Text(exp.achievements, fontSize = 9.sp, color = Color.DarkGray, modifier = Modifier.padding(start = 6.dp, top = 2.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Skills Block
                Text("CORE SKILLS & EXPERTISE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = PrimaryBlue)
                Divider(color = Color.LightGray, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 3.dp))
                if (skills.isEmpty()) {
                    Text("No core skills.", fontSize = 9.sp, fontStyle = FontStyle.Italic, color = Color.Gray)
                } else {
                    Text(skills.joinToString("   •   "), fontSize = 9.5.sp, color = Color.Black)
                }
            }
        }
    }
}

@Composable
fun StepConsentAndSubmit(
    job: MockJob,
    viewModel: AppViewModel,
    isConsentChecked: Boolean,
    onConsentCheckedChange: (Boolean) -> Unit
) {
    val subscription by viewModel.userSubscription.collectAsState()
    val submittedThisMonth = viewModel.getApplicationsSubmittedThisMonth()
    val monthlyLimit = subscription?.appliesMonthlyLimit ?: 5
    val hasAllowance = submittedThisMonth < monthlyLimit

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = "Step 4: Dispatch Consent & Subscription",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue
        )
        Text(
            text = "We verify monthly quotas before submitting applications automatically to the employer's systems.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Subscription Stats Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (hasAllowance) Color(0xFFE8F5E9) else Color(0xFFFEEBEE)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, if (hasAllowance) Color(0xFFC8E6C9) else Color(0xFFFFCDD2))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Subscription Status",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (hasAllowance) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Plan Level:", fontSize = 13.sp)
                    Text(subscription?.planTier?.uppercase() ?: "TRIAL", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Monthly Limit:", fontSize = 13.sp)
                    Text("$monthlyLimit automatic applies", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Submitted This Month:", fontSize = 13.sp)
                    Text("$submittedThisMonth applications", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Divider(modifier = Modifier.padding(vertical = 10.dp))

                if (hasAllowance) {
                    val remaining = monthlyLimit - submittedThisMonth
                    Text(
                        "You have $remaining of $monthlyLimit submissions remaining this calendar month.",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF2E7D32)
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "WARNING: You have reached your monthly submission limit ($monthlyLimit/$monthlyLimit). Please upgrade your subscription to enable unlimited submissions.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB71C1C)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                viewModel.activeApplyFlowJob = null // Close active apply flow
                                viewModel.currentTab = "profile"
                                viewModel.profileSubScreen = "subscription_comparison"
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7931E)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                                .testTag("apply_flow_upgrade_button"),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Upgrade to Premium (15 Auto-Applies/month)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (hasAllowance) {
            // Legal Consent Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isConsentChecked,
                        onCheckedChange = onConsentCheckedChange,
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "I authorize LS Services to automatically submit this application on my behalf.",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            "The dispatch system will use your structured CV data and required documents to submit directly to ${job.organization}.",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StepConfirmation(
    job: MockJob,
    onClose: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Application Submitted!",
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = PrimaryBlue,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "Your automatic application for ${job.title} at ${job.organization} is now registered inside our system.",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Status: pending submission verification.",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = AccentOrange,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(48.dp)
        ) {
            Text("Done", fontWeight = FontWeight.Bold)
        }
    }
}
