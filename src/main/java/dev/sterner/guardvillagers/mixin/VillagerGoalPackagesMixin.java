package dev.sterner.guardvillagers.mixin;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mojang.datafixers.util.Pair;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.task.RepairGolemTask;
import dev.sterner.guardvillagers.common.entity.task.ShareGossipWithGuard;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.GateBehavior;
import net.minecraft.world.entity.ai.behavior.InteractWith;
import net.minecraft.world.entity.ai.behavior.RunOne;
import net.minecraft.world.entity.ai.behavior.TradeWithVillager;
import net.minecraft.world.entity.ai.behavior.VillagerGoalPackages;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(VillagerGoalPackages.class)
public class VillagerGoalPackagesMixin {
    @Inject(method = "getWorkPackage", at = @At("RETURN"), cancellable = true)
    private static void guardvillagers$workPackage(Holder<VillagerProfession> profession, float speedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        if (!profession.is(VillagerProfession.ARMORER)) {
            return;
        }
        ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>> original = cir.getReturnValue();
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> list = new ArrayList<>(original);
        list.add(Pair.of(4, new RepairGolemTask()));
        cir.setReturnValue(ImmutableList.copyOf(list));
    }

    @Inject(method = "getMeetPackage", at = @At("RETURN"), cancellable = true)
    private static void guardvillagers$meetPackage(float speedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> list = new ArrayList<>(cir.getReturnValue());
        for (int i = 0; i < list.size(); i++) {
            Pair<Integer, ? extends BehaviorControl<? super Villager>> p = list.get(i);
            if (p.getSecond() instanceof GateBehavior<?> && !(p.getSecond() instanceof RunOne<?>)) {
                list.set(i, Pair.of(p.getFirst(), new GateBehavior<>(
                    ImmutableMap.of(),
                    ImmutableSet.of(MemoryModuleType.INTERACTION_TARGET),
                    GateBehavior.OrderPolicy.ORDERED,
                    GateBehavior.RunningPolicy.RUN_ONE,
                    ImmutableList.of(Pair.of(new ShareGossipWithGuard(), 1), Pair.of(new TradeWithVillager(), 1))
                )));
                break;
            }
        }
        cir.setReturnValue(ImmutableList.copyOf(list));
    }

    @Inject(method = "getIdlePackage", at = @At("RETURN"), cancellable = true)
    private static void guardvillagers$idlePackage(float speedModifier, CallbackInfoReturnable<ImmutableList<Pair<Integer, ? extends BehaviorControl<? super Villager>>>> cir) {
        List<Pair<Integer, ? extends BehaviorControl<? super Villager>>> list = new ArrayList<>(cir.getReturnValue());
        list.add(Pair.of(2, new RunOne<>(
            ImmutableList.of(Pair.of(InteractWith.of(GuardVillagers.GUARD_VILLAGER, 8, MemoryModuleType.INTERACTION_TARGET, speedModifier, 2), 2))
        )));
        cir.setReturnValue(ImmutableList.copyOf(list));
    }
}
