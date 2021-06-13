package com

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.hl3hl3.arcoremeasure.BuildConfig

object Logger {

    private var firebaseCrashlytics: FirebaseCrashlytics? = null

    private fun getCrashlytics(): FirebaseCrashlytics {
        val crashlytics = firebaseCrashlytics ?: FirebaseCrashlytics.getInstance()
        firebaseCrashlytics = crashlytics
        return crashlytics
    }

    fun log(e: Exception) {
        try {
            getCrashlytics().recordException(e)
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
        } catch (ex: Exception) {
            if (BuildConfig.DEBUG) {
                ex.printStackTrace()
            }
        }
    }

    fun logStatus(msg: String) {
        try {
            getCrashlytics().log(msg)
        } catch (e: Exception) {
            log(e)
        }
    }

    fun log(tag: String, log: String) {
        if (BuildConfig.DEBUG) {
            Log.d(tag, log)
        }
    }

}