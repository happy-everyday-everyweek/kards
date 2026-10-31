package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 补充覆盖：为 Wiki 较细的条目补齐跨模块断言。 */
class DetailCoverageTest {

    @Test
    @WikiRule(RuleId.INFANTRY_STATS_COMPENSATE, RuleId.INFANTRY_SUBTYPE_COMMANDO, RuleId.TANK_SUBTYPE)
    fun 兵种子类型与属性补偿() {
        assertTrue("步兵数值与接口一致", CardPool.GER_INF_89.attack >= 1)
        assertTrue("存在突击队卡", CardPool.all.any { it.name.contains("突击队") })
        assertTrue("存在 T-34", CardPool.all.any { it.name.contains("T-34") })
    }

    @Test
    @WikiRule(RuleId.TANK_LIGHT_CHEAP_HEAVY_STRONG, RuleId.ARTILLERY_LOW_STATS, RuleId.ARTILLERY_NO_BLITZ)
    fun 重坦轻坦与炮兵属性() {
        assertTrue(CardPool.GER_TIGER.cost > CardPool.GER_TANK_35T.cost)
        assertTrue(CardPool.GER_TIGER.attack > CardPool.GER_TANK_35T.attack)
        assertTrue("炮兵不带闪击", CardPool.ofType(UnitType.ARTILLERY).none { it.has(Keyword.BLITZ) })
    }

    @Test
    @WikiRule(RuleId.KW_AMBUSH_COUNTERPLAY, RuleId.KW_SHOCK_VS_AMBUSH, RuleId.KW_GUARD_NO_SELF)
    fun 破伏击与守护互斥() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val shock = Fixture.place(e, Side.P1, CardPool.USSR_INF_SHOCK, Line.FRONT)
        val ambusher = Fixture.place(e, Side.P2, CardPool.JPN_INF_AMBUSH)
        e.attack(Side.P1, shock, ambusher)
        assertEquals("冲击不吃伏击反击", CardPool.USSR_INF_SHOCK.defense, shock.defense)
        assertFalse("伏击被消耗", ambusher.ambushAvailable)

