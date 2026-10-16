package com.summondemons.safely.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.summondemons.safely.time.DebugSettings
import com.summondemons.safely.time.GameClock
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/** Lands each hourly tick just after the hour turns. */
private const val TOP_OF_HOUR_SLACK_MS = 50L

/** Device-local time plus the debug day offset (§3). */
class SystemGameClock(context: Context, private val debug: StateFlow<DebugSettings>) : GameClock {

    private val appContext = context.applicationContext

    override fun now(): ZonedDateTime = ZonedDateTime.now().plusDays(debug.value.dayOffset.toLong())

    override val ticks: Flow<ZonedDateTime> =
        merge(systemTimeChanges(), topsOfTheHour(), debug.drop(1).map { })
            .map { now() }
            .onStart { emit(now()) }
            .conflate()

    private fun systemTimeChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                trySend(Unit)
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        // System broadcasts still reach a non-exported receiver.
        ContextCompat.registerReceiver(appContext, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        awaitClose { appContext.unregisterReceiver(receiver) }
    }

    private fun topsOfTheHour(): Flow<Unit> = flow {
        while (true) {
            val now = ZonedDateTime.now()
            val next = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
            delay(Duration.between(now, next).toMillis() + TOP_OF_HOUR_SLACK_MS)
            emit(Unit)
        }
    }
}
