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

package io.github.gunpowder.mixin.sleepvote;

import io.github.gunpowder.events.WorldPreSleepCallback;
import io.github.gunpowder.mixin.cast.SleepSetter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin_SleepVote extends World implements SleepSetter {
    @Shadow
    private boolean allPlayersSleeping;

    @Shadow public abstract void setTimeOfDay(long l);

    @Shadow protected abstract void wakeSleepingPlayers();

    @Shadow protected abstract void resetWeather();

    @Shadow @Final private List<ServerPlayerEntity> players;

    protected ServerWorldMixin_SleepVote(MutableWorldProperties properties, RegistryKey<World> registryKey, DimensionType dimensionType, Supplier<Profiler> supplier, boolean bl, boolean bl2, long l) {
        super(properties, registryKey, dimensionType, supplier, bl, bl2, l);
    }

    @Override
    public void setSleeping(boolean sleeping) {
        allPlayersSleeping = sleeping;

        if (sleeping) {
            if (getGameRules().getBoolean(GameRules.DO_DAYLIGHT_CYCLE)) {
                long l = properties.getTimeOfDay() + 24000L;
                setTimeOfDay(l - l % 24000L);
                WorldPreSleepCallback.EVENT.invoker().trigger((ServerWorld)(Object)this, this.players.stream().filter(LivingEntity::isSleeping).collect(Collectors.toList()));
            }

            wakeSleepingPlayers();
            if (getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
                resetWeather();
            }
        }
    }
}
