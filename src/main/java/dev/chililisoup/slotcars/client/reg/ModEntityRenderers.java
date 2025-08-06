package dev.chililisoup.slotcars.client.reg;

import dev.chililisoup.slotcars.SlotCars;
import dev.chililisoup.slotcars.client.model.SlotCarModel;
import dev.chililisoup.slotcars.client.renderer.SlotCarRenderer;
import dev.chililisoup.slotcars.reg.ModEntityTypes;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

@Environment(EnvType.CLIENT)
public class ModEntityRenderers {
    public static final ModelLayerLocation SLOT_CAR_MODEL = register(
            "slot_car",
            ModEntityTypes.SLOT_CAR,
            SlotCarRenderer::new,
            SlotCarModel::createBodyLayer
    );

    private static <T extends Entity> ModelLayerLocation register(
            String name,
            EntityType<? extends T> entityType,
            EntityRendererProvider<T> entityRendererProvider,
            EntityModelLayerRegistry.TexturedModelDataProvider provider
    ) {
        EntityRendererRegistry.register(entityType, entityRendererProvider);
        ModelLayerLocation model = new ModelLayerLocation(SlotCars.loc(name), "main");
        EntityModelLayerRegistry.registerModelLayer(model, provider);
        return model;
    }

    public static void init() {}
}
