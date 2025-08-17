package dev.chililisoup.slotcars.block;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.MapCodec;
import dev.chililisoup.slotcars.reg.ModBlockTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public abstract class AbstractTrackBlock extends Block {
    public static final Pair<BlockPos, Path> EMPTY_TRACK = Pair.of(BlockPos.ZERO, Path.EMPTY);

    protected static final VoxelShape SHAPE_FLAT = Block.column(16.0, 0.0, 2.0);
    protected static final VoxelShape SHAPE_SLOPE = Block.column(16.0, 0.0, 8.0);
    protected static final VoxelShape SHAPE_SLOPE_TOP = Block.column(16.0, 8.0, 16.0);

    protected AbstractTrackBlock(Properties properties) {
        super(properties);
    }

    public static boolean isTrack(Level level, BlockPos blockPos) {
        return isTrack(level.getBlockState(blockPos));
    }

    public static boolean isTrack(BlockState blockState) {
        return blockState.is(ModBlockTags.TRACKS) && blockState.getBlock() instanceof AbstractTrackBlock;
    }

    @Override
    protected abstract @NotNull MapCodec<? extends AbstractTrackBlock> codec();

    public abstract Path[] getPaths(BlockState blockState);

    public static Path[] rotatePaths(Path[] paths, int clockwiseTurns) {
        Path[] rotated = new Path[paths.length];

        for (int i = 0; i < paths.length; i++) {
            Path path = paths[i];
            Vec3[] points = path.points;
            Vec3[] rotatedPoints = new Vec3[points.length];
            Vec3i entrance = path.entrance;
            Vec3i exit = path.exit;

            if (clockwiseTurns == 1) {
                entrance = new Vec3i(-entrance.getZ(), entrance.getY(), entrance.getX());
                exit = new Vec3i(-exit.getZ(), exit.getY(), exit.getX());
                for (int j = 0; j < points.length; j++)
                    rotatedPoints[j] = points[j].rotateClockwise90();
            } else if (clockwiseTurns == 2) {
                entrance = new Vec3i(-entrance.getX(), entrance.getY(), -entrance.getZ());
                exit = new Vec3i(-exit.getX(), exit.getY(), -exit.getZ());
                for (int j = 0; j < points.length; j++)
                    rotatedPoints[j] = points[j].multiply(-1, 1, -1);
            } else if (clockwiseTurns == 3) {
                entrance = new Vec3i(entrance.getZ(), entrance.getY(), -entrance.getX());
                exit = new Vec3i(exit.getZ(), exit.getY(), -exit.getX());
                for (int j = 0; j < points.length; j++)
                    rotatedPoints[j] = points[j].rotateClockwise90().multiply(-1, 1, -1);
            } else {
                return paths;
            }

            rotated[i] = new Path(rotatedPoints, entrance, exit);
        }

        return rotated;
    }

    public static Vec3 closestPointToPath(Vec3 pathStart, Vec3 pathEnd, Vec3 position) {
        Vec3 point = position.subtract(pathStart);
        Vec3 line = pathEnd.subtract(pathStart);
        return pathStart.add(point.projectedOn(line));
    }

    public static Vec3 closestPointToPath(Pair<BlockPos, Path> currentTrack, Vec3 position) {
        Pair<Vec3, Vec3> exits = AbstractTrackBlock.getPathExits(currentTrack, position);
        return closestPointToPath(exits.getFirst(), exits.getSecond(), position);
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

    public static Path getClosestPath(BlockState blockState, BlockPos blockPos, Vec3 position, Vec3 direction) {
        Path[] paths = ((AbstractTrackBlock) blockState.getBlock()).getPaths(blockState);
        Vec3 relativePos = position.subtract(blockPos.getBottomCenter());

        Pair<Integer, Double> closest = null;
        Path closestPath = paths[0];

        for (Path path : paths) {
            Pair<Integer, Double> pathDistance = path.closestPoint(relativePos);

            if (closest == null || pathDistance.getSecond() < closest.getSecond()) {
                closest = pathDistance;
                closestPath = path;
            }
        }

        int index = closest.getFirst();
        Vec3 pathVector = closestPath.points[index + 1].subtract(closestPath.points[index]).normalize();
        if (pathVector.dot(direction) < 0)
            return closestPath.reversed();
        return closestPath;
    }

    public static @Nullable Path getConnectingPath(BlockState blockState, Vec3 connectingPoint) {
        Path[] paths = ((AbstractTrackBlock) blockState.getBlock()).getPaths(blockState);

        for (Path path : paths) {
            if (path.points[0].subtract(connectingPoint).lengthSqr() < 0.0001)
                return path;
            if (path.points[path.points.length - 1].subtract(connectingPoint).lengthSqr() < 0.0001)
                return path.reversed();
        }

        return null;
    }

    public static @Nullable Pair<BlockPos, Path> getTrack(Level level, BlockPos blockPos, Vec3 localStartPoint, boolean backwards) {
        BlockPos nextBlock = blockPos;
        Vec3 nextStartPoint = localStartPoint;
        BlockState blockState = level.getBlockState(nextBlock);

        if (!AbstractTrackBlock.isTrack(blockState)) {
            nextBlock = nextBlock.below();
            nextStartPoint = nextStartPoint.add(0, 1, 0);
            blockState = level.getBlockState(nextBlock);

            if (!AbstractTrackBlock.isTrack(blockState)) return null;
        }

        Path connectingPath = getConnectingPath(blockState, nextStartPoint);
        if (connectingPath == null) return null;

        return Pair.of(nextBlock, backwards ? connectingPath.reversed() : connectingPath);
    }

    public static @Nullable Pair<BlockPos, Path> getNextTrack(Level level, Pair<BlockPos, Path> currentTrack, boolean backwards) {
        Path path = currentTrack.getSecond();
        Vec3 localEndPoint = backwards ? path.points[0] : path.points[path.points.length - 1];

        BlockPos nextBlock = currentTrack.getFirst().offset(path.getExit(localEndPoint));
        Vec3 localStartPoint = currentTrack.getFirst().getBottomCenter().add(localEndPoint).subtract(nextBlock.getBottomCenter());

        return getTrack(level, nextBlock, localStartPoint, backwards);
    }

    public static Pair<Vec3, Vec3> getPathExits(@NotNull Pair<BlockPos, Path> track, Vec3 position) {
        Vec3 center = track.getFirst().getBottomCenter();
        Path path = track.getSecond();
        int closestIndex = path.closestPoint(position.subtract(center)).getFirst();
        return Pair.of(path.points[closestIndex].add(center), path.points[closestIndex + 1].add(center));
    }
    
    public record Path(Vec3[] points, Vec3i entrance, Vec3i exit) {
        public static final Path EMPTY = new Path(new Vec3[0], Vec3i.ZERO, Vec3i.ZERO);

        public static Path reversed(Path path) {
            Vec3[] reversedPoints = new Vec3[path.points.length];
            for (int i = 0; i < path.points.length; i++)
                reversedPoints[i] = path.points[path.points.length - 1 - i];

            return new Path(reversedPoints, path.exit, path.entrance);
        }
        
        public Path reversed() {
            return reversed(this);
        }
        
        public Vec3i getExit(Vec3 localEndPoint) {
            double entranceDistance = this.points[0].distanceToSqr(localEndPoint);
            double exitDistance = this.points[this.points.length - 1].distanceToSqr(localEndPoint);
            return entranceDistance < exitDistance ? this.entrance : this.exit;
        }

        public Pair<Integer, Double> closestPoint(Vec3 position) {
            int closestIndex = -1;
            double closestDistance = Double.MAX_VALUE;

            for (int i = 0; i < this.points.length - 1; i++) {
                Vec3 middle = this.points[i].add(this.points[i + 1]).scale(0.5);
                double distance = middle.distanceToSqr(position);
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestIndex = i;
                }
            }

            return Pair.of(closestIndex, closestDistance);
        }

        @Override
        public @NotNull String toString() {
            return String.format("%s -> %s [%s]", this.entrance, this.exit, Arrays.toString(this.points));
        }
    }
}
