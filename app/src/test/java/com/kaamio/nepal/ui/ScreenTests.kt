package com.kaamio.nepal.ui

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kaamio.nepal.ErrorScreenView
import com.kaamio.nepal.LoadingScreenView
import com.kaamio.nepal.ui.theme.MyApplicationTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun errorScreenView_displaysTitleAndMessage() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ErrorScreenView(message = "Network unavailable", onRetry = {})
            }
        }
        composeTestRule.onNodeWithText("Something went wrong").assertIsDisplayed()
        composeTestRule.onNodeWithText("Network unavailable").assertIsDisplayed()
    }

    @Test
    fun errorScreenView_retryButton_displaysAndIsClickable() {
        var retryCalled = false
        composeTestRule.setContent {
            MyApplicationTheme {
                ErrorScreenView(message = "Test error", onRetry = { retryCalled = true })
            }
        }
        composeTestRule.onNodeWithText("Retry", ignoreCase = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry", ignoreCase = true).assertHasClickAction()
        composeTestRule.onNodeWithText("Retry", ignoreCase = true).performClick()
        assert(retryCalled) { "Retry callback was not invoked" }
    }

    @Test
    fun loadingScreenView_displaysLoadingIndicator() {
        composeTestRule.setContent {
            MyApplicationTheme {
                LoadingScreenView()
            }
        }
        composeTestRule.onNodeWithText("Loading\u2026").assertIsDisplayed()
    }
}
