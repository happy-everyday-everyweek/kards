package com.kards.core

/**
 * 指令与反制的执行效果表。
 *
 * 机制描述来自官方 Wiki《卡牌》；具体到每张卡对应哪种效果，是本地按卡面语义落成的
 * 最小可执行实现。这里只登记「效果类型」，数值一律从 [CardPool] 里的真实卡牌读取。
 *
 * 用「国家 + 卡名」定位而不是常量名：卡池已经整体换成数据源生成，常量名不再稳定，
 * 而卡名是国家内的唯一标识。
 */
enum class OrderEffect(val cn: String) {
    DAMAGE_ONE("对单个单位造成伤害"),
    DAMAGE_ALL("对敌方全体造成伤害"),
    DRAW("抽牌"),
    GAIN_KREDIT("获得指挥点"),
    GAIN_SLOT("增加指挥点槽"),
    LOSE_SLOT_SELF("减少己方指挥点槽"),
    LOSE_SLOT_ENEMY("减少敌方指挥点槽"),
    SUPPRESS_ONE("抑制一个单位"),
    PIN_ALL("压制敌方全体"),
    REPAIR_ALL("完全修复友方全体"),
    BUFF_ALL("友方全体 +1/+1"),
    DEVELOP("开发：三选一"),
    INTEL("情报"),
    RETREAT_ONE("使一个单位撤退"),
    TAKE_CONTROL("获得一个单位控制权"),
    CONVERT("转换一张手牌"),
    FORECAST("预报天气"),
    SALVAGE("收缴"),
    EXILE("流亡卡牌"),
    PINCER("钳击"),
    HEAL_HQ("提升总部防御力"),
    SACRIFICE_SELF("弃置自身换取强力效果"),
    NO_OP("无附加效果")
}

/** 反制触发条件。来源：《卡牌》。 */
enum class CounterTrigger(val cn: String) {
    ENEMY_DEPLOYS_UNIT("敌方部署单位时"),
    ENEMY_ATTACKS("敌方发起攻击时"),
    ENEMY_PLAYS_ORDER("敌方使用指令时"),
    ENEMY_DAMAGES_HQ("敌方对总部造成伤害时"),
    ENEMY_MOVES_TO_FRONT("敌方进入前线时"),
    ENEMY_UNIT_DESTROYED("敌方单位被消灭时"),
    NONE("无")
}

/** 把卡牌与它的效果、触发条件关联起来。 */
object EffectTable {

    /**
     * 指令效果登记表：(国家, 卡名) -> 效果。
     * 卡牌在当前数据源中不存在时直接报错，避免登记一个永远查不到的条目。
     */
    private val orderSpecs: List<Triple<Nation, String, OrderEffect>> = listOf(
        Triple(Nation.GERMANY, "俯冲轰炸", OrderEffect.DAMAGE_ONE),
        Triple(Nation.GERMANY, "突然袭击", OrderEffect.DAMAGE_ONE),
        Triple(Nation.GERMANY, "闪击战法", OrderEffect.BUFF_ALL),
        Triple(Nation.GERMANY, "北方铁矿", OrderEffect.GAIN_KREDIT),
        Triple(Nation.GERMANY, "斩草除根", OrderEffect.DAMAGE_ALL),
        Triple(Nation.GERMANY, "转变攻击", OrderEffect.CONVERT),
        Triple(Nation.GERMANY, "德意志帝国研发", OrderEffect.DEVELOP),
        Triple(Nation.GERMANY, "交叉火力", OrderEffect.PINCER),
        Triple(Nation.GERMANY, "战利品", OrderEffect.FORECAST),
        Triple(Nation.GERMANY, "老兔子", OrderEffect.EXILE),
        Triple(Nation.GERMANY, "战术打击", OrderEffect.RETREAT_ONE),
        Triple(Nation.USA, "战争机器", OrderEffect.GAIN_SLOT),
        Triple(Nation.USA, "战争需要", OrderEffect.GAIN_SLOT),
        Triple(Nation.USA, "反潜巡逻", OrderEffect.GAIN_SLOT),
        Triple(Nation.USA, "战争债券", OrderEffect.GAIN_KREDIT),
        Triple(Nation.USA, "供应链", OrderEffect.DRAW),
        Triple(Nation.USA, "低空扫射", OrderEffect.LOSE_SLOT_ENEMY),
        Triple(Nation.USA, "补给优先", OrderEffect.GAIN_KREDIT),
        Triple(Nation.USA, "保密计划", OrderEffect.INTEL),
        Triple(Nation.USA, "生产令", OrderEffect.DRAW),
        Triple(Nation.UK, "炮击", OrderEffect.LOSE_SLOT_ENEMY),
        Triple(Nation.UK, "海军力量", OrderEffect.DAMAGE_ONE),
        Triple(Nation.UK, "商船队", OrderEffect.DRAW),
        Triple(Nation.UK, "雷达警报", OrderEffect.FORECAST),
        Triple(Nation.UK, "军情五处", OrderEffect.SUPPRESS_ONE),
        Triple(Nation.UK, "整修", OrderEffect.REPAIR_ALL),
        Triple(Nation.USSR, "战时生产", OrderEffect.GAIN_KREDIT),
        Triple(Nation.USSR, "乌拉尔工厂", OrderEffect.DRAW),
        Triple(Nation.USSR, "朱可夫", OrderEffect.BUFF_ALL),
        Triple(Nation.USSR, "俄罗斯母亲", OrderEffect.HEAL_HQ),
        Triple(Nation.USSR, "无产者，联合起来！", OrderEffect.GAIN_KREDIT),
        Triple(Nation.USSR, "背水一战", OrderEffect.CONVERT),
        Triple(Nation.USSR, "暴风雪", OrderEffect.SUPPRESS_ONE),
        Triple(Nation.JAPAN, "虎！虎！虎！", OrderEffect.DAMAGE_ALL),
        Triple(Nation.JAPAN, "扩张", OrderEffect.DRAW),
        Triple(Nation.JAPAN, "为了荣耀", OrderEffect.BUFF_ALL),
        Triple(Nation.JAPAN, "帝国之力", OrderEffect.DAMAGE_ONE),
        Triple(Nation.JAPAN, "亡命之计", OrderEffect.SACRIFICE_SELF),
        Triple(Nation.JAPAN, "以智取胜", OrderEffect.PINCER),
        Triple(Nation.JAPAN, "新式战法", OrderEffect.DEVELOP),
        Triple(Nation.FRANCE, "抵抗万岁！", OrderEffect.DAMAGE_ONE),
        Triple(Nation.ITALY, "关键防御", OrderEffect.PIN_ALL),
        Triple(Nation.POLAND, "冲锋在前", OrderEffect.BUFF_ALL),
        Triple(Nation.POLAND, "总参二局", OrderEffect.EXILE)
    )

