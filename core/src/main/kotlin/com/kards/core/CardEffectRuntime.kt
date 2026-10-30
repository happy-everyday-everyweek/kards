package com.kards.core

/** 通用卡面操作执行器。目标未由 UI 指定时使用确定性的合法目标，Bot 与玩家遵循同一结算。 */
object CardEffectRuntime {
    fun execute(engine: Engine, side: Side, source: Card, self: UnitInstance? = null, explicit: UnitInstance? = null) {
        CardEffectParser.parse(source).forEach { effect -> apply(engine, side, effect, self, explicit) }
        engine.checkWin()
    }

    private fun units(engine: Engine, target: EffectTarget, side: Side, self: UnitInstance?, explicit: UnitInstance?): List<UnitInstance> = when (target) {
        EffectTarget.SELF -> listOfNotNull(self)
        EffectTarget.ENEMY_UNITS -> engine.opponentOf(side).allUnits().toList()
        EffectTarget.FRIENDLY_UNITS -> engine.player(side).allUnits().toList()
        EffectTarget.ALL_UNITS -> engine.p1.allUnits() + engine.p2.allUnits()
        else -> listOfNotNull(explicit ?: engine.opponentOf(side).allUnits().firstOrNull() ?: self)
    }

    private fun apply(engine: Engine, side: Side, effect: ParsedEffect, self: UnitInstance?, explicit: UnitInstance?) {
        val me = engine.player(side)
        val foe = engine.opponentOf(side)
        val targets = units(engine, effect.target, side, self, explicit)
        when (effect.op) {
            EffectOp.DAMAGE -> targets.forEach { engine.effectDamage(it, effect.amount) }
            EffectOp.DAMAGE_HQ -> {
                val hq = if (effect.target == EffectTarget.FRIENDLY_HQ) me.hq else foe.hq
                hq.takeDamage(effect.amount)
            }
            EffectOp.DESTROY -> targets.forEach { engine.destroy(it) }
            EffectOp.DRAW -> engine.draw(side, effect.amount)
            EffectOp.GAIN_KREDITS -> me.gainKredits(effect.amount)
            EffectOp.GAIN_SLOTS -> me.gainSlots(effect.amount)
            EffectOp.LOSE_SLOTS -> me.slots = (me.slots - effect.amount).coerceAtLeast(0)
            EffectOp.BUFF_ATTACK -> targets.forEach { it.attackBonus += effect.amount }
            EffectOp.BUFF_DEFENSE -> targets.forEach { it.maxDefenseBonus += effect.amount }
            EffectOp.PIN -> targets.forEach { engine.pin(it) }
            EffectOp.SUPPRESS -> targets.forEach { engine.suppress(it) }
            EffectOp.REPAIR -> targets.forEach { engine.repair(it) }
            EffectOp.RETREAT -> targets.forEach { engine.retreat(it) }
            EffectOp.TAKE_CONTROL -> targets.forEach { engine.takeControl(it, side) }
            EffectOp.DISCARD -> repeat(effect.amount) { me.hand.removeFirstOrNull()?.let { me.discardPile += it.card } }
            EffectOp.SHUFFLE_DECK -> { val shuffled = me.drawPile.shuffled(); me.drawPile.clear(); me.drawPile += shuffled }
            EffectOp.SET_ATTACK_TO_DEFENSE -> targets.forEach { it.attackBonus = it.defense - it.card.attack }
            EffectOp.HEAL_HQ -> me.hq.damage = (me.hq.damage - effect.amount).coerceAtLeast(0)
            EffectOp.REDUCE_COST, EffectOp.INCREASE_COST -> {
                val delta = if (effect.op == EffectOp.REDUCE_COST) -effect.amount else effect.amount
                me.hand.firstOrNull()?.let { it.costModifier += delta }
            }
            EffectOp.ADD_COPY, EffectOp.ADD_CARD -> self?.let { if (me.handHasRoom()) me.hand += HandCard(it.card) }
            EffectOp.COUNTER_DEPLOY -> explicit?.let { engine.destroy(it) }
            EffectOp.PREVENT_DEPLOY -> Unit
        }
    }
}