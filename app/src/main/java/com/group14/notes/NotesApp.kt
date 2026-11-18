package com.group14.notes

import android.app.Application
import com.group14.notes.util.AppSettings

/** Runs once per app process, before any screen: applies the saved light/dark/system theme. */
class NotesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppSettings(this).applyTheme()
    }
}
