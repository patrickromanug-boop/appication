package com.example

import com.example.data.supabase.UserProfile
import com.example.ui.AppViewModel
import com.example.ui.MockJob
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationTargetingTest {

    private val sampleJobEngineering = MockJob(
        id = "job_1",
        title = "Senior Civil Engineer",
        organization = "LS Construction Ltd",
        location = "Kampala, Uganda",
        jobType = "Full Time",
        category = "Engineering",
        purpose = "Oversee infrastructure projects",
        requirements = "BSc Civil Engineering, 5+ years experience"
    )

    private val sampleJobHealth = MockJob(
        id = "job_2",
        title = "Clinical Nurse Specialist",
        organization = "St Mary Hospital",
        location = "Gulu, Uganda",
        jobType = "Full Time",
        category = "Healthcare & Medical",
        purpose = "Provide patient care and nursing oversight",
        requirements = "Diploma in Nursing, registered nurse license"
    )

    @Test
    fun testMatchingByCategory() {
        val userProfile = UserProfile(
            id = "user_101",
            fullName = "John Builder",
            phone = "+256700000000",
            preferredCategories = listOf("Engineering"),
            preferredLocations = listOf("Kampala")
        )

        val matchEngineering = AppViewModel.evaluateJobMatchStatic(sampleJobEngineering, userProfile)
        assertTrue("Job in Engineering should match preferred category Engineering", matchEngineering.isMatch)
        assertTrue(matchEngineering.matchedReason?.contains("Engineering") == true)

        val matchHealth = AppViewModel.evaluateJobMatchStatic(sampleJobHealth, userProfile)
        assertFalse("Job in Healthcare should NOT match preferred category Engineering when location doesn't match", matchHealth.isMatch)
    }

    @Test
    fun testMatchingByLocation() {
        val userProfile = UserProfile(
            id = "user_102",
            fullName = "Grace Medical",
            phone = "+256711111111",
            preferredCategories = listOf("ICT"),
            preferredLocations = listOf("Gulu")
        )

        val matchHealth = AppViewModel.evaluateJobMatchStatic(sampleJobHealth, userProfile)
        assertTrue("Job in Gulu should match preferred location Gulu", matchHealth.isMatch)
        assertTrue(matchHealth.matchedReason?.contains("Gulu") == true)
    }

    @Test
    fun testMatchingBySkillKeyword() {
        val userProfile = UserProfile(
            id = "user_103",
            fullName = "Alex Specialist",
            phone = "+256722222222",
            skills = listOf("Nursing", "Autocad")
        )

        val matchHealth = AppViewModel.evaluateJobMatchStatic(sampleJobHealth, userProfile)
        assertTrue("Job with Nursing requirement should match skill Nursing", matchHealth.isMatch)
        assertEquals("Keyword 'Nursing'", matchHealth.matchedReason)
    }
}
