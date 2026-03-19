package dev.sterner.guardvillagers.client.screen;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import dev.sterner.guardvillagers.common.network.GuardFollowPacket;
import dev.sterner.guardvillagers.common.network.GuardPatrolPacket;
import dev.sterner.guardvillagers.common.screenhandler.GuardVillagerScreenHandler;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

public class GuardVillagerScreen extends HandledScreen<GuardVillagerScreenHandler> {

    private static final Identifier GUARD_GUI_TEXTURES = Identifier.of(GuardVillagers.MODID, "textures/gui/inventory.png");
    private static final Identifier GUARD_FOLLOWING_ICON = Identifier.of(GuardVillagers.MODID, "textures/gui/following_icons.png");
    private static final Identifier GUARD_NOT_FOLLOWING_ICON = Identifier.of(GuardVillagers.MODID, "textures/gui/not_following_icons.png");
    private static final Identifier PATROL_ICON = Identifier.of(GuardVillagers.MODID, "textures/gui/patrollingui.png");
    private static final Identifier NOT_PATROLLING_ICON = Identifier.of(GuardVillagers.MODID, "textures/gui/notpatrollingui.png");

    private static final Identifier ICONS = Identifier.ofVanilla("textures/gui/icons");
    private final PlayerEntity player;
    private final GuardEntity guardEntity;
    private float mousePosX;
    private float mousePosY;
    private boolean buttonPressed;

    public GuardVillagerScreen(GuardVillagerScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, handler.guardEntity.getDisplayName());
        this.titleX = 80;
        this.playerInventoryTitleX = 100;
        this.player = inventory.player;
        guardEntity = handler.guardEntity;
    }

    @Override
    protected void init() {
        super.init();
        if (!GuardVillagersConfig.followHero || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            this.addDrawableChild(new GuardGuiButton(this.x + 100, this.height / 2 - 40, 20, 18, 0, 0, 19, 256, 256, GUARD_FOLLOWING_ICON, GUARD_NOT_FOLLOWING_ICON, true,
                    (button) -> {
                        ClientPlayNetworking.send(new GuardFollowPacket(guardEntity.getId()));
                    })
            );
        }
        if (!GuardVillagersConfig.setGuardPatrolHotv || player.hasStatusEffect(StatusEffects.HERO_OF_THE_VILLAGE)) {
            this.addDrawableChild(new GuardGuiButton(this.x + 120, this.height / 2 - 40, 20, 18, 0, 0, 19, 256, 256, PATROL_ICON, NOT_PATROLLING_ICON, false,
                    (button) -> {
                        buttonPressed = !buttonPressed;
                        ClientPlayNetworking.send(new GuardPatrolPacket(guardEntity.getId(), buttonPressed));
                    })
            );
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int i = (this.width - this.backgroundWidth) / 2;
        int j = (this.height - this.backgroundHeight) / 2;
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, GUARD_GUI_TEXTURES, i, j, 0, 0, this.backgroundWidth, this.backgroundHeight, 256, 256);
        int entityCenterX = i + 56;
        int entityCenterY = j + 80;
        InventoryScreen.drawEntity(ctx, entityCenterX, entityCenterY, entityCenterX + 49, entityCenterY + 70, 30, 0.0625f, (float) entityCenterX - this.mousePosX, (float) (entityCenterY - 50) - this.mousePosY, this.guardEntity);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int x, int y) {
        super.drawForeground(ctx, x, y);
        int health = MathHelper.ceil(guardEntity.getHealth());
        int armor = guardEntity.getArmor();
        int statusU = guardEntity.hasStatusEffect(StatusEffects.POISON) ? 4 : 0;
        //Health
        for (int i = 0; i < 10; i++) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 20, 16, 0, 9, 9, 256, 256);
        }
        for (int i = 0; i < health / 2; i++) {
            if (health % 2 != 0 && health / 2 == i + 1) {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 20, 16 + 9 * (4 + statusU), 0, 9, 9, 256, 256);
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, ((i + 1) * 8) + 80, 20, 16 + 9 * (5 + statusU), 0, 9, 9, 256, 256);
            } else {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 20, 16 + 9 * (4 + statusU), 0, 9, 9, 256, 256);
            }
        }
        for (int i = 0; i < 10; i++) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 30, 16, 9, 9, 9, 256, 256);
        }
        for (int i = 0; i < armor / 2; i++) {
            if (armor % 2 != 0 && armor / 2 == i + 1) {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 30, 16 + 9 * 2, 9, 9, 9, 256, 256);
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, ((i + 1) * 8) + 80, 30, 16 + 9, 9, 9, 9, 256, 256);
            } else {
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ICONS, (i * 8) + 80, 30, 16 + 9 * 2, 9, 9, 9, 256, 256);
            }
        }

    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(ctx, mouseX, mouseY, partialTicks);
        this.mousePosX = (float) mouseX;
        this.mousePosY = (float) mouseY;
        super.render(ctx, mouseX, mouseY, partialTicks);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }


    class GuardGuiButton extends ButtonWidget {
        private final int u;
        private final int v;
        private final int hoveredVOffset;
        private final int textureWidth;
        private final int textureHeight;
        private final Identifier texture;
        private final Identifier newTexture;
        private final boolean isFollowButton;

        public GuardGuiButton(int xIn, int yIn, int widthIn, int heightIn, int u, int v, int hoveredVOffset, int textureWidth, int textureHeight, Identifier texture, Identifier newTexture, boolean isFollowButton, PressAction pressAction) {
            super(xIn, yIn, widthIn, heightIn, net.minecraft.text.Text.empty(), pressAction, ButtonWidget.DEFAULT_NARRATION_SUPPLIER);
            this.u = u;
            this.v = v;
            this.hoveredVOffset = hoveredVOffset;
            this.textureWidth = textureWidth;
            this.textureHeight = textureHeight;
            this.texture = texture;
            this.newTexture = newTexture;
            this.isFollowButton = isFollowButton;
        }

        boolean requirementsForTexture() {
            boolean following = guardEntity.isFollowing();
            boolean patrol = guardEntity.isPatrolling();
            return this.isFollowButton ? following : patrol;
        }

        @Override
        public void drawIcon(DrawContext context, int mouseX, int mouseY, float delta) {
            Identifier icon = this.requirementsForTexture() ? texture : newTexture;
            int vOffset = this.v + (this.isHovered() ? this.hoveredVOffset : 0);
            context.drawTexture(RenderPipelines.GUI_TEXTURED, icon, this.getX(), this.getY(), (float) this.u, (float) vOffset, this.getWidth(), this.getHeight(), textureWidth, textureHeight);
        }
    }

}
