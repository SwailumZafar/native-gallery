package com.example.nativegallery.ui

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class GalleryNavigationSmokeTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(InitialPermissionPromptBypassRule())
        .around(composeRule)

    @Test
    fun primaryTabsRenderAndNavigate() {
        composeRule.waitForText("Search photos and videos")
        composeRule.onNodeWithText("Search photos and videos").assertIsDisplayed()

        composeRule.onNodeWithText("Albums").performClick()
        composeRule.waitForText("Search albums")
        composeRule.onNodeWithText("Search albums").assertIsDisplayed()

        composeRule.onNodeWithText("Photos").performClick()
        composeRule.waitForText("Search photos and videos")
        composeRule.onNodeWithText("Search photos and videos").assertIsDisplayed()
    }

    @Test
    fun eachBottomNavigationDestinationRespondsToOneTap() {
        composeRule.waitForText("Search photos and videos")

        composeRule.onNodeWithText("Menu").performClick()
        composeRule.waitForText("Gallery settings and tools")
        composeRule.onNodeWithText("Gallery settings and tools").assertIsDisplayed()

        composeRule.onNodeWithText("Albums").performClick()
        composeRule.waitForText("Search albums")
        composeRule.onNodeWithText("Search albums").assertIsDisplayed()

        composeRule.onNodeWithText("Photos").performClick()
        composeRule.waitForText("Search photos and videos")
        composeRule.onNodeWithText("Search photos and videos").assertIsDisplayed()
    }

    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.waitForText(text: String) {
        waitUntil(timeoutMillis = NavigationTimeoutMillis) {
            onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private class InitialPermissionPromptBypassRule : TestRule {
        override fun apply(base: Statement, description: Description): Statement {
            return object : Statement() {
                override fun evaluate() {
                    val context = InstrumentationRegistry.getInstrumentation().targetContext
                    val preferences = context.getSharedPreferences(
                        PermissionPromptPreferences,
                        Context.MODE_PRIVATE
                    )
                    val wasHandled = preferences.getBoolean(InitialPromptHandled, false)
                    preferences.edit().putBoolean(InitialPromptHandled, true).commit()
                    try {
                        base.evaluate()
                    } finally {
                        preferences.edit().putBoolean(InitialPromptHandled, wasHandled).commit()
                    }
                }
            }
        }
    }

    private companion object {
        const val NavigationTimeoutMillis = 15_000L
        const val PermissionPromptPreferences = "native_gallery_permission_prompt"
        const val InitialPromptHandled = "initial_prompt_handled"
    }
}
