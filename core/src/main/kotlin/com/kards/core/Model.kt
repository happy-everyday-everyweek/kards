package com.kards.core

/**
 * 十国。来源：kards.miraheze.org《国家》、wiki.biligame.com/kards《首页》。
 * 前五国既可做主国也可做盟国；后五国只能做盟国，且没有精英卡。
 */
enum class Nation(val cn: String, val canBeMain: Boolean, val hasElite: Boolean) {
    GERMANY("德国", true, true),
    USA("美国", true, true),
    UK("英国", true, true),
    USSR("苏联", true, true),
    JAPAN("日本", true, true),
    FRANCE("法国", false, false),
    ITALY("意大利", false, false),
    POLAND("波兰", false, false),
    FINLAND("芬兰", false, false),
    ANZAC("澳新军团", false, false);
}

/** 卡牌四大类型。来源：《卡牌》。 */
enum class CardType(val cn: String) {
    HQ("总部"),
    UNIT("单位"),
    ORDER("指令"),
    COUNTERMEASURE("反制")
}

/** 单位五种类型。来源：《卡牌》《词条》。 */
enum class UnitType(val cn: String) {
    INFANTRY("步兵"),
    TANK("坦克"),
    FIGHTER("战斗机"),
    BOMBER("轰炸机"),
    ARTILLERY("炮兵")
}

/** 稀有度与同名携带上限。来源：《卡组》。 */
enum class Rarity(val cn: String, val maxCopies: Int) {
    STANDARD("普通", 4),
    LIMITED("限定", 3),
    SPECIAL("特殊", 2),
    ELITE("精英", 1)
}

/** 词条。来源：《词条》《卡牌》与 bwiki 关键词解释。 */
enum class Keyword(val cn: String) {
    AMBUSH("伏击"),
    BLITZ("闪击"),
    FURY("奋战"),
    GUARD("守护"),
    HEAVY_ARMOR("重甲"),
    SHOCK("冲击"),
    SMOKE("烟幕"),
    DEPLOY("部署"),
    DESTRUCTION("亡计"),
    REPAIR("修复"),
    COUNTDOWN("倒计时"),
    IMMUNE("免疫"),
    MOBILIZE("动员"),
    PIN("压制"),
    ALPINE("山地"),
    DEVELOP("开发"),
    INTEL("情报"),
    SUPPRESS("抑制"),
    TAKE_CONTROL("控制"),
    RETREAT("撤退"),
    EXILE("流亡"),
    VETERAN("老兵"),
    SALVAGE("收缴"),
    PINCER("钳击"),
    COVERT("隐蔽"),
    CONVERT("转换"),
    BOND("协力"),
    FORECAST("预报"),
    /** 亡命之计等「弃置自身换取强力效果」的代价型词条。来源：《卡组》极限快、《基本行动思路》。 */
    SACRIFICE("牺牲");

    val isCombatKeyword: Boolean
        get() = this in setOf(AMBUSH, BLITZ, FURY, GUARD, HEAVY_ARMOR, SHOCK, SMOKE)
}

/** 阵线。来源：《基本机制》《卡牌》。 */
enum class Line(val cn: String) {
    SUPPORT("支援阵线"),
    FRONT("前线")
}

/** 对局双方。 */
enum class Side(val cn: String) {
    P1("玩家一"),
    P2("玩家二");

    val opponent: Side get() = if (this == P1) P2 else P1
}

/**
 * 卡牌定义。
 * cost 为部署费用，actionCost 为行动花费（移动/攻击），armor 为重甲 X。
 */
data class Card(
    val id: String,
    val name: String,
    val nation: Nation,
    val type: CardType,
    val rarity: Rarity = Rarity.STANDARD,
    val cost: Int = 0,
    val actionCost: Int = 1,
    val attack: Int = 0,
    val defense: Int = 0,
    val unitType: UnitType? = null,
    val keywords: Set<Keyword> = emptySet(),
    val armor: Int = 0,
    val countdown: Int = 0,
    val intel: Int = 0,
    /** 卡面写有「也算作是坦克」。 */
    val countsAsTank: Boolean = false,
    /** 卡面写有「本单位能在同一回合移动并攻击」。 */
    val canMoveAndAttack: Boolean = false,
    /** 流亡卡牌的流亡所属国家。 */
    val exileNation: Nation? = null,
    /** 卡池归属，来自 Wiki 卡表「卡池」列；未标注时为 null。 */
    val pool: PoolSystem.Pool? = null,
    /** 数据源提供的完整卡面原文；效果实现必须据此核对。 */
    val effectText: String = "",
    /** 衍生的老兵版本 id，只使用数据源中存在的卡。 */
    val veteranCardId: String? = null
) {
    init {
        if (type == CardType.UNIT) {
            require(unitType != null) { "单位卡必须指定 unitType：$id" }
        }
        require(armor <= 3) { "单位最多 3 重甲：$id" }
    }

    fun has(k: Keyword): Boolean = keywords.contains(k)
}

