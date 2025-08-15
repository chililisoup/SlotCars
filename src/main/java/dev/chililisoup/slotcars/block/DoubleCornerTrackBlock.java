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
    protected @NotNull MapCodec<? extends AbstractTrackBlock> codec() {
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
                                new Vec3(-0.15, 0.1, 0.375),
                                new Vec3(-0.0625, 0.1, 0.125),
                                new Vec3(0.125, 0.1, -0.0625),
                                new Vec3(0.375, 0.1, -0.15),
                                new Vec3(0.5, 0.1, -0.15625)
                        },
                        south,
                        east
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.15625, 0.1, 0.5),
                                new Vec3(0.15, 0.1, 0.4),
                                new Vec3(0.25, 0.1, 0.25),
                                new Vec3(0.4, 0.1, 0.15),
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
