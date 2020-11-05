package pugz.omni.common.block.cavier_caves;

import net.minecraft.particles.IParticleData;
import pugz.omni.core.registry.OmniBlocks;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathType;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class SpeleothemBlock extends FallingBlock implements IWaterLoggable {
    public static final EnumProperty<Size> SIZE = EnumProperty.create("size", Size.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public SpeleothemBlock(Properties properties) {
        super(properties);
        setDefaultState(stateContainer.getBaseState().with(SIZE, Size.LARGE).with(WATERLOGGED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        Item held = context.getItem().getItem();
        FluidState fluidstate = context.getWorld().getFluidState(context.getPos());
        BlockState state = getDefaultState();

        if (held == OmniBlocks.ICE_SPELEOTHEM.get().asItem()) state = state.with(SIZE, Size.ICE_LARGE);
        else if (held == OmniBlocks.ICE_SPELEOTHEM.get().asItem()) state = state.with(SIZE, Size.ICE_LARGE);

        return state.with(WATERLOGGED, fluidstate.isTagged(FluidTags.WATER));
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public ActionResultType onBlockActivated(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult ray) {
        ItemStack held = player.getHeldItem(handIn);
        Size size = state.get(SIZE);

        if (!worldIn.isRemote && held.getItem() instanceof PickaxeItem) {
            FluidState fluidstate = worldIn.getFluidState(pos);

            if (size == Size.LARGE || size == Size.ICE_LARGE) worldIn.setBlockState(pos, getDefaultState().with(SIZE, Size.MEDIUM).with(WATERLOGGED, fluidstate.isTagged(FluidTags.WATER)), 0);
            else if (size == Size.MEDIUM) worldIn.setBlockState(pos, getDefaultState().with(SIZE, Size.SMALL).with(WATERLOGGED, fluidstate.isTagged(FluidTags.WATER)), 0);
            else if (size == Size.SMALL) worldIn.removeBlock(pos, false);

            if (held.isDamageable()) held.damageItem(1, player, (living) -> {
                living.sendBreakAnimation(handIn);
            });

            return ActionResultType.SUCCESS;
        }
        return ActionResultType.FAIL;
    }

    public boolean propagatesSkylightDown(BlockState state, IBlockReader reader, BlockPos pos) {
        return true;
    }

    public void tick(BlockState state, ServerWorld world, BlockPos pos, Random rand) {
        if (!this.isValidPosition(state, world, pos) && (world.isAirBlock(pos.down()) || canFallThrough(world.getBlockState(pos.down())) && pos.getY() >= 0)) {
            trySpawnEntity(world, pos);
        }
    }

    private void trySpawnEntity(World world, BlockPos pos) {
        FallingBlockEntity fallingblockentity = new FallingBlockEntity(world, (double) pos.getX() + 0.5D, (double) pos.getY(), (double) pos.getZ() + 0.5D, world.getBlockState(pos));
        this.onStartFalling(fallingblockentity);
        fallingblockentity.setHurtEntities(true);
        world.addEntity(fallingblockentity);
    }

    /**
     * Performs a random tick on a block.
     */
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState state, ServerWorld world, BlockPos pos, Random rand) {
        for (int y = pos.getY(); y >= Math.max(0, pos.getY() - 64); --y) {
            BlockPos check = new BlockPos(pos.getX(), y, pos.getZ());
            BlockState block = world.getBlockState(check);

            if (block.getBlock() == Blocks.CAULDRON && rand.nextInt(40) == 0 && state.getBlock() != OmniBlocks.NETHERRACK_SPELEOTHEM.get()) {
                int level = block.get(CauldronBlock.LEVEL);
                if (level < 3) world.setBlockState(check, block.with(CauldronBlock.LEVEL, level + 1), 3);
            }
        }
    }

    public boolean ticksRandomly(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
        BlockState down = worldIn.getBlockState(pos.down());
        BlockState up = worldIn.getBlockState(pos.up());
        return down.isSolid() || up.isSolid();
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state) {
        return state.get(WATERLOGGED) ? Fluids.WATER.getStillFluidState(false) : super.getFluidState(state);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean allowsMovement(@Nonnull BlockState state, @Nonnull IBlockReader worldIn, @Nonnull BlockPos pos, PathType type) {
        return type == PathType.WATER && worldIn.getFluidState(pos).isTagged(FluidTags.WATER);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return state.get(SIZE).shape;
    }

    @Override
    protected int getFallDelay() {
        return 4;
    }

    @Override
    public void onEndFalling(World worldIn, BlockPos pos, BlockState fallingState, BlockState hitState, FallingBlockEntity fallingBlock) {
        worldIn.destroyBlock(pos, false);
    }

    public int getDustColor(BlockState state, IBlockReader reader, BlockPos pos) {
        return state.getMaterialColor(reader, pos).colorValue;
    }

    public void onFallenUpon(World worldIn, BlockPos pos, Entity entityIn, float fallDistance) {
        BlockState state = worldIn.getBlockState(pos);
        entityIn.onLivingFall(fallDistance, 4.0F + 1 / (state.get(SIZE).width * 0.5F));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onProjectileCollision(World world, BlockState state, BlockRayTraceResult hit, ProjectileEntity projectile) {
        trySpawnEntity(world, hit.getPos());
    }

    public void animateTick(BlockState state, World world, BlockPos pos, Random rand) {
        if (world.isAirBlock(pos.down()) || world.getBlockState(pos.down()).getBlock() == Blocks.CAULDRON) {
            for(int i = 0; i < rand.nextInt(1) + 1; ++i) {
                this.addDripParticle(world, pos, state);
            }
        }
    }

    private void addDripParticle(World world, BlockPos pos, BlockState state) {
        if (state.getFluidState().isEmpty() && !(world.rand.nextFloat() < 0.3F)) {
            VoxelShape shape = state.getCollisionShape(world, pos);
            double d0 = shape.getEnd(Direction.Axis.Y);
            if (d0 >= 1.0D && !state.isIn(BlockTags.IMPERMEABLE)) {
                double d1 = shape.getStart(Direction.Axis.Y);
                if (d1 > 0.0D) {
                    this.addDripParticle(world, pos, state, shape, (double)pos.getY() + d1 - 0.05D);
                } else {
                    BlockPos down = pos.down();
                    BlockState downState = world.getBlockState(down);
                    double d2 = downState.getCollisionShape(world, down).getEnd(Direction.Axis.Y);
                    if ((d2 < 1.0D || !downState.hasOpaqueCollisionShape(world, down)) && downState.getFluidState().isEmpty()) {
                        this.addDripParticle(world, pos, state, shape, (double)pos.getY() - 0.05D);
                    }
                }
            }
        }
    }

    private void addDripParticle(World world, BlockPos pos, BlockState state, VoxelShape shape, double y) {
        IParticleData type;
        if (state.getBlock() != OmniBlocks.NETHERRACK_SPELEOTHEM.get()) type = ParticleTypes.DRIPPING_WATER;
        else type = ParticleTypes.DRIPPING_LAVA;

        world.addParticle(type, MathHelper.lerp(world.rand.nextDouble(), (double)pos.getX() + shape.getStart(Direction.Axis.X), (double)pos.getX() + shape.getEnd(Direction.Axis.X)), y, MathHelper.lerp(world.rand.nextDouble(), (double)pos.getZ() + shape.getStart(Direction.Axis.Z), (double)pos.getZ() + shape.getEnd(Direction.Axis.Z)), 0.0D, 0.0D, 0.0D);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(SIZE, WATERLOGGED);
    }

    public enum Size implements IStringSerializable {
        SMALL("small", 4),
        MEDIUM("medium", 8),
        ICE_LARGE("ice_large", 12),
        LARGE("large", 14);

        Size(String nameIn, int width) {
            int pad = (16 - width) / 2;
            shape = Block.makeCuboidShape(pad, 0, pad, 16 - pad, 16, 16 - pad);
            name = nameIn;
            this.width = width;
        }

        public VoxelShape shape;
        public String name;
        private final int width;

        @Nonnull
        @Override
        public String getString() {
            return name;
        }
    }
}