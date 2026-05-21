package com.mroldl001.mimochat

import android.app.Application
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MIMOChatApp : Application() {
    
    @Inject
    lateinit var preferencesManager: PreferencesManager
}
