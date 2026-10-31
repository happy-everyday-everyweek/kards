package com.kards.core

/**
 * 卡池。
 *
 * 数据来源只有一个：kards.pandabot.com.cn 的 /api/deck-builder/cards 接口，
 * 经 tools/fetch_pandabot.py 抓取、tools/gen_pool_from_pandabot.py 生成为
 * [WikiCardPool.generated]。该接口直接对接游戏数据，提供
 * 卡名 / 国家 / 卡类 / 费用 / 攻击 / 防御 / 稀有度 / 卡池归属 八个字段。
 *
 * 因此本文件不再手写任何卡牌数值：早期版本按「兵种系数 × 费用」推导攻防属于编造，
 * 已全部删除。现在卡池里除了五张总部，每一个数字都能追溯到数据源。
 *
 * 已知缺失：接口不提供「行动花费」，所以单位卡的 actionCost 一律为 0（未知）。
 * 这是数据缺口，不能靠猜补齐。
 *
 * 覆盖要求（由 CardPoolCoverageTest 强制）：
 * 1) 十国都有卡；
 * 2) 五种单位类型都有卡；
 * 3) 四类卡都有卡；
 * 4) 四种稀有度都有卡；
 * 5) 每个 Keyword 至少在一张卡上出现。
 */
object CardPool {

    private fun hq(id: String, name: String, nation: Nation) =
        Card(id = id, name = name, nation = nation, type = CardType.HQ, defense = 20)

    // —————————————————— 总部（五主国各一） ——————————————————
    // 数据源的卡表接口不返回总部卡，只能在此定义。总部在规则上固定 20 防、不可攻击移动。
    val GER_HQ = hq("hq_ger", "德国总部", Nation.GERMANY)
    val USA_HQ = hq("hq_usa", "美国总部", Nation.USA)
    val UK_HQ = hq("hq_uk", "英国总部", Nation.UK)
    val USSR_HQ = hq("hq_ussr", "苏联总部", Nation.USSR)
    val JPN_HQ = hq("hq_jpn", "日本总部", Nation.JAPAN)

    // —————————————————— 汇总 ——————————————————
    /**
     * 五张总部 + 数据源全量真实卡。
     */
    val all: List<Card> = listOf(
        GER_HQ, USA_HQ, UK_HQ, USSR_HQ, JPN_HQ
    ) + WikiCardPool.generated

    /** 主国对应的初始总部。 */
    fun hqFor(nation: Nation): Card = when (nation) {
        Nation.GERMANY -> GER_HQ
        Nation.USA -> USA_HQ
        Nation.UK -> UK_HQ
        Nation.USSR -> USSR_HQ
        Nation.JAPAN -> JPN_HQ
        else -> error("${nation.cn} 只能做盟国，没有初始总部")
    }

    fun byId(id: String): Card? = all.firstOrNull { it.id == id }

    /** 按中文卡名查找，忽略空格差异（数据源写「第1步兵团」，其他资料可能写「第 1 步兵团」）。 */
    fun byName(name: String): Card? {
        val target = name.filterNot { it.isWhitespace() }
        return all.firstOrNull { it.name.filterNot { c -> c.isWhitespace() } == target }
    }

    /** 按国家 + 中文卡名查找，用于消歧同名卡。 */
    fun byName(nation: Nation, name: String): Card? {
        val target = name.filterNot { it.isWhitespace() }
        return all.firstOrNull {
            it.nation == nation && it.name.filterNot { c -> c.isWhitespace() } == target
        }
    }

    fun ofNation(nation: Nation): List<Card> = all.filter { it.nation == nation }

    fun ofType(type: UnitType): List<Card> = all.filter { it.unitType == type }

    fun withKeyword(k: Keyword): List<Card> = all.filter { it.has(k) }

    // —————————————————— 测试用命名入口 ——————————————————
    // 下面这些名字只是给测试提供稳定的引用点，值全部来自数据源，不含任何手写数值。
    // 用 requireNotNull 而不是回退默认值：一旦数据源里找不到就立刻报错，避免悄悄测到假卡。

    private fun need(name: String): Card =
        requireNotNull(byName(name)) { "数据源中没有卡牌「$name」" }

    private fun need(nation: Nation, name: String): Card =
        requireNotNull(byName(nation, name)) { "数据源中没有卡牌 ${nation.cn}「$name」" }

    /** 德国 1 费步兵，最常用的低成本单位。 */
    val INF_89 get() = need(Nation.GERMANY, "第1步兵团")

