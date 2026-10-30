package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 战斗词条：伏击、闪击、奋战、守护、重甲、冲击、烟幕。来源：《词条》。 */
class CombatKeywordTest {

    @Test
    @WikiRule(RuleId.KW_AMBUSH)
    fun 伏击首次被攻击先造成反击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val attacker = Fixture.place(e, Side.P1, CardPool.AFRIKA, Line.FRONT)
        val ambusher = Fixture.place(e, Side.P2, CardPool.AMBUSH_JPN, Line.SUPPORT)
        e.attack(Side.P1, attacker, ambusher)
        assertTrue("攻方先吃了伏击伤害", attacker.damage >= CardPool.AMBUSH_JPN.attack)
        assertFalse("伏击已用掉", ambusher.ambushAvailable)
    }

    @Test
    @WikiRule(RuleId.KW_AMBUSH)
    fun 小单位可以先探掉伏击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val ambusher = Fixture.place(e, Side.P2, CardPool.AMBUSH_JPN, Line.SUPPORT)
        val probe = Fixture.place(e, Side.P1, CardPool.INF_89, Line.FRONT)
        e.attack(Side.P1, probe, ambusher)
        assertFalse("伏击已被小单位消耗", ambusher.ambushAvailable)
    }

    @Test
    @WikiRule(RuleId.KW_AMBUSH_REFRESH)
    fun 伏击在拥有者下回合开始时恢复() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val ambusher = Fixture.place(e, Side.P2, CardPool.AMBUSH_JPN, Line.SUPPORT)
        ambusher.ambushAvailable = false
        e.beginTurn(Side.P2, drawAtStart = false)
        assertTrue("伏击重新可用", ambusher.ambushAvailable)
    }

    @Test
    @WikiRule(RuleId.KW_AMBUSH_REFRESH, RuleId.KW_AMBUSH)
    fun 伏击只在拥有者回合刷新() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val ambusher = Fixture.place(e, Side.P2, CardPool.AMBUSH_JPN, Line.SUPPORT)
        ambusher.ambushAvailable = false
        e.beginTurn(Side.P1, drawAtStart = false)
        assertFalse("对手回合不刷新", ambusher.ambushAvailable)
    }

    @Test
    @WikiRule(RuleId.KW_BLITZ)
    fun 闪击单位入场当回合即可行动() {
        val e = Fixture.started(kredits = 20)
        val blitz = Fixture.place(e, Side.P1, CardPool.BLITZ_JPN, Line.FRONT, enteredThisTurn = true)
        val target = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, blitz, Target.EnemyUnit(target)))
    }

    @Test
    @WikiRule(RuleId.KW_BLITZ)
    fun 无闪击的非坦克入场当回合不能行动() {
        val e = Fixture.started()
        val inf = Fixture.place(e, Side.P1, CardPool.INF_89, Line.FRONT, enteredThisTurn = true)
        val target = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        val r = e.canAttack(Side.P1, inf, Target.EnemyUnit(target))
        assertTrue(r is ActionResult.Rejected)
        assertTrue((r as ActionResult.Rejected).reason.contains("闪击"))
    }

    @Test
    @WikiRule(RuleId.KW_FURY)
    fun 奋战一回合可攻击两次() {
        val e = Fixture.started(kredits = 20)
        Fixture.freshTurn(e)
        val fury = Fixture.place(e, Side.P1, CardPool.FURY_USSR, Line.FRONT)
        val t1 = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        val t2 = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        assertEquals(ActionResult.Ok, e.attack(Side.P1, fury, t1))
        assertEquals(1, fury.attacksThisTurn)
        assertEquals(ActionResult.Ok, e.attack(Side.P1, fury, t2))
        assertEquals(2, fury.attacksThisTurn)
    }

    @Test
    @WikiRule(RuleId.KW_FURY)
    fun 第三次攻击被拒绝() {
        val e = Fixture.started(kredits = 30)
        Fixture.freshTurn(e)
        val fury = Fixture.place(e, Side.P1, CardPool.FURY_USSR, Line.FRONT)
        repeat(2) { assertEquals(ActionResult.Ok, e.attackHq(Side.P1, fury)) }
        val third = e.attackHq(Side.P1, fury)
        assertTrue(third is ActionResult.Rejected)
        assertTrue((third as ActionResult.Rejected).reason.contains("次数"))
    }

    @Test
    @WikiRule(RuleId.KW_FURY, RuleId.ACTION_COST_ON_MOVE_AND_ATTACK)
    fun 奋战两次攻击各付行动花费() {
        val e = Fixture.started(kredits = 30)
        Fixture.freshTurn(e)
        val fury = Fixture.place(e, Side.P1, CardPool.FURY_USSR, Line.FRONT)
        val before = e.p1.kredits
        e.attackHq(Side.P1, fury)
        e.attackHq(Side.P1, fury)
        assertEquals("两次各付一次", before - 2 * fury.actionCost, e.p1.kredits)
    }

    @Test
    @WikiRule(RuleId.KW_GUARD)
    fun 守护保护相邻单位() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val atk = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        val guard = Fixture.place(e, Side.P2, CardPool.GUARD_UK, Line.SUPPORT)
        val sheltered = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        // 守护与目标必须相邻：guard(0) 与 sheltered(1) 相邻。
        assertEquals(1, e.p2.support.indexOf(sheltered) - e.p2.support.indexOf(guard))
        val r = e.canAttack(Side.P1, atk, Target.EnemyUnit(sheltered))
        assertTrue(r is ActionResult.Rejected)
        assertTrue((r as ActionResult.Rejected).reason.contains("守护"))
    }

    @Test
    @WikiRule(RuleId.KW_GUARD)
    fun 守护单位自身不被保护() {
        val e = Fixture.started()
        val g1 = Fixture.place(e, Side.P2, CardPool.GUARD_UK, Line.SUPPORT)
        Fixture.place(e, Side.P2, CardPool.GUARD_UNIT_2, Line.SUPPORT)
        assertFalse("守护不能互相守护", e.isGuardedBy(e.p2, Line.SUPPORT, g1))
    }

    @Test
    @WikiRule(RuleId.KW_GUARD)
    fun 不相邻的单位不受守护() {
        val e = Fixture.started()
        val guard = Fixture.place(e, Side.P2, CardPool.GUARD_UK, Line.SUPPORT)
        Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        val far = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        assertFalse("隔了两个位置，不受保护", e.isGuardedBy(e.p2, Line.SUPPORT, far))
        assertTrue(guard.has(Keyword.GUARD))
    }

    @Test
    @WikiRule(RuleId.KW_HEAVY_ARMOR)
    fun 重甲减免对战伤害() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val attacker = Fixture.place(e, Side.P1, CardPool.AFRIKA, Line.FRONT)
        val armored = Fixture.place(e, Side.P2, CardPool.ARMOR_TANK, Line.SUPPORT)
        e.attack(Side.P1, attacker, armored)
        assertEquals("4 点攻击被重甲 2 抵掉 2", 2, armored.damage)
    }

    @Test
    @WikiRule(RuleId.KW_HEAVY_ARMOR)
    fun 重甲不减免指令伤害() {
        val e = Fixture.started()
        val armored = Fixture.place(e, Side.P2, CardPool.ARMOR_TANK)
        e.effectDamage(armored, 3)
        assertEquals("效果伤害全吃", 3, armored.damage)
    }

    @Test
    @WikiRule(RuleId.KW_HEAVY_ARMOR)
    fun 重甲最多三点() {
        var threw = false
        try {
            CardPool.INF_89.copy(id = "bad", armor = 4)
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("超过 3 应被拒绝", threw)
    }

    @Test
    @WikiRule(RuleId.KW_SHOCK)
    fun 冲击首击不吃反击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val shock = Fixture.place(e, Side.P1, CardPool.SHOCK_USSR, Line.FRONT)
        val target = Fixture.place(e, Side.P2, CardPool.PZ_GREN, Line.SUPPORT)
        e.attack(Side.P1, shock, target)
        assertEquals("攻方无反击伤害", CardPool.SHOCK_USSR.defense, shock.defense)
    }

    @Test
    @WikiRule(RuleId.KW_SHOCK)
    fun 攻击总部不会移除冲击() {
        val e = Fixture.started(kredits = 20)
        Fixture.freshTurn(e)
        val shock = Fixture.place(e, Side.P1, CardPool.SHOCK_USSR, Line.FRONT)
        e.attackHq(Side.P1, shock)
        assertTrue("冲击仍在", shock.shockAvailable)
    }

    @Test
    @WikiRule(RuleId.KW_SHOCK)
    fun 攻击单位后移除冲击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val shock = Fixture.place(e, Side.P1, CardPool.SHOCK_USSR, Line.FRONT)
        val target = Fixture.place(e, Side.P2, CardPool.GUARD_UNIT_2, Line.SUPPORT)
        e.attack(Side.P1, shock, target)
        assertFalse("攻击单位后失去冲击", shock.shockAvailable)
    }

    @Test
    @WikiRule(RuleId.KW_SMOKE)
    fun 烟幕单位不能被单位攻击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val atk = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        val smoke = Fixture.place(e, Side.P2, CardPool.SMOKE_UNIT, Line.SUPPORT)
        val r = e.canAttack(Side.P1, atk, Target.EnemyUnit(smoke))
        assertTrue(r is ActionResult.Rejected)
        assertTrue((r as ActionResult.Rejected).reason.contains("烟幕"))
    }

    @Test
    @WikiRule(RuleId.KW_SMOKE)
    fun 移动后失去烟幕() {
        val e = Fixture.started(kredits = 20)
        val u = Fixture.place(e, Side.P1, CardPool.SMOKE_UNIT, Line.SUPPORT)
        assertTrue(u.smokeActive)
        e.moveUnit(Side.P1, u, Line.FRONT)
        assertFalse(u.smokeActive)
    }

    @Test
    @WikiRule(RuleId.KW_SMOKE)
    fun 烟幕挡不住指令伤害() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P2, CardPool.SMOKE_UNIT)
        e.effectDamage(u, 2)
        assertEquals("指令照样生效", 2, u.damage)
    }
}