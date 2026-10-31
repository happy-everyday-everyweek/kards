package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 指令执行。来源：《卡牌》《基本构筑思路》。 */
class OrderExecutionTest {

    @Test
    @WikiRule(RuleId.ORDER_ONE_SHOT, RuleId.KREDIT_SPENT_ON_ALL_ACTIONS)
    fun 指令结算后进入弃牌堆() {
        val e = Fixture.started(kredits = 10)
        val before = e.p1.kredits
        val discard = e.p1.discardPile.size
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.USA_ORDER_HX))
        assertEquals("支付卡面费用", before - CardPool.USA_ORDER_HX.cost, e.p1.kredits)
        assertEquals("结算后进弃牌堆", discard + 1, e.p1.discardPile.size)
    }

    @Test
    @WikiRule(RuleId.ORDER_ONE_SHOT)
    fun 非指令卡不能当指令打() {
        val e = Fixture.started()
        assertTrue(e.playOrder(Side.P1, CardPool.GER_INF_89) is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.ORDER_ONE_SHOT, RuleId.KREDIT_SPENT_ON_ALL_ACTIONS)
    fun 指挥点不足打不出指令() {
        val e = Fixture.started(kredits = 1)
        assertTrue(e.playOrder(Side.P1, CardPool.GER_TIGER_E) is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES)
    fun 每张指令都有落地的效果绑定() {
        val orders = CardPool.all.filter { it.type == CardType.ORDER }
        val unmapped = orders.filter { CardEffectParser.parse(it).isEmpty() }
        assertTrue("未绑定效果的指令：${unmapped.map { it.name }}", unmapped.isEmpty())
    }

    @Test
    @WikiRule(RuleId.BUILD_SINGLE_REMOVAL)
    fun 单个伤害指令能打死小单位() {
        val e = Fixture.started(kredits = 10)
        Fixture.freshTurn(e)
        val target = Fixture.place(e, Side.P2, CardPool.USA_INF_432)
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.GER_ORDER_DIVE, target))
        assertTrue("真实原文的 1 点伤害已结算", target.damage >= 1)
        assertEquals("攻击力按原文降为 0", 0, target.attack)
    }

    @Test
    @WikiRule(RuleId.BUILD_AOE_REMOVAL)
    fun 群体伤害指令覆盖敌方全场() {
        val e = Fixture.started(kredits = 20)
        Fixture.freshTurn(e)
        repeat(3) { Fixture.place(e, Side.P2, CardPool.USA_INF_432) }
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.JPN_ORDER_TORA))
        assertTrue("至少两个单位受伤", e.p2.allUnits().count { it.damage > 0 } >= 2)
    }

    @Test
    @WikiRule(RuleId.KREDIT_FROM_ORDERS)
    fun 部分指令给额外指挥点() {
        val e = Fixture.started(kredits = 5)
        val before = e.p1.kredits
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.GER_ORDER_NORTH))
        assertEquals("净变化：-1 费 +2 点", before - CardPool.GER_ORDER_NORTH.cost + 2, e.p1.kredits)
    }

    @Test
    @WikiRule(RuleId.SLOT_CARDS_BREAK_12)
    fun 跳费指令可突破12槽() {
        val e = Fixture.started(kredits = 10)
        e.modifySlots(Side.P1, 12)
        assertTrue("槽已超过 12", e.p1.slots > 12)
        val before = e.p1.slots
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.USA_ORDER_MACHINE))
        assertEquals("指令再 +1", before + 1, e.p1.slots)
    }

    @Test
    @WikiRule(RuleId.SLOT_REDUCE_ENEMY)
    fun 减费指令降低敌方槽() {
        val e = Fixture.started(kredits = 10)
        e.p2.slots = 5
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.UK_ORDER_ARTY))
        assertEquals("敌方 4 槽", 4, e.p2.slots)
    }

    @Test
    @WikiRule(RuleId.KW_INTEL)
    fun 情报指令显示对手手牌() {
        val e = Fixture.started(kredits = 10)
        repeat(3) { e.p2.hand += HandCard(CardPool.USA_INF_432) }
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.USA_ORDER_INTEL))
        assertTrue(e.log.any { it.contains("情报") })
    }

    @Test
    @WikiRule(RuleId.KW_DEVELOP)
    fun 开发指令给出三选一() {
        val e = Fixture.started(kredits = 10)
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.GER_ORDER_DEV))
        assertTrue(e.log.any { it.contains("使用指令") })
    }

    @Test
    @WikiRule(RuleId.KW_SUPPRESS)
    fun 抑制指令清空目标关键词() {
        val e = Fixture.started(kredits = 10)
        val target = Fixture.place(e, Side.P2, CardPool.USA_TANK_SHERMAN)
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.UK_ORDER_SUPPRESS, target))
        assertTrue("被抑制", target.suppressed)
        assertEquals("重甲消失", 0, target.armorValue)
    }

    @Test
    @WikiRule(RuleId.KW_PIN)
    fun 压制指令让敌方无法行动() {
        val e = Fixture.started(kredits = 10)
        val u = Fixture.place(e, Side.P2, CardPool.USA_INF_432)
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.ITA_ORDER_DEFEND))
        assertTrue("被压制", e.isPinned(u))
    }

    @Test
    @WikiRule(RuleId.KW_REPAIR)
    fun 修复指令补满友方() {
        val e = Fixture.started(kredits = 10)
        val u = Fixture.place(e, Side.P1, CardPool.GER_INF_89)
        u.damage = 2
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.UK_ORDER_REPAIR))
        assertEquals("补满", u.maxDefense, u.defense)
    }

    @Test
    @WikiRule(RuleId.KW_RETREAT)
    fun 撤退指令把敌人推回支援阵线() {
        val e = Fixture.started(kredits = 10)
        val u = Fixture.place(e, Side.P2, CardPool.USA_INF_432, Line.FRONT)
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.GER_ORDER_RETREAT, u))
        assertEquals(Line.SUPPORT, u.line)
    }

    @Test
    @WikiRule(RuleId.HQ_HEAL_CARDS)
    fun 提升总部防御力的指令() {
        val e = Fixture.started(kredits = 20)
        e.p1.hq.damage = 10
        val before = e.p1.hq.defense
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.USSR_ORDER_MOTHER))
        assertTrue("总部防御力上升", e.p1.hq.defense > before)
    }

    @Test
    @WikiRule(RuleId.KW_SACRIFICE)
    fun 牺牲类指令以自身为代价() {
        val e = Fixture.started(kredits = 10)
        Fixture.freshTurn(e)
        Fixture.place(e, Side.P2, CardPool.USA_INF_432)
        assertEquals(ActionResult.Ok, e.playOrder(Side.P1, CardPool.JPN_ORDER_DEATH))
        assertTrue("自身进入弃牌堆", e.p1.discardPile.any { it.id == CardPool.JPN_ORDER_DEATH.id })
        assertTrue("敌人被打到", e.p2.allUnits().any { it.damage > 0 })
    }

    @Test
    @WikiRule(RuleId.BUILD_FAST_NO_AOE, RuleId.KW_SACRIFICE)
    fun 亡命之计是快攻可用的群解() {
        assertTrue(EffectTable.hasEffect(CardPool.JPN_ORDER_DEATH))
        assertEquals(OrderEffect.SACRIFICE_SELF, EffectTable.effectOf(CardPool.JPN_ORDER_DEATH))
        assertTrue(BuildRules.aggroAllowsAoe("亡命之计"))
    }
}

