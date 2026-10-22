package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.accessibility.AppAccessibilityService
import com.example.model.InstalledAppInfo
import com.example.ui.components.TopBar
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TasksScreen
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.DarkGreen
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

enum class NavDestination(val titlePt: String) {
    HOME("Início"),
    CHAT("Conversas"),
    TASKS("Tarefas"),
    SETTINGS("Configurações")
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as GeminiMobileApp

        setContent {
            MyApplicationTheme {
                MainContent(app = app)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Accessibility status re-evaluates when returning from Android Settings
    }
}

@Composable
fun MainContent(app: GeminiMobileApp) {
    var currentScreen by remember { mutableStateOf(NavDestination.HOME) }
    var resumeVersion by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeVersion++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val assistantState by app.taskManager.assistantState.collectAsState()
    val activeProviderName by app.settingsRepository.aiProviderFlow.collectAsState(initial = "LOCAL_INTELLIGENT")

    val installedApps = remember { mutableStateListOf<InstalledAppInfo>() }

    LaunchedEffect(Unit) {
        val pm = app.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(mainIntent, PackageManager.ResolveInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(mainIntent, 0)
        }

        installedApps.clear()
        for (info in resolved) {
            val appName = info.loadLabel(pm).toString()
            val pkg = info.activityInfo.packageName
            val isSystem = (info.activityInfo.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
            installedApps.add(InstalledAppInfo(appName, pkg, isSystem))
        }
    }

    val isAccessibilityEnabled = remember(resumeVersion) {
        AppAccessibilityService.isAccessibilityEnabled(context)
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        topBar = {
            TopBar(
                assistantState = assistantState,
                activeModelName = if (activeProviderName == "GEMINI_API") "Gemini Cloud" else "Motor Local",
                isAccessibilityEnabled = isAccessibilityEnabled
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                contentColor = TextSecondary,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == NavDestination.HOME,
                    onClick = { currentScreen = NavDestination.HOME },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == NavDestination.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Início",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(NavDestination.HOME.titlePt, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = DarkGreen,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == NavDestination.CHAT,
                    onClick = { currentScreen = NavDestination.CHAT },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == NavDestination.CHAT) Icons.Filled.Chat else Icons.Outlined.Chat,
                            contentDescription = "Conversas",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(NavDestination.CHAT.titlePt, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = DarkGreen,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == NavDestination.TASKS,
                    onClick = { currentScreen = NavDestination.TASKS },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == NavDestination.TASKS) Icons.Filled.Checklist else Icons.Outlined.Checklist,
                            contentDescription = "Tarefas",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(NavDestination.TASKS.titlePt, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = DarkGreen,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = currentScreen == NavDestination.SETTINGS,
                    onClick = { currentScreen = NavDestination.SETTINGS },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == NavDestination.SETTINGS) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = "Configurações",
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = { Text(NavDestination.SETTINGS.titlePt, fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonGreen,
                        selectedTextColor = NeonGreen,
                        indicatorColor = DarkGreen,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )
            }
        }
    ) { innerPadding ->
        when (currentScreen) {
            NavDestination.HOME -> HomeScreen(
                taskManager = app.taskManager,
                speechManager = app.speechManager,
                installedApps = installedApps,
                modifier = Modifier.padding(innerPadding)
            )
            NavDestination.CHAT -> ChatScreen(
                database = app.database,
                taskManager = app.taskManager,
                speechManager = app.speechManager,
                installedApps = installedApps,
                modifier = Modifier.padding(innerPadding)
            )
            NavDestination.TASKS -> TasksScreen(
                database = app.database,
                modifier = Modifier.padding(innerPadding)
            )
            NavDestination.SETTINGS -> SettingsScreen(
                settingsRepository = app.settingsRepository,
                database = app.database,
                speechManager = app.speechManager,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
