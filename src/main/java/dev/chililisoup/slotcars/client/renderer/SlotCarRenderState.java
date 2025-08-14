package dev.chililisoup.slotcars.client.renderer;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

@Environment(EnvType.CLIENT)
public class SlotCarRenderState extends EntityRenderState {
    public boolean invisible;
    public float xRot;
    public float yRot;
    public float zRot;
    public int color;
}
