package pugz.omni.common.entity.colormatic;

import javax.annotation.Nonnull;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ConcretePowderBlock;
import net.minecraft.block.FallingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.DirectionalPlaceContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.registries.ForgeRegistries;
import pugz.omni.common.block.colormatic.LayerConcreteBlock;
import pugz.omni.common.block.colormatic.LayerConcretePowderBlock;
import pugz.omni.core.Omni;
import pugz.omni.core.registry.OmniEntities;

public class FallingConcretePowderEntity extends Entity implements IEntityAdditionalSpawnData {
    public int fallTime;
    private int layers;
    public boolean shouldDropItem = true;
    protected static final DataParameter<BlockPos> ORIGIN = EntityDataManager.createKey(FallingConcretePowderEntity.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Integer> LAYERS = EntityDataManager.createKey(FallingConcretePowderEntity.class, DataSerializers.VARINT);
    private BlockState fallState;
    private final EntitySize size;

    public FallingConcretePowderEntity(EntityType<FallingConcretePowderEntity> entity, World worldIn) {
        super(entity, worldIn);
        this.layers = 1;
        size = new EntitySize(0.98f, 0.1225f * layers, true);
    }

    public FallingConcretePowderEntity(World worldIn, double x, double y, double z, int layers, BlockState state) {
        super(OmniEntities.FALLING_CONCRETE_POWDER.get(), worldIn);
        this.preventEntitySpawning = true;
        this.setPosition(x, y, z);
        this.setMotion(Vector3d.ZERO);
        this.prevPosX = x;
        this.prevPosY = y;
        this.prevPosZ = z;
        this.layers = layers;
        this.setData(getPosition(), layers);
        fallState = state;
        size = new EntitySize(0.98f, 0.1225f * layers, true);
    }

    @Nonnull
    @Override
    public EntitySize getSize(Pose poseIn) {
        return size;
    }

    @SuppressWarnings("deprecation")
    public void tick() {
        if (this.fallState.isAir() || !(fallState.getBlock() instanceof LayerConcretePowderBlock)) {
            this.remove();
        } else {
            Block block = this.fallState.getBlock();
            if (this.fallTime++ == 0) {
                BlockPos blockpos = this.getPosition();
                if (this.world.getBlockState(blockpos).isIn(block)) {
                    this.world.removeBlock(blockpos, false);
                } else if (!this.world.isRemote) {
                    return;
                }
            }

            if (!this.hasNoGravity()) {
                this.setMotion(this.getMotion().add(0.0D, -0.04D, 0.0D));
            }

            this.move(MoverType.SELF, this.getMotion());
            if (!this.world.isRemote) {
                BlockPos blockpos1 = this.getPosition();
                boolean flag = this.fallState.getBlock() instanceof ConcretePowderBlock;
                boolean flag1 = flag && this.world.getFluidState(blockpos1).isTagged(FluidTags.WATER);
                double d0 = this.getMotion().lengthSquared();
                if (flag && d0 > 1.0D) {
                    BlockRayTraceResult blockraytraceresult = this.world.rayTraceBlocks(new RayTraceContext(new Vector3d(this.prevPosX, this.prevPosY, this.prevPosZ), this.getPositionVec(), RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.SOURCE_ONLY, this));
                    if (blockraytraceresult.getType() != RayTraceResult.Type.MISS && this.world.getFluidState(blockraytraceresult.getPos()).isTagged(FluidTags.WATER)) {
                        blockpos1 = blockraytraceresult.getPos();
                        flag1 = true;
                    }
                }

                if (!this.onGround && !flag1) {
                    if (!this.world.isRemote && (this.fallTime > 100 && (blockpos1.getY() < 1 || blockpos1.getY() > 256) || this.fallTime > 600)) {
                        if (this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                            this.entityDropItem(block);
                        }

                        this.remove();
                    }
                } else {
                    BlockState hitState = this.world.getBlockState(blockpos1);
                    this.setMotion(this.getMotion().mul(0.7D, -0.5D, 0.7D));
                    if (!hitState.isIn(Blocks.MOVING_PISTON)) {
                        this.remove();
                        boolean flag2 = hitState.isReplaceable(new DirectionalPlaceContext(this.world, blockpos1, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
                        boolean flag3 = FallingBlock.canFallThrough(this.world.getBlockState(blockpos1.down()));
                        boolean flag4 = this.fallState.isValidPosition(this.world, blockpos1) && !flag3;
                        if ((flag2 || (hitState.getBlock() instanceof LayerConcretePowderBlock || hitState.getBlock() instanceof LayerConcreteBlock)) && flag4) {
                            if (this.fallState.hasProperty(BlockStateProperties.WATERLOGGED) && this.world.getFluidState(blockpos1).getFluid() == Fluids.WATER) {
                                this.fallState = this.fallState.with(BlockStateProperties.WATERLOGGED, true);
                            }

                            if (hitState.getBlock() instanceof LayerConcretePowderBlock) {
                                if (block.getMaterialColor() == hitState.getBlock().getMaterialColor()) {
                                    this.shouldDropItem = false;
                                    if (hitState.get(LayerConcretePowderBlock.LAYERS) == 8)
                                        world.setBlockState(blockpos1.up(), this.fallState, 3);

                                    else {
                                        int totalLayers = hitState.get(LayerConcretePowderBlock.LAYERS) + this.fallState.get(LayerConcretePowderBlock.LAYERS);

                                        if (totalLayers <= 8)
                                            world.setBlockState(blockpos1, hitState.with(LayerConcretePowderBlock.LAYERS, totalLayers), 3);
                                        else {
                                            world.setBlockState(blockpos1, this.fallState.with(LayerConcretePowderBlock.LAYERS, 8), 3);
                                            world.setBlockState(blockpos1.up(), this.fallState.with(LayerConcretePowderBlock.LAYERS, totalLayers - 8), 3);
                                        }
                                    }
                                } else this.shouldDropItem = true;
                            } else if (hitState.getBlock() instanceof LayerConcreteBlock) {
                                if (((LayerConcretePowderBlock)block).getSolidifiedState().getBlock().getMaterialColor() == hitState.getBlock().getMaterialColor()) {
                                    this.shouldDropItem = false;
                                    if (hitState.get(LayerConcreteBlock.WATERLOGGED) && hitState.get(LayerConcreteBlock.LAYERS) < 7) {
                                        int totalLayers = hitState.get(LayerConcreteBlock.LAYERS) + this.fallState.get(LayerConcretePowderBlock.LAYERS);

                                        if (totalLayers <= 8)
                                            world.setBlockState(blockpos1, hitState.with(LayerConcreteBlock.LAYERS, totalLayers).with(LayerConcreteBlock.WATERLOGGED, totalLayers < 8), 3);
                                        else {
                                            world.setBlockState(blockpos1, hitState.with(LayerConcreteBlock.LAYERS, 8).with(LayerConcreteBlock.WATERLOGGED, false), 3);
                                            world.setBlockState(blockpos1.up(), this.fallState.with(LayerConcretePowderBlock.LAYERS, totalLayers - 8).with(LayerConcretePowderBlock.WATERLOGGED, false), 3);
                                        }
                                    }
                                } else this.shouldDropItem = true;
                            } else if (!(hitState.getBlock() instanceof LayerConcretePowderBlock)) {
                                if (this.world.setBlockState(blockpos1, this.fallState, 3)) {
                                    ((LayerConcretePowderBlock) block).onEndFalling(this.world, blockpos1, this.fallState);
                                }
                            } else if (this.shouldDropItem && this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS) && this.layers == 8) {
                                this.entityDropItem(block);
                            }
                        } else if (this.shouldDropItem && this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS) && this.layers == 8) {
                            this.entityDropItem(block);
                        }
                    }
                }
            }
            this.setMotion(this.getMotion().scale(0.98D));
        }
    }

    public void setData(BlockPos pos, int layers) {
        this.dataManager.set(ORIGIN, pos);
        this.dataManager.set(LAYERS, layers);
    }

    public BlockPos getOrigin() {
        return this.dataManager.get(ORIGIN);
    }

    public int getLayers() {
        return this.dataManager.get(LAYERS);
    }

    @Override
    public boolean canBeCollidedWith() {
        return this.isAlive();
    }

    @Override
    public boolean canRenderOnFire() {
        return false;
    }

    @Override
    protected boolean canTriggerWalking() {
        return false;
    }

    @Override
    public boolean canBeAttackedWithItem() {
        return false;
    }

    @Override
    protected void registerData() {
        this.dataManager.register(ORIGIN, BlockPos.ZERO);
        this.dataManager.register(LAYERS, 1);
    }

    @Override
    protected void readAdditional(CompoundNBT compound) {
        if (NBTUtil.readBlockState(compound.getCompound("BlockState")).getBlock() instanceof LayerConcretePowderBlock) this.fallState = NBTUtil.readBlockState(compound.getCompound("BlockState"));
        else this.fallState = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(Omni.MOD_ID, "red_concrete_powder")).getDefaultState();
        this.fallTime = compound.getInt("Time");
        if (compound.contains("Layers", Constants.NBT.TAG_INT)) {
            this.layers = compound.getInt("Layers");
        }
    }

    @Override
    protected void writeAdditional(CompoundNBT compound) {
        compound.put("BlockState", NBTUtil.writeBlockState(this.fallState));
        compound.putInt("Time", this.fallTime);
        compound.putInt("Layers", this.layers);
    }

    @Nonnull
    public BlockState getBlockState() {
        return fallState;
    }

    @Nonnull
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeInt(Block.getStateId(this.getBlockState()));
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        this.fallState = Block.getStateById(additionalData.readInt());
    }
}