package com.kards.core

/** 从接口原文解析的可执行效果；每个操作都保留来源文本，拒绝无来源的默认效果。 */
enum class EffectOp {
    DAMAGE, DESTROY, DRAW, GAIN_KREDITS, GAIN_SLOTS, LOSE_SLOTS,
    BUFF_ATTACK, BUFF_DEFENSE, PIN, SUPPRESS, REPAIR, RETREAT,
    ADD_COPY, ADD_CARD, DISCARD, HEAL_HQ, DAMAGE_HQ, TAKE_CONTROL,
    REDUCE_COST, INCREASE_COST, SET_ATTACK_TO_DEFENSE, SHUFFLE_DECK,
    PREVENT_DEPLOY, COUNTER_DEPLOY
}

enum class EffectTarget { SELF, TARGET_UNIT, ENEMY_UNITS, FRIENDLY_UNITS, ALL_UNITS, ENEMY_HQ, FRIENDLY_HQ, TARGET_CARD }
data class ParsedEffect(val op: EffectOp, val amount: Int = 0, val target: EffectTarget = EffectTarget.TARGET_UNIT, val source: String)

object CardEffectParser {
    private val cnNumbers = mapOf("一" to 1, "两" to 2, "二" to 2, "三" to 3, "四" to 4, "五" to 5, "六" to 6, "七" to 7, "八" to 8, "九" to 9, "十" to 10)

    fun parse(card: Card): List<ParsedEffect> {
        if (card.effectText.isBlank()) return emptyList()
        return card.effectText.replace("\\r\\n", "。").split('。', '；')
            .map { it.trim() }.filter { it.isNotBlank() }.flatMap { sentence(it) }
    }

    private fun number(text: String, before: String): Int {
        val match = Regex("([0-9]+|[一二两三四五六七八九十]+)\\s*$before").find(text) ?: return 0
        return match.groupValues[1].toIntOrNull() ?: cnNumbers[match.groupValues[1]] ?: 0
    }

    private fun target(text: String): EffectTarget = when {
        "所有敌方单位" in text || "全部敌方单位" in text -> EffectTarget.ENEMY_UNITS
        "所有友方单位" in text || "全部友方单位" in text -> EffectTarget.FRIENDLY_UNITS
        "所有单位" in text || "全部单位" in text -> EffectTarget.ALL_UNITS
        "敌方总部" in text || "其总部" in text -> EffectTarget.ENEMY_HQ
        "友方总部" in text -> EffectTarget.FRIENDLY_HQ
        "本单位" in text -> EffectTarget.SELF
        else -> EffectTarget.TARGET_UNIT
    }

    private fun sentence(text: String): List<ParsedEffect> {
        val target = target(text)
        val out = mutableListOf<ParsedEffect>()
        fun add(op: EffectOp, amount: Int = 0, to: EffectTarget = target) { out += ParsedEffect(op, amount, to, text) }
        if (Regex("造成\\s*([0-9]+|[一二两三四五六七八九十]+)\\s*点伤害").containsMatchIn(text)) {
            add(if (target == EffectTarget.ENEMY_HQ || target == EffectTarget.FRIENDLY_HQ) EffectOp.DAMAGE_HQ else EffectOp.DAMAGE, number(text, "点伤害"))
        }
        if ("消灭" in text) add(EffectOp.DESTROY)
        Regex("抽\\s*([0-9]+|[一二两三四五六七八九十]+)\\s*张").find(text)?.let { add(EffectOp.DRAW, number(text, "张"), EffectTarget.SELF) }
        Regex("获得\\s*([0-9]+|[一二两三四五六七八九十]+)\\s*个指挥点").find(text)?.let { add(EffectOp.GAIN_KREDITS, number(text, "个指挥点"), EffectTarget.SELF) }
        Regex("获得\\s*([0-9]+|[一二两三四五六七八九十]+)\\s*个指挥点槽").find(text)?.let { add(EffectOp.GAIN_SLOTS, number(text, "个指挥点槽"), EffectTarget.SELF) }
        if ("失去" in text && "指挥点槽" in text) add(EffectOp.LOSE_SLOTS, number(text, "个指挥点槽").coerceAtLeast(1), EffectTarget.SELF)
        Regex("\\+\\s*([0-9]+)\\s*攻击力").find(text)?.let { add(EffectOp.BUFF_ATTACK, it.groupValues[1].toInt()) }
        Regex("-\\s*([0-9]+)\\s*攻击力").find(text)?.let { add(EffectOp.BUFF_ATTACK, -it.groupValues[1].toInt()) }
        Regex("\\+\\s*([0-9]+)\\s*防御力").find(text)?.let { add(EffectOp.BUFF_DEFENSE, it.groupValues[1].toInt()) }
        Regex("\\+\\s*([0-9]+)\\s*防御").find(text)?.let { add(EffectOp.BUFF_DEFENSE, it.groupValues[1].toInt()) }
        Regex("\\+\\s*([0-9]+)\\s*\\+\\s*([0-9]+)").find(text)?.let {
            add(EffectOp.BUFF_ATTACK, it.groupValues[1].toInt()); add(EffectOp.BUFF_DEFENSE, it.groupValues[2].toInt())
        }
        if ("压制" in text && "无法被压制" !in text) add(EffectOp.PIN)
        if ("抑制" in text && "无法被抑制" !in text) add(EffectOp.SUPPRESS)
        if ("完全修复" in text) add(EffectOp.REPAIR)
        if ("撤退" in text) add(EffectOp.RETREAT)
        if ("获得控制权" in text) add(EffectOp.TAKE_CONTROL)
        if ("弃" in text && ("随机" in text || "所有" in text)) add(EffectOp.DISCARD, number(text, "张").coerceAtLeast(1))
        if ("洗切卡组" in text) add(EffectOp.SHUFFLE_DECK, 0, EffectTarget.SELF)
        if ("攻击力等同于其防御力" in text) add(EffectOp.SET_ATTACK_TO_DEFENSE)
        Regex("-\\s*([0-9]+)\\s*花费").find(text)?.let { add(EffectOp.REDUCE_COST, it.groupValues[1].toInt(), EffectTarget.TARGET_CARD) }
        Regex("\\+\\s*([0-9]+)\\s*花费").find(text)?.let { add(EffectOp.INCREASE_COST, it.groupValues[1].toInt(), EffectTarget.TARGET_CARD) }
        if ("反制其部署" in text) add(EffectOp.COUNTER_DEPLOY)
        return out
    }
}
