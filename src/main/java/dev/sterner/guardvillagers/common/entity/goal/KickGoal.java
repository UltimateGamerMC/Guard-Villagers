package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;

public class KickGoal extends Goal {

    public final GuardEntity guard;

    public KickGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        return guard.getTarget() != null
            && guard.distanceTo(guard.getTarget()) <= 2.5D
            && guard.getMainHandItem().is(ItemTags.SWORDS)
            && !guard.isBlocking()
            && guard.kickCoolDown == 0;
    }

    @Override
    public void start() {
        guard.setKicking(true);
        if (guard.kickTicks <= 0) {
            guard.kickTicks = 10;
        }
        LivingEntity target = guard.getTarget();
        if (target != null && guard.level() instanceof ServerLevel sw) {
            guard.doHurtTarget(sw, target);
        }
    }

    @Override
    public void stop() {
        guard.setKicking(false);
        guard.kickCoolDown = 50;
    }
}
