package dev.sterner.guardvillagers.common.entity;

import dev.sterner.guardvillagers.GuardVillagers;
import net.minecraft.loot.context.LootContextTypes;
import net.minecraft.util.Identifier;
import net.minecraft.util.context.ContextType;

public class GuardEntityLootTables {
    public static final ContextType SLOT = LootContextTypes.EQUIPMENT;

    public static final Identifier GUARD_MAIN_HAND = Identifier.of(GuardVillagers.MODID, "entities/guard_main_hand");
    public static final Identifier GUARD_OFF_HAND = Identifier.of(GuardVillagers.MODID, "entities/guard_off_hand");
    public static final Identifier GUARD_HELMET = Identifier.of(GuardVillagers.MODID, "entities/guard_helmet");
    public static final Identifier GUARD_CHEST = Identifier.of(GuardVillagers.MODID, "entities/guard_chestplate");
    public static final Identifier GUARD_LEGGINGS = Identifier.of(GuardVillagers.MODID, "entities/guard_legs");
    public static final Identifier GUARD_FEET = Identifier.of(GuardVillagers.MODID, "entities/guard_feet");
}
