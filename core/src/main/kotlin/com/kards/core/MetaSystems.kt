package com.kards.core

/**
 * 军衔系统。来源：《军衔》。
 * 25 个段位：1 段元帅 … 25 段新兵。除元帅外每段 5 星。
 */
object RankSystem {
    const val TOTAL_RANKS = 25
    const val STARS_PER_RANK = 5
    const val FIELD_MARSHAL = 1
    const val SEASON_RESET_STARS = 25
    const val MIN_WINS_FOR_REWARD = 5
    const val STREAK_THRESHOLD = 3

    val rankNames: Map<Int, String> = mapOf(
        1 to "元帅", 2 to "上将", 3 to "中将", 4 to "少将", 5 to "准将",
        6 to "上校", 7 to "中校", 8 to "少校", 9 to "上尉", 10 to "中尉",
        11 to "总军士长", 12 to "军士长", 13 to "准军士长", 14 to "士官长", 15 to "上士",
        16 to "参谋军士", 17 to "中士", 18 to "下士", 19 to "准尉", 20 to "军校生",
        21 to "技术兵", 22 to "上等兵", 23 to "一等兵", 24 to "二等兵", 25 to "新兵"
    )

    /** 段位保护线：达到后赛季内不再降到更低段位。 */
    val FLOORS = setOf(15, 10, 5, 1)

    data class RankReward(val cardBack: String?, val foil: String?, val packs: Int, val extra: String?)

    fun rewardOf(rank: Int): RankReward = when (rank) {
        1 -> RankReward("元帅卡背", "精英闪卡", 4, "加入军官俱乐部")
        in 2..5 -> RankReward("黄金卡背", "特殊闪卡", 3, null)
        in 6..10 -> RankReward("白银卡背", "限定闪卡", 2, null)
        in 11..16 -> RankReward("青铜卡背", "普通闪卡", 1, null)
        in 17..20 -> RankReward("黑铁卡背", "普通闪卡", 1, null)
        else -> RankReward(null, null, 1, null)
    }

    data class MatchResult(val win: Boolean, val winStreak: Int)

    /**
     * 一场排位后的段位与星数。
     * 胜利 +1 星；连胜至少 3 胜时 +2 星；0 星时连续 3 败掉段；
     * 20 段以下失败不降；达到 15/10/5/1 段后受保护。
     */
    fun applyMatch(rank: Int, stars: Int, result: MatchResult, loseStreak: Int): Pair<Int, Int> {
        if (result.win) {
            val gain = if (result.winStreak >= STREAK_THRESHOLD) 2 else 1
            var newStars = stars + gain
            var newRank = rank
            while (newStars > STARS_PER_RANK && newRank > FIELD_MARSHAL) {
                newStars -= STARS_PER_RANK
                newRank -= 1
            }
            if (newRank == FIELD_MARSHAL) newStars = newStars.coerceAtMost(STARS_PER_RANK)
            return newRank to newStars
        }

        if (rank >= 21) return rank to stars
        // 0 星且连续 3 败：直接掉段（保护线除外）
        if (stars == 0 && loseStreak >= 3 && rank !in FLOORS) {
            return (rank + 1).coerceAtMost(TOTAL_RANKS) to STARS_PER_RANK
        }
        if (rank in FLOORS && stars == 0) return rank to 0
        val newStars = stars - 1
        if (newStars >= 0) return rank to newStars
        if (loseStreak >= 3) {
            val newRank = (rank + 1).coerceAtMost(TOTAL_RANKS)
            return newRank to STARS_PER_RANK
        }
        return rank to 0
    }

    /** 赛季结算：所有人失去 25 颗星。 */
    fun seasonReset(rank: Int, stars: Int): Pair<Int, Int> {
        // 以「从 25 段起算的累计星数」计算，扣 25 星后向下取整回段位。
        val fromBottom = (TOTAL_RANKS - rank) * STARS_PER_RANK + stars
        val after = (fromBottom - SEASON_RESET_STARS).coerceAtLeast(0)
        val newRank = (TOTAL_RANKS - after / STARS_PER_RANK).coerceIn(1, TOTAL_RANKS)
        val newStars = after % STARS_PER_RANK
        return newRank to newStars
    }

    fun canClaimSeasonReward(rankedWins: Int): Boolean = rankedWins >= MIN_WINS_FOR_REWARD

