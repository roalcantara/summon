package com.summondemons.safely.navigation

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.summondemons.safely.AppGraph
import com.summondemons.safely.BuildConfig
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.platform.AndroidHeadingSource
import com.summondemons.safely.platform.AndroidPcmSource
import com.summondemons.safely.platform.GameSounds
import com.summondemons.safely.platform.LocalGameSounds
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.ui.screens.HomeScreen
import com.summondemons.safely.ui.GameViewModel
import com.summondemons.safely.ui.fx.LocalNavAnimatedVisibilityScope
import com.summondemons.safely.ui.fx.LocalSharedTransitionScope
import com.summondemons.safely.ui.screens.DemonEntry
import com.summondemons.safely.ui.screens.GrimoireScreen
import com.summondemons.safely.ui.screens.ProfileScreen
import com.summondemons.safely.ui.screens.RitualScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(graph: AppGraph, navController: NavHostController = rememberNavController()) {
    val factory = remember(graph) { GameViewModel.factory(graph.progressStore, graph.clock, graph.debugSettings) }
    val game: GameViewModel = viewModel(factory = factory) // activity-scoped
    val state by game.uiState.collectAsStateWithLifecycle()
    val appContext = LocalContext.current.applicationContext
    val sounds = remember { GameSounds(appContext) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { game.evaluate() }

    if (!state.loaded) {
        // The splash normally covers this; never render screens from an unloaded Progress().
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }

    CompositionLocalProvider(LocalGameSounds provides sounds) {
        SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(navController = navController, startDestination = Home) {
                composable<Home> {
                    ProvideDestinationScope {
                        val demons = DemonCatalog.all
                        val anyRestless = demons.any { ProgressRules.isRestless(state.record(it.id), state.today) }
                        val focusId = ProgressRules.focusDemon(state.progress, DemonCatalog.ids, state.today)
                        HomeScreen(
                            boundCount = demons.count { state.record(it.id).bound },
                            demonCount = demons.size,
                            courtHouses = state.courtHouses,
                            restlessAccent = if (anyRestless) DemonCatalog.byId(focusId)?.accent else null,
                            showHint = !state.progress.homeHintSeen,
                            escaped = demons.filter { state.record(it.id).escapeBannerPending },
                            onOpen = dropUnlessResumed {
                                game.openGrimoireFromHome()
                                navController.navigate(Grimoire) { launchSingleTop = true }
                            },
                            onDismissEscape = game::dismissEscapeBanners,
                            onDebug = null, // Task 26
                        )
                    }
                }

                composable<Grimoire> {
                    ProvideDestinationScope {
                        GrimoireScreen(
                            demons = DemonCatalog.all.map { DemonEntry(it, unlocked = state.record(it.id).bound) },
                            onSummon = { id -> navController.navigate(Ritual(id)) },
                            onView = { id -> navController.navigate(Profile(id, isNewSummon = false)) },
                        )
                    }
                }

                composable<Ritual> { entry ->
                    ProvideDestinationScope {
                        val demon = DemonCatalog.byId(entry.toRoute<Ritual>().demonId)
                        if (demon == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            // The day the ritual started spends the summon, even if the climax lands after midnight.
                            val startDay = rememberSaveable { game.today() }
                            val ritualFactory = remember(demon) {
                                RitualViewModel.factory(
                                    demon,
                                    AndroidHeadingSource(appContext),
                                    AndroidPcmSource(),
                                    inCourt = { game.inCourtNow(demon.house) },
                                    commitSummon = { flawless -> game.commitSummon(demon.id, startDay, flawless) },
                                )
                            }
                            RitualScreen(
                                viewModel = viewModel(factory = ritualFactory),
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
                        val demon = DemonCatalog.byId(route.demonId)
                        if (demon == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            ProfileScreen(
                                demon = demon,
                                isNewSummon = route.isNewSummon,
                                onDone = dropUnlessResumed { navController.popBackStack<Grimoire>(inclusive = false) },
                                onUnsummon = if (BuildConfig.DEBUG && !route.isNewSummon) {
                                    dropUnlessResumed {
                                        game.unsummon(demon.id)
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
}

@Composable
private fun AnimatedVisibilityScope.ProvideDestinationScope(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this, content = content)
