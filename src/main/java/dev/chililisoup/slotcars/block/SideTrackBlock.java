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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class SideTrackBlock extends AbstractTrackBlock {
    public static final MapCodec<SideTrackBlock> CODEC = simpleCodec(SideTrackBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public SideTrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<SideTrackBlock> codec() {
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
    protected @NotNull InteractionResult useWithoutItem(BlockState blockState, Level level, BlockPos blockPos, Player player, BlockHitResult blockHitResult) {
        if (!player.isShiftKeyDown() || !player.getAbilities().mayBuild)
            return InteractionResult.PASS;

        if (!level.isClientSide)
            level.setBlock(blockPos, blockState.setValue(FACING, blockState.getValue(FACING).getOpposite()), 3);

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    private static final Map<Direction, Path[]> PATHS = Maps.newEnumMap(Util.make(() -> {
        Vec3i north = Direction.NORTH.getUnitVec3i();
        Vec3i south = Direction.SOUTH.getUnitVec3i();

        Path[] northPaths = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.28125, 0.1, 0.5),
                                new Vec3(-0.28125, 0.1, -0.5)
                        },
                        south,
                        north
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.03125, 0.1, 0.5),
                                new Vec3(0.03125, 0.1, -0.5)
                        },
                        south,
                        north
                )
        };

        return ImmutableMap.of(
                Direction.EAST,
                rotatePaths(northPaths, 1),
                Direction.WEST,
                rotatePaths(northPaths, 3),
                Direction.NORTH,
                northPaths,
                Direction.SOUTH,
                rotatePaths(northPaths, 2)
        );
    }));

    @Override
    public Path[] getPaths(BlockState blockState) {
        return PATHS.get(blockState.getValue(FACING));
    }
}
