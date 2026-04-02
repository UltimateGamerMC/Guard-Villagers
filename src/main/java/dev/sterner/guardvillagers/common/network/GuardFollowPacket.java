package dev.sterner.guardvillagers.common.network;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;

public record GuardFollowPacket(int id) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GuardFollowPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "guard_follow"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuardFollowPacket> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, GuardFollowPacket::id, GuardFollowPacket::new);

    public static void handle(GuardFollowPacket packet, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        var entity = player.level().getEntity(packet.id());
        if (entity instanceof GuardEntity guard) {
            guard.setFollowing(!guard.isFollowing());
            guard.setOwnerId(player.getUUID());
            guard.playSound(SoundEvents.VILLAGER_YES, 1, 1);
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
