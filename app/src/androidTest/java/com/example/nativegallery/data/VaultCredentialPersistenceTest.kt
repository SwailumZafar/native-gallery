package com.example.nativegallery.data

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import androidx.lifecycle.viewModelScope
import com.example.nativegallery.ui.LockedMediaSecurityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Isolated preferences: never changes the installed user's vault credentials or files. */
@RunWith(AndroidJUnit4::class)
class VaultCredentialPersistenceTest {
    @Test fun credentialsDecoyAndEscalatingFailuresSurviveRepositoryRecreation() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val prefix = "vault-credential-fixture-${UUID.randomUUID()}-"
        val isolated = object : ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences = base.getSharedPreferences(prefix + name, mode)
        }
        try {
            val repository = HiddenSecurityRepository(isolated)
            assertTrue(repository.setPin("123456"))
            assertTrue(repository.setRecovery("My stored recovery phrase", "private recovery phrase 824"))
            assertTrue(repository.setDecoyPin("654321"))
            repository.addDecoyMedia(setOf("decoy-photo"))
            repository.rememberVaultView(true)
            assertTrue(HiddenSecurityRepository(isolated).lastVaultWasDecoy())
            assertFalse(repository.setCredential(VaultCredentialKind.Pin, "654321"))
            assertTrue(repository.setCredential(VaultCredentialKind.Password, "a longer password 824"))
            val reopened = HiddenSecurityRepository(isolated)
            assertEquals(VaultCredentialKind.Password, reopened.credentialKind())
            assertTrue(reopened.verifyPin("a longer password 824"))
            assertTrue(reopened.verifyDecoyPin("654321"))
            assertTrue(reopened.verifyRecovery("private recovery phrase 824"))
            assertEquals(setOf("decoy-photo"), reopened.decoyMediaIds())
            var now = 1_000L
            repeat(30) {
                val failure = HiddenSecurityRepository(isolated).recordFailedPinAttempt(now)
                now = maxOf(now, failure.lockoutUntilMillis)
            }
            assertTrue(HiddenSecurityRepository(isolated).requiresRecovery())
            reopened.clearFailedPinAttempts()
            assertFalse(HiddenSecurityRepository(isolated).requiresRecovery())
            assertTrue(reopened.setCredential(VaultCredentialKind.Pattern, "12369"))
            assertTrue(HiddenSecurityRepository(isolated).verifyPin("12369"))
            reopened.clearPin()
            assertFalse(reopened.hasRecovery())
            assertFalse(reopened.hasDecoyPin())
            assertFalse(reopened.lastVaultWasDecoy())
            assertTrue(reopened.setPin("123456"))
            runBlocking {
                val model = withContext(Dispatchers.Main) { LockedMediaSecurityViewModel(reopened) }
                try {
                    withContext(Dispatchers.Main) { model.recoverWithDeviceCredential() }
                    assertFalse("Phone verification alone must not unlock a legacy vault", model.uiState.value.isUnlocked)
                    withContext(Dispatchers.Main) { model.recover("", "123456") }
                    withTimeout(5_000) { while (!model.uiState.value.isUnlocked) delay(10) }
                } finally { withContext(Dispatchers.Main) { model.viewModelScope.cancel() } }
            }
        } finally {
            base.getSharedPreferences(prefix + "native_gallery_hidden_security", Context.MODE_PRIVATE).edit().clear().commit()
        }
    }
}
