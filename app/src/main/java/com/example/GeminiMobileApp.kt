package com.example

import android.app.Application
import com.example.automation.TaskManager
import com.example.data.SettingsRepository
import com.example.data.local.AppDatabase
import com.example.voice.SpeechManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GeminiMobileApp : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    lateinit var database: AppDatabase
        private set
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var taskManager: TaskManager
        private set
    lateinit var speechManager: SpeechManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getDatabase(this)
        settingsRepository = SettingsRepository(this)
        taskManager = TaskManager.getInstance(this, appScope)
        speechManager = SpeechManager(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        speechManager.destroy()
    }
}
