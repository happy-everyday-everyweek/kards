package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 指挥点与指挥点槽。来源：《基本机制》。 */
class KreditRulesTest {

    @Test
    @WikiRule(RuleId.SLOT_START_0, RuleId.SLOT_PLUS_1_PER_TURN_TO_12)
    fun 开局槽为零_先手首回合为一() {
        val e = Fixture.engine()
        assertEquals("开局双方槽均为 0", 0, e.p1.slots)
        assertEquals("开局双方槽均为 0", 0, e.p2.slots)
        e.start(Side.P1)
        assertEquals("先手首回合槽 1", 1, e.p1.slots)
    }

    @Test
    @WikiRule(RuleId.SLOT_PLUS_1_PER_TURN_TO_12, RuleId.SLOT_START_0)
    fun 双方各自回合开始才加槽() {
        val e = Fixture.engine()
        e.start(Side.P1)
        assertEquals("后手未开始，槽仍为 0", 0, e.p2.slots)
        e.endTurn()
        assertEquals("后手回合槽 1", 1, e.p2.slots)
        e.endTurn()
        assertEquals("先手第二轮槽 2", 2, e.p1.slots)
    }

    @Test
    @WikiRule(RuleId.SLOT_PLUS_1_PER_TURN_TO_12, RuleId.SLOT_CAP_24)
    fun 槽在十二点后不再自然增长() {
        val e = Fixture.engine()
        e.start(Side.P1)
        repeat(30) { e.beginTurn(Side.P1, drawAtStart = false) }
        assertEquals("自然增长止于 12", 12, e.p1.slots)
    }

    @Test
    @WikiRule(RuleId.SLOT_CAP_24, RuleId.SLOT_PLUS_1_PER_TURN_TO_12)
    fun 槽的硬上限是二十四() {
        val e = Fixture.started()
        e.modifySlots(Side.P1, 100)
        assertEquals("卡牌效果也无法突破 24", 24, e.p1.slots)
        e.modifySlots(Side.P1, -100)
        assertEquals("可被降到 0", 0, e.p1.slots)
    }

    @Test
    @WikiRule(RuleId.KREDIT_RESET_TO_SLOTS, RuleId.SLOT_PLUS_1_PER_TURN_TO_12)
    fun 回合开始指挥点被强制设为槽数() {
        val e = Fixture.engine()
        e.start(Side.P1)
        e.p1.kredits = 0
        e.beginTurn(Side.P1, drawAtStart = false)
        assertEquals("指挥点 = 槽数", e.p1.slots, e.p1.kredits)
    }

    @Test
    @WikiRule(RuleId.KREDIT_RESET_TO_SLOTS, RuleId.KREDIT_CAP_24)
    fun 未花完的指挥点不会累积() {
        val e = Fixture.engine()
        e.start(Side.P1)
        e.p1.kredits = 1
        e.beginTurn(Side.P1, drawAtStart = false)
        assertEquals("不会累加到上回合余额", e.p1.slots, e.p1.kredits)
        assertTrue("不会超过槽数", e.p1.kredits <= e.p1.slots)
    }

    @Test
    @WikiRule(RuleId.KREDIT_CAP_24)
    fun 指挥点上限二十四_溢出舍弃() {
        val e = Fixture.started()
        e.p1.kredits = 20
        e.grantKredits(Side.P1, 10)
        assertEquals("溢出部分舍弃", 24, e.p1.kredits)
    }

    @Test
    @WikiRule(RuleId.KREDIT_SPENT_ON_ALL_ACTIONS, RuleId.ACTION_COST_ON_MOVE_AND_ATTACK)
    fun 部署与移动都扣指挥点() {
        val e = Fixture.started(kredits = 8)
        val u = Fixture.place(e, Side.P1, CardPool.PZ_GREN)
        assertEquals(ActionResult.Ok, e.moveUnit(Side.P1, u, Line.FRONT))
        assertEquals("移动扣 1 点行动花费", 7, e.p1.kredits)
        assertEquals(ActionResult.Ok, e.deployUnit(Side.P1, CardPool.TANK_35T))
        assertEquals("部署扣卡面费用", 7 - CardPool.TANK_35T.cost, e.p1.kredits)
    }

    @Test
    @WikiRule(RuleId.KREDIT_SPENT_ON_ALL_ACTIONS)
    fun 攻击也扣指挥点() {
        val e = Fixture.started(kredits = 5)
        Fixture.freshTurn(e)
        val u = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        val before = e.p1.kredits
        e.attackHq(Side.P1, u)
        assertEquals("攻击扣行动花费", before - u.actionCost, e.p1.kredits)
    }

    @Test
    @WikiRule(RuleId.KREDIT_SPENT_ON_ALL_ACTIONS)
    fun 指挥点不足时部署被拒绝() {
        val e = Fixture.started(kredits = 1)
        val r = e.deployUnit(Side.P1, CardPool.FURRY_TANK)
        assertTrue(r is ActionResult.Rejected)
        assertTrue((r as ActionResult.Rejected).reason.contains("指挥点不足"))
    }

    @Test
    @WikiRule(RuleId.KREDIT_SPENT_ON_ALL_ACTIONS)
    fun 指挥点不足时移动被拒绝() {
        val e = Fixture.started(kredits = 0)
        val u = Fixture.place(e, Side.P1, CardPool.PZ_GREN)
        assertTrue(e.moveUnit(Side.P1, u, Line.FRONT) is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.KREDIT_SPEND_VISIBLE_EXCEPT_COUNTER, RuleId.COUNTER_COST_HIDDEN)
    fun 反制激活不可见于敌方() {
        val e = Fixture.started()
        e.p1.activeCounters += CardPool.COUNTER_AMBUSH
        assertEquals("反制在手中激活", 1, e.p1.activeCounters.size)
        assertEquals("反制类型存在", CardType.COUNTERMEASURE, CardPool.COUNTER_AMBUSH.type)
    }
}

/** 抽牌、手牌与士气伤害。来源：《基本机制》《卡组》。 */
class DrawAndMoraleTest {