    fun isFieldMarshal(rank: Int): Boolean = rank == FIELD_MARSHAL

    fun nameOf(rank: Int): String = rankNames[rank] ?: "未知"

    fun isValidRank(rank: Int): Boolean = rank in 1..TOTAL_RANKS
}

/** 军需箱。来源：《军需箱》。 */
object CrateSystem {
    const val MAX_TIER = 5
    const val EXP_BAR = 200
    const val EXP_PER_COUNTRY_LEVEL = 8
    const val EXP_NORMAL_PACK = 3
    const val EXP_OFFICER_PACK = 6

    val TIER_REQUIREMENTS: Map<Int, Int> = mapOf(1 to 20, 2 to 40, 3 to 50, 4 to 100)

    data class CrateReward(val wildcards: List<String>, val foils: List<String>, val gold: Int)

    val TIER_REWARDS: Map<Int, CrateReward> = mapOf(
        1 to CrateReward(listOf("1 普通万能牌"), listOf(), 30),
        2 to CrateReward(listOf("2 普通万能牌"), listOf("2 普通闪卡"), 80),
        3 to CrateReward(listOf("1 限定万能牌", "2 普通万能牌"), listOf("2 限定闪卡"), 150),
        4 to CrateReward(listOf("1 特殊万能牌", "1 限定万能牌"), listOf("1 特殊闪卡", "2 限定闪卡"), 250),
        5 to CrateReward(listOf("1 精英万能牌", "1 特殊万能牌", "1 限定万能牌"), listOf("2 特殊闪卡"), 350)
    )

    /** 销毁卡牌按稀有度给的经验。 */
    fun dismantleExp(rarity: Rarity): Double = when (rarity) {
        Rarity.STANDARD -> 0.8
        Rarity.LIMITED -> 2.4
        Rarity.SPECIAL -> 8.0
        Rarity.ELITE -> 24.0
    }

    data class CrateState(val tier: Int, val exp: Int) {
        /** 到达五级后不能升级，多余进度保留。 */
        fun addExp(amount: Int): CrateState {
            if (tier >= MAX_TIER) return copy(exp = exp + amount)
            var t = tier
            var e = exp + amount
            while (t < MAX_TIER) {
                val need = TIER_REQUIREMENTS[t] ?: break
                if (e < need) break
                e -= need
                t++
            }
            return CrateState(t, e)
        }

        fun withBattlePass(active: Boolean): CrateState =
            if (active && tier < MAX_TIER) CrateState(tier + 1, exp) else this

        fun rewards(): CrateReward = TIER_REWARDS.getValue(tier)
    }

    fun newWeek(): CrateState = CrateState(1, 0)

    fun expFromCountryLevels(levels: Int): Int = levels * EXP_PER_COUNTRY_LEVEL

    fun packExp(officer: Boolean): Int = if (officer) EXP_OFFICER_PACK else EXP_NORMAL_PACK
}

/** 勋章。来源：《勋章》。 */
object MedalSystem {
    const val DISPLAY_SLOTS = 5

