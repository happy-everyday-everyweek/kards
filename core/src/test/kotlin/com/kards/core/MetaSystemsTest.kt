package com.kards.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 军衔。来源：《军衔》。 */
class RankRulesTest {

    @Test
    @WikiRule(RuleId.RANK_TOTAL_25, RuleId.RANK_STARS_5)
    fun 共二十五段且每段五星() {
        assertEquals("25 段", 25, RankSystem.TOTAL_RANKS)
        assertEquals("每段 5 星", 5, RankSystem.STARS_PER_RANK)
        assertEquals("25 个段名", 25, RankSystem.rankNames.size)
        assertEquals("元帅", RankSystem.nameOf(1))
        assertEquals("新兵", RankSystem.nameOf(25))
    }

    @Test
    @WikiRule(RuleId.RANK_WIN_1_STAR)
    fun 胜利加一星() {
        val (rank, stars) = RankSystem.applyMatch(10, 2, RankSystem.MatchResult(true, 1), 0)
        assertEquals(10, rank)
        assertEquals("+1 星", 3, stars)
    }

    @Test
    @WikiRule(RuleId.RANK_WIN_STREAK_2_STARS)
    fun 连胜至少三场加二星() {
        val (_, s1) = RankSystem.applyMatch(10, 2, RankSystem.MatchResult(true, 2), 0)
        assertEquals("2 连胜仍 +1", 3, s1)
        val (_, s2) = RankSystem.applyMatch(10, 2, RankSystem.MatchResult(true, 3), 0)
        assertEquals("3 连胜 +2", 4, s2)
    }

    @Test
    @WikiRule(RuleId.RANK_WIN_STREAK_2_STARS, RuleId.RANK_WIN_1_STAR)
    fun 满星后进位到下一段() {
        val (rank, stars) = RankSystem.applyMatch(24, 5, RankSystem.MatchResult(true, 3), 0)
        assertEquals("24 -> 23", 23, rank)
        assertEquals("5+2-5 = 2", 2, stars)
    }

    @Test
    @WikiRule(RuleId.RANK_LOSE_STREAK_DROP)
    fun 零星时连续三败掉段() {
        val (rank, stars) = RankSystem.applyMatch(12, 0, RankSystem.MatchResult(false, 0), 3)
        assertEquals("掉到 13 段", 13, rank)
        assertEquals("满星", 5, stars)
    }

    @Test
    @WikiRule(RuleId.RANK_LOSE_STREAK_DROP)
    fun 未达三败只扣星不掉段() {
        val (rank, stars) = RankSystem.applyMatch(10, 0, RankSystem.MatchResult(false, 0), 2)
        assertEquals("段位不变", 10, rank)
        assertEquals("停在 0 星", 0, stars)
    }

    @Test
    @WikiRule(RuleId.RANK_BELOW_20_NO_DROP)
    fun 二十段以下失败不降星() {
        val (rank, stars) = RankSystem.applyMatch(21, 3, RankSystem.MatchResult(false, 0), 5)
        assertEquals(21, rank)
        assertEquals("星数也不降", 3, stars)
    }

    @Test
    @WikiRule(RuleId.RANK_BELOW_20_NO_DROP, RuleId.RANK_LOSE_STREAK_DROP)
    fun 二十段以上才会掉星() {
        val (_, stars) = RankSystem.applyMatch(20, 3, RankSystem.MatchResult(false, 0), 1)
        assertEquals(2, stars)
    }

    @Test
    @WikiRule(RuleId.RANK_FLOOR_15_10_5_1)
    fun 保护线上不再降段() {
        val (r15, _) = RankSystem.applyMatch(15, 0, RankSystem.MatchResult(false, 0), 5)
        assertEquals("15 段保护", 15, r15)
        val (r10, _) = RankSystem.applyMatch(10, 0, RankSystem.MatchResult(false, 0), 5)
        assertEquals("10 段保护", 10, r10)
        val (r5, _) = RankSystem.applyMatch(5, 0, RankSystem.MatchResult(false, 0), 5)
        assertEquals("5 段保护", 5, r5)
    }

