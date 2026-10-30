package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 集成测试第三组：补齐「每条规则都要有跨模块的集成路径」这一要求。
 * 用例刻意跨越多个子系统（资源、前线、兵种、词条、卡组、模式）。
 */
class RuleCoverageIntegrationTest {

    @Test
    @WikiRule(
        RuleId.SLOT_CAP_24,
        RuleId.KREDIT_CAP_24,
        RuleId.CARD_FOUR_TYPES,
        RuleId.UNIT_FIVE_TYPES
    )
    fun 资源顶格与单位类型枚举联动的终局场景() {
        val e = Fixture.started(kredits = 24)
        e.modifySlots(Side.P1, 100)
        assertEquals("槽顶格 24", 24, e.p1.slots)
        assertEquals(ActionResult.Ok, e.deployUnit(Side.P1, CardPool.ELITE_GER))
        assertEquals(
            "指挥点按卡面费用扣除",
            24 - CardPool.ELITE_GER.cost,
            e.p1.kredits
        )
        assertEquals("四类卡", 4, CardType.entries.size)
        assertEquals("五类单位", 5, UnitType.entries.size)
    }

    @Test
    @WikiRule(
        RuleId.ORDER_ONE_SHOT,
        RuleId.COUNTER_PRIORITY_OVER_ORDER,
        RuleId.COUNTER_TRIGGER_ON_OPPONENT_TURN,
        RuleId.KW_DESTRUCTION
    )
    fun 指令与反制在回合里的先后关系() {
        val e = Fixture.started(kredits = 20)
        assertTrue(e.deployUnit(Side.P1, CardPool.ORDER_BOMB) is ActionResult.Rejected)
        e.p1.activeCounters += CardPool.COUNTER_ELITE
        assertEquals(Side.P2, Side.P1.opponent)
        val doomed = CardPool.INF_89.copy(id = "u_doomed", keywords = setOf(Keyword.DESTRUCTION))
        val u = Fixture.place(e, Side.P1, doomed)
        e.effectDamage(u, 99)
        assertTrue(e.log.any { it.contains("触发亡计") })
    }

    @Test
    @WikiRule(
        RuleId.DECK_ALLY_NO_ELITE,
        RuleId.NATION_ALLY_ONLY_NO_ELITE,
        RuleId.DECK_REQUIRES_MAIN_NATION,
        RuleId.NATION_FIVE_MAJOR
    )
    fun 盟国精英卡在构筑阶段就被拦住() {
        val cards = MutableList(39) { i -> CardPool.INF_89.copy(id = "u_$i") }
        cards[0] = CardPool.ELITE_USA
        val deck = Deck(Nation.GERMANY, CardPool.GER_HQ, cards)
        val rules = DeckValidator.validate(deck).map { it.rule }
        assertTrue("盟国带精英被拒", rules.contains(RuleId.DECK_ALLY_NO_ELITE))
        assertTrue(Nation.entries.filter { !it.canBeMain }.none { it.hasElite })
    }

    @Test
    @WikiRule(
        RuleId.MORALE_DAMAGE_ON_EMPTY_DRAW,
        RuleId.DRAW_ONE_PER_TURN,
        RuleId.HQ_WIN_DOWN_TO_ZERO,
        RuleId.HAND_CAP_9_BURN
    )
    fun 抽空牌库后按回合抽牌触发士气伤害并败北() {
        val e = Fixture.started()
        e.p1.drawPile.clear()
        e.p1.hand.clear()
        e.beginTurn(Side.P1, drawAtStart = true)
        assertEquals("第一次士气伤害 1", 1, e.p1.fatigueDamageTotal)

        while (e.p1.hand.size < Engine.HAND_CAP) e.p1.hand += HandCard(CardPool.INF_89)
        val before = e.p1.fatigueDamageTotal
        e.draw(Side.P1)
        assertEquals("爆牌不触发士气", before, e.p1.fatigueDamageTotal)

        e.p1.hq.damage = 20
        e.checkWin()
        assertEquals(Side.P2, e.winner)
    }

