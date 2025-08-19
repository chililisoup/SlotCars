package dev.chililisoup.slotcars.mixin.client;

import dev.chililisoup.slotcars.client.sounds.SlotCarSoundInstance;
import dev.chililisoup.slotcars.entity.SlotCar;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket;
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    @Inject(method = "postAddEntitySoundInstance", at = @At("HEAD"), cancellable = true)
    private void addSlotCarSoundInstance(Entity entity, CallbackInfo ci) {
        if (entity instanceof SlotCar car) {
            ((ClientPacketListener) (Object) this).minecraft.getSoundManager().play(new SlotCarSoundInstance(car));
            ci.cancel();
        }
    }

    @Unique private boolean slotCars$shouldTossPacket(Entity entity) {
        return entity instanceof SlotCar car && car.isLocalInstanceAuthoritative();
    }

    @Unique private boolean slotCars$shouldTossPacket(int id) {
        return this.slotCars$shouldTossPacket(((ClientPacketListener) (Object) this).getLevel().getEntity(id));
    }

    @Unique private boolean slotCars$shouldTossPacket(Function<Level, Entity> entitySupplier) {
        Level level = ((ClientPacketListener) (Object) this).getLevel();
        return this.slotCars$shouldTossPacket(entitySupplier.apply(level));
    }

    @Inject(method = "handleMoveEntity", at = @At(value = "HEAD"), cancellable = true)
    private void tossMoveSync(ClientboundMoveEntityPacket clientboundMoveEntityPacket, CallbackInfo ci) {
        if (slotCars$shouldTossPacket(clientboundMoveEntityPacket::getEntity))
            ci.cancel();
    }

    @Inject(method = "handleEntityPositionSync", at = @At(value = "HEAD"), cancellable = true)
    private void checkPositionSync(ClientboundEntityPositionSyncPacket clientboundEntityPositionSyncPacket, CallbackInfo ci) {
        if (slotCars$shouldTossPacket(clientboundEntityPositionSyncPacket.id()))
            ci.cancel();
    }

    @Inject(method = "handleSetEntityMotion", at = @At(value = "HEAD"), cancellable = true)
    private void tossMotionSync(ClientboundSetEntityMotionPacket clientboundSetEntityMotionPacket, CallbackInfo ci) {
        if (slotCars$shouldTossPacket(clientboundSetEntityMotionPacket.getId()))
            ci.cancel();
    }
}