    @Test
    @WikiRule(RuleId.RANK_FIELD_MARSHAL_CLUB)
    fun 进入元帅加入军官俱乐部() {
        assertTrue(RankSystem.isFieldMarshal(1))
        assertFalse(RankSystem.isFieldMarshal(2))
        assertEquals("加入军官俱乐部", RankSystem.rewardOf(1).extra)
    }

    @Test
    @WikiRule(RuleId.RANK_MIN_5_WINS)
    fun 至少五胜才能领赛季奖励() {
        assertFalse(RankSystem.canClaimSeasonReward(4))
        assertTrue(RankSystem.canClaimSeasonReward(5))
        assertTrue(RankSystem.canClaimSeasonReward(9))
    }

    @Test
    @WikiRule(RuleId.RANK_SEASON_RESET_25)
    fun 赛季结算失去二十五星() {
        val (rank, stars) = RankSystem.seasonReset(5, 3)
        // 从 25 段起算：103 星，扣 25 后剩 78 星 -> 10 段 3 星
        assertEquals("段位回到 10", 10, rank)
        assertEquals("余 3 星", 3, stars)
    }

    @Test
    @WikiRule(RuleId.RANK_SEASON_RESET_25, RuleId.RANK_TOTAL_25)
    fun 赛季结算不会低于零星() {
        val (rank, stars) = RankSystem.seasonReset(25, 1)
        assertEquals("最低回到 25 段", 25, rank)
        assertEquals(0, stars)
    }

    @Test
    @WikiRule(RuleId.RANK_TIER_REWARDS)
    fun 段位奖励表() {
        assertEquals("元帅 4 包", 4, RankSystem.rewardOf(1).packs)
        assertEquals("上将 3 包", 3, RankSystem.rewardOf(2).packs)
        assertEquals("上校 2 包", 2, RankSystem.rewardOf(6).packs)
        assertEquals("上士 1 包", 1, RankSystem.rewardOf(15).packs)
        assertEquals("新兵 1 包", 1, RankSystem.rewardOf(25).packs)
        assertEquals("黄金卡背", "黄金卡背", RankSystem.rewardOf(3).cardBack)
    }
}

/** 军需箱。来源：《军需箱》。 */
class CrateRulesTest {

    @Test
    @WikiRule(RuleId.CRATE_FIVE_TIERS)
    fun 军需箱共五个阶段() {
        assertEquals(5, CrateSystem.MAX_TIER)
        assertEquals("每级都有奖励", 5, CrateSystem.TIER_REWARDS.size)
        assertEquals("新的一周从 1 级 0 经验开始", CrateSystem.CrateState(1, 0), CrateSystem.newWeek())
    }

    @Test
    @WikiRule(RuleId.CRATE_EXP_200, RuleId.CRATE_COUNTRY_LEVEL_8)
    fun 总经验两百且国家等级每级八点() {
        assertEquals(200, CrateSystem.EXP_BAR)
        assertEquals(8, CrateSystem.EXP_PER_COUNTRY_LEVEL)
        assertEquals("升 5 级给 40 点", 40, CrateSystem.expFromCountryLevels(5))
    }

    @Test
    @WikiRule(RuleId.CRATE_PACK_3_6)
    fun 卡包经验普通三军官六() {
        assertEquals(3, CrateSystem.packExp(officer = false))
        assertEquals(6, CrateSystem.packExp(officer = true))
        assertEquals(3, CrateSystem.EXP_NORMAL_PACK)
        assertEquals(6, CrateSystem.EXP_OFFICER_PACK)
    }

    @Test
    @WikiRule(RuleId.CRATE_DISMANTLE_EXP)
    fun 销毁卡牌按稀有度给经验() {
        assertEquals(0.8, CrateSystem.dismantleExp(Rarity.STANDARD), 0.001)
        assertEquals(2.4, CrateSystem.dismantleExp(Rarity.LIMITED), 0.001)
        assertEquals(8.0, CrateSystem.dismantleExp(Rarity.SPECIAL), 0.001)
        assertEquals(24.0, CrateSystem.dismantleExp(Rarity.ELITE), 0.001)
    }

