package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.ui.AppViewModel
import com.example.ui.MainAppShell
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private lateinit var viewModel: AppViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        
        viewModel = ViewModelProvider(this)[AppViewModel::class.java]
        handleIntent(intent)

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()

            val darkTheme = when (themeMode) {
                "light" -> false
                "dark" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize()
                ) {
                    MainAppShell(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val jobId = intent.getStringExtra("job_id")
        if (!jobId.isNullOrEmpty()) {
            viewModel.handleNotificationTap(jobId)
        }
        val openTab = intent.getStringExtra("open_tab")
        if (!openTab.isNullOrEmpty()) {
            viewModel.currentTab = openTab
        }

        // Handle referral deep link data (e.g. lsjobs://referral?code=PATRICK482 or https://lsrecruitingservices.com/ref?code=PATRICK482)
        val uri = intent.data
        if (uri != null) {
            val code = uri.getQueryParameter("code") ?: uri.getQueryParameter("ref")
            if (!code.isNullOrBlank()) {
                viewModel.handleReferralCodeReceived(code)
            }
        }
    }
}
