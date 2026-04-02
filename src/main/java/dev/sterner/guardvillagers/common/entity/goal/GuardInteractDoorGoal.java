package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;

import java.util.List;

public class GuardInteractDoorGoal extends OpenDoorGoal {
    private final GuardEntity guard;

    public GuardInteractDoorGoal(GuardEntity pMob, boolean pCloseDoor) {
        super(pMob, pCloseDoor);
        this.guard = pMob;
    }

    @Override
    public boolean canUse() {
        return super.canUse();
    }

    @Override
    public void start() {
        if (areOtherMobsComingThroughDoor(guard)) {
            super.start();
            guard.swing(InteractionHand.MAIN_HAND);
        }
    }

    private boolean areOtherMobsComingThroughDoor(GuardEntity pEntity) {
        List<? extends PathfinderMob> nearbyEntityList = pEntity.level().getEntitiesOfClass(PathfinderMob.class, pEntity.getBoundingBox().inflate(4.0D));
        if (!nearbyEntityList.isEmpty()) {
            for (PathfinderMob mob : nearbyEntityList) {
                if (mob.blockPosition().closerThan(pEntity.blockPosition(), 2.0D))
                    return isMobComingThroughDoor(mob);
            }
        }
        return false;
    }

    private boolean isMobComingThroughDoor(PathfinderMob pEntity) {
        if (pEntity.getNavigation() == null) {
            return false;
            } else {
                Path path = pEntity.getNavigation().getPath();
                if (path == null || path.isDone()) {
                    return false;
                } else {
                    Node end = path.getEndNode();
                    Node next = path.getNextNode();
                    if (end == null) {
                        return false;
                    } else {
                        return pEntity.blockPosition().equals(end.asBlockPos())
                            || (next != null && pEntity.blockPosition().equals(next.asBlockPos()));
                    }
                }
            }
    }
}
