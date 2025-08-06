package dev.chililisoup.slotcars.client.model;

import dev.chililisoup.slotcars.client.renderer.SlotCarRenderState;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;

@Environment(EnvType.CLIENT)
public class SlotCarModel extends EntityModel<SlotCarRenderState> {
    public SlotCarModel(ModelPart modelPart) {
        super(modelPart);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition bone = partdefinition.addOrReplaceChild(
                "bone",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-10.5F, -1.0F, 3.0F, 5.0F, 1.0F, 9.0F, new CubeDeformation(0.0F))
                        .texOffs(0, 10)
                        .addBox(-10.5F, -2.0F, 6.0F, 5.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offset(8.0F, 24.0F, -8.0F)
        );

        bone.addOrReplaceChild(
                "spoiler",
                CubeListBuilder.create()
                        .texOffs(24, 13)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 1.0F, 1.0F, new CubeDeformation(0.0F))
                        .texOffs(19, 15)
                        .addBox(-2.5F, 0.0F, -1.5F, 5.0F, 0.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(-8.0F, -2.0F, 13.0F)
        );

        return LayerDefinition.create(meshdefinition, 32, 16);
    }
}
