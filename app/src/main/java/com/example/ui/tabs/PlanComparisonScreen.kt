package com.example.ui.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import androidx.compose.ui.platform.testTag

// Pricing Configuration constants
object BillingConfig {
    const val PRICE_BASIC_TEXT = "UGX 15,000 / month"
    const val PRICE_PREMIUM_TEXT = "UGX 35,000 / month"
    const val PRICE_PREMIUM_PRO_TEXT = "UGX 75,000 / month"

    const val PRICE_BASIC_VAL = 15000.0
    const val PRICE_PREMIUM_VAL = 35000.0
    const val PRICE_PREMIUM_PRO_VAL = 75000.0
}

data class PlanDisplayDetails(
    val id: String,
    val name: String,
    val priceText: String,
    val notifStyle: String,
    val categoriesLimit: String,
    val appliesLimit: String,
    val adsText: String,
    val isRecommended: Boolean = false,
    val description: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanComparisonScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val userSubscription by viewModel.userSubscription.collectAsState()
    
    val paidPlans = listOf(
        PlanDisplayDetails(
            id = "basic",
            name = "Basic Plan",
            priceText = BillingConfig.PRICE_BASIC_TEXT,
            notifStyle = "Instant (capped to 10 alerts/day)",
            categoriesLimit = "1-2 Categories/Locations",
            appliesLimit = "2-3 Auto-Applies / month",
            adsText = "Contains Ads",
            description = "Perfect for casual job seekers who want instant notification alerts."
        ),
        PlanDisplayDetails(
            id = "premium",
            name = "Premium Plan",
            priceText = BillingConfig.PRICE_PREMIUM_TEXT,
            notifStyle = "Instant (capped to 50 alerts/day)",
            categoriesLimit = "Up to 5 Categories/Locations",
            appliesLimit = "10-15 Auto-Applies / month",
            adsText = "Ad-Free Experience",
            isRecommended = true,
            description = "Our most popular tier. High daily alert caps and 15 monthly auto-applies."
        ),
        PlanDisplayDetails(
            id = "premium_pro",
            name = "Premium Pro Plan",
            priceText = BillingConfig.PRICE_PREMIUM_PRO_TEXT,
            notifStyle = "Instant (Unlimited Alerts)",
            categoriesLimit = "Unlimited Categories & Locations",
            appliesLimit = "Unlimited Auto-Applies",
            adsText = "Ad-Free Experience",
            description = "The ultimate professional toolkit. No boundaries or daily limits."
        )
    )

    var processingPlanId by remember { mutableStateOf<String?>(null) }

    // initiatePayment placeholder function to be wired to a payment provider in the next prompt
    fun initiatePayment(planTier: String) {
        processingPlanId = planTier
        // Simulated navigation or loading state
    }

    if (processingPlanId != null) {
        // Proceeding to payment screen overlay/dialog
        AlertDialog(
            onDismissRequest = { processingPlanId = null },
            confirmButton = {
                Button(
                    onClick = {
                        // Let's also support upgrading mock state easily for developer testing!
                        viewModel.mockUpgradeToTier(processingPlanId!!)
                        processingPlanId = null
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF7931E))
                ) {
                    Text("Complete Instant Upgrade", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { processingPlanId = null }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFF1F3FD4), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Redirecting...", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Proceeding to payment gateway for the ${processingPlanId?.replace("_", " ")?.uppercase()} plan...",
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Select 'Complete Instant Upgrade' to activate this subscription tier immediately.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Your Plan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Current Plan Header Box
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "CURRENT PLAN STATUS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        val activePlan = userSubscription?.planTier?.replace("_", " ")?.uppercase() ?: "FREE"
                        Text(
                            text = activePlan,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        if (userSubscription?.planTier == "trial") {
                            // Calculate simple mock days left or show trial ends at
                            Text(
                                text = "Trial Mode Active • Instant Notifications • Ad-Free",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                        } else if (userSubscription?.planTier == "free") {
                            Text(
                                text = "Basic features only • Ad-Supported • Digest-Only Alerts",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium
                            )
                        } else {
                            val renewal = userSubscription?.renewalDate?.take(10) ?: "Monthly Auto-Renewal"
                            Text(
                                text = "Status: Active • Renews/Expires: $renewal",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "UPGRADE TO PROFESSIONAL ACCESS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }

            // Comparison list cards
            items(paidPlans, key = { it.id }) { plan ->
                val isCurrent = userSubscription?.planTier == plan.id
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("plan_card_${plan.id}"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (plan.isRecommended) Color(0xFFF0F2FA) else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (plan.isRecommended) 2.dp else 1.dp,
                        color = if (plan.isRecommended) Color(0xFF1F3FD4) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        if (plan.isRecommended) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1F3FD4), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                    .align(Alignment.End)
                            ) {
                                Text(
                                    text = "RECOMMENDED",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Text(
                            text = plan.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (plan.isRecommended) Color(0xFF1F3FD4) else MaterialTheme.colorScheme.onSurface
                        )
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = plan.description,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = plan.priceText,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Divider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
                        
                        // Limits specifications
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BulletRowSpec(text = "Notifications: ${plan.notifStyle}")
                            BulletRowSpec(text = "Job Sector Limits: ${plan.categoriesLimit}")
                            BulletRowSpec(text = "Submissions: ${plan.appliesLimit}")
                            BulletRowSpec(text = "Advertising: ${plan.adsText}")
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = { initiatePayment(plan.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("select_plan_button_${plan.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrent) Color.LightGray else Color(0xFFF7931E)
                            ),
                            enabled = !isCurrent,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isCurrent) "Current Plan" else "Select ${plan.name}",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BulletRowSpec(text: String) {
    Row(verticalAlignment = Alignment.Top, modifier = Modifier.fillMaxWidth()) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = "Check",
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(16.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
    }
}
