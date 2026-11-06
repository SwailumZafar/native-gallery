package com.example.nativegallery.ui

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.nativegallery.data.VaultCredentialKind
import com.example.nativegallery.ui.theme.GalleryTheme
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Mock UI only: never reads or changes the user's vault credentials or media. */
class VaultUiFlowTest {
    @get:Rule val rule = createComposeRule()
    @Before fun keepAwake() = TestScreenAwake.enable()

    @Test fun unlockShowsOnlyTheSavedMethodWithoutRecoveryQuestions() {
        showAuth()
        rule.onNodeWithTag("vault-pattern-Pattern").assertIsDisplayed()
        rule.onNodeWithText("PIN").assertDoesNotExist()
        rule.onNodeWithText("Password").assertDoesNotExist()
        rule.onNodeWithText("Recovery phrase").assertDoesNotExist()
        saveRoot("vault-unlock-pattern-0943.png")
    }

    @Test fun initialPinSetupUsesOneFieldAtATimeAndRequiresConfirmation() {
        var created: Pair<String, String>? = null
        showAuth(hasPin = false, onCreate = { first, second -> created = first to second })
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        rule.onNode(hasSetTextAction()).performTextInput("123456")
        rule.onNodeWithText("Continue").performClick()
        assertNull(created)
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(1)
        rule.onNodeWithText("New PIN").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextInput("123456")
        rule.onNodeWithText("Create PIN").performClick()
        rule.runOnIdle { assertEquals("123456" to "123456", created) }
    }

    @Test fun recoveryDoesNotShowTwoUnlockMethodsAtOnce() {
        showAuth(recoveryRequired = true)
        rule.onNodeWithTag("vault-pattern-Pattern").assertDoesNotExist()
        rule.onNode(hasSetTextAction()).performTextInput("a saved recovery phrase")
        rule.onNodeWithText("Continue").performClick()
        rule.onNodeWithTag("vault-pattern-Pattern").assertIsDisplayed()
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
    }

    @Test fun settingsKeepRecoverySeparateAndConfirmPatternsOnTheNextPage() {
        var state by mutableStateOf(LockedMediaSecurityUiState(hasPin = true, isUnlocked = true))
        var saved: Triple<VaultCredentialKind, String, String>? = null
        rule.setContent {
            GalleryTheme(darkTheme = true) {
                VaultSettingsSheet(state, { kind, first, second ->
                    saved = Triple(kind, first, second)
                    state = state.copy(credentialKind = kind, settingsSavedEventId = state.settingsSavedEventId + 1)
                }, { _, _, _ -> }, { _, _ -> }, {}, {}, {})
            }
        }
        rule.onAllNodes(hasSetTextAction()).assertCountEquals(0)
        saveSheet("vault-settings-home-0943.png")
        rule.onNodeWithText("Unlock method").performClick()
        rule.onNodeWithText("Pattern").performClick()
        rule.onNodeWithTag("vault-pattern-New Pattern").assertIsDisplayed()
        rule.onNodeWithTag("vault-pattern-Confirm Pattern").assertDoesNotExist()
        rule.onNodeWithText("Recovery phrase").assertDoesNotExist()
        saveSheet("vault-settings-pattern-0943.png")
        drawPattern()
        rule.onNodeWithText("Continue").performScrollTo().performClick()
        rule.onNodeWithTag("vault-pattern-New Pattern").assertDoesNotExist()
        rule.onNodeWithTag("vault-pattern-Confirm Pattern").assertIsDisplayed()
        drawPattern()
        rule.onNodeWithText("Save").performScrollTo().performClick()
        rule.runOnIdle { assertEquals(Triple(VaultCredentialKind.Pattern, "1236", "1236"), saved) }
        rule.onNodeWithText("Vault settings").assertIsDisplayed()
    }

    private fun drawPattern() {
        listOf(1, 2, 3, 6).forEach { dot ->
            rule.onNodeWithContentDescription("Pattern dot $dot").performSemanticsAction(SemanticsActions.OnClick) { it() }
        }
    }

    private fun showAuth(hasPin: Boolean = true, recoveryRequired: Boolean = false,
                         onCreate: (String, String) -> Unit = { _, _ -> }) {
        rule.setContent {
            GalleryTheme(darkTheme = true) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    LockedMediaAuthCard(hasPin, false, null, false, onCreate, {}, {}, VaultCredentialKind.Pattern,
                        recoveryRequired, true, "This question must stay out of ordinary unlock", 0L, { _, _ -> }, {}, false)
                }
            }
        }
    }

    private fun saveSheet(name: String) = save(name, rule.onNodeWithTag("vault-settings-sheet").captureToImage().asAndroidBitmap())
    private fun saveRoot(name: String) = save(name, rule.onRoot().captureToImage().asAndroidBitmap())
    private fun save(name: String, bitmap: Bitmap) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.getExternalFilesDir(null), name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
