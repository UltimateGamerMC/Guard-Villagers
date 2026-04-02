package dev.sterner.guardvillagers.mixin;

import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Mob.class)
public abstract class MobEntityMixin {
    @Inject(method = "setTarget", at = @At("TAIL"))
    private void onSetTarget(@Nullable LivingEntity target, CallbackInfo ci) {
        if (target == null || (Object) this instanceof GuardEntity) {
            return;
        }
        boolean isVillager = target.getType() == EntityType.VILLAGER || target instanceof GuardEntity;
        if (isVillager) {
            Mob self = (Mob) (Object) this;
            double r = GuardVillagersConfig.guardVillagerHelpRange;
            List<Mob> list = self.level().getEntitiesOfClass(Mob.class, self.getBoundingBox().inflate(r, 5.0D, r));
            for (Mob mobEntity : list) {
                if ((mobEntity instanceof GuardEntity || self.getType() == EntityType.IRON_GOLEM) && mobEntity.getTarget() == null) {
                    mobEntity.setTarget(self);
                }
            }
        }

        if ((Object) this instanceof IronGolem golem && target instanceof GuardEntity) {
            golem.setTarget(null);
        }
    }
}
