package com.example.data.supabase

data class SupabaseUser(
    val id: String,
    val email: String,
    val accessToken: String? = null
)

data class UserProfile(
    val id: String,
    val fullName: String,
    val phone: String? = null,
    val role: String = "user", // "user", "admin"
    val education: String = "[]", // JSON representation or plain string for simplicity
    val skills: List<String> = emptyList(),
    val experience: String = "[]",
    val preferredCategories: List<String> = emptyList(),
    val preferredLocations: List<String> = emptyList(),
    val themePreference: String = "system",
    val hideServicesPopup: Boolean = false,
    val referralCode: String? = null,
    val referredBy: String? = null,
    val notifyAllJobs: Boolean = true,
    val notifyMatchingPreferences: Boolean = false
)

data class UserDocument(
    val id: String,
    val userId: String,
    val documentType: String, // e.g., 'id_card', 'academic_transcript', 'passport_photo', 'recommendation_letter', 'cover_letter', or 'other:Custom Label'
    val fileUrl: String,
    val uploadedAt: String
)

data class UserSubscription(
    val id: String,
    val userId: String,
    val planTier: String = "trial", // "trial", "free", "basic", "premium", "premium_pro"
    val status: String = "trial", // "trial", "active", "expired"
    val trialEndsAt: String?,
    val renewalDate: String?,
    val notifDailyLimit: Int? = 5,
    val appliesMonthlyLimit: Int? = 5,
    val categoriesLimit: Int? = 3,
    val paymentProviderRef: String? = null
)

data class JobType(
    val id: String,
    val name: String
)

data class JobCategory(
    val id: String,
    val name: String
)

data class JobLocation(
    val id: String,
    val name: String,
    val latitude: Double?,
    val longitude: Double?
)

data class JobListing(
    val id: String,
    val title: String,
    val organization: String,
    val location: String,
    val category: String,
    val jobType: String,
    val purpose: String,
    val requirements: String,
    val otherDetails: String?,
    val deadline: String,
    val officialLink: String?,
    val opensExternally: Boolean = false,
    val requiredDocuments: List<String> = emptyList(),
    val applicationMethod: String = "auto_apply_supported",
    val viewsCount: Int = 0,
    val status: String = "active",
    val postedBy: String? = null,
    val createdAt: String
)

data class UserApplication(
    val id: String,
    val userId: String,
    val jobId: String,
    val generatedCvUrl: String,
    val documentsAttached: List<String> = emptyList(),
    val status: String = "pending", // "pending", "Forwarded to Employer", "Accepted", "Rejected"
    val appliedAt: String,
    val jobTitle: String? = null,
    val jobOrganization: String? = null,
    val candidateName: String? = "Patrick Roman",
    val candidateEmail: String? = "patrickromanug@gmail.com",
    val candidatePhone: String? = "+256 771 234 567",
    val adminNotes: String? = null
)

data class ReferralRecord(
    val id: String,
    val referrerId: String,
    val referredUserId: String,
    val referredName: String? = null,
    val referredEmail: String? = null,
    val status: String = "pending", // "pending", "completed"
    val rewardGranted: Boolean = false,
    val createdAt: String
)

data class CompanyAd(
    val id: String = java.util.UUID.randomUUID().toString(),
    val companyName: String,
    val headline: String,
    val description: String,
    val photoUrl: String,
    val websiteUrl: String = "",
    val contactPhone: String = "",
    val category: String = "General",
    val isActive: Boolean = true,
    val createdAt: String = "2026-07-28"
)

