package dev.sterner.guardvillagers.client.renderer;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersClient;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.client.model.GuardVillagerModel;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.ArmorModelSet;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.SwingAnimationType;
import com.mojang.blaze3d.vertex.PoseStack;
import org.jetbrains.annotations.Nullable;

public class GuardRenderer extends HumanoidMobRenderer<GuardEntity, HumanoidRenderState, GuardVillagerModel> {

    private GuardEntity currentEntity;

    public GuardRenderer(EntityRendererProvider.Context context) {
        super(context, new GuardVillagerModel(context.bakeLayer(GuardVillagersClient.GUARD)), 0.5F);
        ArmorModelSet<HumanoidModel<HumanoidRenderState>> inner = ArmorModelSet.bake(ModelLayers.PLAYER_ARMOR, context.getModelSet(), HumanoidModel::new);
        this.addLayer(new HumanoidArmorLayer<HumanoidRenderState, GuardVillagerModel, HumanoidModel<HumanoidRenderState>>(this, inner, context.getEquipmentRenderer()));
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public void extractRenderState(GuardEntity entity, HumanoidRenderState state, float tickDelta) {
        super.extractRenderState(entity, state, tickDelta);
        this.currentEntity = entity;
    }

    @Override
    protected HumanoidModel.ArmPose getArmPose(GuardEntity entity, HumanoidArm arm) {
        ItemStack stack = entity.getItemHeldByArm(arm);
        if (stack.isEmpty()) return HumanoidModel.ArmPose.EMPTY;
        if (entity.getTicksUsingItem() > 0) {
            ItemUseAnimation useAnimation = stack.getUseAnimation();
            if (useAnimation == ItemUseAnimation.BLOCK) return HumanoidModel.ArmPose.BLOCK;
            if (useAnimation == ItemUseAnimation.BOW) return HumanoidModel.ArmPose.BOW_AND_ARROW;
            if (useAnimation == ItemUseAnimation.SPEAR) return HumanoidModel.ArmPose.SPEAR;
            if (useAnimation == ItemUseAnimation.CROSSBOW) {
                boolean mainMatches = (entity.getMainArm() == arm) == (entity.getUsedItemHand() == InteractionHand.MAIN_HAND);
                return mainMatches ? HumanoidModel.ArmPose.CROSSBOW_CHARGE : HumanoidModel.ArmPose.EMPTY;
            }
            return HumanoidModel.ArmPose.EMPTY;
        }
        boolean mainCrossbow = entity.getMainHandItem().getItem() instanceof CrossbowItem;
        boolean offCrossbow = entity.getOffhandItem().getItem() instanceof CrossbowItem;
        if (mainCrossbow && entity.isAggressive()) return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        if (offCrossbow && entity.getMainHandItem().getUseAnimation() == ItemUseAnimation.NONE && entity.isAggressive()) return HumanoidModel.ArmPose.CROSSBOW_HOLD;
        if (stack.get(DataComponents.SWING_ANIMATION) != null && stack.get(DataComponents.SWING_ANIMATION).type() == SwingAnimationType.STAB) {
            return HumanoidModel.ArmPose.SPEAR;
        }
        if (stack.is(ItemTags.SPEARS)) return HumanoidModel.ArmPose.SPEAR;
        return HumanoidModel.ArmPose.EMPTY;
    }

    @Override
    protected void scale(HumanoidRenderState state, PoseStack poseStack) {
        poseStack.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Nullable
    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        if (this.currentEntity == null) return Identifier.fromNamespaceAndPath(GuardVillagers.MODID, "textures/entity/guard/guard_0.png");
        return Identifier.fromNamespaceAndPath(GuardVillagers.MODID, GuardVillagersConfig.useSteveModel
            ? "textures/entity/guard/guard_steve_" + this.currentEntity.getGuardVariant() + ".png"
            : "textures/entity/guard/guard_" + this.currentEntity.getGuardVariant() + ".png");
    }
}
