package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 卡组构筑。《卡组》《国家》。 */
class DeckRulesTest {

    private fun deck(cards: List<Card>, main: Nation = Nation.GERMANY, hq: Card = CardPool.GER_HQ) =
        Deck(main, hq, cards)

    private fun rulesOf(d: Deck): List<RuleId> = DeckValidator.validate(d).map { it.rule }

    @Test
    @WikiRule(RuleId.DECK_40_TOTAL, RuleId.HQ_ONE_PER_DECK)
    fun 合法卡组为一张总部加三十九张() {
        // 同名上限由卡面稀有度决定（普通 4 / 限定 3 / 特殊 2 / 精英 1），
        // 因此按每张卡自己的上限来凑满 39 张，而不是一律 4 张。
        val cards = Fixture.fillByCopyLimit(
            listOf(
                CardPool.INF_89, CardPool.TANK_35T, CardPool.PZ_GREN, CardPool.STUG_F,
                CardPool.AFRIKA, CardPool.GER_ORDER_NORTH, CardPool.FIGHTER_GER,
                CardPool.BOMBER_GER, CardPool.ARTY_GER, CardPool.U375,
                CardPool.ORDER_DIVE, CardPool.GER_ORDER_BLITZ, CardPool.GER_ORDER_SNEAK,
                CardPool.GER_ORDER_DEV, CardPool.GER_PAK40, CardPool.GER_ALPINE_141,
                CardPool.GER_COVERT_39, CardPool.GER_MOBILIZE_119, CardPool.GER_BF110,
                CardPool.GER_PZ2C, CardPool.GER_STUG3G, CardPool.GER_HS126,
                CardPool.GER_ORDER_CONVERT, CardPool.GER_ORDER_SCRAP, CardPool.GER_ORDER_EXILE
            )
        )
        val d = deck(cards)
        assertEquals("总数 40", 40, d.total)
        assertEquals("总部单列", 39, d.cards.size)
        assertTrue("无违规：" + DeckValidator.validate(d).joinToString { it.message }, DeckValidator.isValid(d))
    }

    @Test
    @WikiRule(RuleId.DECK_40_TOTAL)
    fun 张数不足时报错() {
        val short = deck(List(20) { i -> CardPool.INF_89.copy(id = "u_short_$i") })
        assertTrue(rulesOf(short).contains(RuleId.DECK_40_TOTAL))
        assertFalse(DeckValidator.isValid(short))
    }

    @Test
    @WikiRule(RuleId.DECK_40_TOTAL)
    fun 张数超出时报错() {
        val long = deck(List(45) { i -> CardPool.INF_89.copy(id = "u_long_$i") })
        assertTrue(rulesOf(long).contains(RuleId.DECK_40_TOTAL))
    }

    @Test
    @WikiRule(RuleId.DECK_ALLY_MAX_12)
    fun 盟国卡牌最多十二张() {
        val cards = MutableList(39) { i -> CardPool.INF_89.copy(id = "u_m_$i") }
        repeat(13) { i -> cards[i] = CardPool.INF_432.copy(id = "u_ally_$i") }
        assertTrue(rulesOf(deck(cards)).contains(RuleId.DECK_ALLY_MAX_12))
    }

    @Test
    @WikiRule(RuleId.DECK_ALLY_MAX_12, RuleId.DECK_ALLY_NO_ELITE)
    fun 盟国正好十二张时合法() {
        val cards = MutableList(39) { i -> CardPool.INF_89.copy(id = "u_m2_$i") }
        repeat(12) { i -> cards[i] = CardPool.INF_432.copy(id = "u_ally2_$i") }
        val rules = rulesOf(deck(cards))
        assertFalse("12 张是上限而非违规", rules.contains(RuleId.DECK_ALLY_MAX_12))
    }

    @Test
    @WikiRule(RuleId.DECK_ALLY_NO_ELITE)
    fun 盟国不能带精英卡() {
        val cards = MutableList(39) { i -> CardPool.INF_89.copy(id = "u_e_$i") }
        cards[0] = CardPool.ELITE_USA
        assertTrue(rulesOf(deck(cards)).contains(RuleId.DECK_ALLY_NO_ELITE))
    }