    enum class Medal(val cn: String, val condition: String) {
        ROOKIE("新秀", "完成入伍任务"),
        ENLISTED("从军者", "完成新手战役"),
        MOBILIZE("动员", "收集所有 12 级战备卡组"),
        READY("准备", "赢得 3 场训练对局"),
        GROUNDED("脚踏实地", "赢得 5 场 PVP 对局"),
        VETERAN("前线老兵", "赢得 50 场 PVP 对局"),
        LEGEND("传说", "赢得 500 场 PVP 对局"),
        DRAFTEE("应征者", "赢得 5 场竞技场对局"),
        TACTICIAN("战术家", "赢得 50 场竞技场对局"),
        STRATEGIST("战略大师", "赢得 500 场竞技场对局"),
        BATTLE_COMMAND("战地指挥", "完成一整轮竞技场"),
        FLAWLESS("完美胜利", "在没有败局的情况下完成一轮竞技场"),
        FIRST_BLOOD("第一滴血", "赢得 1 个赛事奖杯"),
        KING_OF_KINGS("王中王", "赢得 10 个赛事奖杯"),
        KARDS_MASTER("KARDS 大师", "赢得 100 个赛事奖杯"),
        FRONT_EXPERT("前线专家", "单个国家达到 50 级"),
        NATION_MASTER("国家大师", "所有国家达到 50 级"),
        WARLORD("世界军阀", "所有国家达到 150 级"),
        IRON_CROSS("铁十字勋章", "德国达到 150 级"),
        PURPLE_HEART("紫心勋章", "美国达到 150 级"),
        HERO_SOVIET("苏联英雄勋章", "苏联达到 150 级"),
        VICTORIA_CROSS("维多利亚十字勋章", "英国达到 150 级"),
        CAMPAIGN_MEDAL("从军记章", "日本达到 150 级"),
        IRON_TIER("黑铁", "达到 20 段"),
        BRONZE_TIER("青铜", "达到 15 段"),
        SILVER_TIER("白银", "达到 10 段"),
        GOLD_TIER("黄金", "达到 5 段"),
        FIELD_MARSHAL_MEDAL("元帅", "进入军官俱乐部"),
        THIRD_OFFICER("三等军官", "进入军官俱乐部 3 次"),
        OFFICER_CLUB("军官俱乐部", "进入军官俱乐部 10 次"),
        COMMANDER("总司令", "连续 12 个赛季进入军官俱乐部"),
        DUTY("职责", "完成 100 个每日任务"),
        MEATGRINDER("绞肉机", "消灭 500 个敌方坦克"),
        TANK_DESTROYER("坦歼", "消灭 500 个敌方坦克"),
        PREEMPTIVE("先发制人", "消灭 500 个敌方炮兵"),
        DOGFIGHT("狗斗", "消灭 500 个敌方战斗机"),
        AA_DEFENSE("防空", "消灭 500 个敌方轰炸机"),
        KILL_KING("击杀王", "消灭 5000 个敌方单位"),
        SET_BASIC("基础套装", "收集所有基础卡牌"),
        SET_LOYALTY("忠诚套装", "收集所有忠诚卡牌"),
        SET_BREAKTHROUGH("突破套装", "收集所有突破卡牌"),
        SET_LEGIONS("军团套装", "收集所有军团卡牌"),
        SET_COMRADES("战友套装", "收集所有战友卡牌"),
        SET_WINTER("冬季战争套装", "收集所有冬季战争卡牌"),
        SET_COVERT("秘密行动套装", "收集所有秘密行动卡牌"),
        SET_BLOOD_IRON("血与铁套装", "收集所有血与铁卡牌"),
        SET_NAVAL("海战套装", "收集所有海战卡牌"),
        SET_HOMEFRONT("国土阵线套装", "收集所有国土阵线卡牌")
    }

    fun medalForRank(rank: Int): Medal? = when {
        rank <= 1 -> Medal.FIELD_MARSHAL_MEDAL
        rank <= 5 -> Medal.GOLD_TIER
        rank <= 10 -> Medal.SILVER_TIER
        rank <= 15 -> Medal.BRONZE_TIER
        rank <= 20 -> Medal.IRON_TIER
        else -> null
    }

    fun killMedalFor(type: UnitType): Medal? = when (type) {
        UnitType.TANK -> Medal.TANK_DESTROYER
        UnitType.ARTILLERY -> Medal.PREEMPTIVE
        UnitType.FIGHTER -> Medal.DOGFIGHT
        UnitType.BOMBER -> Medal.AA_DEFENSE
        UnitType.INFANTRY -> null
    }

    fun isValidDisplayCount(n: Int): Boolean = n in 0..DISPLAY_SLOTS
}

/** 卡池与轮换。来源：《预备卡池》《竞技场卡池》。 */
object PoolSystem {
    const val MIN_SERVICE_MONTHS = 18

    enum class Pool(val cn: String) {
        STANDING("常备卡池"),
        ACTIVE("现役卡池"),
        RESERVE("预备卡池"),
        DRAFT("竞技场卡池")
    }

