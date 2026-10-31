package com.kards.app.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kards.core.ActionResult
import com.kards.core.Card
import com.kards.core.CardPool
import com.kards.core.CardType
import com.kards.core.Deck
import com.kards.core.Engine
import com.kards.core.HandCard
import com.kards.core.Keyword
import com.kards.core.Line
import com.kards.core.Nation
import com.kards.core.Side
import com.kards.core.Target
import com.kards.core.UnitInstance
import com.kards.core.UnitType

class GameController(
    private val campaign: Campaign? = null,
    private val mission: Mission? = null,
    private val strategy: CampaignStrategy? = null,
    private val customDeck: Deck? = null,
    private val onMissionComplete: (Int) -> Unit = {}
) {
    private val playerNation = campaign?.nation ?: customDeck?.mainNation ?: Nation.GERMANY

    var selectedHandIndex by mutableStateOf(-1)
        private set
    var selectedUnit by mutableStateOf<UnitInstance?>(null)
        private set
    var resultText by mutableStateOf<String?>(null)
        private set
    var toast by mutableStateOf<String?>(null)
        private set
    lateinit var engine: Engine
        private set
    var revision by mutableStateOf(0)
        private set

    var frontTurns by mutableStateOf(0)
        private set
    var kills by mutableStateOf(0)
        private set
    var losses by mutableStateOf(0)
        private set
    var survivedTurns by mutableStateOf(0)
        private set
    var earnedStars by mutableStateOf(0)
        private set
    private var missionWon = false
    private var lastFriendlyIds: Set<Int> = emptySet()
    private var lastEnemyIds: Set<Int> = emptySet()

    init { newGame() }

    fun newGame() {
        val base = customDeck?.takeIf { campaign == null || it.mainNation == campaign.nation }
            ?: buildDeck(playerNation)
        val p1Deck = applyStrategy(base)
        val p2Base = buildDeck(campaign?.enemy ?: botNationFor(playerNation))
        val p2Deck = if ((mission?.enemyHqBonus ?: 0) > 0) {
            p2Base.copy(hq = p2Base.hq.copy(defense = p2Base.hq.defense + mission!!.enemyHqBonus))
        } else p2Base

        engine = Engine(Engine.newState(p1Deck, Side.P1), Engine.newState(p2Deck, Side.P2))
        engine.start(Side.P1)

        when (strategy?.bonus) {
            StrategyBonus.START_SLOT -> engine.p1.gainSlots(strategy.amount)
            StrategyBonus.SUPPLY_DRAW -> repeat(strategy.amount) { engine.draw(Side.P1) }
            else -> Unit
        }

        frontTurns = 0
        kills = 0
        losses = 0
        survivedTurns = 0
        earnedStars = 0
        missionWon = false
        selectedHandIndex = -1
        selectedUnit = null
        resultText = null
        toast = null
        lastFriendlyIds = engine.p1.allUnits().map { it.hashCode() }.toSet()
        lastEnemyIds = engine.p2.allUnits().map { it.hashCode() }.toSet()
        refresh()
    }

    private fun botNationFor(player: Nation): Nation =
        campaign?.enemy ?: if (player == Nation.USSR) Nation.GERMANY else Nation.USSR

    private fun applyStrategy(deck: Deck): Deck {
        val s = strategy ?: return deck
        val cards = deck.cards.map { c -> applyStrategyToCard(c, s) }
        val hq = if (s.bonus == StrategyBonus.HQ_DEFENSE) deck.hq.copy(defense = deck.hq.defense + s.amount) else deck.hq
        return Deck(deck.mainNation, hq, cards)
    }

    private fun applyStrategyToCard(c: Card, s: CampaignStrategy): Card = when (s.bonus) {
        StrategyBonus.ELITE_ATTACK -> if (c.type == CardType.UNIT) c.copy(attack = c.attack + s.amount) else c
        StrategyBonus.UNIT_DEFENSE -> if (c.type == CardType.UNIT) c.copy(defense = c.defense + s.amount) else c
        StrategyBonus.INFANTRY_COST -> if (c.type == CardType.UNIT && c.unitType == UnitType.INFANTRY)
            c.copy(cost = (c.cost - s.amount).coerceAtLeast(0)) else c
        StrategyBonus.AIR_COST -> if (c.type == CardType.UNIT &&
            (c.unitType == UnitType.FIGHTER || c.unitType == UnitType.BOMBER))
            c.copy(cost = (c.cost - s.amount).coerceAtLeast(0)) else c
        StrategyBonus.TANK_COST -> if (c.type == CardType.UNIT && c.unitType == UnitType.TANK)
            c.copy(cost = (c.cost - s.amount).coerceAtLeast(0)) else c
        else -> c
    }

    private fun buildDeck(nation: Nation): Deck {
        val pool = CardPool.ofNation(nation).filter { it.type != CardType.HQ }
            .sortedBy { it.cost }
        require(pool.isNotEmpty()) { "${nation.cn} 没有可用卡牌" }
        val chosen = mutableListOf<Card>()
        fun countOf(name: String) = chosen.count { it.name == name }
        var guard = 0
        while (chosen.size < 39 && guard++ < 10000) {
            var added = false
            for (c in pool) {
                if (chosen.size >= 39) break
                if (countOf(c.name) < c.rarity.maxCopies) {
                    chosen += c
                    added = true
                }
            }
            if (!added) break
        }
        var i = 0
        while (chosen.size < 39) { chosen += pool[i % pool.size]; i++ }
        return Deck(nation, CardPool.hqFor(nation), chosen.take(39))
    }

    fun refresh() {
        revision++
        syncCounters()
        if (engine.finished && resultText == null) {
            resultText = when (engine.winner) {
                Side.P1 -> finishMission()
                Side.P2 -> "战役失败"
                else -> "对局结束"
            }
        }
    }

    /**
     * 用场上单位快照差统计消灭与损失。
     *
     * 回到手牌的单位也会被计入一次损失，这与实机口径略有差异，
     * 但比完全不统计更接近，且不会影响消灭数。
     */
    private fun syncCounters() {
        val friendly = engine.p1.allUnits().map { it.hashCode() }.toSet()
        val enemy = engine.p2.allUnits().map { it.hashCode() }.toSet()
        losses += (lastFriendlyIds - friendly).size
        kills += (lastEnemyIds - enemy).size
        lastFriendlyIds = friendly
        lastEnemyIds = enemy
    }

    private fun challengeMet(c: StarChallenge): Boolean = when (c.kind) {
        ChallengeKind.LOSSES_AT_MOST -> losses <= c.amount
        ChallengeKind.CONTROL_FRONT_AT_END -> engine.frontOwner == Side.P1
        ChallengeKind.HQ_DEFENSE_AT_LEAST -> engine.p1.hq.defense >= c.amount
        ChallengeKind.KILL_RATIO_AT_LEAST -> kills >= losses * c.amount.coerceAtLeast(1)
        ChallengeKind.NO_FRONT_AT_WIN -> engine.frontOwner != Side.P1
        ChallengeKind.KILLS_AT_LEAST -> kills >= c.amount
    }

    /** 星级等于达成的挑战条数；通关本身保底 1 星。 */
    private fun starCount(): Int {
        val m = mission ?: return 0
        if (m.challenges.isEmpty()) return 1
        return m.challenges.count { challengeMet(it) }.coerceIn(1, 3)
    }

    private fun objectiveReached(): Boolean {
        val m = mission ?: return false
        return when (m.goal) {
            MissionGoal.KILL_UNITS -> kills >= m.goalAmount
            MissionGoal.FRONT_UNITS -> engine.p1.front.size >= m.goalAmount
            MissionGoal.DESTROY_NAMED_HQ -> engine.p2.hq.defense <= 0
            MissionGoal.DESTROY_HQ -> engine.p2.hq.defense <= 0
        }
    }

    private fun finishMission(): String {
        missionWon = true
        val stars = starCount()
        earnedStars = stars
        onMissionComplete(stars)
        return if (campaign != null) "战役胜利 · $stars 星" else "你赢了"
    }

    /** 玩家在自己的回合结束时结算消灭类任务。 */
    private fun checkObjectiveOnEndTurn() {
        if (missionWon || mission == null || engine.finished) return
        if (mission.goal != MissionGoal.KILL_UNITS) return
        if (!objectiveReached()) return
        missionWon = true
        val stars = starCount()
        earnedStars = stars
        onMissionComplete(stars)
        resultText = "任务完成 · $stars 星"
        revision++
    }

    /** 玩家新回合开始时结算前线驻守类任务。 */
    private fun checkObjectiveOnBeginTurn() {
        if (missionWon || mission == null || engine.finished) return
        if (mission.goal != MissionGoal.FRONT_UNITS) return
        if (!objectiveReached()) return
        missionWon = true
        val stars = starCount()
        earnedStars = stars
        onMissionComplete(stars)
        resultText = "任务完成 · $stars 星"
        revision++
    }

    fun challengeStatus(): List<Pair<StarChallenge, Boolean>> =
        mission?.challenges?.map { it to challengeMet(it) } ?: emptyList()

    private fun say(msg: String) { toast = msg; refresh() }
    fun clearToast() { toast = null }

    val hand: List<HandCard> get() = engine.p1.hand
    val mySupport: List<UnitInstance> get() = engine.p1.support
    val myFront: List<UnitInstance> get() = engine.p1.front
    val enemySupport: List<UnitInstance> get() = engine.p2.support
    val enemyFront: List<UnitInstance> get() = engine.p2.front
    val myHqDefense: Int get() = engine.p1.hq.defense
    val enemyHqDefense: Int get() = engine.p2.hq.defense
    val myKredits: Int get() = engine.p1.kredits
    val mySlots: Int get() = engine.p1.slots
    val isMyTurn: Boolean get() = engine.active == Side.P1 && !engine.finished && !missionWon

    fun selectHand(index: Int) {
        selectedHandIndex = if (selectedHandIndex == index) -1 else index
        selectedUnit = null
        refresh()
    }

    fun selectUnit(unit: UnitInstance) {
        selectedUnit = if (selectedUnit === unit) null else unit
        selectedHandIndex = -1
        refresh()
    }

    fun deploySelected(line: Line) {
        val idx = selectedHandIndex
        if (idx !in hand.indices) return
        val selected = hand[idx]
        val card = selected.card
        when (val r = engine.deployUnit(Side.P1, card, line, selected.cost)) {
            is ActionResult.Ok -> {
                if (idx < engine.p1.hand.size && engine.p1.hand[idx].card.id == card.id)
                    engine.p1.hand.removeAt(idx)
                else engine.p1.hand.removeAll { it.card.id == card.id }
                selectedHandIndex = -1
                say("已部署 ${card.name}")
            }
            is ActionResult.Rejected -> say(r.reason)
        }
    }

    fun playSelectedOrder() {
        val idx = selectedHandIndex
        if (idx !in hand.indices) return
        val card = hand[idx].card
        if (card.type != CardType.ORDER) { say("这不是指令卡"); return }
        applyOrder(card, Side.P1)
        selectedHandIndex = -1
    }

    private fun applyOrder(card: Card, side: Side) {
        val p = engine.player(side)
        val hc = p.hand.firstOrNull { it.card.id == card.id }
        val r = engine.playOrder(side, card, costOverride = hc?.cost ?: card.cost)
        if (r is ActionResult.Ok) {
            val hi = p.hand.indexOfFirst { it.card.id == card.id }
            if (hi >= 0) p.hand.removeAt(hi)
            say("使用了 ${card.name}")
        } else if (r is ActionResult.Rejected) say(r.reason)
    }

    fun playSelectedCounter() {
        val idx = selectedHandIndex
        if (idx !in hand.indices) return
        val card = hand[idx].card
        if (card.type != CardType.COUNTERMEASURE) { say("这不是反制卡"); return }
        when (val r = engine.activateCounter(Side.P1, card)) {
            is ActionResult.Ok -> {
                if (idx in engine.p1.hand.indices) engine.p1.hand.removeAt(idx)
                selectedHandIndex = -1
                say("已激活反制 ${card.name}")
            }
            is ActionResult.Rejected -> say(r.reason)
        }
    }

    fun moveSelectedTo(line: Line) {
        val u = selectedUnit ?: return
        when (val r = engine.moveUnit(Side.P1, u, line)) {
            is ActionResult.Ok -> say("${u.card.name} 移动到${line.cn}")
            is ActionResult.Rejected -> say(r.reason)
        }
    }

    fun attackEnemy(target: UnitInstance) {
        val u = selectedUnit ?: return
        when (val r = engine.attack(Side.P1, u, target)) {
            is ActionResult.Ok -> { selectedUnit = null; say("${u.card.name} 发起攻击") }
            is ActionResult.Rejected -> say(r.reason)
        }
    }

    fun attackEnemyHq() {
        val u = selectedUnit ?: return
        when (val r = engine.attackHq(Side.P1, u)) {
            is ActionResult.Ok -> { selectedUnit = null; say("${u.card.name} 攻击敌方总部") }
            is ActionResult.Rejected -> say(r.reason)
        }
    }

    fun endTurn() {
        if (!isMyTurn) return
        selectedUnit = null
        selectedHandIndex = -1
        if (engine.frontOwner == Side.P1) frontTurns++
        survivedTurns++
        checkObjectiveOnEndTurn()
        if (missionWon) { refresh(); return }
        engine.endTurn()
        refresh()
        if (!engine.finished && !missionWon) botTurn()
    }

    private fun botTurn() {
        val bot = engine.p2
        // 1. 先检查斩杀。
        val lethal = bot.allUnits().sumOf { a ->
            if (engine.canAttack(Side.P2, a, Target.EnemyHq(Side.P1)) is ActionResult.Ok) a.attack else 0
        } >= engine.p1.hq.defense
        if (lethal) {
            bot.allUnits().toList().forEach { a ->
                if (!engine.finished && engine.canAttack(Side.P2, a, Target.EnemyHq(Side.P1)) is ActionResult.Ok)
                    engine.attackHq(Side.P2, a)
            }
        }

        // 2. 使用低费过牌与资源指令，再保留费用给单位。
        var orderGuard = 0
        while (!engine.finished && orderGuard++ < 8) {
            val playable = bot.hand.mapIndexed { i, h -> i to h }
                .filter { it.second.card.type == CardType.ORDER && it.second.cost <= bot.kredits }
                .sortedBy { it.second.cost }
                .firstOrNull() ?: break
            val (index, hc) = playable
            val card = hc.card
            val useful = card.cost == 0 || card.attack > 0 || card.defense > 0 ||
                card.effectText.contains("抽") || card.effectText.contains("指挥点") ||
                card.effectText.contains("伤害") || card.effectText.contains("消灭") ||
                card.effectText.contains("压制") || card.effectText.contains("抑制")
            if (!useful) break
            if (engine.playOrder(Side.P2, card, costOverride = hc.cost) !is ActionResult.Ok) break
            if (index in bot.hand.indices) bot.hand.removeAt(index)
        }

        // 3. 按站场价值部署，优先补充前线。
        var deployGuard = 0
        while (!engine.finished && deployGuard++ < 20) {
            val playable = bot.hand.mapIndexed { i, h -> i to h }
                .filter { it.second.card.type == CardType.UNIT && it.second.cost <= bot.kredits }
                .sortedByDescending { unitValue(it.second.card) }
                .firstOrNull() ?: break
            val (index, hc) = playable
            val card = hc.card
            val preferFront = engine.frontOwner != Side.P1 && bot.frontHasRoom() &&
                card.unitType != UnitType.ARTILLERY
            val line = if (preferFront) Line.FRONT else Line.SUPPORT
            val ok = engine.deployUnit(Side.P2, card, line, hc.cost)
            if (ok is ActionResult.Ok) {
                if (index in bot.hand.indices) bot.hand.removeAt(index)
            } else if (engine.deployUnit(Side.P2, card, Line.SUPPORT, hc.cost) is ActionResult.Ok) {
                if (index in bot.hand.indices) bot.hand.removeAt(index)
            } else break
        }

        // 4. 近战单位争夺空前线（保留攻击费用）。
        bot.support.filter { (it.unitType == UnitType.INFANTRY || it.tankLike) && it.actionCost <= bot.kredits }
            .forEach { if (engine.frontOwner != Side.P1 && bot.frontHasRoom()) engine.moveUnit(Side.P2, it, Line.FRONT) }

        // 5. 价值最高的合法攻击，直到无正收益动作。
        var attacks = 0
        while (!engine.finished && attacks++ < 40) {
            val actions = bot.allUnits().flatMap { attacker ->
                val unitActions = engine.p1.allUnits().map { target ->
                    val canKill = attacker.attack >= target.defense + target.armorValue
                    val survives = attacker.defense > target.attack
                    val value = when {
                        canKill && survives -> 900 + target.attack * 10 + target.defense * 3
                        canKill -> 500 + target.attack * 8
                        target.attack == 0 -> 200 + attacker.attack
                        survives -> 100 + target.defense
                        else -> -150
                    }
                    BotAttack(attacker, Target.EnemyUnit(target), value)
                }
                val hq = BotAttack(attacker, Target.EnemyHq(Side.P1),
                    if (engine.p1.hq.defense <= attacker.attack) 100000 else 350 + attacker.attack * 5)
                (unitActions + hq)
            }.filter { engine.canAttack(Side.P2, it.attacker, it.target) is ActionResult.Ok }
            val best = actions.maxByOrNull { it.score } ?: break
            if (best.score < 0) break
            when (val t = best.target) {
                is Target.EnemyUnit -> engine.attack(Side.P2, best.attacker, t.unit)
                is Target.EnemyHq -> engine.attackHq(Side.P2, best.attacker)
            }
        }

        if (!engine.finished && !missionWon) engine.endTurn()
        refresh()
        checkObjectiveOnBeginTurn()
    }

    private fun unitValue(c: Card): Int =
        c.attack * 3 + c.defense * 2 + c.cost * 2 +
            (if (c.has(Keyword.GUARD)) 4 else 0) +
            (if (c.has(Keyword.BLITZ)) 3 else 0) +
            (if (c.has(Keyword.AMBUSH)) 2 else 0) +
            (if (c.has(Keyword.FURY)) 3 else 0)

    private data class BotAttack(val attacker: UnitInstance, val target: Target, val score: Int)

    fun cardBadge(card: Card): String = buildString {
        append(card.cost).append('K')
        if (card.type == CardType.UNIT) append("  ").append(card.attack).append('/').append(card.defense)
    }

    fun unitLabel(u: UnitInstance): String = buildString {
        append(u.card.name).append("  ").append(u.attack).append('/').append(u.defense)
        if (u.damage > 0) append(" (-").append(u.damage).append(')')
    }

    fun unitTypeName(t: UnitType?): String = t?.cn ?: ""
}