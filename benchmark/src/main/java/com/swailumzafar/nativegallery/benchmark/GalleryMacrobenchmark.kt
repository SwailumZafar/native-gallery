package com.swailumzafar.nativegallery.benchmark

import android.net.Uri
import android.os.Build
import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.Q)
class GalleryMacrobenchmark {

    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    private var fixtures: List<Uri> = emptyList()

    @Before
    fun setUp() {
        BenchmarkMediaFixtures.grantTargetReadPermission()
        fixtures = BenchmarkMediaFixtures.seed(FixtureCount)
    }

    @After
    fun tearDown() {
        BenchmarkMediaFixtures.delete(fixtures)
    }

    @Test
    fun coldStartup() {
        benchmarkRule.measureRepeated(
            packageName = TargetPackage,
            metrics = listOf(StartupTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.COLD,
            iterations = StartupIterations,
            setupBlock = {
                pressHome()
            }
        ) {
            startActivityAndWait()
            waitForText("Photos")
        }
    }

    @Test
    fun photoGridScroll() {
        benchmarkRule.measureRepeated(
            packageName = TargetPackage,
            metrics = listOf(FrameTimingMetric()),
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM,
            iterations = ScrollIterations,
            setupBlock = {
                pressHome()
                startActivityAndWait()
                waitForText("Photos")
            }
        ) {
            repeat(ScrollSwipes) {
                device.swipe(
                    device.displayWidth / 2,
                    device.displayHeight * 3 / 4,
                    device.displayWidth / 2,
                    device.displayHeight / 4,
                    SwipeSteps
                )
                device.waitForIdle()
            }
        }
    }

    private fun MacrobenchmarkScope.waitForText(text: String) {
        check(device.wait(Until.hasObject(By.text(text)), UiTimeoutMillis)) {
            "Timed out waiting for $text"
        }
    }

    private val MacrobenchmarkScope.device: UiDevice
        get() = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private companion object {
        const val FixtureCount = 48
        const val StartupIterations = 5
        const val ScrollIterations = 5
        const val ScrollSwipes = 5
        const val SwipeSteps = 18
        const val UiTimeoutMillis = 8_000L
    }
}
