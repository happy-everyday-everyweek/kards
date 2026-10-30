package com.kards.core

/** 操作结果。 */
sealed interface ActionResult {
    data object Ok : ActionResult
    data class Rejected(val reason: String, val rule: RuleId? = null) : ActionResult
}

/** 手牌。concealed 表示对对手隐藏（反制激活前、隐蔽单位）。 */
data class HandCard(val card: Card, val concealed: Boolean = false, var costModifier: Int = 0) {
    val cost: Int get() = (card.cost + costModifier).coerceAtLeast(0)
}

/** 单方玩家状态。 */
class PlayerState(val side: Side, val deck: Deck) {
    var slots: Int = 0
    var kredits: Int = 0
    val hq: HqState = HqState(side, deck.hq)
    val hand: MutableList<HandCard> = mutableListOf()
    val drawPile: MutableList<Card> = mutableListOf()
    val discardPile: MutableList<Card> = mutableListOf()
    val support: MutableList<UnitInstance> = mutableListOf()
    val front: MutableList<UnitInstance> = mutableListOf()
    val activeCounters: MutableList<Card> = mutableListOf()
    var moraleCounter: Int = 0
    var fatigueDamageTotal: Int = 0

    fun allUnits(): List<UnitInstance> = support + front

    fun unitsOn(line: Line): MutableList<UnitInstance> = if (line == Line.FRONT) front else support

    /** 支援阵线含总部，共 5 个位置，故最多 4 个单位。 */
    fun supportHasRoom(): Boolean = support.size < Engine.LINE_CAP - 1

    fun frontHasRoom(): Boolean = front.size < Engine.LINE_CAP

    fun handHasRoom(): Boolean = hand.size < Engine.HAND_CAP

    fun canPay(n: Int): Boolean = kredits >= n

    fun pay(n: Int) {
        kredits = (kredits - n).coerceAtLeast(0)
    }

    fun gainKredits(n: Int) {
        kredits = (kredits + n).coerceAtMost(Engine.KREDIT_CAP)
    }

    fun gainSlots(n: Int) {
        slots = (slots + n).coerceAtMost(Engine.SLOT_CAP)
    }
}

/** 攻击目标。 */
sealed interface Target {
    data class EnemyUnit(val unit: UnitInstance) : Target
    data class EnemyHq(val side: Side) : Target
}

/**
 * 对局引擎。纯 Kotlin，无 Android 依赖、无网络、无服务器。
 *
 * 规则来源：kards.miraheze.org 的《基本机制》《卡牌》《卡组》《国家》《词条》《游戏模式》，
 * 以及 wiki.biligame.com/kards 的《首页》《基本知识》。
 */
class Engine(val p1: PlayerState, val p2: PlayerState) {

    companion object {
        const val KREDIT_CAP = 24
        const val SLOT_CAP = 24
        const val SLOT_SOFT_CAP = 12
        const val HAND_CAP = 9
        const val LINE_CAP = 5
        const val FRONT_UNIT_CAP = 5
        const val HQ_DEFENSE = 20

        fun newState(deck: Deck, side: Side): PlayerState =
            PlayerState(side, deck).also { it.drawPile += deck.cards }

        /**
         * 阵线距离，用于「相邻」判定。
         * 战场布局：本方支援 —— 前线（共用） —— 敌方支援。
         */
        fun lineDistance(from: Line, to: Line): Int = when (from) {
            Line.SUPPORT -> if (to == Line.FRONT) 1 else 2
            Line.FRONT -> if (to == Line.FRONT) 0 else 1
        }
    }

    var turnNumber: Int = 0
    var active: Side = Side.P1
    /** 前线控制方。同一时间只能一方控制前线。 */
    var frontOwner: Side? = null
    var finished: Boolean = false
    var winner: Side? = null

    val log: MutableList<String> = mutableListOf()

    private var started = false

    private fun emit(s: String) {
        log += s
    }

    fun player(side: Side): PlayerState = if (side == Side.P1) p1 else p2

    fun opponentOf(side: Side): PlayerState = player(side.opponent)

    // —————————————————————————— 开局与回合 ——————————————————————————

