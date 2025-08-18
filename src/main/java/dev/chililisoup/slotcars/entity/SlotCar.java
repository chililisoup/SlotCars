package dev.chililisoup.slotcars.entity;

import com.mojang.datafixers.util.Pair;
import dev.chililisoup.slotcars.SlotCars;
import dev.chililisoup.slotcars.block.AbstractTrackBlock;
import dev.chililisoup.slotcars.network.ServerboundMoveSlotCarPacket;
import dev.chililisoup.slotcars.reg.ModBlockTags;
import dev.chililisoup.slotcars.reg.ModEntityTypes;
import dev.chililisoup.slotcars.reg.ModItems;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public class SlotCar extends Entity implements TraceableEntity {
    public static final AttachmentType<SlotCarHolder> ACTIVE_SLOT_CAR = AttachmentRegistry.create(
            SlotCars.loc("active_slot_car"),
            builder -> builder.initializer(SlotCarHolder::new).copyOnDeath()
    );
    private static final EntityDataAccessor<Integer> DATA_COLOR = SynchedEntityData.defineId(SlotCar.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_X_ROT_OVERRIDE = SynchedEntityData.defineId(SlotCar.class, EntityDataSerializers.FLOAT);
    private static final int DEFAULT_COLOR = -11430696;
    private static final double ACCELERATION = 0.9375;
    private static final double LN_ACCELERATION = Math.log(ACCELERATION);
    private static final double MAX_SPEED = 0.8;
    private static final int MAX_TICK_ITERATIONS = 16;

    private final InterpolationHandler interpolationHandler = new InterpolationHandler(this);
    protected @Nullable EntityReference<Player> owner;
    private @Nullable Pair<BlockPos, AbstractTrackBlock.Path> currentTrack;
    private @Nullable Pair<BlockPos, AbstractTrackBlock.Path> respawnTrack;
    private @Nullable Vec3 respawnPoint;
    private int respawnTimer = -5;
    private boolean backwards = false;
    private boolean derailed = false;

    public SlotCar(EntityType<? extends SlotCar> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public @NotNull InterpolationHandler getInterpolation() {
        return this.interpolationHandler;
    }

    public void setInitialPos(Vec3 pos) {
        this.setPos(pos);
        this.xo = pos.x;
        this.yo = pos.y;
        this.zo = pos.z;
    }

    @Nullable
    public static SlotCar createSlotCar(
            Level level,
            Vec3 position,
            Vec3 rotation,
            EntitySpawnReason entitySpawnReason,
            ItemStack itemStack,
            @Nullable Player player
    ) {
        SlotCar car = ModEntityTypes.SLOT_CAR.create(level, entitySpawnReason);
        if (car != null) {
            car.setOwner(player);
            car.setInitialPos(position);
            car.setLookVector(rotation);
            car.setColor(itemStack.get(DataComponents.DYED_COLOR));
            EntityType.createDefaultStackConfig(level, itemStack, player).accept(car);
        }

        return car;
    }

    @Override
    public void remove(Entity.RemovalReason removalReason) {
        this.updateOwnerInfo(null);
        super.remove(removalReason);
    }

    @Override
    public void onClientRemoval() {
        this.updateOwnerInfo(null);
    }

    protected void setOwner(@Nullable EntityReference<Player> entityReference) {
        this.owner = entityReference;
    }

    public void setOwner(@Nullable Player player) {
        this.setOwner(player != null ? new EntityReference<>(player) : null);
        this.updateOwnerInfo(this);
    }

    @Override
    public @Nullable Player getOwner() {
        return EntityReference.get(this.owner, this.level(), Player.class);
    }

    private void updateOwnerInfo(@Nullable SlotCar car) {
        Player player = this.getOwner();
        if (player != null) {
            player.setAttached(ACTIVE_SLOT_CAR, new SlotCarHolder(car));
        }
    }

    protected boolean isLocalClientAuthoritative() {
        Player player = this.getOwner();
        return player != null && player.isLocalPlayer();
    }

    public boolean isClientAuthoritative() {
        Player player = this.getOwner();
        return player != null && player.isClientAuthoritative();
    }

    private void setColor(@Nullable DyedItemColor color) {
        if (color == null) return;
        this.getEntityData().set(DATA_COLOR, color.rgb());
    }

    public int getColor() {
        return this.getEntityData().get(DATA_COLOR);
    }

    @Override
    public float getXRot() {
        float override = this.getEntityData().get(DATA_X_ROT_OVERRIDE);
        return override == 0F ? super.getXRot() : override;
    }

    public void setXRotOverride(float override) {
        this.getEntityData().set(DATA_X_ROT_OVERRIDE, override);
    }

    public boolean isDerailed() {
        return this.derailed;
    }

    public boolean isCrashing() {
        return this.derailed || this.respawnTimer > -5;
    }

    private void derail() {
        this.derailed = true;
        this.respawnPoint = this.position();
        this.setCurrentTrack(null);
    }

    public void setDerailed(boolean derailed) {
        this.derailed = derailed;
    }

    private void respawn() {
        this.derailed = false;
        this.backwards = false;

        if (this.respawnPoint != null)
            this.setPos(AbstractTrackBlock.closestPointToPath(this.respawnTrack, this.respawnPoint));

        this.reapplyPosition();
        this.setCurrentTrack(this.respawnTrack);
        this.setDeltaMovement(Vec3.ZERO);
    }

    public int getRespawnTimer() {
        return this.respawnTimer;
    }

    public static int respawnLength() {
        return 30;
    }

    private void setCurrentTrack(@Nullable Pair<BlockPos, AbstractTrackBlock.Path> track) {
        this.currentTrack = track;
        if (track != null) this.respawnTrack = track;
    }
    
    public boolean onTrack() {
        return this.currentTrack != null;
    }

    public void setOnTrack(boolean onTrack) {
        this.currentTrack = onTrack ? AbstractTrackBlock.EMPTY_TRACK : null;
    }

    @Override
    public void tick() {
        Player player = this.getOwner();
        if (player == null) {
            this.discard();
            return;
        }

        if (this.shouldDespawn(player))
            return;

        this.getInterpolation().interpolate();

        if (this.getOwner().isLocalPlayer()) {
            if (!this.derailed) {
                if (this.currentTrack == null) {
                    BlockPos blockPos = this.getCurrentBlockPos();
                    BlockState blockState = this.level().getBlockState(blockPos);

                    if (AbstractTrackBlock.isTrack(blockState)) this.setCurrentTrack(Pair.of(
                            blockPos,
                            AbstractTrackBlock.getClosestPath(
                                    blockState,
                                    blockPos,
                                    this.position(),
                                    (this.firstTick ?
                                            this.getLookAngle() :
                                            (this.backwards ? this.getDeltaMovement().reverse() : this.getDeltaMovement())
                                    ).normalize()
                            )
                    ));

                    if (!this.firstTick && this.currentTrack != null) {
                        Pair<Vec3, Vec3> exits = AbstractTrackBlock.getPathExits(this.currentTrack, this.position());
                        if (AbstractTrackBlock.distanceToPathSqr(exits.getFirst(), exits.getSecond(), this.position()) > 0.125) {
                            this.setCurrentTrack(null);
                        }
                    }
                } else if (!AbstractTrackBlock.isTrack(this.level().getBlockState(this.currentTrack.getFirst()))) {
                    this.setCurrentTrack(null);
                }
            } else {
                if (this.respawnTimer > 0) this.respawnTimer--;
            }

            if (this.respawnTimer <= 0 && this.respawnTimer > -5) {
                respawn();
                this.respawnTimer--;
            } else {
                if (this.onTrack()) {
                    this.noPhysics = true;
                    this.moveAlongTrack();
                    this.noPhysics = false;
                } else {
                    this.comeOffTrack();
                    this.applyGravity();
                }
            }

            ClientPlayNetworking.send(ServerboundMoveSlotCarPacket.fromCar(this));
        }

        this.applyEffectsFromBlocks();
        this.firstTick = false;
    }

    public void moveAlongTrack() {
        Player player = this.getOwner();
        if (player == null) return;

        if (this.currentTrack == null) return;

        this.resetFallDistance();
        this.applyGravity();

        Vec3 center = this.currentTrack.getFirst().getBottomCenter();
        Vec3[] path = this.currentTrack.getSecond().points();
        int exitsIndex = AbstractTrackBlock.closestOrderedPathPoint(path, this.position().subtract(center)).getFirst();

        if (exitsIndex >= path.length - 1) {
            Pair<BlockPos, AbstractTrackBlock.Path> nextTrack = this.nextTrack();
            if (nextTrack != null) {
                this.setCurrentTrack(nextTrack);
                center = this.currentTrack.getFirst().getBottomCenter();
                path = this.currentTrack.getSecond().points();
                exitsIndex = 0;
            } else {
                exitsIndex--;
            }
        }

        Pair<Vec3, Vec3> exits = Pair.of(path[exitsIndex].add(center), path[exitsIndex + 1].add(center));
        Vec3 pathVector = exits.getSecond().subtract(exits.getFirst()).normalize();
        Vec3 deltaMovement = this.getDeltaMovement();

        double speed = deltaMovement.length();
        double maintainedSpeed = 1;
        Vec3 currentPos = this.position();
        double elapsedTick = 0;
        int tickIterations = 0;

        boolean powered = player.isUsingItem();
        while (elapsedTick < 1 && tickIterations++ < MAX_TICK_ITERATIONS) {
            double partialTick = 1.0 - elapsedTick;

            double distanceToExit = currentPos.distanceTo(exits.getSecond());
            if (distanceToExit < 0.001) {
                if (exitsIndex >= path.length - 2) {
                    Pair<BlockPos, AbstractTrackBlock.Path> nextTrack = this.nextTrack();
                    if (nextTrack != null) {
                        this.setCurrentTrack(nextTrack);
                        center = this.currentTrack.getFirst().getBottomCenter();
                        path = this.currentTrack.getSecond().points();
                        exitsIndex = 0;
                    } else {
                        break;
                    }
                } else {
                    exitsIndex++;
                }

                exits = Pair.of(path[exitsIndex].add(center), path[exitsIndex + 1].add(center));
                pathVector = exits.getSecond().subtract(exits.getFirst()).normalize();
                distanceToExit = currentPos.distanceTo(exits.getSecond());
            }

            maintainedSpeed = deltaMovement.normalize().dot(pathVector);
            if (powered) {
                Block block = this.level().getBlockState(this.currentTrack.getFirst()).getBlock();
                if (block instanceof AbstractTrackBlock trackBlock && trackBlock.canDerail()) {
                    Vec3 xzMotion = deltaMovement.multiply(1, 0, 1);
                    double xzLength = xzMotion.length();

                    if (xzLength > 0) {
                        double harshness = (1 - Math.abs(xzMotion.normalize()
                                .dot(pathVector.multiply(1, 0, 1).normalize())
                        )) * xzLength;

                        if (harshness > this.maxHarshness()) {
                            this.derail();
                            deltaMovement = deltaMovement.scale(0.75).add(0, 0.15, 0);
                            currentPos = currentPos.add(deltaMovement);
                            elapsedTick = 1;
                            break;
                        }
                    }
                }

                maintainedSpeed = Math.max(getMinPoweredMaintainedSpeed(), maintainedSpeed);
                double accelerationTicks = Math.min(distanceToExit / (speed * maintainedSpeed), partialTick);
                speed = accelerate(speed * maintainedSpeed, accelerationTicks);
                elapsedTick += accelerationTicks;
                currentPos = currentPos.add(pathVector.scale(speed * accelerationTicks));
            } else {
                speed *= 0.96 * maintainedSpeed;
                currentPos = currentPos.add(pathVector.scale(speed * partialTick));
                elapsedTick = 1;
            }

            deltaMovement = pathVector.scale(speed);
            this.backwards = maintainedSpeed < 0;
        }

        if (elapsedTick < 1) {
            currentPos = currentPos.add(pathVector.scale(speed * maintainedSpeed * (1 - elapsedTick)));
        }

        if (this.onTrack()) {
            BlockPos endBlock = BlockPos.containing(currentPos);
            BlockPos currentBlock = this.currentTrack.getFirst();
            if (endBlock.getX() != currentBlock.getX() || endBlock.getZ() != currentBlock.getZ()) {
                Pair<BlockPos, AbstractTrackBlock.Path> nextTrack = this.nextTrack();

                if (nextTrack != null) {
                    BlockPos nextBlock = nextTrack.getFirst();
                    if (endBlock.getX() != nextBlock.getX() || endBlock.getZ() != nextBlock.getZ())
                        this.derail();
                    else this.setCurrentTrack(nextTrack);
                } else this.setCurrentTrack(null);
            }

            if (this.onTrack()) {
                exits = AbstractTrackBlock.getPathExits(this.currentTrack, currentPos);
                pathVector = exits.getSecond().subtract(exits.getFirst()).normalize();
            }
        }

        Vec3 totalDeltaMovement = !this.onTrack() ?
                currentPos.subtract(this.position()) :
                AbstractTrackBlock.closestPointToPath(
                        exits.getFirst(),
                        exits.getSecond(),
                        currentPos
                ).subtract(this.position());

        this.setLookVector(pathVector.lerp(this.getLookAngle().normalize(), 0.375));
        if (this.currentTrack != null) {
            Block block = this.level().getBlockState(this.currentTrack.getFirst()).getBlock();
            if (block instanceof AbstractTrackBlock trackBlock && trackBlock.isLoop()) {
                this.setLookVector(new Vec3(this.currentTrack.getSecond().exit()));
                float loopProgress = (float) exitsIndex / ((float) this.currentTrack.getSecond().points().length - 2);
                this.setXRotOverride(loopProgress * -360F);
            } else this.setXRotOverride(0F);
        } else this.noPhysics = false;
        this.move(MoverType.SELF, totalDeltaMovement);
        this.setDeltaMovement(deltaMovement);
    }

    public void setLookVector(Vec3 vector) {
        double x = vector.x;
        double y = vector.y;
        double z = vector.z;
        double horizontalLength = Math.sqrt(x * x + z * z);
        this.setXRot(Mth.wrapDegrees((float)(-(Mth.atan2(y, horizontalLength) * 180.0F / (float)Math.PI))));
        this.setYRot(Mth.wrapDegrees((float)(Mth.atan2(z, x) * 180.0F / (float)Math.PI) - 90.0F));
    }

    public @Nullable Pair<BlockPos, AbstractTrackBlock.Path> nextTrack() {
        if (this.currentTrack == null) return null;
        return AbstractTrackBlock.getNextTrack(this.level(), this.currentTrack, this.backwards);
    }

    protected void comeOffTrack() {
        Vec3 deltaMovement = this.getDeltaMovement();
        this.setDeltaMovement(
                Mth.clamp(deltaMovement.x, -MAX_SPEED, MAX_SPEED),
                deltaMovement.y, Mth.clamp(deltaMovement.z, -MAX_SPEED, MAX_SPEED)
        );

        if (this.onGround())
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5));

        this.move(MoverType.SELF, this.getDeltaMovement());

        if (this.onGround()) {
            this.setXRot(0);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.95));
            this.setXRot(Mth.lerp(0.1F, this.getXRot(), 45));
        }

        if ((this.onGround() || this.horizontalCollision) && this.derailed && this.respawnTimer < 0)
            this.respawnTimer = respawnLength();
    }

    public double maxHarshness() {
        return 0.2;
    }

    public static double accelerate(double speed, double partialTick) {
        if (speed >= MAX_SPEED * 0.99) return Math.max(speed, MAX_SPEED);

        double curvePoint = accelerateInverse(speed);
        return MAX_SPEED * (1.0 - (Math.pow(ACCELERATION, curvePoint + partialTick)));
    }

    public static double accelerateInverse(double speed) {
        return Math.log(1.0 - (speed / MAX_SPEED)) / LN_ACCELERATION;
    }

    public static double getMinPoweredMaintainedSpeed() {
        return 0.15;
    }

    public BlockPos getCurrentBlockPos() {
        int x = Mth.floor(this.getX());
        int y = Mth.floor(this.getY());
        int z = Mth.floor(this.getZ());
        return new BlockPos(x, y, z);
    }

    private boolean shouldDespawn(Player player) {
        if (this.level().isClientSide) return false;

        boolean inMainHand = player.getMainHandItem().is(ModItems.CONTROLLER);
        boolean inOffhand = player.getOffhandItem().is(ModItems.CONTROLLER);
        if (!player.isRemoved() && player.isAlive() && (inMainHand || inOffhand) && this.distanceToSqr(player) < 4096)
            return false;

        this.discard();
        return true;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return d < 4096;
    }

    @Override
    public void handleEntityEvent(byte b) {
        if (b == 40) this.explode();
        else super.handleEntityEvent(b);
    }

    private void explode() {
        ParticleOptions particleOptions = ParticleTypes.EXPLOSION;

        this.level().addParticle(
                particleOptions,
                this.getRandomX(1.0),
                this.getRandomY() + 0.5,
                this.getRandomZ(1.0),
                this.random.nextGaussian() * 0.02,
                this.random.nextGaussian() * 0.02,
                this.random.nextGaussian() * 0.02
        );
    }

    @Override
    public void restoreFrom(Entity entity) {
        super.restoreFrom(entity);
        if (entity instanceof SlotCar car) {
            this.owner = car.owner;
        }
    }

    @Override
    public @NotNull Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        Entity entity = this.getOwner();
        return new ClientboundAddEntityPacket(this, serverEntity, entity == null ? 0 : entity.getId());
    }

    @Override
    public void recreateFromPacket(ClientboundAddEntityPacket packet) {
        super.recreateFromPacket(packet);
        Vec3 vec3 = this.getDeltaMovement();
        this.lerpMotion(vec3.x, vec3.y, vec3.z);

        Entity entity = this.level().getEntity(packet.getData());
        if (entity instanceof Player player) {
            this.setOwner(player);
        } else {
            int i = packet.getData();
            SlotCars.LOGGER.error("Failed to recreate slot car on client. {} (id: {}) is not a valid owner.", this.level().getEntity(i), i);
            this.discard();
        }
    }

    @Override
    protected double getDefaultGravity() {
        return this.isInWater() ? 0.005 : 0.04;
    }

    @Override
    protected float getBlockSpeedFactor() {
        BlockState blockState = this.level().getBlockState(this.blockPosition());
        return blockState.is(ModBlockTags.TRACKS) ? 1.0F : super.getBlockSpeedFactor();
    }

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource damageSource, float f) {
        return false;
    }

    @Override
    protected Entity.@NotNull MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, DEFAULT_COLOR);
        builder.define(DATA_X_ROT_OVERRIDE, 0F);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput valueInput) {
        this.setOwner(EntityReference.read(valueInput, "Owner"));
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput valueOutput) {
        EntityReference.store(this.owner, valueOutput, "Owner");
    }

    public static class SlotCarHolder {
        private final SlotCar car;

        public SlotCarHolder(@Nullable SlotCar car) {
            this.car = car;
        }

        public SlotCarHolder() {
            this(null);
        }

        public @Nullable SlotCar get() {
            return this.car;
        }
    }
}
