package dev.chililisoup.slotcars.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.phys.Vec3;

@Environment(EnvType.CLIENT)
public class SlotCarRenderState extends EntityRenderState {
    public Vec3 pos;
    public float xRot;
    public float yRot;
}
