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

public class WideCornerTrackBlock extends AbstractTrackBlock {
    public static final MapCodec<WideCornerTrackBlock> CODEC = simpleCodec(WideCornerTrackBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public WideCornerTrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<WideCornerTrackBlock> codec() {
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
                                new Vec3(-0.28125, 0.1, 0.5),
                                new Vec3(-0.266239, 0.1, 0.347586),
                                new Vec3(-0.221781, 0.1, 0.201029),
                                new Vec3(-0.149586, 0.1, 0.065961),
                                new Vec3(-0.052427, 0.1, -0.052427),
                                new Vec3(0.065961, 0.1, -0.149586),
                                new Vec3(0.201029, 0.1, -0.221781),
                                new Vec3(0.347586, 0.1, -0.266239),
                                new Vec3(0.5, 0.1, -0.28125)
                        },
                        south,
                        east
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.03125, 0.1, 0.5),
                                new Vec3(0.047222, 0.1, 0.378679),
                                new Vec3(0.094051, 0.1, 0.265625),
                                new Vec3(0.168544, 0.1, 0.168544),
                                new Vec3(0.265625, 0.1, 0.094051),
                                new Vec3(0.378679, 0.1, 0.047222),
                                new Vec3(0.5, 0.1, 0.03125)
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
