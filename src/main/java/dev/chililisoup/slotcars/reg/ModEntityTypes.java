package dev.chililisoup.slotcars.reg;

import dev.chililisoup.slotcars.SlotCars;
import dev.chililisoup.slotcars.entity.SlotCar;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntityTypes {
    public static final EntityType<SlotCar> SLOT_CAR = register(
            "slot_car",
            EntityType.Builder.<SlotCar>of(SlotCar::new, MobCategory.MISC)
                    .noLootTable()
                    .noSave()
                    .noSummon()
                    .sized(0.25F, 0.25F)
                    .clientTrackingRange(16)
    );

    private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        ResourceKey<EntityType<?>> entityKey = entityKey(name);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, entityKey, builder.build(entityKey));
    }

    private static ResourceKey<EntityType<?>> entityKey(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, SlotCars.loc(name));
    }

    public static void init() {}
}
