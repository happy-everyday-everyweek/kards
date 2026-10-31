package com.kards.core

/** 通用卡面操作执行器。目标未由 UI 指定时使用确定性的合法目标，Bot 与玩家遵循同一结算。 */
object CardEffectRuntime {
    fun execute(engine: Engine, side: Side, source: Card, self: UnitInstance? = null, explicit: UnitInstance? = null, trigger: EffectTrigger? = null) {
        CardEffectParser.parse(source)
            .filter { trigger == null || it.trigger == trigger }
            .forEach { effect -> apply(engine, side, effect, self, explicit) }
        engine.checkWin()
    }

    private fun units(engine: Engine, target: EffectTarget, side: Side, self: UnitInstance?, explicit: UnitInstance?): List<UnitInstance> {
        val me = engine.player(side).allUnits()
        val foe = engine.opponentOf(side).allUnits()
        return when (target) {
            EffectTarget.SELF -> listOfNotNull(self)
            EffectTarget.ENEMY_ONE -> listOfNotNull(explicit?.takeIf { it in foe } ?: foe.firstOrNull())
            EffectTarget.FRIENDLY_ONE -> listOfNotNull(
                explicit?.takeIf { it in me } ?: self ?: me.firstOrNull()
            )
            EffectTarget.TARGET_UNIT -> listOfNotNull(explicit ?: foe.firstOrNull() ?: self)
            EffectTarget.ENEMY_UNITS -> foe.toList()
            EffectTarget.FRIENDLY_UNITS -> me.toList()
            EffectTarget.ALL_UNITS -> me + foe
            else -> emptyList()
        }
    }

    private fun apply(engine: Engine, side: Side, effect: ParsedEffect, self: UnitInstance?, explicit: UnitInstance?) {
        val me = engine.player(side)
        val foe = engine.opponentOf(side)
        val targets = units(engine, effect.target, side, self, explicit)
        when (effect.op) {
            EffectOp.DAMAGE -> {
                if (effect.target == EffectTarget.ENEMY_UNITS) {
                    foe.allUnits().toList().forEach { engine.effectDamage(it, effect.amount) }
                    foe.hq.takeDamage(effect.amount)
                } else targets.forEach { engine.effectDamage(it, effect.amount) }
            }
            EffectOp.DAMAGE_HQ -> {
                val hq = if (effect.target == EffectTarget.FRIENDLY_HQ) me.hq else foe.hq
                hq.takeDamage(effect.amount)
            }
            EffectOp.DESTROY -> targets.forEach { engine.destroy(it) }
            EffectOp.DRAW -> engine.draw(side, effect.amount)
            EffectOp.GAIN_KREDITS -> me.gainKredits(effect.amount)
            EffectOp.GAIN_SLOTS -> me.gainSlots(effect.amount)
            EffectOp.LOSE_SLOTS -> {
                val player = if (effect.target == EffectTarget.ENEMY_HQ) foe else me
                player.slots = (player.slots - effect.amount).coerceAtLeast(0)
            }
            EffectOp.BUFF_ATTACK -> targets.forEach {
                if (effect.trigger == EffectTrigger.PASSIVE) it.passiveAttackBonus += effect.amount
                else it.attackBonus += effect.amount
            }
            EffectOp.BUFF_DEFENSE -> targets.forEach {
                if (effect.trigger == EffectTrigger.PASSIVE) it.passiveDefenseBonus += effect.amount
                else it.maxDefenseBonus += effect.amount
            }
            EffectOp.PIN -> targets.forEach { engine.pin(it) }
            EffectOp.SUPPRESS -> targets.forEach { engine.suppress(it) }
            EffectOp.REPAIR -> targets.forEach { engine.repair(it) }
            EffectOp.RETREAT -> targets.forEach { engine.retreat(it) }
            EffectOp.TAKE_CONTROL -> targets.forEach { engine.takeControl(it, side) }
            EffectOp.DISCARD -> repeat(effect.amount) {
                val card = me.hand.randomOrNull() ?: return@repeat
                me.hand.remove(card)
                me.discardPile += card.card
            }
            EffectOp.ENEMY_DISCARD -> repeat(effect.amount) {
                val card = foe.hand.randomOrNull() ?: return@repeat
                foe.hand.remove(card)
                foe.discardPile += card.card
            }
            EffectOp.INTEL -> engine.intel(side, effect.amount)
            EffectOp.DEVELOP -> engine.develop(side, CardPool.all)
            EffectOp.ADD_COPY -> self?.let {
                if (me.handHasRoom()) me.hand += HandCard(
                    it.card.copy(cost = if ("花费为 0" in effect.source || "花费为0" in effect.source) 0 else it.card.cost)
                )
            }
            EffectOp.ADD_CARD -> {
                val card = CardPool.all.firstOrNull { it.name == effect.detail }
                if (card != null && me.handHasRoom()) {
                    me.hand += HandCard(card, costModifier = if ("花费为 0" in effect.source || "花费为0" in effect.source) -card.cost else 0)
                }
            }
            EffectOp.SHUFFLE_DECK -> { val shuffled = me.drawPile.shuffled(); me.drawPile.clear(); me.drawPile += shuffled }
            EffectOp.SET_ATTACK_TO_DEFENSE -> targets.forEach { it.attackBonus = it.defense - it.card.attack }
            EffectOp.HEAL_HQ -> engine.healHq(side, effect.amount)
            EffectOp.REDUCE_COST, EffectOp.INCREASE_COST -> {
                val delta = if (effect.op == EffectOp.REDUCE_COST) -effect.amount else effect.amount
                me.hand.firstOrNull()?.let { it.costModifier += delta }
            }
            EffectOp.ADD_COPY, EffectOp.ADD_CARD -> self?.let { if (me.handHasRoom()) me.hand += HandCard(it.card) }
            EffectOp.COUNTER_DEPLOY -> explicit?.let { engine.destroy(it) }
            EffectOp.SET_ATTACK -> targets.forEach { it.attackBonus = effect.amount - it.card.attack - it.passiveAttackBonus }
            EffectOp.RETURN_HAND -> {
                if ("放回卡组顶" in effect.source) {
                    val card = foe.hand.removeFirstOrNull()
                    if (card != null) foe.drawPile.add(0, card.card)
                } else {
                    val target = explicit ?: targets.firstOrNull()
                    if (target != null) engine.returnToHand(target)
                }
            }
            EffectOp.REVEAL -> targets.forEach { it.covertRevealed = true }
            EffectOp.COPY_DEAD_THIS_TURN -> me.destroyedThisTurn.forEach {
                if (me.handHasRoom()) me.hand += HandCard(it)
            }
            EffectOp.PREVENT_DEPLOY -> Unit
        }
    }
}