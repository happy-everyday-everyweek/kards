package com.kards.app.game

import android.content.Context
import com.kards.core.Nation

/**
 * 关卡任务类型。取值只来自实机画面，不做扩充推测：
 * B 站 BV1aKSSYdEgD 的关卡简报与游戏内任务提示逐帧截取。
 */
enum class MissionGoal(val cn: String) {
    KILL_UNITS("消灭指定数量的敌方单位"),
    FRONT_UNITS("回合开始时前线拥有指定数量单位"),
    DESTROY_NAMED_HQ("摧毁指定总部"),
    DESTROY_HQ("摧毁敌方总部")
}

/** 星级条件类型。 */
enum class ChallengeKind(val cn: String) {
    LOSSES_AT_MOST("自身损失单位数不超过上限"),
    CONTROL_FRONT_AT_END("结束战斗时控制前线"),
    HQ_DEFENSE_AT_LEAST("结束战斗时总部防御力不低于下限"),
    KILL_RATIO_AT_LEAST("消灭数与自身损失数之比不低于倍数"),
    NO_FRONT_AT_WIN("获胜回合开始时未控制前线"),
    KILLS_AT_LEAST("消灭指定数量敌方单位")
}

/**
 * 一条星级挑战。
 *
 * [text] 保留实机简报上的原始措辞，方便和录屏逐字核对；
 * [kind] 与 [amount] 是引擎可判定的形式。
 */
data class StarChallenge(
    val text: String,
    val kind: ChallengeKind,
    val amount: Int = 0
)

data class Mission(
    val index: Int,
    val title: String,
    val brief: String,
    val goal: MissionGoal,
    val goalAmount: Int = 0,
    val goalTarget: String = "",
    val challenges: List<StarChallenge> = emptyList(),
    val enemyHqBonus: Int = 0,
    val reward: String,
    val source: String = "",
    val titleVerified: Boolean = false
)

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

data class Campaign(
    val id: String,
    val title: String,
    val subtitle: String,
    val nation: Nation,
    val enemy: Nation,
    val description: String,
    val strategies: List<CampaignStrategy>,
    val missions: List<Mission>
)

private const val SRC_USA = "B站 BV1aKSSYdEgD 实机画面"
private const val SRC_JPN = "B站 BV17Z4y1T73d 评论区"
private const val SRC_GER = "B站 BV15DAWz7EGt 评论区"

/** 没有实机资料的关卡：任务保持为摧毁总部，不虚构星级条件。 */
private fun placeholderMission(
    prefix: String,
    number: Int,
    reward: String,
    enemyHqBonus: Int
): Mission = Mission(
    index = number,
    title = "$prefix · 第 $number 关",
    brief = "摧毁敌方总部。关卡名与专属条件尚未取得实机资料。",
    goal = MissionGoal.DESTROY_HQ,
    enemyHqBonus = enemyHqBonus,
    reward = reward
)

