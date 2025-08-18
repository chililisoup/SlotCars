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

public class HairpinTrackBlock extends AbstractTrackBlock {
    public static final MapCodec<HairpinTrackBlock> CODEC = simpleCodec(HairpinTrackBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public HairpinTrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<HairpinTrackBlock> codec() {
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
        Vec3i south = Direction.SOUTH.getUnitVec3i();

        Path[] northPath = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.15625, 0.1, 0.5),
                                new Vec3(-0.21875, 0.1, 0.28125),
                                new Vec3(-0.25, 0.1, -0.03125),
                                new Vec3(-0.25, 0.1, -0.15625),
                                new Vec3(-0.125, 0.1, -0.25),

                                new Vec3(0, 0.1, -0.28125),

                                new Vec3(0.125, 0.1, -0.25),
                                new Vec3(0.25, 0.1, -0.15625),
                                new Vec3(0.25, 0.1, -0.03125),
                                new Vec3(0.21875, 0.1, 0.28125),
                                new Vec3(0.15625, 0.1, 0.5)
                        },
                        south,
                        south
                )
        };

        return ImmutableMap.of(
                Direction.EAST,
                rotatePaths(northPath, 1),
                Direction.WEST,
                rotatePaths(northPath, 3),
                Direction.NORTH,
                northPath,
                Direction.SOUTH,
                rotatePaths(northPath, 2)
        );
    }));

    @Override
    public Path[] getPaths(BlockState blockState) {
        return PATHS.get(blockState.getValue(FACING));
    }
}