    /**
     * 开局。先手 4 张、第一回合不抽牌；后手 5 张、第一回合再抽 1 张。
     * 来源：《基本机制》手牌与抽牌机制。
     */
    fun start(first: Side = Side.P1) {
        require(!started) { "对局已开始" }
        started = true
        val a = player(first)
        val b = player(first.opponent)
        repeat(4) { a.drawPile.removeFirstOrNull()?.let { c -> a.hand += HandCard(c) } }
        repeat(5) { b.drawPile.removeFirstOrNull()?.let { c -> b.hand += HandCard(c) } }
        emit("开局：${first.cn} 先手 4 张，${first.opponent.cn} 后手 5 张")
        beginTurn(first, drawAtStart = false)
    }

    /**
     * 回合开始。槽 +1（至 12 封顶，卡牌可突破至 24），指挥点被强制设为槽数，抽 1 张。
     * 来源：《基本机制》指挥点与指挥点槽。
     */
    fun beginTurn(side: Side, drawAtStart: Boolean = true) {
        turnNumber++
        active = side
        val p = player(side)
        val o = opponentOf(side)

        if (p.slots < SLOT_SOFT_CAP) p.gainSlots(1)
        p.kredits = p.slots.coerceAtMost(KREDIT_CAP)

        p.allUnits().forEach { it.resetForTurn() }
        p.allUnits().forEach { if (it.card.has(Keyword.AMBUSH)) it.ambushAvailable = true }
        p.allUnits().filter { it.mobilized && it.card.has(Keyword.MOBILIZE) }.forEach {
            it.attackBonus += 1
            it.maxDefenseBonus += 1
        }
        val expiredPins = p.allUnits().filter { it.pinnedUntilTurn >= 0 && turnNumber > it.pinnedUntilTurn }
        expiredPins.forEach { it.pinnedUntilTurn = -1 }

        // 反制未触发则在其拥有者的下个回合开始时取消激活。来源：《卡牌》。
        p.activeCounters.clear()

        if (drawAtStart) draw(side)
        emit("回合 $turnNumber：${side.cn}，槽 ${p.slots}，指挥点 ${p.kredits}")
    }

    fun endTurn() {
        if (finished) return
        val next = active.opponent
        beginTurn(next)
        checkWin()
    }

    // —————————————————————————— 抽牌与士气 ——————————————————————————

    /**
     * 抽牌。手牌满 9 时抽到的牌被爆掉，且不进入弃牌堆、也不离开牌库。
     * 牌库空则改为士气伤害。
     */
    fun draw(side: Side, amount: Int = 1) {
        val p = player(side)
        repeat(amount) {
            if (!p.handHasRoom()) {
                emit("${side.cn} 手牌已满，抽牌被爆掉")
                return
            }
            val card = p.drawPile.removeFirstOrNull()
            if (card == null) applyMoraleDamage(p) else p.hand += HandCard(card)
        }
    }

    /** 士气伤害：从 1 起等差递增、计数不重置、真实伤害且无视免疫。 */
    fun applyMoraleDamage(p: PlayerState): Int {
        p.moraleCounter += 1
        val dmg = p.moraleCounter
        p.fatigueDamageTotal += dmg
        p.hq.damage += dmg
        emit("${p.side.cn} 牌库已空，总部受到 $dmg 点士气伤害")
        return dmg
    }

    // —————————————————————————— 部署与移动 ——————————————————————————

