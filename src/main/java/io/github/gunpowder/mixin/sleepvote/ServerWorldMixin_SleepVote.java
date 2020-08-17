package io.github.gunpowder.mixin.sleepvote;

import io.github.gunpowder.mixin.cast.SleepSetter;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.GameRules;
import net.minecraft.world.MutableWorldProperties;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.function.Supplier;

@Mixin(ServerWorld.class)
public abstract class ServerWorldMixin_SleepVote extends World implements SleepSetter {
    @Shadow
    private boolean allPlayersSleeping;

    @Shadow public abstract void setTimeOfDay(long l);

    @Shadow protected abstract void wakeSleepingPlayers();

    @Shadow protected abstract void resetWeather();

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
            }

            wakeSleepingPlayers();
            if (getGameRules().getBoolean(GameRules.DO_WEATHER_CYCLE)) {
                resetWeather();
            }
        }
    }
}