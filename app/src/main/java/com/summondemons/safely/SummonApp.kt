package com.summondemons.safely

import android.app.Application

class SummonApp : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