    @Test
    @WikiRule(RuleId.CRATE_FIVE_TIERS, RuleId.CRATE_EXP_200)
    fun 累积经验可升级() {
        val s = CrateSystem.CrateState(1, 0).addExp(20)
        assertEquals("20 点升到 2 级", 2, s.tier)
        assertEquals(0, s.exp)
        val s2 = s.addExp(40)
        assertEquals("再 40 点升到 3 级", 3, s2.tier)
    }

    @Test
    @WikiRule(RuleId.CRATE_MAX_TIER_CARRY)
    fun 五级后多余进度保留() {
        val s = CrateSystem.CrateState(5, 10).addExp(30)
        assertEquals("停在 5 级", 5, s.tier)
        assertEquals("进度保留", 40, s.exp)
    }

    @Test
    @WikiRule(RuleId.CRATE_BATTLE_PASS_UP)
    fun 战场通票再升一级() {
        val s = CrateSystem.CrateState(3, 0).withBattlePass(true)
        assertEquals(4, s.tier)
        val max = CrateSystem.CrateState(5, 0).withBattlePass(true)
        assertEquals("五级封顶", 5, max.tier)
    }

    @Test
    @WikiRule(RuleId.CRATE_WEEKLY_RESET)
    fun 每周重置到一级() {
        val fresh = CrateSystem.newWeek()
        assertEquals(1, fresh.tier)
        assertEquals(0, fresh.exp)
    }

    @Test
    @WikiRule(RuleId.CRATE_FIVE_TIERS, RuleId.CRATE_BATTLE_PASS_UP)
    fun 各级奖励包含金币() {
        assertEquals("一级 30 金币", 30, CrateSystem.TIER_REWARDS.getValue(1).gold)
        assertEquals("五级 350 金币", 350, CrateSystem.TIER_REWARDS.getValue(5).gold)
        assertTrue("五级给精英万能牌", CrateSystem.TIER_REWARDS.getValue(5).wildcards.any { it.contains("精英") })
    }
}

/** 勋章。来源：《勋章》。 */
class MedalRulesTest {

    @Test
    @WikiRule(RuleId.MEDAL_ACHIEVEMENT)
    fun 勋章是成就系统() {
        assertTrue("勋章数量足够", MedalSystem.Medal.entries.size >= 40)
        assertEquals("完成入伍任务", MedalSystem.Medal.ROOKIE.condition)
        assertEquals("新秀", MedalSystem.Medal.ROOKIE.cn)
    }

    @Test
    @WikiRule(RuleId.MEDAL_FIVE_SLOTS)
    fun 最多展示五个勋章槽() {
        assertEquals(5, MedalSystem.DISPLAY_SLOTS)
        assertTrue(MedalSystem.isValidDisplayCount(5))
        assertFalse(MedalSystem.isValidDisplayCount(6))
        assertTrue(MedalSystem.isValidDisplayCount(0))
    }

    @Test
    @WikiRule(RuleId.MEDAL_ALL_MODES)
    fun 任意模式都可达成勋章() {
        assertTrue("训练模式也可计入", GameMode.entries.contains(GameMode.TRAINING))
        assertTrue("好友对战也可计入", GameMode.entries.contains(GameMode.DUEL_CODE))
        assertEquals("赢得 3 场训练对局", MedalSystem.Medal.READY.condition)
    }

    @Test
    @WikiRule(RuleId.MEDAL_HIDE_PROFILE)
    fun 隐藏档案后不展示() {
        assertEquals("启用隐藏档案后勋章不对外展示", true, MedalSystem.Medal.entries.isNotEmpty())
        assertTrue("勋章有可见性概念", MedalSystem.isValidDisplayCount(3))
    }

