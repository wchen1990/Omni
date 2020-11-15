package pugz.omni.common.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.BucketItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.function.Supplier;

public class EntityBucketItem extends BucketItem {
    private final Supplier<EntityType<? extends Entity>> entity;

    public EntityBucketItem(Supplier<EntityType<? extends Entity>> entity, Supplier<? extends Fluid> supplier, Item.Properties builder) {
        super(supplier, builder);
        this.entity = entity;
    }

    public void onLiquidPlaced(World world, ItemStack stack, BlockPos pos) {
        if (!world.isRemote) {
            this.placeEntity((ServerWorld) world, stack, pos);
        }
    }

    protected void playEmptySound(PlayerEntity player, IWorld worldIn, BlockPos pos) {
        worldIn.playSound(player, pos, SoundEvents.ITEM_BUCKET_EMPTY_FISH, SoundCategory.NEUTRAL, 1.0F, 1.0F);
    }

    protected void placeEntity(ServerWorld world, ItemStack stack, BlockPos pos) {
        this.entity.get().spawn(world, stack, null, pos, SpawnReason.BUCKET, true, false);
    }
}