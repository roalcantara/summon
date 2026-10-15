package com.summondemons.safely.ui

import androidx.lifecycle.ViewModel
import com.summondemons.safely.data.Demon
import com.summondemons.safely.data.DemonCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class DemonEntry(val demon: Demon, val unlocked: Boolean)

/** Activity-scoped. Unlock state is in memory only (PRODUCT.md). */
class DemonViewModel : ViewModel() {

    private val _demons = MutableStateFlow(DemonCatalog.all.map { DemonEntry(it, unlocked = false) })
    val demons: StateFlow<List<DemonEntry>> = _demons.asStateFlow()

    fun demon(id: String): Demon? = DemonCatalog.byId(id)

    fun unlock(id: String) {
        _demons.update { entries ->
            entries.map { if (it.demon.id == id) it.copy(unlocked = true) else it }
        }
    }
}
