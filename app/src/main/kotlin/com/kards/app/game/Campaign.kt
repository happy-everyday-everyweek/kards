package com.kards.app.game

import android.content.Context
import com.kards.core.Nation

enum class MissionGoal(val cn: String) {
    DESTROY_HQ("摧毁敌方总部"),
    CONTROL_FRONT("累计控制前线 3 个回合"),
    DESTROY_UNITS("消灭 8 个敌方单位"),
    SURVIVE("坚守 8 个回合")
}

enum class StrategyBonus {
    ELITE_ATTACK, HQ_DEFENSE, START_SLOT, SUPPLY_DRAW,
    INFANTRY_COST, AIR_COST, TANK_COST, UNIT_DEFENSE
}

data class CampaignStrategy(
    val id: String,
    val name: String,
    val description: String,
    val bonus: StrategyBonus,
    val amount: Int
)

data class Mission(
    val index: Int,
    val title: String,
    val brief: String,
    val goal: MissionGoal,
    val enemyHqBonus: Int = 0,
    val reward: String
)

data class Campaign(
    val id: String,
    val title: String,
    val subtitle: String,
    val nation: Nation,
    val enemy: Nation,
    val strategies: List<CampaignStrategy>,
    val missions: List<Mission>
)

private fun stages(prefix: String, rewards: List<String>): List<Mission> = listOf(
    Mission(1, "$prefix · 第一阶段", "突破外围防线，摧毁敌军总部。", MissionGoal.DESTROY_HQ, 0, rewards[0]),
    Mission(2, "$prefix · 第二阶段", "夺取前线并累计控制 3 个回合。", MissionGoal.CONTROL_FRONT, 2, rewards[1]),
    Mission(3, "$prefix · 第三阶段", "消灭 8 个敌方单位，瓦解其战斗群。", MissionGoal.DESTROY_UNITS, 3, rewards[2]),
    Mission(4, "$prefix · 第四阶段", "在增援抵达前坚守 8 个回合。", MissionGoal.SURVIVE, 4, rewards[3]),
    Mission(5, "$prefix · 最终阶段", "面对强化总部，完成最后一击。", MissionGoal.DESTROY_HQ, 8, rewards[4])
)

