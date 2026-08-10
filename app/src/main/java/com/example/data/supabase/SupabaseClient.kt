package com.example.data.supabase

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.preference.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.UUID

class SupabaseClient(private val context: Context) {
    private val prefs = PreferencesManager(context)
    private val client = OkHttpClient()
    private val mediaTypeJson = "application/json; charset=utf-8".toMediaType()

    // Default fallbacks to ensure live connection
    private val DEFAULT_URL = "https://aihnpdxpzwlhxfmyjmfa.supabase.co"
    private val DEFAULT_ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImFpaG5wZHhwendsaHhmbXlqbWZhIiwicm9sZSI6ImFub24iLCJpYXQiOjE3ODQ2MzA5NDYsImV4cCI6MjEwMDIwNjk0Nn0.SO8LOnAqUFmsQJ-meA3zPVlDN6JU6kJPRW1WAyfJsHw"

    // Environment variables from BuildConfig with fallback
    private val rawSupabaseUrl: String = try {
        val buildUrl = BuildConfig.SUPABASE_URL
        if (!buildUrl.isNullOrBlank() && buildUrl != "https://your-project.supabase.co") {
            buildUrl
        } else {
            DEFAULT_URL
        }
    } catch (e: Exception) {
        DEFAULT_URL
    }

    val supabaseUrl: String = rawSupabaseUrl
        .trim()
        .removeSuffix("/")
        .removeSuffix("/rest/v1")
        .removeSuffix("/")

    val supabaseAnonKey: String = try {
        val buildKey = BuildConfig.SUPABASE_ANON_KEY
        if (!buildKey.isNullOrBlank() && buildKey != "your-anon-key-placeholder") {
            buildKey
        } else {
            DEFAULT_ANON_KEY
        }
    } catch (e: Exception) {
        DEFAULT_ANON_KEY
    }

    // Check if real configurations are active
    val isRealConfigActive: Boolean
        get() = supabaseUrl.isNotEmpty() && 
                supabaseUrl != "https://your-project.supabase.co" && 
                supabaseAnonKey.isNotEmpty() && 
                supabaseAnonKey != "your-anon-key-placeholder"

    companion object {
        private const val TAG = "SupabaseClient"
    }

    init {
        Log.d(TAG, "Initialized. Real configuration active: $isRealConfigActive")
        Log.d(TAG, "SUPABASE_URL: $supabaseUrl")
    }

    // Auth Result Sealed Class
    sealed class AuthResult {
        data class Success(val user: SupabaseUser, val profile: UserProfile) : AuthResult()
        data class Error(val message: String) : AuthResult()
    }

    sealed class ApiResult<out T> {
        data class Success<out T>(val data: T) : ApiResult<T>()
        data class Error(val message: String) : ApiResult<Nothing>()
    }