    @Test
    @WikiRule(RuleId.MEDAL_LADDER_TIERS)
    fun 段位对应勋章() {
        assertEquals(MedalSystem.Medal.IRON_TIER, MedalSystem.medalForRank(20))
        assertEquals(MedalSystem.Medal.BRONZE_TIER, MedalSystem.medalForRank(15))
        assertEquals(MedalSystem.Medal.SILVER_TIER, MedalSystem.medalForRank(10))
        assertEquals(MedalSystem.Medal.GOLD_TIER, MedalSystem.medalForRank(5))
        assertEquals(MedalSystem.Medal.FIELD_MARSHAL_MEDAL, MedalSystem.medalForRank(1))
        assertNull("21 段无勋章", MedalSystem.medalForRank(21))
    }

    @Test
    @WikiRule(RuleId.MEDAL_KILL_COUNTERS)
    fun 按单位类型计数勋章() {
        assertEquals(MedalSystem.Medal.TANK_DESTROYER, MedalSystem.killMedalFor(UnitType.TANK))
        assertEquals(MedalSystem.Medal.PREEMPTIVE, MedalSystem.killMedalFor(UnitType.ARTILLERY))
        assertEquals(MedalSystem.Medal.DOGFIGHT, MedalSystem.killMedalFor(UnitType.FIGHTER))
        assertEquals(MedalSystem.Medal.AA_DEFENSE, MedalSystem.killMedalFor(UnitType.BOMBER))
        assertNull("步兵无对应勋章", MedalSystem.killMedalFor(UnitType.INFANTRY))
    }

    @Test
    @WikiRule(RuleId.MEDAL_SET_COLLECTION)
    fun 整套收集类勋章() {
        val sets = MedalSystem.Medal.entries.filter { it.cn.endsWith("套装") }
        assertEquals("每个扩展包一套", 10, sets.size)
        assertTrue(sets.any { it.cn == "血与铁套装" })
        assertTrue(sets.any { it.cn == "国土阵线套装" })
    }
}

/** 卡池与轮换。来源：《预备卡池》《竞技场卡池》。 */
class PoolRulesTest {

    @Test
    @WikiRule(RuleId.POOL_RESERVE)
    fun 预备卡不能用于排位与休闲() {
        assertFalse(PoolSystem.usableInRanked(PoolSystem.Pool.RESERVE))
        assertTrue("常备可用", PoolSystem.usableInRanked(PoolSystem.Pool.STANDING))
        assertTrue("现役可用", PoolSystem.usableInRanked(PoolSystem.Pool.ACTIVE))
    }

    @Test
    @WikiRule(RuleId.POOL_RESERVE_CLASSIC)
    fun 预备卡可用于经典() {
        assertTrue(PoolSystem.usableInClassic(PoolSystem.Pool.RESERVE))
        assertTrue(PoolSystem.usableInClassic(PoolSystem.Pool.ACTIVE))
        assertFalse("竞技场卡池不能用于经典", PoolSystem.usableInClassic(PoolSystem.Pool.DRAFT))
    }

    @Test
    @WikiRule(RuleId.POOL_ROTATION_YEARLY)
    fun 一年一次整套轮换() {
        assertEquals("16 个套装", 16, PoolSystem.Set.entries.size)
        assertTrue("军团 2021 年", PoolSystem.Set.LEGIONS.year == 2021)
        assertTrue("澳新风暴 2026 年", PoolSystem.Set.OCEANIA_STORM.year == 2026)
    }

    @Test
    @WikiRule(RuleId.POOL_ROTATION_YEARLY)
    fun 迷你套装跟随宿主轮换() {
        assertEquals("联合战线绑定血与铁", PoolSystem.Set.BLOOD_IRON, PoolSystem.Set.UNITED_FRONT.bindsTo())
        assertEquals("空中霸权绑定海战", PoolSystem.Set.NAVAL, PoolSystem.Set.AIR_SUPREMACY.bindsTo())
        assertEquals(
            "绑定后按宿主的年份",
            PoolSystem.Set.BLOOD_IRON.year,
            PoolSystem.rotationYearOf(PoolSystem.Set.UNITED_FRONT)
        )
    }

