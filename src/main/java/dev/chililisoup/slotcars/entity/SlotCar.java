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
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
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

    private final InterpolationHandler interpolationHandler = new InterpolationHandler(this);
    protected @Nullable EntityReference<Player> owner;
    private @Nullable Pair<BlockPos, Vec3[]> currentTrack;
    private @Nullable Pair<BlockPos, Vec3[]> respawnTrack;
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
            Vec3 pos,
            float yRot,
            EntitySpawnReason entitySpawnReason,
            ItemStack itemStack,
            @Nullable Player player
    ) {
        SlotCar car = ModEntityTypes.SLOT_CAR.create(level, entitySpawnReason);
        if (car != null) {
            car.setOwner(player);
            car.setInitialPos(pos);
            car.setYRot(yRot);
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

    public boolean isDerailed() {
        return this.derailed;
    }

    private void derail() {
        this.derailed = true;
        this.respawnPoint = this.position();
        this.setCurrentTrack(null);
    }

    private void respawn() {
        this.derailed = false;
        this.backwards = false;

        if (this.respawnPoint != null)
            this.setPos(this.respawnPoint);

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

    private void setCurrentTrack(@Nullable Pair<BlockPos, Vec3[]> track) {
        this.currentTrack = track;
        if (track != null) this.respawnTrack = track;
    }

    @Override
    public void tick() {
        Player player = this.getOwner();
        if (player == null) {
            this.discard();
            return;
        }

        if (!this.level().isClientSide && this.shouldDespawn(player))
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
                if (this.currentTrack != null) {
                    this.moveAlongTrack();
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
        Vec3[] path = this.currentTrack.getSecond();
        int exitsIndex = AbstractTrackBlock.closestOrderedPathPoint(path, this.position().subtract(center)).getFirst();

        if (exitsIndex >= path.length - 1) {
            Pair<BlockPos, Vec3[]> nextTrack = this.nextTrack();
            if (nextTrack != null) {
                this.setCurrentTrack(nextTrack);
                center = this.currentTrack.getFirst().getBottomCenter();
                path = this.currentTrack.getSecond();
                exitsIndex = 0;
            } else {
                exitsIndex--;
            }
        }

        Pair<Vec3, Vec3> exits = Pair.of(path[exitsIndex].add(center), path[exitsIndex + 1].add(center));
        Vec3 pathVector = exits.getSecond().subtract(exits.getFirst()).normalize();
        Vec3 deltaMovement = this.getDeltaMovement();

        double maintainedSpeed = deltaMovement.normalize().dot(pathVector);
        double speed = deltaMovement.length();

        boolean powered = player.isUsingItem();
        if (powered) {
            speed = accelerate(speed);
            maintainedSpeed = Math.max(this.getMinPoweredSpeed(), maintainedSpeed);

            Vec3 xzMotion = deltaMovement.multiply(1, 0, 1);

            if (xzMotion.lengthSqr() > 0.1) {
                double harshness = (1 - Math.abs(xzMotion.normalize()
                        .dot(pathVector.multiply(1, 0, 1).normalize())
                )) * speed;

                if (harshness > this.maxHarshness()) {
                    this.derail();
                    deltaMovement = deltaMovement.scale(0.75).add(0, 0.15, 0);
                }
            }
        } else {
            speed *= this.naturalSlowdown();
        }

        if (!this.derailed) {
            deltaMovement = pathVector.scale(speed * maintainedSpeed);
            this.backwards = maintainedSpeed < 0;
        }

        Vec3 endPos = this.position().add(deltaMovement);
        if (this.currentTrack != null) {
            BlockPos endBlock = BlockPos.containing(endPos);
            BlockPos currentBlock = this.currentTrack.getFirst();
            if (endBlock.getX() != currentBlock.getX() || endBlock.getZ() != currentBlock.getZ()) {
                this.setCurrentTrack(this.nextTrack());

                if (this.currentTrack != null) {
                    currentBlock = this.currentTrack.getFirst();
                    if (endBlock.getX() != currentBlock.getX() || endBlock.getZ() != currentBlock.getZ())
                        this.setCurrentTrack(null);
                }
            }

            if (this.currentTrack != null) {
                exits = AbstractTrackBlock.getPathExits(this.currentTrack, endPos);
                pathVector = exits.getSecond().subtract(exits.getFirst()).normalize();
            }
        }

        Vec3 totalDeltaMovement = this.currentTrack == null ?
                endPos.subtract(this.position()) :
                AbstractTrackBlock.closestPointToPath(
                        exits.getFirst(),
                        exits.getSecond(),
                        endPos
                ).subtract(this.position());

        this.setLookVector(pathVector.lerp(this.getLookAngle().normalize(), 0.375));
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

    public @Nullable Pair<BlockPos, Vec3[]> nextTrack() {
        if (this.currentTrack == null) return null;
        return AbstractTrackBlock.getNextTrack(this.level(), this.currentTrack, this.backwards);
    }

    protected void comeOffTrack() {
        double maxSpeed = this.getMaxSpeed();
        Vec3 deltaMovement = this.getDeltaMovement();
        this.setDeltaMovement(
                Mth.clamp(deltaMovement.x, -maxSpeed, maxSpeed),
                deltaMovement.y, Mth.clamp(deltaMovement.z, -maxSpeed, maxSpeed)
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
        return 0.175;
    }

    public double accelerate(double speed) {
        return Math.max(Math.min(speed * 1.075, this.getMaxSpeed()), this.getMinPoweredSpeed());
    }

    public double getMinPoweredSpeed() {
        return this.isInWater() ? 0.075 : 0.15;
    }

    public double getMaxSpeed() {
        return this.isInWater() ? 0.3 : 0.6;
    }

    protected double naturalSlowdown() {
        return this.isInWater() ? 0.95 : 0.96;
    }

    public BlockPos getCurrentBlockPos() {
        int x = Mth.floor(this.getX());
        int y = Mth.floor(this.getY());
        int z = Mth.floor(this.getZ());
        return new BlockPos(x, y, z);
    }

    private boolean shouldDespawn(Player player) {
        boolean inMainHand = player.getMainHandItem().is(ModItems.CONTROLLER);
        boolean inOffhand = player.getOffhandItem().is(ModItems.CONTROLLER);
        if (!player.isRemoved() && player.isAlive() && (inMainHand || inOffhand) && this.distanceToSqr(player) < 1024.0)
            return false;

        this.discard();
        return true;
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
        return this.isInWater() ? 0.01 : 0.06;
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
