package dev.sterner.guardvillagers;

import dev.sterner.guardvillagers.client.model.GuardSteveModel;
import dev.sterner.guardvillagers.client.model.GuardVillagerModel;
import dev.sterner.guardvillagers.client.renderer.GuardRenderer;
import dev.sterner.guardvillagers.client.screen.GuardVillagerScreen;
import dev.sterner.guardvillagers.common.network.GuardFollowPacket;
import dev.sterner.guardvillagers.common.network.GuardPatrolPacket;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.Identifier;

import static dev.sterner.guardvillagers.GuardVillagers.*;

public class GuardVillagersClient implements ClientModInitializer {

    public static final ModelLayerLocation GUARD = new ModelLayerLocation(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "guard"), "main");
    public static final ModelLayerLocation GUARD_STEVE = new ModelLayerLocation(Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "guard_steve"), "main");

    @Override
    public void onInitializeClient() {
        PayloadTypeRegistry.serverboundPlay().register(GuardFollowPacket.TYPE, GuardFollowPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(GuardPatrolPacket.TYPE, GuardPatrolPacket.CODEC);
        MenuScreens.register(GUARD_SCREEN_HANDLER, GuardVillagerScreen::new);
        ModelLayerRegistry.registerModelLayer(GUARD, GuardVillagerModel::createBodyLayer);
        ModelLayerRegistry.registerModelLayer(GUARD_STEVE, GuardSteveModel::createMesh);
        EntityRendererRegistry.register(GUARD_VILLAGER, GuardRenderer::new);
    }
}
