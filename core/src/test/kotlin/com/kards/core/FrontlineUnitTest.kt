package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** 阵线与前线。来源：《卡牌》《基本机制》。 */
class FrontlineTest {

    @Test
    @WikiRule(RuleId.FRONT_SINGLE_CONTROL)
    fun 前线同时只能一方控制() {
        val e = Fixture.started(kredits = 20)
        Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        assertEquals("P1 控制前线", Side.P1, e.frontOwner)
        e.beginTurn(Side.P2, drawAtStart = false)
        val r = e.deployUnit(Side.P2, CardPool.INF_432, Line.FRONT)
        assertTrue("P2 无法进入被对手控制的前线", r is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.FRONT_SINGLE_CONTROL)
    fun 前线空出后对手可夺取() {
        val e = Fixture.started(kredits = 20)
        val mine = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        e.p1.front.remove(mine)
        e.frontOwner = null
        e.beginTurn(Side.P2, drawAtStart = false)
        assertEquals(ActionResult.Ok, e.deployUnit(Side.P2, CardPool.INF_432, Line.FRONT))
        assertEquals("P2 接管前线", Side.P2, e.frontOwner)
    }

    @Test
    @WikiRule(RuleId.FRONT_LINE_CAP_5)
    fun 支援阵线含总部共五个位置() {
        val e = Fixture.started(kredits = 50)
        repeat(4) { assertEquals(ActionResult.Ok, e.deployUnit(Side.P1, CardPool.INF_89)) }
        val fifth = e.deployUnit(Side.P1, CardPool.INF_89)
        assertTrue("第五个单位放不下", fifth is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.FRONT_LINE_CAP_5)
    fun 阵线容量常量与规则一致() {
        assertEquals("每条阵线 5 个位置", 5, Engine.LINE_CAP)
        assertEquals("支援阵线含总部，故单位位 4", 4, Engine.LINE_CAP - 1)
    }

    @Test
    @WikiRule(RuleId.FRONT_INFANTRY_TANK_NEED_FRONT)
    fun 步兵在支援阵线打不到敌方总部() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val inf = Fixture.place(e, Side.P1, CardPool.INF_89, Line.SUPPORT)
        val r = e.canAttack(Side.P1, inf, Target.EnemyHq(Side.P2))
        assertTrue(r is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.FRONT_INFANTRY_TANK_NEED_FRONT)
    fun 坦克推到前线后可攻击总部() {
        val e = Fixture.started(kredits = 20)
        Fixture.freshTurn(e)
        val tank = Fixture.place(e, Side.P1, CardPool.TANK_35T, Line.SUPPORT)
        e.moveUnit(Side.P1, tank, Line.FRONT)
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, tank, Target.EnemyHq(Side.P2)))
        val before = e.p2.hq.defense
        e.attackHq(Side.P1, tank)
        assertEquals("总部掉等同攻击力的血", before - tank.attack, e.p2.hq.defense)
    }

    @Test
    @WikiRule(RuleId.ACTION_COST_ON_MOVE_AND_ATTACK)
    fun 移动与攻击分别计价() {
        val e = Fixture.started(kredits = 10)
        Fixture.freshTurn(e)
        val u = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.SUPPORT)
        val before = e.p1.kredits
        assertEquals(ActionResult.Ok, e.moveUnit(Side.P1, u, Line.FRONT))
        assertEquals("移动扣一次行动花费", before - u.actionCost, e.p1.kredits)
        val mid = e.p1.kredits
        e.attackHq(Side.P1, u)
        assertEquals(mid - u.actionCost, e.p1.kredits)
    }

    @Test
    @WikiRule(RuleId.ACTION_COST_ON_MOVE_AND_ATTACK)
    fun 行动花费高的单位扣得更多() {
        val e = Fixture.started(kredits = 20)
        Fixture.freshTurn(e)
        val heavy = Fixture.place(e, Side.P1, CardPool.FURRY_TANK, Line.SUPPORT)
        val before = e.p1.kredits
        e.moveUnit(Side.P1, heavy, Line.FRONT)
        assertEquals("按卡面行动花费扣费", before - CardPool.FURRY_TANK.actionCost, e.p1.kredits)
    }
}

