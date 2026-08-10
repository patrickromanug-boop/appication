package com.example.data.preference

import android.content.Context
import android.content.SharedPreferences

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ls_services_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "theme_pref" // "light", "dark", "system"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_ROLE = "user_role" // "user", "admin"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_TOKEN = "user_token"
        private const val KEY_GUEST_BOOKMARKS = "guest_bookmarks"
        private const val KEY_REPORTED_JOBS = "reported_jobs"
        private const val KEY_APPLIED_JOBS = "applied_jobs"
    }

    var themePreference: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var userId: String?
        get() = prefs.getString(KEY_USER_ID, null)
        set(value) = prefs.edit().putString(KEY_USER_ID, value).apply()

    var userEmail: String?
        get() = prefs.getString(KEY_USER_EMAIL, null)
        set(value) = prefs.edit().putString(KEY_USER_EMAIL, value).apply()

    var userName: String?
        get() = prefs.getString(KEY_USER_NAME, null)
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userRole: String
        get() = prefs.getString(KEY_USER_ROLE, "user") ?: "user"
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_LOGGED_IN, value).apply()

    var userToken: String?
        get() = prefs.getString(KEY_USER_TOKEN, null)
        set(value) = prefs.edit().putString(KEY_USER_TOKEN, value).apply()

    var guestBookmarks: Set<String>
        get() = prefs.getStringSet(KEY_GUEST_BOOKMARKS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_GUEST_BOOKMARKS, HashSet(value)).apply()

    var reportedJobs: Set<String>
        get() = prefs.getStringSet(KEY_REPORTED_JOBS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_REPORTED_JOBS, HashSet(value)).apply()

    var appliedJobs: Set<String>
        get() = prefs.getStringSet(KEY_APPLIED_JOBS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_APPLIED_JOBS, HashSet(value)).apply()

    var pushToken: String?
        get() {
            var token = prefs.getString("push_token", null)
            if (token == null) {
                token = "android_token_" + java.util.UUID.randomUUID().toString().take(12)
                prefs.edit().putString("push_token", token).apply()
            }
            return token
        }
        set(value) = prefs.edit().putString("push_token", value).apply()

    var hasShownNotifPermissionExplanation: Boolean
        get() = prefs.getBoolean("has_shown_notif_permission_explanation", false)
        set(value) = prefs.edit().putBoolean("has_shown_notif_permission_explanation", value).apply()

    var hasShownTrialEndBanner: Boolean
        get() = prefs.getBoolean("has_shown_trial_end_banner", false)
        set(value) = prefs.edit().putBoolean("has_shown_trial_end_banner", value).apply()

    var hasShownCvOnboarding: Boolean
        get() = prefs.getBoolean("has_shown_cv_onboarding", false)
        set(value) = prefs.edit().putBoolean("has_shown_cv_onboarding", value).apply()

    var servicesPopupDismissedUntil: Long
        get() = prefs.getLong("services_popup_dismissed_until", 0L)
        set(value) = prefs.edit().putLong("services_popup_dismissed_until", value).apply()

    var pendingReferralCode: String?
        get() = prefs.getString("pending_referral_code", null)
        set(value) = prefs.edit().putString("pending_referral_code", value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean("has_completed_onboarding", false)
        set(value) = prefs.edit().putBoolean("has_completed_onboarding", value).apply()

    var deletedJobIds: Set<String>
        get() = prefs.getStringSet("deleted_job_ids", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("deleted_job_ids", HashSet(value)).apply()

    var customUploadedJobsJson: String?
        get() = prefs.getString("custom_uploaded_jobs_json", null)
        set(value) = prefs.edit().putString("custom_uploaded_jobs_json", value).apply()

    var cachedJobsJson: String?
        get() = prefs.getString("cached_jobs_json", null)
        set(value) = prefs.edit().putString("cached_jobs_json", value).apply()

    var cachedJobsTimestamp: Long
        get() = prefs.getLong("cached_jobs_timestamp", 0L)
        set(value) = prefs.edit().putLong("cached_jobs_timestamp", value).apply()

    var cachedProfileJson: String?
        get() = prefs.getString("cached_profile_json", null)
        set(value) = prefs.edit().putString("cached_profile_json", value).apply()

    var cachedProfileTimestamp: Long
        get() = prefs.getLong("cached_profile_timestamp", 0L)
        set(value) = prefs.edit().putLong("cached_profile_timestamp", value).apply()

    var cachedApplicationsJson: String?
        get() = prefs.getString("cached_applications_json", null)
        set(value) = prefs.edit().putString("cached_applications_json", value).apply()

    var cachedApplicationsTimestamp: Long
        get() = prefs.getLong("cached_applications_timestamp", 0L)
        set(value) = prefs.edit().putLong("cached_applications_timestamp", value).apply()

    var notifiedJobIds: Set<String>
        get() = prefs.getStringSet("notified_job_ids_all_v2", emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet("notified_job_ids_all_v2", value).apply()

    var hasCompletedInitialJobSync: Boolean
        get() = prefs.getBoolean("has_completed_initial_job_sync_all_v2", false)
        set(value) = prefs.edit().putBoolean("has_completed_initial_job_sync_all_v2", value).apply()

    fun clearSession() {
        prefs.edit()
            .remove(KEY_USER_ID)
            .remove(KEY_USER_EMAIL)
            .remove(KEY_USER_NAME)
            .remove(KEY_USER_ROLE)
            .remove(KEY_USER_TOKEN)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .apply()
    }
}