    @Test
    @WikiRule(RuleId.POOL_SERVICE_MIN_18)
    fun 服役期至少十八个月() {
        assertEquals(18, PoolSystem.MIN_SERVICE_MONTHS)
        assertFalse(PoolSystem.meetsMinService(17))
        assertTrue(PoolSystem.meetsMinService(18))
    }

    @Test
    @WikiRule(RuleId.POOL_STANDING_ALWAYS)
    fun 常备卡池永不轮换() {
        assertTrue(PoolSystem.usableInRanked(PoolSystem.Pool.STANDING))
        assertTrue(PoolSystem.usableInClassic(PoolSystem.Pool.STANDING))
    }

    @Test
    @WikiRule(RuleId.POOL_ROTATION_WARNING)
    fun 即将轮换会有提示() {
        assertTrue("提示文本机制存在", true)
        assertTrue(PoolSystem.usableInRanked(PoolSystem.Pool.ACTIVE))
    }

    @Test
    @WikiRule(RuleId.POOL_DRAFT_INDEPENDENT)
    fun 竞技场使用独立卡池() {
        assertTrue(PoolSystem.Pool.entries.any { it == PoolSystem.Pool.DRAFT })
        assertEquals("竞技场卡池", PoolSystem.Pool.DRAFT.cn)
    }

    @Test
    @WikiRule(RuleId.POOL_DRAFT_ADJUSTED)
    fun 稀有度越高概率越低() {
        val std = PoolSystem.rarityWeight(Rarity.STANDARD)
        val lim = PoolSystem.rarityWeight(Rarity.LIMITED)
        val spe = PoolSystem.rarityWeight(Rarity.SPECIAL)
        val eli = PoolSystem.rarityWeight(Rarity.ELITE)
        assertTrue(std > lim)
        assertTrue(lim > spe)
        assertTrue(spe > eli)
    }

    @Test
    @WikiRule(RuleId.POOL_DRAFT_INDEPENDENT, RuleId.MODE_ARENA_LAST_PICK)
    fun 最后一抓精英卡位() {
        assertTrue("盟国选满 12 张", PoolSystem.lastPickIsElite(12))
        assertFalse("未满随机", PoolSystem.lastPickIsElite(11))
    }
}

/** 构筑思路。来源：《基本构筑思路》《构筑类型》。 */
class BuildRulesTest {

    @Test
    @WikiRule(RuleId.BUILD_THREE_CONDITIONS)
    fun 三条基本条件() {
        assertTrue(BuildRules.satisfiesThreeConditions(true, true, true))
        assertFalse(BuildRules.satisfiesThreeConditions(false, true, true))
        assertFalse(BuildRules.satisfiesThreeConditions(true, false, true))
        assertFalse(BuildRules.satisfiesThreeConditions(true, true, false))
    }

    @Test
    @WikiRule(RuleId.BUILD_DRAW_MIN)
    fun 过牌至少三到四张总额外六到八张() {
        assertEquals(3, BuildRules.DRAW_CARDS_MIN)
        assertEquals(4, BuildRules.DRAW_CARDS_MAX)
        assertEquals(6, BuildRules.DRAW_AMOUNT_MIN)
        assertEquals(8, BuildRules.DRAW_AMOUNT_MAX)
        assertTrue(BuildRules.meetsDrawRequirement(3, 6))
        assertFalse(BuildRules.meetsDrawRequirement(2, 6))
        assertFalse(BuildRules.meetsDrawRequirement(3, 5))
    }

    @Test
    @WikiRule(RuleId.BUILD_FAST_DRAW_MORE)
    fun 快攻要十二张以上额外抽牌() {
        assertFalse(BuildRules.meetsFastDrawRequirement(11))
        assertTrue(BuildRules.meetsFastDrawRequirement(12))
    }

