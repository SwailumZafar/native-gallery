package com.example.nativegallery.ui

import android.app.Activity
import android.view.WindowManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleCallback
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import java.util.concurrent.atomic.AtomicBoolean

/** Keep test windows awake without changing the user's screen timeout or other device settings. */
internal object TestScreenAwake : ActivityLifecycleCallback {
    private val registered = AtomicBoolean(false)
    fun enable() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val lifecycle = ActivityLifecycleMonitorRegistry.getInstance()
            if (registered.compareAndSet(false, true)) lifecycle.addLifecycleCallback(this)
            lifecycle.getActivitiesInStage(Stage.RESUMED).forEach(::keepAwake)
        }
    }
    override fun onActivityLifecycleChanged(activity: Activity, stage: Stage) {
        if (stage == Stage.RESUMED) keepAwake(activity)
    }
    private fun keepAwake(activity: Activity) {
        activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
