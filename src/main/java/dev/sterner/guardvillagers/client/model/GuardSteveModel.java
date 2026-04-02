package dev.sterner.guardvillagers.client.model;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.player.PlayerModel;

public class GuardSteveModel extends PlayerModel {
    public GuardSteveModel(net.minecraft.client.model.geom.ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createMesh() {
        return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);
    }
}
