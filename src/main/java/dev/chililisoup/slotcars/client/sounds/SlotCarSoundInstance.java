package dev.chililisoup.slotcars.client.sounds;

import dev.chililisoup.slotcars.entity.SlotCar;
import dev.chililisoup.slotcars.reg.ModSoundEvents;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public class SlotCarSoundInstance extends AbstractTickableSoundInstance {
    private static final float VOLUME_MIN = 0.0F;
    private static final float VOLUME_MAX = 0.7F;
    private static final float PITCH_MIN = 0.75F;
    private static final float PITCH_MAX = 1.5F;
    private final SlotCar car;

    public SlotCarSoundInstance(SlotCar car) {
        super(ModSoundEvents.SLOT_CAR_TRAVEL, SoundSource.NEUTRAL, SoundInstance.createUnseededRandom());
        this.car = car;
        this.looping = true;
        this.delay = 0;
        this.volume = 0.0F;
        this.x = (float)car.getX();
        this.y = (float)car.getY();
        this.z = (float)car.getZ();
    }

    @Override
    public boolean canPlaySound() {
        return !this.car.isSilent();
    }

    @Override
    public boolean canStartSilent() {
        return true;
    }

    @Override
    public void tick() {
        if (this.car.isRemoved()) {
            this.stop();
            return;
        }

        this.x = (float)this.car.getX();
        this.y = (float)this.car.getY();
        this.z = (float)this.car.getZ();
        float speed = (float)this.car.getDeltaMovement().horizontalDistance();

        if (speed >= 0.01F && this.car.level().tickRateManager().runsNormally() && this.car.onTrack()) {
            float lerpAmount = Mth.clamp(speed, 0.0F, 1F);
            this.pitch = Mth.lerp(lerpAmount, PITCH_MIN, PITCH_MAX);
            this.volume = Mth.lerp(lerpAmount, VOLUME_MIN, VOLUME_MAX);
        } else {
            this.volume *= 0.8F;
        }
    }
}
