package com.pugz.omni.common.block.colormatic;

import com.pugz.omni.common.block.IStackable;
import net.minecraft.block.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

public class FlowersBlock extends BushBlock implements IStackable {
    private static final VoxelShape MULTI_FLOWER_SHAPE = Block.makeCuboidShape(4.0D, 0.0D, 4.0D, 12.0D, 10.0D, 12.0D);
    public static final IntegerProperty FLOWERS = IntegerProperty.create("flowers", 1, 4);
    private final Block base;

    public FlowersBlock(AbstractBlock.Properties properties, Block base) {
        super(properties);
        this.base = base;
        setDefaultState(this.stateContainer.getBaseState().with(FLOWERS, 2));
    }

    @Override
    public Block getBase() {
        return base;
    }

    @Override
    public Block getBlock() {
        return this;
    }

    @Override
    @SuppressWarnings("deprecated")
    public boolean isValidPosition(BlockState state, IWorldReader worldIn, BlockPos pos) {
        return base.isValidPosition(state, worldIn, pos);
    }

    @OnlyIn(Dist.CLIENT)
    public void animateTick(BlockState stateIn, World worldIn, BlockPos pos, Random rand) {
        base.animateTick(stateIn, worldIn, pos, rand);
    }

    @SuppressWarnings("deprecated")
    public void onEntityCollision(BlockState state, World worldIn, BlockPos pos, Entity entityIn) {
        base.onEntityCollision(state, worldIn, pos, entityIn);
    }

    @Nonnull
    @SuppressWarnings("deprecated")
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        Vector3d vector3d = state.getOffset(worldIn, pos);
        return state.get(FLOWERS) == 2 ? base.getShape(state, worldIn, pos, context) : MULTI_FLOWER_SHAPE.withOffset(vector3d.x, vector3d.y, vector3d.z);
    }

    @Override
    @SuppressWarnings("deprecated")
    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
        base.randomTick(base.getDefaultState(), worldIn, pos, random);
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {
        return new ItemStack(base);
    }

    @Nonnull
    public AbstractBlock.OffsetType getOffsetType() {
        return base.getOffsetType();
    }

    @Override
    public void removeOne(World world, BlockPos pos, BlockState state) {
        world.playSound((PlayerEntity)null, pos, SoundEvents.BLOCK_CROP_BREAK, SoundCategory.BLOCKS, 0.7F, 0.9F + world.rand.nextFloat() * 0.2F);
        int i = state.get(FLOWERS);
        switch (i) {
            case 1:
                world.destroyBlock(pos, true, (PlayerEntity)null);
                break;
            case 2:
                world.setBlockState(pos, base.getDefaultState(), 3);
                break;
            default:
                world.setBlockState(pos, state.with(FLOWERS, i - 1), 3);
                world.playEvent(2001, pos, Block.getStateId(state));
                break;
        }
    }

    public void harvestBlock(World world, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack) {
        super.harvestBlock(world, player, pos, state, te, stack);
        this.removeOne(world, pos, state);
    }

    @Nonnull
    @Override
    @SuppressWarnings("deprecated")
    public ActionResultType onBlockActivated(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        ItemStack held = player.getHeldItem(handIn);

        if (held.getItem() == getBase().asItem()) {
            int i = state.get(FLOWERS);
            if (i < 4) {
                player.sendBreakAnimation(handIn);
                if (!player.isCreative()) {
                    held.shrink(1);
                }

                world.setBlockState(pos, state.with(FLOWERS, i + 1), 3);
                return ActionResultType.func_233537_a_(world.isRemote);
            }
        }
        return ActionResultType.FAIL;
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FLOWERS);
    }
}