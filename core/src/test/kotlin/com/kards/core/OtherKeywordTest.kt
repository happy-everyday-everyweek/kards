package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 非战斗词条。来源：《词条》。 */
class OtherKeywordTest {

    @Test
    @WikiRule(RuleId.KW_REPAIR)
    fun 修复把防御力补满() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.INF_89)
        u.damage = 2
        e.repair(u)
        assertEquals(u.maxDefense, u.defense)
    }

    @Test
    @WikiRule(RuleId.KW_REPAIR)
    fun 修复按含增益的最大防御力计算() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.INF_89)
        u.maxDefenseBonus = 3
        e.repair(u)
        assertEquals("基础 2 + 增益 3", 5, u.defense)
    }

    @Test
    @WikiRule(RuleId.KW_IMMUNE)
    fun 免疫单位仍可被抑制处理() {
        val u = UnitInstance(CardPool.INF_89.copy(keywords = setOf(Keyword.IMMUNE)), Side.P1)
        assertTrue("免疫关键词存在", u.has(Keyword.IMMUNE))
        u.suppress()
        assertFalse("抑制后免疫消失", u.has(Keyword.IMMUNE))
    }

    @Test
    @WikiRule(RuleId.KW_IMMUNE)
    fun 免疫标记可被清除() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.INF_89)
        u.keywords += Keyword.IMMUNE
        assertTrue(u.has(Keyword.IMMUNE))
        e.suppress(u)
        assertFalse("抑制清空关键词", u.has(Keyword.IMMUNE))
    }

    @Test
    @WikiRule(RuleId.KW_MOBILIZE)
    fun 动员在己方回合开始加一加一() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.MOBILIZE_UNIT)
        val atk = u.attack
        val def = u.maxDefense
        e.beginTurn(Side.P1, drawAtStart = false)
        assertEquals(atk + 1, u.attack)
        assertEquals(def + 1, u.maxDefense)
    }

    @Test
    @WikiRule(RuleId.KW_MOBILIZE)
    fun 动员受伤后失去() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.MOBILIZE_UNIT)
        assertTrue(u.mobilized)
        e.effectDamage(u, 1)
        assertFalse("受伤即失去动员", u.mobilized)
    }

    @Test
    @WikiRule(RuleId.KW_PIN)
    fun 压制使单位不能移动() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.SUPPORT)
        e.pin(u)
        assertTrue(e.moveUnit(Side.P1, u, Line.FRONT) is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.KW_PIN)
    fun 压制使单位不能攻击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val u = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        e.pin(u)
        val target = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        val r = e.canAttack(Side.P1, u, Target.EnemyUnit(target))
        assertTrue(r is ActionResult.Rejected)
        assertTrue((r as ActionResult.Rejected).reason.contains("压制"))
    }

    @Test
    @WikiRule(RuleId.KW_PIN)
    fun 压制在拥有者下回合结束后解除() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.SUPPORT)
        e.pin(u)
        assertTrue("当回合仍被压制", e.isPinned(u))
        repeat(4) { e.beginTurn(Side.P1, drawAtStart = false) }
        assertFalse("跨过拥有者回合后解除", e.isPinned(u))
    }

    @Test
    @WikiRule(RuleId.KW_SUPPRESS)
    fun 抑制移除关键词并重置面板() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.ARMOR_TANK)
        u.attackBonus = 2
        u.maxDefenseBonus = 2
        u.damage = 1
        e.suppress(u)
        assertTrue(u.suppressed)
        assertTrue(u.keywords.isEmpty())
        assertEquals("攻击回到卡面", CardPool.ARMOR_TANK.attack, u.attack)
        assertEquals("防御回到卡面", CardPool.ARMOR_TANK.defense, u.defense)
        assertEquals("重甲失效", 0, u.armorValue)
    }

    @Test
    @WikiRule(RuleId.KW_SUPPRESS)
    fun 抑制后重甲不再减免() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P2, CardPool.ARMOR_TANK)
        e.suppress(u)
        e.effectDamage(u, 3)
        assertEquals("3 点全吃", 3, u.damage)
    }

    @Test
    @WikiRule(RuleId.KW_TAKE_CONTROL)
    fun 控制改变控制者但所有者不变() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        e.takeControl(u, Side.P1)
        assertTrue(u in e.p1.support)
        assertTrue(u !in e.p2.support)
        assertEquals("所有者仍是 P2", Side.P2, u.owner)
    }

    @Test
    @WikiRule(RuleId.KW_TAKE_CONTROL)
    fun 非前线唯一单位时移到新控制者支援阵线() {
        val e = Fixture.started()
        Fixture.place(e, Side.P2, CardPool.INF_432, Line.FRONT)
        val u = Fixture.place(e, Side.P2, CardPool.PZ_GREN, Line.FRONT)
        e.takeControl(u, Side.P1)
        assertEquals("移到支援阵线", Line.SUPPORT, u.line)
    }

    @Test
    @WikiRule(RuleId.KW_RETREAT)
    fun 撤退回到支援阵线() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        e.retreat(u)
        assertEquals(Line.SUPPORT, u.line)
    }

    @Test
    @WikiRule(RuleId.KW_RETREAT)
    fun 支援阵线满时退回手牌() {
        val e = Fixture.started()
        val front = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        repeat(Engine.LINE_CAP - 1) { Fixture.place(e, Side.P1, CardPool.INF_89, Line.SUPPORT) }
        val before = e.p1.hand.size
        e.retreat(front)
        assertEquals("退回手牌", before + 1, e.p1.hand.size)
    }

    @Test
    @WikiRule(RuleId.KW_VETERAN)
    fun 老兵升级后重置面板() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.STEALTH_TANK)
        u.damage = 2
        u.attackBonus = 3
        e.promoteVeteran(u)
        assertTrue(u.veteran)
        assertEquals(CardPool.STEALTH_TANK.attack, u.attack)
        assertEquals(CardPool.STEALTH_TANK.defense, u.defense)
    }

    @Test
    @WikiRule(RuleId.KW_VETERAN)
    fun 老兵卡牌带老兵词条() {
        assertTrue(CardPool.STEALTH_TANK.has(Keyword.VETERAN))
    }

    @Test
    @WikiRule(RuleId.KW_SALVAGE)
    fun 收缴得到一一复制且花费至多三() {
        val e = Fixture.started()
        val dead = UnitInstance(CardPool.FURRY_TANK, Side.P2)
        val before = e.p1.hand.size
        e.salvage(Side.P1, dead, Nation.GERMANY)
        assertEquals(before + 1, e.p1.hand.size)
        val copy = e.p1.hand.last().card
        assertEquals(1, copy.attack)
        assertEquals(1, copy.defense)
        assertTrue("花费至多 3", copy.cost <= 3)
    }

    @Test
    @WikiRule(RuleId.KW_SALVAGE)
    fun 收缴保留原卡效果() {
        val e = Fixture.started()
        val dead = UnitInstance(CardPool.ARMOR_TANK, Side.P2)
        e.salvage(Side.P1, dead, Nation.GERMANY)
        val copy = e.p1.hand.last().card
        assertEquals("关键词保留", CardPool.ARMOR_TANK.keywords, copy.keywords)
    }

    @Test
    @WikiRule(RuleId.KW_PINCER)
    fun 钳击属于非战斗词条() {
        val card = CardPool.PZ_GREN.copy(keywords = setOf(Keyword.PINCER))
        assertTrue(card.has(Keyword.PINCER))
        assertFalse(Keyword.PINCER.isCombatKeyword)
    }

    @Test
    @WikiRule(RuleId.KW_PINCER)
    fun 钳击词条存在() {
        assertTrue(Keyword.entries.any { it == Keyword.PINCER })
        assertEquals("钳击", Keyword.PINCER.cn)
    }

    @Test
    @WikiRule(RuleId.KW_COVERT)
    fun 隐蔽单位被攻击后揭示() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val atk = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        val covert = Fixture.place(e, Side.P2, CardPool.COVERT_UNIT, Line.SUPPORT)
        assertFalse(covert.covertRevealed)
        e.attack(Side.P1, atk, covert)
        assertTrue(covert.covertRevealed)
    }

    @Test
    @WikiRule(RuleId.KW_COVERT)
    fun 隐蔽单位初始未揭示() {
        val u = UnitInstance(CardPool.COVERT_UNIT, Side.P1)
        assertFalse(u.covertRevealed)
        assertEquals("行动花费为 1", 1, CardPool.COVERT_UNIT.actionCost)
    }

    @Test
    @WikiRule(RuleId.KW_CONVERT)
    fun 转换后新实例状态重置() {
        val old = UnitInstance(CardPool.PZ_GREN, Side.P1)
        old.damage = 2
        val fresh = UnitInstance(CardPool.INF_89, Side.P1)
        assertEquals("新卡伤害归零", 0, fresh.damage)
        assertEquals("关键词按新卡计算", CardPool.INF_89.keywords, fresh.keywords)
    }

    @Test
    @WikiRule(RuleId.KW_CONVERT)
    fun 转换属于非战斗词条() {
        assertFalse(Keyword.CONVERT.isCombatKeyword)
    }

    @Test
    @WikiRule(RuleId.KW_COUNTDOWN)
    fun 倒计时在部署时记录剩余回合() {
        val e = Fixture.started()
        val card = CardPool.INF_89.copy(
            id = "u_countdown",
            keywords = setOf(Keyword.COUNTDOWN),
            countdown = 2
        )
        assertEquals(ActionResult.Ok, e.deployUnit(Side.P1, card))
        val u = e.p1.support.last()
        assertEquals("记录为 2", 2, u.countdownLeft)
    }

    @Test
    @WikiRule(RuleId.KW_COUNTDOWN)
    fun 倒计时不是战斗词条() {
        assertEquals("倒计时", Keyword.COUNTDOWN.cn)
        assertFalse(Keyword.COUNTDOWN.isCombatKeyword)
    }

    @Test
    @WikiRule(RuleId.KW_DEPLOY)
    fun 部署词条在部署时生效() {
        val e = Fixture.started()
        val card = CardPool.INF_89.copy(id = "u_deploy", keywords = setOf(Keyword.DEPLOY))
        assertEquals(ActionResult.Ok, e.deployUnit(Side.P1, card))
        assertTrue("部署卡已在场", e.p1.support.any { it.card.id == "u_deploy" })
    }

    @Test
    @WikiRule(RuleId.KW_DEPLOY)
    fun 部署效果属于非战斗词条() {
        assertFalse(Keyword.DEPLOY.isCombatKeyword)
    }

    @Test
    @WikiRule(RuleId.KW_DESTRUCTION)
    fun 亡计在单位被消灭时触发() {
        val e = Fixture.started()
        val card = CardPool.INF_89.copy(id = "u_亡计", keywords = setOf(Keyword.DESTRUCTION))
        val u = Fixture.place(e, Side.P1, card)
        e.effectDamage(u, 99)
        assertTrue("日志出现亡计", e.log.any { it.contains("触发亡计") })
    }

    @Test
    @WikiRule(RuleId.KW_DESTRUCTION)
    fun 亡计单位被消灭后离场() {
        val e = Fixture.started()
        val card = CardPool.INF_89.copy(id = "u_亡计2", keywords = setOf(Keyword.DESTRUCTION))
        val u = Fixture.place(e, Side.P1, card)
        e.effectDamage(u, 99)
        assertFalse(u in e.p1.support)
    }

    @Test
    @WikiRule(RuleId.KW_ALPINE)
    fun 山地每个其他山地给一一() {
        val e = Fixture.started()
        Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        val u = Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        e.applyAlpineBonus(u)
        assertEquals(CardPool.ALPINE_UNIT.attack + 1, u.attack)
        assertEquals(CardPool.ALPINE_UNIT.defense + 1, u.defense)
    }

    @Test
    @WikiRule(RuleId.KW_ALPINE)
    fun 两个其他山地则加二() {
        val e = Fixture.started()
        Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        val u = Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        e.applyAlpineBonus(u)
        assertEquals(CardPool.ALPINE_UNIT.attack + 2, u.attack)
    }

    @Test
    @WikiRule(RuleId.KW_DEVELOP)
    fun 开发返回三张候选取一张() {
        val e = Fixture.started()
        val before = e.p1.hand.size
        val three = e.develop(Side.P1, CardPool.all.filter { it.type == CardType.ORDER })
        assertEquals(3, three.size)
        assertEquals(before + 1, e.p1.hand.size)
    }

    @Test
    @WikiRule(RuleId.KW_DEVELOP)
    fun 开发候选来自指定卡池() {
        val e = Fixture.started()
        val pool = CardPool.all.filter { it.type == CardType.COUNTERMEASURE }
        val three = e.develop(Side.P1, pool)
        assertTrue(three.all { it.type == CardType.COUNTERMEASURE })
    }

    @Test
    @WikiRule(RuleId.KW_INTEL)
    fun 情报显示至多X张() {
        val e = Fixture.started()
        repeat(4) { e.p2.hand += HandCard(CardPool.INF_432) }
        assertEquals(2, e.intel(Side.P1, 2))
    }

    @Test
    @WikiRule(RuleId.KW_INTEL)
    fun 情报数量超过手牌时只显示实际张数() {
        val e = Fixture.started()
        e.p2.hand.clear()
        e.p2.hand += HandCard(CardPool.INF_432)
        e.p2.hand += HandCard(CardPool.INF_432)
        assertEquals("只有 2 张可看", 2, e.intel(Side.P1, 5))
    }

    @Test
    @WikiRule(RuleId.KW_FORECAST)
    fun 预报派生牌可在222取值() {
        val weather = CardPool.ORDER_DIVE.copy(id = "w_sunny", name = "晴天", cost = 2)
        assertEquals("晴天费用 2K", 2, weather.cost)
    }

    @Test
    @WikiRule(RuleId.KW_FORECAST)
    fun 预报三档与类别() {
        assertEquals("2K / 4K / 6K", listOf(2, 4, 6), listOf(2, 4, 6))
        assertFalse("预报属于非战斗词条", Keyword.FORECAST.isCombatKeyword)
    }

    @Test
    @WikiRule(RuleId.COMBAT_VS_ATTACK)
    fun 战斗词条共七种() {
        val combat = Keyword.entries.filter { it.isCombatKeyword }
        assertEquals(7, combat.size)
        assertEquals(
            setOf("伏击", "闪击", "奋战", "守护", "重甲", "冲击", "烟幕"),
            combat.map { it.cn }.toSet()
        )
    }
}