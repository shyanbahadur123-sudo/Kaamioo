package com.kaamio.nepal.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.kaamio.nepal.ErrorScreenView
import com.kaamio.nepal.LoadingScreenView
import com.kaamio.nepal.R
import com.kaamio.nepal.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxxhdpi")
class ScreenshotTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureLoadingScreen() {
        composeTestRule.setContent {
            MyApplicationTheme {
                LoadingScreenView()
            }
        }
        composeTestRule.onRoot().captureRoboImage("loading_screen.png")
    }

    @Test
    fun captureErrorScreen() {
        composeTestRule.setContent {
            MyApplicationTheme {
                ErrorScreenView(message = "Network unavailable", onRetry = {})
            }
        }
        composeTestRule.onRoot().captureRoboImage("error_screen.png")
    }

    @Test
    @Ignore("SignInScreen calls hiltViewModel() internally; requires a Hilt test harness instead of a plain Robolectric rule")
    fun captureSignInScreen() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SignInScreen(
                    email = "",
                    onEmailChange = {},
                    pass = "",
                    onPassChange = {},
                    onSignIn = {},
                    onSignUpInstead = {},
                    isLoading = false,
                    error = null,
                    onForgotPassword = {},
                    success = null
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage("sign_in_screen.png")
    }
}