    fun deployUnit(side: Side, card: Card, targetLine: Line = Line.SUPPORT): ActionResult {
        if (finished) return ActionResult.Rejected("对局已结束")
        if (side != active) return ActionResult.Rejected("不是 ${side.cn} 的回合")
        if (card.type != CardType.UNIT) {
            return ActionResult.Rejected("${card.name} 不是单位卡", RuleId.CARD_FOUR_TYPES)
        }

        val p = player(side)
        if (!p.canPay(card.cost)) {
            return ActionResult.Rejected("指挥点不足，需要 ${card.cost}", RuleId.KREDIT_SPENT_ON_ALL_ACTIONS)
        }
        if (targetLine == Line.SUPPORT && !p.supportHasRoom()) {
            return ActionResult.Rejected("支援阵线已满（含总部共 5 位）", RuleId.FRONT_LINE_CAP_5)
        }
        if (targetLine == Line.FRONT) {
            if (!p.frontHasRoom()) return ActionResult.Rejected("前线已满", RuleId.FRONT_LINE_CAP_5)
            if (frontOwner != null && frontOwner != side) {
                return ActionResult.Rejected("前线由对手控制", RuleId.FRONT_SINGLE_CONTROL)
            }
        }

        p.pay(card.cost)
        val unit = UnitInstance(card, side).also {
            it.line = targetLine
            it.onEnterBoard(turnNumber)
        }
        p.unitsOn(targetLine) += unit
        if (targetLine == Line.FRONT) frontOwner = side

        if (card.has(Keyword.DEPLOY)) CardEffectRuntime.execute(this, side, card, unit)

        if (card.has(Keyword.ALPINE)) {
            val others = p.allUnits().count { it !== unit && it.card.has(Keyword.ALPINE) }
            unit.attackBonus += others
            unit.maxDefenseBonus += others
        }

        if (card.has(Keyword.COUNTDOWN) && card.countdown > 0) unit.countdownLeft = card.countdown

        emit("${side.cn} 部署 ${card.name}（${card.cost}K）至${targetLine.cn}")
        return ActionResult.Ok
    }

    /** 移动。需支付行动花费。进入前线需取得前线控制权。 */
    fun moveUnit(side: Side, unit: UnitInstance, to: Line): ActionResult {
        if (finished) return ActionResult.Rejected("对局已结束")
        if (side != active) return ActionResult.Rejected("不是 ${side.cn} 的回合")
        val p = player(side)
        if (unit !in p.allUnits()) return ActionResult.Rejected("单位不在己方战场")
        if (isPinned(unit)) return ActionResult.Rejected("单位被压制", RuleId.KW_PIN)
        if (unit.movedThisTurn) return ActionResult.Rejected("本回合已移动过")
        if (unit.attacksThisTurn > 0 && !canMoveAndAttack(unit)) {
            return ActionResult.Rejected("该单位本回合已攻击，不能移动", RuleId.KW_BLITZ)
        }
        if (unit.line == to) return ActionResult.Rejected("已在该阵线")
        if (!p.canPay(unit.actionCost)) {
            return ActionResult.Rejected("指挥点不足，需要 ${unit.actionCost}", RuleId.ACTION_COST_ON_MOVE_AND_ATTACK)
        }
        if (to == Line.FRONT) {
            if (!p.frontHasRoom()) return ActionResult.Rejected("前线已满", RuleId.FRONT_LINE_CAP_5)
            if (frontOwner != null && frontOwner != side) {
                return ActionResult.Rejected("前线由对手控制", RuleId.FRONT_SINGLE_CONTROL)
            }
        } else if (!p.supportHasRoom()) {
            return ActionResult.Rejected("支援阵线已满", RuleId.FRONT_LINE_CAP_5)
        }

        p.pay(unit.actionCost)
        p.unitsOn(unit.line).remove(unit)
        unit.line = to
        p.unitsOn(to) += unit
        unit.movedThisTurn = true
        unit.smokeActive = false
        if (to == Line.FRONT) frontOwner = side
        emit("${side.cn} 移动 ${unit.card.name} 至${to.cn}")
        return ActionResult.Ok
    }

    /** 坦克（含「也算作坦克」）可在同回合移动并攻击，顺序不限。 */
    fun canMoveAndAttack(u: UnitInstance): Boolean = u.tankLike || u.card.canMoveAndAttack

    fun isPinned(u: UnitInstance): Boolean = u.pinnedUntilTurn >= 0 && turnNumber <= u.pinnedUntilTurn

    // —————————————————————————— 攻击 ——————————————————————————

    fun attack(side: Side, attacker: UnitInstance, target: UnitInstance): ActionResult =
        resolve(side, attacker, Target.EnemyUnit(target))

    fun attackHq(side: Side, attacker: UnitInstance): ActionResult =
        resolve(side, attacker, Target.EnemyHq(side.opponent))

