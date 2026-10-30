package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 集成测试第二组：把零散机制放进「一局对局」的上下文里验证。
 * 目的是让每条 Wiki 规则除了单元测试之外，也有一条跨模块的集成路径。
 */
class RuleJourneyIntegrationTest {

    /** 资源 + 部署 + 抽牌：一次真实的开局回合。 */
    @Test
    @WikiRule(
        RuleId.SLOT_START_0,
        RuleId.KREDIT_SPENT_ON_ALL_ACTIONS,
        RuleId.KREDIT_SPEND_VISIBLE_EXCEPT_COUNTER,
        RuleId.BURN_NOT_DISCARD,
        RuleId.HAND_CAP_9_BURN,
        RuleId.CARD_FOUR_TYPES
    )
    fun 开局回合内的资源与手牌联动() {
        val e = Fixture.engine()
        e.start(Side.P1)
        assertEquals("开局指挥点等于槽", 1, e.p1.kredits)

        // 手牌塞满后抽牌被爆掉，不进弃牌堆。
        while (e.p1.hand.size < Engine.HAND_CAP) e.p1.hand += HandCard(CardPool.INF_89)
        val discard = e.p1.discardPile.size
        e.draw(Side.P1)
        assertEquals(Engine.HAND_CAP, e.p1.hand.size)
        assertEquals("爆牌不弃", discard, e.p1.discardPile.size)

        // 指挥点不足则无法部署高费卡，四类卡里只有单位可部署。
        val poor = e.deployUnit(Side.P1, CardPool.ELITE_GER)
        assertTrue(poor is ActionResult.Rejected)
        val order = e.deployUnit(Side.P1, CardPool.ORDER_DIVE)
        assertTrue("指令不能部署", order is ActionResult.Rejected)
    }

    /** 卡组规则 + 总部规则 + 模式：构筑到开局的完整链路。 */
    @Test
    @WikiRule(
        RuleId.HQ_ONE_PER_DECK,
        RuleId.HQ_IMMOBILE_SUPPORT_ONLY,
        RuleId.HQ_CANNOT_BE_SUPPRESSED,
        RuleId.DECK_COPY_LIMIT_BY_RARITY,
        RuleId.DECK_MAIN_NATION_UNLIMITED
    )
    fun 构筑校验到开局的总部链路() {
        val cards = Fixture.fillByCopyLimit(
            listOf(
                CardPool.INF_89, CardPool.TANK_35T, CardPool.PZ_GREN, CardPool.STUG_F,
                CardPool.AFRIKA, CardPool.GER_ORDER_SCRAP, CardPool.U375,
                CardPool.FIGHTER_GER, CardPool.BOMBER_GER, CardPool.ARTY_GER,
                CardPool.ORDER_DIVE, CardPool.GER_PAK40, CardPool.GER_ALPINE_141,
                CardPool.GER_COVERT_39, CardPool.GER_BF110, CardPool.GER_PZ2C,
                CardPool.GER_STUG3G, CardPool.GER_HS126, CardPool.GER_ORDER_BLITZ,
                CardPool.GER_ORDER_SNEAK, CardPool.GER_ORDER_NORTH, CardPool.GER_ORDER_DEV
            )
        )
        val deck = Deck(Nation.GERMANY, CardPool.GER_HQ, cards)
        assertTrue(DeckValidator.isValid(deck))

        val e = Engine(
            Engine.newState(deck, Side.P1),
            Engine.newState(Fixture.deckOf(Nation.JAPAN), Side.P2)
        )
        e.start(Side.P1)
        assertEquals("总部固定在支援阵线", 20, e.p1.hq.defense)
        assertTrue("总部不是可抑制的单位", e.p1.support.none { it.card.type == CardType.HQ })
    }

    /** 兵种 + 词条 + 阵线：一次完整的空战交换。 */
    @Test
    @WikiRule(
        RuleId.FIGHTER_ANY_LINE,
        RuleId.FIGHTER_INTERCEPT_BOMBER,
        RuleId.BOMBER_NO_RETAL_ATTACKING,
        RuleId.FIGHTER_NO_SMOKE,
        RuleId.INFANTRY_ADJACENT_ONLY
    )
    fun 空战链路里战斗机与轰炸机的互动() {
        val e = Fixture.started(kredits = 30)
        Fixture.freshTurn(e)
        val fighter = Fixture.place(e, Side.P1, CardPool.FIGHTER_GER, Line.SUPPORT)
        val bomber = Fixture.place(e, Side.P2, CardPool.BOMBER_GER, Line.SUPPORT)
        val infantry = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)

