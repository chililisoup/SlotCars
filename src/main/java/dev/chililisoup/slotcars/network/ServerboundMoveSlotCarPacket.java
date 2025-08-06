package dev.chililisoup.slotcars.network;

import dev.chililisoup.slotcars.SlotCars;
import dev.chililisoup.slotcars.entity.SlotCar;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public record ServerboundMoveSlotCarPacket(Vec3 position, float yRot, float xRot, boolean onGround) implements CustomPacketPayload {
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
        });
    }

    public static ServerboundMoveSlotCarPacket fromEntity(Entity entity) {
        return entity.isInterpolating() ?
                new ServerboundMoveSlotCarPacket(
                        entity.getInterpolation().position(), entity.getInterpolation().yRot(), entity.getInterpolation().xRot(), entity.onGround()
                ) :
                new ServerboundMoveSlotCarPacket(
                        entity.position(), entity.getYRot(), entity.getXRot(), entity.onGround()
                );
    }
}