/** 兵种。来源：《卡牌》《词条》。 */
class UnitTypeTest {

    @Test
    @WikiRule(RuleId.INFANTRY_ADJACENT_ONLY)
    fun 步兵只能打相邻阵线() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val inf = Fixture.place(e, Side.P1, CardPool.INF_89, Line.SUPPORT)
        val back = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        val r = e.canAttack(Side.P1, inf, Target.EnemyUnit(back))
        assertTrue("支援打支援跨了阵线", r is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.INFANTRY_ADJACENT_ONLY)
    fun 步兵在前线可打敌方前排与后排() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val inf = Fixture.place(e, Side.P1, CardPool.INF_89, Line.FRONT)
        val back = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        val front = Fixture.place(e, Side.P2, CardPool.INF_432, Line.FRONT)
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, inf, Target.EnemyUnit(back)))
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, inf, Target.EnemyUnit(front)))
    }

    @Test
    @WikiRule(RuleId.TANK_MOVE_AND_ATTACK)
    fun 坦克可同回合移动并攻击() {
        val e = Fixture.started(kredits = 20)
        Fixture.freshTurn(e)
        val tank = Fixture.place(e, Side.P1, CardPool.TANK_35T, Line.SUPPORT)
        val target = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        assertEquals(ActionResult.Ok, e.moveUnit(Side.P1, tank, Line.FRONT))
        assertEquals(ActionResult.Ok, e.attack(Side.P1, tank, target))
    }

    @Test
    @WikiRule(RuleId.TANK_MOVE_AND_ATTACK)
    fun 坦克可先攻击后移动() {
        val e = Fixture.started(kredits = 20)
        Fixture.freshTurn(e)
        val tank = Fixture.place(e, Side.P1, CardPool.TANK_35T, Line.FRONT)
        e.attackHq(Side.P1, tank)
        assertEquals("坦克攻击后仍可移动", ActionResult.Ok, e.moveUnit(Side.P1, tank, Line.SUPPORT))
    }

    @Test
    @WikiRule(RuleId.FIGHTER_ANY_LINE)
    fun 战斗机不受阵线限制() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val f = Fixture.place(e, Side.P1, CardPool.FIGHTER_GER, Line.SUPPORT)
        val back = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        val front = Fixture.place(e, Side.P2, CardPool.INF_432, Line.FRONT)
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, f, Target.EnemyUnit(back)))
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, f, Target.EnemyUnit(front)))
    }

    @Test
    @WikiRule(RuleId.FIGHTER_INTERCEPT_BOMBER)
    fun 战斗机拦截轰炸机() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val bomber = Fixture.place(e, Side.P1, CardPool.BOMBER_GER, Line.FRONT)
        val victim = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        Fixture.place(e, Side.P2, CardPool.FIGHTER_GER, Line.SUPPORT)
        val r = e.canAttack(Side.P1, bomber, Target.EnemyUnit(victim))
        assertTrue(r is ActionResult.Rejected)
        assertTrue((r as ActionResult.Rejected).reason.contains("拦截"))
    }

    @Test
    @WikiRule(RuleId.FIGHTER_INTERCEPT_BOMBER)
    fun 轰炸机可以打战斗机本身() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val bomber = Fixture.place(e, Side.P1, CardPool.BOMBER_GER, Line.FRONT)
        val fighter = Fixture.place(e, Side.P2, CardPool.FIGHTER_GER, Line.SUPPORT)
        assertEquals(
            "目标本身是战斗机时不被拦截",
            ActionResult.Ok,
            e.canAttack(Side.P1, bomber, Target.EnemyUnit(fighter))
        )
    }

    @Test
    @WikiRule(RuleId.FIGHTER_NO_SMOKE)
    fun 战斗机不带烟幕() {
        assertFalse("战斗机卡不应带烟幕", CardPool.FIGHTER_GER.has(Keyword.SMOKE))
        val illegal = CardPool.FIGHTER_GER.copy(keywords = setOf(Keyword.SMOKE))
        assertEquals("非法组合仅用于确认规则约束", UnitType.FIGHTER, illegal.unitType)
    }

    @Test
    @WikiRule(RuleId.FIGHTER_NO_SMOKE, RuleId.KW_SMOKE)
    fun 前线单位也不能具有烟幕() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.SMOKE_UNIT, Line.FRONT)
        u.smokeActive = true
        e.beginTurn(Side.P1, drawAtStart = false)
        assertTrue("前排单位的烟幕不应恢复生效", !u.smokeActive || u.line != Line.SUPPORT)
    }

    @Test
    @WikiRule(RuleId.BOMBER_NO_RETAL_ATTACKING, RuleId.BOMBER_NO_RETAL_WHEN_ATTACKED)
    fun 轰炸机攻击非战斗机不受反击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val bomber = Fixture.place(e, Side.P1, CardPool.BOMBER_GER, Line.FRONT)
        val victim = Fixture.place(e, Side.P2, CardPool.AFRIKA, Line.SUPPORT)
        e.attack(Side.P1, bomber, victim)
        assertEquals("攻方无伤", CardPool.BOMBER_GER.defense, bomber.defense)
    }

    @Test
    @WikiRule(RuleId.BOMBER_NO_RETAL_ATTACKING)
    fun 轰炸机攻击战斗机时会受反击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        // 轰炸机 5 防、战斗机 6 防且 3 攻：双方都能活下来，才能观察到反击。
        val tankyBomber = CardPool.BOMBER_GER.copy(id = "u_tanky_bomber", defense = 5)
        val tankyFighter = CardPool.FIGHTER_GER.copy(id = "u_tanky_fighter", attack = 3, defense = 6)
        val bomber = Fixture.place(e, Side.P1, tankyBomber, Line.FRONT)
        val fighter = Fixture.place(e, Side.P2, tankyFighter, Line.SUPPORT)
        e.attack(Side.P1, bomber, fighter)
        assertTrue("战斗机存活才有反击", fighter.isAlive)
        assertTrue("轰炸机存活才能观察", bomber.isAlive)
        assertEquals("吃满战斗机的 3 点反击", 3, bomber.damage)
    }

    @Test
    @WikiRule(RuleId.BOMBER_NO_RETAL_WHEN_ATTACKED)
    fun 轰炸机被攻击时不反击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val inf = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        val bomber = Fixture.place(e, Side.P2, CardPool.BOMBER_GER, Line.SUPPORT)
        e.attack(Side.P1, inf, bomber)
        assertEquals("攻方零伤", CardPool.PZ_GREN.defense, inf.defense)
    }

    @Test
    @WikiRule(RuleId.BOMBER_IGNORE_GUARD)
    fun 轰炸机无视守护() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val bomber = Fixture.place(e, Side.P1, CardPool.BOMBER_GER, Line.FRONT)
        Fixture.place(e, Side.P2, CardPool.GUARD_UK, Line.SUPPORT)
        val sheltered = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, bomber, Target.EnemyUnit(sheltered)))
    }

    @Test
    @WikiRule(RuleId.ARTILLERY_NO_RETAL)
    fun 炮兵攻击不受反击() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val arty = Fixture.place(e, Side.P1, CardPool.ARTY_GER, Line.SUPPORT)
        val target = Fixture.place(e, Side.P2, CardPool.PZ_GREN, Line.SUPPORT)
        e.attack(Side.P1, arty, target)
        assertEquals("炮兵无伤", CardPool.ARTY_GER.defense, arty.defense)
    }

    @Test
    @WikiRule(RuleId.ARTILLERY_IGNORE_GUARD, RuleId.BOMBER_IGNORE_GUARD)
    fun 炮兵无视守护() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val arty = Fixture.place(e, Side.P1, CardPool.ARTY_GER, Line.SUPPORT)
        Fixture.place(e, Side.P2, CardPool.GUARD_UK, Line.SUPPORT)
        val sheltered = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, arty, Target.EnemyUnit(sheltered)))
    }
}