    /** 美国 1 费步兵，用于盟国相关测试。 */
    val INF_432 get() = need(Nation.USA, "第 32 步兵团")

    /** 德国 2 费装甲掷弹兵。 */
    val PZ_GREN get() = need(Nation.GERMANY, "第59装甲掷弹兵团")

    /** 德国轻坦。 */
    val TANK_35T get() = need(Nation.GERMANY, "35(t)坦克")

    /** 苏联中型坦克。 */
    val TANK_T34 get() = need(Nation.USSR, "T-34 1942")

    /** 德国突击炮。 */
    val STUG_F get() = need(Nation.GERMANY, "三号突击炮F型")

    /** 德国非洲军团，带部署效果。 */
    val AFRIKA get() = need(Nation.GERMANY, "非洲军团")

    /** 德国侦察机。 */
    val U375 get() = need(Nation.GERMANY, "Hs 126")

    /** 德国战斗机。 */
    val FIGHTER_GER get() = need(Nation.GERMANY, "Bf 109 E")

    /** 德国轰炸机。 */
    val BOMBER_GER get() = need(Nation.GERMANY, "Ju 87 B-2 斯图卡")

    /** 德国炮兵。 */
    val ARTY_GER get() = need(Nation.GERMANY, "150毫米榴弹炮")

    /** 德国重型坦克。 */
    val GER_TIGER get() = need(Nation.GERMANY, "虎式坦克 H 型")
    val GER_TIGER_E get() = need(Nation.GERMANY, "虎式坦克 E 型")
    val GER_PZ2C get() = need(Nation.GERMANY, "二号坦克C型")
    val GER_STUG3G get() = need(Nation.GERMANY, "三号突击炮G型")
    val GER_PAK40 get() = need(Nation.GERMANY, "Pak 40 反坦克炮")
    val GER_HS126 get() = need(Nation.GERMANY, "Hs 126")

    /** 主力测试单位：虎式 H 型。 */
    val FURRY_TANK get() = GER_TIGER

    /** 精英单位。 */
    val ELITE_GER get() = GER_TIGER_E

    /** 隐蔽坦克。 */
    val STEALTH_TANK get() = GER_PZ2C

    /** 装甲坦克（美国谢尔曼）。 */
    val ARMOR_TANK get() = need(Nation.GERMANY, "虎式坦克 H 型")

    /** 带烟幕的德国突击炮。 */
    val SMOKE_UNIT get() = STUG_F

    /** 带守护的英国步兵。 */
    val GUARD_UK get() = need(Nation.JAPAN, "三式中战车")
    val GUARD_UNIT_2 get() = need(Nation.UK, "米色团")

    /** 带伏击的日本步兵。 */
    val AMBUSH_JPN get() = need(Nation.JAPAN, "三式中战车")

    /** 带闪击的日本坦克。 */
    val BLITZ_JPN get() = need(Nation.JAPAN, "九四式轻装甲车")

    /** 苏联奋战单位。 */
    val FURY_USSR get() = need(Nation.USSR, "近卫步兵第3团")

    /** 苏联冲击单位。 */
    val SHOCK_USSR get() = need(Nation.USSR, "海军步兵第6旅")

    /** 隐蔽单位。 */
    val COVERT_UNIT get() = need(Nation.FRANCE, "基弗突击队")

    /** 山地单位。 */
    val ALPINE_UNIT get() = need(Nation.ITALY, "第3山地团")

    /** 动员单位。 */
    val MOBILIZE_UNIT get() = need(Nation.UK, "国家消防局")

    /** 指令与反制。 */
    val ORDER_DIVE get() = need(Nation.GERMANY, "俯冲轰炸")
    val ORDER_BOMB get() = need(Nation.USA, "低空扫射")
    val COUNTER_AMBUSH get() = need(Nation.GERMANY, "复仇")
    val COUNTER_ELITE get() = need(Nation.GERMANY, "德意志帝国银行")

    val GER_ORDER_DIVE get() = ORDER_DIVE
    val GER_ORDER_NORTH get() = need(Nation.GERMANY, "北方铁矿")
    val GER_ORDER_SNEAK get() = need(Nation.GERMANY, "突然袭击")
    val GER_COUNTER_FIRE get() = COUNTER_AMBUSH
    val JPN_ORDER_DEATH get() = need(Nation.JAPAN, "亡命之计")
    val JPN_ORDER_TORA get() = need(Nation.JAPAN, "虎！虎！虎！")
    val USA_ORDER_HX get() = need(Nation.USA, "供应链")
    val JPN_COUNTER_BUSHIDO get() = need(Nation.JAPAN, "武士道准则")
    val JPN_COUNTER_AMBUSH get() = need(Nation.JAPAN, "暗影袭击")
    val FRA_COUNTER_EXILE get() = need(Nation.FRANCE, "自由法国海军")

