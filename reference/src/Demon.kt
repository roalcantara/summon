package com.example.summondemonssafely.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class Demon(
    val id: String,
    val name: String,
    val title: String,
    val themeColor: Color,
    val chant: String,
    val chantPhrases: List<String>,
    val description: String,
    val quote: String,
    val power: Int,
    val stars: Int,
    val level: Int,
    val role: String,
    val element: String,
    val legions: Int,
    val isUnlocked: Boolean,
    val stats: DemonStats,
    val synergy: DemonSynergy
)

data class DemonStats(
    val damage: Int,
    val defense: Int,
    val arcaneMastery: Int,
    val healing: Int,
    val agility: Int,
    val intel: Int,
    val strength: Int,
    val maxHealth: Int,
    val magicAtk: Int,
    val fireResist: Int
)

data class DemonSynergy(
    val name: String,
    val description: String,
    val bonus: String
)

class DemonViewModel : ViewModel() {

    private val _demons = MutableStateFlow(
        listOf(
            Demon(
                id = "asmodeus",
                name = "Asmodeus",
                title = "Lord of Arithmetic & Hidden Wealth",
                themeColor = Color(0xFFFFD54F), // Yellow / Amber
                chant = "DAEMONIUM RESIDENTE, OSTENDE MIHI FACIEM TUAM POTENTIAM, VENIT AD ME. DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS, UT VOTA OMNIA OPUS EST",
                chantPhrases = listOf(
                    "DAEMONIUM RESIDENTE,",
                    "OSTENDE MIHI FACIEM TUAM POTENTIAM,",
                    "VENIT AD ME.",
                    "DONA MIHI PER FOEDUS QUOD EST IRE AD PRAESENS SUM VOBIS, UT VOTA OMNIA OPUS EST."
                ),
                description = "Teaches arithmetic, astronomy, geometry, and invincibility. Answers all questions—if you can stand the smugness.",
                quote = "Three heads are better than one—especially when two want your soul!",
                power = 18450,
                stars = 5,
                level = 62,
                role = "Arch-Sorcerer",
                element = "Infernal",
                legions = 72,
                isUnlocked = true, // Playable / Unlocked by default
                stats = DemonStats(
                    damage = 8420,
                    defense = 5190,
                    arcaneMastery = 9980,
                    healing = 2800,
                    agility = 480,
                    intel = 1240,
                    strength = 610,
                    maxHealth = 94200,
                    magicAtk = 3420,
                    fireResist = 45
                ),
                synergy = DemonSynergy(
                    name = "Triumvirate of Pride",
                    description = "Boosts burning burst rate by 22% during Rituals.",
                    bonus = "+15% Magic Crit Active"
                )
            ),
            Demon(
                id = "astaroth",
                name = "Astaroth",
                title = "Duke of Past, Present & Future",
                themeColor = Color(0xFFE040FB), // Magenta / Purple
                chant = "TUI GRATIA LOVIS GRATIA SIT CURA. HIC EST ME DIGNUS LEGI SINT BENE OBEDIENTES",
                chantPhrases = listOf(
                    "TUI GRATIA LOVIS GRATIA SIT CURA.",
                    "HIC EST ME DIGNUS LEGI",
                    "SINT BENE OBEDIENTES."
                ),
                description = "Duke of past secrets and liberal sciences. Appears riding an infernal dragon beast, carrying a viper. Speaks truth through dreams and visions.",
                quote = "The past is ink, the future is shadow, and I hold the candle.",
                power = 16820,
                stars = 4,
                level = 58,
                role = "Void Knight",
                element = "Nether",
                legions = 40,
                isUnlocked = false, // Locked initially; unlocked via Ritual
                stats = DemonStats(
                    damage = 7120,
                    defense = 6450,
                    arcaneMastery = 11200,
                    healing = 3400,
                    agility = 560,
                    intel = 1390,
                    strength = 540,
                    maxHealth = 88500,
                    magicAtk = 4100,
                    fireResist = 30
                ),
                synergy = DemonSynergy(
                    name = "Nether Veil Accord",
                    description = "Increases invocation resistance by 30% against demonic backfire.",
                    bonus = "+18% Spell Haste"
                )
            )
        )
    )

    val demons: StateFlow<List<Demon>> = _demons.asStateFlow()

    fun getDemonById(id: String): Demon? {
        return _demons.value.find { it.id == id }
    }

    fun unlockDemon(id: String) {
        _demons.update { currentList ->
            currentList.map { demon ->
                if (demon.id == id) demon.copy(isUnlocked = true) else demon
            }
        }
    }
}