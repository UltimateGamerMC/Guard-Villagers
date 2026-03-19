package dev.sterner.guardvillagers.common.network;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public record GuardPatrolPacket(int id, boolean pressed) implements CustomPayload {
    public static final CustomPayload.Id<GuardPatrolPacket> ID = new CustomPayload.Id<>(Identifier.of(GuardVillagers.MODID, "guard_patrol"));
    public static final CustomPayload.Type<RegistryByteBuf, GuardPatrolPacket> TYPE = new CustomPayload.Type<>(ID, PacketCodec.tuple(PacketCodecs.VAR_INT, GuardPatrolPacket::id, PacketCodecs.BOOLEAN, GuardPatrolPacket::pressed, GuardPatrolPacket::new));

    public static void handle(GuardPatrolPacket packet, ServerPlayNetworking.Context context) {
        ServerPlayerEntity player = context.player();
        var entity = player.getEntityWorld().getEntityById(packet.id());
        if (entity instanceof GuardEntity guard) {
            if (packet.pressed()) {
                guard.setPatrolPos(guard.getBlockPos());
            }
            guard.setPatrolling(packet.pressed());
        }
    }

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
