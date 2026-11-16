package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun appNameMatchesHexForge() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("HEXFORGE", appName)
    }

    @Test
    fun onboardingAcceptAndGatekeeperRefusalFlow() {
        composeRule.waitForIdle()
        // Legal notice gate must appear on first launch with pinned CTA
        composeRule.onNodeWithTag("accept_legal_button").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("targets_lazy_column").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()

        // Scroll to the online blocked target in Targets LazyColumn and verify Gatekeeper refusal
        composeRule.onNodeWithTag("targets_lazy_column")
            .performScrollToNode(hasTestTag("verify_target_com.tencent.ig"))
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("verify_target_com.tencent.ig").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag("gatekeeper_feedback_text").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("gatekeeper_feedback_text").assertIsDisplayed()

        // Open Chronicle bottom sheet from Tier Status Strip
        composeRule.onNodeWithTag("open_chronicle_strip_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("close_chronicle_button").assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        // Switch to Oracle tab and toggle Persian RTL
        composeRule.onNodeWithTag("nav_oracle").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("settings_lang_fa").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("اوراکل و تنظیمات فورج").assertIsDisplayed()
    }
}
