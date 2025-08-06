package dev.chililisoup.slotcars.reg;

import dev.chililisoup.slotcars.SlotCars;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;

public class SlotCarsCreativeTab {
    public static final ResourceKey<CreativeModeTab> TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            SlotCars.loc("slot_cars")
    );
    public static final CreativeModeTab TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            TAB_KEY,
            FabricItemGroup.builder()
                    .icon(() -> ModBlocks.TRACK.asItem().getDefaultInstance())
                    .title(Component.translatable("itemGroup.slotCars"))
                    .build()
    );

    public static void init() {}
}
