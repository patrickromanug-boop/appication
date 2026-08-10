package com.example.util

import com.example.data.supabase.UserApplication
import com.example.data.supabase.UserProfile
import com.example.ui.MockJob
import org.json.JSONArray
import org.json.JSONObject

object CacheUtils {

    fun MockJob.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("organization", organization)
            put("location", location)
            put("jobType", jobType)
            put("category", category)
            put("salary", salary)
            put("deadline", deadline)
            put("purpose", purpose)
            put("requirements", requirements)
            put("otherDetails", otherDetails)
            put("opensExternally", opensExternally)
            put("officialLink", officialLink)
            put("applicationMethod", applicationMethod)
            put("requiredDocuments", JSONArray(requiredDocuments))
            put("postedBy", postedBy ?: JSONObject.NULL)
            put("viewsCount", viewsCount)
            put("status", status)
            put("createdAt", createdAt)
        }
    }

    fun JSONObject.toMockJob(): MockJob {
        val reqDocsArr = optJSONArray("requiredDocuments")
        val reqDocsList = mutableListOf<String>()
        if (reqDocsArr != null) {
            for (i in 0 until reqDocsArr.length()) {
                reqDocsList.add(reqDocsArr.getString(i))
            }
        }
        return MockJob(
            id = getString("id"),
            title = getString("title"),
            organization = getString("organization"),
            location = getString("location"),
            jobType = getString("jobType"),
            category = getString("category"),
            salary = optString("salary", "Negotiable"),
            deadline = optString("deadline", "2026-08-15"),
            purpose = optString("purpose", ""),
            requirements = optString("requirements", ""),
            otherDetails = optString("otherDetails", ""),
            opensExternally = optBoolean("opensExternally", false),
            officialLink = optString("officialLink", "https://lsrecruitingservices.com"),
            applicationMethod = optString("applicationMethod", "auto_apply"),
            requiredDocuments = reqDocsList,
            postedBy = if (has("postedBy") && !isNull("postedBy")) getString("postedBy") else null,
            viewsCount = optInt("viewsCount", 10),
            status = optString("status", "active"),
            createdAt = optString("createdAt", "2026-07-21T00:00:00Z")
        )
    }

    @JvmName("jobListToJsonString")
    fun List<MockJob>.toJsonString(): String {
        val jsonArray = JSONArray()
        forEach { job ->
            jsonArray.put(job.toJson())
        }
        return jsonArray.toString()
    }

    fun parseMockJobList(jsonString: String?): List<MockJob> {
        if (jsonString.isNullOrBlank()) return emptyList()
        val list = mutableListOf<MockJob>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(obj.toMockJob())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun UserProfile.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("fullName", fullName)
            put("phone", phone ?: JSONObject.NULL)
            put("role", role)
            put("education", education)
            put("skills", JSONArray(skills))
            put("experience", experience)
            put("preferredCategories", JSONArray(preferredCategories))
            put("preferredLocations", JSONArray(preferredLocations))
            put("themePreference", themePreference)
            put("hideServicesPopup", hideServicesPopup)
            put("referralCode", referralCode ?: JSONObject.NULL)
            put("referredBy", referredBy ?: JSONObject.NULL)
            put("notifyAllJobs", notifyAllJobs)
            put("notifyMatchingPreferences", notifyMatchingPreferences)
        }
    }

    fun JSONObject.toUserProfile(): UserProfile {
        fun parseStringList(key: String): List<String> {
            val arr = optJSONArray(key) ?: return emptyList()
            val res = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                res.add(arr.getString(i))
            }
            return res
        }

        return UserProfile(
            id = getString("id"),
            fullName = getString("fullName"),
            phone = if (has("phone") && !isNull("phone")) getString("phone") else null,
            role = optString("role", "user"),
            education = optString("education", "[]"),
            skills = parseStringList("skills"),
            experience = optString("experience", "[]"),
            preferredCategories = parseStringList("preferredCategories"),
            preferredLocations = parseStringList("preferredLocations"),
            themePreference = optString("themePreference", "system"),
            hideServicesPopup = optBoolean("hideServicesPopup", false),
            referralCode = if (has("referralCode") && !isNull("referralCode")) getString("referralCode") else null,
            referredBy = if (has("referredBy") && !isNull("referredBy")) getString("referredBy") else null,
            notifyAllJobs = optBoolean("notifyAllJobs", true),
            notifyMatchingPreferences = optBoolean("notifyMatchingPreferences", false)
        )
    }

    fun UserProfile.toJsonString(): String = toJson().toString()

    fun parseUserProfile(jsonString: String?): UserProfile? {
        if (jsonString.isNullOrBlank()) return null
        return try {
            JSONObject(jsonString).toUserProfile()
        } catch (e: Exception) {
            null
        }
    }

    fun UserApplication.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("userId", userId)
            put("jobId", jobId)
            put("generatedCvUrl", generatedCvUrl)
            put("documentsAttached", JSONArray(documentsAttached))
            put("status", status)
            put("appliedAt", appliedAt)
            put("jobTitle", jobTitle ?: JSONObject.NULL)
            put("jobOrganization", jobOrganization ?: JSONObject.NULL)
            put("candidateName", candidateName ?: JSONObject.NULL)
            put("candidateEmail", candidateEmail ?: JSONObject.NULL)
            put("candidatePhone", candidatePhone ?: JSONObject.NULL)
            put("adminNotes", adminNotes ?: JSONObject.NULL)
        }
    }

    fun JSONObject.toUserApplication(): UserApplication {
        val docsArr = optJSONArray("documentsAttached")
        val docsList = mutableListOf<String>()
        if (docsArr != null) {
            for (i in 0 until docsArr.length()) {
                docsList.add(docsArr.getString(i))
            }
        }
        return UserApplication(
            id = getString("id"),
            userId = optString("userId", "user_001"),
            jobId = getString("jobId"),
            generatedCvUrl = optString("generatedCvUrl", ""),
            documentsAttached = docsList,
            status = optString("status", "pending"),
            appliedAt = optString("appliedAt", "2026-07-21T00:00:00Z"),
            jobTitle = if (has("jobTitle") && !isNull("jobTitle")) getString("jobTitle") else null,
            jobOrganization = if (has("jobOrganization") && !isNull("jobOrganization")) getString("jobOrganization") else null,
            candidateName = if (has("candidateName") && !isNull("candidateName")) getString("candidateName") else null,
            candidateEmail = if (has("candidateEmail") && !isNull("candidateEmail")) getString("candidateEmail") else null,
            candidatePhone = if (has("candidatePhone") && !isNull("candidatePhone")) getString("candidatePhone") else null,
            adminNotes = if (has("adminNotes") && !isNull("adminNotes")) getString("adminNotes") else null
        )
    }

    @JvmName("userApplicationListToJsonString")
    fun List<UserApplication>.toJsonString(): String {
        val array = JSONArray()
        forEach { app -> array.put(app.toJson()) }
        return array.toString()
    }

    fun parseUserApplicationList(jsonString: String?): List<UserApplication> {
        if (jsonString.isNullOrBlank()) return emptyList()
        val list = mutableListOf<UserApplication>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                list.add(array.getJSONObject(i).toUserApplication())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    fun formatRelativeTime(timestampMs: Long): String {
        if (timestampMs <= 0L) return "a while ago"
        val diff = System.currentTimeMillis() - timestampMs
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 30 -> "just now"
            minutes < 1 -> "less than 1m ago"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }
}
