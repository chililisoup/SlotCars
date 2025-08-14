package dev.chililisoup.slotcars;

import dev.chililisoup.slotcars.reg.*;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SlotCars implements ModInitializer {
    public static final String MOD_ID = "slotcars";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static ResourceLocation loc(String id) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, id);
    }

    @Override
    public void onInitialize() {
        SlotCarsCreativeTab.init();
        ModBlocks.init();
        ModItems.init();
        ModEntityTypes.init();
        ModPackets.init();
        ModCauldronInteractions.init();
    }
}
