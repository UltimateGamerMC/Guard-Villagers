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

public record GuardPatrolPacket(int id, boolean pressed) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GuardPatrolPacket> TYPE = new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "guard_patrol"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuardPatrolPacket> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, GuardPatrolPacket::id,
        ByteBufCodecs.BOOL, GuardPatrolPacket::pressed,
        GuardPatrolPacket::new
    );

    public static void handle(GuardPatrolPacket packet, ServerPlayNetworking.Context context) {
        ServerPlayer player = context.player();
        var entity = player.level().getEntity(packet.id());
        if (entity instanceof GuardEntity guard) {
            if (packet.pressed()) {
                guard.setPatrolPos(guard.blockPosition());
            }
            guard.setPatrolling(packet.pressed());
        }
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
