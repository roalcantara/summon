package com.summondemons.safely.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class DemonViewModelTest {

    @Test
    fun allDemonsStartLocked() {
        val vm = DemonViewModel()
        assertEquals(listOf(false, false), vm.demons.value.map { it.unlocked })
    }

    @Test
    fun unlockFlipsOnlyThatDemon() {
        val vm = DemonViewModel()
        vm.unlock("astaroth")
        assertEquals(
            mapOf("asmodeus" to false, "astaroth" to true),
            vm.demons.value.associate { it.demon.id to it.unlocked },
        )
    }

    @Test
    fun lockReSealsOnlyThatDemon() {
        val vm = DemonViewModel()
        vm.unlock("asmodeus")
        vm.unlock("astaroth")
        vm.lock("astaroth")
        assertEquals(
            mapOf("asmodeus" to true, "astaroth" to false),
            vm.demons.value.associate { it.demon.id to it.unlocked },
        )
    }

    @Test
    fun demonLookup() {
        val vm = DemonViewModel()
        assertNotNull(vm.demon("asmodeus"))
        assertNull(vm.demon("baal"))
    }
}