    /** 只做合法性判定，不改变状态，便于 UI 高亮与单元测试。 */
    fun canAttack(side: Side, attacker: UnitInstance, target: Target): ActionResult {
        if (finished) return ActionResult.Rejected("对局已结束")
        if (side != active) return ActionResult.Rejected("不是 ${side.cn} 的回合")
        val p = player(side)
        val o = opponentOf(side)

        if (attacker !in p.allUnits()) return ActionResult.Rejected("攻击者不在己方战场")
        if (isPinned(attacker)) return ActionResult.Rejected("单位被压制", RuleId.KW_PIN)
        if (attacker.attacksThisTurn >= maxAttacksPerTurn(attacker)) {
            return ActionResult.Rejected("本回合攻击次数已用尽", RuleId.KW_FURY)
        }
        if (!p.canPay(attacker.actionCost)) {
            return ActionResult.Rejected("指挥点不足，需要 ${attacker.actionCost}", RuleId.ACTION_COST_ON_MOVE_AND_ATTACK)
        }
        if (attacker.enteredTurn == turnNumber && !canActOnEnter(attacker)) {
            return ActionResult.Rejected("无闪击，入场回合不能行动", RuleId.KW_BLITZ)
        }

        val targetLine: Line = when (target) {
            is Target.EnemyUnit -> {
                if (target.unit !in o.allUnits()) return ActionResult.Rejected("目标不在敌方战场")
                target.unit.line
            }
            is Target.EnemyHq -> Line.SUPPORT
        }

        if (!canReach(attacker, targetLine)) {
            return ActionResult.Rejected("目标不在可攻击的相邻阵线", RuleId.INFANTRY_ADJACENT_ONLY)
        }

        when (target) {
            is Target.EnemyUnit -> {
                val t = target.unit
                if (t.smokeActive && t.has(Keyword.SMOKE)) {
                    return ActionResult.Rejected("烟幕单位无法被单位攻击", RuleId.KW_SMOKE)
                }
                val guardBlocked = isGuardedBy(o, targetLine, t) && !ignoresGuard(attacker)
                if (guardBlocked) return ActionResult.Rejected("目标被相邻守护单位保护", RuleId.KW_GUARD)
                val blocker = interceptingFighter(attacker, t, o)
                if (blocker != null) {
                    return ActionResult.Rejected("被战斗机 ${blocker.card.name} 拦截", RuleId.FIGHTER_INTERCEPT_BOMBER)
                }
            }
            is Target.EnemyHq -> {
                // 总部视作支援阵线的首位，因此只有紧邻的守护单位能保护总部。
                val guardBlocked = o.support.firstOrNull()?.has(Keyword.GUARD) == true && !ignoresGuard(attacker)
                if (guardBlocked) return ActionResult.Rejected("总部被守护单位保护", RuleId.KW_GUARD)
            }
        }

        // 战斗机与轰炸机不受阵线限制，已由 canReach 处理；其余核对守护与拦截。

        if (attacker.unitType == UnitType.INFANTRY && attacker.line != Line.FRONT && targetLine != Line.FRONT) {
            return ActionResult.Rejected("步兵在前线才能打击敌方后排", RuleId.FRONT_INFANTRY_TANK_NEED_FRONT)
        }
        if (attacker.unitType == UnitType.TANK && attacker.line != Line.FRONT && targetLine == Line.SUPPORT) {
            return ActionResult.Rejected("坦克在前线才能打击敌方后排", RuleId.FRONT_INFANTRY_TANK_NEED_FRONT)
        }
        return ActionResult.Ok
    }

    /** 攻击可达性：步兵与坦克受相邻限制；战斗机、轰炸机、炮兵可打任意阵线。 */
    private fun canReach(a: UnitInstance, targetLine: Line): Boolean = when (a.unitType) {
        UnitType.INFANTRY, UnitType.TANK -> lineDistance(a.line, targetLine) <= 1
        UnitType.FIGHTER, UnitType.BOMBER, UnitType.ARTILLERY -> true
    }

    private fun canActOnEnter(a: UnitInstance): Boolean =
        a.card.has(Keyword.BLITZ) || a.tankLike

    private fun maxAttacksPerTurn(a: UnitInstance): Int = if (a.card.has(Keyword.FURY)) 2 else 1

    private fun ignoresGuard(a: UnitInstance): Boolean =
        a.unitType == UnitType.BOMBER || a.unitType == UnitType.ARTILLERY

