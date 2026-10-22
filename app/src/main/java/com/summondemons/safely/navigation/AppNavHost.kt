package com.summondemons.safely.navigation

import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import com.summondemons.safely.platform.AndroidHeadingSource
import com.summondemons.safely.platform.AndroidPcmSource
import com.summondemons.safely.platform.ClipRecorders
import com.summondemons.safely.platform.AppLocale
import com.summondemons.safely.platform.SessionLogs
import com.summondemons.safely.platform.DeviceCheck
import com.summondemons.safely.progress.ProgressRules
import com.summondemons.safely.platform.WhisperTranscriber
import com.summondemons.safely.ritual.CheckMode
import com.summondemons.safely.platform.GameSounds
import com.summondemons.safely.platform.LocalGameSounds
import com.summondemons.safely.ritual.RitualPhase
import com.summondemons.safely.ritual.RitualViewModel
import com.summondemons.safely.ui.GameUiState
import com.summondemons.safely.ui.GameViewModel
import com.summondemons.safely.ui.fx.LocalNavAnimatedVisibilityScope
import com.summondemons.safely.ui.fx.LocalSharedTransitionScope
import com.summondemons.safely.ui.screens.GrimoireScreen
import com.summondemons.safely.ui.components.SettingsSheet
import com.summondemons.safely.ui.screens.DebugSheet
import com.summondemons.safely.ui.screens.HomeScreen
import com.summondemons.safely.ui.screens.RitualScreen

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AppNavHost(graph: AppGraph, navController: NavHostController = rememberNavController()) {
    val factory = remember(graph) { GameViewModel.factory(graph.progressStore, graph.clock, graph.debugSettings) }
    val game: GameViewModel = viewModel(factory = factory) // activity-scoped
    val state by game.uiState.collectAsStateWithLifecycle()
    val appContext = LocalContext.current.applicationContext
    val activity = LocalActivity.current
    val sounds = remember { GameSounds(appContext) }
    DisposableEffect(sounds) { onDispose { sounds.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { game.evaluate() }

    if (!state.loaded) {
        // The splash normally covers this; never render screens from an unloaded Progress().
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        return
    }
    var debugOpen by rememberSaveable { mutableStateOf(false) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    val debugSettings by graph.debugSettings.settings.collectAsStateWithLifecycle()
    val onMenu = { settingsOpen = true }

    CompositionLocalProvider(LocalGameSounds provides sounds) {
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                NavHost(navController = navController, startDestination = Home) {
                    composable<Home> {
                        LockPortrait()
                        ProvideDestinationScope {
                            HomeDestination(state, onMenu, onOpen = dropUnlessResumed {
                                game.openGrimoireFromHome()
                                navController.navigate(Grimoire) { launchSingleTop = true }
                            }, onDismissEscape = game::dismissEscapeBanners)
                        }
                    }

                    composable<Grimoire> {
                        LockPortrait()
                        ProvideDestinationScope {
                            val lifecycle = LocalLifecycleOwner.current.lifecycle
                            GrimoireScreen(
                                state = state,
                                onBack = dropUnlessResumed { navController.popBackStack() },
                                onSelect = game::select,
                                onSummon = { id ->
                                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) navController.navigate(Ritual(id))
                                },
                                onVisit = game::visit,
                                onDismissCelebration = game::dismissCelebration,
                                onConsumeReveal = game::consumeReveal,
                                onSecretShown = game::secretShown,
                                onMenu = onMenu,
                            )
                        }
                    }

                    composable<Ritual> { entry ->
                        ProvideDestinationScope {
                            val demon = DemonCatalog.byId(entry.toRoute<Ritual>().demonId)
                            if (demon == null) {
                                LaunchedEffect(Unit) { navController.popBackStack() }
                            } else {
                                RitualDestination(demon, game, state, appContext, navController)
                            }
                        }
                    }
                }
            }
        }
        if (settingsOpen) {
            SettingsSheet(
                language = AppLocale.get(appContext),
                onLanguage = { tag ->
                    AppLocale.set(appContext, tag)
                    activity?.recreate()
                },
                onDismiss = { settingsOpen = false },
                onDebug = if (BuildConfig.DEBUG) ({ settingsOpen = false; debugOpen = true }) else null,
            )
        }
        if (BuildConfig.DEBUG && debugOpen) {
            DebugSheet(
                state = state,
                court = debugSettings.court,
                demons = DemonCatalog.all,
                onDismiss = { debugOpen = false },
                onAdvanceDay = game::advanceDay,
                onResetDaily = game::resetDaily,
                onCourt = game::setCourt,
                onBind = game::bind,
                onUnsummon = game::unsummon,
                onResetAll = {
                    game.resetAll()
                    debugOpen = false
                    navController.popBackStack<Home>(inclusive = false)
                },
            )
        }
    }
}

