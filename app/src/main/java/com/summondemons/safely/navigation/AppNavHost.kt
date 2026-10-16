package com.summondemons.safely.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.summondemons.safely.BuildConfig
import com.summondemons.safely.platform.AndroidHeadingSource
import com.summondemons.safely.platform.AndroidPcmSource
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ui.DemonViewModel
import com.summondemons.safely.ui.fx.LocalNavAnimatedVisibilityScope
import com.summondemons.safely.ui.fx.LocalSharedTransitionScope
import com.summondemons.safely.ui.screens.GrimoireScreen
import com.summondemons.safely.ui.screens.ProfileScreen
import com.summondemons.safely.ui.screens.RitualScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(navController: NavHostController = rememberNavController()) {
    val demonViewModel: DemonViewModel = viewModel() // activity-scoped
    val appContext = LocalContext.current.applicationContext

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(navController = navController, startDestination = Grimoire) {
                composable<Grimoire> {
                    ProvideDestinationScope {
                        val demons by demonViewModel.demons.collectAsStateWithLifecycle()
                        GrimoireScreen(
                            demons = demons,
                            onSummon = { id -> navController.navigate(Ritual(id)) },
                            onView = { id -> navController.navigate(Profile(id, isNewSummon = false)) },
                        )
                    }
                }

                composable<Ritual> { entry ->
                    ProvideDestinationScope {
                        val demon = demonViewModel.demon(entry.toRoute<Ritual>().demonId)
                        if (demon == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            val factory = remember(demon) {
                                RitualViewModel.factory(
                                    demon,
                                    AndroidHeadingSource(appContext),
                                    AndroidPcmSource(),
                                    commitSummon = { demonViewModel.unlock(demon.id) },
                                )
                            }
                            RitualScreen(
                                viewModel = viewModel(factory = factory),
                                onSummoned = {
                                    navController.navigate(Profile(demon.id, isNewSummon = true)) { popUpTo<Grimoire>() }
                                },
                                onAbort = dropUnlessResumed { navController.popBackStack() },
                            )
                        }
                    }
                }

                composable<Profile> { entry ->
                    ProvideDestinationScope {
                        val route = entry.toRoute<Profile>()
                        val demon = demonViewModel.demon(route.demonId)
                        if (demon == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            ProfileScreen(
                                demon = demon,
                                isNewSummon = route.isNewSummon,
                                onDone = dropUnlessResumed { navController.popBackStack<Grimoire>(inclusive = false) },
                                onUnsummon = if (BuildConfig.DEBUG && !route.isNewSummon) {
                                    dropUnlessResumed {
                                        demonViewModel.lock(demon.id)
                                        navController.popBackStack<Grimoire>(inclusive = false)
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedVisibilityScope.ProvideDestinationScope(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this, content = content)