/**
 * 战场上的单位实例。承载伤害、增益与每回合状态。
 */
class UnitInstance(val card: Card, val owner: Side) {
    var damage: Int = 0
    var attackBonus: Int = 0
    var maxDefenseBonus: Int = 0
    /** 卡面持续条件产生的静态加成，每次重算时清零，不会跨回合叠加。 */
    var passiveAttackBonus: Int = 0
    var passiveDefenseBonus: Int = 0
    var line: Line = Line.SUPPORT
    var suppressed: Boolean = false
    /** 压制的解除回合号。 */
    var pinnedUntilTurn: Int = -1
    var movedThisTurn: Boolean = false
    var attacksThisTurn: Int = 0
    var enteredTurn: Int = 0
    val keywords: MutableSet<Keyword> = card.keywords.toMutableSet()

    var ambushAvailable: Boolean = card.has(Keyword.AMBUSH)
    var shockAvailable: Boolean = card.has(Keyword.SHOCK)
    var smokeActive: Boolean = card.has(Keyword.SMOKE)
    var mobilized: Boolean = card.has(Keyword.MOBILIZE)
    var covertRevealed: Boolean = !card.has(Keyword.COVERT)
    var veteran: Boolean = false

    /** 倒计时剩余；到 0 触发事件。 */
    var countdownLeft: Int = 0

    /** 被缴获后归属的收缴方国家。 */
    var salvagedBy: Nation? = null

    val unitType: UnitType get() = card.unitType!!

    /** 也算作坦克时，坦克专属规则同样适用。 */
    val tankLike: Boolean get() = unitType == UnitType.TANK || card.countsAsTank

    val attack: Int get() = if (suppressed) card.attack else card.attack + attackBonus + passiveAttackBonus

    val maxDefense: Int get() = if (suppressed) card.defense else card.defense + maxDefenseBonus + passiveDefenseBonus

    val defense: Int get() = maxDefense - damage

    val actionCost: Int get() = if (suppressed) card.actionCost else card.actionCost

    val armorValue: Int get() = if (suppressed) 0 else card.armor.coerceAtMost(3)

    val isAlive: Boolean get() = defense > 0

    fun has(k: Keyword): Boolean = !suppressed && keywords.contains(k)

    fun takeDamage(raw: Int, ignoresArmor: Boolean = false) {
        val applied = if (ignoresArmor) raw else (raw - armorValue).coerceAtLeast(0)
        damage += applied
    }

    /** 修复：防御力增加至最大防御力。 */
    fun fullRepair() {
        damage = 0
    }

    /** 部署/加入战场时刷新每回合状态。 */
    fun onEnterBoard(turn: Int) {
        enteredTurn = turn
        movedThisTurn = false
        attacksThisTurn = 0
        ambushAvailable = card.has(Keyword.AMBUSH)
        shockAvailable = card.has(Keyword.SHOCK)
        smokeActive = card.has(Keyword.SMOKE)
    }

    fun resetForTurn() {
        movedThisTurn = false
        attacksThisTurn = 0
    }

    /**
     * 抑制品：移除所有关键词与效果，重置面板。
     * 来源：《词条》抑制条。
     */
    fun suppress() {
        suppressed = true
        keywords.clear()
        attackBonus = 0
        maxDefenseBonus = 0
        passiveAttackBonus = 0
        passiveDefenseBonus = 0
        damage = 0
        smokeActive = false
        ambushAvailable = false
        shockAvailable = false
        mobilized = false
    }

    override fun toString(): String = "${card.name}(${attack}/${defense})@${line.cn}"
}

/** 总部状态。 */
class HqState(val side: Side, val card: Card) {
    var damage: Int = 0
    var maxDefenseBonus: Int = 0
    var immune: Boolean = false
    val maxDefense: Int get() = card.defense + maxDefenseBonus
    val defense: Int get() = maxDefense - damage

    fun takeDamage(raw: Int, ignoresImmunity: Boolean = false): Int {
        if (immune && !ignoresImmunity) return 0
        damage += raw
        return raw
    }
}
