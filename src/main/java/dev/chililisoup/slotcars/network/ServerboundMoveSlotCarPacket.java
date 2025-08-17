package dev.chililisoup.slotcars.network;

import dev.chililisoup.slotcars.SlotCars;
import dev.chililisoup.slotcars.entity.SlotCar;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public record ServerboundMoveSlotCarPacket(Vec3 position, float yRot, float xRot, boolean onGround, int respawnTimer, boolean derailed, boolean onTrack) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ServerboundMoveSlotCarPacket> TYPE = new CustomPacketPayload.Type<>(
            SlotCars.loc("move_slot_car")
    );
    public static final StreamCodec<FriendlyByteBuf, ServerboundMoveSlotCarPacket> CODEC = StreamCodec.composite(
            Vec3.STREAM_CODEC,
            ServerboundMoveSlotCarPacket::position,
            ByteBufCodecs.FLOAT,
            ServerboundMoveSlotCarPacket::yRot,
            ByteBufCodecs.FLOAT,
            ServerboundMoveSlotCarPacket::xRot,
            ByteBufCodecs.BOOL,
            ServerboundMoveSlotCarPacket::onGround,
            ByteBufCodecs.INT,
            ServerboundMoveSlotCarPacket::respawnTimer,
            ByteBufCodecs.BOOL,
            ServerboundMoveSlotCarPacket::derailed,
            ByteBufCodecs.BOOL,
            ServerboundMoveSlotCarPacket::onTrack,
            ServerboundMoveSlotCarPacket::new
    );

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(TYPE, CODEC);

        ServerPlayNetworking.registerGlobalReceiver(TYPE, (packet, context) -> {
            SlotCar.SlotCarHolder holder = context.player().getAttached(SlotCar.ACTIVE_SLOT_CAR);
            if (holder == null) return;

            SlotCar car = holder.get();
            if (car == null) return;

            Vec3 deltaMovement = packet.position.subtract(car.position());
            car.setDeltaMovement(deltaMovement);
            car.move(MoverType.SELF, deltaMovement);
            car.setYRot(packet.yRot);
            car.setXRot(packet.xRot);
            car.setOnGround(packet.onGround);
            car.setDerailed(packet.derailed);
            car.setOnTrack(packet.onTrack);

            car.setInvisible(packet.respawnTimer > 0);
            if (packet.respawnTimer == SlotCar.respawnLength()) {
                context.player().level().broadcastEntityEvent(car, (byte)40);
                car.playSound(SoundEvents.GENERIC_EXPLODE.value(), 0.25F, 1.25F);
            }
        });
    }

    public static ServerboundMoveSlotCarPacket fromCar(SlotCar car) {
        Vec3 position;
        float yRot;
        float xRot;

        if (car.isInterpolating()) {
            position = car.getInterpolation().position();
            yRot = car.getInterpolation().yRot();
            xRot = car.getInterpolation().xRot();
        } else {
            position = car.position();
            yRot = car.getYRot();
            xRot = car.getXRot();
        }

        return new ServerboundMoveSlotCarPacket(
                position,
                yRot,
                xRot,
                car.onGround(),
                car.getRespawnTimer(),
                car.isDerailed(),
                car.onTrack()
        );
    }
}
