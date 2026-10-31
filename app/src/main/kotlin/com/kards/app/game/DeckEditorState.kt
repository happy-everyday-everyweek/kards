package com.kards.app.game

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kards.core.Card
import com.kards.core.CardPool
import com.kards.core.CardType
import com.kards.core.Deck
import com.kards.core.Nation

/** 本地卡组编辑与持久化，仅保存真实卡池 id。 */
class DeckEditorState(context: Context, initialNation: Nation = Nation.GERMANY) {
    private val prefs = context.getSharedPreferences("custom_deck", Context.MODE_PRIVATE)
    var nation by androidx.compose.runtime.mutableStateOf(initialNation)
        private set
    val cards = mutableStateListOf<Card>()

    init {
        val ids = prefs.getString("ids", "").orEmpty().split(',').filter { it.isNotBlank() }
        ids.forEach { id -> CardPool.all.firstOrNull { it.id == id && it.type != CardType.HQ }?.let { cards += it } }
        if (cards.isNotEmpty()) nation = cards.first().nation
    }

    fun available(query: String = ""): List<Card> = CardPool.ofNation(nation)
        .filter { it.type != CardType.HQ && (query.isBlank() || it.name.contains(query, true)) }
        .distinctBy { it.id }
        .sortedWith(compareBy({ it.cost }, { it.name }))

    fun count(card: Card): Int = cards.count { it.name == card.name }
    val isReady: Boolean get() = cards.size == 39

    fun add(card: Card) {
        if (cards.size < 39 && count(card) < card.rarity.maxCopies) cards += card
    }

    fun remove(card: Card) {
        val index = cards.indexOfLast { it.id == card.id }
        if (index >= 0) cards.removeAt(index)
    }

    fun changeNation(value: Nation) {
        nation = value
        cards.clear()
    }

    fun autoFill() {
        available().forEach { card ->
            while (cards.size < 39 && count(card) < card.rarity.maxCopies) cards += card
        }
    }

    fun save() {
        prefs.edit().putString("ids", cards.joinToString(",") { it.id }).apply()
    }

    fun toDeck(): Deck? = if (isReady) {
        Deck(nation, CardPool.hqFor(nation), cards.toList())
            .takeIf { com.kards.core.DeckValidator.isValid(it) }
    } else null
}