package com.summondemons.safely

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.summondemons.safely.navigation.AppNavHost
import com.summondemons.safely.platform.AppLocale
import com.summondemons.safely.ui.theme.SummonDemonsSafelyTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    // The language picked in Settings, if any (ADR-0031)
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        // Dark theme only: light-on-dark system bar icons regardless of system setting.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        val graph = (application as SummonApp).graph
        // Covers every destination restored after process death: nothing renders an unloaded Progress().
        var ready = false
        splash.setKeepOnScreenCondition { !ready }
        lifecycleScope.launch {
            graph.awaitReady()
            ready = true
        }
        setContent {
            SummonDemonsSafelyTheme {
                AppNavHost(graph)
            }
        }
    }
}
