package dev.chililisoup.slotcars.block;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jetbrains.annotations.Nullable;

public class TrackState {
    private final Level level;
    private final BlockPos pos;
    private BlockState state;
    private final List<BlockPos> connections = Lists.newArrayList();

    public TrackState(Level level, BlockPos blockPos, BlockState blockState) {
        this.level = level;
        this.pos = blockPos;
        this.state = blockState;
        RailShape railShape = blockState.getValue(BlockStateProperties.RAIL_SHAPE);
        this.updateConnections(railShape);
    }

    private void updateConnections(RailShape railShape) {
        this.connections.clear();
        this.connections.addAll(TrackBlock.getConnectingBlocks(this.pos, railShape));
    }

    private void removeSoftConnections() {
        for (int i = 0; i < this.connections.size(); i++) {
            TrackState trackState = this.getTrack(this.connections.get(i));
            if (trackState != null && trackState.connectsTo(this))
                this.connections.set(i, trackState.pos);
            else
                this.connections.remove(i--);
        }
    }

    private boolean hasTrack(BlockPos blockPos) {
        return AbstractTrackBlock.isTrack(this.level, blockPos) ||
                AbstractTrackBlock.isTrack(this.level, blockPos.above()) ||
                AbstractTrackBlock.isTrack(this.level, blockPos.below());
    }

    private @Nullable TrackState getTrack(BlockPos blockPos) {
        BlockState blockState = this.level.getBlockState(blockPos);
        if (AbstractTrackBlock.isTrack(blockState))
            return new TrackState(this.level, blockPos, blockState);

        BlockPos checkPos = blockPos.above();
        blockState = this.level.getBlockState(checkPos);
        if (AbstractTrackBlock.isTrack(blockState))
            return new TrackState(this.level, checkPos, blockState);

        checkPos = blockPos.below();
        blockState = this.level.getBlockState(checkPos);
        return AbstractTrackBlock.isTrack(blockState) ? new TrackState(this.level, checkPos, blockState) : null;
    }

    private boolean connectsTo(TrackState trackState) {
        return this.hasConnection(trackState.pos);
    }

    private boolean hasConnection(BlockPos blockPos) {
        for (BlockPos connection : this.connections) {
            if (connection.getX() == blockPos.getX() && connection.getZ() == blockPos.getZ())
                return true;
        }
        return false;
    }

    protected int countPotentialConnections() {
        int i = 0;
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (this.hasTrack(this.pos.relative(direction))) i++;
        }
        return i;
    }

    private boolean canConnectTo(TrackState trackState) {
        return this.connectsTo(trackState) || this.connections.size() < 2;
    }

    private RailShape calculateShape(boolean powered, RailShape railShape) {
        BlockPos north = this.pos.north();
        BlockPos south = this.pos.south();
        BlockPos west = this.pos.west();
        BlockPos east = this.pos.east();
        boolean hasNorth = this.hasNeighborTrack(north);
        boolean hasSouth = this.hasNeighborTrack(south);
        boolean hasWest = this.hasNeighborTrack(west);
        boolean hasEast = this.hasNeighborTrack(east);
        RailShape newShape = null;
        boolean hasNorthOrSouth = hasNorth || hasSouth;
        boolean hasEastOrWest = hasWest || hasEast;

        if (hasNorthOrSouth && !hasEastOrWest) newShape = RailShape.NORTH_SOUTH;
        if (hasEastOrWest && !hasNorthOrSouth) newShape = RailShape.EAST_WEST;

        boolean hasSouthEast = hasSouth && hasEast;
        boolean hasSouthWest = hasSouth && hasWest;
        boolean hasNorthEast = hasNorth && hasEast;
        boolean hasNorthWest = hasNorth && hasWest;

        if (hasSouthEast && !hasNorth && !hasWest) newShape = RailShape.SOUTH_EAST;
        if (hasSouthWest && !hasNorth && !hasEast) newShape = RailShape.SOUTH_WEST;
        if (hasNorthWest && !hasSouth && !hasEast) newShape = RailShape.NORTH_WEST;
        if (hasNorthEast && !hasSouth && !hasWest) newShape = RailShape.NORTH_EAST;

        if (newShape == null) {
            if (hasNorthOrSouth) newShape = railShape;

            if (powered) {
                if (hasSouthEast) newShape = RailShape.SOUTH_EAST;
                if (hasSouthWest) newShape = RailShape.SOUTH_WEST;
                if (hasNorthEast) newShape = RailShape.NORTH_EAST;
                if (hasNorthWest) newShape = RailShape.NORTH_WEST;
            } else {
                if (hasNorthWest) newShape = RailShape.NORTH_WEST;
                if (hasNorthEast) newShape = RailShape.NORTH_EAST;
                if (hasSouthWest) newShape = RailShape.SOUTH_WEST;
                if (hasSouthEast) newShape = RailShape.SOUTH_EAST;
            }
        }

        if (newShape == RailShape.NORTH_SOUTH) {
            if (AbstractTrackBlock.isTrack(this.level, north.above()))
                newShape = RailShape.ASCENDING_NORTH;
            if (AbstractTrackBlock.isTrack(this.level, south.above()))
                newShape = RailShape.ASCENDING_SOUTH;
        }

        if (newShape == RailShape.EAST_WEST) {
            if (AbstractTrackBlock.isTrack(this.level, east.above()))
                newShape = RailShape.ASCENDING_EAST;
            if (AbstractTrackBlock.isTrack(this.level, west.above()))
                newShape = RailShape.ASCENDING_WEST;
        }

        return newShape == null ? railShape : newShape;
    }

    private void connectTo(TrackState trackState) {
        this.connections.add(trackState.pos);
        this.state = this.state.setValue(
                BlockStateProperties.RAIL_SHAPE,
                calculateShape(false, this.state.getValue(BlockStateProperties.RAIL_SHAPE))
        );
        this.level.setBlock(this.pos, this.state, 3);
    }

    private boolean hasNeighborTrack(BlockPos blockPos) {
        TrackState trackState = this.getTrack(blockPos);
        if (trackState == null) {
            return false;
        } else {
            trackState.removeSoftConnections();
            return trackState.canConnectTo(this);
        }
    }

    public TrackState place(boolean powered, boolean updateLevel, RailShape railShape) {
        RailShape newShape = calculateShape(powered, railShape);

        this.updateConnections(newShape);
        this.state = this.state.setValue(BlockStateProperties.RAIL_SHAPE, newShape);
        if (updateLevel || this.level.getBlockState(this.pos) != this.state) {
            this.level.setBlock(this.pos, this.state, 3);

            for (BlockPos connection : this.connections) {
                TrackState trackState = this.getTrack(connection);
                if (trackState != null) {
                    trackState.removeSoftConnections();
                    if (trackState.canConnectTo(this))
                        trackState.connectTo(this);
                }
            }
        }

        return this;
    }

    public BlockState getState() {
        return this.state;
    }
}
