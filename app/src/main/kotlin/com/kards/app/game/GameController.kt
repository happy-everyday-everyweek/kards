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
class GameController(
    private val campaign: Campaign? = null,
    private val customDeck: Deck? = null,
    private val onVictory: () -> Unit = {}
) {
    private val playerNation = customDeck?.mainNation ?: campaign?.nation ?: Nation.GERMANY

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
        val p1Deck = customDeck?.let { Deck(it.mainNation, it.hq, it.cards.shuffled()) } ?: buildDeck(playerNation)
        val botNation = botNationFor(playerNation)
        val p2Deck = buildDeck(botNation)

        engine = Engine(
            Engine.newState(p1Deck, Side.P1),
            Engine.newState(p2Deck, Side.P2)
        )
        engine.start(Side.P1)   // start 内部已经调用了 beginTurn，这里不能再重复调用
        playerIsFirst = true
        selectedHandIndex = -1
        selectedUnit = null
        resultText = null
        toast = null
        refresh()
    }

    private fun botNationFor(player: Nation): Nation =
        campaign?.enemy ?: if (player == Nation.USSR) Nation.GERMANY else Nation.USSR

    /**
     * 用真实卡牌拼一套 40 张卡组：1 张总部 + 39 张。
     * 按费用从低到高铺，保证 Bot 与玩家都有牌可出。
     */
    private fun buildDeck(nation: Nation): Deck {
        // 只用该国的真实卡。按费用升序，保证低费卡够用。
        val pool = CardPool.ofNation(nation)
            .filter { it.type != CardType.HQ }
            .sortedBy { it.cost }
        require(pool.isNotEmpty()) { "${nation.cn} 没有可用卡牌" }

        val chosen = mutableListOf<Card>()
        // 同名上限按「卡名」判定（数据源里同名卡可能有多个条目）
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
        // 极端情况下（可选卡太少）用低费卡补满，保证能开局
        var i = 0
        while (chosen.size < 39) {
            chosen += pool[i % pool.size]
            i++
        }
        return Deck(nation, CardPool.hqFor(nation), chosen.take(39))
    }

    fun refresh() {
        revision++
        if (engine.finished && resultText == null) {
            resultText = when (engine.winner) {
                Side.P1 -> {
                if (campaign != null) onVictory()
                "战役胜利"
            }
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
    val isMyTurn: Boolean get() = engine.active == Side.P1 && !engine.finished

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
                // 引擎只负责战场结算，手牌移除由调用方处理，并从指定位置删掉这一张
                if (idx < engine.p1.hand.size && engine.p1.hand[idx].card.id == card.id) {
                    engine.p1.hand.removeAt(idx)
                } else {
                    engine.p1.hand.removeAll { it.card.id == card.id }
                }
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
        // 只从手牌移除一张同名卡，不能一次删光
        val hi = p.hand.indexOfFirst { it.card.id == card.id }
        if (hi >= 0) p.hand.removeAt(hi)
        p.discardPile += card
        val foe = engine.player(side.opponent)
        com.kards.core.CardEffectRuntime.execute(engine, side, card)
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
        var guard = 0
        // 优先用高费高质量单位铺满费用曲线，而不是无脑从最低费开始出。
        while (guard++ < 30) {
            val playable = bot.hand.mapIndexed { i, h -> i to h.card }
                .filter { it.second.type == CardType.UNIT && it.second.cost <= bot.kredits }
                .sortedWith(compareByDescending<Pair<Int, Card>> {
                    it.second.cost + it.second.attack + it.second.defense
                }.thenBy { it.second.cost })
            if (playable.isEmpty()) break
            val (index, card) = playable.first()
            val line = if (engine.frontOwner != Side.P1 && bot.frontHasRoom() &&
                card.unitType != UnitType.ARTILLERY) Line.FRONT else Line.SUPPORT
            val result = engine.deployUnit(Side.P2, card, line)
            if (result !is ActionResult.Ok &&
                engine.deployUnit(Side.P2, card, Line.SUPPORT) !is ActionResult.Ok) break
            bot.hand.removeAt(index)
            refresh()
        }

        // 支援阵线的近战单位主动争夺空前线，保留行动点用于攻击。
        bot.support.filter { it.unitType == UnitType.INFANTRY || it.tankLike }.forEach { unit ->
            if (engine.frontOwner != Side.P1 && bot.frontHasRoom()) {
                engine.moveUnit(Side.P2, unit, Line.FRONT)
            }
        }

        var attacks = 0
        while (!engine.finished && attacks++ < 30) {
            val action = bot.allUnits().flatMap { attacker ->
                val targets = engine.p1.allUnits().map { target ->
                    val canWin = attacker.attack >= target.defense + target.armorValue
                    val survives = attacker.defense > target.attack || !target.isAlive
                    val score = when {
                        canWin && survives -> 1000 + target.attack * 8 + target.defense
                        canWin -> 450 + target.attack * 6
                        target.attack == 0 -> 150
                        else -> -200 + attacker.attack - target.attack
                    }
                    BotAttack(attacker, Target.EnemyUnit(target), score)
                } + BotAttack(
                    attacker, Target.EnemyHq(Side.P1),
                    if (engine.p1.hq.defense <= attacker.attack) 10000 else 500 + attacker.attack * 3
                )
                targets.filter { engine.canAttack(Side.P2, it.attacker, it.target) is ActionResult.Ok }
            }.maxByOrNull { it.score }
            if (action == null || (action.score < 0 && engine.p1.hq.defense > 6)) break
            when (val target = action.target) {
                is Target.EnemyUnit -> engine.attack(Side.P2, action.attacker, target.unit)
                is Target.EnemyHq -> engine.attackHq(Side.P2, action.attacker)
            }
            refresh()
        }
        if (!engine.finished) engine.endTurn()
        refresh()
    }

    private data class BotAttack(
        val attacker: UnitInstance,
        val target: Target,
        val score: Int
    )

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