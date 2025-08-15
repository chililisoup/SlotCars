package dev.chililisoup.slotcars.mixin.client;

import dev.chililisoup.slotcars.client.sounds.SlotCarSoundInstance;
import dev.chililisoup.slotcars.entity.SlotCar;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "postAddEntitySoundInstance", at = @At("HEAD"), cancellable = true)
    private void addSlotCarSoundInstance(Entity entity, CallbackInfo ci) {
        if (entity instanceof SlotCar car) {
            ((ClientPacketListener) (Object) this).minecraft.getSoundManager().play(new SlotCarSoundInstance(car));
            ci.cancel();
        }
    }
}
