package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 全卡牌图鉴校验。
 *
 * 数据源：kards.pandabot.com.cn 的 /api/deck-builder/cards 接口，
 * 经 tools/fetch_pandabot.py -> tools/gen_pool_from_pandabot.py 生成为
 * WikiCardPool。该接口直接对接游戏数据，提供
 * 卡名 / 国家 / 卡类 / 费用 / 攻击 / 防御 / 稀有度 / 卡池归属 八个字段。
 *
 * 核心目的：确认卡池里每个数值都来自数据源，没有任何推导值。
 * 唯一例外是五张总部卡——接口不返回总部，只能本地定义（规则上固定 20 防）。
 */
class CardCatalogTest {

    private fun norm(s: String): String = s.filterNot { it.isWhitespace() }

    private val generatedKeys: Set<Pair<Nation, String>> =
        WikiCardPool.generated.map { it.nation to norm(it.name) }.toSet()

    private val hqCards: List<Card> = CardPool.all.filter { it.type == CardType.HQ }
    private val nonHqCards: List<Card> = CardPool.all.filter { it.type != CardType.HQ }

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES)
    fun 卡池规模来自数据源() {
        assertTrue(
            "生成卡池应超过 1500 张，实际 ${WikiCardPool.generated.size}",
            WikiCardPool.generated.size > 1500
        )
        assertEquals("总部恰好五张", 5, hqCards.size)
        assertEquals(
            "卡池总数 = 总部 + 生成卡",
            5 + WikiCardPool.generated.size,
            CardPool.all.size
        )
    }

    @Test
    @WikiRule(RuleId.NATION_TEN)
    fun 十国都有真实卡牌() {
        Nation.entries.forEach { nation ->
            assertTrue(
                "${nation.cn} 应有卡，实际 ${CardPool.ofNation(nation).size}",
                CardPool.ofNation(nation).isNotEmpty()
            )
        }
    }

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES, RuleId.NATION_TEN)
    fun 除总部外每张卡都来自数据源() {
        val orphans = nonHqCards
            .filter { (it.nation to norm(it.name)) !in generatedKeys }
            .map { "${it.nation.cn} ${it.name}" }
        assertTrue(
            "以下 ${orphans.size} 张卡不在数据源中：\n" + orphans.joinToString("\n"),
            orphans.isEmpty()
        )
    }

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES)
    fun 生成卡池逐字段自洽() {
        val problems = mutableListOf<String>()
        for (c in WikiCardPool.generated) {
            if (c.cost < 0 || c.cost > 24) problems += "${c.name} 费用异常 ${c.cost}"
            when (c.type) {
                CardType.UNIT -> {
                    if (c.unitType == null) problems += "${c.name} 单位缺少兵种"
                    if (c.attack < 0 || c.defense < 0) problems += "${c.name} 攻防为负"
                }
                CardType.ORDER, CardType.COUNTERMEASURE -> {
                    if (c.unitType != null) problems += "${c.name} 非单位却带兵种"
                }
                CardType.HQ -> problems += "${c.name} 生成卡不应包含总部"
            }
        }
        assertTrue(
            "生成卡有 ${problems.size} 处字段异常：\n" + problems.take(30).joinToString("\n"),
            problems.isEmpty()
        )
    }

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES)
    fun 攻防只对单位生效() {
        val nonUnits = WikiCardPool.generated.filter { it.type != CardType.UNIT }
        assertTrue(
            "非单位卡的攻防必须为 0",
            nonUnits.all { it.attack == 0 && it.defense == 0 }
        )
        val units = WikiCardPool.generated.filter { it.type == CardType.UNIT }
        assertTrue("存在真实单位卡", units.isNotEmpty())
        assertTrue(
            "多数单位卡攻防为正（来自数据源）",
            units.count { it.attack > 0 && it.defense > 0 } > units.size / 2
        )
    }

    @Test
    @WikiRule(RuleId.KREDIT_CAP_24)
    fun 费用分布合理() {
        val costs = WikiCardPool.generated.map { it.cost }
        assertTrue("存在 0 费卡", costs.any { it == 0 })
        assertTrue("存在 1 费卡", costs.any { it == 1 })
        assertTrue("存在高费卡（>=6）", costs.any { it >= 6 })
        assertTrue("最大费用不超过 24", costs.max() <= 24)
    }

    @Test
    @WikiRule(RuleId.HQ_ONE_PER_DECK, RuleId.HQ_DEFENSE_20)
    fun 每个主国都有独立总部() {
        val hqNations = hqCards.map { it.nation }.toSet()
        assertEquals("五个主国各一总部", 5, hqNations.size)
        assertTrue(
            hqNations.containsAll(
                setOf(Nation.GERMANY, Nation.USA, Nation.UK, Nation.USSR, Nation.JAPAN)
            )
        )
        assertTrue("总部都是 20 防", hqCards.all { it.defense == 20 })
    }

    @Test
    @WikiRule(RuleId.NATION_ALLY_ONLY_NO_ELITE, RuleId.DECK_ALLY_NO_ELITE)
    fun 盟国没有精英规则标记() {
        val allyOnly = Nation.entries.filter { !it.canBeMain }
        assertTrue("盟国不应标记为可带精英", allyOnly.all { !it.hasElite })
    }

    @Test
    @WikiRule(RuleId.UNIT_FIVE_TYPES)
    fun 五国都覆盖到多种兵种() {
        listOf(Nation.GERMANY, Nation.USSR, Nation.USA, Nation.UK, Nation.JAPAN).forEach { n ->
            val types = CardPool.ofNation(n).mapNotNull { it.unitType }.toSet()
            assertTrue("${n.cn} 至少覆盖 3 种兵种，实际 $types", types.size >= 3)
        }
    }

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES, RuleId.ORDER_ONE_SHOT)
    fun 四类卡都存在于卡池() {
        CardType.entries.forEach { t ->
            assertTrue("卡池应含${t.cn}", CardPool.all.any { it.type == t })
        }
    }

    @Test
    @WikiRule(RuleId.DECK_COPY_LIMIT_BY_RARITY)
    fun 稀有度与同名上限一致() {
        assertEquals(4, Rarity.STANDARD.maxCopies)
        assertEquals(3, Rarity.LIMITED.maxCopies)
        assertEquals(2, Rarity.SPECIAL.maxCopies)
        assertEquals(1, Rarity.ELITE.maxCopies)
        Rarity.entries.forEach { r ->
            assertTrue("卡池含${r.cn}卡", CardPool.all.any { it.rarity == r })
        }
    }

    @Test
    @WikiRule(RuleId.POOL_RESERVE, RuleId.POOL_RESERVE_CLASSIC)
    fun 卡池归属来自数据源() {
        val reserve = CardPool.all.filter { it.pool == PoolSystem.Pool.RESERVE }
        val active = CardPool.all.filter { it.pool == PoolSystem.Pool.ACTIVE }
        assertTrue("存在预备卡", reserve.isNotEmpty())
        assertTrue("存在现役卡", active.isNotEmpty())
        assertFalse("预备卡不能打排位", PoolSystem.usableInRanked(PoolSystem.Pool.RESERVE))
        assertTrue("预备卡可打经典", PoolSystem.usableInClassic(PoolSystem.Pool.RESERVE))
    }
}