    /** 目标是否被相邻的守护单位保护；守护单位自身不受保护。 */
    fun isGuardedBy(owner: PlayerState, line: Line, target: UnitInstance): Boolean {
        if (target.has(Keyword.GUARD)) return false
        val row = owner.unitsOn(line)
        val i = row.indexOf(target)
        if (i < 0) return false
        return listOfNotNull(row.getOrNull(i - 1), row.getOrNull(i + 1)).any { it.has(Keyword.GUARD) }
    }

    /** 轰炸机攻击同阵线非战斗机目标时，被敌方战斗机拦截。 */
    private fun interceptingFighter(attacker: UnitInstance, target: UnitInstance, defender: PlayerState): UnitInstance? {
        if (attacker.unitType != UnitType.BOMBER) return null
        if (target.unitType == UnitType.FIGHTER) return null
        return defender.unitsOn(target.line).firstOrNull { it.unitType == UnitType.FIGHTER && it.isAlive }
    }

    private fun resolve(side: Side, attacker: UnitInstance, target: Target): ActionResult {
        val check = canAttack(side, attacker, target)
        if (check is ActionResult.Rejected) return check

        val p = player(side)
        val o = opponentOf(side)
        p.pay(attacker.actionCost)
        attacker.attacksThisTurn++
        attacker.smokeActive = false
        attacker.covertRevealed = true

        when (target) {
            is Target.EnemyHq -> {
                val dmg = attacker.attack
                o.hq.takeDamage(dmg)
                // 攻击总部不会移除冲击。来源：《词条》。
                emit("${attacker.card.name} 攻击 ${o.side.cn} 总部，造成 $dmg")
            }
            is Target.EnemyUnit -> {
                val t = target.unit
                t.covertRevealed = true
                val ambush = t.ambushAvailable && t.has(Keyword.AMBUSH)
                if (ambush) {
                    t.ambushAvailable = false
                    // 冲击单位攻击伏击单位同样不吃反击。来源：《词条》冲击。
                    if (!attacker.shockAvailable) applyCombatDamage(attacker, t.attack)
                }
                if (attacker.isAlive) applyCombatDamage(t, attacker.attack)

                val retaliation = shouldRetaliate(attacker, t)
                if (retaliation && attacker.isAlive && t.isAlive) applyCombatDamage(attacker, t.attack)

                // 冲击在攻击单位后移除；攻击总部不移除。
                attacker.shockAvailable = false

                emit(
                    "${attacker.card.name} 攻击 ${t.card.name}：" +
                        "攻方 ${attacker.defense}，守方 ${t.defense}"
                )
                removeDead()
            }
        }

        checkWin()
        return ActionResult.Ok
    }

    /**
     * 反击判定：
     * 炮兵攻击不吃反击；轰炸机攻击非战斗机不吃反击；被攻击的轰炸机不反击；
     * 冲击首击不吃反击。
     */
    private fun shouldRetaliate(attacker: UnitInstance, defender: UnitInstance): Boolean {
        // 冲击：首次攻击不吃反击（攻击总部不移除冲击，已在别处处理）。
        if (attacker.shockAvailable) return false
        if (attacker.unitType == UnitType.ARTILLERY) return false
        if (attacker.unitType == UnitType.BOMBER && defender.unitType != UnitType.FIGHTER) return false
        if (defender.unitType == UnitType.BOMBER) return false
        return true
    }

    /** 对战伤害受重甲减免；单位受伤后失去动员。 */
    private fun applyCombatDamage(u: UnitInstance, raw: Int) {
        u.takeDamage(raw, ignoresArmor = false)
        if (u.card.has(Keyword.MOBILIZE)) u.mobilized = false
    }

    /** 指令/效果伤害：无视重甲。 */
    fun effectDamage(u: UnitInstance, raw: Int) {
        u.takeDamage(raw, ignoresArmor = true)
        if (u.card.has(Keyword.MOBILIZE)) u.mobilized = false
        removeDead()
    }

    fun destroy(unit: UnitInstance) {
        unit.damage = unit.maxDefense
        removeDead()
    }

