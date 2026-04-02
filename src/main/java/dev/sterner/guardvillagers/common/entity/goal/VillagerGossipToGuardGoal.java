package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.entity.npc.villager.Villager;

import java.util.EnumSet;
import java.util.Optional;

public class VillagerGossipToGuardGoal extends Goal {
    protected final Villager villager;
    protected GuardEntity guard;

    public VillagerGossipToGuardGoal(Villager villager) {
        this.villager = villager;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        Optional<LivingEntity> opt = this.villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET);
        if (opt.isPresent() && opt.get() instanceof GuardEntity g) {
            this.guard = g;
            long gameTime = guard.level().getGameTime();
            if (!nearbyVillagersInteractingWithGuards() && (gameTime < this.guard.lastGossipTime || gameTime >= this.guard.lastGossipTime + 1200L))
                return this.guard.getTarget() == null && this.villager.level().isDarkOutside();
        }
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        return !nearbyVillagersInteractingWithGuards()
            && guard.getTarget() == null
            && this.villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).filter(e -> e.equals(guard)).isPresent();
    }

    @Override
    public void start() {
        this.villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, guard);
    }

    @Override
    public void tick() {
        this.villager.getBrain().setMemory(MemoryModuleType.INTERACTION_TARGET, guard);
        if (!nearbyVillagersInteractingWithGuards()
            && this.villager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET).filter(e -> e.equals(guard)).isPresent()) {
            villager.lookAt(guard, 30.0F, 30.0F);
            villager.getLookControl().setLookAt(guard, 30.0F, 30.0F);
            if (this.villager.distanceTo(guard) > 2.0D) {
                this.villager.getNavigation().moveTo(guard, 0.5D);
            } else {
                this.villager.getNavigation().stop();
                guard.gossip(villager, villager.level().getGameTime());
            }
        }
    }

    @Override
    public void stop() {
        this.villager.getBrain().eraseMemory(MemoryModuleType.INTERACTION_TARGET);
    }

    private boolean nearbyVillagersInteractingWithGuards() {
        Optional<NearestVisibleLivingEntities> opt = villager.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
        if (opt.isEmpty()) {
            return false;
        }
        for (LivingEntity entity : opt.get().nearbyEntities()) {
            if (entity instanceof Villager nearbyVillager) {
                Optional<LivingEntity> it = nearbyVillager.getBrain().getMemory(MemoryModuleType.INTERACTION_TARGET);
                if (it.isPresent() && it.get().equals(guard))
                    return true;
            }
        }
        return false;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }
}
