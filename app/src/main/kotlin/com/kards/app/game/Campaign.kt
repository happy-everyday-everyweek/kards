package com.kards.app.game

import android.content.Context
import com.kards.core.Nation

/** 五场离线 PVE 战役。AI 强度只改变决策，不修改卡牌属性或规则。 */
data class Campaign(
    val id: String,
    val title: String,
    val subtitle: String,
    val nation: Nation,
    val enemy: Nation,
    val difficulty: Int,
    val objective: String = "摧毁敌方总部"
)

object CampaignCatalog {
    val all = listOf(
        Campaign("moscow", "莫斯科保卫战", "苏联 · 1941", Nation.USSR, Nation.GERMANY, 1),
        Campaign("alamein", "第二次阿拉曼战役", "德国 · 1942", Nation.GERMANY, Nation.UK, 2),
        Campaign("philippines", "菲律宾战役", "日本 · 1942", Nation.JAPAN, Nation.USA, 2),
        Campaign("guadalcanal", "瓜达尔卡纳尔岛战役", "美国 · 1942", Nation.USA, Nation.JAPAN, 3),
        Campaign("tunisia", "突尼斯战役", "英国 · 1943", Nation.UK, Nation.GERMANY, 3)
    )
}

class CampaignProgress(context: Context) {
    private val prefs = context.getSharedPreferences("campaign_progress", Context.MODE_PRIVATE)
    fun isCompleted(id: String): Boolean = prefs.getBoolean(id, false)
    fun complete(id: String) = prefs.edit().putBoolean(id, true).apply()
    fun completedCount(): Int = CampaignCatalog.all.count { isCompleted(it.id) }
}
