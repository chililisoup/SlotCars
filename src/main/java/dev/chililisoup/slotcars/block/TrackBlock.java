package dev.chililisoup.slotcars.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
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
    public static final EnumProperty<RailShape> SHAPE = BlockStateProperties.RAIL_SHAPE;

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
        return blockState.getValue(SHAPE).isSlope() ? SHAPE_BOTTOM_HALF : SHAPE_FLAT;
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

    private static final Map<RailShape, Path[]> PATHS = Maps.newEnumMap(Util.make(() -> {
        Vec3i north = Direction.NORTH.getUnitVec3i();
        Vec3i east = Direction.EAST.getUnitVec3i();
        Vec3i south = Direction.SOUTH.getUnitVec3i();

        Path[] straightNorth = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.15625, 0.1, 0.5),
                                new Vec3(-0.15625, 0.1, -0.5)
                        },
                        south,
                        north
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.15625, 0.1, 0.5),
                                new Vec3(0.15625, 0.1, -0.5)
                        },
                        south,
                        north
                )
        };
        Path[] slopeNorth = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.15625, 0.1, 0.5),
                                new Vec3(-0.15625, 1.1, -0.5)
                        },
                        south,
                        north.above()
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.15625, 0.1, 0.5),
                                new Vec3(0.15625, 1.1, -0.5)
                        },
                        south,
                        north.above()
                )
        };
        Path[] curveSouthEast = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.15625, 0.1, 0.5),
                                new Vec3(-0.139796, 0.1, 0.353971),
                                new Vec3(-0.091261, 0.1, 0.215264),
                                new Vec3(-0.013077, 0.1, 0.090835),
                                new Vec3(0.090835, 0.1, -0.013077),
                                new Vec3(0.215264, 0.1, -0.091261),
                                new Vec3(0.353971, 0.1, -0.139796),
                                new Vec3(0.5, 0.1, -0.15625)
                        },
                        south,
                        east
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.15625, 0.1, 0.5),
                                new Vec3(0.173074, 0.1, 0.393775),
                                new Vec3(0.2219, 0.1, 0.297949),
                                new Vec3(0.297949, 0.1, 0.2219),
                                new Vec3(0.393775, 0.1, 0.173074),
                                new Vec3(0.5, 0.1, 0.15625)
                        },
                        south,
                        east
                )
        };

        return ImmutableMap.of(
                RailShape.NORTH_SOUTH,
                straightNorth,
                RailShape.EAST_WEST,
                rotatePaths(straightNorth, 1),
                RailShape.ASCENDING_EAST,
                rotatePaths(slopeNorth, 1),
                RailShape.ASCENDING_WEST,
                rotatePaths(slopeNorth, 3),
                RailShape.ASCENDING_NORTH,
                slopeNorth,
                RailShape.ASCENDING_SOUTH,
                rotatePaths(slopeNorth, 2),
                RailShape.SOUTH_EAST,
                curveSouthEast,
                RailShape.SOUTH_WEST,
                rotatePaths(curveSouthEast, 1),
                RailShape.NORTH_WEST,
                rotatePaths(curveSouthEast, 2),
                RailShape.NORTH_EAST,
                rotatePaths(curveSouthEast, 3)
        );
    }));

    @Override
    public Path[] getPaths(BlockState blockState) {
        return PATHS.get(blockState.getValue(SHAPE));
    }

    public static List<BlockPos> getConnectingBlocks(BlockPos blockPos, RailShape railShape) {
        List<BlockPos> allFound = new ArrayList<>();

        for (Path path : PATHS.get(railShape)) {
            allFound.add(blockPos.offset(path.entrance()));
            allFound.add(blockPos.offset(path.exit()));
        }

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