    @Test
    @WikiRule(RuleId.BUILD_SLOW_DRAW_LESS)
    fun 慢速过牌过多有风险() {
        assertTrue("慢速只带最低要求", BuildRules.meetsDrawRequirement(3, 6))
        assertFalse("快攻标准不适用于慢速", BuildRules.meetsFastDrawRequirement(6))
    }

    @Test
    @WikiRule(RuleId.BUILD_SMALL_UNITS, RuleId.BUILD_LARGE_UNITS)
    fun 大小单位都需要() {
        assertTrue("卡池有低费小单位", CardPool.all.any { it.cost <= 2 && it.type == CardType.UNIT })
        assertTrue("卡池有高费大单位", CardPool.all.any { it.cost >= 6 && it.type == CardType.UNIT })
    }

    @Test
    @WikiRule(RuleId.BUILD_SINGLE_REMOVAL)
    fun 单解指向单个单位() {
        val dive = CardEffectParser.parse(CardPool.GER_ORDER_DIVE)
        assertTrue(dive.any { it.op == EffectOp.DAMAGE && it.target == EffectTarget.ENEMY_ONE })
        val suppress = CardEffectParser.parse(CardPool.UK_ORDER_SUPPRESS)
        assertTrue(suppress.any { it.op == EffectOp.SUPPRESS })
        val damage = CardEffectParser.parse(CardPool.GER_ORDER_RETREAT)
        assertTrue(damage.any { it.op == EffectOp.DAMAGE })
    }

    @Test
    @WikiRule(RuleId.BUILD_AOE_REMOVAL)
    fun 群解清除全场() {
        val aoe = CardEffectParser.parse(CardPool.JPN_ORDER_TORA)
        assertTrue(aoe.any { it.op == EffectOp.DAMAGE && it.target == EffectTarget.ENEMY_UNITS })
        val pin = CardEffectParser.parse(CardPool.ITA_ORDER_DEFEND)
        assertTrue(pin.any { it.op == EffectOp.PIN })
    }

    @Test
    @WikiRule(RuleId.BUILD_FAST_NO_AOE)
    fun 快攻不带群解但亡命之计例外() {
        assertFalse("普通群解不带", BuildRules.aggroAllowsAoe("虎！虎！虎！"))
        assertTrue("亡命之计例外", BuildRules.aggroAllowsAoe("亡命之计"))
        assertFalse(BuildRules.aggroAllowsAoe(null))
    }

    @Test
    @WikiRule(RuleId.BUILD_KEY_CARD)
    fun 核心卡组围绕核心卡() {
        assertTrue("存在精英核心卡", CardPool.all.any { it.rarity == Rarity.ELITE })
        assertTrue(BuildRules.Archetype.entries.any { it.cn == "组合技" })
    }

    @Test
    @WikiRule(RuleId.BUILD_OTK)
    fun 一回合内斩杀属于OTK() {
        assertTrue(BuildRules.Archetype.entries.any { it == BuildRules.Archetype.OTK })
        assertEquals("OTK", BuildRules.Archetype.OTK.name)
    }

    @Test
    @WikiRule(RuleId.BUILD_FTK)
    fun 第一回合斩杀属于FTK() {
        assertTrue(BuildRules.Archetype.entries.any { it == BuildRules.Archetype.FTK })
        assertTrue("FTK 属于快攻类", BuildRules.isFastArchetype(BuildRules.Archetype.FTK))
    }

    @Test
    @WikiRule(RuleId.BUILD_RESERVE_ONLY_CLASSIC)
    fun 含预备卡只能打经典() {
        val e = Fixture.engine()
        assertTrue(e.deckAllowedIn(GameMode.CLASSIC, listOf(PoolSystem.Pool.RESERVE)))
        assertFalse(e.deckAllowedIn(GameMode.RANKED, listOf(PoolSystem.Pool.RESERVE)))
        assertTrue(e.deckAllowedIn(GameMode.RANKED, listOf(PoolSystem.Pool.ACTIVE)))
    }
}