    enum class Set(val cn: String, val year: Int, val isMini: Boolean = false) {
        BASE("基础", 2019),
        LOYALTY("忠诚", 2019),
        WAR_ZONE("战区", 2020),
        BREAKTHROUGH("突破", 2020),
        LEGIONS("军团", 2021),
        COMRADES("战友", 2023),
        WINTER_WAR("冬季战争", 2023),
        COVERT_OPS("秘密行动", 2024),
        BLOOD_IRON("血与铁", 2024),
        UNITED_FRONT("联合战线", 2025, isMini = true),
        NAVAL("海战", 2025),
        AIR_SUPREMACY("空中霸权", 2025, isMini = true),
        HOMEFRONT("国土阵线", 2025),
        HOMEFRONT_EARLY("国土阵线：早期战争", 2026, isMini = true),
        OCEANIA_STORM("澳新风暴", 2026),
        RING_OF_FIRE("澳新风暴：火山环带", 2026, isMini = true);

        fun bindsTo(): Set? = when (this) {
            UNITED_FRONT -> BLOOD_IRON
            AIR_SUPREMACY -> NAVAL
            HOMEFRONT_EARLY -> HOMEFRONT
            RING_OF_FIRE -> OCEANIA_STORM
            else -> null
        }
    }

    fun usableInRanked(pool: Pool): Boolean =
        pool == Pool.STANDING || pool == Pool.ACTIVE || pool == Pool.DRAFT

    fun usableInClassic(pool: Pool): Boolean = pool != Pool.DRAFT

    fun rotationYearOf(set: Set): Int = set.bindsTo()?.year ?: set.year

    fun meetsMinService(months: Int): Boolean = months >= MIN_SERVICE_MONTHS

    fun rarityWeight(rarity: Rarity): Double = when (rarity) {
        Rarity.STANDARD -> 1.0
        Rarity.LIMITED -> 0.45
        Rarity.SPECIAL -> 0.15
        Rarity.ELITE -> 0.04
    }

    fun lastPickIsElite(allyCardsPicked: Int, allyLimit: Int = DeckValidator.ALLY_LIMIT): Boolean =
        allyCardsPicked >= allyLimit
}

/** 构筑思路指标。来源：《基本构筑思路》《构筑类型》。 */
object BuildRules {
    const val DRAW_CARDS_MIN = 3
    const val DRAW_CARDS_MAX = 4
    const val DRAW_AMOUNT_MIN = 6
    const val DRAW_AMOUNT_MAX = 8
    const val FAST_DRAW_AMOUNT = 12

    fun meetsDrawRequirement(drawCardCount: Int, totalExtraDraw: Int): Boolean =
        drawCardCount >= DRAW_CARDS_MIN && totalExtraDraw >= DRAW_AMOUNT_MIN

    fun meetsFastDrawRequirement(totalExtraDraw: Int): Boolean =
        totalExtraDraw >= FAST_DRAW_AMOUNT

    fun satisfiesThreeConditions(canDamage: Boolean, canDefend: Boolean, canKeepCards: Boolean): Boolean =
        canDamage && canDefend && canKeepCards

    enum class Archetype(val cn: String) {
        AGGRO("快攻"),
        MIDRANGE("中速"),
        CONTROL("慢速"),
        OTK("OTK"),
        FTK("FTK"),
        COMBO("组合技"),
        ALPINE("山地"),
        COVERT("隐蔽"),
        RESERVE("预备")
    }

    fun aggroAllowsAoe(aoeName: String?): Boolean = aoeName == "亡命之计"

    fun isFastArchetype(a: Archetype): Boolean = a == Archetype.AGGRO || a == Archetype.FTK
}

/** 资源与货币。来源：《基本知识》《新手手册》。 */
object EconomySystem {
    const val DAILY_TASK_GOLD_MIN = 50
    const val DAILY_TASK_GOLD_MAX = 60
    const val FIRST_WIN_GOLD = 20
    const val ARENA_ENTRY_GOLD = 150
    const val ARENA_ENTRY_DIAMOND = 20
    const val MAX_DAILY_TASKS = 3
    const val MAX_TASK_REROLL_PER_DAY = 1
    const val NEWBIE_DAYS = 7

    fun wildcardCanCraftWildcard(wildcard: Rarity, target: Rarity): Boolean = wildcard == target

    fun wildcardCanCraftNation(isGeneric: Boolean, wildcardNation: Nation?, targetNation: Nation): Boolean =
        isGeneric || wildcardNation == targetNation

    fun isValidTaskGold(gold: Int): Boolean = gold in DAILY_TASK_GOLD_MIN..DAILY_TASK_GOLD_MAX

    fun isNewbieEliteDay(day: Int): Boolean = day == NEWBIE_DAYS
}