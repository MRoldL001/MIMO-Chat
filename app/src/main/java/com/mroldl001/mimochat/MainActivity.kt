package com.mroldl001.mimochat

import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import android.content.pm.ActivityInfo
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.mroldl001.mimochat.data.preferences.PreferencesManager
import com.mroldl001.mimochat.ui.AppNavigation
import com.mroldl001.mimochat.ui.theme.MIMOChatTheme
import com.mroldl001.mimochat.ui.theme.ThemeColor
import com.mroldl001.mimochat.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val windowManager = windowManager
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(displayMetrics)
        
        val widthDp = displayMetrics.widthPixels / displayMetrics.density
        
        val isPhone = widthDp < 600
        
        requestedOrientation = if (isPhone) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
        }
        
        setContent {
            MainContent(isExpandedScreen = !isPhone)
        }
    }
}

@Composable
private fun MainContent(
    viewModel: MainViewModel = hiltViewModel(),
    isExpandedScreen: Boolean
) {
    var themeColor by remember { mutableStateOf(viewModel.preferencesManager.getThemeColor()) }
    var themeMode by remember { mutableStateOf(viewModel.preferencesManager.getThemeMode()) }
    var isSearchOpen by remember { mutableStateOf(false) }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var onBackToChat: (() -> Unit)? by remember { mutableStateOf(null) }

    BackHandler(enabled = isSearchOpen || isDrawerOpen) {
        isSearchOpen = false
        isDrawerOpen = false
        onBackToChat?.invoke()
    }
    
    val onNavigateFromSearch: () -> Unit = { isSearchOpen = false }
    val onNavigateFromDrawer: (Boolean) -> Unit = { isDrawerOpen = it }
    
    MIMOChatTheme(
        themeColor = themeColor,
        themeMode = themeMode
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            AppNavigation(
                isExpandedScreen = isExpandedScreen,
                onThemeChanged = { newColor, newMode ->
                    themeColor = newColor
                    themeMode = newMode
                },
                onSearchStateChanged = { isOpen ->
                    isSearchOpen = isOpen
                },
                onNavigateFromSearch = onNavigateFromSearch,
                onNavigateFromDrawer = onNavigateFromDrawer,
                onBackToChat = { onBackToChat = it }
            )
        }
    }
}

@HiltViewModel
class MainViewModel @Inject constructor(
    val preferencesManager: PreferencesManager
) : ViewModel()