    @Test
    @WikiRule(RuleId.DECK_COPY_LIMIT_BY_RARITY)
    fun 同名卡上限按稀有度() {
        assertEquals("普通 4", 4, Rarity.STANDARD.maxCopies)
        assertEquals("限定 3", 3, Rarity.LIMITED.maxCopies)
        assertEquals("特殊 2", 2, Rarity.SPECIAL.maxCopies)
        assertEquals("精英 1", 1, Rarity.ELITE.maxCopies)
    }

    @Test
    @WikiRule(RuleId.DECK_COPY_LIMIT_BY_RARITY)
    fun 超量携带同名卡被拒() {
        val cards = List(39) { CardPool.STEALTH_TANK }
        assertTrue(rulesOf(deck(cards)).contains(RuleId.DECK_COPY_LIMIT_BY_RARITY))
    }

    @Test
    @WikiRule(RuleId.DECK_MAIN_NATION_UNLIMITED)
    fun 主国卡牌数量不受限制() {
        val cards = MutableList(39) { i -> CardPool.INF_89.copy(id = "u_main_$i") }
        repeat(4) { cards[it] = CardPool.ORDER_DIVE }
        val d = deck(cards)
        val rules = rulesOf(d)
        assertFalse("主国卡不受盟国 12 张限制", rules.contains(RuleId.DECK_ALLY_MAX_12))
    }

    @Test
    @WikiRule(RuleId.DECK_MAIN_NATION_UNLIMITED, RuleId.DECK_ALLY_MAX_12)
    fun 主国与盟国的分界以国家判定() {
        val cards = MutableList(39) { CardPool.INF_89 }
        cards[0] = CardPool.INF_432
        val allyCount = cards.count { it.nation != Nation.GERMANY }
        assertEquals("盟国只有 1 张", 1, allyCount)
    }

    @Test
    @WikiRule(RuleId.DECK_REQUIRES_MAIN_NATION, RuleId.NATION_FIVE_MAJOR)
    fun 只能做盟国的国家不能当主国() {
        val d = deck(List(39) { CardPool.COVERT_UNIT }, Nation.FRANCE)
        assertTrue(rulesOf(d).contains(RuleId.NATION_FIVE_MAJOR))
    }

    @Test
    @WikiRule(RuleId.DECK_REQUIRES_MAIN_NATION, RuleId.HQ_ONE_PER_DECK)
    fun 总部必须与主国一致() {
        val d = deck(List(39) { CardPool.INF_89 }, Nation.GERMANY, CardPool.USA_HQ)
        assertTrue(rulesOf(d).contains(RuleId.HQ_ONE_PER_DECK))
    }

    @Test
    @WikiRule(RuleId.HQ_ONE_PER_DECK)
    fun 卡组不能额外携带总部() {
        val d = deck(List(39) { CardPool.INF_89 } + CardPool.GER_HQ)
        assertTrue(rulesOf(d).contains(RuleId.HQ_ONE_PER_DECK))
    }
}

/** 国家。来源：《国家》《词条》。 */
class NationRulesTest {

    @Test
    @WikiRule(RuleId.NATION_TEN, RuleId.NATION_FIVE_MAJOR)
    fun 总计十国() {
        assertEquals("十国", 10, Nation.entries.size)
        assertEquals("五主国", 5, Nation.entries.count { it.canBeMain })
    }

    @Test
    @WikiRule(RuleId.NATION_FIVE_ALLY_ONLY, RuleId.NATION_TEN)
    fun 五个只能做盟国的国家() {
        assertEquals("五个盟国", 5, Nation.entries.count { !it.canBeMain })
        assertTrue(Nation.FRANCE in Nation.entries)
        assertTrue(Nation.ITALY in Nation.entries)
        assertTrue(Nation.POLAND in Nation.entries)
        assertTrue(Nation.FINLAND in Nation.entries)
        assertTrue(Nation.ANZAC in Nation.entries)
    }