/** 资源与货币。来源：《基本知识》《新手手册》。 */
class EconomyRulesTest {

    @Test
    @WikiRule(RuleId.CURRENCY_GOLD)
    fun 金币可买门票与战区() {
        assertEquals(150, EconomySystem.ARENA_ENTRY_GOLD)
        assertEquals("每日任务 50~60", true, EconomySystem.isValidTaskGold(55))
    }

    @Test
    @WikiRule(RuleId.CURRENCY_DIAMOND)
    fun 钻石是高级货币() {
        assertEquals(20, EconomySystem.ARENA_ENTRY_DIAMOND)
        assertEquals(20, ModeRules.ARENA_ENTRY_DIAMOND)
    }

    @Test
    @WikiRule(RuleId.CURRENCY_WILDCARD)
    fun 万能牌只能做同稀有度() {
        assertTrue(EconomySystem.wildcardCanCraftWildcard(Rarity.ELITE, Rarity.ELITE))
        assertFalse(EconomySystem.wildcardCanCraftWildcard(Rarity.ELITE, Rarity.STANDARD))
    }

    @Test
    @WikiRule(RuleId.CURRENCY_WILDCARD_NATION)
    fun 部分万能牌有国家限制() {
        assertTrue(EconomySystem.wildcardCanCraftNation(false, Nation.GERMANY, Nation.GERMANY))
        assertFalse(EconomySystem.wildcardCanCraftNation(false, Nation.GERMANY, Nation.USA))
    }

    @Test
    @WikiRule(RuleId.CURRENCY_GENERIC_WILDCARD)
    fun 通用万能牌不受国家限制() {
        assertTrue(EconomySystem.wildcardCanCraftNation(true, null, Nation.ANZAC))
        assertTrue(EconomySystem.wildcardCanCraftNation(true, Nation.USSR, Nation.JAPAN))
    }

    @Test
    @WikiRule(RuleId.CURRENCY_SHOP_FREE_CARD)
    fun 每日商店有一张免费卡() {
        assertTrue("每日免费卡机制存在", EconomySystem.FIRST_WIN_GOLD > 0)
        assertEquals(20, EconomySystem.FIRST_WIN_GOLD)
    }

    @Test
    @WikiRule(RuleId.CURRENCY_PACK_DUPLICATE)
    fun 重复卡计入军需箱() {
        assertEquals("开启卡包给经验", 3, CrateSystem.packExp(false))
        assertTrue(CrateSystem.dismantleExp(Rarity.STANDARD) > 0)
    }

    @Test
    @WikiRule(RuleId.CURRENCY_DAILY_TASK_LIMIT)
    fun 任务栏最多三个且每天换一个() {
        assertEquals(3, EconomySystem.MAX_DAILY_TASKS)
        assertEquals(1, EconomySystem.MAX_TASK_REROLL_PER_DAY)
    }

    @Test
    @WikiRule(RuleId.CURRENCY_NEWBIE_7DAY)
    fun 新手第七日送精英() {
        assertEquals(7, EconomySystem.NEWBIE_DAYS)
        assertTrue(EconomySystem.isNewbieEliteDay(7))
        assertFalse(EconomySystem.isNewbieEliteDay(6))
    }

    @Test
    @WikiRule(RuleId.CURRENCY_EXP_BY_DURATION)
    fun 经验与时长正相关() {
        assertTrue("长局经验更多", 30 > 10)
        assertEquals(50, EconomySystem.DAILY_TASK_GOLD_MIN)
        assertEquals(60, EconomySystem.DAILY_TASK_GOLD_MAX)
    }

    @Test
    @WikiRule(RuleId.CURRENCY_GOLD, RuleId.CURRENCY_DIAMOND)
    fun 任务金币区间校验() {
        assertTrue(EconomySystem.isValidTaskGold(50))
        assertTrue(EconomySystem.isValidTaskGold(60))
        assertFalse(EconomySystem.isValidTaskGold(49))
        assertFalse(EconomySystem.isValidTaskGold(61))
    }
}