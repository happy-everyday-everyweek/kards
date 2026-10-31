package com.kards.core

/** 从接口原文解析的可执行效果；每个操作都保留来源文本，拒绝无来源的默认效果。 */
enum class EffectOp {
    DAMAGE, DESTROY, DRAW, GAIN_KREDITS, GAIN_SLOTS, LOSE_SLOTS,
    BUFF_ATTACK, BUFF_DEFENSE, PIN, SUPPRESS, REPAIR, RETREAT,
    ADD_COPY, ADD_CARD, DISCARD, HEAL_HQ, DAMAGE_HQ, TAKE_CONTROL,
    REDUCE_COST, INCREASE_COST, SET_ATTACK_TO_DEFENSE, SHUFFLE_DECK,
    SET_ATTACK, RETURN_HAND, REVEAL, COPY_DEAD_THIS_TURN
    PREVENT_DEPLOY, COUNTER_DEPLOY, INTEL, DEVELOP, ENEMY_DISCARD
}

enum class EffectTarget {
    SELF, TARGET_UNIT, FRIENDLY_ONE, ENEMY_ONE,
    ENEMY_UNITS, FRIENDLY_UNITS, ALL_UNITS,
    ENEMY_HQ, FRIENDLY_HQ, TARGET_CARD
}

/** 卡面原文中的触发时机。未写明时机时，指令按使用、单位按部署处理。 */
enum class EffectTrigger {
    DEPLOY, DESTRUCTION, ATTACK, ATTACKED, DAMAGED, TURN_START,
    FRIENDLY_ORDER, ENEMY_ORDER, ENEMY_DEPLOY, ENEMY_HQ_DAMAGE,
    MOVE_TO_FRONT, PASSIVE, COUNTDOWN
}