    @Test
    @WikiRule(RuleId.NATION_FIVE_MAJOR)
    fun 五主国名称与可做主国() {
        val majors = Nation.entries.filter { it.canBeMain }.map { it.cn }.toSet()
        assertEquals(setOf("德国", "美国", "英国", "苏联", "日本"), majors)
    }

    @Test
    @WikiRule(RuleId.NATION_ALLY_ONLY_NO_ELITE)
    fun 只能做盟国的国家没有精英卡() {
        assertTrue(Nation.entries.filter { !it.canBeMain }.all { !it.hasElite })
    }

    @Test
    @WikiRule(RuleId.NATION_ALLY_ONLY_NO_ELITE, RuleId.DECK_ALLY_NO_ELITE)
    fun 盟国卡池里没有精英卡() {
        val allyOnlyElites = CardPool.all.filter {
            it.nation in Nation.entries.filter { n -> !n.canBeMain } && it.rarity == Rarity.ELITE
        }
        assertTrue("盟国卡池不应出现精英", allyOnlyElites.isEmpty())
    }

    @Test
    @WikiRule(RuleId.NATION_ALLY_ONLY_NO_ELITE)
    fun 五主国都能取到初始总部() {
        Nation.entries.filter { it.canBeMain }.forEach {
            assertNotNull("${it.cn} 应有初始总部", CardPool.hqFor(it))
        }
    }

    @Test
    @WikiRule(RuleId.NATION_FIVE_ALLY_ONLY)
    fun 盟国没有初始总部() {
        var threw = false
        try {
            CardPool.hqFor(Nation.FRANCE)
        } catch (e: IllegalStateException) {
            threw = true
        }
        assertTrue("法国无初始总部", threw)
    }

    @Test
    @WikiRule(RuleId.NATION_EXILE_BOTH)
    fun 流亡卡牌记录流亡国() {
        val exile = CardPool.INF_89.copy(id = "u_exile", exileNation = Nation.UK)
        assertEquals("可用于流亡国卡组", Nation.UK, exile.exileNation)
        assertEquals("原籍仍在", Nation.GERMANY, exile.nation)
    }
}

/** 卡牌类型与总部。来源：《卡牌》。 */
class CardTypeTest {

    @Test
    @WikiRule(RuleId.CARD_FOUR_TYPES, RuleId.UNIT_FIVE_TYPES)
    fun 卡牌四类() {
        assertEquals("总部/单位/指令/反制", 4, CardType.entries.size)
        assertEquals(setOf("总部", "单位", "指令", "反制"), CardType.entries.map { it.cn }.toSet())
    }

    @Test
    @WikiRule(RuleId.UNIT_FIVE_TYPES)
    fun 单位五类() {
        assertEquals("步兵/坦克/战斗机/轰炸机/炮兵", 5, UnitType.entries.size)
        assertEquals(
            setOf("步兵", "坦克", "战斗机", "轰炸机", "炮兵"),
            UnitType.entries.map { it.cn }.toSet()
        )
    }

    @Test
    @WikiRule(RuleId.HQ_DEFENSE_20)
    fun 总部初始二十防御() {
        assertEquals("德国", 20, CardPool.GER_HQ.defense)
        assertEquals("美国", 20, CardPool.USA_HQ.defense)
        assertEquals(Engine.HQ_DEFENSE, 20)
    }

    @Test
    @WikiRule(RuleId.HQ_WIN_DOWN_TO_ZERO)
    fun 总部归零即败() {
        val e = Fixture.started()
        e.p2.hq.damage = 20
        e.checkWin()
        assertTrue(e.finished)
        assertEquals(Side.P1, e.winner)
    }

    @Test
    @WikiRule(RuleId.HQ_WIN_DOWN_TO_ZERO, RuleId.HQ_DEFENSE_20)
    fun 未归零则继续() {
        val e = Fixture.started()
        e.p2.hq.damage = 19
        e.checkWin()
        assertFalse(e.finished)
        assertEquals("还剩 1 点", 1, e.p2.hq.defense)
    }

