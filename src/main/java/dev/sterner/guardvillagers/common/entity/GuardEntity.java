package dev.sterner.guardvillagers.common.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.goal.*;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.TimeUtil;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemStackWithSlot;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.Difficulty;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.goal.target.ResetUniversalAngerTargetGoal;
import net.minecraft.world.entity.ai.goal.target.TargetGoal;
import net.minecraft.world.entity.ai.gossip.GossipContainer;
import net.minecraft.world.entity.animal.polarbear.PolarBear;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.Witch;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class GuardEntity extends PathfinderMob implements CrossbowAttackMob, RangedAttackMob, NeutralMob {
    private static final Identifier USE_ITEM_SPEED_PENALTY_ID = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "use_item_speed_penalty");
    private static final AttributeModifier USE_ITEM_SPEED_PENALTY = new AttributeModifier(USE_ITEM_SPEED_PENALTY_ID, -0.25D, AttributeModifier.Operation.ADD_VALUE);
    private static final EntityDataAccessor<Optional<BlockPos>> GUARD_POS = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.OPTIONAL_BLOCK_POS);
    private static final EntityDataAccessor<Boolean> PATROLLING = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> GUARD_VARIANT = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> RUNNING_TO_EAT = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DATA_CHARGING_STATE = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> KICKING = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FOLLOWING = SynchedEntityData.defineId(GuardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final Map<Pose, EntityDimensions> SIZE_BY_POSE = ImmutableMap.<Pose, EntityDimensions>builder()
        .put(Pose.STANDING, EntityDimensions.scalable(0.6F, 1.95F))
        .put(Pose.SLEEPING, SLEEPING_DIMENSIONS)
        .put(Pose.FALL_FLYING, EntityDimensions.scalable(0.6F, 0.6F))
        .put(Pose.SWIMMING, EntityDimensions.scalable(0.6F, 0.6F))
        .put(Pose.SPIN_ATTACK, EntityDimensions.scalable(0.6F, 0.6F))
        .put(Pose.CROUCHING, EntityDimensions.scalable(0.6F, 1.75F))
        .put(Pose.DYING, EntityDimensions.fixed(0.2F, 0.2F))
        .build();
    private static final UniformInt ANGER_TIME = TimeUtil.rangeOfSeconds(20, 39);
    private static final Map<EquipmentSlot, Identifier> EQUIPMENT_SLOT_ITEMS = Util.make(Maps.newHashMap(), slotItems -> {
        slotItems.put(EquipmentSlot.MAINHAND, GuardEntityLootTables.GUARD_MAIN_HAND);
        slotItems.put(EquipmentSlot.OFFHAND, GuardEntityLootTables.GUARD_OFF_HAND);
        slotItems.put(EquipmentSlot.HEAD, GuardEntityLootTables.GUARD_HELMET);
        slotItems.put(EquipmentSlot.CHEST, GuardEntityLootTables.GUARD_CHEST);
        slotItems.put(EquipmentSlot.LEGS, GuardEntityLootTables.GUARD_LEGGINGS);
        slotItems.put(EquipmentSlot.FEET, GuardEntityLootTables.GUARD_FEET);
    });
    private final GossipContainer gossips = new GossipContainer();
    public long lastGossipTime;
    public long lastGossipDecayTime;
    public final SimpleContainer guardInventory = new SimpleContainer(6);
    public int kickTicks;
    public int shieldCoolDown;
    public int kickCoolDown;
    public boolean interacting;
    protected boolean spawnWithArmor;
    private @Nullable UUID ownerId;
    private long persistentAngerEndTime = -1L;
    private @Nullable EntityReference<LivingEntity> persistentAngerTarget;

    public GuardEntity(EntityType<? extends GuardEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static int slotToInventoryIndex(EquipmentSlot slot) {
        return switch (slot) {
            case CHEST -> 1;
            case FEET -> 3;
            case LEGS -> 2;
            default -> 0;
        };
    }

    public static int getRandomTypeForBiome(Level level, BlockPos pos) {
        ResourceKey<VillagerType> type = VillagerType.byBiome(level.getBiome(pos));
        if (type.equals(VillagerType.SNOW)) return 6;
        if (type.equals(VillagerType.TAIGA)) return 5;
        if (type.equals(VillagerType.JUNGLE)) return 4;
        if (type.equals(VillagerType.SWAMP)) return 3;
        if (type.equals(VillagerType.SAVANNA)) return 2;
        if (type.equals(VillagerType.DESERT)) return 1;
        return 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, GuardVillagersConfig.healthModifier)
            .add(Attributes.MOVEMENT_SPEED, GuardVillagersConfig.speedModifier)
            .add(Attributes.ATTACK_DAMAGE, 1.0D)
            .add(Attributes.FOLLOW_RANGE, GuardVillagersConfig.followRangeModifier);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return SIZE_BY_POSE.getOrDefault(pose, super.getDefaultDimensions(pose));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(GUARD_VARIANT, 0);
        builder.define(DATA_CHARGING_STATE, false);
        builder.define(KICKING, false);
        builder.define(FOLLOWING, false);
        builder.define(GUARD_POS, Optional.empty());
        builder.define(PATROLLING, false);
        builder.define(RUNNING_TO_EAT, false);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData groupData) {
        this.setPersistenceRequired();
        int type = getRandomTypeForBiome(level.getLevel(), this.blockPosition());
        if (groupData instanceof GuardEntityData gd) {
            type = gd.variantData;
            groupData = new GuardEntityData(type);
        }
        this.setGuardEntityVariant(type);
        RandomSource random = level.getRandom();
        this.populateDefaultEquipmentSlots(random, difficulty);
        return super.finalizeSpawn(level, difficulty, spawnReason, groupData);
    }

    @Override
    protected void doPush(Entity entity) {
        if (entity instanceof PathfinderMob living) {
            LivingEntity t = living.getTarget();
            boolean attackTargets = t instanceof Villager || t instanceof IronGolem || t instanceof GuardEntity;
            if (attackTargets) this.setTarget(living);
        }
        super.doPush(entity);
    }

    @Nullable
    public BlockPos getPatrolPos() {
        return this.entityData.get(GUARD_POS).orElse(null);
    }

    public void setPatrolPos(@Nullable BlockPos position) {
        this.entityData.set(GUARD_POS, Optional.ofNullable(position));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuardVillagers.GUARD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        if (this.isBlocking()) {
            return SoundEvents.SHIELD_BLOCK.value();
        }
        return GuardVillagers.GUARD_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuardVillagers.GUARD_DEATH;
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        for (int i = 0; i < this.guardInventory.getContainerSize(); ++i) {
            ItemStack itemstack = this.guardInventory.getItem(i);
            RandomSource random = level.getRandom();
            if (!itemstack.isEmpty() && !EnchantmentHelper.has(itemstack, EnchantmentEffectComponents.PREVENT_EQUIPMENT_DROP) && random.nextFloat() < GuardVillagersConfig.chanceToDropEquipment) {
                this.spawnAtLocation(level, itemstack);
            }
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setOwnerId(input.read("Owner", UUIDUtil.CODEC).orElse(null));
        this.setGuardEntityVariant(input.getIntOr("Type", 0));
        this.kickTicks = input.getIntOr("KickTicks", 0);
        this.setFollowing(input.getBooleanOr("Following", false));
        this.interacting = input.getBooleanOr("Interacting", false);
        this.setPatrolling(input.getBooleanOr("Patrolling", false));
        this.shieldCoolDown = input.getIntOr("KickCooldown", 0);
        this.kickCoolDown = input.getIntOr("ShieldCooldown", 0);
        this.lastGossipDecayTime = input.getLongOr("LastGossipDecay", 0L);
        this.lastGossipTime = input.getLongOr("LastGossipTime", 0L);
        this.spawnWithArmor = input.getBooleanOr("SpawnWithArmor", false);
        if (input.getInt("PatrolPosX").isPresent()) {
            int x = input.getIntOr("PatrolPosX", 0);
            int y = input.getIntOr("PatrolPosY", 0);
            int z = input.getIntOr("PatrolPosZ", 0);
            this.entityData.set(GUARD_POS, Optional.of(new BlockPos(x, y, z)));
        }
        input.read("Gossips", GossipContainer.CODEC).ifPresent(this.gossips::putAll);
        for (ItemStackWithSlot slot : input.listOrEmpty("Inventory", ItemStackWithSlot.CODEC)) {
            if (slot.isValidInContainer(this.guardInventory.getContainerSize())) {
                this.guardInventory.setItem(slot.slot(), slot.stack());
            }
        }
        if (!this.level().isClientSide()) {
            this.readPersistentAngerSaveData(this.level(), input);
        }
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Type", this.getGuardEntityVariant());
        output.putInt("KickTicks", this.kickTicks);
        output.putInt("ShieldCooldown", this.shieldCoolDown);
        output.putInt("KickCooldown", this.kickCoolDown);
        output.putBoolean("Following", this.isFollowing());
        output.putBoolean("Interacting", this.interacting);
        output.putBoolean("Patrolling", this.isPatrolling());
        output.putBoolean("SpawnWithArmor", this.spawnWithArmor);
        output.putLong("LastGossipTime", this.lastGossipTime);
        output.putLong("LastGossipDecay", this.lastGossipDecayTime);
        output.storeNullable("Owner", UUIDUtil.CODEC, this.getOwnerId());
        var inv = output.list("Inventory", ItemStackWithSlot.CODEC);
        for (int i = 0; i < this.guardInventory.getContainerSize(); ++i) {
            ItemStack stack = this.guardInventory.getItem(i);
            if (!stack.isEmpty()) inv.add(new ItemStackWithSlot(i, stack));
        }
        if (this.getPatrolPos() != null) {
            output.putInt("PatrolPosX", this.getPatrolPos().getX());
            output.putInt("PatrolPosY", this.getPatrolPos().getY());
            output.putInt("PatrolPosZ", this.getPatrolPos().getZ());
        }
        output.store("Gossips", GossipContainer.CODEC, this.gossips);
        this.addPersistentAngerSaveData(output);
    }

    private void maybeDecayGossip() {
        long i = this.level().getGameTime();
        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = i;
        } else if (i >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = i;
        }
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> this.guardInventory.getItem(0);
            case CHEST -> this.guardInventory.getItem(1);
            case LEGS -> this.guardInventory.getItem(2);
            case FEET -> this.guardInventory.getItem(3);
            case OFFHAND -> this.guardInventory.getItem(4);
            case MAINHAND -> this.guardInventory.getItem(5);
            case BODY, SADDLE -> ItemStack.EMPTY;
        };
    }

    public GossipContainer getGossips() {
        return this.gossips;
    }

    public int getPlayerEntityReputation(Player player) {
        return this.gossips.getReputation(player.getUUID(), gossipType -> true);
    }

    @Nullable
    public Player getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            boolean heroOfTheVillage = uuid != null && this.level().getPlayerByUUID(uuid) != null && this.level().getPlayerByUUID(uuid).hasEffect(MobEffects.HERO_OF_THE_VILLAGE);
            return uuid == null || (this.level().getPlayerByUUID(uuid) != null && (!heroOfTheVillage && GuardVillagersConfig.followHero) || !GuardVillagersConfig.followHero && this.level().getPlayerByUUID(uuid) == null) ? null : this.level().getPlayerByUUID(uuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean isOwner(LivingEntity entityIn) {
        return entityIn == this.getOwner();
    }

    @Nullable
    public UUID getOwnerId() {
        return this.ownerId;
    }

    public void setOwnerId(@Nullable UUID id) {
        this.ownerId = id;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (this.isKicking() && target instanceof LivingEntity living) {
            living.knockback(1.0, Mth.sin(this.getYRot() * ((float) Math.PI / 180F)), -Mth.cos(this.getYRot() * ((float) Math.PI / 180F)));
            this.kickTicks = 10;
            level.broadcastEntityEvent(this, (byte) 4);
            this.lookAt(target, 90.0F, 90.0F);
        }
        ItemStack hand = this.getMainHandItem();
        hand.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte status) {
        if (status == 4) {
            this.kickTicks = 10;
        } else {
            super.handleEntityEvent(status);
        }
    }

    @Override
    public boolean isImmobile() {
        return this.interacting || super.isImmobile();
    }

    @Override
    public void die(DamageSource damageSource) {
        if ((this.level().getDifficulty() == Difficulty.NORMAL || this.level().getDifficulty() == Difficulty.HARD) && damageSource.getEntity() instanceof Zombie) {
            ZombieVillager zombieguard = this.convertTo(EntityType.ZOMBIE_VILLAGER, ConversionParams.single(this, true, true), z -> {
                z.finalizeSpawn((ServerLevelAccessor) this.level(), ((ServerLevel) this.level()).getCurrentDifficultyAt(z.blockPosition()), EntitySpawnReason.CONVERSION, new Zombie.ZombieGroupData(false, true));
                if (!this.isSilent()) {
                    ((ServerLevel) this.level()).levelEvent(null, 1026, this.blockPosition(), 0);
                }
            });
            if (zombieguard != null && (this.level().getDifficulty() == Difficulty.HARD || !this.random.nextBoolean())) {
                return;
            }
        }
        super.die(damageSource);
    }

    public ItemStack eatFood(Level level, ItemStack stack) {
        var food = stack.get(DataComponents.FOOD);
        if (food != null) {
            this.heal(food.nutrition());
        }
        level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.5F, level.getRandom().nextFloat() * 0.1F + 0.9F);
        return stack;
    }

    @Override
    public void aiStep() {
        this.updateSwingTime();
        if (this.kickTicks > 0) --this.kickTicks;
        if (this.kickCoolDown > 0) --this.kickCoolDown;
        if (this.shieldCoolDown > 0) --this.shieldCoolDown;
        if (this.getHealth() < this.getMaxHealth() && this.tickCount % 200 == 0) {
            this.heal(GuardVillagersConfig.amountOfHealthRegenerated);
        }
        if (this.spawnWithArmor && this.level() instanceof ServerLevel serverWorld) {
            for (EquipmentSlot equipmentslottype : EquipmentSlot.values()) {
                for (ItemStack st : this.getStacksFromLootTable(equipmentslottype, serverWorld)) {
                    this.setItemSlot(equipmentslottype, st);
                }
            }
            this.spawnWithArmor = false;
        }
        super.aiStep();
        if (!this.level().isClientSide()) {
            this.updatePersistentAnger((ServerLevel) this.level(), true);
        }
    }

    @Override
    public void tick() {
        this.maybeDecayGossip();
        super.tick();
    }

    public boolean isInAttackRange(LivingEntity entity) {
        AttackRange lv = this.getAttackRangeWith(this.getMainHandItem());
        double d = lv.effectiveMaxRange(this) * Math.sqrt(3.55);
        double e = lv.effectiveMinRange(this);
        AABB box = entity.getBoundingBox();
        return this.getAttackBoundingBox(d).intersects(box) && (e <= 0.0 || !this.getAttackBoundingBox(e).intersects(box));
    }

    public void damageShield(float amount) {
        if (this.getUseItem().getItem() == Items.SHIELD) {
            if (amount >= 3.0F) {
                int i = 1 + Mth.floor(amount);
                InteractionHand hand = this.getUsedItemHand();
                this.getUseItem().hurtAndBreak(i, this, hand.asEquipmentSlot());
                if (this.getUseItem().isEmpty()) {
                    if (hand == InteractionHand.MAIN_HAND) {
                        this.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }
                    this.playSound(SoundEvents.SHIELD_BREAK.value(), 0.8F, 0.8F + this.level().getRandom().nextFloat() * 0.4F);
                }
            }
        }
    }

    @Override
    public void startUsingItem(InteractionHand hand) {
        super.startUsingItem(hand);
        ItemStack itemstack = this.getItemInHand(hand);
        if (itemstack.getItem() == Items.SHIELD) {
            AttributeInstance modifiableattributeinstance = this.getAttribute(Attributes.MOVEMENT_SPEED);
            if (modifiableattributeinstance != null) {
                modifiableattributeinstance.removeModifier(USE_ITEM_SPEED_PENALTY_ID);
                modifiableattributeinstance.addTransientModifier(USE_ITEM_SPEED_PENALTY);
            }
        }
    }

    @Override
    public void stopUsingItem() {
        super.stopUsingItem();
        AttributeInstance inst = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (inst != null && inst.hasModifier(USE_ITEM_SPEED_PENALTY_ID)) {
            inst.removeModifier(USE_ITEM_SPEED_PENALTY_ID);
        }
    }

    private static boolean isArmorStack(ItemStack stack) {
        var c = stack.get(DataComponents.EQUIPPABLE);
        return c != null && c.slot().isArmor();
    }

    public void disableShield(boolean increase) {
        var enchantments = this.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        int eff = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.EFFICIENCY), this.getMainHandItem());
        float chance = 0.25F + (float) eff * 0.05F;
        if (increase) chance += 0.75;
        if (this.random.nextFloat() < chance) {
            this.shieldCoolDown = 100;
            this.stopUsingItem();
            this.level().broadcastEntityEvent(this, (byte) 30);
        }
    }

    public boolean isCharging() {
        return this.entityData.get(DATA_CHARGING_STATE);
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(DATA_CHARGING_STATE, charging);
    }

    public boolean isKicking() {
        return this.entityData.get(KICKING);
    }

    public void setKicking(boolean kicking) {
        this.entityData.set(KICKING, kicking);
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance localDifficulty) {
        this.setDropChance(EquipmentSlot.MAINHAND, 1.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 1.0F);
        this.spawnWithArmor = true;
    }

    public List<ItemStack> getStacksFromLootTable(EquipmentSlot slot, ServerLevel serverWorld) {
        if (EQUIPMENT_SLOT_ITEMS.containsKey(slot)) {
            ResourceKey<LootTable> key = ResourceKey.create(Registries.LOOT_TABLE, EQUIPMENT_SLOT_ITEMS.get(slot));
            LootTable loot = serverWorld.getServer().reloadableRegistries().getLootTable(key);
            LootParams ctx = new LootParams.Builder(serverWorld).withParameter(LootContextParams.THIS_ENTITY, this).withParameter(LootContextParams.ORIGIN, this.position()).create(LootContextParamSets.EQUIPMENT);
            return loot.getRandomItems(ctx);
        }
        return Collections.emptyList();
    }

    public int getGuardEntityVariant() {
        return this.entityData.get(GUARD_VARIANT);
    }

    public void setGuardEntityVariant(int typeId) {
        this.entityData.set(GUARD_VARIANT, typeId);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(0, new KickGoal(this));
        this.goalSelector.addGoal(0, new GuardEatFoodGoal(this));
        this.goalSelector.addGoal(0, new RaiseShieldGoal(this));
        this.goalSelector.addGoal(1, new GuardRunToEatGoal(this));
        this.goalSelector.addGoal(2, new RangedCrossbowAttackPassiveGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.addGoal(2, new RangedBowAttackPassiveGoal<>(this, 0.5D, 20, 15.0F));
        this.goalSelector.addGoal(2, new GuardEntityMeleeGoal(this, 0.8D, true));
        this.goalSelector.addGoal(3, new GuardEntity.FollowHeroGoal(this));
        if (GuardVillagersConfig.guardEntitysRunFromPolarBears)
            this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, PolarBear.class, 12.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(3, new GolemRandomStrollInVillageGoal(this, 0.5D));
        this.goalSelector.addGoal(3, new MoveThroughVillageGoal(this, 0.5D, false, 4, () -> false));
        if (GuardVillagersConfig.guardEntitysOpenDoors) this.goalSelector.addGoal(3, new GuardInteractDoorGoal(this, true));
        if (GuardVillagersConfig.guardEntityFormation) this.goalSelector.addGoal(5, new FollowShieldGuards(this));
        if (GuardVillagersConfig.clericHealing) this.goalSelector.addGoal(6, new RunToClericGoal(this));
        if (GuardVillagersConfig.armorerRepairGuardEntityArmor)
            this.goalSelector.addGoal(6, new ArmorerRepairGuardArmorGoal(this));
        this.goalSelector.addGoal(4, new WalkBackToCheckPointGoal(this, 0.5D));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Villager.class, 8.0F));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new GuardLookAtAndStopMovingWhenBeingTheInteractionTarget(this));
        this.targetSelector.addGoal(5, new GuardEntity.DefendVillageGuardEntityGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Raider.class, true));
        this.targetSelector.addGoal(2, new HurtByTargetGoal(this, GuardEntity.class, IronGolem.class).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Witch.class, true));
        this.targetSelector.addGoal(3, new HeroHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new HeroHurtTargetGoal(this));
        if (GuardVillagersConfig.attackAllMobs)
            this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<Mob>(this, Mob.class, 5, true, true, (target, world) -> target instanceof Monster && !GuardVillagersConfig.mobBlackList.contains(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString())));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Player.class, 10, true, false, this::isAngryAt));
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal<>(this, Zombie.class, true));
        this.targetSelector.addGoal(4, new ResetUniversalAngerTargetGoal<>(this, false));
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float pullProgress) {
        this.shieldCoolDown = 8;
        if (this.getMainHandItem().getItem() instanceof CrossbowItem)
            this.performCrossbowAttack(this, 6.0F);
        if (this.getMainHandItem().getItem() instanceof BowItem) {
            ItemStack bowStack = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
            ItemStack itemStack = this.getProjectile(bowStack);
            ItemStack hand = this.getUseItem();
            AbstractArrow persistentProjectileEntity = ProjectileUtil.getMobArrow(this, itemStack, pullProgress, bowStack);
            var enchantments = this.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            int powerLevel = EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.POWER), bowStack);
            if (powerLevel > 0) {
                double baseDamage = pullProgress * 2.0 + this.level().getDifficulty().getId() * 0.11;
                persistentProjectileEntity.setBaseDamage(baseDamage + (double) powerLevel * 0.5D + 0.5D);
            }
            if (EnchantmentHelper.getItemEnchantmentLevel(enchantments.getOrThrow(Enchantments.FLAME), bowStack) > 0)
                persistentProjectileEntity.igniteForSeconds(100);
            double d = target.getX() - this.getX();
            double e = target.getY(0.3333333333333333D) - persistentProjectileEntity.getY();
            double f = target.getZ() - this.getZ();
            double g = Math.sqrt(d * d + f * f);
            if (this.level() instanceof ServerLevel sl) {
                Projectile.spawnProjectileUsingShoot(persistentProjectileEntity, sl, itemStack, d, e + g * 0.20000000298023224D, f, 1.6F, 14 - this.level().getDifficulty().getId() * 4);
            }
            this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            hand.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public void setItemSlot(EquipmentSlot slotIn, ItemStack stack) {
        super.setItemSlot(slotIn, stack);
        switch (slotIn) {
            case CHEST:
                if (this.guardInventory.getItem(1).isEmpty())
                    this.guardInventory.setItem(1, stack);
                break;
            case FEET:
                if (this.guardInventory.getItem(3).isEmpty())
                    this.guardInventory.setItem(3, stack);
                break;
            case HEAD:
                if (this.guardInventory.getItem(0).isEmpty())
                    this.guardInventory.setItem(0, stack);
                break;
            case LEGS:
                if (this.guardInventory.getItem(2).isEmpty())
                    this.guardInventory.setItem(2, stack);
                break;
            case MAINHAND:
                this.guardInventory.setItem(5, stack);
                break;
            case OFFHAND:
                this.guardInventory.setItem(4, stack);
                break;
            default:
                break;
        }
    }

    public int getGuardVariant() {
        return this.entityData.get(GUARD_VARIANT);
    }

    public ItemStack getProjectile(ItemStack shootable) {
        if (shootable.getItem() instanceof ProjectileWeaponItem rw) {
            Predicate<ItemStack> predicate = rw.getSupportedHeldProjectiles();
            ItemStack itemstack = ProjectileWeaponItem.getHeldProjectile(this, predicate);
            return itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack;
        }
        return ItemStack.EMPTY;
    }

    public int getKickTicks() {
        return this.kickTicks;
    }

    public boolean isFollowing() {
        return this.entityData.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.entityData.set(FOLLOWING, following);
    }

    @Override
    public boolean canAttack(LivingEntity target) {
        return !GuardVillagersConfig.mobBlackList.contains(BuiltInRegistries.ENTITY_TYPE.getKey(target.getType()).toString()) && !target.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !this.isOwner(target) && !(target instanceof Villager) && !(target instanceof IronGolem) && !(target instanceof GuardEntity) && super.canAttack(target);
    }

    @Override
    public void rideTick() {
        super.rideTick();
        if (this.getVehicle() instanceof PathfinderMob creatureentity) {
            this.yBodyRot = creatureentity.yBodyRot;
        }
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public void setTarget(@Nullable LivingEntity entity) {
        if (entity instanceof GuardEntity || entity instanceof Villager || entity instanceof IronGolem)
            return;
        super.setTarget(entity);
    }

    public void gossip(Villager villager, long gameTime) {
        if (gameTime < this.lastGossipTime || gameTime >= this.lastGossipTime + 1200L) {
            this.gossips.transferFrom(villager.getGossips(), this.random, 10);
            this.lastGossipTime = gameTime;
        }
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        boolean configValues = player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.setGuardPatrolHotv || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv && GuardVillagersConfig.setGuardPatrolHotv || this.getPlayerEntityReputation(player) >= GuardVillagersConfig.reputationRequirement || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !GuardVillagersConfig.giveGuardStuffHotv && !GuardVillagersConfig.setGuardPatrolHotv || this.getOwnerId() != null && this.getOwnerId().equals(player.getUUID());
        boolean inventoryRequirements = !player.isSpectator();
        if (inventoryRequirements) {
            if (this.getTarget() != player && this.isEffectiveAi() && configValues) {
                if (player instanceof ServerPlayer) {
                    this.openGui((ServerPlayer) player);
                    return InteractionResult.SUCCESS;
                }
            }
            return InteractionResult.CONSUME;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void hurtArmor(DamageSource damageSource, float damage) {
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 0; i < this.guardInventory.getContainerSize(); ++i) {
                ItemStack itemstack = this.guardInventory.getItem(i);
                if ((!damageSource.is(DamageTypes.ON_FIRE) || itemstack.canBeHurtBy(damageSource)) && isArmorStack(itemstack)) {
                    var eq = itemstack.get(DataComponents.EQUIPPABLE);
                    if (eq != null) {
                        itemstack.hurtAndBreak((int) damage, this, eq.slot());
                    }
                }
            }
        }
    }

    @Override
    public void thunderHit(ServerLevel level, LightningBolt lightning) {
        if (level.getDifficulty() != Difficulty.PEACEFUL) {
            Witch witchentity = EntityType.WITCH.create(level, EntitySpawnReason.CONVERSION);
            if (witchentity == null) return;
            witchentity.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            witchentity.finalizeSpawn(level, level.getCurrentDifficultyAt(witchentity.blockPosition()), EntitySpawnReason.CONVERSION, null);
            witchentity.setNoAi(this.isNoAi());
            witchentity.setCustomName(this.getCustomName());
            witchentity.setCustomNameVisible(this.isCustomNameVisible());
            witchentity.setPersistenceRequired();
            level.addFreshEntityWithPassengers(witchentity);
            this.discard();
        } else {
            super.thunderHit(level, lightning);
        }
    }

    @Override
    public long getPersistentAngerEndTime() {
        return this.persistentAngerEndTime;
    }

    @Override
    public void setPersistentAngerEndTime(long time) {
        this.persistentAngerEndTime = time;
    }

    @Nullable
    @Override
    public EntityReference<LivingEntity> getPersistentAngerTarget() {
        return this.persistentAngerTarget;
    }

    @Override
    public void setPersistentAngerTarget(@Nullable EntityReference<LivingEntity> persistentAngerTarget) {
        this.persistentAngerTarget = persistentAngerTarget;
    }

    @Override
    public void startPersistentAngerTimer() {
        this.setTimeToRemainAngry(ANGER_TIME.sample(this.random));
    }

    public void openGui(ServerPlayer player) {
        this.setOwnerId(player.getUUID());
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        this.interacting = true;
        if (!this.level().isClientSide()) {
            player.openMenu(new GuardScreenHandlerFactory());
        }
    }

    public void setGuardVariant(int i) {
        this.entityData.set(GUARD_VARIANT, i);
    }

    private class GuardScreenHandlerFactory implements ExtendedMenuProvider<Integer> {
        private GuardEntity guard() {
            return GuardEntity.this;
        }

        @Override
        public Integer getScreenOpeningData(ServerPlayer player) {
            return this.guard().getId();
        }

        @Override
        public Component getDisplayName() {
            return this.guard().getDisplayName();
        }

        @Nullable
        @Override
        public AbstractContainerMenu createMenu(int syncId, net.minecraft.world.entity.player.Inventory inv, Player player) {
            return GuardVillagerScreenHandler.create(syncId, inv, this.guard().getId());
        }
    }

    public boolean isEating() {
        return GuardEatFoodGoal.isConsumable(this.getUseItem()) && this.isUsingItem();
    }

    public boolean isPatrolling() {
        return this.entityData.get(PATROLLING);
    }

    public void setPatrolling(boolean patrolling) {
        this.entityData.set(PATROLLING, patrolling);
    }

    @Override
    public boolean canUseNonMeleeWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem || super.canUseNonMeleeWeapon(stack);
    }

    public static class GuardEntityData implements SpawnGroupData {
        public final int variantData;

        public GuardEntityData(int type) {
            this.variantData = type;
        }
    }

    public static class DefendVillageGuardEntityGoal extends TargetGoal {
        private final GuardEntity guard;
        private LivingEntity villageAggressorTarget;

        public DefendVillageGuardEntityGoal(GuardEntity guardIn) {
            super(guardIn, false, true);
            this.guard = guardIn;
            this.setFlags(EnumSet.of(Goal.Flag.TARGET, Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            AABB box = this.guard.getBoundingBox().inflate(10.0D, 8.0D, 10.0D);
            List<Villager> list = guard.level().getEntitiesOfClass(Villager.class, box);
            List<Player> list1 = guard.level().getEntitiesOfClass(Player.class, box);
            for (Villager villager : list) {
                for (Player player : list1) {
                    int i = villager.getPlayerReputation(player);
                    if (i <= GuardVillagersConfig.reputationRequirementToBeAttacked) {
                        this.villageAggressorTarget = player;
                    }
                }
            }
            return villageAggressorTarget != null && !villageAggressorTarget.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && !this.villageAggressorTarget.isSpectator() && !((Player) this.villageAggressorTarget).getAbilities().instabuild;
        }

        @Override
        public void start() {
            this.guard.setTarget(this.villageAggressorTarget);
            super.start();
        }
    }

    public static class FollowHeroGoal extends Goal {
        public final GuardEntity guard;

        public FollowHeroGoal(GuardEntity mob) {
            this.guard = mob;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public void tick() {
            if (guard.getOwner() != null && guard.distanceToSqr(guard.getOwner()) > 9.0D) {
                guard.getNavigation().moveTo(guard.getOwner(), 0.7D);
                guard.getLookControl().setLookAt(guard.getOwner());
            } else {
                guard.getNavigation().stop();
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.canUse();
        }

        @Override
        public boolean canUse() {
            return guard.isFollowing() && guard.getOwner() != null;
        }

        @Override
        public void stop() {
            this.guard.getNavigation().stop();
        }
    }

    public static class GuardEntityMeleeGoal extends MeleeAttackGoal {
        public final GuardEntity guard;

        public GuardEntityMeleeGoal(GuardEntity guard, double speedIn, boolean useLongMemory) {
            super(guard, speedIn, useLongMemory);
            this.guard = guard;
        }

        @Override
        public boolean canUse() {
            return !(this.guard.getMainHandItem().getItem() instanceof CrossbowItem) && this.guard.getTarget() != null && !this.guard.isEating() && super.canUse();
        }

        @Override
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.guard.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                if (target.distanceToSqr(guard) <= 9.0D && !guard.isBlocking()) {
                    guard.getMoveControl().strafe(-2.0F, 0.0F);
                    guard.lookAt(target, 30.0F, 30.0F);
                }
                super.tick();
            }
        }

        @Override
        protected boolean canPerformAttack(LivingEntity target) {
            return this.isTimeToAttack() && this.guard.isInAttackRange(target) && this.guard.getSensing().hasLineOfSight(target);
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity enemy) {
            if (this.canPerformAttack(enemy)) {
                this.resetAttackCooldown();
                this.guard.stopUsingItem();
                if (guard.shieldCoolDown == 0) this.guard.shieldCoolDown = 8;
                this.guard.swing(InteractionHand.MAIN_HAND);
                this.guard.doHurtTarget(Goal.getServerLevel(this.guard), enemy);
            }
        }
    }
}
