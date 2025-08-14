package dev.chililisoup.slotcars.reg;

import net.minecraft.core.cauldron.CauldronInteraction;

public class ModCauldronInteractions {
    public static void init() {
        CauldronInteraction.WATER.map().put(ModItems.CONTROLLER, CauldronInteraction::dyedItemIteration);
    }
}