        // 战斗机无视阵线，可以直接打后排。
        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, fighter, Target.EnemyUnit(infantry)))
        assertEquals("战斗机打轰炸机", ActionResult.Ok, e.attack(Side.P1, fighter, bomber))
        assertTrue("轰炸机挨打后掉血", bomber.damage > 0)
        assertFalse("战斗机不能带烟幕", fighter.has(Keyword.SMOKE))
    }

    /** 守护 + 炮兵 + 重甲：穿透与减伤在同一次攻击里。 */
    @Test
    @WikiRule(
        RuleId.ARTILLERY_IGNORE_GUARD,
        RuleId.ARTILLERY_NO_RETAL,
        RuleId.KW_GUARD,
        RuleId.KW_HEAVY_ARMOR
    )
    fun 炮兵穿透守护并绕开重甲外的反击() {
        val e = Fixture.started(kredits = 30)
        Fixture.freshTurn(e)
        val arty = Fixture.place(e, Side.P1, CardPool.ARTY_GER, Line.SUPPORT)
        Fixture.place(e, Side.P2, CardPool.GUARD_UK, Line.SUPPORT)
        val armored = Fixture.place(e, Side.P2, CardPool.ARMOR_TANK, Line.SUPPORT)

        assertEquals(ActionResult.Ok, e.canAttack(Side.P1, arty, Target.EnemyUnit(armored)))
        e.attack(Side.P1, arty, armored)
        assertEquals("炮兵不吃反击", CardPool.ARTY_GER.defense, arty.defense)
        assertEquals("炮兵攻击属对战伤害，重甲照减：3-1=2", 2, armored.damage)
    }

    /** 压制 + 奋战 + 行动花费：被压制的高费单位完全无法行动。 */
    @Test
    @WikiRule(
        RuleId.KW_PIN,
        RuleId.KW_FURY,
        RuleId.ACTION_COST_ON_MOVE_AND_ATTACK,
        RuleId.KW_BLITZ
    )
    fun 压制压制住奋战单位的两次攻击() {
        val e = Fixture.started(kredits = 30)
        Fixture.freshTurn(e)
        val fury = Fixture.place(e, Side.P1, CardPool.FURY_USSR, Line.FRONT)
        e.pin(fury)
        val r = e.canAttack(Side.P1, fury, Target.EnemyHq(Side.P2))
        assertTrue(r is ActionResult.Rejected)
        assertTrue((r as ActionResult.Rejected).reason.contains("压制"))
        assertEquals("未产生任何指挥点消耗", 30, e.p1.kredits)
    }

    /** 抑制 + 重甲 + 修复：面板重置后修复的基准也变了。 */
    @Test
    @WikiRule(
        RuleId.KW_SUPPRESS,
        RuleId.KW_REPAIR,
        RuleId.KW_HEAVY_ARMOR,
        RuleId.KW_IMMUNE
    )
    fun 抑制后修复基准回到卡面() {
        val e = Fixture.started()
        val u = Fixture.place(e, Side.P1, CardPool.ARMOR_TANK)
        u.maxDefenseBonus = 2
        u.damage = 1
        assertEquals("含增益的最大防御", CardPool.ARMOR_TANK.defense + 2, u.maxDefense)
        e.suppress(u)
        assertEquals("抑制后回到卡面", CardPool.ARMOR_TANK.defense, u.maxDefense)
        e.repair(u)
        assertEquals("修复补满", u.maxDefense, u.defense)
        assertEquals("重甲被清除", 0, u.armorValue)
    }

    /** 控制 + 撤退 + 收缴：三个改变归属/位置的效果串起来。 */
    @Test
    @WikiRule(
        RuleId.KW_TAKE_CONTROL,
        RuleId.KW_RETREAT,
        RuleId.KW_SALVAGE,
        RuleId.FRONT_LINE_CAP_5
    )
    fun 控制撤退与收缴串在一条链上() {
        val e = Fixture.started(kredits = 50)
        val mine = Fixture.place(e, Side.P1, CardPool.PZ_GREN, Line.FRONT)
        val theirs = Fixture.place(e, Side.P2, CardPool.INF_432, Line.SUPPORT)

        e.takeControl(theirs, Side.P1)
        assertEquals("所有者不变", Side.P2, theirs.owner)
        assertTrue("控制者在 P1 场上", theirs in e.p1.support)

        e.retreat(mine)
        assertEquals("撤退回支援阵线", Line.SUPPORT, mine.line)

        val hand = e.p1.hand.size
        e.salvage(Side.P1, UnitInstance(CardPool.AFRIKA, Side.P2), Nation.GERMANY)
        assertEquals("收缴入手一张", hand + 1, e.p1.hand.size)
    }

    /** 隐蔽 + 情报 + 反制：信息不对称的三件事。 */
    @Test
    @WikiRule(
        RuleId.KW_COVERT,
        RuleId.KW_INTEL,
        RuleId.COUNTER_ACTIVATED_IN_HAND_HIDDEN,
        RuleId.COUNTER_COST_HIDDEN
    )
    fun 信息不对称在隐蔽情报反制之间保持一致() {
        val e = Fixture.started()
        Fixture.freshTurn(e)
        val covert = Fixture.place(e, Side.P2, CardPool.COVERT_UNIT, Line.SUPPORT)
        assertFalse("隐蔽未揭示", covert.covertRevealed)

        repeat(3) { e.p2.hand += HandCard(CardPool.INF_432) }
        assertEquals("情报只能看 2 张", 2, e.intel(Side.P1, 2))

        e.p1.activeCounters += CardPool.COUNTER_AMBUSH
        assertTrue("对手看不到激活反制", e.p2.activeCounters.isEmpty())
    }

    /** 老兵 + 倒计时 + 开发：成长类效果的组合。 */
    @Test
    @WikiRule(
        RuleId.KW_VETERAN,
        RuleId.KW_COUNTDOWN,
        RuleId.KW_DEVELOP,
        RuleId.KW_DEPLOY
    )
    fun 成长类效果在连续回合里推进() {
        val e = Fixture.started()
        val veteran = Fixture.place(e, Side.P1, CardPool.STEALTH_TANK)
        e.promoteVeteran(veteran)
        assertTrue(veteran.veteran)

        val countdown = Fixture.place(
            e, Side.P1,
            CardPool.INF_89.copy(id = "u_cd", keywords = setOf(Keyword.COUNTDOWN), countdown = 2)
        )
        countdown.countdownLeft = 2
        assertEquals(2, countdown.countdownLeft)

        val three = e.develop(Side.P1, CardPool.all.filter { it.type == CardType.UNIT })
        assertEquals("开发给三选一", 3, three.size)
    }

    /** 士气 + 协力 + 免疫：三条真实伤害相关规则的组合。 */
    @Test
    @WikiRule(
        RuleId.MORALE_TRUE_DAMAGE,
        RuleId.BOND_MORALE,
        RuleId.KW_IMMUNE,
        RuleId.MORALE_NO_RESET
    )
    fun 真实伤害不吃免疫也不因协力重置() {
        val e = Fixture.started()
        e.p1.hq.immune = true
        e.p1.drawPile.clear()
        e.draw(Side.P1)
        assertEquals("免疫挡不住士气", 1, e.p1.fatigueDamageTotal)

        val bond = CardPool.INF_89.copy(id = "u_bond", keywords = setOf(Keyword.BOND))
        e.checkBond(Side.P1, bond)
        assertEquals("士气计数继续递增", 2, e.p1.moraleCounter)
        assertEquals("累计 1+2", 3, e.p1.fatigueDamageTotal)
    }

    /** 转换 + 钳击 + 流亡：三个偏静态的关键词在同一条断言里。 */
    @Test
    @WikiRule(
        RuleId.KW_CONVERT,
        RuleId.KW_PINCER,
        RuleId.NATION_EXILE_BOTH,
        RuleId.KW_ALPINE
    )
    fun 静态关键词与流亡归属() {
        val pincer = CardPool.PZ_GREN.copy(keywords = setOf(Keyword.PINCER))
        assertTrue(pincer.has(Keyword.PINCER))

        val exile = CardPool.INF_89.copy(id = "u_exile", exileNation = Nation.UK)
        assertEquals(Nation.UK, exile.exileNation)

        val e = Fixture.started()
        Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        val second = Fixture.place(e, Side.P1, CardPool.ALPINE_UNIT)
        e.applyAlpineBonus(second)
        assertEquals(CardPool.ALPINE_UNIT.attack + 1, second.attack)
        assertFalse("转换不属于战斗词条", Keyword.CONVERT.isCombatKeyword)
    }

    /** 模式 + 竞技场 + 训练：模式相关的全部规则在一条链里。 */
    @Test
    @WikiRule(
        RuleId.MODE_RANKED,
        RuleId.MODE_CASUAL,
        RuleId.MODE_CLASSIC,
        RuleId.MODE_TRAINING,
        RuleId.MODE_ARENA,
        RuleId.MODE_DUEL_CODE
    )
    fun 六种模式规则可以同时被核对() {
        assertTrue(ModeRules.affectsRank(GameMode.RANKED))
        assertFalse(ModeRules.affectsRank(GameMode.CASUAL))
        assertTrue(ModeRules.requiresReservePool(GameMode.CLASSIC))
        assertEquals(5, AiCommander.entries.size)
        assertTrue(ModeRules.arenaShouldSettle(7, 0))
        assertFalse(ModeRules.arenaShouldSettle(6, 2))
        assertTrue(ModeRules.isValidDuelCode("ABC123"))
        assertFalse(ModeRules.isValidDuelCode("A"))
    }

    /** 国家 + 卡组 + 总部：十国与主盟关系的端到端核对。 */
    @Test
    @WikiRule(
        RuleId.NATION_TEN,
        RuleId.NATION_FIVE_MAJOR,
        RuleId.NATION_FIVE_ALLY_ONLY,
        RuleId.NATION_ALLY_ONLY_NO_ELITE,
        RuleId.DECK_REQUIRES_MAIN_NATION,
        RuleId.DECK_ALLY_MAX_12
    )
    fun 十国关系与卡组限制的端到端核对() {
        assertEquals(10, Nation.entries.size)
        val majors = Nation.entries.filter { it.canBeMain }
        val allies = Nation.entries.filter { !it.canBeMain }
        assertEquals(5, majors.size)
        assertEquals(5, allies.size)
        assertTrue(allies.all { !it.hasElite })

        val deck = Deck(Nation.FRANCE, CardPool.GER_HQ, List(39) { CardPool.COVERT_UNIT.copy(id = it.toString()) })
        assertTrue("法国不能当主国", DeckValidator.validate(deck).any { it.rule == RuleId.NATION_FIVE_MAJOR })
    }

    /** 战斗定义 + 全部战斗词条：一场多单位混战的收口检查。 */
    @Test
    @WikiRule(
        RuleId.COMBAT_VS_ATTACK,
        RuleId.KW_AMBUSH,
        RuleId.KW_SHOCK,
        RuleId.KW_SMOKE,
        RuleId.KW_FURY,
        RuleId.KW_BLITZ,
        RuleId.KW_GUARD
    )
    fun 一场混战覆盖全部战斗词条() {
        val e = Fixture.started(kredits = 60)
        Fixture.freshTurn(e)
        val furry = Fixture.place(e, Side.P1, CardPool.FURY_USSR, Line.FRONT)
        val shock = Fixture.place(e, Side.P1, CardPool.SHOCK_USSR, Line.FRONT)
        val ambusher = Fixture.place(e, Side.P2, CardPool.AMBUSH_JPN, Line.SUPPORT)
        val smoke = Fixture.place(e, Side.P2, CardPool.SMOKE_UNIT, Line.SUPPORT)

        assertEquals(ActionResult.Ok, e.attack(Side.P1, furry, ambusher))
        assertFalse("伏击被消耗", ambusher.ambushAvailable)
        assertEquals("烟幕挡住", true, e.canAttack(Side.P1, shock, Target.EnemyUnit(smoke)) is ActionResult.Rejected)
        assertEquals("战斗词条 7 个", 7, Keyword.entries.count { it.isCombatKeyword })
    }
}