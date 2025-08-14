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

    @Override
    public @NotNull InteractionResult useOn(UseOnContext useOnContext) {
        Player player = useOnContext.getPlayer();
        if (player == null) return InteractionResult.FAIL;
        if (getCar(player) != null) return InteractionResult.PASS;

        Level level = useOnContext.getLevel();
        if (!level.getBlockState(useOnContext.getClickedPos()).is(ModBlockTags.TRACKS))
            return super.useOn(useOnContext);

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
            SlotCar car = SlotCar.createSlotCar(
                    level,
                    useOnContext.getClickLocation(),
                    player.getYHeadRot(),
                    EntitySpawnReason.DISPENSER,
                    useOnContext.getItemInHand(),
                    player
            );
            serverLevel.addFreshEntity(car);
        }

        player.awardStat(Stats.ITEM_USED.get(this));
        player.gameEvent(GameEvent.ITEM_INTERACT_START);

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
