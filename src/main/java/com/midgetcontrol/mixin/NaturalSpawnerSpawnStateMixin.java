package com.midgetcontrol.mixin;

import com.midgetcontrol.MidgetControl;
import com.midgetcontrol.config.MidgetControlConfig;
import com.midgetcontrol.spawn.SpawnCapCalculator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.SpawnState.class)
public abstract class NaturalSpawnerSpawnStateMixin {
    @Inject(method = "canSpawnForCategoryGlobal", at = @At("RETURN"), cancellable = true)
    private void midgetcontrol$applyCategoryCap(MobCategory category, CallbackInfoReturnable<Boolean> callback) {
        blockAtConfiguredLimit(category, callback);
    }

    @Inject(method = "canSpawn", at = @At("RETURN"), cancellable = true)
    private void midgetcontrol$recheckCapForPack(
            EntityType<?> entityType,
            BlockPos position,
            ChunkAccess chunk,
            CallbackInfoReturnable<Boolean> callback
    ) {
        blockAtConfiguredLimit(entityType.getCategory(), callback);
    }

    private void blockAtConfiguredLimit(MobCategory category, CallbackInfoReturnable<Boolean> callback) {
        if (!callback.getReturnValue()) {
            return;
        }

        MidgetControlConfig config = MidgetControl.config();
        if (!config.naturalSpawningEnabled()) {
            return;
        }

        int capPercent = capPercent(config, category);
        if (capPercent >= 100) {
            return;
        }

        NaturalSpawner.SpawnState state = (NaturalSpawner.SpawnState) (Object) this;
        int diameter = NaturalSpawner.SPAWN_DISTANCE_CHUNK * 2 + 1;
        int configuredCap = SpawnCapCalculator.scaledCap(
                category.getMaxInstancesPerChunk(),
                state.getSpawnableChunkCount(),
                capPercent,
                diameter
        );

        if (state.getMobCategoryCounts().getInt(category) >= configuredCap) {
            callback.setReturnValue(false);
        }
    }

    private static int capPercent(MidgetControlConfig config, MobCategory category) {
        if (category == MobCategory.MONSTER) {
            return config.monsterCapPercent();
        }
        if (category == MobCategory.CREATURE) {
            return config.creatureCapPercent();
        }
        if (category == MobCategory.AMBIENT) {
            return config.ambientCapPercent();
        }
        if (category == MobCategory.AXOLOTLS) {
            return config.axolotlCapPercent();
        }
        if (category == MobCategory.UNDERGROUND_WATER_CREATURE) {
            return config.undergroundWaterCreatureCapPercent();
        }
        if (category == MobCategory.WATER_CREATURE) {
            return config.waterCreatureCapPercent();
        }
        if (category == MobCategory.WATER_AMBIENT) {
            return config.waterAmbientCapPercent();
        }
        return 100;
    }
}