object CampaignCatalog {
    val all = listOf(
        Campaign(
            "moscow", "莫斯科保卫战", "苏联 · 1941", Nation.USSR, Nation.GERMANY,
            listOf(
                CampaignStrategy("far_east", "远东精锐", "抽调远东精锐部队，友方单位攻击力 +1。", StrategyBonus.ELITE_ATTACK, 1),
                CampaignStrategy("militia", "民兵防线", "动员民众构筑工事，总部 +5 防御。", StrategyBonus.HQ_DEFENSE, 5),
                CampaignStrategy("reserves", "预备队", "投入战略预备队，开局额外 1 个指挥点槽。", StrategyBonus.START_SLOT, 1)
            ),
            stages("莫斯科", listOf("1 个军需箱", "1 张限定卡", "1 张特殊卡", "1 个军官卡包", "战役精英卡"))
        ),
        Campaign(
            "alamein", "第二次阿拉曼战役", "德国 · 1942", Nation.GERMANY, Nation.UK,
            listOf(
                CampaignStrategy("rommel", "隆美尔战术", "依靠机动战术，友方单位攻击力 +1。", StrategyBonus.ELITE_ATTACK, 1),
                CampaignStrategy("supply", "补给线", "保障远征补给，开局额外抽 1 张牌。", StrategyBonus.SUPPLY_DRAW, 1),
                CampaignStrategy("italian", "意大利支援", "意大利部队支援，步兵费用 -1。", StrategyBonus.INFANTRY_COST, 1)
            ),
            stages("阿拉曼", listOf("1 个军需箱", "1 张限定卡", "1 张特殊卡", "1 个军官卡包", "战役精英卡"))
        ),
        Campaign(
            "philippines", "菲律宾战役", "日本 · 1942", Nation.JAPAN, Nation.USA,
            listOf(
                CampaignStrategy("jungle", "丛林战", "依靠丛林作战经验，步兵费用 -1。", StrategyBonus.INFANTRY_COST, 1),
                CampaignStrategy("air", "航空优势", "依靠空中优势，空军费用 -1。", StrategyBonus.AIR_COST, 1),
                CampaignStrategy("brute", "强攻", "投入久战老兵，友方单位防御 +1。", StrategyBonus.UNIT_DEFENSE, 1)
            ),
            stages("菲律宾", listOf("1 个军需箱", "1 张限定卡", "1 张特殊卡", "1 个军官卡包", "战役精英卡"))
        ),
        Campaign(
            "guadalcanal", "瓜达尔卡纳尔岛战役", "美国 · 1942", Nation.USA, Nation.JAPAN,
            listOf(
                CampaignStrategy("marines", "陆战队核心", "强化陆战队，步兵费用 -1。", StrategyBonus.INFANTRY_COST, 1),
                CampaignStrategy("henderson", "亨德森机场", "争夺机场制空权，空军费用 -1。", StrategyBonus.AIR_COST, 1),
                CampaignStrategy("navy", "海军支援", "依靠舰队火力，总部 +5 防御。", StrategyBonus.HQ_DEFENSE, 5)
            ),
            stages("瓜岛", listOf("1 个军需箱", "1 张限定卡", "1 张特殊卡", "1 个军官卡包", "战役精英卡"))
        ),
        Campaign(
            "tunisia", "突尼斯战役", "英国 · 1943", Nation.UK, Nation.GERMANY,
            listOf(
                CampaignStrategy("coast", "沿海推进", "沿海推进并获取海军支援，开局 +2 指挥点。", StrategyBonus.START_SLOT, 2),
                CampaignStrategy("vichy", "维希法军", "争取法军支援形成兵力优势，开局额外抽 2 张牌。", StrategyBonus.SUPPLY_DRAW, 2),
                CampaignStrategy("blade", "刀锋部队", "装甲迂回，坦克费用 -1。", StrategyBonus.TANK_COST, 1)
            ),
            stages("突尼斯", listOf("1 个军需箱", "1 张限定卡", "1 张特殊卡", "1 个军官卡包", "战役精英卡"))
        )
    )

    fun byId(id: String): Campaign? = all.firstOrNull { it.id == id }
}

class CampaignProgress(context: Context) {
    private val prefs = context.getSharedPreferences("campaign_progress_v2", Context.MODE_PRIVATE)

    fun unlocked(campaignId: String): Int = prefs.getInt("${campaignId}_unlocked", 1)
    fun stars(campaignId: String, mission: Int): Int = prefs.getInt("${campaignId}_m${mission}_stars", 0)
    fun strategy(campaignId: String): String? = prefs.getString("${campaignId}_strategy", null)

    fun selectStrategy(campaignId: String, strategyId: String) {
        prefs.edit().putString("${campaignId}_strategy", strategyId).apply()
    }

    fun complete(campaignId: String, mission: Int, stars: Int) {
        val safe = stars.coerceIn(1, 3)
        val old = prefs.getInt("${campaignId}_m${mission}_stars", 0)
        val next = if (safe > old) safe else old
        prefs.edit()
            .putInt("${campaignId}_m${mission}_stars", next)
            .putInt("${campaignId}_unlocked", maxOf(unlocked(campaignId), (mission + 1).coerceAtMost(5)))
            .apply()
    }

    fun isCompleted(campaignId: String): Boolean = stars(campaignId, 5) > 0
    fun completedCount(): Int = CampaignCatalog.all.count { isCompleted(it.id) }
    fun totalStars(): Int = CampaignCatalog.all.sumOf { c -> (1..5).sumOf { stars(c.id, it) } }
}