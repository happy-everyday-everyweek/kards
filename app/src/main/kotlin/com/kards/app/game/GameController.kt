package com.kards.app.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.kards.core.ActionResult
import com.kards.core.Card
import com.kards.core.CardPool
import com.kards.core.CardType
import com.kards.core.Deck
import com.kards.core.EffectTable
import com.kards.core.Engine
import com.kards.core.HandCard
import com.kards.core.Line
import com.kards.core.Nation
import com.kards.core.OrderEffect
import com.kards.core.Side
import com.kards.core.Target
import com.kards.core.UnitInstance
import com.kards.core.UnitType

/**
 * 一局人机对战的完整状态机。
 *
 * 规则骨架全部来自 core 里的 [Engine]（同一套代码被单元测试覆盖），
 * 这里只负责：把 UI 操作翻译成引擎调用、驱动 Bot 出牌、维护 Compose 可观察状态。
 */
class GameController(private val playerNation: Nation = Nation.GERMANY) {

    /** 玩家选中的手牌（用于部署/使用）。 */
    var selectedHandIndex by mutableStateOf(-1)
        private set

    /** 玩家选中的自己已上场单位（用于移动或攻击）。 */
    var selectedUnit by mutableStateOf<UnitInstance?>(null)
        private set

    /** 对局结束提示。 */
    var resultText by mutableStateOf<String?>(null)
        private set

    /** 给 UI 看的短提示。 */
    var toast by mutableStateOf<String?>(null)
        private set

    lateinit var engine: Engine
        private set

    var playerIsFirst by mutableStateOf(true)
        private set

    /** 触发界面的重绘计数，引擎内部状态是可变对象，靠它通知 Compose。 */
    var revision by mutableStateOf(0)
        private set

    init {
        newGame()
    }

    fun newGame() {
        val p1Deck = buildDeck(playerNation)
        val botNation = botNationFor(playerNation)
        val p2Deck = buildDeck(botNation)

        engine = Engine(
            Engine.newState(p1Deck, Side.P1),
            Engine.newState(p2Deck, Side.P2)
        )
        engine.start(Side.P1)
        playerIsFirst = true
        selectedHandIndex = -1
        selectedUnit = null
        resultText = null
        toast = null
        engine.beginTurn(Side.P1)
        refresh()
    }

    private fun botNationFor(player: Nation): Nation =
        if (player == Nation.USSR) Nation.GERMANY else Nation.USSR

    /**
     * 用真实卡牌拼一套 40 张卡组：1 张总部 + 39 张。
     * 按费用从低到高铺，保证 Bot 与玩家都有牌可出。
     */
    private fun buildDeck(nation: Nation): Deck {
        val pool = CardPool.ofNation(nation)
            .filter { it.type != CardType.HQ && it.cost <= 6 }
            .sortedBy { it.cost }
        val chosen = mutableListOf<Card>()
        var i = 0
        while (chosen.size < 39 && pool.isNotEmpty()) {
            val c = pool[i % pool.size]
            // 同名上限：用稀有度自带的 maxCopies 限制
            if (chosen.count { it.id == c.id } < c.rarity.maxCopies) chosen += c
            i++
            if (i > 5000) break
        }
        while (chosen.size < 39 && pool.isNotEmpty()) {
            chosen += pool[chosen.size % pool.size]
        }
        return Deck(nation, CardPool.hqFor(nation), chosen.take(39))
    }

    fun refresh() {
        revision++
        if (engine.finished && resultText == null) {
            resultText = when (engine.winner) {
                Side.P1 -> "你赢了"
                Side.P2 -> "Bot 获胜"
                else -> "对局结束"
            }
        }
    }

    private fun say(msg: String) {
        toast = msg
        refresh()
    }

    fun clearToast() {
        toast = null
    }

    val hand: List<HandCard> get() = engine.p1.hand
    val mySupport: List<UnitInstance> get() = engine.p1.support
    val myFront: List<UnitInstance> get() = engine.p1.front
    val enemySupport: List<UnitInstance> get() = engine.p2.support
    val enemyFront: List<UnitInstance> get() = engine.p2.front
    val myHqDefense: Int get() = engine.p1.hq.defense
    val enemyHqDefense: Int get() = engine.p2.hq.defense
    val myKredits: Int get() = engine.p1.kredits
    val mySlots: Int get() = engine.p1.slots
    val isMyTurn: Boolean get() = engine.currentSide == Side.P1 && !engine.finished

    // —————————————————— 玩家操作 ——————————————————

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

    /** 点击手牌后，选择部署到某条阵线。 */
    fun deploySelected(line: Line) {
        val idx = selectedHandIndex
        if (idx !in hand.indices) return
        val card = hand[idx].card
        val r = engine.deployUnit(Side.P1, card, line)
        when (r) {
            is ActionResult.Ok -> {
                selectedHandIndex = -1
                say("已部署 ${card.name}")
            }
            is ActionResult.Rejected -> say(r.reason)
        }
    }

    /** 支付指挥点使用指令。 */
    fun playSelectedOrder() {
        val idx = selectedHandIndex
        if (idx !in hand.indices) return
        val card = hand[idx].card
        if (card.type != CardType.ORDER) {
            say("这不是指令卡")
            return
        }
        applyOrder(card, Side.P1)
        selectedHandIndex = -1
    }

