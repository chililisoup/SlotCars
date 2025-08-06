package dev.chililisoup.slotcars.client;

import dev.chililisoup.slotcars.client.reg.ModEntityRenderers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.world.level.block.Block;

import static dev.chililisoup.slotcars.reg.ModBlocks.CUTOUT_BLOCKS;

@Environment(EnvType.CLIENT)
public class SlotCarsClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModEntityRenderers.init();

        BlockRenderLayerMap.putBlocks(ChunkSectionLayer.CUTOUT, CUTOUT_BLOCKS.toArray(Block[]::new));
    }
}
