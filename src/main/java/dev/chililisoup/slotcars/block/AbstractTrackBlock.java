package dev.chililisoup.slotcars.block;

import com.mojang.serialization.MapCodec;
import dev.chililisoup.slotcars.reg.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractTrackBlock extends Block {
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;
    private static final VoxelShape SHAPE_FLAT = Block.column(16.0, 0.0, 2.0);
    private static final VoxelShape SHAPE_SLOPE = Block.column(16.0, 0.0, 8.0);
    private final boolean isStraight;

    public static boolean isTrack(Level level, BlockPos blockPos) {
        return isTrack(level.getBlockState(blockPos));
    }

    public static boolean isTrack(BlockState blockState) {
        return blockState.is(ModBlockTags.TRACKS) && blockState.getBlock() instanceof AbstractTrackBlock;
    }

    protected AbstractTrackBlock(Boolean isStraight, Properties properties) {
        super(properties);
        this.isStraight = isStraight;
    }

    @Override
    protected abstract @NotNull MapCodec<? extends AbstractTrackBlock> codec();

    public boolean isStraight() {
        return this.isStraight;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return blockState.getValue(SHAPE).isSlope() ? SHAPE_SLOPE : SHAPE_FLAT;
    }

    @Override
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        if (!blockState2.is(blockState.getBlock())) {
            this.updateState(blockState, level, blockPos, bl);
        }
    }

    protected void updateState(BlockState blockState, Level level, BlockPos blockPos, Block block) {}

    protected void updateState(BlockState blockState, Level level, BlockPos blockPos, boolean bl) {
        blockState = this.updateDir(level, blockPos, blockState, true);
        if (this.isStraight)
            level.neighborChanged(blockState, blockPos, this, null, bl);

    }

    protected BlockState updateDir(Level level, BlockPos blockPos, BlockState blockState, boolean updateLevel) {
        if (level.isClientSide)
            return blockState;

        RailShape railShape = blockState.getValue(SHAPE);
        return new TrackState(level, blockPos, blockState).place(level.hasNeighborSignal(blockPos), updateLevel, railShape).getState();
    }

    @Override
    protected void neighborChanged(BlockState blockState, Level level, BlockPos blockPos, Block block, @Nullable Orientation orientation, boolean bl) {
        if (!level.isClientSide && level.getBlockState(blockPos).is(this)) {
            this.updateState(blockState, level, blockPos, block);
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        Direction direction = blockPlaceContext.getHorizontalDirection();
        boolean eastWest = direction == Direction.EAST || direction == Direction.WEST;
        return super.defaultBlockState().setValue(SHAPE, eastWest ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH);
    }
}
