package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;

public class GuardRunToEatGoal extends RandomStrollGoal {
    private final GuardEntity guard;
    private int walkTimer;

    public GuardRunToEatGoal(GuardEntity guard) {
        super(guard, 1.0D, 1, false);
        this.guard = guard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.TARGET, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!(guard.getHealth() < (guard.getMaxHealth() / 2)
            && GuardEatFoodGoal.isConsumable(guard.getOffhandItem())
            && !guard.isEating()
            && guard.getTarget() != null)) {
            return false;
        }
        this.trigger();
        return super.canUse();
    }

    @Override
    public void start() {
        super.start();
        this.guard.setTarget(null);
        if (this.walkTimer <= 0) {
            this.walkTimer = 20;
        }
    }

    @Override
    public void tick() {
        --walkTimer;
        List<LivingEntity> list = this.guard.level().getEntitiesOfClass(LivingEntity.class, this.guard.getBoundingBox().inflate(5.0D, 3.0D, 5.0D));
        if (!list.isEmpty()) {
            for (LivingEntity mob : list) {
                if (mob.getLastHurtMob() instanceof GuardEntity || mob instanceof Mob m && m.getTarget() instanceof GuardEntity) {
                    if (walkTimer < 20)
                        this.walkTimer += 5;
                }
            }
        }
    }

    @Override
    protected @Nullable Vec3 getPosition() {
        List<LivingEntity> list = this.guard.level().getEntitiesOfClass(LivingEntity.class, this.guard.getBoundingBox().inflate(5.0D, 3.0D, 5.0D));
        if (!list.isEmpty()) {
            for (LivingEntity mob : list) {
                if (mob.getLastHurtMob() instanceof GuardEntity || mob instanceof Mob m && m.getTarget() instanceof GuardEntity) {
                    return DefaultRandomPos.getPosTowards(guard, 16, 7, mob.position(), (float) (Math.PI / 2));
                }
            }
        }
        return super.getPosition();
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && this.walkTimer > 0 && !guard.isEating();
    }

    @Override
    public void stop() {
        super.stop();
        this.guard.startUsingItem(InteractionHand.OFF_HAND);
        this.guard.getNavigation().stop();
    }
}
