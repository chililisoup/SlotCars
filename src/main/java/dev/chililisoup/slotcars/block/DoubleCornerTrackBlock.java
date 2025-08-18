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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class DoubleCornerTrackBlock extends AbstractTrackBlock {
    public static final MapCodec<DoubleCornerTrackBlock> CODEC = simpleCodec(DoubleCornerTrackBlock::new);
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    public DoubleCornerTrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected @NotNull MapCodec<DoubleCornerTrackBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPE_FLAT;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState().setValue(AXIS, blockPlaceContext.getHorizontalDirection().getAxis());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AXIS);
    }

    private static final Map<Direction.Axis, Path[]> PATHS = Maps.newEnumMap(Util.make(() -> {
        Vec3i east = Direction.EAST.getUnitVec3i();
        Vec3i south = Direction.SOUTH.getUnitVec3i();

        Path[] xPathsFirst = new Path[]{
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

        Path[] xPathsSecond = rotatePaths(xPathsFirst, 2);
        Path[] xPaths = ArrayUtils.addAll(xPathsFirst, xPathsSecond);

        return ImmutableMap.of(
                Direction.Axis.X,
                xPaths,
                Direction.Axis.Z,
                rotatePaths(xPaths, 1)
        );
    }));

    @Override
    public Path[] getPaths(BlockState blockState) {
        return PATHS.get(blockState.getValue(AXIS));
    }
}
