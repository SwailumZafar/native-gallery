package com.example.nativegallery.ui

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.junit.runners.model.Statement

@RunWith(AndroidJUnit4::class)
class ThreeButtonNavigationInsetTest {

    private val composeRule = createAndroidComposeRule<MainActivity>()

    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(InitialPermissionPromptBypassRule())
        .around(composeRule)

    @Test
    fun bottomNavigationClearsThreeButtonSystemNavigation() {
        val activity = composeRule.activity
        val navigationMode = Settings.Secure.getInt(
            activity.contentResolver,
            "navigation_mode",
            UnknownNavigationMode
        )
        assumeTrue("This assertion only applies in three-button navigation mode", navigationMode == ThreeButtonMode)
        composeRule.waitUntil(NavigationTimeoutMillis) {
            composeRule
                .onAllNodesWithText("Search photos and videos")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val navigationInset = WindowInsetsCompat
            .toWindowInsetsCompat(activity.window.decorView.rootWindowInsets)
            .getInsets(WindowInsetsCompat.Type.navigationBars())
            .bottom
        assumeTrue("The configured device did not report a bottom navigation inset", navigationInset > 0)

        val rootBottom = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        val photosNavigationNode = composeRule
            .onAllNodes(hasText("Photos") and hasClickAction())
            .fetchSemanticsNodes()
            .single()
        val clearance = rootBottom - photosNavigationNode.boundsInRoot.bottom

        assertTrue(
            "Bottom navigation must remain above the three-button system navigation area",
            clearance >= navigationInset
        )
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
        const val ThreeButtonMode = 0
        const val UnknownNavigationMode = -1
    }
}
