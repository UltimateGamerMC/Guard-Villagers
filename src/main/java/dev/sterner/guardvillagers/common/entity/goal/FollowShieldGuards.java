package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FollowShieldGuards extends Goal {
    private static final TargetingConditions NEARBY_THREATS = TargetingConditions.forCombat().range(3.0D).ignoreLineOfSight();
    private final GuardEntity taskOwner;
    private GuardEntity guardtofollow;
    private double x;
    private double y;
    private double z;

    public FollowShieldGuards(GuardEntity taskOwnerIn) {
        this.taskOwner = taskOwnerIn;
    }

    @Override
    public boolean canUse() {
        if (!(this.taskOwner.level() instanceof ServerLevel serverLevel)) {
            return false;
        }
        List<? extends GuardEntity> list = this.taskOwner.level().getEntitiesOfClass(this.taskOwner.getClass(), this.taskOwner.getBoundingBox().inflate(8.0D, 8.0D, 8.0D));
        if (!list.isEmpty()) {
            for (GuardEntity guard : list) {
                if (!guard.isInvisible()
                    && guard.getOffhandItem().is(Items.SHIELD)
                    && guard.isBlocking()
                    && serverLevel.getEntitiesOfClass(LivingEntity.class, this.taskOwner.getBoundingBox().inflate(5.0D), e -> NEARBY_THREATS.test(serverLevel, guard, e)).size() < 5) {
                    this.guardtofollow = guard;
                    Vec3 vec3 = this.getPosition();
                    if (vec3 == null) {
                        return false;
                    } else {
                        this.x = vec3.x;
                        this.y = vec3.y;
                        this.z = vec3.z;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    protected Vec3 getPosition() {
        return DefaultRandomPos.getPosTowards(this.taskOwner, 16, 7, this.guardtofollow.position(), (float) (Math.PI / 2F));
    }

    @Override
    public boolean canContinueToUse() {
        return !this.taskOwner.getNavigation().isDone() && this.taskOwner.getPassengers().isEmpty();
    }

    @Override
    public void stop() {
        this.taskOwner.getNavigation().stop();
        super.stop();
    }

    @Override
    public void start() {
        this.taskOwner.getNavigation().moveTo(this.x, this.y, this.z, 0.4D);
    }
}
