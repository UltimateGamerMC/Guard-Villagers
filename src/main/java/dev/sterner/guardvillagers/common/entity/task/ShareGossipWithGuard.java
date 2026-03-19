package dev.sterner.guardvillagers.common.entity.task;

import com.google.common.collect.ImmutableMap;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.entity.ai.brain.MemoryModuleState;
import net.minecraft.entity.ai.brain.MemoryModuleType;
import net.minecraft.entity.ai.brain.task.MultiTickTask;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.world.ServerWorld;

public class ShareGossipWithGuard extends MultiTickTask<VillagerEntity> {
    public ShareGossipWithGuard() {
        super(ImmutableMap.of(MemoryModuleType.INTERACTION_TARGET, MemoryModuleState.VALUE_PRESENT, MemoryModuleType.VISIBLE_MOBS, MemoryModuleState.VALUE_PRESENT));
    }

    @Override
    protected boolean shouldRun(ServerWorld serverWorld, VillagerEntity villagerEntity) {
        return villagerEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET)
                .filter(e -> e.getType() == GuardVillagers.GUARD_VILLAGER).isPresent();
    }

    @Override
    protected boolean shouldKeepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        return this.shouldRun(serverWorld, villagerEntity);
    }

    @Override
    protected void run(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        GuardEntity guard = (GuardEntity) villagerEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();
        villagerEntity.lookAtEntity(guard, 30.0F, 30.0F);
        guard.lookAtEntity(villagerEntity, 30.0F, 30.0F);
        villagerEntity.getNavigation().startMovingTo(guard, 0.5);
        guard.getNavigation().startMovingTo(villagerEntity, 0.5);
    }

    @Override
    protected void keepRunning(ServerWorld serverWorld, VillagerEntity villagerEntity, long time) {
        GuardEntity guard = (GuardEntity) villagerEntity.getBrain().getOptionalRegisteredMemory(MemoryModuleType.INTERACTION_TARGET).get();
        if (villagerEntity.squaredDistanceTo(guard) < 5.0D) {
            villagerEntity.lookAtEntity(guard, 30.0F, 30.0F);
            guard.lookAtEntity(villagerEntity, 30.0F, 30.0F);
            villagerEntity.getNavigation().startMovingTo(guard, 0.5);
            guard.getNavigation().startMovingTo(villagerEntity, 0.5);
            guard.gossip(villagerEntity, time);
        }
    }

    @Override
    protected void finishRunning(ServerWorld pLevel, VillagerEntity villagerEntity, long time) {
        villagerEntity.getBrain().forget(MemoryModuleType.INTERACTION_TARGET);
    }
}