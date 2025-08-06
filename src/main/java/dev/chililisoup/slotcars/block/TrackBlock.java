package dev.chililisoup.slotcars.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class TrackBlock extends AbstractTrackBlock {
    public static final MapCodec<TrackBlock> CODEC = simpleCodec(TrackBlock::new);
    @Override
    public @NotNull MapCodec<TrackBlock> codec() {
        return CODEC;
    }

    public TrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH));
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return blockState.getValue(SHAPE).isSlope() ? SHAPE_SLOPE : SHAPE_FLAT;
    }

    @Override
    protected void onPlace(BlockState blockState, Level level, BlockPos blockPos, BlockState blockState2, boolean bl) {
        if (!blockState2.is(blockState.getBlock())) {
            this.updateState(blockState, level, blockPos);
        }
    }

    protected void updateState(BlockState blockState, Level level, BlockPos blockPos, Block block) {
        if (block.defaultBlockState().isSignalSource() && new TrackState(level, blockPos, blockState).countPotentialConnections() == 3) {
            this.updateDir(level, blockPos, blockState, false);
        }
    }

    protected void updateState(BlockState blockState, Level level, BlockPos blockPos) {
        this.updateDir(level, blockPos, blockState, true);
    }

    protected void updateDir(Level level, BlockPos blockPos, BlockState blockState, boolean updateLevel) {
        if (level.isClientSide)
            return;

        RailShape railShape = blockState.getValue(SHAPE);
        new TrackState(level, blockPos, blockState).place(level.hasNeighborSignal(blockPos), updateLevel, railShape);
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

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    private static final Map<RailShape, Pair<Vec3[], Vec3[]>> PATHS = Maps.newEnumMap(Util.make(() -> {
        Pair<Vec3[], Vec3[]> straightNorth = Pair.of(
                new Vec3[]{
                        new Vec3(-0.15625, 0.1, 0.5),
                        new Vec3(-0.15625, 0.1, -0.5)
                },
                new Vec3[]{
                        new Vec3(0.15625, 0.1, 0.5),
                        new Vec3(0.15625, 0.1, -0.5)
                }
        );
        Pair<Vec3[], Vec3[]> slopeNorth = Pair.of(
                new Vec3[]{
                        new Vec3(-0.15625, 0.1, 0.5),
                        new Vec3(-0.15625, 1.1, -0.5)
                },
                new Vec3[]{
                        new Vec3(0.15625, 0.1, 0.5),
                        new Vec3(0.15625, 1.1, -0.5)
                }
        );
        Pair<Vec3[], Vec3[]> curveSouthEast = Pair.of(
                new Vec3[]{
                        new Vec3(-0.15625, 0.1, 0.5),
                        new Vec3(-0.15, 0.1, 0.375),
                        new Vec3(-0.0625, 0.1, 0.125),
                        new Vec3(0.125, 0.1, -0.0625),
                        new Vec3(0.375, 0.1, -0.15),
                        new Vec3(0.5, 0.1, -0.15625)
                },
                new Vec3[]{
                        new Vec3(0.15625, 0.1, 0.5),
                        new Vec3(0.15, 0.1, 0.4),
                        new Vec3(0.25, 0.1, 0.25),
                        new Vec3(0.4, 0.1, 0.15),
                        new Vec3(0.5, 0.1, 0.15625)
                }
        );

        return ImmutableMap.of(
                RailShape.NORTH_SOUTH,
                straightNorth,
                RailShape.EAST_WEST,
                rotatePath(straightNorth, 1),
                RailShape.ASCENDING_EAST,
                rotatePath(slopeNorth, 1),
                RailShape.ASCENDING_WEST,
                rotatePath(slopeNorth, 3),
                RailShape.ASCENDING_NORTH,
                slopeNorth,
                RailShape.ASCENDING_SOUTH,
                rotatePath(slopeNorth, 2),
                RailShape.SOUTH_EAST,
                curveSouthEast,
                RailShape.SOUTH_WEST,
                rotatePath(curveSouthEast, 1),
                RailShape.NORTH_WEST,
                rotatePath(curveSouthEast, 2),
                RailShape.NORTH_EAST,
                rotatePath(curveSouthEast, 3)
        );
    }));

    @Override
    public Pair<Vec3[], Vec3[]> getPaths(BlockState blockState) {
        return PATHS.get(blockState.getValue(SHAPE));
    }

    public static List<BlockPos> getConnectingBlocks(BlockPos blockPos, RailShape railShape) {
        Pair<Vec3[], Vec3[]> paths = PATHS.get(railShape);
        Vec3[] first = paths.getFirst();
        Vec3[] second = paths.getSecond();

        List<BlockPos> allFound = new ArrayList<>();

        allFound.add(getNextBlockPos(blockPos, first[0]));
        allFound.add(getNextBlockPos(blockPos, first[first.length - 1]));
        allFound.add(getNextBlockPos(blockPos, second[0]));
        allFound.add(getNextBlockPos(blockPos, second[second.length - 1]));

        List<BlockPos> connections = new ArrayList<>();
        allFound.forEach(checkPos -> {
            if (checkPos == null)
                return;
            if (connections.stream().anyMatch(pos -> pos.equals(checkPos)))
                return;
            connections.add(checkPos);
        });

        return connections;
    }
}
