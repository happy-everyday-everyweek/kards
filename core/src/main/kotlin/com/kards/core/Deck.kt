package com.kards.core

/** 卡组构筑校验结果，携带具体违规条目。 */
data class DeckViolation(val rule: RuleId, val message: String)

data class Deck(val mainNation: Nation, val hq: Card, val cards: List<Card>) {
    val total get() = cards.size + 1
}

/**
 * 卡组构筑校验。规则来源：《卡组》《国家》。
 */
object DeckValidator {
    const val TOTAL_CARDS = 40
    const val ALLY_LIMIT = 12

    fun validate(deck: Deck): List<DeckViolation> {
        val out = mutableListOf<DeckViolation>()

        if (deck.hq.type != CardType.HQ) {
            out += DeckViolation(RuleId.CARD_FOUR_TYPES, "卡组第一张必须是总部")
        }
        if (deck.hq.nation != deck.mainNation) {
            out += DeckViolation(RuleId.HQ_ONE_PER_DECK, "总部必须与主国一致")
        }
        if (deck.total != TOTAL_CARDS) {
            out += DeckViolation(RuleId.DECK_40_TOTAL, "卡组应为 1 总部 + 39 张，实际 ${deck.total}")
        }

        val hqCount = deck.cards.count { it.type == CardType.HQ }
        if (hqCount > 0) {
            out += DeckViolation(RuleId.HQ_ONE_PER_DECK, "卡组不能额外携带总部")
        }

        val allyCards = deck.cards.filter { it.nation != deck.mainNation }
        if (allyCards.size > ALLY_LIMIT) {
            out += DeckViolation(RuleId.DECK_ALLY_MAX_12, "盟国卡牌 ${allyCards.size} 张，超过 12")
        }

        deck.cards.filter { it.nation != deck.mainNation && it.rarity == Rarity.ELITE }
            .forEach { out += DeckViolation(RuleId.DECK_ALLY_NO_ELITE, "盟国不能携带精英卡：${it.name}") }

        deck.cards.groupBy { it.name }.forEach { (_, group) ->
            val card = group.first()
            if (group.size > card.rarity.maxCopies) {
                // 同名上限按稀有度：普通 4 / 限定 3 / 特殊 2 / 精英 1。
                out += DeckViolation(
                    RuleId.DECK_COPY_LIMIT_BY_RARITY,
                    "${card.name} 携带 ${group.size} 张，上限 ${card.rarity.maxCopies}"
                )
            }
        }

        deck.cards.filter { it.nation != deck.mainNation && !it.nation.canBeMain }
            .forEach { /* 合法：只能做盟国的国家可以在盟国位出现 */ }

        if (!deck.mainNation.canBeMain) {
            out += DeckViolation(RuleId.NATION_FIVE_MAJOR, "${deck.mainNation.cn} 不能作为主国")
        }

        // 流亡卡牌可用于原籍或流亡国卡组。
        deck.cards.filter { it.exileNation != null }.forEach { card ->
            val acceptable = card.exileNation == deck.mainNation
            if (!acceptable && card.nation != deck.mainNation && !isAlly(deck, card)) {
                out += DeckViolation(RuleId.NATION_EXILE_BOTH, "流亡卡牌国家不匹配：${card.name}")
            }
        }

        return out
    }

    /** 流亡卡牌计入哪个国家：优先原国籍，其次流亡国。 */
    fun isAlly(deck: Deck, card: Card): Boolean {
        if (card.nation == deck.mainNation) return false
        return true
    }

    fun isValid(deck: Deck): Boolean = validate(deck).isEmpty()
}

/** 模式与卡池。来源：《游戏模式》。 */
enum class GameMode(val cn: String) {
    RANKED("排位"),
    CASUAL("休闲"),
    CLASSIC("经典"),
    TRAINING("训练"),
    ARENA("竞技场"),
    DUEL_CODE("战斗代码")
}

/** 训练模式的五个 AI。来源：《游戏模式》。 */
enum class AiCommander(val cn: String, val nation: Nation) {
    IVANOV("Ivanov", Nation.USSR),
    SATO("Sato", Nation.JAPAN),
    SMITH("Smith", Nation.UK),
    MULLER("Muller", Nation.GERMANY),
    WILLIAMS("Williams", Nation.USA)
}

object ModeRules {
    /** 只有排位晋升军衔。 */
    fun affectsRank(mode: GameMode): Boolean = mode == GameMode.RANKED

    /** 经典需要卡组含预备卡。 */
    fun requiresReservePool(mode: GameMode): Boolean = mode == GameMode.CLASSIC

    /** 竞技场奖励表：胜场 -> 金币。来源：《游戏模式》。 */
    val ARENA_REWARDS: Map<Int, Int> = mapOf(0 to 0, 1 to 0, 2 to 20, 3 to 40, 4 to 60, 5 to 90, 6 to 150, 7 to 180)

    val ARENA_LOOT: Map<Int, Int> = mapOf(0 to 1, 1 to 1, 2 to 1, 3 to 1, 4 to 1, 5 to 1, 6 to 1, 7 to 2)

    const val ARENA_MAX_WINS = 7
    const val ARENA_MAX_LOSSES = 3
    const val ARENA_ENTRY_GOLD = 150
    const val ARENA_ENTRY_DIAMOND = 20

    /** 竞技场是否应结算：7 胜或 3 败。 */
    fun arenaShouldSettle(wins: Int, losses: Int): Boolean = wins >= ARENA_MAX_WINS || losses >= ARENA_MAX_LOSSES

    fun arenaGold(wins: Int): Int = ARENA_REWARDS[wins.coerceIn(0, 7)]!!

    /** 战斗代码：3-15 字符，emoji 不可用。 */
    fun isValidDuelCode(code: String): Boolean {
        val length = code.codePointCount(0, code.length)
        if (length < 3 || length > 15) return false
        var i = 0
        while (i < code.length) {
            val cp = code.codePointAt(i)
            if (cp in 0x1F000..0x1FAFF) return false
            if (cp in 0x2600..0x27BF) return false
            i += Character.charCount(cp)
        }
        return true
    }
}