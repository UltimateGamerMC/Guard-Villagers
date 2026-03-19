package dev.sterner.guardvillagers.common.entity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.Dynamic;
import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import dev.sterner.guardvillagers.common.entity.goal.*;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RangedAttackMob;
import net.minecraft.entity.conversion.EntityConversionContext;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.MerchantEntity;
import net.minecraft.entity.passive.PolarBearEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.entity.raid.RaiderEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.*;
import net.minecraft.entity.LazyEntityReference;
import net.minecraft.inventory.StackWithSlot;
import net.minecraft.loot.LootTable;
import net.minecraft.loot.context.LootContextParameters;
import net.minecraft.loot.context.LootWorldContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.component.type.AttackRangeComponent;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.intprovider.UniformIntProvider;
import net.minecraft.util.math.random.Random;
import net.minecraft.registry.RegistryKey;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import net.minecraft.village.VillagerGossips;
import net.minecraft.village.VillagerType;
import net.minecraft.world.*;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class GuardEntity extends PathAwareEntity implements CrossbowUser, RangedAttackMob, Angerable, InventoryChangedListener, InteractionObserver {
    private static final UUID MODIFIER_UUID = UUID.fromString("5CD17E52-A79A-43D3-A529-90FDE04B181E");
    private static final Identifier USE_ITEM_SPEED_PENALTY_ID = Identifier.of(GuardVillagers.MODID, "use_item_speed_penalty");
    private static final EntityAttributeModifier USE_ITEM_SPEED_PENALTY = new EntityAttributeModifier(USE_ITEM_SPEED_PENALTY_ID, -0.25D, EntityAttributeModifier.Operation.ADD_VALUE);
    private static final TrackedData<Optional<BlockPos>> GUARD_POS = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.OPTIONAL_BLOCK_POS);
    private static final TrackedData<Boolean> PATROLLING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Integer> GUARD_VARIANT = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.INTEGER);
    private static final TrackedData<Boolean> RUNNING_TO_EAT = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> DATA_CHARGING_STATE = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> KICKING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final TrackedData<Boolean> FOLLOWING = DataTracker.registerData(GuardEntity.class, TrackedDataHandlerRegistry.BOOLEAN);
    private static final Map<EntityPose, EntityDimensions> SIZE_BY_POSE = ImmutableMap.<EntityPose, EntityDimensions>builder().put(EntityPose.STANDING, EntityDimensions.changing(0.6F, 1.95F)).put(EntityPose.SLEEPING, SLEEPING_DIMENSIONS).put(EntityPose.GLIDING, EntityDimensions.changing(0.6F, 0.6F)).put(EntityPose.SWIMMING, EntityDimensions.changing(0.6F, 0.6F)).put(EntityPose.SPIN_ATTACK, EntityDimensions.changing(0.6F, 0.6F)).put(EntityPose.CROUCHING, EntityDimensions.changing(0.6F, 1.75F)).put(EntityPose.DYING, EntityDimensions.fixed(0.2F, 0.2F)).build();
    private static final UniformIntProvider angerTime = TimeHelper.betweenSeconds(20, 39);
    private static final Map<EquipmentSlot, Identifier> EQUIPMENT_SLOT_ITEMS = Util.make(Maps.newHashMap(), (slotItems) -> {
        slotItems.put(EquipmentSlot.MAINHAND, GuardEntityLootTables.GUARD_MAIN_HAND);
        slotItems.put(EquipmentSlot.OFFHAND, GuardEntityLootTables.GUARD_OFF_HAND);
        slotItems.put(EquipmentSlot.HEAD, GuardEntityLootTables.GUARD_HELMET);
        slotItems.put(EquipmentSlot.CHEST, GuardEntityLootTables.GUARD_CHEST);
        slotItems.put(EquipmentSlot.LEGS, GuardEntityLootTables.GUARD_LEGGINGS);
        slotItems.put(EquipmentSlot.FEET, GuardEntityLootTables.GUARD_FEET);
    });
    private final VillagerGossips gossips = new VillagerGossips();
    public long lastGossipTime;
    public long lastGossipDecayTime;
    public SimpleInventory guardInventory = new SimpleInventory(6);
    public int kickTicks;
    public int shieldCoolDown;
    public int kickCoolDown;
    public boolean interacting;
    protected boolean spawnWithArmor;
    private @Nullable UUID ownerId;
    private long angerEndTime = -1L;
    private LazyEntityReference<LivingEntity> angryAt;

    public GuardEntity(EntityType<? extends GuardEntity> type, World world) {
        super(type, world);
        this.guardInventory.addListener(this);
        this.setPersistent();
    }

    public static int slotToInventoryIndex(EquipmentSlot slot) {
        return switch (slot) {
            case CHEST -> 1;
            case FEET -> 3;
            case LEGS -> 2;
            default -> 0;
        };
    }

    /**
     * Credit - SmellyModder for Biome Specific Textures
     */
    public static int getRandomTypeForBiome(WorldAccess world, BlockPos pos) {
        RegistryKey<VillagerType> type = VillagerType.forBiome(world.getBiome(pos));
        if (type.equals(VillagerType.SNOW)) return 6;
        else if (type.equals(VillagerType.TAIGA)) return 5;
        else if (type.equals(VillagerType.JUNGLE)) return 4;
        else if (type.equals(VillagerType.SWAMP)) return 3;
        else if (type.equals(VillagerType.SAVANNA)) return 2;
        else if (type.equals(VillagerType.DESERT)) return 1;
        else return 0;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return MobEntity.createMobAttributes()
                .add(EntityAttributes.MAX_HEALTH, GuardVillagersConfig.healthModifier)
                .add(EntityAttributes.MOVEMENT_SPEED, GuardVillagersConfig.speedModifier)
                .add(EntityAttributes.ATTACK_DAMAGE, 1.0D)
                .add(EntityAttributes.FOLLOW_RANGE, GuardVillagersConfig.followRangeModifier);
    }

    @Nullable
    @Override
    public EntityData initialize(ServerWorldAccess world, LocalDifficulty difficulty, SpawnReason spawnReason, @Nullable EntityData dataTracker) {
        this.setPersistent();
        int type = GuardEntity.getRandomTypeForBiome(world, this.getBlockPos());
        if (dataTracker instanceof GuardEntity.GuardEntityData) {
            type = ((GuardEntity.GuardEntityData) dataTracker).variantData;
            dataTracker = new GuardEntity.GuardEntityData(type);
        }
        this.setGuardEntityVariant(type);
        Random random = world.getRandom();
        this.initEquipment(random, difficulty);
        return super.initialize(world, difficulty, spawnReason, dataTracker);
    }

    @Override
    protected void pushAway(Entity entity) {
        if (entity instanceof PathAwareEntity living) {
            boolean attackTargets = living.getTarget() instanceof VillagerEntity || living.getTarget() instanceof IronGolemEntity || living.getTarget() instanceof GuardEntity;
            if (attackTargets) this.setTarget(living);
        }
        super.pushAway(entity);
    }

    @Nullable
    public BlockPos getPatrolPos() {
        return this.dataTracker.get(GUARD_POS).orElse(null);
    }

    @Nullable
    public void setPatrolPos(BlockPos position) {
        this.dataTracker.set(GUARD_POS, Optional.ofNullable(position));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return GuardVillagers.GUARD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        if (this.isBlocking()) {
            return SoundEvents.ITEM_SHIELD_BLOCK.value();
        } else {
            return GuardVillagers.GUARD_HURT;
        }
    }

    @Override
    protected SoundEvent getDeathSound() {
        return GuardVillagers.GUARD_DEATH;
    }

    @Override
    protected void dropEquipment(ServerWorld world, DamageSource source, boolean causedByPlayer) {
        for (int i = 0; i < this.guardInventory.size(); ++i) {
            ItemStack itemstack = this.guardInventory.getStack(i);
            Random random = world.getRandom();
            if (!itemstack.isEmpty() && !EnchantmentHelper.hasAnyEnchantmentsWith(itemstack, net.minecraft.component.EnchantmentEffectComponentTypes.PREVENT_EQUIPMENT_DROP) && random.nextFloat() < GuardVillagersConfig.chanceToDropEquipment)
                this.dropStack(world, itemstack);
        }
    }

    @Override
    protected void readCustomData(ReadView view) {
        super.readCustomData(view);
        view.read("Owner", net.minecraft.util.Uuids.STRICT_CODEC).ifPresent(uuid -> {
            try {
                this.setOwnerId(uuid);
            } catch (Throwable t) {
                this.setOwnerId(null);
            }
        });
        this.setGuardEntityVariant(view.getInt("Type", 0));
        this.kickTicks = view.getInt("KickTicks", 0);
        this.setFollowing(view.getBoolean("Following", false));
        this.interacting = view.getBoolean("Interacting", false);
        this.setPatrolling(view.getBoolean("Patrolling", false));
        this.shieldCoolDown = view.getInt("KickCooldown", 0);
        this.kickCoolDown = view.getInt("ShieldCooldown", 0);
        this.lastGossipDecayTime = view.getLong("LastGossipDecay", 0L);
        this.lastGossipTime = view.getLong("LastGossipTime", 0L);
        this.spawnWithArmor = view.getBoolean("SpawnWithArmor", false);
        if (view.getOptionalInt("PatrolPosX").isPresent()) {
            int x = view.getInt("PatrolPosX", 0);
            int y = view.getInt("PatrolPosY", 0);
            int z = view.getInt("PatrolPosZ", 0);
            this.dataTracker.set(GUARD_POS, Optional.of(new BlockPos(x, y, z)));
        }
        view.read("Gossips", VillagerGossips.CODEC).ifPresent(this.gossips::add);
        for (StackWithSlot slot : view.getTypedListView("Inventory", StackWithSlot.CODEC)) {
            if (slot.isValidSlot(this.guardInventory.size())) {
                this.guardInventory.setStack(slot.slot(), slot.stack());
            }
        }
        if (!getEntityWorld().isClient()) this.readAngerFromData(getEntityWorld(), view);
    }

    @Override
    protected void consumeItem() {
        if (this.isUsingItem()) {
            Hand hand = this.getActiveHand();
            if (!this.activeItemStack.equals(this.getStackInHand(hand))) {
                this.stopUsingItem();
            } else {
                if (!this.activeItemStack.isEmpty() && this.isUsingItem()) {
                    ItemStack itemStack = this.activeItemStack.finishUsing(this.getEntityWorld(), this);
                    if (itemStack != this.activeItemStack) {
                        this.setStackInHand(hand, itemStack);
                    }
                    if (this.activeItemStack.get(net.minecraft.component.DataComponentTypes.FOOD) == null) this.activeItemStack.decrement(1);
                    this.stopUsingItem();
                }

            }
        }
    }

    @Override
    protected void writeCustomData(WriteView view) {
        super.writeCustomData(view);
        view.putInt("Type", this.getGuardEntityVariant());
        view.putInt("KickTicks", this.kickTicks);
        view.putInt("ShieldCooldown", this.shieldCoolDown);
        view.putInt("KickCooldown", this.kickCoolDown);
        view.putBoolean("Following", this.isFollowing());
        view.putBoolean("Interacting", this.interacting);
        view.putBoolean("Patrolling", this.isPatrolling());
        view.putBoolean("SpawnWithArmor", this.spawnWithArmor);
        view.putLong("LastGossipTime", this.lastGossipTime);
        view.putLong("LastGossipDecay", this.lastGossipDecayTime);
        view.putNullable("Owner", net.minecraft.util.Uuids.STRICT_CODEC, this.getOwnerId());
        WriteView.ListAppender<StackWithSlot> inv = view.getListAppender("Inventory", StackWithSlot.CODEC);
        for (int i = 0; i < this.guardInventory.size(); ++i) {
            ItemStack stack = this.guardInventory.getStack(i);
            if (!stack.isEmpty()) inv.add(new StackWithSlot(i, stack));
        }
        if (this.getPatrolPos() != null) {
            view.putInt("PatrolPosX", this.getPatrolPos().getX());
            view.putInt("PatrolPosY", this.getPatrolPos().getY());
            view.putInt("PatrolPosZ", this.getPatrolPos().getZ());
        }
        view.put("Gossips", VillagerGossips.CODEC, this.gossips);
        this.writeAngerToData(view);
    }

    private void maybeDecayGossip() {
        long i = getEntityWorld().getTime();
        if (this.lastGossipDecayTime == 0L) {
            this.lastGossipDecayTime = i;
        } else if (i >= this.lastGossipDecayTime + 24000L) {
            this.gossips.decay();
            this.lastGossipDecayTime = i;
        }
    }

    @Override
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        switch (slot) {
            case HEAD:
                return this.guardInventory.getStack(0);
            case CHEST:
                return this.guardInventory.getStack(1);
            case LEGS:
                return this.guardInventory.getStack(2);
            case FEET:
                return this.guardInventory.getStack(3);
            case OFFHAND:
                return this.guardInventory.getStack(4);
            case MAINHAND:
                return this.guardInventory.getStack(5);
        }
        return ItemStack.EMPTY;
    }


    public VillagerGossips getGossips() {
        return this.gossips;
    }

    public int getPlayerEntityReputation(PlayerEntity player) {
        return this.gossips.getReputationFor(player.getUuid(), (gossipType) -> true);
    }

    @Nullable
    public LivingEntity getOwner() {
        try {
            UUID uuid = this.getOwnerId();
            boolean heroOfTheVillage = uuid != null && getEntityWorld().getPlayerByUuid(uuid) != null && getEntityWorld().getPlayerByUuid(uuid).hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE);
            return uuid == null || (getEntityWorld().getPlayerByUuid(uuid) != null && (!heroOfTheVillage && GuardVillagersConfig.followHero) || !GuardVillagersConfig.followHero && getEntityWorld().getPlayerByUuid(uuid) == null) ? null : getEntityWorld().getPlayerByUuid(uuid);
        } catch (IllegalArgumentException illegalargumentexception) {
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

    public void setOwnerId(@Nullable UUID p_184754_1_) {
        this.ownerId = p_184754_1_;
    }

    @Override
    public boolean tryAttack(ServerWorld world, Entity target) {
        if (this.isKicking()) {
            ((LivingEntity) target).takeKnockback(1.0F, MathHelper.sin(this.getYaw() * ((float) Math.PI / 180F)), (-MathHelper.cos(this.getYaw() * ((float) Math.PI / 180F))));
            this.kickTicks = 10;
            world.sendEntityStatus(this, (byte) 4);
            this.lookAtEntity(target, 90.0F, 90.0F);
        }
        ItemStack hand = this.getMainHandStack();
        hand.damage(1, this, EquipmentSlot.MAINHAND);
        return super.tryAttack(world, target);
    }

    @Override
    public void handleStatus(byte status) {
        if (status == 4) {
            this.kickTicks = 10;
        } else {
            super.handleStatus(status);
        }
    }

    @Override
    public boolean isImmobile() {
        return this.interacting || super.isImmobile();
    }

    @Override
    public void onDeath(DamageSource damageSource) {
        if ((getEntityWorld().getDifficulty() == Difficulty.NORMAL || getEntityWorld().getDifficulty() == Difficulty.HARD) && damageSource.getAttacker() instanceof ZombieEntity) {
            ZombieVillagerEntity zombieguard = this.convertTo(EntityType.ZOMBIE_VILLAGER, EntityConversionContext.create(this, true, true), SpawnReason.CONVERSION, (ZombieVillagerEntity z) -> {
                z.initialize((ServerWorldAccess) getEntityWorld(), ((ServerWorldAccess) getEntityWorld()).getLocalDifficulty(z.getBlockPos()), SpawnReason.CONVERSION, new ZombieEntity.ZombieData(false, true));
                if (!this.isSilent()) getEntityWorld().syncWorldEvent(null, 1026, this.getBlockPos(), 0);
            });
            if (zombieguard == null || (getEntityWorld().getDifficulty() != Difficulty.HARD && this.random.nextBoolean())) {
                return;
            }
        }
        super.onDeath(damageSource);
    }

    public ItemStack eatFood(World world, ItemStack stack) {
        var food = stack.get(net.minecraft.component.DataComponentTypes.FOOD);
        if (food != null) {
            this.heal(food.nutrition());
        }
        world.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.ENTITY_PLAYER_BURP, SoundCategory.PLAYERS, 0.5F, world.getRandom().nextFloat() * 0.1F + 0.9F);
        return stack;
    }

    public void tickMovement() {
        if (this.kickTicks > 0)
            --this.kickTicks;
        if (this.kickCoolDown > 0)
            --this.kickCoolDown;
        if (this.shieldCoolDown > 0)
            --this.shieldCoolDown;
        if (this.getHealth() < this.getMaxHealth() && this.age % 200 == 0) {
            this.heal(GuardVillagersConfig.amountOfHealthRegenerated);
        }
        if (spawnWithArmor && this.getEntityWorld() instanceof ServerWorld serverWorld) {
            for (EquipmentSlot equipmentslottype : EquipmentSlot.values()) {
                for (ItemStack stack : this.getStacksFromLootTable(equipmentslottype, serverWorld)) {
                    this.equipStack(equipmentslottype, stack);
                }
            }
            this.spawnWithArmor = false;
        }
        if (!getEntityWorld().isClient()) this.tickAngerLogic((ServerWorld) getEntityWorld(), true);
        this.tickHandSwing();
        super.tickMovement();
    }

    public void tick() {
        this.maybeDecayGossip();
        super.tick();
    }

    public boolean isInAttackRange(LivingEntity entity) {
        AttackRangeComponent lv = this.getAttackRange();
        double d = lv.getEffectiveMaxRange(this) * Math.sqrt(3.55);
        double e = lv.getEffectiveMinRange(this);
        Box box = entity.getBoundingBox();
        return this.getAttackBox(d).intersects(box) && (e <= 0.0 || !this.getAttackBox(e).intersects(box));
    }

    @Override
    protected void takeShieldHit(ServerWorld world, LivingEntity entityIn) {
        super.takeShieldHit(world, entityIn);
        if (entityIn.getMainHandStack().getItem() instanceof AxeItem) this.disableShield(true);
    }

    public void damageShield(float amount) {
        if (this.activeItemStack.getItem() == Items.SHIELD) {
            if (amount >= 3.0F) {
                int i = 1 + MathHelper.floor(amount);
                Hand hand = this.getActiveHand();
                this.activeItemStack.damage(i, this, hand.getEquipmentSlot());
                if (this.activeItemStack.isEmpty()) {
                    if (hand == Hand.MAIN_HAND) {
                        this.equipStack(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
                    } else {
                        this.equipStack(EquipmentSlot.OFFHAND, ItemStack.EMPTY);
                    }
                    this.activeItemStack = ItemStack.EMPTY;
                    this.playSound(SoundEvents.ITEM_SHIELD_BREAK.value(), 0.8F, 0.8F + getEntityWorld().getRandom().nextFloat() * 0.4F);
                }
            }
        }
    }

    @Override
    public void setCurrentHand(Hand hand) {
        super.setCurrentHand(hand);
        ItemStack itemstack = this.getStackInHand(hand);
        if (itemstack.getItem() == Items.SHIELD) { // See above

            EntityAttributeInstance modifiableattributeinstance = this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED);
            modifiableattributeinstance.removeModifier(USE_ITEM_SPEED_PENALTY);
            modifiableattributeinstance.addTemporaryModifier(USE_ITEM_SPEED_PENALTY);
        }
    }

    @Override
    public void stopUsingItem() {
        super.stopUsingItem();
        if (this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).hasModifier(USE_ITEM_SPEED_PENALTY_ID))
            this.getAttributeInstance(EntityAttributes.MOVEMENT_SPEED).removeModifier(USE_ITEM_SPEED_PENALTY_ID);
    }

    private static boolean isArmorStack(ItemStack stack) {
        var c = stack.get(net.minecraft.component.DataComponentTypes.EQUIPPABLE);
        return c != null && c.slot().isArmorSlot();
    }

    public void disableShield(boolean increase) {
        var enchantments = this.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
        int eff = EnchantmentHelper.getLevel(enchantments.getEntry(enchantments.getValueOrThrow(Enchantments.EFFICIENCY)), this.getMainHandStack());
        float chance = 0.25F + (float) eff * 0.05F;
        if (increase) chance += 0.75;
        if (this.random.nextFloat() < chance) {
            this.shieldCoolDown = 100;
            this.stopUsingItem();
            getEntityWorld().sendEntityStatus(this, (byte) 30);
        }
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        super.initDataTracker(builder);
        builder.add(GUARD_VARIANT, 0);
        builder.add(DATA_CHARGING_STATE, false);
        builder.add(KICKING, false);
        builder.add(FOLLOWING, false);
        builder.add(GUARD_POS, Optional.<BlockPos>empty());
        builder.add(PATROLLING, false);
        builder.add(RUNNING_TO_EAT, false);
    }

    public boolean isCharging() {
        return this.dataTracker.get(DATA_CHARGING_STATE);
    }

    public void setChargingCrossbow(boolean charging) {
        this.dataTracker.set(DATA_CHARGING_STATE, charging);
    }

    public boolean isKicking() {
        return this.dataTracker.get(KICKING);
    }

    public void setKicking(boolean kicking) {
        this.dataTracker.set(KICKING, kicking);
    }

    @Override
    protected void initEquipment(Random random, LocalDifficulty localDifficulty) {
        this.setEquipmentDropChance(EquipmentSlot.MAINHAND, 1.0F);
        this.setEquipmentDropChance(EquipmentSlot.OFFHAND, 1.0F);
        this.spawnWithArmor = true;
    }

    public List<ItemStack> getStacksFromLootTable(EquipmentSlot slot, ServerWorld serverWorld) {
        if (EQUIPMENT_SLOT_ITEMS.containsKey(slot)) {
            LootTable loot = serverWorld.getServer().getReloadableRegistries().getLootTable(net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.LOOT_TABLE, EQUIPMENT_SLOT_ITEMS.get(slot)));
            LootWorldContext ctx = new LootWorldContext.Builder(serverWorld).add(LootContextParameters.THIS_ENTITY, this).add(LootContextParameters.ORIGIN, this.getEntityPos()).build(GuardEntityLootTables.SLOT);
            return loot.generateLoot(ctx);
        }
        return Collections.emptyList();
    }

    public int getGuardEntityVariant() {
        return this.dataTracker.get(GUARD_VARIANT);
    }

    public void setGuardEntityVariant(int typeId) {
        this.dataTracker.set(GUARD_VARIANT, typeId);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(0, new KickGoal(this));
        this.goalSelector.add(0, new GuardEatFoodGoal(this));
        this.goalSelector.add(0, new RaiseShieldGoal(this));
        this.goalSelector.add(1, new GuardRunToEatGoal(this));
        this.goalSelector.add(2, new RangedCrossbowAttackPassiveGoal<>(this, 1.0D, 8.0F));
        this.goalSelector.add(2, new RangedBowAttackPassiveGoal<>(this, 0.5D, 20, 15.0F));
        this.goalSelector.add(2, new GuardEntityMeleeGoal(this, 0.8D, true));
        this.goalSelector.add(3, new GuardEntity.FollowHeroGoal(this));
        if (GuardVillagersConfig.guardEntitysRunFromPolarBears)
            this.goalSelector.add(3, new FleeEntityGoal<>(this, PolarBearEntity.class, 12.0F, 1.0D, 1.2D));
        this.goalSelector.add(3, new WanderAroundPointOfInterestGoal(this, 0.5D, false));
        this.goalSelector.add(3, new IronGolemWanderAroundGoal(this, 0.5D));
        this.goalSelector.add(3, new MoveThroughVillageGoal(this, 0.5D, false, 4, () -> false));
        if (GuardVillagersConfig.guardEntitysOpenDoors) this.goalSelector.add(3, new GuardInteractDoorGoal(this, true));
        if (GuardVillagersConfig.guardEntityFormation) this.goalSelector.add(5, new FollowShieldGuards(this));
        if (GuardVillagersConfig.clericHealing) this.goalSelector.add(6, new RunToClericGoal(this));
        if (GuardVillagersConfig.armorerRepairGuardEntityArmor)
            this.goalSelector.add(6, new ArmorerRepairGuardArmorGoal(this));
        this.goalSelector.add(4, new WalkBackToCheckPointGoal(this, 0.5D));
        this.goalSelector.add(5, new WanderAroundFarGoal(this, 0.5D));
        this.goalSelector.add(8, new LookAtEntityGoal(this, MerchantEntity.class, 8.0F));
        this.goalSelector.add(8, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(8, new GuardLookAtAndStopMovingWhenBeingTheInteractionTarget(this));
        this.targetSelector.add(5, new GuardEntity.DefendVillageGuardEntityGoal(this));
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, RavagerEntity.class, true));
        this.targetSelector.add(2, (new RevengeGoal(this, GuardEntity.class, IronGolemEntity.class)).setGroupRevenge());
        this.targetSelector.add(2, new ActiveTargetGoal<>(this, WitchEntity.class, true));
        this.targetSelector.add(3, new HeroHurtByTargetGoal(this));
        this.targetSelector.add(3, new HeroHurtTargetGoal(this));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, RaiderEntity.class, true));
        if (GuardVillagersConfig.attackAllMobs)
            this.targetSelector.add(3, new ActiveTargetGoal<MobEntity>(this, MobEntity.class, 5, true, true, (target, world) -> target instanceof Monster && !GuardVillagersConfig.mobBlackList.contains(target.getSavedEntityId())));
        this.targetSelector.add(3, new ActiveTargetGoal<>(this, PlayerEntity.class, 10, true, false, this::shouldAngerAt));
        this.targetSelector.add(4, new ActiveTargetGoal<>(this, ZombieEntity.class, true));
        this.targetSelector.add(4, new UniversalAngerGoal<>(this, false));
    }

    public boolean canBeLeashedBy(PlayerEntity player) {
        return false;
    }

    @Override
    public void shootAt(LivingEntity target, float pullProgress) {
        this.shieldCoolDown = 8;
        if (this.getMainHandStack().getItem() instanceof CrossbowItem)
            this.shoot(this, 6.0F);
        if (this.getMainHandStack().getItem() instanceof BowItem) {
            ItemStack bowStack = this.getStackInHand(ProjectileUtil.getHandPossiblyHolding(this, Items.BOW));
            ItemStack itemStack = this.getProjectileType(bowStack);
            ItemStack hand = this.getActiveItem();
            PersistentProjectileEntity persistentProjectileEntity = ProjectileUtil.createArrowProjectile(this, itemStack, pullProgress, bowStack);
            var enchantments = this.getRegistryManager().getOrThrow(net.minecraft.registry.RegistryKeys.ENCHANTMENT);
            int powerLevel = EnchantmentHelper.getLevel(enchantments.getEntry(enchantments.getValueOrThrow(Enchantments.POWER)), bowStack);
            if (powerLevel > 0) {
                double baseDamage = pullProgress * 2.0 + this.getEntityWorld().getDifficulty().getId() * 0.11;
                persistentProjectileEntity.setDamage(baseDamage + (double) powerLevel * 0.5D + 0.5D);
            }
            if (EnchantmentHelper.getLevel(enchantments.getEntry(enchantments.getValueOrThrow(Enchantments.FLAME)), bowStack) > 0)
                persistentProjectileEntity.setFireTicks(100);
            double d = target.getX() - this.getX();
            double e = target.getBodyY(0.3333333333333333D) - persistentProjectileEntity.getY();
            double f = target.getZ() - this.getZ();
            double g = Math.sqrt(d * d + f * f);
            persistentProjectileEntity.setVelocity(d, e + g * 0.20000000298023224D, f, 1.6F, (float) (14 - this.getEntityWorld().getDifficulty().getId() * 4));
            this.playSound(SoundEvents.ENTITY_SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            this.getEntityWorld().spawnEntity(persistentProjectileEntity);
            hand.damage(1, this, EquipmentSlot.MAINHAND);
        }
    }

    @Override
    public void equipStack(EquipmentSlot slotIn, ItemStack stack) {
        super.equipStack(slotIn, stack);
        switch (slotIn) {
            case CHEST:
                if (this.guardInventory.getStack(1).isEmpty())
                    this.guardInventory.setStack(1, stack);
                break;
            case FEET:
                if (this.guardInventory.getStack(3).isEmpty())
                    this.guardInventory.setStack(3, stack);
                break;
            case HEAD:
                if (this.guardInventory.getStack(0).isEmpty())
                    this.guardInventory.setStack(0, stack);
                break;
            case LEGS:
                if (this.guardInventory.getStack(2).isEmpty())
                    this.guardInventory.setStack(2, stack);
                break;
            case MAINHAND:
                this.guardInventory.setStack(5, stack);
                break;
            case OFFHAND:
                this.guardInventory.setStack(4, stack);
                break;
        }
    }

    public int getGuardVariant() {
        return this.dataTracker.get(GUARD_VARIANT);
    }


    @Override
    public ItemStack getProjectileType(ItemStack shootable) {
        if (shootable.getItem() instanceof RangedWeaponItem) {
            Predicate<ItemStack> predicate = ((RangedWeaponItem) shootable.getItem()).getHeldProjectiles();
            ItemStack itemstack = RangedWeaponItem.getHeldProjectile(this, predicate);
            return itemstack.isEmpty() ? new ItemStack(Items.ARROW) : itemstack;
        } else {
            return ItemStack.EMPTY;
        }
    }

    public int getKickTicks() {
        return this.kickTicks;
    }

    public boolean isFollowing() {
        return this.dataTracker.get(FOLLOWING);
    }

    public void setFollowing(boolean following) {
        this.dataTracker.set(FOLLOWING, following);
    }

    @Override
    public boolean canTarget(LivingEntity target) {
        return !GuardVillagersConfig.mobBlackList.contains(target.getSavedEntityId()) && !target.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !this.isOwner(target) && !(target instanceof VillagerEntity) && !(target instanceof IronGolemEntity) && !(target instanceof GuardEntity) && super.canTarget(target);
    }

    @Override
    public void tickRiding() {
        super.tickRiding();
        if (this.getVehicle() instanceof PathAwareEntity creatureentity) {
            this.bodyYaw = creatureentity.bodyYaw;
        }
    }

    public double getHeightOffset() {
        return -0.35D;
    }

    public void postShoot() {
        this.despawnCounter = 0;
    }

    @Override
    public void setTarget(LivingEntity entity) {
        if (entity instanceof GuardEntity || entity instanceof VillagerEntity || entity instanceof IronGolemEntity)
            return;
        super.setTarget(entity);
    }


    public void gossip(VillagerEntity villager, long gameTime) {
        if ((gameTime < this.lastGossipTime || gameTime >= this.lastGossipTime + 1200L) && (gameTime < villager.gossipStartTime || gameTime >= villager.gossipStartTime + 1200L)) {
            this.gossips.shareGossipFrom(villager.getGossip(), this.random, 10);
            this.lastGossipTime = gameTime;
            villager.gossipStartTime = gameTime;
        }
    }

    public void setCharging(boolean charging) {

    }

    public void shoot(LivingEntity arg0, ItemStack arg1, ProjectileEntity arg2, float arg3) {
        this.shoot(arg0, 1.6F);
    }

    @Override
    public void knockback(LivingEntity entityIn) {
        if (this.isKicking()) {
            this.setKicking(false);
        }
        super.knockback(this);
    }

    @Override
    protected ActionResult interactMob(PlayerEntity player, Hand hand) {
        boolean configValues = player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.setGuardPatrolHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv && GuardVillagersConfig.setGuardPatrolHotv || this.getPlayerEntityReputation(player) >= GuardVillagersConfig.reputationRequirement || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !GuardVillagersConfig.giveGuardStuffHotv && !GuardVillagersConfig.setGuardPatrolHotv || this.getOwnerId() != null && this.getOwnerId().equals(player.getUuid());
        boolean inventoryRequirements = !player.shouldCancelInteraction();
        if (inventoryRequirements) {
            if (this.getTarget() != player && this.canMoveVoluntarily() && configValues) {
                if (player instanceof ServerPlayerEntity) {
                    this.openGui((ServerPlayerEntity) player);
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.CONSUME;
        }
        return super.interactMob(player, hand);
    }

    @Override
    public void onInteractionWith(EntityInteraction interaction, Entity entity) {

    }

    @Override
    public void onInventoryChanged(Inventory sender) {

    }


    @Override
    public void damageArmor(DamageSource damageSource, float damage) {
        if (damage >= 0.0F) {
            damage = damage / 4.0F;
            if (damage < 1.0F) {
                damage = 1.0F;
            }
            for (int i = 0; i < this.guardInventory.size(); ++i) {
                ItemStack itemstack = this.guardInventory.getStack(i);
                if ((!damageSource.isOf(DamageTypes.ON_FIRE) || itemstack.takesDamageFrom(damageSource)) && isArmorStack(itemstack)) {
                    var eq = itemstack.get(net.minecraft.component.DataComponentTypes.EQUIPPABLE);
                    if (eq != null) {
                        itemstack.damage((int) damage, this, eq.slot());
                    }
                }
            }
        }
    }

    @Override
    public void onStruckByLightning(ServerWorld world, LightningEntity lightning) {
        if (world.getDifficulty() != Difficulty.PEACEFUL) {
            WitchEntity witchentity = EntityType.WITCH.create(world, SpawnReason.CONVERSION);
            if (witchentity == null) return;
            witchentity.copyPositionAndRotation(this);
            witchentity.initialize(world, world.getLocalDifficulty(witchentity.getBlockPos()), SpawnReason.CONVERSION, null);
            witchentity.setAiDisabled(this.isAiDisabled());
            witchentity.setCustomName(this.getCustomName());
            witchentity.setCustomNameVisible(this.isCustomNameVisible());
            witchentity.setPersistent();
            world.spawnNewEntityAndPassengers(witchentity);
            this.discard();
        } else {
            super.onStruckByLightning(world, lightning);
        }
    }

    @Override
    public LazyEntityReference<LivingEntity> getAngryAt() {
        return this.angryAt;
    }

    @Override
    public void setAngryAt(LazyEntityReference<LivingEntity> ref) {
        this.angryAt = ref;
    }

    @Override
    public long getAngerEndTime() {
        return this.angerEndTime;
    }

    @Override
    public void setAngerEndTime(long time) {
        this.angerEndTime = time;
    }

    @Override
    public void chooseRandomAngerTime() {
        this.setAngerDuration(angerTime.get(random));
    }

    public void openGui(ServerPlayerEntity player) {
        this.setOwnerId(player.getUuid());
        if (player.currentScreenHandler != player.playerScreenHandler) {
            player.closeHandledScreen();
        }
        this.interacting = true;
        if (!this.getEntityWorld().isClient()) {
            player.openHandledScreen(new GuardScreenHandlerFactory());
        }
    }

    public void setGuardVariant(int i) {
        this.dataTracker.set(GUARD_VARIANT, i);
    }

    private class GuardScreenHandlerFactory implements ExtendedScreenHandlerFactory<Integer> {
        private GuardEntity guard() {
            return GuardEntity.this;
        }

        @Override
        public Integer getScreenOpeningData(ServerPlayerEntity player) {
            return this.guard().getId();
        }

        @Override
        public Text getDisplayName() {
            return this.guard().getDisplayName();
        }

        @Override
        public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
            var guardInv = this.guard().guardInventory;
            return new GuardVillagerScreenHandler(syncId, inv, guardInv, this.guard());
        }
    }

    public boolean isEating() {
        return GuardEatFoodGoal.isConsumable(this.getActiveItem()) && this.isUsingItem();
    }

    public boolean isPatrolling() {
        return this.dataTracker.get(PATROLLING);
    }

    public void setPatrolling(boolean patrolling) {
        this.dataTracker.set(PATROLLING, patrolling);
    }

    @Override
    public boolean canUseRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem || super.canUseRangedWeapon(stack);
    }

    public static class GuardEntityData implements EntityData {
        public final int variantData;

        public GuardEntityData(int type) {
            this.variantData = type;
        }
    }

    public static class DefendVillageGuardEntityGoal extends TrackTargetGoal {
        private final GuardEntity guard;
        private LivingEntity villageAggressorTarget;

        public DefendVillageGuardEntityGoal(GuardEntity guardIn) {
            super(guardIn, false, true);
            this.guard = guardIn;
            this.setControls(EnumSet.of(Goal.Control.TARGET, Goal.Control.MOVE));
        }

        @Override
        public boolean canStart() {
            Box box = this.guard.getBoundingBox().expand(10.0D, 8.0D, 10.0D);
            List<VillagerEntity> list = guard.getEntityWorld().getNonSpectatingEntities(VillagerEntity.class, box);
            List<PlayerEntity> list1 = guard.getEntityWorld().getNonSpectatingEntities(PlayerEntity.class, box);
            for (VillagerEntity villager : list) {
                for (PlayerEntity player : list1) {
                    int i = villager.getReputation(player);
                    if (i <= GuardVillagersConfig.reputationRequirementToBeAttacked) {
                        this.villageAggressorTarget = player;
                    }
                }
            }
            return villageAggressorTarget != null && !villageAggressorTarget.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE) && !this.villageAggressorTarget.isSpectator() && !((PlayerEntity) this.villageAggressorTarget).isCreative();
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
            this.setControls(EnumSet.of(Goal.Control.MOVE, Goal.Control.LOOK));
        }

        @Override
        public void tick() {
            if (guard.getOwner() != null && guard.getOwner().distanceTo(guard) > 3.0D) {
                guard.getNavigation().startMovingTo(guard.getOwner(), 0.7D);
                guard.getLookControl().lookAt(guard.getOwner());
            } else {
                guard.getNavigation().stop();
            }
        }

        @Override
        public boolean shouldContinue() {
            return this.canStart();
        }

        @Override
        public boolean canStart() {
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
        public boolean canStart() {
            return !(this.guard.getMainHandStack().getItem() instanceof CrossbowItem) && this.guard.getTarget() != null && !this.guard.isEating() && super.canStart();
        }

        @Override
        public boolean shouldContinue() {
            return super.shouldContinue() && this.guard.getTarget() != null;
        }

        @Override
        public void tick() {
            LivingEntity target = guard.getTarget();
            if (target != null) {
                if (target.distanceTo(guard) <= 3.0D && !guard.isBlocking()) {
                    guard.getMoveControl().strafeTo(-2.0F, 0.0F);
                    guard.lookAtEntity(target, 30.0F, 30.0F);
                }
                if (this.path != null && target.distanceTo(guard) <= 2.0D) guard.getNavigation().stop();
                super.tick();
            }
        }

        @Override
        protected boolean canAttack(LivingEntity target) {
            return this.isCooledDown() && this.guard.isInAttackRange(target) && this.guard.getVisibilityCache().canSee(target);
        }

        @Override
        protected void attack(LivingEntity enemy) {
            if (this.canAttack(enemy)) {
                this.resetCooldown();
                this.guard.stopUsingItem();
                if (guard.shieldCoolDown == 0) this.guard.shieldCoolDown = 8;
                this.guard.swingHand(Hand.MAIN_HAND);
                this.guard.tryAttack(MeleeAttackGoal.getServerWorld(this.guard), enemy);
            }
        }
    }
}
