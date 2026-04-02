package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemUseAnimation;

import java.util.List;

public class GuardEatFoodGoal extends Goal {
    public final GuardEntity guard;

    public GuardEatFoodGoal(GuardEntity guard) {
        this.guard = guard;
    }

    public static boolean isConsumable(ItemStack stack) {
        ItemUseAnimation anim = stack.getUseAnimation();
        return (anim == ItemUseAnimation.EAT || anim == ItemUseAnimation.DRINK) && !stack.is(Items.SPLASH_POTION);
    }

    @Override
    public boolean canUse() {
        return guard.getHealth() < guard.getMaxHealth() && GuardEatFoodGoal.isConsumable(guard.getOffhandItem()) && guard.isEating()
            || guard.getHealth() < guard.getMaxHealth() && GuardEatFoodGoal.isConsumable(guard.getOffhandItem()) && guard.getTarget() == null && !guard.isAggressive();
    }

    @Override
    public boolean canContinueToUse() {
        List<LivingEntity> list = this.guard.level().getEntitiesOfClass(LivingEntity.class, this.guard.getBoundingBox().inflate(5.0D, 3.0D, 5.0D));
        if (!list.isEmpty()) {
            for (LivingEntity mob : list) {
                if (mob instanceof Mob m && m.getTarget() instanceof GuardEntity) {
                    return false;
                }
            }
        }
        return guard.isUsingItem() && guard.getTarget() == null && guard.getHealth() < guard.getMaxHealth()
            || guard.getTarget() != null && guard.getHealth() < guard.getMaxHealth() / 2 + 2 && guard.isEating();
    }

    @Override
    public void start() {
        guard.startUsingItem(InteractionHand.OFF_HAND);
    }
}
