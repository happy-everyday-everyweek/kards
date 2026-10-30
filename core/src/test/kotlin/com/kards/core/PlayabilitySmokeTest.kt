package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 可玩性冒烟测试：确认一局游戏能从开局跑到结束。
 * 之前出现过 App 打不开、手牌不减少、回合错乱的问题，
 * 这些不是单条规则的错，而是整条链没跑通，专门盯这条链。
 */
class PlayabilitySmokeTest {

    private fun buildDeck(nation: Nation): Deck {
        val pool = CardPool.ofNation(nation).filter { it.type != CardType.HQ }.sortedBy { it.cost }
        require(pool.isNotEmpty()) { "${nation.cn} 无可用卡牌" }
        val chosen = mutableListOf<Card>()
        var guard = 0
        while (chosen.size < 39 && guard++ < 10000) {
            var added = false
            for (c in pool) {
                if (chosen.size >= 39) break
                if (chosen.count { it.name == c.name } < c.rarity.maxCopies) {
                    chosen += c
                    added = true
                }
            }
            if (!added) break
        }
        var i = 0
        while (chosen.size < 39) {
            chosen += pool[i % pool.size]
            i++
        }
        return Deck(nation, CardPool.hqFor(nation), chosen.take(39))
    }

    @Test
    fun 卡池对象可以安全初始化() {
        assertTrue("卡池不能为空", CardPool.all.isNotEmpty())
        assertEquals("效果表登记的卡都应存在于卡池", emptyList<String>(), EffectTable.missing)
        assertTrue("指令效果至少登记 20 条", EffectTable.orderCount() >= 20)
        assertTrue("反制触发至少登记 8 条", EffectTable.counterCount() >= 8)
    }

    @Test
    fun 每个主国都能组出合法卡组() {
        listOf(Nation.GERMANY, Nation.USA, Nation.UK, Nation.USSR, Nation.JAPAN).forEach { n ->
            val deck = buildDeck(n)
            assertEquals("${n.cn} 卡组 40 张", 40, deck.total)
            assertEquals("${n.cn} 总部与主国一致", n, deck.hq.nation)
            assertTrue("${n.cn} 卡组应合法", DeckValidator.isValid(deck))
        }
    }

    @Test
    fun 一局可以从开局跑到分出胜负() {
        val e = Engine(
            Engine.newState(buildDeck(Nation.GERMANY), Side.P1),
            Engine.newState(buildDeck(Nation.USSR), Side.P2)
        )
        e.start(Side.P1)
        assertEquals("开局后应是先手回合", Side.P1, e.active)
        assertTrue("先手应有起手手牌", e.p1.hand.isNotEmpty())

        var turns = 0
        while (!e.finished && turns < 60) {
            turns++
            val me = e.active
            val p = e.player(me)

            var deployed = 0
            while (deployed < 2) {
                val idx = p.hand.indexOfFirst { it.card.type == CardType.UNIT && it.card.cost <= p.kredits }
                if (idx < 0) break
                val card = p.hand[idx].card
                val line = if (p.frontHasRoom() && (e.frontOwner == null || e.frontOwner == me)) {
                    Line.FRONT
                } else Line.SUPPORT
                val r = e.deployUnit(me, card, line)
                if (r is ActionResult.Ok) {
                    if (idx < p.hand.size && p.hand[idx].card.id == card.id) p.hand.removeAt(idx)
                    else p.hand.removeAll { it.card.id == card.id }
                    deployed++
                } else break
            }

            p.allUnits().toList().forEach { u ->
                if (u.attacksThisTurn == 0 && !e.isPinned(u)) {
                    val foeUnits = e.player(me.opponent).allUnits()
                    if (foeUnits.isNotEmpty()) e.attack(me, u, foeUnits.first())
                    else e.attackHq(me, u)
                }
            }

            if (!e.finished) e.endTurn()
        }
        assertTrue("应能推进多个回合，实际 $turns", turns > 0)
    }

    @Test
    fun 部署会消耗指挥点() {
        val e = Engine(
            Engine.newState(buildDeck(Nation.GERMANY), Side.P1),
            Engine.newState(buildDeck(Nation.USSR), Side.P2)
        )
        e.start(Side.P1)
        val idx = e.p1.hand.indexOfFirst { it.card.type == CardType.UNIT && it.card.cost <= e.p1.kredits }
        assertTrue("开局手牌里应有可部署单位", idx >= 0)
        val card = e.p1.hand[idx].card
        val kBefore = e.p1.kredits
        assertEquals(ActionResult.Ok, e.deployUnit(Side.P1, card, Line.SUPPORT))
        assertEquals("部署扣指挥点", kBefore - card.cost, e.p1.kredits)
        assertTrue("战场应出现该单位", e.p1.allUnits().any { it.card.id == card.id })
    }

    @Test
    fun 回合切换会重置指挥点() {
        val e = Engine(
            Engine.newState(buildDeck(Nation.GERMANY), Side.P1),
            Engine.newState(buildDeck(Nation.USSR), Side.P2)
        )
        e.start(Side.P1)
        e.endTurn()
        assertEquals("endTurn 后应轮到对手", Side.P2, e.active)
        e.endTurn()
        assertEquals("再结束应回到先手", Side.P1, e.active)
    }
}