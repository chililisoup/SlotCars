package dev.chililisoup.slotcars.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
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
        super(false, properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SHAPE, RailShape.NORTH_SOUTH));
    }

    @Override
    protected void updateState(BlockState blockState, Level level, BlockPos blockPos, Block block) {
        if (block.defaultBlockState().isSignalSource() && new TrackState(level, blockPos, blockState).countPotentialConnections() == 3) {
            this.updateDir(level, blockPos, blockState, false);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SHAPE);
    }

    private static Pair<Vec3[], Vec3[]> rotatePath(Pair<Vec3[], Vec3[]> path, int clockwiseTurns) {
        Vec3[] first = new Vec3[path.getFirst().length];
        Vec3[] second = new Vec3[path.getSecond().length];

        if (clockwiseTurns == 1) {
            for (int i = 0; i < first.length; i++)
                first[i] = path.getFirst()[i].rotateClockwise90();

            for (int i = 0; i < second.length; i++)
                second[i] = path.getSecond()[i].rotateClockwise90();
        } else if (clockwiseTurns == 2) {
            for (int i = 0; i < first.length; i++)
                first[i] = path.getFirst()[i].multiply(-1, 1, -1);

            for (int i = 0; i < second.length; i++)
                second[i] = path.getSecond()[i].multiply(-1, 1, -1);
        } else if (clockwiseTurns == 3) {
            for (int i = 0; i < first.length; i++)
                first[i] = path.getFirst()[i].rotateClockwise90().multiply(-1, 1, -1);

            for (int i = 0; i < second.length; i++)
                second[i] = path.getSecond()[i].rotateClockwise90().multiply(-1, 1, -1);
        } else {
            return path;
        }

        return Pair.of(first, second);
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

    public static Pair<Vec3[], Vec3[]> getPaths(BlockState blockState) {
        return PATHS.get(blockState.getValue(SHAPE));
    }

    public static Vec3[] reversePath(Vec3[] path) {
        Vec3[] reversed = new Vec3[path.length];
        for (int i = 0; i < path.length; i++)
            reversed[i] = path[path.length - 1 - i];

        return reversed;
    }

    public static Vec3 closestPointToPath(Vec3 pathStart, Vec3 pathEnd, Vec3 position) {
        Vec3 point = position.subtract(pathStart);
        Vec3 line = pathEnd.subtract(pathStart);
        return pathStart.add(point.projectedOn(line));
    }

    public static double distanceToPathSqr(Vec3 pathStart, Vec3 pathEnd, Vec3 position) {
        return closestPointToPath(pathStart, pathEnd, position).distanceToSqr(position);
    }

    public static Pair<Integer, Double> closestOrderedPathPoint(Vec3[] path, Vec3 position) {
        int closestIndex = -1;
        double closestDistance = Double.MAX_VALUE;

        for (int i = 0; i < path.length - 1; i++) {
            double distance = path[i].distanceToSqr(position);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }

        return Pair.of(closestIndex, closestDistance);
    }

    public static Pair<Integer, Double> closestPathPoint(Vec3[] path, Vec3 position) {
        int closestIndex = -1;
        double closestDistance = Double.MAX_VALUE;

        for (int i = 0; i < path.length - 1; i++) {
            Vec3 middle = path[i].add(path[i + 1]).scale(0.5);
            double distance = middle.distanceToSqr(position);
            if (distance < closestDistance) {
                closestDistance = distance;
                closestIndex = i;
            }
        }

        return Pair.of(closestIndex, closestDistance);
    }

    public static Vec3[] getClosestPath(BlockState blockState, BlockPos blockPos, Vec3 position, Vec3 direction) {
        Pair<Vec3[], Vec3[]> paths = getPaths(blockState);
        Vec3[] first = paths.getFirst();
        Vec3[] second = paths.getSecond();
        Vec3 center = blockPos.getBottomCenter();

        Pair<Integer, Double> closestFirst = closestPathPoint(first, position.subtract(center));
        Pair<Integer, Double> closestSecond = closestPathPoint(second, position.subtract(center));

        if (closestFirst.getSecond() < closestSecond.getSecond()) {
            int index = closestFirst.getFirst();
            Vec3 pathVector = first[index + 1].subtract(first[index]).normalize();
            if (pathVector.dot(direction) < 0)
                return reversePath(first);
            return first;
        }

        int index = closestSecond.getFirst();
        Vec3 pathVector = second[index + 1].subtract(second[index]).normalize();
        if (pathVector.dot(direction) < 0)
            return reversePath(second);
        return second;
    }

    public static @Nullable Vec3[] getConnectingPath(BlockState blockState, Vec3 connectingPoint) {
        Pair<Vec3[], Vec3[]> paths = getPaths(blockState);
        Vec3[] first = paths.getFirst();
        Vec3[] second = paths.getSecond();

        if (first[0].subtract(connectingPoint).lengthSqr() < 0.0001)
            return first;
        if (first[first.length - 1].subtract(connectingPoint).lengthSqr() < 0.0001)
            return reversePath(first);
        if (second[0].subtract(connectingPoint).lengthSqr() < 0.0001)
            return second;
        if (second[second.length - 1].subtract(connectingPoint).lengthSqr() < 0.0001)
            return reversePath(second);

        return null;
    }

    public static BlockPos getNextBlockPos(BlockPos blockPos, Vec3 localEndPoint) {
        Vec3 globalEndPoint = blockPos.getBottomCenter().add(localEndPoint);
        Vec3 offset = localEndPoint.multiply(1, 0, 1);
        BlockPos firstCheck = BlockPos.containing(globalEndPoint.add(offset));
        BlockPos secondCheck = BlockPos.containing(globalEndPoint.subtract(offset));

        return (firstCheck.getX() == blockPos.getX() && firstCheck.getZ() == blockPos.getZ()) ?
                secondCheck : firstCheck;
    }

    public static @Nullable Pair<BlockPos, Vec3[]> getNextTrack(Level level, BlockPos blockPos, Vec3 localEndPoint, boolean backwards) {
        BlockPos nextBlock = getNextBlockPos(blockPos, localEndPoint);

        BlockState blockState = level.getBlockState(nextBlock);
        if (!AbstractTrackBlock.isTrack(blockState)) {
            nextBlock = nextBlock.below();
            blockState = level.getBlockState(nextBlock);

            if (!AbstractTrackBlock.isTrack(blockState)) return null;
        }

        Vec3 localStartPoint = blockPos.getBottomCenter().add(localEndPoint).subtract(nextBlock.getBottomCenter());
        Vec3[] connectingPath = getConnectingPath(blockState, localStartPoint);
        if (connectingPath == null) return null;

        return Pair.of(nextBlock, backwards ? reversePath(connectingPath) : connectingPath);
    }

    public static @Nullable Pair<BlockPos, Vec3[]> getNextTrack(Level level, Pair<BlockPos, Vec3[]> currentTrack, boolean backwards) {
        Vec3[] path = currentTrack.getSecond();
        Vec3 localEndPoint = backwards ? path[0] : path[path.length - 1];
        return getNextTrack(level, currentTrack.getFirst(), localEndPoint, backwards);
    }

    public static Pair<Vec3, Vec3> getPathExits(@NotNull Pair<BlockPos, Vec3[]> track, Vec3 position) {
        Vec3 center = track.getFirst().getBottomCenter();
        Vec3[] path = track.getSecond();
        int closestIndex = closestPathPoint(path, position.subtract(center)).getFirst();
        return Pair.of(path[closestIndex].add(center), path[closestIndex + 1].add(center));
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
