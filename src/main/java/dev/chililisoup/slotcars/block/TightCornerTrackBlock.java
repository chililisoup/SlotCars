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
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class TightCornerTrackBlock extends AbstractTrackBlock {
    public static final MapCodec<TightCornerTrackBlock> CODEC = simpleCodec(TightCornerTrackBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public TightCornerTrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<TightCornerTrackBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return SHAPE_FLAT;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        return this.defaultBlockState().setValue(FACING, blockPlaceContext.getHorizontalDirection());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static final Map<Direction, Path[]> PATHS = Maps.newEnumMap(Util.make(() -> {
        Vec3i east = Direction.EAST.getUnitVec3i();
        Vec3i south = Direction.SOUTH.getUnitVec3i();

        Path[] curveSouthEast = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.03125, 0.1, 0.5),
                                new Vec3(-0.013148, 0.1, 0.362502),
                                new Vec3(0.039924, 0.1, 0.234375),
                                new Vec3(0.12435, 0.1, 0.12435),
                                new Vec3(0.234375, 0.1, 0.039924),
                                new Vec3(0.362502, 0.1, -0.013148),
                                new Vec3(0.5, 0.1, -0.03125)
                        },
                        south,
                        east
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.28125, 0.1, 0.5),
                                new Vec3(0.297901, 0.1, 0.416288),
                                new Vec3(0.34532, 0.1, 0.34532),
                                new Vec3(0.416288, 0.1, 0.297901),
                                new Vec3(0.5, 0.1, 0.28125)
                        },
                        south,
                        east
                )
        };

        return ImmutableMap.of(
                Direction.EAST,
                rotatePaths(curveSouthEast, 1),
                Direction.SOUTH,
                rotatePaths(curveSouthEast, 2),
                Direction.WEST,
                rotatePaths(curveSouthEast, 3),
                Direction.NORTH,
                curveSouthEast
        );
    }));

    @Override
    public Path[] getPaths(BlockState blockState) {
        return PATHS.get(blockState.getValue(FACING));
    }
}
