package dev.sterner.guardvillagers.client.model;


import dev.sterner.guardvillagers.common.entity.GuardEntity;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.ModelData;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.math.MathHelper;

public class GuardSteveModel extends PlayerEntityModel {
    public GuardSteveModel(ModelPart root) {
        super(root, false);
    }

    public void setAngles(GuardEntity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netbipedHeadYaw, float bipedHeadPitch) {
        if (entityIn.getKickTicks() > 0) {
            float f1 = 1.0F - (float) MathHelper.abs(10 - 2 * entityIn.getKickTicks()) / 10.0F;
            ModelPart rightLeg = this.root.getChild(EntityModelPartNames.RIGHT_LEG);
            rightLeg.pitch = MathHelper.lerp(f1, rightLeg.pitch, -1.40F);
        }
        if (entityIn.getMainArm() == Arm.RIGHT) {
            this.eatingAnimationRightHand(Hand.MAIN_HAND, entityIn, ageInTicks);
            this.eatingAnimationLeftHand(Hand.OFF_HAND, entityIn, ageInTicks);
        } else {
            this.eatingAnimationRightHand(Hand.OFF_HAND, entityIn, ageInTicks);
            this.eatingAnimationLeftHand(Hand.MAIN_HAND, entityIn, ageInTicks);
        }
    }

    public static TexturedModelData createMesh() {
        ModelData meshdefinition = PlayerEntityModel.getTexturedModelData(Dilation.NONE, false);
        return TexturedModelData.of(meshdefinition, 64, 64);
    }

    public void eatingAnimationRightHand(Hand hand, GuardEntity entity, float ageInTicks) {
        ItemStack itemstack = entity.getStackInHand(hand);
        boolean drinkingoreating = itemstack.getUseAction() == UseAction.EAT
                || itemstack.getUseAction() == UseAction.DRINK;
        if (entity.isEating() && drinkingoreating
                || entity.getItemUseTimeLeft() > 0 && drinkingoreating && entity.getActiveHand() == hand) {
            ModelPart rightArm = this.root.getChild(EntityModelPartNames.RIGHT_ARM);
            ModelPart head = this.root.getChild(EntityModelPartNames.HEAD);
            ModelPart hat = head.getChild(EntityModelPartNames.HAT);
            rightArm.yaw = -0.5F;
            rightArm.pitch = -1.3F;
            rightArm.roll = MathHelper.cos(ageInTicks) * 0.1F;
            head.pitch = MathHelper.cos(ageInTicks) * 0.2F;
            head.yaw = 0.0F;
            hat.setTransform(head.getTransform());
        }
    }

    public void eatingAnimationLeftHand(Hand hand, GuardEntity entity, float ageInTicks) {
        ItemStack itemstack = entity.getStackInHand(hand);
        boolean drinkingoreating = itemstack.getUseAction() == UseAction.EAT
                || itemstack.getUseAction() == UseAction.DRINK;
        if (entity.isEating() && drinkingoreating
                || entity.getItemUseTimeLeft() > 0 && drinkingoreating && entity.getActiveHand() == hand) {
            ModelPart leftArm = this.root.getChild(EntityModelPartNames.LEFT_ARM);
            ModelPart head = this.root.getChild(EntityModelPartNames.HEAD);
            ModelPart hat = head.getChild(EntityModelPartNames.HAT);
            leftArm.yaw = 0.5F;
            leftArm.pitch = -1.3F;
            leftArm.roll = MathHelper.cos(ageInTicks) * 0.1F;
            head.pitch = MathHelper.cos(ageInTicks) * 0.2F;
            head.yaw = 0.0F;
            hat.setTransform(head.getTransform());
        }
    }
}