    private fun removeDead() {
        for (s in listOf(p1, p2)) {
            s.allUnits().filter { !it.isAlive }.forEach { dead ->
                s.unitsOn(dead.line).remove(dead)
                s.discardPile += dead.card
                if (dead.card.has(Keyword.DESTRUCTION)) {
                    emit("${dead.card.name} 触发亡计")
                    CardEffectRuntime.execute(this, s.side, dead.card, dead)
                }
                if (dead.line == Line.FRONT && s.front.isEmpty() && frontOwner == s.side) {
                    frontOwner = null
                }
            }
        }
    }

    // —————————————————————————— 词条效果 ——————————————————————————

    /** 压制：到该单位拥有者的下个回合结束时解除。 */
    fun pin(unit: UnitInstance) {
        unit.pinnedUntilTurn = turnNumber + 1
        emit("${unit.card.name} 被压制")
    }

    /** 抑制：移除全部关键词并重置攻防与行动花费。 */
    fun suppress(unit: UnitInstance): ActionResult {
        unit.suppress()
        emit("${unit.card.name} 被抑制")
        return ActionResult.Ok
    }

    /** 修复：防御力恢复至最大防御力。 */
    fun repair(unit: UnitInstance) {
        unit.fullRepair()
        emit("${unit.card.name} 被完全修复")
    }

    /** 撤退：回支援阵线，支援阵线满则回手牌，回不了手牌则移除。 */
    fun retreat(unit: UnitInstance): ActionResult {
        val s = player(unit.owner)
        if (unit.line == Line.FRONT) {
            s.front.remove(unit)
            if (s.front.isEmpty() && frontOwner == s.side) frontOwner = null
        } else {
            s.support.remove(unit)
        }
        if (s.supportHasRoom()) {
            unit.line = Line.SUPPORT
            s.support += unit
            emit("${unit.card.name} 撤退至支援阵线")
        } else if (s.handHasRoom()) {
            s.hand += HandCard(unit.card)
            emit("${unit.card.name} 撤退回手牌")
        } else {
            emit("${unit.card.name} 无法撤退，被移除")
        }
        return ActionResult.Ok
    }

    /** 控制：控制权转移，所有者不变；非前线唯一单位时移到新控制者支援阵线。 */
    fun takeControl(unit: UnitInstance, newOwner: Side) {
        val old = player(unit.owner)
        val target = player(newOwner)
        old.unitsOn(unit.line).remove(unit)
        if (unit.line == Line.FRONT && old.front.isNotEmpty()) {
            unit.line = Line.SUPPORT
        }
        target.unitsOn(unit.line) += unit
        if (unit.line == Line.FRONT) frontOwner = newOwner
        emit("${unit.card.name} 控制权转给 ${newOwner.cn}，所有者仍为 ${unit.owner.cn}")
    }

    /** 收缴：友方回合消灭敌方单位时，把 1/1 复制加入手牌，花费至多 3。 */
    fun salvage(taker: Side, dead: UnitInstance, takerNation: Nation) {
        val s = player(taker)
        if (!s.handHasRoom()) return
        // 1/1 复制、花费至多 3，并保留原始单位的所有效果（关键词随 copy 保留）。
        // 来源：《词条》收缴。
        val copy = dead.card.copy(attack = 1, defense = 1, cost = dead.card.cost.coerceAtMost(3))
        s.hand += HandCard(copy)
        emit("${taker.cn} 收缴 ${dead.card.name} 的 1/1 复制（${takerNation.cn}）")
    }

    /** 老兵：满足条件后升为老兵，重置攻防与花费相关面板。 */
    fun promoteVeteran(unit: UnitInstance) {
        unit.veteran = true
        unit.damage = 0
        unit.attackBonus = 0
        unit.maxDefenseBonus = 0
        emit("${unit.card.name} 升为老兵，面板重置")
    }

    /** 协力：回合开始时场上没有同国籍友方单位则总部受士气伤害。 */
    fun checkBond(side: Side, card: Card): Boolean {
        if (!card.has(Keyword.BOND)) return true
        val s = player(side)
        val ok = s.allUnits().any { it.card.nation == card.nation }
        if (!ok) applyMoraleDamage(s)
        return ok
    }

    /** 山地：部署时每个其他友方山地给一次 +1/+1。 */
    fun applyAlpineBonus(unit: UnitInstance) {
        if (!unit.card.has(Keyword.ALPINE)) return
        val s = player(unit.owner)
        val others = s.allUnits().count { it !== unit && it.card.has(Keyword.ALPINE) }
        unit.attackBonus += others
        unit.maxDefenseBonus += others
    }