/** 反制执行。来源：《卡牌》。 */
class CounterExecutionTest {

    @Test
    @WikiRule(RuleId.COUNTER_ACTIVATED_IN_HAND_HIDDEN, RuleId.KREDIT_SPENT_ON_ALL_ACTIONS)
    fun 激活反制要付费且对手不可见() {
        val e = Fixture.started(kredits = 10)
        val before = e.p1.kredits
        assertEquals(ActionResult.Ok, e.activateCounter(Side.P1, CardPool.GER_COUNTER_FIRE))
        assertEquals(before - CardPool.GER_COUNTER_FIRE.cost, e.p1.kredits)
        assertEquals(1, e.p1.activeCounters.size)
        assertEquals("对手看不到", 0, e.p2.activeCounters.size)
    }

    @Test
    @WikiRule(RuleId.COUNTER_TRIGGER_ON_OPPONENT_TURN)
    fun 敌方部署时触发() {
        val e = Fixture.started(kredits = 10)
        e.activateCounter(Side.P1, CardPool.GER_COUNTER_FIRE)
        val fired = e.triggerCounter(Side.P1, CounterTrigger.ENEMY_DEPLOYS_UNIT)
        assertEquals("触发一张", 1, fired.size)
        assertTrue("触发后移除", e.p1.activeCounters.isEmpty())
        assertTrue("进入弃牌堆", e.p1.discardPile.any { it.id == CardPool.GER_COUNTER_FIRE.id })
    }

