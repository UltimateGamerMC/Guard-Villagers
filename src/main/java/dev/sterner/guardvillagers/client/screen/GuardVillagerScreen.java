package dev.sterner.guardvillagers.client.screen;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.network.GuardFollowPacket;
import dev.sterner.guardvillagers.common.network.GuardPatrolPacket;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;

public class GuardVillagerScreen extends AbstractContainerScreen<GuardVillagerScreenHandler> {

    private static final Identifier GUARD_GUI_TEXTURES = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "textures/gui/inventory.png");
    private static final Identifier GUARD_FOLLOWING_ICON = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "textures/gui/following_icons.png");
    private static final Identifier GUARD_NOT_FOLLOWING_ICON = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "textures/gui/not_following_icons.png");
    private static final Identifier PATROL_ICON = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "textures/gui/patrollingui.png");
    private static final Identifier NOT_PATROLLING_ICON = Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "textures/gui/notpatrollingui.png");
    private static final Identifier ICONS = Identifier.withDefaultNamespace("textures/gui/icons");
    private final GuardEntity guardEntity;
    private float mousePosX;
    private float mousePosY;
    private boolean patrolPressed;

    public GuardVillagerScreen(GuardVillagerScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, handler.guardEntity != null ? handler.guardEntity.getDisplayName() : title);
        this.titleLabelX = 80;
        this.inventoryLabelX = 100;
        this.guardEntity = handler.guardEntity;
    }

    @Override
    protected void init() {
        super.init();
        if (this.guardEntity == null) {
            return;
        }
        if (!GuardVillagersConfig.followHero || this.minecraft.player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            this.addRenderableWidget(new GuardIconButton(this.leftPos + 100, this.height / 2 - 40, 20, 18, 0, 0, 19, 256, 256, GUARD_FOLLOWING_ICON, GUARD_NOT_FOLLOWING_ICON, true, () ->
                ClientPlayNetworking.send(new GuardFollowPacket(this.guardEntity.getId()))));
        }
        if (!GuardVillagersConfig.setGuardPatrolHotv || this.minecraft.player.hasEffect(MobEffects.HERO_OF_THE_VILLAGE)) {
            this.addRenderableWidget(new GuardIconButton(this.leftPos + 120, this.height / 2 - 40, 20, 18, 0, 0, 19, 256, 256, PATROL_ICON, NOT_PATROLLING_ICON, false, () -> {
                this.patrolPressed = !this.patrolPressed;
                ClientPlayNetworking.send(new GuardPatrolPacket(this.guardEntity.getId(), this.patrolPressed));
            }));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        this.mousePosX = mouseX;
        this.mousePosY = mouseY;
        super.extractRenderState(graphics, mouseX, mouseY, a);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int xo = this.leftPos;
        int yo = this.topPos;
        graphics.blit(RenderPipelines.GUI_TEXTURED, GUARD_GUI_TEXTURES, xo, yo, 0.0F, 0.0F, this.imageWidth, this.imageHeight, 256, 256);
        if (this.guardEntity != null) {
            int entityCenterX = xo + 56;
            int entityCenterY = yo + 80;
            InventoryScreen.extractEntityInInventoryFollowsMouse(graphics, entityCenterX, entityCenterY, entityCenterX + 49, entityCenterY + 70, 30, 0.0625F, (float) entityCenterX - this.mousePosX, (float) (entityCenterY - 50) - this.mousePosY, this.guardEntity);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int xm, int ym) {
        super.extractLabels(graphics, xm, ym);
        if (this.guardEntity == null) {
            return;
        }
        int health = Mth.ceil(this.guardEntity.getHealth());
        int armor = this.guardEntity.getArmorValue();
        int statusU = this.guardEntity.hasEffect(MobEffects.POISON) ? 4 : 0;
        for (int i = 0; i < 10; i++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 20, 16.0F, 0.0F, 9, 9, 256, 256);
        }
        for (int i = 0; i < health / 2; i++) {
            if (health % 2 != 0 && health / 2 == i + 1) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 20, 16.0F + 9.0F * (4 + statusU), 0.0F, 9, 9, 256, 256);
                graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, ((i + 1) * 8) + 80, 20, 16.0F + 9.0F * (5 + statusU), 0.0F, 9, 9, 256, 256);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 20, 16.0F + 9.0F * (4 + statusU), 0.0F, 9, 9, 256, 256);
            }
        }
        for (int i = 0; i < 10; i++) {
            graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 30, 16.0F, 9.0F, 9, 9, 256, 256);
        }
        for (int i = 0; i < armor / 2; i++) {
            if (armor % 2 != 0 && armor / 2 == i + 1) {
                graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 30, 16.0F + 9.0F * 2, 9.0F, 9, 9, 256, 256);
                graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, ((i + 1) * 8) + 80, 30, 16.0F + 9.0F, 9.0F, 9, 9, 256, 256);
            } else {
                graphics.blit(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 30, 16.0F + 9.0F * 2, 9.0F, 9, 9, 256, 256);
            }
        }
    }

    private final class GuardIconButton extends AbstractWidget {
        private final int u;
        private final int v;
        private final int hoveredVOffset;
        private final int textureWidth;
        private final int textureHeight;
        private final Identifier textureOn;
        private final Identifier textureOff;
        private final boolean followButton;
        private final Runnable onPress;

        GuardIconButton(int x, int y, int w, int h, int u, int v, int hoveredVOffset, int tw, int th, Identifier on, Identifier off, boolean followButton, Runnable onPress) {
            super(x, y, w, h, Component.empty());
            this.u = u;
            this.v = v;
            this.hoveredVOffset = hoveredVOffset;
            this.textureWidth = tw;
            this.textureHeight = th;
            this.textureOn = on;
            this.textureOff = off;
            this.followButton = followButton;
            this.onPress = onPress;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            this.onPress.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            this.defaultButtonNarrationText(output);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
            boolean on = guardEntity != null && (this.followButton ? guardEntity.isFollowing() : guardEntity.isPatrolling());
            Identifier icon = on ? this.textureOn : this.textureOff;
            int vOffset = this.v + (this.isHovered() ? this.hoveredVOffset : 0);
            graphics.blit(RenderPipelines.GUI_TEXTURED, icon, this.getX(), this.getY(), (float) this.u, (float) vOffset, this.getWidth(), this.getHeight(), this.textureWidth, this.textureHeight);
        }
    }
}
