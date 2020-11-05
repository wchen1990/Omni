package pugz.omni.common.entity.colormatic;

import pugz.omni.common.block.colormatic.LayerConcreteBlock;
import pugz.omni.common.block.colormatic.LayerConcretePowderBlock;
import pugz.omni.core.registry.OmniEntities;
import net.minecraft.block.*;
import net.minecraft.entity.*;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.DirectionalPlaceContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.network.play.server.SSpawnObjectPacket;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;

import javax.annotation.Nonnull;

public class FallingConcretePowderEntity extends Entity {
    public int fallTime;
    private int layers;
    public boolean shouldDropItem;
    protected static final DataParameter<BlockPos> ORIGIN = EntityDataManager.createKey(FallingConcretePowderEntity.class, DataSerializers.BLOCK_POS);
    private static final DataParameter<Integer> LAYERS = EntityDataManager.createKey(FallingConcretePowderEntity.class, DataSerializers.VARINT);
    private BlockState fallState;
    private EntitySize size;

    public FallingConcretePowderEntity(World worldIn) {
        super(OmniEntities.FALLING_CONCRETE_POWDER.get(), worldIn);
        this.layers = 1;
        size = new EntitySize(0.98f, 0.1225f * layers, true);
    }

    public FallingConcretePowderEntity(World worldIn, double x, double y, double z, int layers, BlockState state) {
        super(OmniEntities.FALLING_CONCRETE_POWDER.get(), worldIn);
        this.preventEntitySpawning = true;
        this.setPosition(x, y + (1.0F - this.getHeight()) / 2.0F, z);
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

    /**
     * Called to update the entity's position/logic.
     */
    @SuppressWarnings("deprecation")
    public void tick() {
        if (fallState.isAir()) {
            this.remove();
        } else {
            Block block = this.fallState.getBlock();
            if (this.fallTime++ == 0) {
                BlockPos blockpos = this.getPosition();
                if (this.world.getBlockState(blockpos).isIn(block)) {
                    this.world.removeBlock(blockpos, false);
                } else if (!this.world.isRemote) {
                    this.remove();
                    return;
                }
            }

            if (!this.hasNoGravity()) {
                this.setMotion(this.getMotion().add(0.0D, -0.04D, 0.0D));
            }

            this.move(MoverType.SELF, this.getMotion());
            if (!this.world.isRemote) {
                BlockPos blockpos1 = this.getPosition();

                if (!this.onGround) {
                    if (!this.world.isRemote && (this.fallTime > 100 && (blockpos1.getY() < 1 || blockpos1.getY() > 256) || this.fallTime > 600)) {
                        if (this.shouldDropItem && this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                            this.entityDropItem(block);
                        }

                        this.remove();
                    }
                } else {
                    BlockState blockstate = this.world.getBlockState(blockpos1);
                    this.setMotion(this.getMotion().mul(0.7D, -0.5D, 0.7D));
                    if (!blockstate.isIn(Blocks.MOVING_PISTON)) {
                        this.remove();
                        boolean flag2 = blockstate.isReplaceable(new DirectionalPlaceContext(this.world, blockpos1, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
                        boolean flag3 = FallingBlock.canFallThrough(this.world.getBlockState(blockpos1.down()));
                        boolean flag4 = this.fallState.isValidPosition(this.world, blockpos1) && !flag3;
                        if ((flag2 || (blockstate.getBlock() instanceof LayerConcretePowderBlock || blockstate.getBlock() instanceof LayerConcreteBlock)) && flag4) {
                            if (this.fallState.hasProperty(BlockStateProperties.WATERLOGGED) && this.world.getFluidState(blockpos1).getFluid() == Fluids.WATER) {
                                this.fallState = this.fallState.with(BlockStateProperties.WATERLOGGED, true);
                            }

                            if (blockstate.getBlock() instanceof LayerConcretePowderBlock) {
                                if (block.getMaterialColor() == blockstate.getBlock().getMaterialColor()) {
                                    if (blockstate.get(LayerConcretePowderBlock.LAYERS) == 8)
                                        world.setBlockState(blockpos1.up(), this.fallState, 3);

                                    else {
                                        int totalLayers = blockstate.get(LayerConcretePowderBlock.LAYERS) + this.fallState.get(LayerConcretePowderBlock.LAYERS);

                                        if (totalLayers <= 8)
                                            world.setBlockState(blockpos1, blockstate.with(LayerConcretePowderBlock.LAYERS, totalLayers), 3);
                                        else {
                                            world.setBlockState(blockpos1, this.fallState.with(LayerConcretePowderBlock.LAYERS, 8), 3);
                                            world.setBlockState(blockpos1.up(), this.fallState.with(LayerConcretePowderBlock.LAYERS, totalLayers - 8), 3);
                                        }
                                    }
                                }
                            }

                            else if (blockstate.getBlock() instanceof LayerConcreteBlock) {
                                if (((LayerConcretePowderBlock)block).getSolidifiedState().getBlock().getMaterialColor() == blockstate.getBlock().getMaterialColor() && blockstate.get(LayerConcreteBlock.WATERLOGGED) && blockstate.get(LayerConcreteBlock.LAYERS) < 7) {
                                    int totalLayers = blockstate.get(LayerConcreteBlock.LAYERS) + this.fallState.get(LayerConcretePowderBlock.LAYERS);

                                    if (totalLayers <= 8) world.setBlockState(blockpos1, blockstate.with(LayerConcreteBlock.LAYERS, totalLayers).with(LayerConcreteBlock.WATERLOGGED, totalLayers < 8), 3);
                                    else {
                                        world.setBlockState(blockpos1, blockstate.with(LayerConcreteBlock.LAYERS, 8).with(LayerConcreteBlock.WATERLOGGED, false), 3);
                                        world.setBlockState(blockpos1.up(), this.fallState.with(LayerConcretePowderBlock.LAYERS, totalLayers - 8).with(LayerConcretePowderBlock.WATERLOGGED, false), 3);
                                    }
                                }
                            }

                            else if (!(blockstate.getBlock() instanceof LayerConcretePowderBlock)) {
                                if (this.world.setBlockState(blockpos1, this.fallState, 3)) {
                                    ((LayerConcretePowderBlock) block).onEndFalling(this.world, blockpos1, this.fallState);
                                }
                            }

                            else if (this.shouldDropItem && this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
                                this.entityDropItem(block);
                            }
                        } else if (this.shouldDropItem && this.world.getGameRules().getBoolean(GameRules.DO_ENTITY_DROPS)) {
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

    public World getWorldObj() {
        return this.world;
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
        this.fallState = NBTUtil.readBlockState(compound.getCompound("BlockState"));
        this.fallTime = compound.getInt("Time");
        if (compound.contains("Layers", Constants.NBT.TAG_INT)) {
            this.layers = compound.getInt("Layers");
            size = new EntitySize(0.98f, 0.1225f * layers, true);
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

    public boolean ignoreItemEntityData() {
        return true;
    }

    @Nonnull
    public IPacket<?> createSpawnPacket() {
        return new SSpawnObjectPacket(this, Block.getStateId(this.getBlockState()));
    }
}