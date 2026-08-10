package com.example.ui.tabs

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.LS_SERVICES_WHATSAPP_NUMBER
import com.example.ui.theme.PrimaryBlue

data class LSServiceOffering(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val categoryTag: String,
    val description: String,
    val whatsappMessage: String
)

val LS_SERVICE_OFFERINGS = listOf(
    LSServiceOffering(
        id = "nssf",
        title = "NSSF Registration",
        icon = Icons.Default.AccountBalance,
        categoryTag = "Statutory Compliance",
        description = "Full assistance with National Social Security Fund (NSSF) employer and employee portal registration, compliance certificates, and contribution statement reconciliation.",
        whatsappMessage = "Hi, I'm interested in NSSF Registration services."
    ),
    LSServiceOffering(
        id = "business_reg",
        title = "Business Registration",
        icon = Icons.Default.Business,
        categoryTag = "URSB Corporate",
        description = "Namewise reservation, sole proprietorship, partnership, and company incorporation services with Uganda Registration Services Bureau (URSB).",
        whatsappMessage = "Hi, I'm interested in Business Registration services."
    ),
    LSServiceOffering(
        id = "tin_app",
        title = "TIN Application",
        icon = Icons.Default.ReceiptLong,
        categoryTag = "URA Tax Portal",
        description = "Official Uganda Revenue Authority (URA) Individual and Non-Individual Tax Identification Number (TIN) application, filing assistance, and compliance.",
        whatsappMessage = "Hi, I'm interested in TIN Application services."
    ),
    LSServiceOffering(
        id = "bulk_sms",
        title = "Bulk SMS Services",
        icon = Icons.Default.Message,
        categoryTag = "Enterprise Telecom",
        description = "High-throughput enterprise SMS gateway for applicant alerts, corporate notifications, candidate interviewing broadcasts, and recruitment marketing.",
        whatsappMessage = "Hi, I'm interested in Bulk SMS Services."
    ),
    LSServiceOffering(
        id = "nira",
        title = "NIRA Confirmation Letters",
        icon = Icons.Default.Badge,
        categoryTag = "National Identity",
        description = "National Identification & Registration Authority (NIRA) confirmation letters, lost National ID replacement guidance, and identity verification.",
        whatsappMessage = "Hi, I'm interested in NIRA Confirmation Letters."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherServicesScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Other LS Services",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("other_services_back_button")
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Intro Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("other_services_intro_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryBlue)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "Beyond Job Placements",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "LS Services supports job seekers, entrepreneurs, and established businesses across Uganda with official statutory registrations and enterprise solutions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            items(LS_SERVICE_OFFERINGS, key = { it.id }) { service ->
                ServiceOfferingCard(
                    service = service,
                    onContactClick = {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://wa.me/$LS_SERVICES_WHATSAPP_NUMBER?text=${Uri.encode(service.whatsappMessage)}")
                        }
                        context.startActivity(intent)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ServiceOfferingCard(
    service: LSServiceOffering,
    onContactClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("service_card_${service.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = service.icon,
                        contentDescription = service.title,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = service.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = service.categoryTag,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = service.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // WhatsApp Contact Button
            Button(
                onClick = onContactClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("contact_service_button_${service.id}"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "WhatsApp",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Contact us about this",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}
