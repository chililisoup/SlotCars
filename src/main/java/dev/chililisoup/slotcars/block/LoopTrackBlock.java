package dev.chililisoup.slotcars.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class LoopTrackBlock extends AbstractTrackBlock {
    public static final MapCodec<LoopTrackBlock> CODEC = simpleCodec(LoopTrackBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;

    public LoopTrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.X).setValue(INVERTED, false));
    }

    @Override
    protected @NotNull MapCodec<LoopTrackBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPE_BOTTOM_HALF;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState().setValue(AXIS, blockPlaceContext.getHorizontalDirection().getAxis());
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
        if (!player.isShiftKeyDown() || !player.getAbilities().mayBuild)
            return InteractionResult.PASS;

        if (!level.isClientSide)
            level.setBlock(blockPos, blockState.cycle(INVERTED), 3);

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS, INVERTED);
    }

    @Override
    public boolean canDerail() {
        return false;
    }

    @Override
    public boolean isLoop() {
        return true;
    }

    private static final Map<Direction.Axis, Path[]> RIGHT_PATHS = Maps.newEnumMap(Util.make(() -> {
        Vec3i north = Direction.NORTH.getUnitVec3i();
        Vec3i south = Direction.SOUTH.getUnitVec3i();

        Path[] firstPath = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.28125, 0.1, 0.5),
                                new Vec3(-0.34375, 0.1, 0.1875),
                                new Vec3(-0.34375, 0.1, -0.125),
                                new Vec3(-0.28125, 0.2875, -0.3125),
                                new Vec3(-0.21875, 0.475, -0.3125),

                                new Vec3(-0.125, 0.6625, -0.125),
                                new Vec3(-0.0625, 0.6625, 0.125),

                                new Vec3(0.03125, 0.425, 0.3125),
                                new Vec3(0.09375, 0.2875, 0.3125),
                                new Vec3(0.15625, 0.1, 0.125),
                                new Vec3(0.15625, 0.1, -0.125),
                                new Vec3(0.0625, 0.1, -0.3125), // Midpoint between 10 & 11 so that 3 & 4 is never closer
                                new Vec3(-0.03125, 0.1, -0.5),
                        },
                        south,
                        north
                )
        };

        Path[] secondPath = rotatePaths(firstPath, 2);
        Path[] zPaths = ArrayUtils.addAll(firstPath, secondPath);

        return ImmutableMap.of(
                Direction.Axis.X,
                rotatePaths(zPaths, 1),
                Direction.Axis.Z,
                zPaths
        );
    }));

    private static final Map<Direction.Axis, Path[]> LEFT_PATHS = Maps.newEnumMap(Util.make(() -> ImmutableMap.of(
            Direction.Axis.X,
            multiplyPaths(RIGHT_PATHS.get(Direction.Axis.X), new Vec3i(1, 1, -1)),
            Direction.Axis.Z,
            multiplyPaths(RIGHT_PATHS.get(Direction.Axis.Z), new Vec3i(-1, 1, 1))
    )));

    @Override
    public Path[] getPaths(BlockState blockState) {
        return blockState.getValue(INVERTED) ?
                LEFT_PATHS.get(blockState.getValue(AXIS)) :
                RIGHT_PATHS.get(blockState.getValue(AXIS));
    }
}
