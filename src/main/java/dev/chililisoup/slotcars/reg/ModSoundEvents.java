package dev.chililisoup.slotcars.reg;

import dev.chililisoup.slotcars.SlotCars;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSoundEvents {
    public static final SoundEvent SLOT_CAR_TRAVEL = register("entity.slot_car.travel");

    private static SoundEvent register(String name) {
        ResourceLocation loc = SlotCars.loc(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, loc, SoundEvent.createVariableRangeEvent(loc));
    }

    public static void init() {}
}