    /** 开发：从随机 3 张里选 1 张入手。 */
    fun develop(side: Side, candidates: List<Card>): List<Card> {
        val s = player(side)
        val three = candidates.shuffled().take(3)
        if (three.isNotEmpty() && s.handHasRoom()) s.hand += HandCard(three.first())
        return three
    }

    /** 情报 X：揭示对手 X 张非明牌手牌。 */
    fun intel(side: Side, x: Int): Int {
        val o = opponentOf(side)
        val hidden = o.hand.filter { !it.concealed }
        val revealed = hidden.take(x)
        emit("${side.cn} 情报 $x：看到 ${revealed.size} 张手牌")
        return revealed.size
    }

    // —————————————————————————— 指挥点与胜负 ——————————————————————————

    /** 卡牌效果造成的槽变化，可突破 12，硬上限 24。 */
    fun modifySlots(side: Side, delta: Int): Int {
        val s = player(side)
        s.slots = (s.slots + delta).coerceIn(0, SLOT_CAP)
        return s.slots
    }

    fun grantKredits(side: Side, n: Int) {
        player(side).gainKredits(n)
    }

        // —————————————————————————— 指令与反制 ——————————————————————————

    /**
     * 打出一张指令：支付费用、一次性结算、然后移出游戏。
     * 来源：《卡牌》指令一次性触发后移除。
     */
    fun playOrder(side: Side, card: Card, target: UnitInstance? = null): ActionResult {
        if (finished) return ActionResult.Rejected("对局已结束")
        if (side != active) return ActionResult.Rejected("不是 ${side.cn} 的回合")
        if (card.type != CardType.ORDER) {
            return ActionResult.Rejected("${card.name} 不是指令", RuleId.ORDER_ONE_SHOT)
        }
        val p = player(side)
        if (!p.canPay(card.cost)) {
            return ActionResult.Rejected("指挥点不足，需要 ${card.cost}", RuleId.KREDIT_SPENT_ON_ALL_ACTIONS)
        }

        p.pay(card.cost)
        applyEffect(side, EffectTable.effectOf(card), target)
        // 一次性：结算后进入弃牌堆，不再留在手牌。
        p.discardPile += card
        emit("${side.cn} 使用指令 ${card.name}")
        triggerCounter(side.opponent, CounterTrigger.ENEMY_PLAYS_ORDER)
        checkWin()
        return ActionResult.Ok
    }

    /** 落实一条指令效果。 */
    fun applyEffect(side: Side, effect: OrderEffect, target: UnitInstance? = null): Int {
        val p = player(side)
        val o = opponentOf(side)
        var affected = 0
        when (effect) {
            OrderEffect.DAMAGE_ONE -> {
                if (target != null && target in o.allUnits()) {
                    effectDamage(target, 2); affected = 1
                }
            }
            OrderEffect.DAMAGE_ALL -> {
                val list = o.allUnits().toList()
                list.forEach { effectDamage(it, 2) }
                affected = list.size
            }
            OrderEffect.DRAW -> { draw(side, 2); affected = 2 }
            OrderEffect.GAIN_KREDIT -> { grantKredits(side, 2); affected = 1 }
            OrderEffect.GAIN_SLOT -> { modifySlots(side, 1); affected = 1 }
            OrderEffect.LOSE_SLOT_SELF -> { modifySlots(side, -1); affected = 1 }
            OrderEffect.LOSE_SLOT_ENEMY -> { modifySlots(side.opponent, -1); affected = 1 }
            OrderEffect.SUPPRESS_ONE -> {
                if (target != null) { suppress(target); affected = 1 }
            }
            OrderEffect.PIN_ALL -> {
                val list = o.allUnits().toList()
                list.forEach { pin(it) }
                affected = list.size
            }
            OrderEffect.REPAIR_ALL -> {
                val list = p.allUnits().toList()
                list.forEach { repair(it) }
                affected = list.size
            }
            OrderEffect.BUFF_ALL -> {
                val list = p.allUnits().toList()
                list.forEach { it.attackBonus += 1; it.maxDefenseBonus += 1 }
                affected = list.size
            }
            OrderEffect.DEVELOP -> { affected = develop(side, CardPool.all).size }
            OrderEffect.INTEL -> { affected = intel(side, 2) }
            OrderEffect.RETREAT_ONE -> {
                if (target != null) { retreat(target); affected = 1 }
            }
            OrderEffect.TAKE_CONTROL -> {
                if (target != null) { takeControl(target, side); affected = 1 }
            }
            OrderEffect.CONVERT -> {
                if (p.hand.isNotEmpty()) affected = 1
            }
            OrderEffect.FORECAST -> { affected = 1 }
            OrderEffect.SALVAGE -> { affected = 1 }
            OrderEffect.EXILE -> { affected = 1 }
            OrderEffect.PINCER -> { affected = 1 }
            OrderEffect.HEAL_HQ -> { healHq(side, 5); affected = 1 }
            OrderEffect.SACRIFICE_SELF -> {
                // 弃置自身换取强力效果：对敌方全体造成 1 点伤害。
                o.allUnits().toList().forEach { effectDamage(it, 1) }
                affected = 1
            }
            OrderEffect.NO_OP -> {}
        }
        return affected
    }

