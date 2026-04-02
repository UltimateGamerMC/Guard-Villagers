package dev.sterner.guardvillagers.client.model;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.HumanoidArm;

public class GuardVillagerModel extends HumanoidModel<HumanoidRenderState> {
    public ModelPart nose = this.head.getChild("nose");
    public ModelPart quiver = this.body.getChild("quiver");
    public ModelPart armLShoulderPad = this.rightArm.getChild("shoulderPad_left");
    public ModelPart armRShoulderPad = this.leftArm.getChild("shoulderPad_right");

    public GuardVillagerModel(ModelPart part) {
        super(part);
        this.quiver.xRot = 0.0F;
        this.quiver.yRot = 0.0F;
        this.quiver.zRot = 0.2617993877991494F;
        this.armLShoulderPad.xRot = 0.0F;
        this.armLShoulderPad.yRot = 0.0F;
        this.armLShoulderPad.zRot = -0.3490658503988659F;
        this.armRShoulderPad.xRot = 0.0F;
        this.armRShoulderPad.yRot = 0.0F;
        this.armRShoulderPad.zRot = 0.3490658503988659F;
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition partdefinition = meshdefinition.getRoot();
        PartDefinition torso = partdefinition.addOrReplaceChild(
            "body",
            CubeListBuilder.create().texOffs(52, 50).addBox(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F, new CubeDeformation(0.25F)),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        PartDefinition head = partdefinition.addOrReplaceChild(
            "head",
            CubeListBuilder.create().texOffs(49, 99).addBox(-4.0F, -10.0F, -4.0F, 8.0F, 10.0F, 8.0F),
            PartPose.offset(0.0F, 1.0F, 0.0F)
        );
        PartDefinition rightArm = partdefinition.addOrReplaceChild(
            "right_arm",
            CubeListBuilder.create().texOffs(32, 75).mirror().addBox(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(-5.0F, 2.0F, 0.0F)
        );
        PartDefinition leftArm = partdefinition.addOrReplaceChild(
            "left_arm",
            CubeListBuilder.create().texOffs(33, 48).addBox(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(5.0F, 2.0F, 0.0F)
        );
        torso.addOrReplaceChild(
            "quiver",
            CubeListBuilder.create().texOffs(100, 0).addBox(-2.5F, -2.0F, 0.0F, 5.0F, 10.0F, 5.0F),
            PartPose.offset(0.5F, 3.0F, 2.3F)
        );
        head.addOrReplaceChild(
            "nose",
            CubeListBuilder.create().texOffs(54, 0).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 4.0F, 2.0F),
            PartPose.offset(0.0F, -3.0F, -4.0F)
        );
        partdefinition.addOrReplaceChild(
            "right_leg",
            CubeListBuilder.create().texOffs(16, 48).mirror().addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(-1.9F, 12.0F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "left_leg",
            CubeListBuilder.create().texOffs(16, 28).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
            PartPose.offset(1.9F, 12.0F, 0.0F)
        );
        leftArm.addOrReplaceChild(
            "shoulderPad_right",
            CubeListBuilder.create().texOffs(72, 33).mirror().addBox(0.0F, 0.0F, -3.0F, 5.0F, 3.0F, 6.0F),
            PartPose.offset(-0.5F, -3.5F, 0.0F)
        );
        rightArm.addOrReplaceChild(
            "shoulderPad_left",
            CubeListBuilder.create().texOffs(72, 33).addBox(-5.0F, 0.0F, -3.0F, 5.0F, 3.0F, 6.0F),
            PartPose.offset(0.5F, -3.5F, 0.0F)
        );
        partdefinition.addOrReplaceChild(
            "hat",
            CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -11.0F, -4.5F, 9.0F, 11.0F, 9.0F),
            PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        return LayerDefinition.create(meshdefinition, 128, 128);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        ItemStack mainHand = state.getMainHandItemStack();
        this.quiver.visible = !mainHand.isEmpty() && (mainHand.is(Items.BOW) || mainHand.is(Items.CROSSBOW));
        this.armLShoulderPad.visible = state.chestEquipment.isEmpty();
        this.armRShoulderPad.visible = state.chestEquipment.isEmpty();
        if (state.isUsingItem) {
            boolean eating = mainHand.getUseAnimation() == ItemUseAnimation.EAT || mainHand.getUseAnimation() == ItemUseAnimation.DRINK;
            boolean rightHand = state.mainArm == HumanoidArm.RIGHT ? state.useItemHand == net.minecraft.world.InteractionHand.MAIN_HAND : state.useItemHand == net.minecraft.world.InteractionHand.OFF_HAND;
            float t = state.ticksUsingItem(state.mainArm);
            if (eating && rightHand) {
                this.rightArm.yRot = -0.5F;
                this.rightArm.xRot = -1.3F;
                this.rightArm.zRot = Mth.cos(t) * 0.1F;
                this.head.xRot = Mth.cos(t) * 0.2F;
                this.head.yRot = 0.0F;
                this.hat.loadPose(this.head.storePose());
            } else if (eating && !rightHand) {
                this.leftArm.yRot = 0.5F;
                this.leftArm.xRot = -1.3F;
                this.leftArm.zRot = Mth.cos(t) * 0.1F;
                this.head.xRot = Mth.cos(t) * 0.2F;
                this.head.yRot = 0.0F;
                this.hat.loadPose(this.head.storePose());
            }
        }
    }
}