    @Test
    @WikiRule(RuleId.COUNTER_REMOVED_AFTER_TRIGGER)
    fun 触发后被移除() {
        val e = Fixture.started(kredits = 10)
        e.activateCounter(Side.P1, CardPool.JPN_COUNTER_BUSHIDO)
        e.triggerCounter(Side.P1, CounterTrigger.ENEMY_ATTACKS)
        assertEquals(0, e.p1.activeCounters.size)
    }

    @Test
    @WikiRule(RuleId.COUNTER_TRIGGER_ON_OPPONENT_TURN)
    fun 时机不匹配不会触发() {
        val e = Fixture.started(kredits = 10)
        e.activateCounter(Side.P1, CardPool.GER_COUNTER_FIRE)
        val fired = e.triggerCounter(Side.P1, CounterTrigger.ENEMY_ATTACKS)
        assertEquals("部署类反制不被攻击触发", 0, fired.size)
        assertEquals("仍在挂着", 1, e.p1.activeCounters.size)
    }

    @Test
    @WikiRule(RuleId.COUNTER_PRIORITY_OVER_ORDER)
    fun 打指令会触发敌方反制() {
        val e = Fixture.started(kredits = 20)
        e.active = Side.P2
        e.p2.kredits = 10
        e.activateCounter(Side.P2, CardPool.USSR_COUNTER_ILLUSION)
        e.active = Side.P1
        e.p1.kredits = 10
        e.playOrder(Side.P1, CardPool.USA_ORDER_HX)
        assertTrue("敌方反制被打指令触发", e.p2.discardPile.any { it.id == CardPool.USSR_COUNTER_ILLUSION.id })
    }

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES)
    fun 每张反制都有触发条件() {
        val counters = CardPool.all.filter { it.type == CardType.COUNTERMEASURE }
        assertTrue(counters.isNotEmpty())
        assertTrue("反制数量足够", EffectTable.counterCount() >= 10)
    }

    @Test
    @WikiRule(RuleId.COUNTER_ACTIVATED_IN_HAND_HIDDEN)
    fun 非反制卡不能激活() {
        val e = Fixture.started()
        assertTrue(e.activateCounter(Side.P1, CardPool.GER_INF_89) is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.COUNTER_DEACTIVATE_NEXT_TURN)
    fun 回合流转会把未触发反制清掉() {
        val e = Fixture.started(kredits = 10)
        e.activateCounter(Side.P1, CardPool.GER_COUNTER_FIRE)
        e.endTurn()
        e.endTurn()
        assertTrue("回到自己回合时清空", e.p1.activeCounters.isEmpty())
    }

    @Test
    @WikiRule(RuleId.COUNTER_RARE_IN_USSR)
    fun 苏联反制数量少() {
        val ussrCounters = CardPool.ofNation(Nation.USSR).count { it.type == CardType.COUNTERMEASURE }
        val germanCounters = CardPool.ofNation(Nation.GERMANY).count { it.type == CardType.COUNTERMEASURE }
        assertTrue("苏联反制少于德国", ussrCounters < germanCounters)
    }

    @Test
    @WikiRule(RuleId.COUNTER_COST_HIDDEN)
    fun 反制费用不在敌方视角显示() {
        val e = Fixture.started(kredits = 10)
        e.activateCounter(Side.P1, CardPool.GER_COUNTER_SUPPRESS)
        assertEquals("敌方看不到激活列表", 0, e.p2.activeCounters.size)
        assertFalse(e.hasActiveCounter(Side.P2, CounterTrigger.ENEMY_DEPLOYS_UNIT))
    }
}