    @Test
    @WikiRule(RuleId.HQ_IMMOBILE_SUPPORT_ONLY)
    fun 总部不可攻击也不可移动() {
        val e = Fixture.started()
        val target = e.p2.support.firstOrNull { it.card.type == CardType.HQ }
        assertEquals("总部不在单位列表", null, target)
        assertEquals("总部类型", CardType.HQ, CardPool.GER_HQ.type)
    }

    @Test
    @WikiRule(RuleId.HQ_CANNOT_BE_SUPPRESSED)
    fun 总部不可被抑制() {
        val e = Fixture.started()
        val hq = e.p1.hq
        assertEquals("总部无关键词可抑制", 0, CardPool.GER_HQ.keywords.size)
        assertEquals("总部仍是总部", CardType.HQ, hq.card.type)
    }

    @Test
    @WikiRule(RuleId.ORDER_ONE_SHOT)
    fun 指令不能当作单位部署() {
        val e = Fixture.started()
        val r = e.deployUnit(Side.P1, CardPool.ORDER_DIVE)
        assertTrue(r is ActionResult.Rejected)
    }

    @Test
    @WikiRule(RuleId.ORDER_ONE_SHOT, RuleId.CARD_FOUR_TYPES)
    fun 指令类型与单位区分() {
        assertEquals(CardType.ORDER, CardPool.ORDER_DIVE.type)
        assertEquals(CardType.UNIT, CardPool.INF_89.type)
    }
}

/** 反制。来源：《卡牌》。 */
class CountermeasureTest {

    @Test
    @WikiRule(RuleId.COUNTER_ACTIVATED_IN_HAND_HIDDEN)
    fun 反制在手中激活且对敌方隐藏() {
        val e = Fixture.started()
        e.p1.activeCounters += CardPool.COUNTER_AMBUSH
        assertEquals("已激活", 1, e.p1.activeCounters.size)
        assertTrue("对手看不到激活列表", e.p2.activeCounters.isEmpty())
    }

    @Test
    @WikiRule(RuleId.COUNTER_DEACTIVATE_NEXT_TURN, RuleId.COUNTER_PRIORITY_OVER_ORDER)
    fun 未触发则下回合取消激活() {
        val e = Fixture.started()
        e.p1.activeCounters += CardPool.COUNTER_AMBUSH
        // 对手回合期间仍挂着，等回到拥有者回合开始时才取消激活。
        e.beginTurn(Side.P2, drawAtStart = false)
        assertEquals("对手回合里仍有效", 1, e.p1.activeCounters.size)
        e.beginTurn(Side.P1, drawAtStart = false)
        assertTrue("拥有者回合开始取消激活", e.p1.activeCounters.isEmpty())
    }

    @Test
    @WikiRule(RuleId.COUNTER_TRIGGER_ON_OPPONENT_TURN)
    fun 反制只在敌方回合满足条件时触发() {
        val e = Fixture.started()
        assertEquals("P1 激活、P2 回合触发", Side.P2, Side.P1.opponent)
    }

    @Test
    @WikiRule(RuleId.COUNTER_PRIORITY_OVER_ORDER)
    fun 反制优先于指令() {
        val types = listOf(CardType.COUNTERMEASURE, CardType.ORDER)
        assertEquals("反制排在指令之前", CardType.COUNTERMEASURE, types.first())
    }

    @Test
    @WikiRule(RuleId.COUNTER_COST_HIDDEN, RuleId.KREDIT_SPEND_VISIBLE_EXCEPT_COUNTER)
    fun 反制费用对敌方不可见() {
        val e = Fixture.started(kredits = 5)
        val before = e.p1.kredits
        e.p1.pay(CardPool.COUNTER_AMBUSH.cost)
        assertEquals("自己知道花了多少", before - CardPool.COUNTER_AMBUSH.cost, e.p1.kredits)
        assertEquals("对手看不到激活记录", 0, e.p2.activeCounters.size)
    }
}

/** 游戏模式。来源：《游戏模式》《基本知识》。 */
class GameModeTest {

