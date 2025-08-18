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
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ChicaneTrackBlock extends AbstractTrackBlock {
    public static final MapCodec<ChicaneTrackBlock> CODEC = simpleCodec(ChicaneTrackBlock::new);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty INVERTED = BlockStateProperties.INVERTED;

    public ChicaneTrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH).setValue(INVERTED, false));
    }

    @Override
    protected @NotNull MapCodec<ChicaneTrackBlock> codec() {
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
            level.setBlock(blockPos, blockState.cycle(INVERTED), 3);

        return InteractionResult.SUCCESS;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, INVERTED);
    }

    private static final Map<Direction, Path[]> LEFT_PATHS = Maps.newEnumMap(Util.make(() -> {
        Vec3i north = Direction.NORTH.getUnitVec3i();
        Vec3i south = Direction.SOUTH.getUnitVec3i();

        Path[] chicaneLeftNorth = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.15625, 0.1, 0.5),
                                new Vec3(-0.28125, 0.1, -0.5)
                        },
                        south,
                        north
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.15625, 0.1, 0.5),
                                new Vec3(0.03125, 0.1, -0.5)
                        },
                        south,
                        north
                )
        };

        return ImmutableMap.of(
                Direction.EAST,
                rotatePaths(chicaneLeftNorth, 1),
                Direction.WEST,
                rotatePaths(chicaneLeftNorth, 3),
                Direction.NORTH,
                chicaneLeftNorth,
                Direction.SOUTH,
                rotatePaths(chicaneLeftNorth, 2)
        );
    }));

    private static final Map<Direction, Path[]> RIGHT_PATHS = Maps.newEnumMap(Util.make(() -> ImmutableMap.of(
            Direction.EAST,
            multiplyPaths(LEFT_PATHS.get(Direction.EAST), new Vec3i(1, 1, -1)),
            Direction.WEST,
            multiplyPaths(LEFT_PATHS.get(Direction.WEST), new Vec3i(1, 1, -1)),
            Direction.NORTH,
            multiplyPaths(LEFT_PATHS.get(Direction.NORTH), new Vec3i(-1, 1, 1)),
            Direction.SOUTH,
            multiplyPaths(LEFT_PATHS.get(Direction.SOUTH), new Vec3i(-1, 1, 1))
    )));

    @Override
    public Path[] getPaths(BlockState blockState) {
        return blockState.getValue(INVERTED) ?
                RIGHT_PATHS.get(blockState.getValue(FACING)) :
                LEFT_PATHS.get(blockState.getValue(FACING));
    }
}
