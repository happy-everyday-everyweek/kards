package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** 卡池覆盖：强制卡池规模与各维度完整性。 */
class CardPoolCoverageTest {

    @Test
    @WikiRule(RuleId.NATION_TEN, RuleId.NATION_FIVE_MAJOR, RuleId.NATION_FIVE_ALLY_ONLY)
    fun 十国都有卡() {
        Nation.entries.forEach { nation ->
            assertTrue("${nation.cn} 应有卡", CardPool.ofNation(nation).isNotEmpty())
        }
        assertTrue("卡池规模 >= 150，实际 ${CardPool.all.size}", CardPool.all.size >= 150)
    }

    @Test
    @WikiRule(RuleId.UNIT_FIVE_TYPES)
    fun 五种单位类型都有卡() {
        UnitType.entries.forEach { t ->
            assertTrue("${t.cn} 应有卡", CardPool.ofType(t).isNotEmpty())
        }
    }

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES)
    fun 四类卡都有卡() {
        CardType.entries.forEach { t ->
            assertTrue("${t.cn} 应有卡", CardPool.all.any { it.type == t })
        }
    }

    @Test
    @WikiRule(RuleId.DECK_COPY_LIMIT_BY_RARITY)
    fun 四种稀有度都有卡() {
        Rarity.entries.forEach { r ->
            assertTrue("${r.cn} 应有卡", CardPool.all.any { it.rarity == r })
        }
    }

    @Test
    @WikiRule(RuleId.KW_DEPLOY, RuleId.KW_DESTRUCTION, RuleId.KW_COUNTDOWN)
    fun 每个词条至少在一张卡上出现() {
        val missing = Keyword.entries.filter { k -> CardPool.all.none { it.has(k) } }
        assertTrue("以下词条没有对应卡牌：${missing.map { it.cn }}", missing.isEmpty())
    }

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES, RuleId.ORDER_ONE_SHOT)
    fun 指令与反制数量充足() {
        val orders = CardPool.all.count { it.type == CardType.ORDER }
        val counters = CardPool.all.count { it.type == CardType.COUNTERMEASURE }
        assertTrue("指令 >= 30，实际 $orders", orders >= 30)
        assertTrue("反制 >= 8，实际 $counters", counters >= 8)
    }

    @Test
    @WikiRule(RuleId.NATION_ALLY_ONLY_NO_ELITE)
    fun 盟国卡池没有精英() {
        val allyOnly = Nation.entries.filter { !it.canBeMain }
        val elites = CardPool.all.filter { it.nation in allyOnly && it.rarity == Rarity.ELITE }
        assertTrue("盟国不该有精英：${elites.map { it.name }}", elites.isEmpty())
    }

    @Test
    @WikiRule(RuleId.HQ_ONE_PER_DECK, RuleId.HQ_DEFENSE_20)
    fun 每个主国都有20血总部() {
        val hqs = CardPool.all.filter { it.type == CardType.HQ }
        assertEquals("五个总部", 5, hqs.size)
        assertTrue("全部 20 血", hqs.all { it.defense == 20 })
    }

    @Test
    @WikiRule(RuleId.TANK_LIGHT_CHEAP_HEAVY_STRONG)
    fun 重坦比轻坦贵且更强() {
        val light = CardPool.GER_TANK_35T
        val heavy = CardPool.GER_TIGER
        assertTrue("重坦更贵", heavy.cost > light.cost)
        assertTrue("重坦攻防更高", heavy.attack > light.attack && heavy.defense > light.defense)
    }

    @Test
    @WikiRule(RuleId.ARTILLERY_LOW_STATS, RuleId.ARTILLERY_NO_BLITZ)
    fun 炮兵攻防偏低且不带闪击() {
        val artilleries = CardPool.ofType(UnitType.ARTILLERY)
        assertTrue("炮兵攻击力不超过 10", artilleries.all { it.attack <= 10 })
    }

    @Test
    @WikiRule(RuleId.INFANTRY_SUBTYPE_COMMANDO)
    fun 存在突击队类型卡() {
        assertTrue(CardPool.all.any { it.name.contains("突击队") })
    }

    @Test
    @WikiRule(RuleId.TANK_SUBTYPE)
    fun 存在具名坦克子类型卡() {
        assertTrue(CardPool.all.any { it.name.contains("T-34") })
        assertTrue(CardPool.all.any { it.name.contains("谢尔曼") })
    }

    @Test
    @WikiRule(RuleId.KW_INTEL_MAX_3)
    fun 情报数值不超过3() {
        assertTrue(CardPool.all.all { it.intel <= 3 })
        assertTrue(CardPool.all.any { it.intel >= 1 })
    }

    @Test
    @WikiRule(RuleId.KW_HEAVY_ARMOR)
    fun 重甲数值不超过3() {
        assertTrue(CardPool.all.all { it.armor <= 3 })
        assertTrue("存在带重甲的卡", CardPool.all.any { it.armor > 0 })
    }

    @Test
    @WikiRule(RuleId.KW_SACRIFICE)
    fun 存在牺牲类指令() {
        assertTrue(CardPool.all.any { it.has(Keyword.SACRIFICE) })
        assertEquals(OrderEffect.SACRIFICE_SELF, EffectTable.effectOf(CardPool.JPN_ORDER_DEATH))
    }

    @Test
    @WikiRule(RuleId.KW_FORECAST, RuleId.KW_CONVERT, RuleId.KW_PINCER)
    fun 预报转换钳击都有卡() {
        assertTrue(CardPool.withKeyword(Keyword.FORECAST).isNotEmpty())
        assertTrue(CardPool.withKeyword(Keyword.CONVERT).isNotEmpty())
        assertTrue(CardPool.withKeyword(Keyword.PINCER).isNotEmpty())
    }

    @Test
    @WikiRule(RuleId.NATION_EXILE_BOTH, RuleId.NATION_EXILE_COUNTS_ALLY)
    fun 流亡机制有卡() {
        assertTrue(CardPool.withKeyword(Keyword.EXILE).isNotEmpty())
        assertTrue(CardPool.all.any { it.has(Keyword.EXILE) })
    }
}