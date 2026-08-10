package com.example.ui.tabs

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.AppViewModel
import com.example.ui.theme.AccentOrange
import com.example.ui.theme.PrimaryBlue

data class OnboardingSlide(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val badgeText: String
)

val ONBOARDING_SLIDES = listOf(
    OnboardingSlide(
        title = "Discover Verified Vacancies Across Uganda",
        subtitle = "Browse updated corporate, government, NGO, and local job opportunities across Kampala, Mbarara, Jinja, Gulu, and nationwide.",
        icon = Icons.Default.Search,
        badgeText = "Job Discovery"
    ),
    OnboardingSlide(
        title = "Automated CV Builder & One-Tap Apply",
        subtitle = "Generate custom tailored PDF CVs aligned with employer specifications and apply in seconds directly through LS Services.",
        icon = Icons.Default.AutoAwesome,
        badgeText = "Instant Applications"
    ),
    OnboardingSlide(
        title = "Secure Vault & Statutory Services",
        subtitle = "Keep your National ID and academic documents encrypted in your personal vault, plus access NSSF, TIN, & URSB business support.",
        icon = Icons.Default.FolderSpecial,
        badgeText = "Complete Career Suite"
    )
)

@Composable
fun OnboardingScreen(
    viewModel: AppViewModel,
    onFinish: () -> Unit
) {
    var currentSlideIndex by remember { mutableStateOf(0) }
    val slide = ONBOARDING_SLIDES[currentSlideIndex]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
            .testTag("onboarding_screen")
    ) {
        // Skip button top right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = {
                    viewModel.completeOnboarding()
                    onFinish()
                },
                modifier = Modifier.testTag("onboarding_skip_button")
            ) {
                Text(
                    text = "Skip",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center Content Slide
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedContent(
                targetState = slide,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "OnboardingSlideTransition"
            ) { currentSlide ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .background(PrimaryBlue.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = currentSlide.icon,
                            contentDescription = currentSlide.title,
                            tint = PrimaryBlue,
                            modifier = Modifier.size(56.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = AccentOrange.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = currentSlide.badgeText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentOrange,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = currentSlide.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = currentSlide.subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Carousel Dots
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ONBOARDING_SLIDES.indices.forEach { index ->
                    val isSelected = currentSlideIndex == index
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .height(8.dp)
                            .width(if (isSelected) 24.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.2f))
                    )
                }
            }
        }

        // Bottom Action Button
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            val isLastSlide = currentSlideIndex == ONBOARDING_SLIDES.size - 1

            Button(
                onClick = {
                    if (isLastSlide) {
                        viewModel.completeOnboarding()
                        onFinish()
                    } else {
                        currentSlideIndex++
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("onboarding_next_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text(
                    text = if (isLastSlide) "Get Started" else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isLastSlide) Icons.Default.Check else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next arrow",
                    tint = Color.White
                )
            }
        }
    }
}