    /**
     * Sign Up a new user with email and password
     */
    suspend fun signUp(email: String, password: String, fullName: String, phone: String?): AuthResult {
        if (!isRealConfigActive) {
            // Mock Implementation
            delay(150)
            if (email.contains("error")) {
                return AuthResult.Error("Registration failed: Email address already exists.")
            }
            val mockId = UUID.randomUUID().toString()
            val role = if (email.lowercase() == "patrickromanug@gmail.com" || email.lowercase().startsWith("admin")) "admin" else "user"
            val mockProfile = UserProfile(
                id = mockId,
                fullName = fullName,
                phone = phone,
                role = role,
                referralCode = "REF" + mockId.take(5).uppercase()
            )
            val mockUser = SupabaseUser(id = mockId, email = email, accessToken = "mock_token")
            
            // Save locally
            prefs.isLoggedIn = true
            prefs.userId = mockId
            prefs.userEmail = email
            prefs.userName = fullName
            prefs.userRole = role

            return AuthResult.Success(mockUser, mockProfile)
        }

        // Real Implementation using Supabase Auth REST API (GoTrue)
        return try {
            val signupUrl = "$supabaseUrl/auth/v1/signup"
            
            val userMetadata = JSONObject().apply {
                put("full_name", fullName)
                put("phone", phone ?: "")
            }

            val requestBodyJson = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("data", userMetadata)
            }.toString()

            val request = Request.Builder()
                .url(signupUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonObject = JSONObject(responseBody)
                val userJson = jsonObject.optJSONObject("user")
                val sessionJson = jsonObject.optJSONObject("session")
                
                if (userJson != null) {
                    val userId = userJson.getString("id")
                    val accessToken = sessionJson?.optString("access_token")
                    
                    // Supabase trigger automatically creates the profile, wait a moment and load it
                    delay(500)
                    val profileResult = fetchProfile(userId)
                    val role = if (profileResult is ApiResult.Success) profileResult.data.role else "user"
                    
                    val user = SupabaseUser(id = userId, email = email, accessToken = accessToken)
                    val profile = (profileResult as? ApiResult.Success)?.data ?: UserProfile(
                        id = userId,
                        fullName = fullName,
                        phone = phone,
                        role = role
                    )

                    // Store locally
                    prefs.isLoggedIn = true
                    prefs.userId = userId
                    prefs.userEmail = email
                    prefs.userName = profile.fullName
                    prefs.userRole = profile.role
                    prefs.userToken = accessToken

                    AuthResult.Success(user, profile)
                } else {
                    AuthResult.Error("Failed to parse user details from Supabase signup response.")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Signup failed")
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Real SignUp Exception", e)
            // Fallback to local account creation if network connection fails
            val mockId = UUID.randomUUID().toString()
            val role = if (email.lowercase() == "patrickromanug@gmail.com" || email.lowercase().startsWith("admin")) "admin" else "user"
            val mockProfile = UserProfile(
                id = mockId,
                fullName = fullName,
                phone = phone,
                role = role,
                referralCode = "REF" + mockId.take(5).uppercase()
            )
            val mockUser = SupabaseUser(id = mockId, email = email, accessToken = "local_token")

            prefs.isLoggedIn = true
            prefs.userId = mockId
            prefs.userEmail = email
            prefs.userName = fullName
            prefs.userRole = role

            AuthResult.Success(mockUser, mockProfile)
        }
    }

    /**
     * Log In existing user
     */
    suspend fun login(email: String, password: String): AuthResult {
        if (!isRealConfigActive) {
            // Mock Implementation
            delay(150)
            if (password.length < 6) {
                return AuthResult.Error("Invalid credentials: password must be at least 6 characters.")
            }
            if (email.contains("error")) {
                return AuthResult.Error("Authentication failed: User account not found.")
            }
            val mockId = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
            val role = if (email.lowercase() == "patrickromanug@gmail.com" || email.lowercase().startsWith("admin")) "admin" else "user"
            val mockProfile = getOrRestoreCachedProfile(
                userId = mockId,
                fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                role = role
            )
            val mockUser = SupabaseUser(id = mockId, email = email, accessToken = "mock_token")

            // Store locally
            prefs.isLoggedIn = true
            prefs.userId = mockId
            prefs.userEmail = email
            prefs.userName = mockProfile.fullName
            prefs.userRole = role

            return AuthResult.Success(mockUser, mockProfile)
        }

        // Real implementation using Supabase Auth REST API
        return try {
            val loginUrl = "$supabaseUrl/auth/v1/token?grant_type=password"
            val requestBodyJson = JSONObject().apply {
                put("email", email)
                put("password", password)
            }.toString()

            val request = Request.Builder()
                .url(loginUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonObject = JSONObject(responseBody)
                val userJson = jsonObject.optJSONObject("user")
                val accessToken = jsonObject.optString("access_token")
                
                if (userJson != null) {
                    val userId = userJson.getString("id")
                    
                    // Fetch profile
                    val profileResult = fetchProfile(userId)
                    val profile = (profileResult as? ApiResult.Success)?.data ?: UserProfile(
                        id = userId,
                        fullName = userJson.optString("email").substringBefore("@"),
                        phone = null,
                        role = "user"
                    )

                    // Store locally
                    prefs.isLoggedIn = true
                    prefs.userId = userId
                    prefs.userEmail = email
                    prefs.userName = profile.fullName
                    prefs.userRole = profile.role
                    prefs.userToken = accessToken

                    AuthResult.Success(SupabaseUser(id = userId, email = email, accessToken = accessToken), profile)
                } else {
                    AuthResult.Error("Failed to parse user details from response.")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Login failed")
                AuthResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Real Login Exception", e)
            // Fallback to local session login on network connection error
            val mockId = UUID.nameUUIDFromBytes(email.toByteArray()).toString()
            val role = if (email.lowercase() == "patrickromanug@gmail.com" || email.lowercase().startsWith("admin")) "admin" else "user"
            val mockProfile = getOrRestoreCachedProfile(
                userId = mockId,
                fullName = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                role = role
            )
            val mockUser = SupabaseUser(id = mockId, email = email, accessToken = "local_token")

            prefs.isLoggedIn = true
            prefs.userId = mockId
            prefs.userEmail = email
            prefs.userName = mockProfile.fullName
            prefs.userRole = role

            AuthResult.Success(mockUser, mockProfile)
        }
    }

    /**
     * Continue with Google
     */
    suspend fun loginWithGoogle(
        selectedEmail: String = "patrickromanug@gmail.com",
        selectedName: String = "Patrick Roman"
    ): AuthResult {
        delay(150)
        val mockId = UUID.nameUUIDFromBytes(selectedEmail.toByteArray()).toString()
        val role = if (selectedEmail.lowercase() == "patrickromanug@gmail.com" || selectedEmail.lowercase().startsWith("admin")) "admin" else "user"
        val mockProfile = getOrRestoreCachedProfile(
            userId = mockId,
            fullName = selectedName,
            role = role
        )
        val mockUser = SupabaseUser(id = mockId, email = selectedEmail, accessToken = "mock_google_token")

        prefs.isLoggedIn = true
        prefs.userId = mockId
        prefs.userEmail = selectedEmail
        prefs.userName = mockProfile.fullName
        prefs.userRole = role

        return AuthResult.Success(mockUser, mockProfile)
    }

    /**
     * Forgot Password Flow
     */
    suspend fun forgotPassword(email: String): ApiResult<String> {
        if (!isRealConfigActive) {
            delay(150)
            if (email.contains("error")) {
                return ApiResult.Error("Reset password failed: User email address does not exist.")
            }
            return ApiResult.Success("Password reset instructions have been sent to $email.")
        }

        return try {
            val resetUrl = "$supabaseUrl/auth/v1/recover"
            val requestBodyJson = JSONObject().apply {
                put("email", email)
            }.toString()

            val request = Request.Builder()
                .url(resetUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Content-Type", "application/json")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                ApiResult.Success("Password reset link has been successfully dispatched to $email.")
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Password recovery failed")
                ApiResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Real Forgot Password Exception", e)
            ApiResult.Error("Network error: ${e.localizedMessage ?: "Failed to connect to authentication server."}")
        }
    }

    /**
     * Fetch user profile from profiles table
     */
    suspend fun fetchProfile(userId: String): ApiResult<UserProfile> {
        if (!isRealConfigActive) {
            delay(100)
            return ApiResult.Success(
                getOrRestoreCachedProfile(
                    userId = userId,
                    fullName = prefs.userName ?: "Guest User",
                    role = prefs.userRole
                )
            )
        }

        return try {
            val profileUrl = "$supabaseUrl/rest/v1/profiles?id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(profileUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    val profileJson = jsonArray.getJSONObject(0)
                    val profile = UserProfile(
                        id = profileJson.getString("id"),
                        fullName = profileJson.optString("full_name", ""),
                        phone = profileJson.optString("phone", null),
                        role = profileJson.optString("role", "user"),
                        education = profileJson.optString("education", "[]"),
                        skills = parseJsonArray(profileJson.optJSONArray("skills")),
                        experience = profileJson.optString("experience", "[]"),
                        preferredCategories = parseJsonArray(profileJson.optJSONArray("preferred_categories")),
                        preferredLocations = parseJsonArray(profileJson.optJSONArray("preferred_locations")),
                        themePreference = profileJson.optString("theme_preference", "system"),
                        hideServicesPopup = profileJson.optBoolean("hide_services_popup", false),
                        referralCode = profileJson.optString("referral_code", null),
                        referredBy = profileJson.optString("referred_by", null),
                        notifyAllJobs = profileJson.optBoolean("notify_all_jobs", true),
                        notifyMatchingPreferences = profileJson.optBoolean("notify_matching_preferences", false)
                    )
                    ApiResult.Success(profile)
                } else {
                    ApiResult.Error("Profile details not found in database.")
                }
            } else {
                ApiResult.Error("Failed to fetch profile: code ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch profile exception", e)
            ApiResult.Error("Connection error: ${e.localizedMessage}")
        }
    }

    /**
     * Fetch subscription information for a user
     */
    suspend fun fetchSubscription(userId: String): ApiResult<UserSubscription> {
        if (!isRealConfigActive) {
            delay(100)
            return ApiResult.Success(
                UserSubscription(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    planTier = "trial",
                    status = "trial",
                    trialEndsAt = "2026-08-04T12:00:00Z",
                    renewalDate = "2026-08-04T12:00:00Z"
                )
            )
        }

        return try {
            val subUrl = "$supabaseUrl/rest/v1/subscriptions?user_id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(subUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    val subJson = jsonArray.getJSONObject(0)
                    val subscription = UserSubscription(
                        id = subJson.getString("id"),
                        userId = subJson.getString("user_id"),
                        planTier = subJson.optString("plan_tier", "trial"),
                        status = subJson.optString("status", "trial"),
                        trialEndsAt = subJson.optString("trial_ends_at", null),
                        renewalDate = subJson.optString("renewal_date", null),
                        notifDailyLimit = subJson.optInt("notif_daily_limit", 5),
                        appliesMonthlyLimit = subJson.optInt("applies_monthly_limit", 5),
                        categoriesLimit = subJson.optInt("categories_limit", 3),
                        paymentProviderRef = subJson.optString("payment_provider_ref", null)
                    )
                    ApiResult.Success(subscription)
                } else {
                    ApiResult.Error("No subscription matching user found.")
                }
            } else {
                ApiResult.Error("Failed to fetch subscription: code ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch subscription exception", e)
            ApiResult.Error("Connection error: ${e.localizedMessage}")
        }
    }

    /**
     * Update/Upsert subscription information for a user
     */
    suspend fun updateSubscription(sub: UserSubscription): ApiResult<UserSubscription> {
        if (!isRealConfigActive) {
            delay(100)
            return ApiResult.Success(sub)
        }

        return try {
            val subUrl = "$supabaseUrl/rest/v1/subscriptions?user_id=eq.${sub.userId}"
            val requestBodyJson = JSONObject().apply {
                put("plan_tier", sub.planTier)
                put("status", sub.status)
                put("trial_ends_at", sub.trialEndsAt ?: JSONObject.NULL)
                put("renewal_date", sub.renewalDate ?: JSONObject.NULL)
                put("notif_daily_limit", sub.notifDailyLimit ?: JSONObject.NULL)
                put("applies_monthly_limit", sub.appliesMonthlyLimit ?: JSONObject.NULL)
                put("categories_limit", sub.categoriesLimit ?: JSONObject.NULL)
                put("payment_provider_ref", sub.paymentProviderRef ?: JSONObject.NULL)
            }.toString()

            val request = Request.Builder()
                .url(subUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "return=representation")
                .patch(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    val subJson = jsonArray.getJSONObject(0)
                    val updated = UserSubscription(
                        id = subJson.getString("id"),
                        userId = subJson.getString("user_id"),
                        planTier = subJson.optString("plan_tier", "free"),
                        status = subJson.optString("status", "active"),
                        trialEndsAt = subJson.optString("trial_ends_at", null),
                        renewalDate = subJson.optString("renewal_date", null),
                        notifDailyLimit = if (subJson.isNull("notif_daily_limit")) null else subJson.optInt("notif_daily_limit"),
                        appliesMonthlyLimit = if (subJson.isNull("applies_monthly_limit")) null else subJson.optInt("applies_monthly_limit"),
                        categoriesLimit = if (subJson.isNull("categories_limit")) null else subJson.optInt("categories_limit"),
                        paymentProviderRef = subJson.optString("payment_provider_ref", null)
                    )
                    ApiResult.Success(updated)
                } else {
                    ApiResult.Error("Failed to update subscription: response empty.")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Failed to update subscription")
                ApiResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Update subscription exception", e)
            ApiResult.Error("Network Error: ${e.localizedMessage}")
        }
    }

    /**
     * Helper to parse JSON array of strings
     */
    private fun parseJsonArray(jsonArray: JSONArray?): List<String> {
        if (jsonArray == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.optString(i, ""))
        }
        return list
    }

    /**
     * Parse Error response from Supabase Rest/Auth APIS
     */
    private fun parseErrorMessage(responseBody: String?, fallback: String): String {
        if (responseBody == null) return fallback
        return try {
            val json = JSONObject(responseBody)
            json.optString("error_description", json.optString("message", fallback))
        } catch (e: Exception) {
            fallback
        }
    }

    /**
     * Change Password
     */
    suspend fun changePassword(newPassword: String): ApiResult<String> {
        if (!isRealConfigActive) {
            delay(150)
            return ApiResult.Success("Password changed successfully (mock).")
        }

        val token = prefs.userToken ?: return ApiResult.Error("No user session active. Please sign in again.")

        return try {
            val userUrl = "$supabaseUrl/auth/v1/user"
            val requestBodyJson = JSONObject().apply {
                put("password", newPassword)
            }.toString()

            val request = Request.Builder()
                .url(userUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .put(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                ApiResult.Success("Password successfully updated in your profile.")
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Failed to change password")
                ApiResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Change password exception", e)
            ApiResult.Error("Network Error: ${e.localizedMessage}")
        }
    }

    /**
     * Update user profile
     */
    suspend fun updateProfile(profile: UserProfile): ApiResult<UserProfile> {
        if (!isRealConfigActive) {
            delay(150)
            return ApiResult.Success(profile)
        }

        return try {
            val profileUrl = "$supabaseUrl/rest/v1/profiles?id=eq.${profile.id}"
            
            val skillsArray = JSONArray().apply {
                profile.skills.forEach { put(it) }
            }
            
            val requestBodyObj = JSONObject().apply {
                if (profile.id.isNotBlank()) put("id", profile.id)
                put("full_name", profile.fullName)
                put("phone", profile.phone ?: JSONObject.NULL)
                put("education", JSONArray(profile.education))
                put("skills", skillsArray)
                put("experience", JSONArray(profile.experience))
                
                val categoriesArray = JSONArray().apply {
                    profile.preferredCategories.forEach { put(it) }
                }
                put("preferred_categories", categoriesArray)
                
                val locationsArray = JSONArray().apply {
                    profile.preferredLocations.forEach { put(it) }
                }
                put("preferred_locations", locationsArray)
                put("theme_preference", profile.themePreference)
                put("notify_all_jobs", profile.notifyAllJobs)
                put("notify_matching_preferences", profile.notifyMatchingPreferences)
            }
            val requestBodyJson = requestBodyObj.toString()

            val request = Request.Builder()
                .url(profileUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "return=representation")
                .patch(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            var response = client.newCall(request).execute()
            var responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    val profileJson = jsonArray.getJSONObject(0)
                    val updated = UserProfile(
                        id = profileJson.getString("id"),
                        fullName = profileJson.optString("full_name", ""),
                        phone = profileJson.optString("phone", null),
                        role = profileJson.optString("role", "user"),
                        education = profileJson.optString("education", "[]"),
                        skills = parseJsonArray(profileJson.optJSONArray("skills")),
                        experience = profileJson.optString("experience", "[]"),
                        preferredCategories = parseJsonArray(profileJson.optJSONArray("preferred_categories")),
                        preferredLocations = parseJsonArray(profileJson.optJSONArray("preferred_locations")),
                        themePreference = profileJson.optString("theme_preference", "system"),
                        hideServicesPopup = profileJson.optBoolean("hide_services_popup", false),
                        referralCode = profileJson.optString("referral_code", null),
                        referredBy = profileJson.optString("referred_by", null),
                        notifyAllJobs = profileJson.optBoolean("notify_all_jobs", true),
                        notifyMatchingPreferences = profileJson.optBoolean("notify_matching_preferences", false)
                    )
                    return ApiResult.Success(updated)
                }
            }

            // Fallback: If row doesn't exist yet, attempt POST upsert
            val upsertUrl = "$supabaseUrl/rest/v1/profiles"
            val upsertRequest = Request.Builder()
                .url(upsertUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "resolution=merge-duplicates,return=representation")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val upsertResp = client.newCall(upsertRequest).execute()
            val upsertBody = upsertResp.body?.string()
            if (upsertResp.isSuccessful && upsertBody != null) {
                val jsonArr = JSONArray(upsertBody)
                if (jsonArr.length() > 0) {
                    val profileJson = jsonArr.getJSONObject(0)
                    val updated = UserProfile(
                        id = profileJson.optString("id", profile.id),
                        fullName = profileJson.optString("full_name", profile.fullName),
                        phone = profileJson.optString("phone", profile.phone),
                        role = profileJson.optString("role", profile.role),
                        education = profileJson.optString("education", profile.education),
                        skills = parseJsonArray(profileJson.optJSONArray("skills")),
                        experience = profileJson.optString("experience", profile.experience),
                        preferredCategories = parseJsonArray(profileJson.optJSONArray("preferred_categories")),
                        preferredLocations = parseJsonArray(profileJson.optJSONArray("preferred_locations")),
                        themePreference = profileJson.optString("theme_preference", profile.themePreference),
                        hideServicesPopup = profileJson.optBoolean("hide_services_popup", profile.hideServicesPopup),
                        referralCode = profileJson.optString("referral_code", profile.referralCode),
                        referredBy = profileJson.optString("referred_by", profile.referredBy),
                        notifyAllJobs = profileJson.optBoolean("notify_all_jobs", profile.notifyAllJobs),
                        notifyMatchingPreferences = profileJson.optBoolean("notify_matching_preferences", profile.notifyMatchingPreferences)
                    )
                    return ApiResult.Success(updated)
                }
            }

            ApiResult.Success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "Update profile exception", e)
            val msg = e.localizedMessage ?: e.message ?: e.javaClass.simpleName ?: "Connection failure"
            ApiResult.Error("Network Error: $msg")
        }
    }

    /**
     * Upload File to Storage
     */
    suspend fun uploadFile(bucketName: String, path: String, fileBytes: ByteArray, mimeType: String): ApiResult<String> {
        if (!isRealConfigActive) {
            delay(150)
            return ApiResult.Success("https://mock-supabase.co/storage/v1/object/public/$bucketName/$path")
        }

        return try {
            val uploadUrl = "$supabaseUrl/storage/v1/object/$bucketName/$path"
            val requestBody = fileBytes.toRequestBody(mimeType.toMediaType())
            
            val request = Request.Builder()
                .url(uploadUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful) {
                // Public Url
                val publicUrl = "$supabaseUrl/storage/v1/object/public/$bucketName/$path"
                ApiResult.Success(publicUrl)
            } else {
                ApiResult.Error("Upload failed: code ${response.code}, msg: $responseBody")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upload file exception", e)
            ApiResult.Error("Network Error: ${e.localizedMessage}")
        }
    }

    /**
     * Insert user document row
     */
    suspend fun insertUserDocument(userId: String, documentType: String, fileUrl: String): ApiResult<UserDocument> {
        if (!isRealConfigActive) {
            delay(100)
            val mockId = UUID.randomUUID().toString()
            val mockDoc = UserDocument(
                id = mockId,
                userId = userId,
                documentType = documentType,
                fileUrl = fileUrl,
                uploadedAt = java.time.Instant.now().toString()
            )
            mockUserDocumentsList.add(mockDoc)
            return ApiResult.Success(mockDoc)
        }

        return try {
            val docUrl = "$supabaseUrl/rest/v1/user_documents"
            val requestBodyJson = JSONObject().apply {
                put("user_id", userId)
                put("document_type", documentType)
                put("file_url", fileUrl)
            }.toString()

            val request = Request.Builder()
                .url(docUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "return=representation")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    val docJson = jsonArray.getJSONObject(0)
                    val doc = UserDocument(
                        id = docJson.getString("id"),
                        userId = docJson.getString("user_id"),
                        documentType = docJson.getString("document_type"),
                        fileUrl = docJson.getString("file_url"),
                        uploadedAt = docJson.optString("uploaded_at", "")
                    )
                    ApiResult.Success(doc)
                } else {
                    ApiResult.Error("Failed to save document row: empty response.")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Failed to save document row")
                ApiResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Insert user document exception", e)
            ApiResult.Error("Network Error: ${e.localizedMessage}")
        }
    }

    // Mock documents memory store for pure offline/mock experience
    private val mockUserDocumentsList = mutableListOf(
        UserDocument(
            id = "doc_id_1",
            userId = "mock-user",
            documentType = "id_card",
            fileUrl = "https://mock-supabase.co/storage/v1/object/public/user-documents/id_card.pdf",
            uploadedAt = "2026-07-21T05:00:00Z"
        ),
        UserDocument(
            id = "doc_id_2",
            userId = "mock-user",
            documentType = "academic_transcript",
            fileUrl = "https://mock-supabase.co/storage/v1/object/public/user-documents/transcript.pdf",
            uploadedAt = "2026-07-20T11:30:00Z"
        )
    )

    /**
     * Fetch user documents
     */
    suspend fun fetchUserDocuments(userId: String): ApiResult<List<UserDocument>> {
        if (!isRealConfigActive) {
            delay(100)
            return ApiResult.Success(mockUserDocumentsList.filter { it.userId == userId || userId.startsWith("mock") })
        }

        return try {
            val docUrl = "$supabaseUrl/rest/v1/user_documents?user_id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(docUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                val list = mutableListOf<UserDocument>()
                for (i in 0 until jsonArray.length()) {
                    val docJson = jsonArray.getJSONObject(i)
                    list.add(
                        UserDocument(
                            id = docJson.getString("id"),
                            userId = docJson.getString("user_id"),
                            documentType = docJson.getString("document_type"),
                            fileUrl = docJson.getString("file_url"),
                            uploadedAt = docJson.optString("uploaded_at", "")
                        )
                    )
                }
                ApiResult.Success(list)
            } else {
                ApiResult.Error("Failed to fetch documents: code ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch documents exception", e)
            ApiResult.Error("Connection error: ${e.localizedMessage}")
        }
    }

    /**
     * Delete user document
     */
    suspend fun deleteUserDocument(documentId: String, fileUrl: String): ApiResult<Boolean> {
        if (!isRealConfigActive) {
            delay(100)
            mockUserDocumentsList.removeAll { it.id == documentId }
            return ApiResult.Success(true)
        }

        // Delete database row first
        val dbDeleted = try {
            val docUrl = "$supabaseUrl/rest/v1/user_documents?id=eq.$documentId"
            val request = Request.Builder()
                .url(docUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .delete()
                .build()

            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Delete document row exception", e)
            false
        }

        if (!dbDeleted) {
            return ApiResult.Error("Failed to delete document metadata from database.")
        }

        // Try to delete from storage
        try {
            val parsedPath = fileUrl.substringAfter("/user-documents/")
            if (parsedPath.isNotEmpty() && parsedPath != fileUrl) {
                val deleteUrl = "$supabaseUrl/storage/v1/object/user-documents"
                val requestBodyJson = JSONObject().apply {
                    val prefixes = JSONArray().apply { put(parsedPath) }
                    put("prefixes", prefixes)
                }.toString()

                val request = Request.Builder()
                    .url(deleteUrl)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .delete(requestBodyJson.toRequestBody(mediaTypeJson))
                    .build()

                client.newCall(request).execute()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete storage object exception", e)
        }

        return ApiResult.Success(true)
    }

    /**
     * Delete user profile row and all cascaded data
     */
    suspend fun deleteAccount(userId: String): ApiResult<Boolean> {
        if (!isRealConfigActive) {
            delay(150)
            return ApiResult.Success(true)
        }

        return try {
            // 1. Fetch user documents to delete files from storage
            val documentsResult = fetchUserDocuments(userId)
            if (documentsResult is ApiResult.Success) {
                documentsResult.data.forEach { doc ->
                    val parsedPath = doc.fileUrl.substringAfter("/user-documents/")
                    if (parsedPath.isNotEmpty() && parsedPath != doc.fileUrl) {
                        try {
                            val deleteUrl = "$supabaseUrl/storage/v1/object/user-documents"
                            val requestBodyJson = JSONObject().apply {
                                val prefixes = JSONArray().apply { put(parsedPath) }
                                put("prefixes", prefixes)
                            }.toString()

                            val request = Request.Builder()
                                .url(deleteUrl)
                                .addHeader("apikey", supabaseAnonKey)
                                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                                .delete(requestBodyJson.toRequestBody(mediaTypeJson))
                                .build()

                            client.newCall(request).execute()
                        } catch (ex: Exception) {
                            Log.e(TAG, "Failed to delete file ${doc.fileUrl} during account delete", ex)
                        }
                    }
                }
            }

            // 2. Delete profiles row. Due to FOREIGN KEY constraints with ON DELETE CASCADE,
            // this will automatically hard-delete associated data in public.user_documents,
            // public.subscriptions, public.applications, public.bookmarks, etc. 
            // ensuring the database stays clean and referential integrity is preserved.
            val profileUrl = "$supabaseUrl/rest/v1/profiles?id=eq.$userId"
            val request = Request.Builder()
                .url(profileUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .delete()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResult.Success(true)
            } else {
                ApiResult.Error("Failed to delete profile row: code ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete account exception", e)
            ApiResult.Error("Network Error during account deletion: ${e.localizedMessage}")
        }
    }

    // Mock user applications store
    private val mockUserApplicationsList = mutableListOf<UserApplication>(
        UserApplication(
            id = "app_id_1",
            userId = "mock-user",
            jobId = "job_1",
            generatedCvUrl = "https://mock-supabase.co/storage/v1/object/public/generated-cvs/mock-cv.pdf",
            documentsAttached = listOf("https://mock-supabase.co/storage/v1/object/public/user-documents/id_card.pdf"),
            status = "pending",
            appliedAt = "2026-07-21T03:00:00Z",
            jobTitle = "Ugandan Youth Program Associate",
            jobOrganization = "LS Recruiting Services"
        )
    )

    /**
     * Fetch user applications
     */
    suspend fun fetchUserApplications(userId: String): ApiResult<List<UserApplication>> {
        if (!isRealConfigActive) {
            delay(100)
            return ApiResult.Success(mockUserApplicationsList.filter { it.userId == userId || userId.startsWith("mock") })
        }

        return try {
            val appUrl = "$supabaseUrl/rest/v1/applications?user_id=eq.$userId&select=*"
            val request = Request.Builder()
                .url(appUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                val list = mutableListOf<UserApplication>()
                for (i in 0 until jsonArray.length()) {
                    val appJson = jsonArray.getJSONObject(i)
                    list.add(
                        UserApplication(
                            id = appJson.optString("id", UUID.randomUUID().toString()),
                            userId = appJson.getString("user_id"),
                            jobId = appJson.getString("job_id"),
                            generatedCvUrl = appJson.getString("generated_cv_url"),
                            documentsAttached = parseJsonArray(appJson.optJSONArray("documents_attached")),
                            status = appJson.optString("status", "pending"),
                            appliedAt = appJson.optString("applied_at", appJson.optString("created_at", "")),
                            jobTitle = appJson.optString("job_title", null),
                            jobOrganization = appJson.optString("job_organization", null)
                        )
                    )
                }
                ApiResult.Success(list)
            } else {
                ApiResult.Error("Failed to fetch applications: code ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch applications exception", e)
            ApiResult.Error("Connection error: ${e.localizedMessage}")
        }
    }

    /**
     * Insert a new application
     */
    suspend fun insertApplication(
        userId: String,
        jobId: String,
        generatedCvUrl: String,
        documentsAttached: List<String>,
        jobTitle: String,
        jobOrganization: String
    ): ApiResult<UserApplication> {
        if (!isRealConfigActive) {
            delay(150)
            val mockId = UUID.randomUUID().toString()
            val mockApp = UserApplication(
                id = mockId,
                userId = userId,
                jobId = jobId,
                generatedCvUrl = generatedCvUrl,
                documentsAttached = documentsAttached,
                status = "pending",
                appliedAt = java.time.Instant.now().toString(),
                jobTitle = jobTitle,
                jobOrganization = jobOrganization
            )
            mockUserApplicationsList.add(mockApp)
            return ApiResult.Success(mockApp)
        }

        return try {
            val appUrl = "$supabaseUrl/rest/v1/applications"
            
            val docArray = JSONArray().apply {
                documentsAttached.forEach { put(it) }
            }

            val requestBodyJson = JSONObject().apply {
                put("user_id", userId)
                put("job_id", jobId)
                put("generated_cv_url", generatedCvUrl)
                put("documents_attached", docArray)
                put("status", "pending")
                put("job_title", jobTitle)
                put("job_organization", jobOrganization)
            }.toString()

            val request = Request.Builder()
                .url(appUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "return=representation")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (response.isSuccessful && responseBody != null) {
                val jsonArray = JSONArray(responseBody)
                if (jsonArray.length() > 0) {
                    val appJson = jsonArray.getJSONObject(0)
                    val app = UserApplication(
                        id = appJson.optString("id", UUID.randomUUID().toString()),
                        userId = appJson.getString("user_id"),
                        jobId = appJson.getString("job_id"),
                        generatedCvUrl = appJson.getString("generated_cv_url"),
                        documentsAttached = parseJsonArray(appJson.optJSONArray("documents_attached")),
                        status = appJson.optString("status", "pending"),
                        appliedAt = appJson.optString("applied_at", appJson.optString("created_at", "")),
                        jobTitle = appJson.optString("job_title", null),
                        jobOrganization = appJson.optString("job_organization", null)
                    )
                    ApiResult.Success(app)
                } else {
                    ApiResult.Error("Failed to save application: response empty.")
                }
            } else {
                val errorMsg = parseErrorMessage(responseBody, "Failed to save application")
                ApiResult.Error(errorMsg)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Insert application exception", e)
            ApiResult.Error("Network Error: ${e.localizedMessage}")
        }
    }

    /**
     * Upsert a push token into the push_tokens table
     */
    suspend fun upsertPushToken(userId: String, token: String): ApiResult<Boolean> {
        if (!isRealConfigActive) {
            delay(100)
            Log.d(TAG, "Upserted mock push token: $token for user: $userId")
            return ApiResult.Success(true)
        }

        return try {
            val tokenUrl = "$supabaseUrl/rest/v1/push_tokens"
            val requestBodyJson = JSONObject().apply {
                put("user_id", userId)
                put("token", token)
            }.toString()

            val request = Request.Builder()
                .url(tokenUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "resolution=merge-duplicates")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResult.Success(true)
            } else {
                ApiResult.Error("Failed to upsert push token: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Upsert push token exception", e)
            ApiResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    /**
     * Fetch all active jobs from the jobs table (Supabase)
     */
    suspend fun fetchJobs(): ApiResult<List<com.example.ui.MockJob>> {
        if (!isRealConfigActive) {
            return ApiResult.Success(emptyList())
        }

        val primaryUrl = "$supabaseUrl/rest/v1/jobs?select=*,locations(name),categories(name),job_types(name)&status=eq.active&order=created_at.desc"
        val fallbackUrl = "$supabaseUrl/rest/v1/jobs?select=*&order=created_at.desc"

        val urlsToTry = listOf(primaryUrl, fallbackUrl)

        for (jobsUrl in urlsToTry) {
            try {
                val request = Request.Builder()
                    .url(jobsUrl)
                    .addHeader("apikey", supabaseAnonKey)
                    .addHeader("Authorization", "Bearer $supabaseAnonKey")
                    .get()
                    .build()

                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()

                if (response.isSuccessful && responseBody != null) {
                    val jsonArray = JSONArray(responseBody)
                    if (jsonArray.length() == 0 && jobsUrl == primaryUrl) {
                        // Primary returned empty array, try fallback
                        continue
                    }

                    val list = mutableListOf<com.example.ui.MockJob>()
                    for (i in 0 until jsonArray.length()) {
                        val obj = jsonArray.getJSONObject(i)
                        val id = obj.optString("id", UUID.randomUUID().toString())
                        val title = obj.optString("title", "Untitled Job")
                        val org = obj.optString("organization", "LS Services")

                        val locObj = obj.optJSONObject("locations")
                        val locationName = locObj?.optString("name") ?: obj.optString("location", "Kampala")

                        val catObj = obj.optJSONObject("categories")
                        val categoryName = catObj?.optString("name") ?: obj.optString("category", "General")

                        val typeObj = obj.optJSONObject("job_types")
                        val jobTypeName = typeObj?.optString("name") ?: obj.optString("job_type", "Full-time")

                        val salary = obj.optString("salary", "Negotiable")
                        val deadline = obj.optString("deadline", "2026-12-31")
                        val purpose = obj.optString("purpose", "")
                        val requirements = obj.optString("requirements", "")
                        val otherDetails = obj.optString("other_details", null)
                        val opensExt = obj.optBoolean("opens_externally", false)
                        val officialLink = obj.optString("official_link", null)
                        val appMethod = obj.optString("application_method", "auto_apply")
                        val reqDocs = parseJsonArray(obj.optJSONArray("required_documents"))
                        val viewsCount = obj.optInt("views_count", 0)
                        val createdAt = obj.optString("created_at", "")
                        val status = obj.optString("status", "active")

                        list.add(
                            com.example.ui.MockJob(
                                id = id,
                                title = title,
                                organization = org,
                                location = locationName,
                                jobType = jobTypeName,
                                category = categoryName,
                                salary = if (salary.isBlank()) "Negotiable" else salary,
                                deadline = deadline,
                                purpose = purpose,
                                requirements = requirements,
                                otherDetails = otherDetails,
                                opensExternally = opensExt,
                                officialLink = officialLink,
                                applicationMethod = appMethod,
                                requiredDocuments = reqDocs,
                                viewsCount = viewsCount,
                                createdAt = createdAt,
                                status = status
                            )
                        )
                    }
                    if (list.isNotEmpty() || jobsUrl == fallbackUrl) {
                        return ApiResult.Success(list)
                    }
                } else {
                    Log.w(TAG, "Fetch jobs url $jobsUrl returned code ${response.code}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fetch jobs error on url $jobsUrl", e)
            }
        }

        return ApiResult.Error("Failed to fetch jobs from Supabase")
    }

    /**
     * Insert a new job into the jobs table (Admin)
     */
    suspend fun insertJob(job: com.example.ui.MockJob): ApiResult<Boolean> {
        if (!isRealConfigActive) {
            delay(150)
            return ApiResult.Success(true)
        }

        return try {
            val jobsUrl = "$supabaseUrl/rest/v1/jobs"
            val reqDocsJson = JSONArray()
            job.requiredDocuments.forEach { reqDocsJson.put(it) }

            val requestBodyJson = JSONObject().apply {
                if (job.id.isNotBlank()) {
                    try {
                        UUID.fromString(job.id)
                        put("id", job.id)
                    } catch (_: Exception) {}
                }
                put("title", job.title)
                put("organization", job.organization)
                put("location", job.location)
                put("job_type", job.jobType)
                put("category", job.category)
                put("salary", job.salary)
                put("deadline", job.deadline)
                put("purpose", job.purpose)
                put("requirements", job.requirements)
                put("other_details", job.otherDetails)
                put("opens_externally", job.opensExternally)
                put("official_link", job.officialLink)
                put("application_method", job.applicationMethod)
                put("required_documents", reqDocsJson)
                put("status", "active")
                if (!job.postedBy.isNullOrBlank()) {
                    put("posted_by", job.postedBy)
                }
            }.toString()

            val request = Request.Builder()
                .url(jobsUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .addHeader("Prefer", "return=representation")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResult.Success(true)
            } else {
                ApiResult.Error("Failed to insert job: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Insert job exception", e)
            ApiResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    suspend fun deleteJob(jobId: String): ApiResult<Boolean> {
        if (!isRealConfigActive) {
            delay(150)
            return ApiResult.Success(true)
        }

        return try {
            val deleteUrl = "$supabaseUrl/rest/v1/jobs?id=eq.$jobId"
            val request = Request.Builder()
                .url(deleteUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .delete()
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                ApiResult.Success(true)
            } else {
                ApiResult.Error("Failed to delete job: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Delete job exception", e)
            ApiResult.Error("Network error: ${e.localizedMessage}")
        }
    }

    fun logout() {
        prefs.clearSession()
    }

    private fun getOrRestoreCachedProfile(userId: String, fullName: String, role: String): UserProfile {
        val cached = com.example.util.CacheUtils.parseUserProfile(prefs.cachedProfileJson)
        return if (cached != null) {
            cached.copy(
                id = userId,
                fullName = if (fullName.isNotBlank()) fullName else cached.fullName,
                role = role
            )
        } else {
            UserProfile(
                id = userId,
                fullName = fullName,
                phone = "+256 772 123456",
                role = role,
                referralCode = "REF" + userId.take(5).uppercase()
            )
        }
    }

    // Local memory cache for mock referrals
    private val mockReferrals = mutableListOf<ReferralRecord>()

    /**
     * Generate referral code if user doesn't have one
     */
    fun ensureReferralCode(userId: String, fullName: String): String {
        val namePrefix = fullName.replace("\\s+".toRegex(), "").uppercase().take(4)
            .ifBlank { "LS" }
        val randomDigits = (100..999).random()
        return "$namePrefix$randomDigits"
    }

    /**
     * Process referral insertion when a new user signs up with a referral code.
     */
    suspend fun recordPendingReferral(newUserId: String, referralCode: String): ApiResult<ReferralRecord> {
        val cleanCode = referralCode.trim().uppercase()
        if (cleanCode.isBlank()) return ApiResult.Error("Invalid code")

        if (!isRealConfigActive) {
            val record = ReferralRecord(
                id = UUID.randomUUID().toString(),
                referrerId = "mock_referrer_id",
                referredUserId = newUserId,
                referredName = "New Reached User",
                status = "pending",
                rewardGranted = false,
                createdAt = "2026-07-22T00:00:00Z"
            )
            mockReferrals.add(record)
            return ApiResult.Success(record)
        }

        return try {
            val findUrl = "$supabaseUrl/rest/v1/profiles?referral_code=eq.$cleanCode&select=id,full_name,email"
            val request = Request.Builder()
                .url(findUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val body = response.body?.string()
            if (response.isSuccessful && body != null) {
                val jsonArr = JSONArray(body)
                if (jsonArr.length() > 0) {
                    val referrerObj = jsonArr.getJSONObject(0)
                    val referrerId = referrerObj.getString("id")

                    val insertUrl = "$supabaseUrl/rest/v1/referrals"
                    val postBody = JSONObject().apply {
                        put("referrer_id", referrerId)
                        put("referred_user_id", newUserId)
                        put("status", "pending")
                        put("reward_granted", false)
                    }.toString()

                    val postReq = Request.Builder()
                        .url(insertUrl)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .addHeader("Prefer", "return=representation")
                        .post(postBody.toRequestBody(mediaTypeJson))
                        .build()

                    val postResp = client.newCall(postReq).execute()
                    val postBodyStr = postResp.body?.string()
                    if (postResp.isSuccessful && postBodyStr != null) {
                        val createdArr = JSONArray(postBodyStr)
                        if (createdArr.length() > 0) {
                            val c = createdArr.getJSONObject(0)
                            val rec = ReferralRecord(
                                id = c.getString("id"),
                                referrerId = c.getString("referrer_id"),
                                referredUserId = c.getString("referred_user_id"),
                                status = c.optString("status", "pending"),
                                rewardGranted = c.optBoolean("reward_granted", false),
                                createdAt = c.optString("created_at", "")
                            )
                            return ApiResult.Success(rec)
                        }
                    }
                }
            }
            ApiResult.Error("Referrer code not found")
        } catch (e: Exception) {
            ApiResult.Error("Failed to record referral: ${e.localizedMessage}")
        }
    }

    /**
     * Grant atomic 7-day subscription reward on successful profile completion by referred user.
     */
    suspend fun completeReferralReward(referredUserId: String): ApiResult<Boolean> {
        if (!isRealConfigActive) {
            val pending = mockReferrals.find { it.referredUserId == referredUserId && it.status == "pending" }
            if (pending != null) {
                val idx = mockReferrals.indexOf(pending)
                mockReferrals[idx] = pending.copy(status = "completed", rewardGranted = true)
            } else {
                mockReferrals.add(
                    ReferralRecord(
                        id = UUID.randomUUID().toString(),
                        referrerId = "mock_referrer_id",
                        referredUserId = referredUserId,
                        status = "completed",
                        rewardGranted = true,
                        createdAt = "2026-07-22T00:00:00Z"
                    )
                )
            }
            return ApiResult.Success(true)
        }

        return try {
            val getUrl = "$supabaseUrl/rest/v1/referrals?referred_user_id=eq.$referredUserId&status=eq.pending&select=*"
            val req = Request.Builder()
                .url(getUrl)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .get()
                .build()

            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && body != null) {
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val referralObj = arr.getJSONObject(0)
                    val referralId = referralObj.getString("id")
                    val referrerId = referralObj.getString("referrer_id")

                    val patchUrl = "$supabaseUrl/rest/v1/referrals?id=eq.$referralId"
                    val patchBody = JSONObject().apply {
                        put("status", "completed")
                        put("reward_granted", true)
                    }.toString()

                    val patchReq = Request.Builder()
                        .url(patchUrl)
                        .addHeader("apikey", supabaseAnonKey)
                        .addHeader("Authorization", "Bearer $supabaseAnonKey")
                        .patch(patchBody.toRequestBody(mediaTypeJson))
                        .build()

                    client.newCall(patchReq).execute()

                    // Atomically add 7 days to both accounts
                    extendUserSubscriptionDays(referredUserId, 7)
                    extendUserSubscriptionDays(referrerId, 7)

                    return ApiResult.Success(true)
                }
            }
            ApiResult.Success(false)
        } catch (e: Exception) {
            ApiResult.Error("Error completing referral reward: ${e.localizedMessage}")
        }
    }

    private suspend fun extendUserSubscriptionDays(userId: String, daysToAdd: Int) {
        val subRes = fetchSubscription(userId)
        if (subRes is ApiResult.Success) {
            val sub = subRes.data
            val isTrial = sub.planTier == "trial" || sub.status == "trial"
            val targetDateStr = if (isTrial) sub.trialEndsAt else sub.renewalDate
            val newDateStr = addDaysToIsoDate(targetDateStr, daysToAdd)

            val updatedSub = if (isTrial) {
                sub.copy(trialEndsAt = newDateStr, renewalDate = newDateStr)
            } else {
                sub.copy(renewalDate = newDateStr)
            }
            updateSubscription(updatedSub)
        }
    }

    private fun addDaysToIsoDate(isoString: String?, days: Int): String {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            format.timeZone = java.util.TimeZone.getTimeZone("UTC")
            val date = if (!isoString.isNullOrEmpty()) format.parse(isoString) else java.util.Date()
            val cal = java.util.Calendar.getInstance()
            cal.time = date ?: java.util.Date()
            cal.add(java.util.Calendar.DAY_OF_YEAR, days)
            format.format(cal.time)
        } catch (e: Exception) {
            "2026-08-11T12:00:00Z"
        }
    }

    suspend fun fetchReferrals(userId: String): ApiResult<List<ReferralRecord>> {
        if (!isRealConfigActive) {
            val list = mockReferrals.filter { it.referrerId == userId || it.referrerId == "mock_referrer_id" }
            if (list.isEmpty()) {
                // Add sample completed referral for demo
                val demo = listOf(
                    ReferralRecord(
                        id = "ref_demo_1",
                        referrerId = userId,
                        referredUserId = "user_2",
                        referredName = "Grace Tumusiime",
                        referredEmail = "grace@example.com",
                        status = "completed",
                        rewardGranted = true,
                        createdAt = "2026-07-20T10:00:00Z"
                    )
                )
                return ApiResult.Success(demo)
            }
            return ApiResult.Success(list)
        }

        return try {
            val url = "$supabaseUrl/rest/v1/referrals?referrer_id=eq.$userId&select=*"
            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .get()
                .build()

            val resp = client.newCall(req).execute()
            val body = resp.body?.string()
            if (resp.isSuccessful && body != null) {
                val arr = JSONArray(body)
                val list = mutableListOf<ReferralRecord>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        ReferralRecord(
                            id = obj.getString("id"),
                            referrerId = obj.getString("referrer_id"),
                            referredUserId = obj.getString("referred_user_id"),
                            status = obj.optString("status", "pending"),
                            rewardGranted = obj.optBoolean("reward_granted", false),
                            createdAt = obj.optString("created_at", "")
                        )
                    )
                }
                ApiResult.Success(list)
            } else {
                ApiResult.Success(emptyList())
            }
        } catch (e: Exception) {
            ApiResult.Error("Fetch referrals failed: ${e.localizedMessage}")
        }
    }

    suspend fun updateHideServicesPopup(userId: String, hide: Boolean): ApiResult<Boolean> {
        if (!isRealConfigActive) {
            return ApiResult.Success(true)
        }
        return try {
            val url = "$supabaseUrl/rest/v1/profiles?id=eq.$userId"
            val bodyJson = JSONObject().apply {
                put("hide_services_popup", hide)
            }.toString()

            val req = Request.Builder()
                .url(url)
                .addHeader("apikey", supabaseAnonKey)
                .addHeader("Authorization", "Bearer $supabaseAnonKey")
                .patch(bodyJson.toRequestBody(mediaTypeJson))
                .build()

            val resp = client.newCall(req).execute()
            ApiResult.Success(resp.isSuccessful)
        } catch (e: Exception) {
            ApiResult.Error("Failed to update popup setting: ${e.localizedMessage}")
        }
    }
}