    /** 反制触发条件登记表。 */
    private val counterSpecs: List<Triple<Nation, String, CounterTrigger>> = listOf(
        Triple(Nation.GERMANY, "复仇", CounterTrigger.ENEMY_DEPLOYS_UNIT),
        Triple(Nation.GERMANY, "抑制", CounterTrigger.ENEMY_DEPLOYS_UNIT),
        Triple(Nation.GERMANY, "德意志帝国银行", CounterTrigger.ENEMY_PLAYS_ORDER),
        Triple(Nation.USA, "防空", CounterTrigger.ENEMY_ATTACKS),
        Triple(Nation.UK, "拦截", CounterTrigger.ENEMY_MOVES_TO_FRONT),
        Triple(Nation.UK, "障碍", CounterTrigger.ENEMY_DAMAGES_HQ),
        Triple(Nation.USSR, "燃烧的天空", CounterTrigger.ENEMY_PLAYS_ORDER),
        Triple(Nation.JAPAN, "武士道准则", CounterTrigger.ENEMY_ATTACKS),
        Triple(Nation.JAPAN, "暗影袭击", CounterTrigger.ENEMY_DEPLOYS_UNIT),
        Triple(Nation.FRANCE, "自由法国海军", CounterTrigger.NONE)
    )

    /**
     * 查一个「国家 + 卡名」在卡池里的 id。
     *
     * 找不到时返回 null 而不是抛异常：数据源随时可能变动，
     * 效果表缺一条不该让整个 App 起不来。缺哪些卡记在 [missing] 里供测试断言。
     */
    private fun resolveOrNull(nation: Nation, name: String): String? =
        CardPool.byName(nation, name)?.id

    private val orderPairs = orderSpecs.mapNotNull { (n, name, eff) ->
        resolveOrNull(n, name)?.let { it to eff }
    }
    private val counterPairs = counterSpecs.mapNotNull { (n, name, trig) ->
        resolveOrNull(n, name)?.let { it to trig }
    }

    private val orders: Map<String, OrderEffect> = orderPairs.toMap()
    private val counters: Map<String, CounterTrigger> = counterPairs.toMap()

    /** 效果表里登记了、但卡池里找不到的卡。正常情况下应为空。 */
    val missing: List<String> =
        orderSpecs.filter { resolveOrNull(it.first, it.second) == null }
            .map { "${it.first.cn}「${it.second}」" } +
            counterSpecs.filter { resolveOrNull(it.first, it.second) == null }
                .map { "${it.first.cn}「${it.second}」" }

    fun effectOf(card: Card): OrderEffect = orders[card.id] ?: OrderEffect.NO_OP
    fun triggerOf(card: Card): CounterTrigger = counters[card.id] ?: inferTrigger(card.effectText)

    /**
     * 未手工登记的反制按卡面原文判定时机。只使用文本中明确出现的条件词，
     * 无法识别时返回 [CounterTrigger.NONE]，不默认挂到任一事件上。
     */
    private fun inferTrigger(text: String): CounterTrigger = when {
        text.isBlank() -> CounterTrigger.NONE
        "部署" in text -> CounterTrigger.ENEMY_DEPLOYS_UNIT
        "攻击" in text -> CounterTrigger.ENEMY_ATTACKS
        "使用指令" in text -> CounterTrigger.ENEMY_PLAYS_ORDER
        "总部" in text && "伤害" in text -> CounterTrigger.ENEMY_DAMAGES_HQ
        "前线" in text -> CounterTrigger.ENEMY_MOVES_TO_FRONT
        "被消灭" in text || "被消灭时" in text -> CounterTrigger.ENEMY_UNIT_DESTROYED
        else -> CounterTrigger.NONE
    }
    fun orderCount(): Int = orders.size
    fun counterCount(): Int = counters.size
    fun hasEffect(card: Card): Boolean = orders.containsKey(card.id)
    fun hasTrigger(card: Card): Boolean = counters.containsKey(card.id)
}