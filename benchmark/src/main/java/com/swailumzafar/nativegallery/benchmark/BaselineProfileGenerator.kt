package com.swailumzafar.nativegallery.benchmark

import android.net.Uri
import android.os.Build
import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun generate() {
        BenchmarkMediaFixtures.grantTargetReadPermission()
        val fixtures = BenchmarkMediaFixtures.seed(FixtureCount)
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        try {
            baselineProfileRule.collect(packageName = TargetPackage) {
                pressHome()
                startActivityAndWait()
                check(device.wait(Until.hasObject(By.text("Photos")), UiTimeoutMillis))

                repeat(TrainingSwipes) {
                    device.swipe(
                        device.displayWidth / 2,
                        device.displayHeight * 3 / 4,
                        device.displayWidth / 2,
                        device.displayHeight / 4,
                        SwipeSteps
                    )
                    device.waitForIdle()
                }

                device.findObject(By.text("Albums"))?.click()
                device.wait(Until.hasObject(By.text("Search albums")), UiTimeoutMillis)
                device.findObject(By.text("Photos"))?.click()
                device.wait(Until.hasObject(By.text("Search photos and videos")), UiTimeoutMillis)
            }
        } finally {
            BenchmarkMediaFixtures.delete(fixtures)
        }
    }

    private companion object {
        const val FixtureCount = 48
        const val TrainingSwipes = 4
        const val SwipeSteps = 18
        const val UiTimeoutMillis = 8_000L
    }
}
