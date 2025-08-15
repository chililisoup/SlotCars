package dev.chililisoup.slotcars.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class HalfRampTrackBlock extends AbstractTrackBlock {
    public static final MapCodec<HalfRampTrackBlock> CODEC = simpleCodec(HalfRampTrackBlock::new);
    public static final EnumProperty<Half> HALF = BlockStateProperties.HALF;
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    public HalfRampTrackBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(HALF, Half.BOTTOM).setValue(FACING, Direction.NORTH));
    }

    @Override
    protected @NotNull MapCodec<? extends AbstractTrackBlock> codec() {
        return CODEC;
    }

    @Override
    protected @NotNull VoxelShape getShape(BlockState blockState, BlockGetter blockGetter, BlockPos blockPos, CollisionContext collisionContext) {
        return blockState.getValue(HALF) == Half.BOTTOM ?
                SHAPE_SLOPE :
                SHAPE_SLOPE_TOP;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext blockPlaceContext) {
        Direction direction = blockPlaceContext.getClickedFace();
        BlockPos blockPos = blockPlaceContext.getClickedPos();

        boolean bottom = direction != Direction.DOWN && (
                direction == Direction.UP || !(blockPlaceContext.getClickLocation().y - blockPos.getY() > 0.5)
        );

        return this.defaultBlockState()
                .setValue(HALF, bottom ? Half.BOTTOM : Half.TOP)
                .setValue(FACING, blockPlaceContext.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HALF, FACING);
    }

    @Override
    protected @NotNull InteractionResult useItemOn(
            ItemStack itemStack, BlockState blockState, Level level, BlockPos blockPos, Player player, InteractionHand interactionHand, BlockHitResult blockHitResult
    ) {
        if (blockHitResult.getDirection() != Direction.UP || blockState.getValue(HALF) != Half.BOTTOM)
            return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!(player.getItemInHand(interactionHand).getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof HalfRampTrackBlock))
            return InteractionResult.TRY_WITH_EMPTY_HAND;

        if (!level.isClientSide)
            level.setBlock(blockPos, blockState.setValue(HALF, Half.TOP), 11);

        return InteractionResult.SUCCESS;
    }

    private static final Map<Direction, Path[]> BOTTOM_PATHS = Maps.newEnumMap(Util.make(() -> {
        Vec3i north = Direction.NORTH.getUnitVec3i();
        Vec3i south = Direction.SOUTH.getUnitVec3i();

        Path[] slopeSouth = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.15625, 0.1, 0.5),
                                new Vec3(-0.15625, 0.6, -0.5)
                        },
                        south,
                        north
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.15625, 0.1, 0.5),
                                new Vec3(0.15625, 0.6, -0.5)
                        },
                        south,
                        north
                )
        };

        return ImmutableMap.of(
                Direction.EAST,
                rotatePaths(slopeSouth, 3),
                Direction.WEST,
                rotatePaths(slopeSouth, 1),
                Direction.NORTH,
                rotatePaths(slopeSouth, 2),
                Direction.SOUTH,
                slopeSouth
        );
    }));

    private static final Map<Direction, Path[]> TOP_PATHS = Maps.newEnumMap(Util.make(() -> {
        Vec3i north = Direction.NORTH.getUnitVec3i();
        Vec3i south = Direction.SOUTH.getUnitVec3i();

        Path[] slopeSouth = new Path[]{
                new Path(
                        new Vec3[]{
                                new Vec3(-0.15625, 0.6, 0.5),
                                new Vec3(-0.15625, 1.1, -0.5)
                        },
                        south,
                        north.above()
                ),
                new Path(
                        new Vec3[]{
                                new Vec3(0.15625, 0.6, 0.5),
                                new Vec3(0.15625, 1.1, -0.5)
                        },
                        south,
                        north.above()
                )
        };

        return ImmutableMap.of(
                Direction.EAST,
                rotatePaths(slopeSouth, 3),
                Direction.WEST,
                rotatePaths(slopeSouth, 1),
                Direction.NORTH,
                rotatePaths(slopeSouth, 2),
                Direction.SOUTH,
                slopeSouth
        );
    }));

    @Override
    public Path[] getPaths(BlockState blockState) {
        Direction facing = blockState.getValue(FACING);

        return blockState.getValue(HALF) == Half.BOTTOM ?
                BOTTOM_PATHS.get(facing) :
                TOP_PATHS.get(facing);
    }
}
