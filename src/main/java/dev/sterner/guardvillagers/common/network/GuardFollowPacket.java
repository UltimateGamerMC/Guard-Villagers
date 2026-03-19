package dev.sterner.guardvillagers.common.network;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;

public record GuardFollowPacket(int id) implements CustomPayload {
    public static final CustomPayload.Id<GuardFollowPacket> ID = new CustomPayload.Id<>(Identifier.of(GuardVillagers.MODID, "guard_follow"));
    public static final CustomPayload.Type<RegistryByteBuf, GuardFollowPacket> TYPE = new CustomPayload.Type<>(ID, PacketCodec.tuple(PacketCodecs.VAR_INT, GuardFollowPacket::id, GuardFollowPacket::new));

    public static void handle(GuardFollowPacket packet, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        var entity = player.getEntityWorld().getEntityById(packet.id());
        if (entity instanceof GuardEntity guard) {
            guard.setFollowing(!guard.isFollowing());
            guard.setOwnerId(player.getUuid());
            guard.playSound(SoundEvents.ENTITY_VILLAGER_YES, 1, 1);
        }
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
