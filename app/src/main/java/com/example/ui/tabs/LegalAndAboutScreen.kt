package com.example.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LSLogoLockup
import com.example.ui.theme.PrimaryBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalAndAboutScreen(
    initialTab: Int = 0,
    onBack: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(initialTab) }
    val tabs = listOf("Privacy Policy", "Terms of Service", "About LS Services")
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Legal & Company Info",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("legal_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = PrimaryBlue
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.testTag("legal_tab_$index")
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(scrollState)
            ) {
                when (selectedTab) {
                    0 -> PrivacyPolicyContent()
                    1 -> TermsOfServiceContent()
                    2 -> AboutLSServicesContent()
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun PrivacyPolicyContent() {
    Column(modifier = Modifier.testTag("privacy_policy_content")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PrivacyTip, contentDescription = "Privacy", tint = PrimaryBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Privacy Policy & Data Security",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Effective Date: July 2026 • Compliant with Uganda Data Protection & Privacy Act 2019",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LegalSectionHeader("1. Data Collection & Usage")
        LegalParagraph("LS Services collects user information strictly required for employment placement, CV document compilation, and subscription management. This includes your name, contact phone number, email address, educational background, work experience, and preferred job categories.")

        LegalSectionHeader("2. Document Vault Confidentiality")
        LegalParagraph("All credentials and identity files uploaded to your Document Vault (e.g. National ID cards, academic transcripts, recommendation letters) are securely stored and encrypted. LS Services never sells, monetizes, or exposes your uploaded documents to third-party advertisers.")

        LegalSectionHeader("3. Application Forwarding")
        LegalParagraph("When you trigger auto-apply or submit an application, your compiled PDF CV and attached vault documents are exclusively forwarded to the verified recruiting employer associated with that vacancy.")

        LegalSectionHeader("4. Your Rights")
        LegalParagraph("Under the Uganda Data Protection & Privacy Act 2019, you retain full rights to inspect, update, or permanently delete your account data and document vault at any time via the Settings menu in this application.")
    }
}

@Composable
fun TermsOfServiceContent() {
    Column(modifier = Modifier.testTag("terms_of_service_content")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Gavel, contentDescription = "Terms", tint = PrimaryBlue)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Terms of Service",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Last Updated: July 2026 • LS Services Recruitment Framework",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        LegalSectionHeader("1. Acceptance of Terms")
        LegalParagraph("By creating an account or using the LS Services application, you agree to comply with these Terms of Service. You affirm that all information supplied in your profile, CV builder, and document vault is accurate and authentic.")

        LegalSectionHeader("2. Subscriptions & Trial Extensions")
        LegalParagraph("New accounts receive a 14-day free trial. Users may upgrade to Basic, Premium, or Premium Pro tiers. Referral program rewards grant 7 additional free days to active subscriptions upon successful completion of the referred user's profile.")

        LegalSectionHeader("3. Automated CV Generation & Auto-Apply")
        LegalParagraph("Our automated CV engine compiles profile information into standardized PDF layouts. Users are responsible for reviewing auto-generated details prior to triggering single-tap application submissions.")

        LegalSectionHeader("4. Prohibited Conduct")
        LegalParagraph("Users shall not submit fraudulent credentials, attempt unauthorized access, post false job reports, or misuse bulk application features.")
    }
}

@Composable
fun AboutLSServicesContent() {
    Column(modifier = Modifier.testTag("about_ls_content")) {
        LSLogoLockup(logoSize = 40f, showPill = true)

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "About LS Services",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        LegalParagraph("LS Services is a premier Ugandan human resources, recruitment, and corporate compliance firm based in Kampala. We connect job seekers with verified employers, streamline application workflows, and assist businesses with statutory registrations including NSSF, URSB business registration, URA TIN processing, Bulk SMS gateways, and NIRA confirmation services.")

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Official Contact & Office",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = "Location", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Plot 42 Nakasero Road, Kampala, Uganda", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Phone, contentDescription = "Phone", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("+256 771 234 567 / +256 700 123 456", fontSize = 13.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Email, contentDescription = "Email", tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("support@lsrecruitingservices.com", fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun LegalSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = PrimaryBlue,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
    )
}

@Composable
fun LegalParagraph(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        lineHeight = 20.sp,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}
