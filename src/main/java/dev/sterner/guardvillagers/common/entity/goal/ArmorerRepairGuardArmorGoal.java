package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

import java.util.List;

public class ArmorerRepairGuardArmorGoal extends Goal {
    private final GuardEntity guard;
    private Villager villager;

    public ArmorerRepairGuardArmorGoal(GuardEntity guard) {
        this.guard = guard;
    }

    @Override
    public boolean canUse() {
        List<Villager> list = this.guard.level().getEntitiesOfClass(Villager.class, this.guard.getBoundingBox().inflate(10.0D, 3.0D, 10.0D));
        if (!list.isEmpty()) {
            for (Villager mob : list) {
                if (mob != null) {
                    var prof = mob.getVillagerData().profession();
                    boolean isArmorerOrWeaponSmith = prof.is(VillagerProfession.ARMORER) || prof.is(VillagerProfession.WEAPONSMITH);
                    if (isArmorerOrWeaponSmith && guard.getTarget() == null) {
                        if (prof.is(VillagerProfession.ARMORER)) {
                            for (int i = 0; i < guard.guardInventory.getContainerSize() - 2; ++i) {
                                ItemStack itemstack = guard.guardInventory.getItem(i);
                                if (itemstack.isDamaged() && isArmor(itemstack) && itemstack.getDamageValue() >= itemstack.getMaxDamage() / 2) {
                                    this.villager = mob;
                                    return true;
                                }
                            }
                        }
                        if (prof.is(VillagerProfession.WEAPONSMITH)) {
                            for (int i = 4; i < 6; ++i) {
                                ItemStack itemstack = guard.guardInventory.getItem(i);
                                if (itemstack.isDamaged() && itemstack.getDamageValue() >= itemstack.getMaxDamage() / 2) {
                                    this.villager = mob;
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isArmor(ItemStack stack) {
        Equippable c = stack.get(DataComponents.EQUIPPABLE);
        return c != null && c.slot().getType() == EquipmentSlot.Type.HUMANOID_ARMOR;
    }

    @Override
    public void tick() {
        guard.getLookControl().setLookAt(villager, 30.0F, 30.0F);
        if (guard.distanceTo(villager) >= 2.0D) {
            guard.getNavigation().moveTo(villager, 0.5D);
            villager.getNavigation().moveTo(guard, 0.5D);
        } else {
            var profession = villager.getVillagerData().profession();
            if (profession.is(VillagerProfession.ARMORER)) {
                for (int i = 0; i < guard.guardInventory.getContainerSize() - 2; ++i) {
                    ItemStack itemstack = guard.guardInventory.getItem(i);
                    if (itemstack.isDamaged() && isArmor(itemstack) && itemstack.getDamageValue() >= itemstack.getMaxDamage() / 2 + guard.getRandom().nextInt(5)) {
                        itemstack.setDamageValue(itemstack.getDamageValue() - guard.getRandom().nextInt(5));
                    }
                }
            }
            if (profession.is(VillagerProfession.WEAPONSMITH)) {
                for (int i = 4; i < 6; ++i) {
                    ItemStack itemstack = guard.guardInventory.getItem(i);
                    if (itemstack.isDamaged() && itemstack.getDamageValue() >= itemstack.getMaxDamage() / 2 + guard.getRandom().nextInt(5)) {
                        itemstack.setDamageValue(itemstack.getDamageValue() - guard.getRandom().nextInt(5));
                    }
                }
            }
        }
    }
}
