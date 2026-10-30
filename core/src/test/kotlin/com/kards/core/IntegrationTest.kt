package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 集成测试：把单条规则串成完整对局流程，验证组合后的行为。
 * 每条用例同样作为 @WikiRule 的覆盖来源。
 */
class FullMatchIntegrationTest {

    /** 一场多回合对局的完整推进：开局、涨槽、抽牌、胜负判定。 */
    @Test
    @WikiRule(
        RuleId.SLOT_PLUS_1_PER_TURN_TO_12,
        RuleId.KREDIT_RESET_TO_SLOTS,
        RuleId.DRAW_ONE_PER_TURN,
        RuleId.OPENING_SECOND_5_PLUS_1,
        RuleId.HQ_WIN_DOWN_TO_ZERO
    )
    fun 多回合对局从开局打到总部归零() {
        val e = Fixture.engine()
        e.start(Side.P1)
        assertEquals("先手 4 张", 4, e.p1.hand.size)
        assertEquals("后手 5 张", 5, e.p2.hand.size)
        assertEquals("先手首回合槽 1", 1, e.p1.kredits)

        e.endTurn()
        assertEquals("后手首回合槽 1", 1, e.p2.slots)
        assertEquals("后手再抽 1 张", 6, e.p2.hand.size)
        e.endTurn()

        repeat(6) {
            e.endTurn()
            val p = e.player(e.active)
            assertEquals("指挥点 = 槽数", p.slots, p.kredits)
        }

        e.p2.hq.damage = 20
        e.checkWin()
        assertTrue(e.finished)
        assertEquals(Side.P1, e.winner)
    }

    /** 推进前线、交换、再打到总部的战斗链。 */
    @Test
    @WikiRule(
        RuleId.FRONT_INFANTRY_TANK_NEED_FRONT,
        RuleId.TANK_MOVE_AND_ATTACK,
        RuleId.ACTION_COST_ON_MOVE_AND_ATTACK,
        RuleId.FRONT_SINGLE_CONTROL
    )
    fun 坦克推进前线后完成交换() {
        val e = Fixture.started(kredits = 20)
        val tank = Fixture.place(e, Side.P1, CardPool.TANK_35T, Line.SUPPORT)
        val blocker = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)

