package com.example.summondemonssafely

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.summondemonssafely.ui.screens.DemonProfileScreen
import com.example.summondemonssafely.ui.screens.HomeScreen
import com.example.summondemonssafely.ui.screens.RitualScreen
import com.example.summondemonssafely.ui.theme.SummonDemonsSafelyTheme
import com.example.summondemonssafely.viewmodel.DemonViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SummonDemonsSafelyTheme {
                val demonViewModel: DemonViewModel = viewModel()
                AppNavigation(viewModel = demonViewModel)
            }
        }
    }
}

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Ritual : Screen("ritual/{demonId}") {
        fun createRoute(demonId: String) = "ritual/$demonId"
    }
    object Profile : Screen("profile/{demonId}?isNewSummon={isNewSummon}") {
        fun createRoute(demonId: String, isNewSummon: Boolean) =
            "profile/$demonId?isNewSummon=$isNewSummon"
    }
}

@Composable
fun AppNavigation(viewModel: DemonViewModel) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // 1: The Grimoire (HomeScreen)
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = viewModel,
                onViewDemon = { demonId ->
                    navController.navigate(Screen.Profile.createRoute(demonId, isNewSummon = false))
                },
                onSummonDemon = { demonId ->
                    navController.navigate(Screen.Ritual.createRoute(demonId))
                }
            )
        }

        // 2: The Ritual Setup & Chanting (RitualScreen)
        composable(
            route = Screen.Ritual.route,
            arguments = listOf(
                navArgument("demonId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val demonId = backStackEntry.arguments?.getString("demonId") ?: "asmodeus"
            RitualScreen(
                demonId = demonId,
                viewModel = viewModel,
                onSummonSuccess = { summonedId ->
                    navController.navigate(
                        Screen.Profile.createRoute(summonedId, isNewSummon = true)
                    ) {
                        popUpTo(Screen.Home.route)
                    }
                },
                onAbort = {
                    navController.popBackStack()
                }
            )
        }

        // 3: The Demon Profile & Victory View (DemonProfileScreen)
        composable(
            route = Screen.Profile.route,
            arguments = listOf(
                navArgument("demonId") { type = NavType.StringType },
                navArgument("isNewSummon") {
                    type = NavType.BoolType
                    defaultValue = false
                }
            )
        ) { backStackEntry ->
            val demonId = backStackEntry.arguments?.getString("demonId") ?: "asmodeus"
            val isNewSummon = backStackEntry.arguments?.getBoolean("isNewSummon") ?: false

            DemonProfileScreen(
                demonId = demonId,
                isNewSummon = isNewSummon,
                viewModel = viewModel,
                onNavigateBack = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}