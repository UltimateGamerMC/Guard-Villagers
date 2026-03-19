package dev.sterner.guardvillagers.client.renderer;

import dev.sterner.guardvillagers.GuardVillagers;
import dev.sterner.guardvillagers.GuardVillagersClient;
import dev.sterner.guardvillagers.GuardVillagersConfig;
import dev.sterner.guardvillagers.client.model.GuardArmorModel;
import dev.sterner.guardvillagers.client.model.GuardVillagerModel;
import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.render.entity.BipedEntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.feature.ArmorFeatureRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EquipmentModelData;
import net.minecraft.client.render.entity.state.BipedEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

public class GuardRenderer extends BipedEntityRenderer<GuardEntity, BipedEntityRenderState, BipedEntityModel<BipedEntityRenderState>> {

    private GuardEntity currentEntity;

    public GuardRenderer(EntityRendererFactory.Context context) {
        super(context, new GuardVillagerModel(context.getPart(GuardVillagersClient.GUARD)), 0.5F);
        EquipmentModelData<BipedEntityModel<BipedEntityRenderState>> armorData = GuardVillagersConfig.useSteveModel
                ? EquipmentModelData.mapToEntityModel(EntityModelLayers.PLAYER_EQUIPMENT, context.getEntityModels(), part -> new BipedEntityModel<>(part))
                : EquipmentModelData.mapToEntityModel(new EquipmentModelData<>(GuardVillagersClient.GUARD_ARMOR_INNER, GuardVillagersClient.GUARD_ARMOR_INNER, GuardVillagersClient.GUARD_ARMOR_INNER, GuardVillagersClient.GUARD_ARMOR_INNER), context.getEntityModels(), GuardArmorModel::new);
        this.addFeature(new ArmorFeatureRenderer<>(this, armorData, armorData, context.getEquipmentRenderer()));
    }

    @Override
    public BipedEntityRenderState createRenderState() {
        return new BipedEntityRenderState();
    }

    @Override
    public void updateRenderState(GuardEntity entity, BipedEntityRenderState state, float tickDelta) {
        super.updateRenderState(entity, state, tickDelta);
        this.currentEntity = entity;
    }

    @Override
    protected BipedEntityModel.ArmPose getArmPose(GuardEntity entity, Arm arm) {
        ItemStack stack = entity.getStackInArm(arm);
        if (stack.isEmpty()) return BipedEntityModel.ArmPose.EMPTY;
        if (entity.getItemUseTimeLeft() > 0) {
            UseAction useAction = stack.getUseAction();
            switch (useAction) {
                case BLOCK: return BipedEntityModel.ArmPose.BLOCK;
                case BOW: return BipedEntityModel.ArmPose.BOW_AND_ARROW;
                case SPEAR: return BipedEntityModel.ArmPose.SPEAR;
                case CROSSBOW: return (entity.getMainArm() == arm) == (entity.getActiveHand() == Hand.MAIN_HAND) ? BipedEntityModel.ArmPose.CROSSBOW_CHARGE : BipedEntityModel.ArmPose.EMPTY;
                default: return BipedEntityModel.ArmPose.EMPTY;
            }
        }
        boolean mainCrossbow = entity.getMainHandStack().getItem() instanceof CrossbowItem;
        boolean offCrossbow = entity.getOffHandStack().getItem() instanceof CrossbowItem;
        if (mainCrossbow && entity.isAttacking()) return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
        if (offCrossbow && entity.getMainHandStack().getUseAction() == UseAction.NONE && entity.isAttacking()) return BipedEntityModel.ArmPose.CROSSBOW_HOLD;
        if (stack.isIn(ItemTags.SPEARS)) return BipedEntityModel.ArmPose.SPEAR;
        return BipedEntityModel.ArmPose.EMPTY;
    }

    @Override
    protected void scale(BipedEntityRenderState state, MatrixStack matrices) {
        matrices.scale(0.9375F, 0.9375F, 0.9375F);
    }

    @Nullable
    @Override
    public Identifier getTexture(BipedEntityRenderState state) {
        if (this.currentEntity == null) return Identifier.of(GuardVillagers.MODID, "textures/entity/guard/guard_0.png");
        return Identifier.of(GuardVillagers.MODID, GuardVillagersConfig.useSteveModel
                ? "textures/entity/guard/guard_steve_" + this.currentEntity.getGuardVariant() + ".png"
                : "textures/entity/guard/guard_" + this.currentEntity.getGuardVariant() + ".png");
    }
}
