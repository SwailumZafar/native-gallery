package com.example.nativegallery.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.nativegallery.data.VaultCredentialKind
import com.example.nativegallery.ui.theme.GalleryTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VaultPatternGestureTest {
    @get:Rule val rule = createComposeRule()
    @Test fun drawingStartsOnTouchDownAndWinsOverParentScrolling() {
        var value by mutableStateOf("")
        rule.setContent {
            GalleryTheme(darkTheme = true) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    VaultCredentialField(VaultCredentialKind.Pattern, value, { value = it }, "Test pattern")
                    Spacer(Modifier.height(1000.dp))
                }
            }
        }
        rule.onNodeWithTag("vault-pattern-Test pattern").performTouchInput {
            down(Offset(width / 6f, height / 6f))
            moveTo(Offset(width * 5 / 6f, height / 6f), 180)
            moveTo(Offset(width * 5 / 6f, height * 5 / 6f), 180)
            up()
        }
        rule.runOnIdle { assertEquals("12369", value) }
        rule.onNodeWithText("Clear pattern").performClick()
        rule.onNodeWithContentDescription("Pattern dot 1").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.onNodeWithContentDescription("Pattern dot 3").performSemanticsAction(SemanticsActions.OnClick) { it() }
        rule.runOnIdle { assertEquals("123", value) }
    }
}