    @Test
    @WikiRule(
        RuleId.BOMBER_NO_RETAL_WHEN_ATTACKED,
        RuleId.BOMBER_IGNORE_GUARD,
        RuleId.FIGHTER_INTERCEPT_BOMBER,
        RuleId.FIGHTER_NO_SMOKE
    )
    fun 轰炸机在守护与拦截之间被两边夹击() {
        val e = Fixture.started(kredits = 40)
        Fixture.freshTurn(e)

        val bomber = Fixture.place(e, Side.P1, CardPool.BOMBER_GER, Line.FRONT)
        val guard = Fixture.place(e, Side.P2, CardPool.GUARD_UK, Line.SUPPORT)
        val sheltered = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, bomber, Target.EnemyUnit(sheltered)))

        val attacker = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        val enemyBomber = Fixture.place(e, Side.P2, CardPool.BOMBER_GER, Line.SUPPORT)
        e.attack(Side.P1, attacker, enemyBomber)
        assertEquals("攻击轰炸机不受反击", CardPool.PZ_GREN.defense, attacker.defense)

        Fixture.place(e, Side.P2, CardPool.FIGHTER_GER, Line.SUPPORT)
        val blocked = e.canAttack(Side.P1, bomber, Target.EnemyUnit(sheltered))
        assertTrue("有战斗机就拦截", blocked is ActionResult.Rejected)
        assertFalse("战斗机不能带烟幕", guard.has(Keyword.SMOKE))
    }

    @Test
    @WikiRule(
        RuleId.KW_FORECAST,
        RuleId.KW_DEVELOP,
        RuleId.KW_INTEL,
        RuleId.KW_COUNTDOWN
    )
    fun 预报开发情报在一条信息链上() {
        val e = Fixture.started()
        val weather = CardPool.ORDER_DIVE.copy(id = "w_storm", name = "风暴", cost = 6)
        assertEquals("天气预报 6K 档", 6, weather.cost)

        val three = e.develop(Side.P1, CardPool.all.filter { it.type == CardType.UNIT })
        assertEquals(3, three.size)

        repeat(3) { e.p2.hand += HandCard(CardPool.INF_432) }
        assertEquals("情报 2 张", 2, e.intel(Side.P1, 2))

        val cd = Fixture.place(
            e, Side.P1,
            CardPool.INF_89.copy(id = "u_cd2", keywords = setOf(Keyword.COUNTDOWN), countdown = 3)
        )
        cd.countdownLeft = 3
        assertEquals(3, cd.countdownLeft)
    }

    @Test
    @WikiRule(
        RuleId.KW_VETERAN,
        RuleId.KW_SALVAGE,
        RuleId.KW_TAKE_CONTROL,
        RuleId.KW_RETREAT
    )
    fun 身份与归属的三次改变() {
        val e = Fixture.started(kredits = 50)

        val vet = Fixture.place(e, Side.P1, CardPool.STEALTH_TANK)
        e.promoteVeteran(vet)
        assertTrue(vet.veteran)

        val dead = UnitInstance(CardPool.ARMOR_TANK, Side.P2)
        val handBefore = e.p1.hand.size
        e.salvage(Side.P1, dead, Nation.GERMANY)
        assertEquals(handBefore + 1, e.p1.hand.size)

        val captured = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        e.takeControl(captured, Side.P1)
        assertEquals("所有者不变", Side.P2, captured.owner)

        val front = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        e.retreat(front)
        assertEquals(Line.SUPPORT, front.line)
    }

    @Test
    @WikiRule(
        RuleId.KW_SMOKE,
        RuleId.KW_SHOCK,
        RuleId.KW_FURY,
        RuleId.KW_AMBUSH_REFRESH
    )
    fun 消耗型词条各自按自己的时机失效() {
        val e = Fixture.started(kredits = 60)
        Fixture.freshTurn(e)

        val smoke = Fixture.place(e, Side.P1, CardPool.SMOKE_UNIT, Line.SUPPORT)
        assertTrue(smoke.smokeActive)
        e.moveUnit(Side.P1, smoke, Line.FRONT)
        assertFalse("移动后失去烟幕", smoke.smokeActive)

        val shock = Fixture.place(e, Side.P1, CardPool.SHOCK_USSR, Line.FRONT)
        val enemy = Fixture.place(e, Side.P2, CardPool.GUARD_UNIT_2, Line.SUPPORT)
        e.attack(Side.P1, shock, enemy)
        assertFalse("攻击单位后失去冲击", shock.shockAvailable)

        // 守护单位会保护总部，先清场再打。
        e.p2.support.clear()
        val fury = Fixture.place(e, Side.P1, CardPool.FURY_USSR, Line.FRONT)
        assertEquals(ActionResult.Ok, e.attackHq(Side.P1, fury))
        assertEquals("第一击", 1, fury.attacksThisTurn)
        assertEquals(ActionResult.Ok, e.attackHq(Side.P1, fury))
        assertEquals("奋战两次", 2, fury.attacksThisTurn)
    }

    @Test
    @WikiRule(
        RuleId.KW_ALPINE,
        RuleId.KW_MOBILIZE,
        RuleId.KW_SUPPRESS,
        RuleId.KW_REPAIR
    )
    fun 增益叠起来再被一次抑制清空() {
        val e = Fixture.started()

        Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        val alpine = Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        e.applyAlpineBonus(alpine)
        assertTrue("山地加成生效", alpine.attack > CardPool.ALPINE_UNIT.attack)

        val mob = Fixture.place(e, Side.P1, CardPool.MOBILIZE_UNIT)
        e.beginTurn(Side.P1, drawAtStart = false)
        assertTrue("动员生效", mob.attack > CardPool.MOBILIZE_UNIT.attack)

        e.suppress(alpine)
        assertEquals("抑制把山地加成清零", CardPool.ALPINE_UNIT.attack, alpine.attack)

        alpine.damage = 1
        e.repair(alpine)
        assertEquals("修复补满", alpine.maxDefense, alpine.defense)
    }

    @Test
    @WikiRule(
        RuleId.FRONT_SINGLE_CONTROL,
        RuleId.FRONT_LINE_CAP_5,
        RuleId.INFANTRY_ADJACENT_ONLY,
        RuleId.FRONT_INFANTRY_TANK_NEED_FRONT
    )
    fun 战场的空间约束一起生效() {
        val e = Fixture.started(kredits = 80)

        val tank = Fixture.place(e, Side.P1, CardPool.TANK_35T, Line.SUPPORT)
        assertEquals(ActionResult.Ok, e.moveUnit(Side.P1, tank, Line.FRONT))
        assertEquals(Side.P1, e.frontOwner)

        e.beginTurn(Side.P2, drawAtStart = false)
        assertTrue(e.deployUnit(Side.P2, CardPool.INF_432, Line.FRONT) is ActionResult.Rejected)

        e.beginTurn(Side.P1, drawAtStart = false)
        repeat(4) { e.deployUnit(Side.P1, CardPool.INF_89) }
        assertTrue(e.deployUnit(Side.P1, CardPool.INF_89) is ActionResult.Rejected)

        Fixture.freshTurn(e)
        val backInf = Fixture.place(e, Side.P1, CardPool.INF_89, Line.SUPPORT)
        val enemyBack = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        assertTrue(e.canAttack(Side.P1, backInf, Target.EnemyUnit(enemyBack)) is ActionResult.Rejected)
    }
}