object CampaignCatalog {
    val all = listOf(
        Campaign(
            "moscow", "莫斯科保卫战", "苏联 · 1941", Nation.USSR, Nation.GERMANY,
            description = "官方描述尚未取得，暂不填写。",
            strategies = listOf(
                CampaignStrategy("far_east", "西伯利亚转移", "抽调远东精锐。", StrategyBonus.ELITE_ATTACK, 1),
                CampaignStrategy("militia", "俄罗斯母亲", "动员民众构筑工事。", StrategyBonus.HQ_DEFENSE, 5),
                CampaignStrategy("reserves", "总预备队", "投入战略预备队。", StrategyBonus.START_SLOT, 1)
            ),
            missions = listOf(
                placeholderMission("莫斯科", 1, "1 个军需箱", 0),
                placeholderMission("莫斯科", 2, "1 张限定卡", 2),
                placeholderMission("莫斯科", 3, "1 张特殊卡", 3),
                placeholderMission("莫斯科", 4, "1 个军官卡包", 4),
                placeholderMission("莫斯科", 5, "战役精英卡", 8)
            )
        ),
        Campaign(
            "alamein", "第二次阿拉曼战役", "德国 · 1942", Nation.GERMANY, Nation.UK,
            description = "官方描述尚未取得，暂不填写。",
            strategies = listOf(
                CampaignStrategy("rommel", "隆美尔战术", "依靠机动战术。", StrategyBonus.ELITE_ATTACK, 1),
                CampaignStrategy("supply", "补给线", "保障远征补给。", StrategyBonus.SUPPLY_DRAW, 1),
                CampaignStrategy("italian", "意大利纵队", "意大利部队支援。", StrategyBonus.INFANTRY_COST, 1)
            ),
            missions = listOf(
                placeholderMission("阿拉曼", 1, "1 个军需箱", 0),
                Mission(
                    index = 2,
                    title = "阿拉曼 · 第 2 关",
                    brief = "消灭数需达到自身损失数的 2 倍。关卡名尚未取得实机资料。",
                    goal = MissionGoal.DESTROY_HQ,
                    challenges = listOf(
                        StarChallenge(
                            "消灭数量为自身损失数量的两倍",
                            ChallengeKind.KILL_RATIO_AT_LEAST,
                            2
                        )
                    ),
                    enemyHqBonus = 2,
                    reward = "1 张限定卡",
                    source = SRC_GER
                ),
                placeholderMission("阿拉曼", 3, "1 张特殊卡", 3),
                Mission(
                    index = 4,
                    title = "阿拉曼 · 第 4 关",
                    brief = "需要击杀两个虎式 H，并满足战损要求。关卡名尚未取得实机资料。",
                    goal = MissionGoal.DESTROY_HQ,
                    goalTarget = "虎式 H",
                    enemyHqBonus = 4,
                    reward = "1 个军官卡包",
                    source = SRC_GER
                ),
                Mission(
                    index = 5,
                    title = "阿拉曼 · 第 5 关",
                    brief = "获胜回合开始时不能控制前线。关卡名尚未取得实机资料。",
                    goal = MissionGoal.DESTROY_HQ,
                    challenges = listOf(
                        StarChallenge(
                            "获胜回合开始时不能控制前线",
                            ChallengeKind.NO_FRONT_AT_WIN
                        )
                    ),
                    enemyHqBonus = 8,
                    reward = "战役精英卡",
                    source = SRC_GER
                )
            )
        ),
        Campaign(
            "philippines", "菲律宾战役", "日本 · 1942", Nation.JAPAN, Nation.USA,
            description = "官方描述尚未取得，暂不填写。",
            strategies = listOf(
                CampaignStrategy("jungle", "丛林战", "依靠丛林作战经验。", StrategyBonus.INFANTRY_COST, 1),
                CampaignStrategy("air", "航空优势", "依靠空中优势。", StrategyBonus.AIR_COST, 1),
                CampaignStrategy("brute", "野蛮之师", "投入久战老兵。", StrategyBonus.UNIT_DEFENSE, 1)
            ),
            missions = listOf(
                placeholderMission("菲律宾", 1, "1 个军需箱", 0),
                Mission(
                    index = 2,
                    title = "菲律宾 · 第 2 关",
                    brief = "总部会加血，且有「损失不大于 4 个单位」的任务限制。关卡名尚未取得实机资料。",
                    goal = MissionGoal.DESTROY_HQ,
                    challenges = listOf(
                        StarChallenge(
                            "损失单位数不超过 4 个",
                            ChallengeKind.LOSSES_AT_MOST,
                            4
                        )
                    ),
                    enemyHqBonus = 2,
                    reward = "1 张限定卡",
                    source = SRC_JPN
                ),
                placeholderMission("菲律宾", 3, "1 张特殊卡", 3),
                placeholderMission("菲律宾", 4, "1 个军官卡包", 4),
                Mission(
                    index = 5,
                    title = "开始轰炸！",
                    brief = "发动最后一轮轰炸，摧毁敌军总部。",
                    goal = MissionGoal.DESTROY_HQ,
                    enemyHqBonus = 8,
                    reward = "战役精英卡",
                    titleVerified = true
                )
            )
        ),
        Campaign(
            "guadalcanal", "永远忠诚", "美国 · 1942", Nation.USA, Nation.JAPAN,
            description = "日本正试图扩大其对所罗门群岛的控制。我们的目标是打破他们的控制，但要做到这一点，" +
                "就需要一个陆基机场对抗日军的空中优势。我们占领了瓜达尔卡纳尔岛，并控制了日军正在建设的空军基地。" +
                "我们需要完成亨德森机场的修建工程，同时抵御一定会再次来犯的日军袭击。",
            strategies = listOf(
                CampaignStrategy("base", "基地行动", "围绕陆基航空基地展开行动。", StrategyBonus.AIR_COST, 1),
                CampaignStrategy("navy", "海军服役", "部队完全听令于美国海军，需要提供补给弹药并阻挡日军进攻。", StrategyBonus.HQ_DEFENSE, 5),
                CampaignStrategy("henderson", "亨德森机场", "完成亨德森机场修建工程。", StrategyBonus.SUPPLY_DRAW, 1)
            ),
            missions = listOf(
                Mission(
                    index = 1,
                    title = "美国 · 第 1 关",
                    brief = "任务来自实机简报：在结束你的回合时，消灭了至少 10 个敌方单位。关卡名尚未取得实机资料。",
                    goal = MissionGoal.KILL_UNITS,
                    goalAmount = 10,
                    challenges = listOf(
                        StarChallenge("损失 5 个单位或更少。", ChallengeKind.LOSSES_AT_MOST, 5),
                        StarChallenge("结束战斗时控制住前线。", ChallengeKind.CONTROL_FRONT_AT_END),
                        StarChallenge("结束战斗时总部防御力在 7 点或以上。", ChallengeKind.HQ_DEFENSE_AT_LEAST, 7)
                    ),
                    reward = "1 个军需箱",
                    source = SRC_USA
                ),
                Mission(
                    index = 2,
                    title = "美国 · 第 2 关",
                    brief = "任务来自实机提示：在开始你的回合时前线拥有 3 个单位。关卡名尚未取得实机资料。",
                    goal = MissionGoal.FRONT_UNITS,
                    goalAmount = 3,
                    goalTarget = "亨德森机场",
                    enemyHqBonus = 2,
                    reward = "1 张限定卡",
                    source = SRC_USA
                ),
                Mission(
                    index = 3,
                    title = "美国 · 第 3 关",
                    brief = "任务来自实机提示：摧毁奥斯丁山总部。关卡名尚未取得实机资料。",
                    goal = MissionGoal.DESTROY_NAMED_HQ,
                    goalTarget = "奥斯丁山",
                    enemyHqBonus = 3,
                    reward = "1 张特殊卡",
                    source = SRC_USA
                ),
                placeholderMission("美国", 4, "1 个军官卡包", 4),
                Mission(
                    index = 5,
                    title = "永远忠诚",
                    brief = "场上同时存在主总部、奔马山与奥斯丁山，需要全部摧毁。",
                    goal = MissionGoal.DESTROY_NAMED_HQ,
                    goalTarget = "奔马山",
                    enemyHqBonus = 8,
                    reward = "战役精英卡",
                    source = SRC_USA,
                    titleVerified = true
                )
            )
        ),
        Campaign(
            "tunisia", "突尼斯战役", "英国 · 1943", Nation.UK, Nation.GERMANY,
            description = "官方描述尚未取得，暂不填写。",
            strategies = listOf(
                CampaignStrategy("coast", "皇家海军", "沿海推进并获取海军支援。", StrategyBonus.START_SLOT, 2),
                CampaignStrategy("vichy", "法国联系", "争取法军支援形成兵力优势。", StrategyBonus.SUPPLY_DRAW, 2),
                CampaignStrategy("blade", "刀锋部队", "装甲迂回。", StrategyBonus.TANK_COST, 1)
            ),
            missions = listOf(
                Mission(
                    index = 1,
                    title = "奔向突尼斯",
                    brief = "沿海岸推进，摧毁敌军总部。",
                    goal = MissionGoal.DESTROY_HQ,
                    reward = "1 个军需箱",
                    titleVerified = true
                ),
                placeholderMission("突尼斯", 2, "1 张限定卡", 2),
                placeholderMission("突尼斯", 3, "1 张特殊卡", 3),
                placeholderMission("突尼斯", 4, "1 个军官卡包", 4),
                Mission(
                    index = 5,
                    title = "侧翼拉扯",
                    brief = "完成侧翼机动并摧毁敌军总部。",
                    goal = MissionGoal.DESTROY_HQ,
                    enemyHqBonus = 8,
                    reward = "战役精英卡",
                    titleVerified = true
                )
            )
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