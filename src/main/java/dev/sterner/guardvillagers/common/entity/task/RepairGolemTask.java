package dev.sterner.guardvillagers.common.entity.task;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.behavior.Behavior;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class RepairGolemTask extends Behavior<Villager> {
    private IronGolem golem;
    private boolean hasStartedHealing;

    public RepairGolemTask() {
        super(ImmutableMap.of());
    }

    @Override
    protected boolean checkExtraStartConditions(ServerLevel worldIn, Villager owner) {
        List<IronGolem> list = owner.level().getEntitiesOfClass(IronGolem.class, owner.getBoundingBox().inflate(10.0D, 5.0D, 10.0D));
        if (!list.isEmpty()) {
            for (IronGolem g : list) {
                if (!g.isInvisible() && g.isAlive() && g.getType() == EntityType.IRON_GOLEM) {
                    if (g.getHealth() <= 60.0D || this.hasStartedHealing && g.getHealth() < g.getMaxHealth()) {
                        this.golem = g;
                        owner.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected void stop(ServerLevel worldIn, Villager entityIn, long gameTimeIn) {
        if (golem != null && golem.getHealth() == golem.getMaxHealth()) {
            this.hasStartedHealing = false;
            entityIn.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        }
    }

    @Override
    protected void start(ServerLevel worldIn, Villager entityIn, long gameTimeIn) {
        if (golem == null)
            return;
        entityIn.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
        this.healGolem(entityIn);
    }

    @Override
    protected void tick(ServerLevel worldIn, Villager entityIn, long gameTimeIn) {
        if (golem != null && golem.getHealth() < golem.getMaxHealth())
            this.healGolem(entityIn);
    }

    @Override
    protected boolean canStillUse(ServerLevel level, Villager entityIn, long gameTimeIn) {
        return golem != null && golem.isAlive() && golem.getHealth() < golem.getMaxHealth();
    }

    public void healGolem(Villager healer) {
        if (golem == null) return;
        healer.getNavigation().moveTo(golem, 0.5);
        if (healer.distanceTo(golem) <= 2.0D) {
            this.hasStartedHealing = true;
            healer.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_INGOT));
            healer.swing(InteractionHand.MAIN_HAND);
            golem.heal(15.0F);
            float pitch = 1.0F + (golem.getRandom().nextFloat() - golem.getRandom().nextFloat()) * 0.2F;
            golem.playSound(SoundEvents.IRON_GOLEM_REPAIR, 1.0F, pitch);
        }
    }
}