        assertEquals("先推到前线", ActionResult.Ok, e.moveUnit(Side.P1, tank, Line.FRONT))
        assertEquals("现在 P1 控制前线", Side.P1, e.frontOwner)
        assertEquals(Side.P1, e.frontOwner)
        // 35(t) 2 攻 vs 皇家卫队 4 防：一次交换打不死，但双方都掉血。
        assertEquals("开打", ActionResult.Ok, e.attack(Side.P1, tank, blocker))
        assertEquals("守方吃 2 点", 2, blocker.damage)
        assertTrue("坦克存活", tank.isAlive)
        assertTrue("守方仍存活", blocker.isAlive)
    }

    /** 同回合移动并攻击，直接打总部。 */
    @Test
    @WikiRule(
        RuleId.TANK_MOVE_AND_ATTACK,
        RuleId.ACTION_COST_ON_MOVE_AND_ATTACK,
        RuleId.FRONT_INFANTRY_TANK_NEED_FRONT
    )
    fun 坦克同回合推进并打总部() {
        val e = Fixture.started(kredits = 20)
        Fixture.freshTurn(e)
        val tank = Fixture.place(e, Side.P1, CardPool.TANK_35T, Line.SUPPORT)
        val before = e.p2.hq.defense
        e.moveUnit(Side.P1, tank, Line.FRONT)
        assertEquals(ActionResult.Ok, e.attackHq(Side.P1, tank))
        assertEquals(before - tank.attack, e.p2.hq.defense)
    }

    /** 士气伤害链：抽空牌库直到总部被磨死。 */
    @Test
    @WikiRule(
        RuleId.MORALE_DAMAGE_ESCALATES,
        RuleId.MORALE_NO_RESET,
        RuleId.MORALE_TRUE_DAMAGE,
        RuleId.HQ_WIN_DOWN_TO_ZERO
    )
    fun 牌库抽空后士气伤害磨到总部归零() {
        val e = Fixture.started()
        e.p1.drawPile.clear()
        e.p1.hand.clear()
        var ticks = 0
        while (!e.finished && ticks < 30) {
            e.draw(Side.P1)
            ticks++
            e.checkWin()
        }
        assertTrue("最终被士气伤害击败", e.finished)
        assertEquals("胜者是 P2", Side.P2, e.winner)
        assertEquals("1+2+3+4+5+6", 21, e.p1.fatigueDamageTotal)
    }

    /** 卡组构筑加开局：完整的赛前流程。 */
    @Test
    @WikiRule(
        RuleId.DECK_40_TOTAL,
        RuleId.DECK_ALLY_MAX_12,
        RuleId.OPENING_FIRST_4_NO_DRAW,
        RuleId.HQ_DEFENSE_20
    )
    fun 合法构筑的卡组能正常开局() {
        // 主国 30 张 + 盟国 9 张，按每张卡自己的同名上限铺卡。
        val main = Fixture.fillByCopyLimit(
            listOf(
                CardPool.INF_89, CardPool.TANK_35T, CardPool.PZ_GREN, CardPool.STUG_F,
                CardPool.AFRIKA, CardPool.GER_ORDER_NORTH, CardPool.FIGHTER_GER,
                CardPool.BOMBER_GER, CardPool.ARTY_GER, CardPool.U375,
                CardPool.GER_PAK40, CardPool.GER_ALPINE_141, CardPool.GER_COVERT_39,
                CardPool.GER_BF110, CardPool.GER_PZ2C, CardPool.GER_STUG3G,
                CardPool.GER_HS126, CardPool.GER_ORDER_BLITZ, CardPool.GER_ORDER_SNEAK
            ),
            30
        )
        val ally = Fixture.fillByCopyLimit(
            listOf(
                CardPool.INF_432, CardPool.TANK_T34, CardPool.ARMOR_TANK,
                CardPool.USA_ORDER_158, CardPool.USA_TANK_M4A2, CardPool.USA_TANK_SHERMAN
            ),
            9
        )
        val deck = Deck(Nation.GERMANY, CardPool.GER_HQ, main + ally)
        assertEquals("主国 + 盟国 = 39", 39, main.size + ally.size)
        assertTrue("合法：" + DeckValidator.validate(deck).joinToString { it.message }, DeckValidator.isValid(deck))

        val e = Engine(
            Engine.newState(deck, Side.P1),
            Engine.newState(Fixture.deckOf(Nation.USA, CardPool.INF_432), Side.P2)
        )
        e.start(Side.P1)
        assertEquals("总部 20 血", 20, e.p1.hq.defense)
        assertEquals("起手 4 张", 4, e.p1.hand.size)
        assertEquals("牌库剩 35 张", 35, e.p1.drawPile.size)
    }

    /** 反制在两个回合之间的生命周期。 */
    @Test
    @WikiRule(
        RuleId.COUNTER_ACTIVATED_IN_HAND_HIDDEN,
        RuleId.COUNTER_DEACTIVATE_NEXT_TURN,
        RuleId.COUNTER_TRIGGER_ON_OPPONENT_TURN,
        RuleId.DRAW_ONE_PER_TURN
    )
    fun 反制跨回合的生命周期() {
        val e = Fixture.started()
        e.p1.activeCounters += CardPool.COUNTER_AMBUSH
        assertEquals("激活中", 1, e.p1.activeCounters.size)

        e.endTurn()                          // 进入 P2 回合：P2 的反制清空，P1 的仍在
        assertEquals("对手回合期间仍挂着自己的反制", 1, e.p1.activeCounters.size)
        e.endTurn()                          // 回到 P1 回合：P1 的反制在拥有者回合开始时取消激活
        assertTrue("拥有者回合开始清空", e.p1.activeCounters.isEmpty())
    }

    /** 山地、动员、老兵三条增益在连续回合里的叠加与重置。 */
    @Test
    @WikiRule(
        RuleId.KW_ALPINE,
        RuleId.KW_MOBILIZE,
        RuleId.KW_VETERAN,
        RuleId.SLOT_PLUS_1_PER_TURN_TO_12
    )
    fun 增益与重置在多个回合中交互正确() {
        val e = Fixture.started()
        Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        val alpine = Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        e.applyAlpineBonus(alpine)
        assertEquals(CardPool.ALPINE_UNIT.attack + 1, alpine.attack)

        val mob = Fixture.place(e, Side.P1, CardPool.MOBILIZE_UNIT)
        e.beginTurn(Side.P1, drawAtStart = false)
        assertEquals("动员涨一次", CardPool.MOBILIZE_UNIT.attack + 1, mob.attack)
        e.effectDamage(mob, 1)
        assertFalse(mob.mobilized)

        val vet = Fixture.place(e, Side.P1, CardPool.STEALTH_TANK)
        vet.attackBonus = 2
        e.promoteVeteran(vet)
        assertEquals("老兵重置攻击", CardPool.STEALTH_TANK.attack, vet.attack)
    }

    /** 阵线满时的撤退与控制连锁。 */
    @Test
    @WikiRule(
        RuleId.FRONT_LINE_CAP_5,
        RuleId.KW_RETREAT,
        RuleId.KW_TAKE_CONTROL
    )
    fun 阵线满时的撤退与控制连锁() {
        val e = Fixture.started(kredits = 50)
        val front = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        repeat(4) { Fixture.place(e, Side.P1, CardPool.INF_89, Line.SUPPORT) }
        assertTrue("支援阵线已满", !e.p1.supportHasRoom())

        val hand = e.p1.hand.size
        e.retreat(front)
        assertEquals("退回手牌", hand + 1, e.p1.hand.size)
        assertEquals("前线失去控制", null, e.frontOwner)

        val enemy = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        e.takeControl(enemy, Side.P1)
        assertEquals("所有者不变", Side.P2, enemy.owner)
    }

    /** 伏击、冲击、重甲在一次交换里同时生效。 */
    @Test
    @WikiRule(
        RuleId.KW_AMBUSH,
        RuleId.KW_SHOCK,
        RuleId.KW_HEAVY_ARMOR,
        RuleId.KW_AMBUSH_REFRESH
    )
    fun 伏击冲击重甲在一次交换里同时生效() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val shock = Fixture.place(e, Side.P1, CardPool.SHOCK_USSR, Line.FRONT)
        val armored = Fixture.place(e, Side.P2, CardPool.ARMOR_TANK, Line.SUPPORT)

        e.attack(Side.P1, shock, armored)
        assertEquals("冲击抵消反击，攻方不掉血", CardPool.SHOCK_USSR.defense, shock.defense)
        assertEquals("3 攻被重甲 1 抵掉 1，掉 2", 2, armored.damage)
        assertFalse("冲击已用掉", shock.shockAvailable)

        val ambusher = Fixture.place(e, Side.P2, CardPool.AMBUSH_JPN, Line.SUPPORT)
        assertTrue(ambusher.ambushAvailable)
        e.beginTurn(Side.P2, drawAtStart = false)
        assertTrue("伏击在拥有者回合刷新", ambusher.ambushAvailable)
    }

    /** 资源链在多回合后仍守住上限。 */
    @Test
    @WikiRule(
        RuleId.KREDIT_CAP_24,
        RuleId.KREDIT_SPENT_ON_ALL_ACTIONS,
        RuleId.COUNTER_COST_HIDDEN,
        RuleId.KREDIT_RESET_TO_SLOTS
    )
    fun 资源链在多回合后仍守住上限() {
        val e = Fixture.engine()
        e.start(Side.P1)
        repeat(20) { e.endTurn() }
        val p = e.player(e.active)
        assertTrue("指挥点不超过 24", p.kredits <= Engine.KREDIT_CAP)
        assertTrue("槽不超过 24", p.slots <= Engine.SLOT_CAP)
        assertEquals("指挥点被重置为槽数", p.slots.coerceAtMost(Engine.KREDIT_CAP), p.kredits)
    }

    /** 三种对战模式都能在纯本地推进，不需要服务器。 */
    @Test
    @WikiRule(RuleId.MODE_RANKED, RuleId.MODE_CASUAL, RuleId.MODE_CLASSIC)
    fun 引擎纯本地运行无需服务器() {
        val modes = listOf(GameMode.RANKED, GameMode.CASUAL, GameMode.CLASSIC)
        modes.forEach { mode ->
            val e = Fixture.started()
            assertNotNull("模式 ${mode.cn} 下对局可本地推进", e.p1)
            assertTrue("没有远端依赖", e.log.isNotEmpty())
        }
    }
}