package dev.chililisoup.slotcars.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.chililisoup.slotcars.SlotCars;
import dev.chililisoup.slotcars.client.model.SlotCarModel;
import dev.chililisoup.slotcars.client.reg.ModEntityRenderers;
import dev.chililisoup.slotcars.entity.SlotCar;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

@Environment(EnvType.CLIENT)
public class SlotCarRenderer extends EntityRenderer<SlotCar, SlotCarRenderState> {
    private static final ResourceLocation MINECART_LOCATION = SlotCars.loc("textures/entity/slot_car.png");
    protected final SlotCarModel model;

    public SlotCarRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SlotCarModel(context.bakeLayer(ModEntityRenderers.SLOT_CAR_MODEL));
    }

    public void render(SlotCarRenderState renderState, PoseStack poseStack, MultiBufferSource multiBufferSource, int i) {
        if (renderState.invisible) return;
        super.render(renderState, poseStack, multiBufferSource, i);

        poseStack.pushPose();

        poseStack.mulPose(Axis.YP.rotationDegrees(180 - renderState.yRot));
        poseStack.mulPose(Axis.XP.rotationDegrees(-renderState.xRot));
        poseStack.translate(0.0F, 1.125F, 0.0F);
        poseStack.scale(-0.75F, -0.75F, 0.75F);

        this.model.setupAnim(renderState);
        VertexConsumer vertexConsumer = multiBufferSource.getBuffer(this.model.renderType(MINECART_LOCATION));
        this.model.renderToBuffer(poseStack, vertexConsumer, i, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }

    @Override
    public void extractRenderState(SlotCar car, SlotCarRenderState renderState, float partialTick) {
        super.extractRenderState(car, renderState, partialTick);

        renderState.invisible = car.isInvisible();
        renderState.xRot = car.getXRot(partialTick);
        renderState.yRot = car.getYRot(partialTick);
    }

    @Override
    public @NotNull SlotCarRenderState createRenderState() {
        return new SlotCarRenderState();
    }
}
