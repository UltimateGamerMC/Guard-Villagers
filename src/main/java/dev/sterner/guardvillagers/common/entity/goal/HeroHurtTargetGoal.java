package dev.sterner.guardvillagers.common.entity.goal;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

import java.util.EnumSet;

public class HeroHurtTargetGoal extends TargetGoal {
    private final GuardEntity guard;
    private LivingEntity attacker;
    private int timestamp;

    public HeroHurtTargetGoal(GuardEntity theEntityTameableIn) {
        super(theEntityTameableIn, false);
        this.guard = theEntityTameableIn;
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));
    }

    @Override
    public boolean canUse() {
        LivingEntity livingentity = this.guard.getOwner();
        if (livingentity == null) {
            return false;
        } else {
            this.attacker = livingentity.getLastHurtMob();
            int i = livingentity.getLastHurtMobTimestamp();
            return i != this.timestamp
                && this.attacker != null
                && !(this.attacker instanceof AbstractVillager)
                && !(this.attacker instanceof GuardEntity)
                && this.canAttack(this.attacker, TargetingConditions.forCombat());
        }
    }

    @Override
    public void start() {
        this.mob.setTarget(this.attacker);
        LivingEntity livingentity = this.guard.getOwner();
        if (livingentity != null) {
            this.timestamp = livingentity.getLastHurtMobTimestamp();
        }
        super.start();
    }
}