    private fun applyOrder(card: Card, side: Side) {
        val p = engine.player(side)
        if (!p.canPay(card.cost)) {
            say("指挥点不足（需要 ${card.cost}）")
            return
        }
        p.pay(card.cost)
        p.hand.removeAll { it.card.id == card.id }
        p.discardPile += card
        val foe = engine.player(side.opponent)
        when (EffectTable.effectOf(card)) {
            OrderEffect.DAMAGE_ONE -> foe.allUnits().firstOrNull()?.let { engine.effectDamage(it, 2) }
            OrderEffect.DAMAGE_ALL -> foe.allUnits().forEach { engine.effectDamage(it, 2) }
            OrderEffect.DRAW -> engine.draw(side, 2)
            OrderEffect.GAIN_KREDIT -> p.gainKredits(3)
            OrderEffect.GAIN_SLOT -> p.gainSlots(1)
            OrderEffect.LOSE_SLOT_ENEMY -> foe.slots = (foe.slots - 1).coerceAtLeast(1)
            OrderEffect.SUPPRESS_ONE -> foe.allUnits().firstOrNull()?.let { engine.suppress(it) }
            OrderEffect.PIN_ALL -> foe.allUnits().forEach { engine.pin(it) }
            OrderEffect.REPAIR_ALL -> p.allUnits().forEach { engine.repair(it) }
            OrderEffect.BUFF_ALL -> p.allUnits().forEach { it.attack += 1; it.defense += 1 }
            OrderEffect.HEAL_HQ -> p.hq.defense += 3
            OrderEffect.RETREAT_ONE -> foe.allUnits().firstOrNull()?.let { engine.retreat(it) }
            else -> Unit // 其余效果在 Demo 里按无附加处理
        }
        say("使用了 ${card.name}")
    }

    /** 移动选中的单位到另一条阵线。 */
    fun moveSelectedTo(line: Line) {
        val u = selectedUnit ?: return
        when (val r = engine.moveUnit(Side.P1, u, line)) {
            is ActionResult.Ok -> say("${u.card.name} 移动到${line.cn}")
            is ActionResult.Rejected -> say(r.reason)
        }
    }

    /** 选中的单位攻击敌方某个单位。 */
    fun attackEnemy(target: UnitInstance) {
        val u = selectedUnit ?: return
        when (val r = engine.attack(Side.P1, u, target)) {
            is ActionResult.Ok -> {
                selectedUnit = null
                say("${u.card.name} 发起攻击")
            }
            is ActionResult.Rejected -> say(r.reason)
        }
    }

    /** 选中的单位攻击敌方总部。 */
    fun attackEnemyHq() {
        val u = selectedUnit ?: return
        when (val r = engine.attackHq(Side.P1, u)) {
            is ActionResult.Ok -> {
                selectedUnit = null
                say("${u.card.name} 攻击敌方总部")
            }
            is ActionResult.Rejected -> say(r.reason)
        }
    }

    /** 结束回合，然后让 Bot 行动。 */
    fun endTurn() {
        if (!isMyTurn) return
        selectedUnit = null
        selectedHandIndex = -1
        engine.endTurn()
        refresh()
        if (!engine.finished) botTurn()
    }

    // —————————————————— Bot ——————————————————

    private fun botTurn() {
        val bot = engine.p2
        // 简单策略：能出的牌都出，能攻击就攻击
        var guard = 0
        while (guard++ < 40) {
            val playable = bot.hand
                .mapIndexed { i, h -> i to h.card }
                .filter { it.second.cost <= bot.kredits }
                .sortedBy { it.second.cost }
            if (playable.isEmpty()) break
            val (idx, card) = playable.first()
            if (card.type == CardType.UNIT) {
                val line = if (bot.frontHasRoom() && bot.front.size < 2) Line.FRONT else Line.SUPPORT
                if (engine.deployUnit(Side.P2, card, line) !is ActionResult.Ok) {
                    if (engine.deployUnit(Side.P2, card, Line.SUPPORT) !is ActionResult.Ok) break
                }
                bot.hand.removeAt(idx)
            } else {
                applyOrder(card, Side.P2)
            }
            refresh()
        }
        // Bot 进攻
        bot.allUnits().forEach { u ->
            val targets = engine.p1.allUnits()
            if (targets.isNotEmpty()) {
                val t = targets.minByOrNull { it.defense }
                if (t != null) engine.attack(Side.P2, u, t)
            } else {
                engine.attackHq(Side.P2, u)
            }
            refresh()
        }
        if (engine.finished) return
        engine.endTurn()
        engine.beginTurn(Side.P1)
        refresh()
    }

    // —————————————————— 供 UI 显示 ——————————————————

    fun cardBadge(card: Card): String = buildString {
        append(card.cost).append('K')
        if (card.type == CardType.UNIT) {
            append("  ").append(card.attack).append('/').append(card.defense)
        }
    }

    fun unitLabel(u: UnitInstance): String = buildString {
        append(u.card.name)
        append("  ").append(u.attack).append('/').append(u.defense)
        if (u.damage > 0) append(" (-").append(u.damage).append(')')
    }

    fun unitTypeName(t: UnitType?): String = t?.cn ?: ""
}