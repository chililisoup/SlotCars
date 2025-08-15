package dev.chililisoup.slotcars.reg;

import dev.chililisoup.slotcars.SlotCars;
import dev.chililisoup.slotcars.block.*;
import net.fabricmc.api.EnvType;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

import java.util.ArrayList;
import java.util.function.Function;

public class ModBlocks {
    public static final ArrayList<Block> CUTOUT_BLOCKS = new ArrayList<>();

    public static final Block TRACK = registerTrack("track", TrackBlock::new);
    public static final Block T_INTERSECTION_TRACK = registerTrack("t_intersection_track", TIntersectionTrackBlock::new);
    public static final Block DOUBLE_CORNER_TRACK = registerTrack("double_corner_track", DoubleCornerTrackBlock::new);
    public static final Block HAIRPIN_TRACK = registerTrack("hairpin_track", HairpinTrackBlock::new);
    public static final Block HALF_RAMP_TRACK = registerTrack("half_ramp_track", HalfRampTrackBlock::new);

    private static Block register(
            String name,
            Function<Properties, Block> blockFactory,
            Properties properties
    ) {
        ResourceKey<Block> blockKey = blockKey(name);
        Block block = blockFactory.apply(properties.setId(blockKey));

        ResourceKey<Item> itemKey = itemKey(name);
        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey));
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);
        ItemGroupEvents.modifyEntriesEvent(SlotCarsCreativeTab.TAB_KEY).register(tab -> tab.accept(blockItem));

        return Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
    }

    private static Block registerTrack(String name, Function<Properties, Block> blockFactory) {
        Block track = register(name, blockFactory, Properties.ofFullCopy(Blocks.RAIL));

        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT)
            CUTOUT_BLOCKS.add(track);

        return track;
    }

    private static ResourceKey<Block> blockKey(String name) {
        return ResourceKey.create(Registries.BLOCK, SlotCars.loc(name));
    }

    private static ResourceKey<Item> itemKey(String name) {
        return ResourceKey.create(Registries.ITEM, SlotCars.loc(name));
    }

    public static void init() {}
}
