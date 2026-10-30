package com.kards.core

/**
 * 标注一个测试用例覆盖了哪些 Wiki 规则条目。
 * WikiRuleCoverageTest 会扫描全部测试类，保证每条规则被两个或以上的测试用例引用。
 *
 * 注意：本包存在业务类型 Target，因此这里不给注解加 @Target 限定，
 * 否则会与业务 Target 名称冲突。
 */
@Retention(AnnotationRetention.RUNTIME)
annotation class WikiRule(vararg val value: RuleId)

/**
 * 共用的对局搭建器。不放在测试文件内部，供单元测试与集成测试共享。
 */
object Fixture {
    fun deckOf(nation: Nation, filler: Card = CardPool.INF_89, size: Int = 39): Deck =
        Deck(nation, CardPool.hqFor(nation), List(size) { filler })

    /**
     * 按每张卡自己的同名上限（普通 4 / 限定 3 / 特殊 2 / 精英 1）依次铺卡，
     * 直到凑满 size 张。真实稀有度会限制可携带张数，因此不能用「一律 4 张」硬拼。
     */
    fun fillByCopyLimit(candidates: List<Card>, size: Int = 39): List<Card> {
        val out = mutableListOf<Card>()
        for (card in candidates) {
            repeat(card.rarity.maxCopies) { out += card }
            if (out.size >= size) break
        }
        return out.take(size)
    }

    fun state(side: Side, nation: Nation = Nation.GERMANY): PlayerState =
        Engine.newState(deckOf(nation), side)

    fun engine(
        p1Nation: Nation = Nation.GERMANY,
        p2Nation: Nation = Nation.USA
    ): Engine = Engine(state(Side.P1, p1Nation), state(Side.P2, p2Nation))

    /** 开始对局，并把双方指挥点补到指定值，便于构造场景。 */
    fun started(kredits: Int = 10): Engine {
        val e = engine()
        e.start(Side.P1)
        e.p1.kredits = kredits
        e.p2.kredits = kredits
        return e
    }

    /**
     * 直接把单位摆到场上，用于构造「已经存在若干回合」的场面。
     * 默认把它当作在本回合之前入场，避免闪击规则干扰战斗断言。
     */
    fun place(
        engine: Engine,
        side: Side,
        card: Card,
        line: Line = Line.SUPPORT,
        enteredThisTurn: Boolean = false
    ): UnitInstance {
        val u = UnitInstance(card, side)
        u.line = line
        u.onEnterBoard(if (enteredThisTurn) engine.turnNumber else -1)
        engine.player(side).unitsOn(line) += u
        if (line == Line.FRONT) engine.frontOwner = side
        return u
    }

    /** 把「本回合已入场」的影响排除，专注于战斗规则。 */
    fun freshTurn(engine: Engine) {
        engine.turnNumber = 99
    }
}