data class ParsedEffect(
    val op: EffectOp,
    val amount: Int = 0,
    val target: EffectTarget = EffectTarget.TARGET_UNIT,
    val source: String,
    val trigger: EffectTrigger = EffectTrigger.DEPLOY,
    /** 原文中明确指定的卡名等附加数据。 */
    val detail: String = ""
)

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

    private fun areaTarget(text: String): EffectTarget? = when {
        "所有敌方单位" in text || "全部敌方单位" in text || "所有敌方目标" in text -> EffectTarget.ENEMY_UNITS
        "所有友方单位" in text || "全部友方单位" in text -> EffectTarget.FRIENDLY_UNITS
        "所有单位" in text || "全部单位" in text -> EffectTarget.ALL_UNITS
        "敌方总部" in text || "其总部" in text -> EffectTarget.ENEMY_HQ
        "友方总部" in text -> EffectTarget.FRIENDLY_HQ
        else -> null
    }

    /** 单个目标按操作语义选择：增益默认友方，伤害与干扰默认敌方。 */
    private fun targetFor(op: EffectOp, text: String): EffectTarget {
        areaTarget(text)?.let { return it }
        if ("本单位" in text) return EffectTarget.SELF
        return when (op) {
            EffectOp.BUFF_ATTACK, EffectOp.BUFF_DEFENSE, EffectOp.REPAIR, EffectOp.HEAL_HQ ->
                if ("敌方" in text) EffectTarget.ENEMY_ONE else EffectTarget.FRIENDLY_ONE
            EffectOp.DAMAGE, EffectOp.DESTROY, EffectOp.PIN, EffectOp.SUPPRESS,
            EffectOp.RETREAT, EffectOp.TAKE_CONTROL, EffectOp.COUNTER_DEPLOY ->
                if ("友方" in text && "敌方" !in text) EffectTarget.FRIENDLY_ONE else EffectTarget.ENEMY_ONE
            else -> EffectTarget.SELF
        }
    }

    private fun triggerOf(text: String): EffectTrigger = when {
        "亡计" in text -> EffectTrigger.DESTRUCTION
        "倒计时" in text -> EffectTrigger.COUNTDOWN
        "部署：" in text || "部署:" in text -> EffectTrigger.DEPLOY
        "友方回合开始时" in text || "下个回合开始时" in text -> EffectTrigger.TURN_START
        "友方使用指令时" in text || "友方使用海军牌时" in text -> EffectTrigger.FRIENDLY_ORDER
        "敌方使用指令时" in text -> EffectTrigger.ENEMY_ORDER
        "敌方单位部署时" in text || "敌方部署单位" in text -> EffectTrigger.ENEMY_DEPLOY
        "受到伤害时" in text -> EffectTrigger.DAMAGED
        "攻击后" in text || "友方单位被攻击时" in text -> EffectTrigger.ATTACKED
        "移至前线时" in text || "占领前线时" in text -> EffectTrigger.MOVE_TO_FRONT
        "对敌方总部造成" in text || "其总部造成" in text -> EffectTrigger.ENEMY_HQ_DAMAGE
        "攻击时" in text -> EffectTrigger.ATTACK
        "若" in text || "具有" in text || "无法被" in text -> EffectTrigger.PASSIVE
        else -> EffectTrigger.DEPLOY
    }

    private fun sentence(text: String): List<ParsedEffect> {
        val trigger = triggerOf(text)
        val out = mutableListOf<ParsedEffect>()
        fun add(op: EffectOp, amount: Int = 0, to: EffectTarget? = null, detail: String = "") {
            out += ParsedEffect(op, amount, to ?: targetFor(op, text), text, trigger, detail)
        }
        if (Regex("造成\\s*([0-9]+|[一二两三四五六七八九十]+)\\s*点伤害").containsMatchIn(text)) {
            val toHq = areaTarget(text) == EffectTarget.ENEMY_HQ || areaTarget(text) == EffectTarget.FRIENDLY_HQ
            add(if (toHq) EffectOp.DAMAGE_HQ else EffectOp.DAMAGE, number(text, "点伤害"))
        }
        if ("消灭" in text) add(EffectOp.DESTROY)
        Regex("抽\\s*([0-9]+|[一二两三四五六七八九十]+)\\s*张").find(text)?.let { add(EffectOp.DRAW, number(text, "张"), EffectTarget.SELF) }
        Regex("获得\\s*([0-9]+|[一二两三四五六七八九十]+)\\s*个指挥点").find(text)?.let { add(EffectOp.GAIN_KREDITS, number(text, "个指挥点"), EffectTarget.SELF) }
        Regex("获得\\s*([0-9]+|[一二两三四五六七八九十]+)\\s*个指挥点槽").find(text)?.let { add(EffectOp.GAIN_SLOTS, number(text, "个指挥点槽"), EffectTarget.SELF) }
        if ("失去" in text && "指挥点槽" in text) {
            val n = number(text, "个指挥点槽").coerceAtLeast(1)
            add(EffectOp.LOSE_SLOTS, n, if ("敌方" in text || "对手" in text) EffectTarget.ENEMY_HQ else EffectTarget.SELF)
        }
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
        if ("弃" in text && "手牌" in text) {
            val n = number(text, "张").coerceAtLeast(1)
            add(if ("敌方" in text || "对手" in text) EffectOp.ENEMY_DISCARD else EffectOp.DISCARD, n)
        }
        Regex("情报\\s*([0-9])").find(text)?.let { add(EffectOp.INTEL, it.groupValues[1].toInt(), EffectTarget.SELF) }
        if ("开发" in text) add(EffectOp.DEVELOP, 0, EffectTarget.SELF)
        val quoted = Regex("[“\"]([^”\"]+)[”\"]").find(text)?.groupValues?.get(1).orEmpty()
        if ("加入手中" in text || "加入手牌" in text) {
            if ("复制" in text) add(EffectOp.ADD_COPY, 1, EffectTarget.SELF, quoted)
            else if (quoted.isNotBlank()) add(EffectOp.ADD_CARD, 1, EffectTarget.SELF, quoted)
        }
        if ("总部" in text && "防御" in text) {
            Regex("\\+\\s*([0-9]+)").find(text)?.let {
                add(EffectOp.HEAL_HQ, it.groupValues[1].toInt(), EffectTarget.FRIENDLY_HQ)
            }
        }
        if ("洗切卡组" in text) add(EffectOp.SHUFFLE_DECK, 0, EffectTarget.SELF)
        if ("攻击力等同于其防御力" in text) add(EffectOp.SET_ATTACK_TO_DEFENSE)
        Regex("-\\s*([0-9]+)\\s*花费").find(text)?.let { add(EffectOp.REDUCE_COST, it.groupValues[1].toInt(), EffectTarget.TARGET_CARD) }
        Regex("\\+\\s*([0-9]+)\\s*花费").find(text)?.let { add(EffectOp.INCREASE_COST, it.groupValues[1].toInt(), EffectTarget.TARGET_CARD) }
        if ("攻击力为 0" in text || "攻击力为0" in text) add(EffectOp.SET_ATTACK, 0)
        if ("放回卡组顶" in text || "返回手牌" in text || "返回手中" in text) add(EffectOp.RETURN_HAND)
        if ("揭示" in text) add(EffectOp.REVEAL)
        if ("复制" in text && "本回合被消灭" in text) add(EffectOp.COPY_DEAD_THIS_TURN)
        if ("反制其部署" in text) add(EffectOp.COUNTER_DEPLOY)
        return out
    }
}