@Composable
private fun HomeDestination(
    state: GameUiState,
    onMenu: () -> Unit,
    onOpen: () -> Unit,
    onDismissEscape: () -> Unit,
) {
    val demons = DemonCatalog.all
    val anyRestless = demons.any { ProgressRules.isRestless(state.record(it.id), state.today) }
    val focusId = ProgressRules.focusDemon(state.progress, DemonCatalog.ids, state.today)
    HomeScreen(
        boundCount = demons.count { state.record(it.id).bound },
        demonCount = demons.size,
        courtHouses = state.courtHouses,
        restlessAccent = if (anyRestless) DemonCatalog.byId(focusId)?.accent else null,
        // Always on: play-testers who missed it once had no way to learn how to start (RC, TODO BWFm).
        // homeHintSeen is still recorded, for a smarter hint after the release.
        showHint = true,
        escaped = demons.filter { state.record(it.id).escapeBannerPending },
        onOpen = onOpen,
        onDismissEscape = onDismissEscape,
        onMenu = onMenu,
    )
}

@Composable
private fun RitualDestination(
    demon: Demon,
    game: GameViewModel,
    state: GameUiState,
    appContext: android.content.Context,
    navController: NavHostController,
) {
    val ritualFactory = remember(demon) {
        // The day the ritual started spends the summon, even if the climax lands after midnight (§1).
        // Taken with the factory, which only builds a ViewModel in this same composition: a rotation
        // keeps the existing ViewModel (and its day); process death restarts both the ritual and the day.
        val startDay = game.today()
        RitualViewModel.factory(
            demon,
            AndroidHeadingSource(appContext),
            AndroidPcmSource(),
            inCourt = { game.inCourtNow(demon.house) },
            commitSummon = { flawless -> game.commitSummon(demon.id, startDay, flawless) },
            clipRecorder = ClipRecorders.create(appContext),
            sessionLog = SessionLogs.create(appContext),
            transcriber = WhisperTranscriber.get(appContext),
            checkMode = {
                val result = DeviceCheck.cached(appContext)
                // Debug builds can force a mode from the chant screen's debug panel (ADR-0028).
                val forced = if (BuildConfig.DEBUG) DeviceCheck.forced(appContext) else null
                when {
                    forced == CheckMode.Lenient -> CheckMode.Lenient to "forced in debug"
                    forced == CheckMode.Whisper -> CheckMode.Whisper to ""
                    result == null -> CheckMode.Lenient to "device check still running"
                    result.mode == CheckMode.Lenient -> CheckMode.Lenient to result.reason
                    else -> CheckMode.Whisper to ""
                }
            },
            // Before every ritual; "?" on the chant screen reopens it (ADR-0021).
            showIntro = true,
        )
    }
    val ritual: RitualViewModel = viewModel(factory = ritualFactory)
    // Checked once, when the Ritual opens (incl. restored after process death): a restored back
    // stack must never allow a second summon (§3). Not re-checked later, so the commit at
    // ClimaxStart can never pop the climax.
    val blocked = remember {
        val phase = ritual.uiState.value.ritual.phase
        val settled = phase == RitualPhase.Climax || phase == RitualPhase.Done
        !settled && (state.record(demon.id).bound || !state.canSummonToday)
    }
    if (blocked) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }
    RitualScreen(
        viewModel = ritual,
        firstSummon = ProgressRules.neverSummoned(state.progress),
        onSummoned = {
            game.celebrate(demon.id, ritual.uiState.value.ritual.flawless)
            navController.popBackStack<Grimoire>(inclusive = false)
        },
        onAbort = dropUnlessResumed { navController.popBackStack<Grimoire>(inclusive = false) },
    )
}

@Composable
private fun AnimatedVisibilityScope.ProvideDestinationScope(content: @Composable () -> Unit) =
    CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this, content = content)

@Composable
private fun LockPortrait() {
    val activity = LocalActivity.current ?: return
    DisposableEffect(activity) {
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        onDispose {}
    }
}
