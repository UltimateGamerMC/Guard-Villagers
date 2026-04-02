package dev.sterner.guardvillagers;

import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.network.GuardFollowPacket;
import dev.sterner.guardvillagers.common.network.GuardPatrolPacket;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import eu.midnightdust.lib.config.MidnightConfig;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.Level;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class GuardVillagers implements ModInitializer {
    public static final String MODID = "guardvillagers";

    public static final MenuType<GuardVillagerScreenHandler> GUARD_SCREEN_HANDLER = Registry.register(
        BuiltInRegistries.MENU,
        Identifier.fromNamespaceAndPath(MODID, "guard_screen"),
        new ExtendedMenuType<>(GuardVillagerScreenHandler::create, ByteBufCodecs.VAR_INT)
    );

    public static final ResourceKey<EntityType<?>> GUARD_ENTITY_KEY = ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MODID, "guard"));

    public static final EntityType<GuardEntity> GUARD_VILLAGER = Registry.register(
        BuiltInRegistries.ENTITY_TYPE,
        Identifier.fromNamespaceAndPath(MODID, "guard"),
        EntityType.Builder.of(GuardEntity::new, MobCategory.CREATURE).sized(0.6f, 1.8f).build(GUARD_ENTITY_KEY)
    );

    public static final Item GUARD_SPAWN_EGG = Registry.register(
        BuiltInRegistries.ITEM,
        Identifier.fromNamespaceAndPath(MODID, "guard_spawn_egg"),
        new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, "guard_spawn_egg"))).spawnEgg(GUARD_VILLAGER))
    );

    public static InteractionHand getHandWith(LivingEntity livingEntity, Predicate<Item> itemPredicate) {
        return itemPredicate.test(livingEntity.getMainHandItem().getItem()) ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
    }

    public static SoundEvent GUARD_AMBIENT = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "entity.guard.ambient"));
    public static SoundEvent GUARD_HURT = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "entity.guard.hurt"));
    public static SoundEvent GUARD_DEATH = SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(MODID, "entity.guard.death"));

    @Override
    public void onInitialize() {
        MidnightConfig.init(MODID, GuardVillagersConfig.class);
        FabricDefaultAttributeRegistry.register(GUARD_VILLAGER, GuardEntity.createAttributes());

        Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.fromNamespaceAndPath(MODID, "entity.guard.ambient"), GUARD_AMBIENT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.fromNamespaceAndPath(MODID, "entity.guard.hurt"), GUARD_HURT);
        Registry.register(BuiltInRegistries.SOUND_EVENT, Identifier.fromNamespaceAndPath(MODID, "entity.guard.death"), GUARD_DEATH);

        PayloadTypeRegistry.serverboundPlay().register(GuardFollowPacket.TYPE, GuardFollowPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GuardPatrolPacket.TYPE, GuardPatrolPacket.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GuardFollowPacket.TYPE, GuardFollowPacket::handle);
        ServerPlayNetworking.registerGlobalReceiver(GuardPatrolPacket.TYPE, GuardPatrolPacket::handle);

        CreativeModeTabEvents.modifyOutputEvent(ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.withDefaultNamespace("spawn_eggs")))
            .register(output -> output.accept(GUARD_SPAWN_EGG, CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS));

        ServerLivingEntityEvents.ALLOW_DAMAGE.register(this::onDamage);
        UseEntityCallback.EVENT.register(this::villagerConvert);
    }

    private boolean onDamage(LivingEntity entity, net.minecraft.world.damagesource.DamageSource source, float amount) {
        Entity attacker = source.getEntity();
        if (entity == null || attacker == null)
            return true;
        boolean shouldDamage = true;
        boolean isVillager = entity.getType() == EntityType.VILLAGER || entity.getType() == GuardVillagers.GUARD_VILLAGER;
        boolean isGolem = isVillager || entity.getType() == EntityType.IRON_GOLEM;
        if (isGolem && attacker.getType() == GuardVillagers.GUARD_VILLAGER && !GuardVillagersConfig.guardArrowsHurtVillagers) {
            shouldDamage = false;
        }
        if (isVillager && attacker instanceof Mob) {
            double r = GuardVillagersConfig.guardVillagerHelpRange;
            List<Mob> list = entity.level().getEntitiesOfClass(Mob.class, attacker.getBoundingBox().inflate(r, 5.0D, r));
            for (Mob mob : list) {
                boolean type = mob.getType() == GUARD_VILLAGER || mob.getType() == EntityType.IRON_GOLEM;
                boolean trueSourceGolem = attacker.getType() == GUARD_VILLAGER || attacker.getType() == EntityType.IRON_GOLEM;
                if (!trueSourceGolem && type && mob.getTarget() == null)
                    mob.setTarget((Mob) attacker);
            }
        }
        return shouldDamage;
    }

    private InteractionResult villagerConvert(Player player, Level world, InteractionHand hand, Entity entity, @Nullable EntityHitResult entityHitResult) {
        ItemStack itemStack = player.getItemInHand(hand);
        if ((itemStack.is(ItemTags.SWORDS) || itemStack.getItem() instanceof CrossbowItem) && player.isShiftKeyDown()) {
            if (entityHitResult != null) {
                Entity target = entityHitResult.getEntity();
                if (target instanceof Villager villagerEntity) {
                    if (!villagerEntity.isBaby()) {
                        var profession = villagerEntity.getVillagerData().profession();
                        if (profession.is(VillagerProfession.NONE) || profession.is(VillagerProfession.NITWIT)) {
                            if (!GuardVillagersConfig.convertVillagerIfHaveHotv || player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.convertVillagerIfHaveHotv) {
                                convertVillager(villagerEntity, player, world);
                                if (!player.getAbilities().instabuild)
                                    itemStack.shrink(1);
                                return InteractionResult.SUCCESS;
                            }
                        }
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    private void convertVillager(Villager villagerEntity, Player player, Level world) {
        player.swing(InteractionHand.MAIN_HAND);
        ItemStack itemstack = player.getItemBySlot(EquipmentSlot.MAINHAND);
        GuardEntity guard = GUARD_VILLAGER.create(world, EntitySpawnReason.CONVERSION);
        if (guard == null)
            return;
        if (world.isClientSide()) {
            ParticleOptions particleEffect = ParticleTypes.HAPPY_VILLAGER;
            for (int i = 0; i < 10; ++i) {
                double d0 = villagerEntity.getRandom().nextGaussian() * 0.02D;
                double d1 = villagerEntity.getRandom().nextGaussian() * 0.02D;
                double d2 = villagerEntity.getRandom().nextGaussian() * 0.02D;
                villagerEntity.level().addParticle(particleEffect, villagerEntity.getX() + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getBbWidth() * 2.0F) - (double) villagerEntity.getBbWidth(), villagerEntity.getY() + 0.5D + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getBbWidth()),
                        villagerEntity.getZ() + (double) (villagerEntity.getRandom().nextFloat() * villagerEntity.getBbWidth() * 2.0F) - (double) villagerEntity.getBbWidth(), d0, d1, d2);
            }
        }
        guard.snapTo(villagerEntity.getX(), villagerEntity.getY(), villagerEntity.getZ(), villagerEntity.getYRot(), villagerEntity.getXRot());
        guard.setYHeadRot(villagerEntity.getYHeadRot());
        guard.playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        guard.setItemSlot(EquipmentSlot.MAINHAND, itemstack.copy());
        guard.guardInventory.setItem(5, itemstack.copy());

        int i = GuardEntity.getRandomTypeForBiome(world, guard.blockPosition());
        guard.setGuardVariant(i);
        guard.setPersistenceRequired();
        guard.setCustomName(villagerEntity.getCustomName());
        guard.setCustomNameVisible(villagerEntity.isCustomNameVisible());
        guard.setDropChance(EquipmentSlot.HEAD, 2.0F);
        guard.setDropChance(EquipmentSlot.CHEST, 2.0F);
        guard.setDropChance(EquipmentSlot.FEET, 2.0F);
        guard.setDropChance(EquipmentSlot.LEGS, 2.0F);
        guard.setDropChance(EquipmentSlot.MAINHAND, 2.0F);
        guard.setDropChance(EquipmentSlot.OFFHAND, 2.0F);
        world.addFreshEntity(guard);
        villagerEntity.releasePoi(MemoryModuleType.HOME);
        villagerEntity.releasePoi(MemoryModuleType.JOB_SITE);
        villagerEntity.releasePoi(MemoryModuleType.MEETING_POINT);
        villagerEntity.discard();
    }

    public static boolean hotvChecker(Player player, GuardEntity guard) {
        return player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE) && GuardVillagersConfig.giveGuardStuffHotv
                || !GuardVillagersConfig.giveGuardStuffHotv || guard.getPlayerEntityReputation(player) > GuardVillagersConfig.reputationRequirement && !player.level().isClientSide();
    }
}
