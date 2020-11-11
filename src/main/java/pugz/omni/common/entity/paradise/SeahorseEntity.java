package pugz.omni.common.entity.paradise;

import net.minecraft.block.AbstractCoralPlantBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.CoralFanBlock;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.RandomSwimmingGoal;
import net.minecraft.entity.passive.OcelotEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import pugz.omni.core.registry.OmniEntities;
import pugz.omni.core.registry.OmniItems;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SeahorseEntity extends WaterMobEntity {
    private static final DataParameter<Integer> CORAL_TYPE = EntityDataManager.createKey(SeahorseEntity.class, DataSerializers.VARINT);
    private static final DataParameter<Byte> CORAL_TYPE_FLAGS = EntityDataManager.createKey(SeahorseEntity.class, DataSerializers.BYTE);
    private int remainingCooldownBeforeLocatingNewCoral = 0;
    private BlockPos savedCoralPos = null;

    public SeahorseEntity(EntityType<? extends SeahorseEntity> type, World worldIn) {
        super(type, worldIn);
        this.moveController = new SeahorseEntity.MoveHelperController(this);
    }

    public SeahorseEntity(World world, double posX, double posY, double posZ) {
        this(OmniEntities.SEAHORSE.get(), world);
        this.setPosition(posX, posY, posZ);
    }

    public static AttributeModifierMap.MutableAttribute registerAttributes() {
        return MobEntity.func_233666_p_().createMutableAttribute(Attributes.MAX_HEALTH, 8.0D).createMutableAttribute(Attributes.MOVEMENT_SPEED, 0.8D);
    }

    protected void registerData() {
        super.registerData();
        this.dataManager.register(CORAL_TYPE, 0);
        this.dataManager.register(CORAL_TYPE_FLAGS, (byte)0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 25));
        this.goalSelector.addGoal(4, new GrowCoralGoal());
        this.goalSelector.addGoal(6, new FindCoralGoal());
        this.goalSelector.addGoal(4, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, PlayerEntity.class, 6.0F, 1.0D, 1.0D));
    }

    @Nonnull
    @Override
    protected PathNavigator createNavigator(World worldIn) {
        return new SwimmerPathNavigator(this, worldIn);
    }

    private boolean isCoral(BlockPos pos) {
        return this.world.isBlockPresent(pos) && this.world.getBlockState(pos).getBlock().isIn(BlockTags.CORAL_BLOCKS);
    }

    @Nonnull
    @Override
    protected SoundEvent getSwimSound() {
        return SoundEvents.ENTITY_FISH_SWIM;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP;
    }

    @Override
    public void onEnterBubbleColumn(boolean downwards) {}

    @Override
    public void onEnterBubbleColumnWithAirAbove(boolean downwards) {}

    @Override
    public ItemStack getPickedResult(RayTraceResult target) {
        return new ItemStack(OmniItems.SEAHORSE_SPAWN_EGG.get());
    }

    @Override
    public int getMaxSpawnedInChunk() {
        return 6;
    }

    @Nullable
    public BlockPos getCoralPos() {
        return this.savedCoralPos;
    }

    public SeahorseEntity.CoralType getVariantType() {
        return SeahorseEntity.CoralType.getTypeByIndex(this.dataManager.get(CORAL_TYPE));
    }

    private void setVariantType(SeahorseEntity.CoralType typeIn) {
        this.dataManager.set(CORAL_TYPE, typeIn.getIndex());
    }

    public void writeAdditional(CompoundNBT compound) {
        super.writeAdditional(compound);
        if (this.hasCoral()) {
            compound.put("CoralPos", NBTUtil.writeBlockPos(this.getCoralPos()));
        }
        compound.putString("Type", this.getVariantType().getName());
    }

    public void readAdditional(CompoundNBT compound) {
        this.setVariantType(SeahorseEntity.CoralType.getTypeByName(compound.getString("Type")));
        this.savedCoralPos = null;
        if (compound.contains("CoralPos")) {
            this.savedCoralPos = NBTUtil.readBlockPos(compound.getCompound("CoralPos"));
        }
        super.readAdditional(compound);
    }

    private boolean isTooFar(BlockPos pos) {
        return this.isWithinDistance(pos, 32);
    }

    private boolean isWithinDistance(BlockPos pos, int distance) {
        return !pos.withinDistance(this.getPosition(), (double) distance);
    }

    private void startMovingTo(BlockPos pos) {
        Vector3d vector3d = Vector3d.copyCenteredHorizontally(pos);
        int i = 0;
        BlockPos blockpos = this.getPosition();
        int j = (int)vector3d.y - blockpos.getY();
        if (j > 2) {
            i = 4;
        } else if (j < -2) {
            i = -4;
        }

        int k = 6;
        int l = 8;
        int i1 = blockpos.manhattanDistance(pos);
        if (i1 < 15) {
            k = i1 / 2;
            l = i1 / 2;
        }

        Vector3d vector3d1 = RandomPositionGenerator.func_226344_b_(this, k, l, i, vector3d, (double)((float)Math.PI / 10F));
        if (vector3d1 != null) {
            this.navigator.setRangeMultiplier(0.5F);
            this.navigator.tryMoveToXYZ(vector3d1.x, vector3d1.y, vector3d1.z, 1.0D);
        }
    }

    public boolean hasCoral() {
        return this.savedCoralPos != null;
    }

    static class MoveHelperController extends MovementController {
        private final SeahorseEntity seahorse;

        public MoveHelperController(SeahorseEntity seahorse) {
            super(seahorse);
            this.seahorse = seahorse;
        }

        public void tick() {
            if (this.seahorse.isInWater()) {
                if (this.action != MovementController.Action.MOVE_TO || this.seahorse.getNavigator().noPath()) {
                    this.seahorse.setAIMoveSpeed(0.0F);
                    return;
                }
                double d0 = this.posX - this.seahorse.getPosX();
                double d1 = this.posY - this.seahorse.getPosY();
                double d2 = this.posZ - this.seahorse.getPosZ();
                double d3 = (double) MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                d1 = d1 / d3;
                float f = (float)(MathHelper.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                this.seahorse.rotationYaw = this.limitAngle(this.seahorse.rotationYaw, f, 90.0F);
                this.seahorse.renderYawOffset = this.seahorse.rotationYaw;
                float f1 = (float)(this.speed * this.seahorse.getAttributeValue(Attributes.MOVEMENT_SPEED));
                float f2 = MathHelper.lerp(0.125F, this.seahorse.getAIMoveSpeed(), f1);
                this.seahorse.setAIMoveSpeed(f2);
                this.seahorse.setMotion(this.seahorse.getMotion().add((double)f2 * d0 * 0.005D, (double)f2 * d1 * 0.1D, (double)f2 * d2 * 0.005D));
            } else {
                if (!this.seahorse.onGround) {
                    this.seahorse.setMotion(this.seahorse.getMotion().add(0.0D, -0.008D, 0.0D));
                }
                super.tick();
            }
        }
    }

    public class FindCoralGoal extends Goal {
        private int ticks = SeahorseEntity.this.world.rand.nextInt(10);

        FindCoralGoal() {
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public boolean canStart() {
            return SeahorseEntity.this.savedCoralPos != null && SeahorseEntity.this.isCoral(SeahorseEntity.this.savedCoralPos) && SeahorseEntity.this.isWithinDistance(SeahorseEntity.this.savedCoralPos, 2);
        }

        @Override
        public boolean shouldExecute() {
            return this.canStart();
        }

        @Override
        public boolean shouldContinueExecuting() {
            return this.canStart();
        }

        public void startExecuting() {
            this.ticks = 0;
            super.startExecuting();
        }

        public void resetTask() {
            this.ticks = 0;
            SeahorseEntity.this.navigator.clearPath();
            SeahorseEntity.this.navigator.resetRangeMultiplier();
        }

        public void tick() {
            if (SeahorseEntity.this.savedCoralPos != null) {
                ++this.ticks;
                if (this.ticks > 600) {
                    SeahorseEntity.this.savedCoralPos = null;
                } else if (!SeahorseEntity.this.navigator.hasPath()) {
                    if (SeahorseEntity.this.isTooFar(SeahorseEntity.this.savedCoralPos)) {
                        SeahorseEntity.this.savedCoralPos = null;
                    } else {
                        System.out.println(SeahorseEntity.this.savedCoralPos.getX() + " " + SeahorseEntity.this.savedCoralPos.getY() + " " + SeahorseEntity.this.savedCoralPos.getZ());
                        SeahorseEntity.this.startMovingTo(SeahorseEntity.this.savedCoralPos);
                    }
                }
            }
        }
    }

    class GrowCoralGoal extends Goal {
        private final Predicate<BlockState> coralPredicate = (state) -> {
            return state.isIn(BlockTags.CORAL_BLOCKS);
        };
        private int growTicks = 0;
        private int lastPollinationTick = 0;
        private boolean running;
        private Vector3d nextTarget;
        private int ticks = 0;

        GrowCoralGoal() {
            this.setMutexFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public boolean shouldExecute() {
            if (SeahorseEntity.this.remainingCooldownBeforeLocatingNewCoral > 0) {
                return false;
            } else if (SeahorseEntity.this.world.isRaining()) {
                return false;
            } else if (SeahorseEntity.this.rand.nextFloat() < 0.7F) {
                return false;
            } else {
                Optional<BlockPos> optional = this.getCoral();
                if (optional.isPresent()) {
                    SeahorseEntity.this.savedCoralPos = optional.get();
                    SeahorseEntity.this.navigator.tryMoveToXYZ((double) SeahorseEntity.this.savedCoralPos.getX() + 0.5D, (double) SeahorseEntity.this.savedCoralPos.getY() + 0.5D, (double) SeahorseEntity.this.savedCoralPos.getZ() + 0.5D, (double)1.2F);
                    return true;
                } else {
                    return false;
                }
            }
        }

        public boolean shouldContinueExecuting() {
            if (!this.running) {
                return false;
            } else if (!SeahorseEntity.this.hasCoral()) {
                return false;
            } else if (SeahorseEntity.this.world.isRaining()) {
                return false;
            } else if (this.completedGrowth()) {
                return SeahorseEntity.this.rand.nextFloat() < 0.2F;
            } else if (SeahorseEntity.this.ticksExisted % 20 == 0 && !SeahorseEntity.this.isCoral(SeahorseEntity.this.savedCoralPos)) {
                SeahorseEntity.this.savedCoralPos = null;
                return false;
            } else {
                return true;
            }
        }

        private boolean completedGrowth() {
            return this.growTicks > 400;
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void startExecuting() {
            this.growTicks = 0;
            this.ticks = 0;
            this.lastPollinationTick = 0;
            this.running = true;
        }

        /**
         * Reset the task's internal state. Called when this task is interrupted by another one
         */
        public void resetTask() {
            this.running = false;
            SeahorseEntity.this.navigator.clearPath();
            SeahorseEntity.this.remainingCooldownBeforeLocatingNewCoral = 200;
        }

        /**
         * Keep ticking a continuous task that has already been started
         */
        public void tick() {
            ++this.ticks;
            if (this.ticks > 600) {
                SeahorseEntity.this.savedCoralPos = null;
            } else {
                Vector3d vector3d = Vector3d.copyCenteredHorizontally(SeahorseEntity.this.savedCoralPos).add(0.0D, (double)0.6F, 0.0D);
                if (vector3d.distanceTo(SeahorseEntity.this.getPositionVec()) > 1.0D) {
                    this.nextTarget = vector3d;
                    this.moveToNextTarget();
                } else {
                    if (this.nextTarget == null) {
                        this.nextTarget = vector3d;
                    }

                    boolean flag = SeahorseEntity.this.getPositionVec().distanceTo(this.nextTarget) <= 0.1D;
                    boolean flag1 = true;
                    if (!flag && this.ticks > 600) {
                        SeahorseEntity.this.savedCoralPos = null;
                    } else {
                        if (flag) {
                            boolean flag2 = SeahorseEntity.this.rand.nextInt(25) == 0;
                            if (flag2) {
                                BlockPos coralPos = new BlockPos(this.nextTarget.x, this.nextTarget.y, this.nextTarget.z);
                                Direction direction = Direction.byIndex(SeahorseEntity.this.rand.nextInt(Direction.values().length));
                                BlockPos place = coralPos.offset(direction);

                                if ((SeahorseEntity.this.world.isAirBlock(place) || SeahorseEntity.this.world.getBlockState(place).getMaterial().isLiquid()) && direction != Direction.DOWN) {
                                    if (direction == Direction.UP) {
                                        SeahorseEntity.this.world.setBlockState(place, getVariantType().getCoral().with(AbstractCoralPlantBlock.WATERLOGGED, SeahorseEntity.this.world.getFluidState(place).isTagged(FluidTags.WATER)), 3);
                                    } else {
                                        SeahorseEntity.this.world.setBlockState(place, getVariantType().getFan().with(CoralFanBlock.WATERLOGGED, SeahorseEntity.this.world.getFluidState(place).isTagged(FluidTags.WATER)), 3);
                                    }
                                }

                                this.nextTarget = new Vector3d(vector3d.getX() + (double)this.getRandomOffset(), vector3d.getY(), vector3d.getZ() + (double)this.getRandomOffset());
                                SeahorseEntity.this.navigator.clearPath();
                            } else {
                                flag1 = false;
                            }

                            SeahorseEntity.this.getLookController().setLookPosition(vector3d.getX(), vector3d.getY(), vector3d.getZ());
                        }

                        if (flag1) {
                            this.moveToNextTarget();
                        }

                        ++this.growTicks;
                        if (SeahorseEntity.this.rand.nextFloat() < 0.05F && this.growTicks > this.lastPollinationTick + 60) {
                            this.lastPollinationTick = this.growTicks;
                            SeahorseEntity.this.playSound(SoundEvents.BLOCK_BUBBLE_COLUMN_BUBBLE_POP, 1.0F, 1.0F);
                        }

                    }
                }
            }
        }

        private void moveToNextTarget() {
            SeahorseEntity.this.getMoveHelper().setMoveTo(this.nextTarget.getX(), this.nextTarget.getY(), this.nextTarget.getZ(), (double)0.35F);
        }

        private float getRandomOffset() {
            return (SeahorseEntity.this.rand.nextFloat() * 2.0F - 1.0F) * 0.33333334F;
        }

        private Optional<BlockPos> getCoral() {
            return this.findCoral(this.coralPredicate, 5.0D);
        }

        private Optional<BlockPos> findCoral(Predicate<BlockState> p_226500_1_, double distance) {
            BlockPos blockpos = SeahorseEntity.this.getPosition();
            BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

            for(int i = 0; (double)i <= distance; i = i > 0 ? -i : 1 - i) {
                for(int j = 0; (double)j < distance; ++j) {
                    for(int k = 0; k <= j; k = k > 0 ? -k : 1 - k) {
                        for(int l = k < j && k > -j ? j : 0; l <= j; l = l > 0 ? -l : 1 - l) {
                            blockpos$mutable.setAndOffset(blockpos, k, i - 1, l);
                            if (blockpos.withinDistance(blockpos$mutable, distance) && p_226500_1_.test(SeahorseEntity.this.world.getBlockState(blockpos$mutable))) {
                                return Optional.of(blockpos$mutable);
                            }
                        }
                    }
                }
            }
            return Optional.empty();
        }
    }

    public enum CoralType {
        BUBBLE(0, "bubble", Blocks.BUBBLE_CORAL.getDefaultState(), Blocks.BUBBLE_CORAL_FAN.getDefaultState()),
        FIRE(1, "fire", Blocks.FIRE_CORAL.getDefaultState(), Blocks.FIRE_CORAL_FAN.getDefaultState()),
        TUBE(2, "tube", Blocks.TUBE_CORAL.getDefaultState(), Blocks.TUBE_CORAL_FAN.getDefaultState()),
        BRAIN(3, "brain", Blocks.BRAIN_CORAL.getDefaultState(), Blocks.BRAIN_CORAL_FAN.getDefaultState()),
        HORN(4, "horn", Blocks.HORN_CORAL.getDefaultState(), Blocks.HORN_CORAL_FAN.getDefaultState());

        private static final SeahorseEntity.CoralType[] field_221088_c = Arrays.stream(values()).sorted(Comparator.comparingInt(SeahorseEntity.CoralType::getIndex)).toArray(SeahorseEntity.CoralType[]::new);
        private static final Map<String, SeahorseEntity.CoralType> TYPES_BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(SeahorseEntity.CoralType::getName, (p_221081_0_) -> {
            return p_221081_0_;
        }));
        private final int index;
        private final String name;
        private final BlockState coral;
        private final BlockState fan;

        CoralType(int index, String name, BlockState coral, BlockState fan) {
            this.index = index;
            this.name = name;
            this.coral = coral;
            this.fan = fan;
        }

        public String getName() {
            return this.name;
        }

        public int getIndex() {
            return this.index;
        }

        public BlockState getCoral() {
            return coral;
        }

        public BlockState getFan() {
            return fan;
        }

        public static SeahorseEntity.CoralType getTypeByName(String nameIn) {
            return TYPES_BY_NAME.getOrDefault(nameIn, HORN);
        }

        public static SeahorseEntity.CoralType getTypeByIndex(int indexIn) {
            if (indexIn < 0 || indexIn > field_221088_c.length) {
                indexIn = 0;
            }
            return field_221088_c[indexIn];
        }
    }
}