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
import io.github.gunpowder.configs.SleepConfig
import io.github.gunpowder.mixin.cast.SleepSetter
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.MessageType
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.stat.Stats
import net.minecraft.text.LiteralText
import net.minecraft.util.Util
import java.util.function.Consumer
import kotlin.math.roundToInt
import kotlin.streams.toList

class GunpowderSleepVoteModule : GunpowderModule {
    override val name = "sleepvote"
    override val toggleable = true
    private val gunpowder: GunpowderMod
        get() = GunpowderMod.instance
    private val treshold: Double
            get() = gunpowder.registry.getConfig(SleepConfig::class.java).sleepPercentage

    private val sleeping = mutableListOf<ServerPlayerEntity>()

    override fun registerConfigs() {
        gunpowder.registry.registerConfig("gunpowder-sleepvote.yaml", SleepConfig::class.java, "gunpowder-sleepvote.yaml")
    }

    override fun registerEvents() {
        ServerTickEvents.START_WORLD_TICK.register(ServerTickEvents.StartWorldTick { world ->
            if (world.isClient || !world.dimension.isBedWorking) {
                return@StartWorldTick
            }

            val players = world.players

            players.removeIf { obj: PlayerEntity -> obj.isSpectator }

            if (players.isEmpty() || treshold <= 0) {
                (world as SleepSetter).setSleeping(false)
                return@StartWorldTick
            }

            val total = players.size.toDouble()
            val sleepingPlayers = players.stream().filter { it.isSleepingLongEnough }.toList()

            val sleepingAmount = sleepingPlayers.count().toDouble()
            val percentage = sleepingAmount / total
            val shouldSkip = percentage >= treshold

            sleepingPlayers.filter { !sleeping.contains(it) }.forEach {
                sleeping.add(it as ServerPlayerEntity)
                world.server.playerManager.broadcastChatMessage(
                        LiteralText("${it.displayName.asString()} is now sleeping. (${(percentage * 100).roundToInt()}%, ${(treshold * 100).roundToInt()}% needed)"), MessageType.SYSTEM, Util.NIL_UUID)
            }

            (world as SleepSetter).setSleeping(shouldSkip)
            if (shouldSkip) {
                players.forEach { p -> p.statHandler.setStat(p, Stats.CUSTOM.getOrCreateStat(Stats.TIME_SINCE_REST), 0) }
                world.server.playerManager.broadcastChatMessage(LiteralText("Good morning!"), MessageType.SYSTEM, Util.NIL_UUID)
                sleeping.clear()
            }
        })
    }
}