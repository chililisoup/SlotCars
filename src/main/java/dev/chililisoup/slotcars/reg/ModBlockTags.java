package dev.chililisoup.slotcars.reg;

import dev.chililisoup.slotcars.SlotCars;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public class ModBlockTags {
    public static final TagKey<Block> TRACKS = create("tracks");

    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, SlotCars.loc(name));
    }
}