    @Test
    @WikiRule(RuleId.OPENING_FIRST_4_NO_DRAW)
    fun 先手开局四张且首回合不抽() {
        val e = Fixture.engine()
        e.start(Side.P1)
        assertEquals("先手 4 张", 4, e.p1.hand.size)
        assertEquals("首回合不抽牌", 4, e.p1.hand.size)
    }

    @Test
    @WikiRule(RuleId.OPENING_SECOND_5_PLUS_1, RuleId.DRAW_ONE_PER_TURN)
    fun 后手开局五张且首回合再抽一张() {
        val e = Fixture.engine()
        e.start(Side.P1)
        assertEquals("后手 5 张", 5, e.p2.hand.size)
        e.endTurn()
        assertEquals("后手首回合再抽 1 张", 6, e.p2.hand.size)
    }

    @Test
    @WikiRule(RuleId.DRAW_ONE_PER_TURN, RuleId.OPENING_SECOND_5_PLUS_1)
    fun 每回合开始抽一张() {
        val e = Fixture.engine()
        e.start(Side.P1)
        val before = e.p1.hand.size
        e.endTurn()
        e.endTurn()
        assertEquals("先手第二个回合抽 1 张", before + 1, e.p1.hand.size)
    }

    @Test
    @WikiRule(RuleId.HAND_CAP_9_BURN)
    fun 手牌上限九_第十张爆掉() {
        val e = Fixture.started()
        while (e.p1.hand.size < Engine.HAND_CAP) e.p1.hand += HandCard(CardPool.INF_89)
        e.draw(Side.P1)
        assertEquals("手牌保持 9 张", 9, e.p1.hand.size)
    }

    @Test
    @WikiRule(RuleId.BURN_NOT_DISCARD, RuleId.HAND_CAP_9_BURN)
    fun 爆牌不进弃牌堆也不离开牌库() {
        val e = Fixture.started()
        while (e.p1.hand.size < Engine.HAND_CAP) e.p1.hand += HandCard(CardPool.INF_89)
        val pile = e.p1.drawPile.size
        val discard = e.p1.discardPile.size
        e.draw(Side.P1)
        assertEquals("不进入弃牌堆", discard, e.p1.discardPile.size)
        assertEquals("不从牌库顶移除", pile, e.p1.drawPile.size)
    }

    @Test
    @WikiRule(RuleId.MORALE_DAMAGE_ON_EMPTY_DRAW)
    fun 牌库空时抽牌改为士气伤害() {
        val e = Fixture.started()
        e.p1.drawPile.clear()
        val before = e.p1.hq.defense
        e.draw(Side.P1)
        assertEquals("总部掉 1 点", before - 1, e.p1.hq.defense)
    }

    @Test
    @WikiRule(RuleId.MORALE_DAMAGE_ESCALATES, RuleId.MORALE_DAMAGE_ON_EMPTY_DRAW)
    fun 士气伤害按等差递增() {
        val e = Fixture.started()
        e.p1.drawPile.clear()
        val before = e.p1.hq.defense
        repeat(3) { e.draw(Side.P1) }
        assertEquals("1+2+3", 6, e.p1.fatigueDamageTotal)
        assertEquals("总部共掉 6 点", before - 6, e.p1.hq.defense)
    }

    @Test
    @WikiRule(RuleId.MORALE_NO_RESET, RuleId.MORALE_DAMAGE_ESCALATES)
    fun 士气计数不因洗牌重置() {
        val e = Fixture.started()
        e.p1.drawPile.clear()
        e.draw(Side.P1)
        assertEquals(1, e.p1.moraleCounter)
        e.p1.drawPile += CardPool.INF_89
        e.draw(Side.P1)
        e.p1.drawPile.clear()
        e.draw(Side.P1)
        assertEquals("中断后继续递增", 2, e.p1.moraleCounter)
    }

    @Test
    @WikiRule(RuleId.MORALE_TRUE_DAMAGE)
    fun 士气伤害无视免疫() {
        val e = Fixture.started()
        e.p1.hq.immune = true
        e.p1.drawPile.clear()
        val before = e.p1.hq.defense
        e.draw(Side.P1)
        assertEquals("免疫也无法减免士气伤害", before - 1, e.p1.hq.defense)
    }

    @Test
    @WikiRule(RuleId.BOND_MORALE)
    fun 协力不满足时总部受士气伤害() {
        val e = Fixture.started()
        val bond = CardPool.INF_89.copy(id = "u_bond", keywords = setOf(Keyword.BOND))
        val before = e.p1.hq.defense
        assertFalse(e.checkBond(Side.P1, bond))
        assertEquals("受 1 点士气伤害", before - 1, e.p1.hq.defense)
    }

    @Test
    @WikiRule(RuleId.BOND_MORALE, RuleId.MORALE_TRUE_DAMAGE)
    fun 协力满足时不受伤害() {
        val e = Fixture.started()
        Fixture.place(e, Side.P1, CardPool.INF_89)
        val bond = CardPool.INF_89.copy(id = "u_bond", keywords = setOf(Keyword.BOND))
        val before = e.p1.hq.defense
        assertTrue(e.checkBond(Side.P1, bond))
        assertEquals("条件满足不扣血", before, e.p1.hq.defense)
    }
}