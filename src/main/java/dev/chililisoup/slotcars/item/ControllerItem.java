package dev.chililisoup.slotcars.item;

import dev.chililisoup.slotcars.entity.SlotCar;
import dev.chililisoup.slotcars.reg.ModBlockTags;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class ControllerItem extends Item {
    public ControllerItem(Properties properties) {
        super(properties);
    }

    private static @Nullable SlotCar getCar(Player player) {
        return player.getAttachedOrElse(SlotCar.ACTIVE_SLOT_CAR, new SlotCar.SlotCarHolder()).get();
    }

    private void spawnCar(Level level, Player player, UseOnContext useOnContext) {
        level.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.FISHING_BOBBER_THROW,
                SoundSource.NEUTRAL,
                0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F)
        );

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.addFreshEntity(SlotCar.createSlotCar(
                    level,
                    useOnContext.getClickLocation(),
                    player.getYHeadRot(),
                    EntitySpawnReason.DISPENSER,
                    useOnContext.getItemInHand(),
                    player
            ));
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.gameEvent(GameEvent.ITEM_INTERACT_START);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext useOnContext) {
        Player player = useOnContext.getPlayer();
        if (player == null) return InteractionResult.FAIL;

        Level level = useOnContext.getLevel();
        boolean clickedTrack = level.getBlockState(useOnContext.getClickedPos()).is(ModBlockTags.TRACKS);

        SlotCar car;
        if ((car = getCar(player)) != null) {
            if (!clickedTrack || car.onTrack() || !car.onGround()) return InteractionResult.PASS;

            car.discard();
            this.spawnCar(level, player, useOnContext);
            return InteractionResult.SUCCESS;
        }

        if (!clickedTrack) return super.useOn(useOnContext);

        this.spawnCar(level, player, useOnContext);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @NotNull InteractionResult use(Level level, Player player, InteractionHand interactionHand) {
        SlotCar car = getCar(player);
        if (car == null) return super.use(level, player, interactionHand);

        player.startUsingItem(interactionHand);
        return InteractionResult.PASS;
    }

    @Override
    public int getUseDuration(ItemStack itemStack, LivingEntity livingEntity) {
        return Integer.MAX_VALUE;
    }
}
