/*
 * MIT License
 *
 * Copyright (c) 2020 GunpowderMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package io.github.gunpowder

import io.github.gunpowder.api.GunpowderMod
import io.github.gunpowder.api.GunpowderModule
import io.github.gunpowder.mixin.cast.SleepSetter
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.text.LiteralText
import net.minecraft.text.TranslatableText
import net.minecraft.world.GameRules
import kotlin.math.ceil
import kotlin.math.roundToInt

class GunpowderSleepVoteModule : GunpowderModule {
    override val name = "sleepvote"
    override val toggleable = true
    private val gunpowder: GunpowderMod
        get() = GunpowderMod.instance

    override fun registerEvents() {
        ServerTickEvents.START_WORLD_TICK.register(ServerTickEvents.StartWorldTick { world ->
            val sleeping = mutableListOf<ServerPlayerEntity>()

            if (world.isClient || !world.dimension.isBedWorking) {
                return@StartWorldTick
            }

            val players = world.players

            players.removeIf { obj: PlayerEntity -> obj.isSpectator }

            val sleepPercentage = world.gameRules.getInt(GameRules.PLAYERS_SLEEPING_PERCENTAGE) / 100.0

            if (players.isEmpty() || sleepPercentage <= 0) {
                (world as SleepSetter).setSleeping(false)
                return@StartWorldTick
            }

            val total = players.size.toDouble()
            val sleepingPlayers = players.stream().filter { it.isSleepingLongEnough }.toList()

            val sleepingAmount = sleepingPlayers.count().toDouble()
            val percentage = sleepingAmount / total
            val shouldSkip = percentage >= sleepPercentage

            sleepingPlayers.filter { !sleeping.contains(it) }.forEach {
                sleeping.add(it as ServerPlayerEntity)

                val text = if (shouldSkip) {
                    TranslatableText("sleep.skipping_night")
                } else {
                    TranslatableText(
                        "sleep.players_sleeping",
                        sleepingAmount, ceil(total * sleepPercentage).roundToInt()
                    )
                }

                world.players.forEach { p ->
                    p.sendMessage(text, false)
                }
            }

            (world as SleepSetter).setSleeping(shouldSkip)
            if (shouldSkip) {
                world.players.forEach { p -> p.statHandler.setStat(p, Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST), 0) }
                world.players.forEach { p -> p.sendMessage(LiteralText("Good morning!"), false) }
                sleeping.clear()
            }
        })
    }
}