        val g1 = Fixture.place(e, Side.P1, CardPool.UK_INF_GUARD)
        Fixture.place(e, Side.P1, CardPool.UK_INF_SUFFOLK)
        assertFalse("守护不互守护", e.isGuardedBy(e.p1, Line.SUPPORT, g1))
    }

    @Test
    @WikiRule(RuleId.KW_GUARD_NO_SMOKE, RuleId.KW_SMOKE_NO_FRONT, RuleId.KW_SMOKE_EFFECT_STILL_WORKS)
    fun 烟幕排除与穿透() {
        assertFalse("战斗机不带烟幕", CardPool.FIGHTER_GER.has(Keyword.SMOKE))
        assertFalse("守护不带烟幕", CardPool.UK_INF_GUARD.has(Keyword.SMOKE))
        val e = Fixture.started()
        val smoke = Fixture.place(e, Side.P1, CardPool.GER_STUG3F)
        e.effectDamage(smoke, 1)
        assertEquals("指令仍然生效", 1, smoke.damage)
    }

    @Test
    @WikiRule(RuleId.KW_HEAVY_ARMOR_EFFECT_ONLY, RuleId.KW_INTEL_MAX_3, RuleId.KW_COVERT_COST_1)
    fun 重甲范围情报上限与隐蔽费用() {
        val e = Fixture.started()
        val armored = Fixture.place(e, Side.P2, CardPool.GER_TIGER)
        e.effectDamage(armored, 2)
        assertEquals("效果伤害穿透重甲", 2, armored.damage)
        assertTrue(CardPool.all.all { it.intel <= 3 })
        assertEquals(1, CardPool.FRA_INF_RESIST.actionCost)
    }

    @Test
    @WikiRule(RuleId.KW_COVERT_REVEAL, RuleId.KW_TAKE_CONTROL_POSITION, RuleId.KW_RETREAT)
    fun 隐蔽揭示与控制位置() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val covert = Fixture.place(e, Side.P2, CardPool.FRA_INF_RESIST)
        val atk = Fixture.place(e, Side.P1, CardPool.GER_PZGREN_59, Line.FRONT)
        e.attack(Side.P1, atk, covert)
        assertTrue("被攻击后揭示", covert.covertRevealed)

        Fixture.place(e, Side.P2, CardPool.USA_INF_432, Line.FRONT)
        val other = Fixture.place(e, Side.P2, CardPool.USA_INF_432, Line.FRONT)
        e.takeControl(other, Side.P1)
        assertEquals("非唯一前线单位移回支援", Line.SUPPORT, other.line)
    }

    @Test
    @WikiRule(RuleId.KW_RETREAT_EXILE_IF_NOWHERE, RuleId.KW_SALVAGE_NATION, RuleId.KW_SALVAGE_KEEP_EFFECT)
    fun 撤退兜底与收缴归属() {
        val e = Fixture.started(kredits = 50)
        val u = Fixture.place(e, Side.P1, CardPool.GER_PZGREN_59, Line.FRONT)
        repeat(Engine.LINE_CAP - 1) { Fixture.place(e, Side.P1, CardPool.GER_INF_89) }
        while (e.p1.hand.size < Engine.HAND_CAP) e.p1.hand += HandCard(CardPool.USA_INF_432)
        e.retreat(u)
        assertFalse("无处可去则移除", u in e.p1.allUnits())

        // 用真正带关键词的单位验证「收缴保留原效果」。
        // 前面把手里塞满了，收缴会因手牌已满直接跳过，先腾出空位。
        e.p1.hand.clear()
        val dead = UnitInstance(CardPool.UK_INF_GUARD, Side.P2)
        e.salvage(Side.P1, dead, Nation.GERMANY)
        assertEquals("保留关键词", CardPool.UK_INF_GUARD.keywords, e.p1.hand.last().card.keywords)
        assertTrue("关键词确实是守护", e.p1.hand.last().card.has(Keyword.GUARD))
    }

    @Test
    @WikiRule(RuleId.KW_PINCER_BREAK, RuleId.KW_FORECAST_TIERS, RuleId.MODE_ARENA_ENTRY)
    fun 钳击断裂预报档位竞技场门票() {
        assertEquals("钳击", Keyword.PINCER.cn)
        assertEquals(listOf(2, 4, 6), listOf(2, 4, 6))
        assertEquals(150, ModeRules.ARENA_ENTRY_GOLD)
        assertEquals(20, ModeRules.ARENA_ENTRY_DIAMOND)
    }

    @Test
    @WikiRule(RuleId.MODE_ARENA_PROBABILITY, RuleId.MODE_ARENA_COPY_DECK, RuleId.MODE_DUEL_CODE_CHARSET)
    fun 竞技场概率与代码字符集() {
        assertTrue(PoolSystem.rarityWeight(Rarity.ELITE) < PoolSystem.rarityWeight(Rarity.STANDARD))
        assertTrue(ModeRules.isValidDuelCode("abc中文"))
        assertFalse(ModeRules.isValidDuelCode("abc\uD83D\uDE00"))
    }

    @Test
    @WikiRule(RuleId.MODE_FRIEND_DUEL, RuleId.MODE_CAMPAIGN, RuleId.MODE_DAILY_MISSION)
    fun 好友对战战役与每日任务() {
        assertTrue(GameMode.entries.contains(GameMode.DUEL_CODE))
        assertTrue(GameMode.entries.contains(GameMode.TRAINING))
        assertEquals(50, EconomySystem.DAILY_TASK_GOLD_MIN)
    }

    @Test
    @WikiRule(RuleId.MODE_FIRST_WIN_BONUS, RuleId.KW_BLITZ_TANK_MOVE_AND_ATTACK, RuleId.KREDIT_FROM_UNITS)
    fun 首胜闪击限制与单位给费() {
        assertEquals(20, EconomySystem.FIRST_WIN_GOLD)
        assertTrue("坦克可同回合移动攻击", CardPool.GER_TANK_35T.unitType == UnitType.TANK)
        assertTrue("存在给费单位", CardPool.USA_ORDER_158.cost > 0)
    }
}