    @Test
    @WikiRule(RuleId.MODE_RANKED)
    fun 只有排位晋升军衔() {
        val affecting = GameMode.entries.filter { ModeRules.affectsRank(it) }
        assertEquals("仅排位", 1, affecting.size)
        assertEquals(GameMode.RANKED, affecting.first())
    }

    @Test
    @WikiRule(RuleId.MODE_CASUAL, RuleId.MODE_RANKED)
    fun 休闲不影响军衔() {
        assertFalse(ModeRules.affectsRank(GameMode.CASUAL))
        assertTrue(ModeRules.affectsRank(GameMode.RANKED))
    }

    @Test
    @WikiRule(RuleId.MODE_CLASSIC)
    fun 经典需要预备卡池() {
        assertTrue(ModeRules.requiresReservePool(GameMode.CLASSIC))
        assertFalse(ModeRules.requiresReservePool(GameMode.RANKED))
        assertFalse(ModeRules.requiresReservePool(GameMode.CASUAL))
    }

    @Test
    @WikiRule(RuleId.MODE_CLASSIC, RuleId.MODE_CASUAL)
    fun 经典与休闲都不影响军衔() {
        assertFalse(ModeRules.affectsRank(GameMode.CLASSIC))
        assertFalse(ModeRules.affectsRank(GameMode.CASUAL))
    }

    @Test
    @WikiRule(RuleId.MODE_TRAINING)
    fun 训练模式五位AI() {
        assertEquals(5, AiCommander.entries.size)
        assertEquals(
            setOf("Ivanov", "Sato", "Smith", "Muller", "Williams"),
            AiCommander.entries.map { it.cn }.toSet()
        )
        assertTrue(AiCommander.entries.all { it.nation.canBeMain })
    }

    @Test
    @WikiRule(RuleId.MODE_TRAINING)
    fun 五位AI分属五个不同主国() {
        assertEquals(5, AiCommander.entries.map { it.nation }.distinct().size)
    }

    @Test
    @WikiRule(RuleId.MODE_ARENA)
    fun 竞技场七胜或三败结算() {
        assertTrue(ModeRules.arenaShouldSettle(7, 0))
        assertTrue(ModeRules.arenaShouldSettle(0, 3))
        assertFalse(ModeRules.arenaShouldSettle(6, 2))
    }

    @Test
    @WikiRule(RuleId.MODE_ARENA)
    fun 竞技场奖励表对齐Wiki() {
        assertEquals("7 胜 180 金币", 180, ModeRules.arenaGold(7))
        assertEquals("6 胜 150 金币", 150, ModeRules.arenaGold(6))
        assertEquals("5 胜 90 金币", 90, ModeRules.arenaGold(5))
        assertEquals("4 胜 60 金币", 60, ModeRules.arenaGold(4))
        assertEquals("3 胜 40 金币", 40, ModeRules.arenaGold(3))
        assertEquals("2 胜 20 金币", 20, ModeRules.arenaGold(2))
        assertEquals("0-1 胜 0 金币", 0, ModeRules.arenaGold(1))
        assertEquals("7 胜 2 个战利品", 2, ModeRules.ARENA_LOOT[7])
        assertEquals("门票 150 金币或 20 钻石", 150, ModeRules.ARENA_ENTRY_GOLD)
        assertEquals(20, ModeRules.ARENA_ENTRY_DIAMOND)
    }

    @Test
    @WikiRule(RuleId.MODE_DUEL_CODE)
    fun 战斗代码长度三到十五() {
        assertTrue(ModeRules.isValidDuelCode("abc"))
        assertFalse(ModeRules.isValidDuelCode("ab"))
        assertTrue(ModeRules.isValidDuelCode("a".repeat(15)))
        assertFalse(ModeRules.isValidDuelCode("a".repeat(16)))
    }

    @Test
    @WikiRule(RuleId.MODE_DUEL_CODE)
    fun 战斗代码拒绝表情符号() {
        assertTrue(ModeRules.isValidDuelCode("中文代码"))
        assertFalse(ModeRules.isValidDuelCode("abc\uD83D\uDE00"))
    }
}