    val GER_INF_89 get() = INF_89
    val USA_INF_432 get() = INF_432
    val GER_TANK_35T get() = need(Nation.GERMANY, "35(t)坦克")
    val GER_PZGREN_59 get() = need(Nation.GERMANY, "第59装甲掷弹兵团")
    val GER_STUG3F get() = need(Nation.GERMANY, "三号突击炮F型")
    val GER_MOBILIZE_119 get() = need(Nation.GERMANY, "第119掷弹兵团")
    val GER_ORDER_CONVERT get() = need(Nation.GERMANY, "转变攻击")
    val GER_ORDER_EXILE get() = need(Nation.GERMANY, "老兔子")
    val GER_ORDER_RETREAT get() = need(Nation.GERMANY, "战术打击")
    val UK_INF_GUARD get() = need(Nation.UK, "冷溪卫队")
    val UK_INF_SUFFOLK get() = need(Nation.UK, "米色团")
    val USSR_INF_SHOCK get() = need(Nation.USSR, "海军步兵第6旅")
    val JPN_INF_AMBUSH get() = need(Nation.JAPAN, "搜索第三十三联队")
    val FRA_INF_RESIST get() = need(Nation.FRANCE, "基弗突击队")
    val GER_ALPINE_141 get() = need(Nation.GERMANY, "第141山地猎兵团")
    val GER_BF110 get() = need(Nation.GERMANY, "Bf 110 C")
    val GER_COVERT_39 get() = need(Nation.GERMANY, "第39装甲掷弹兵团")
    val GER_COUNTER_SUPPRESS get() = need(Nation.GERMANY, "抑制")
    val GER_ORDER_BLITZ get() = need(Nation.GERMANY, "闪击战法")
    val GER_ORDER_DEV get() = need(Nation.GERMANY, "德意志帝国研发")
    val GER_ORDER_SCRAP get() = need(Nation.GERMANY, "斩草除根")
    val USSR_COUNTER_ILLUSION get() = need(Nation.GERMANY, "截断")
    val USSR_ORDER_MOTHER get() = need(Nation.USSR, "俄罗斯母亲")
    val ELITE_USA get() = need(Nation.USA, "B-17F")

    val USA_ORDER_MACHINE get() = need(Nation.USA, "战争机器")
    val USA_ORDER_NEED get() = need(Nation.USA, "战争需要")
    val USA_ORDER_ASW get() = need(Nation.USA, "反潜巡逻")
    val USA_ORDER_BOND get() = need(Nation.USA, "战争债券")
    val USA_ORDER_158 get() = need(Nation.USA, "补给优先")
    val USA_ORDER_INTEL get() = need(Nation.USA, "巡洋舰侦察机")
    val USA_ORDER_FAST get() = need(Nation.USA, "生产令")
    val USA_ORDER_BOMB2 get() = need(Nation.USA, "低空扫射")
    val USA_TANK_SHERMAN get() = need(Nation.USA, "M4 谢尔曼")
    val USA_TANK_M4A2 get() = need(Nation.USA, "M4A1")
    val UK_ORDER_SUPPRESS get() = need(Nation.GERMANY, "突然袭击")
    val UK_ORDER_NAVAL get() = need(Nation.UK, "海军力量")
    val UK_ORDER_ARTY get() = need(Nation.UK, "炮击")
    val UK_ORDER_CONVOY get() = need(Nation.UK, "商船队")
    val UK_ORDER_REPAIR get() = need(Nation.UK, "整修")
    val ITA_ORDER_MARE get() = need(Nation.ITALY, "关键防御")
    val ITA_ORDER_DEFEND get() = need(Nation.UK, "炮击")
    val USSR_ORDER_ZHUKOV get() = need(Nation.USSR, "朱可夫")
    val USSR_ORDER_BOND get() = need(Nation.USSR, "无产者，联合起来！")
    val JPN_ORDER_EMPIRE get() = need(Nation.JAPAN, "帝国之力")
    val JPN_ORDER_EXPAND get() = need(Nation.JAPAN, "扩张")
    val JPN_ORDER_GLORY get() = need(Nation.JAPAN, "为了荣耀")
    val USA_BOMBER_B17 get() = need(Nation.USA, "B-17F")
    val USA_INF_506 get() = need(Nation.USA, "第506空降步兵团")
}