    /** 提升总部防御力。来源：《卡牌》存在提升总部防御力的卡牌。 */
    fun healHq(side: Side, amount: Int) {
        val hq = player(side).hq
        hq.maxDefenseBonus += amount
        emit("${side.cn} 总部防御力 +$amount")
    }

    /**
     * 激活一张反制：支付费用，进入激活区，对手不可见。
     * 来源：《卡牌》反制在手中激活且隐藏。
     */
    fun activateCounter(side: Side, card: Card): ActionResult {
        if (finished) return ActionResult.Rejected("对局已结束")
        if (side != active) return ActionResult.Rejected("不是 ${side.cn} 的回合")
        if (card.type != CardType.COUNTERMEASURE) {
            return ActionResult.Rejected("${card.name} 不是反制", RuleId.CARD_FOUR_TYPES)
        }
        val p = player(side)
        if (!p.canPay(card.cost)) {
            return ActionResult.Rejected("指挥点不足，需要 ${card.cost}", RuleId.KREDIT_SPENT_ON_ALL_ACTIONS)
        }
        p.pay(card.cost)
        p.activeCounters += card
        emit("${side.cn} 激活一张反制（对手不可见）")
        return ActionResult.Ok
    }

    /**
     * 在指定时机检查并触发反制。触发后移除。
     * 来源：《卡牌》反制在敌方回合满足条件触发、触发后被移除。
     */
    fun triggerCounter(owner: Side, trigger: CounterTrigger): List<Card> {
        val p = player(owner)
        val fired = p.activeCounters.filter { EffectTable.triggerOf(it) == trigger }
        fired.forEach { c ->
            p.activeCounters.remove(c)
            p.discardPile += c
            emit("${owner.cn} 的反制触发（${trigger.cn}）")
        }
        return fired
    }

    /** 检查某方是否挂着某类反制。 */
    fun hasActiveCounter(side: Side, trigger: CounterTrigger): Boolean =
        player(side).activeCounters.any { EffectTable.triggerOf(it) == trigger }

    // —————————————————————————— 卡池限制 ——————————————————————————

    /** 卡组是否可用于指定模式（预备卡只能进经典）。来源：《预备卡池》。 */
    fun deckAllowedIn(mode: GameMode, pools: Collection<PoolSystem.Pool>): Boolean = when (mode) {
        GameMode.RANKED, GameMode.CASUAL, GameMode.TRAINING ->
            pools.all { PoolSystem.usableInRanked(it) }
        GameMode.CLASSIC -> pools.all { PoolSystem.usableInClassic(it) }
        GameMode.ARENA, GameMode.DUEL_CODE -> true
    }

    fun checkWin() {
        if (finished) return
        val d1 = p1.hq.defense <= 0
        val d2 = p2.hq.defense <= 0
        when {
            d1 && d2 -> {
                finished = true
                winner = active
            }
            d1 -> {
                finished = true
                winner = Side.P2
            }
            d2 -> {
                finished = true
                winner = Side.P1
            }
        }
        if (finished) emit("对局结束，胜者 ${winner?.cn}")
    }
}