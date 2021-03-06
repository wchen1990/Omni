package pugz.omni.common.world.feature.wild_west;

import com.mojang.serialization.Codec;
import net.minecraft.block.BlockState;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import pugz.omni.common.block.wild_west.SaguaroCactusBlock;
import pugz.omni.core.registry.OmniBlocks;

import java.util.Random;

public class SaguaroCactusFeature extends Feature<NoFeatureConfig> {
    public SaguaroCactusFeature(Codec<NoFeatureConfig> configCodec) {
        super(configCodec);
    }

    @Override
    public boolean generate(ISeedReader world, ChunkGenerator chunkGenerator, Random random, BlockPos pos, NoFeatureConfig config) {
        int xOffset = random.nextInt(8) - random.nextInt(8);
        int zOffset = random.nextInt(8) - random.nextInt(8);
        int y = world.getHeight(Heightmap.Type.WORLD_SURFACE, pos.getX() + xOffset, pos.getZ() + zOffset);
        BlockPos place = new BlockPos(pos.getX() + xOffset, y, pos.getZ() + zOffset);

        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (world.getBlockState(place.offset(direction)).getBlock() instanceof SaguaroCactusBlock) return false;
        }

        if (world.getBlockState(place.offset(Direction.DOWN)).getBlock() instanceof SaguaroCactusBlock && random.nextBoolean()) return false;

        return generateCactus(world, random.nextBoolean(), place, random, random.nextInt(50) == 0);
    }

    @SuppressWarnings("deprecation")
    public boolean generateCactus(ISeedReader world, boolean northSouth, BlockPos pos, Random random, boolean isBig) {
        if (!OmniBlocks.SAGUARO_CACTUS.get().getDefaultState().isValidPosition(world, pos)) return false;

        boolean hasArms = random.nextInt(12) > 0;
        boolean twoArms = random.nextInt(5) != 0;

        int centerHeight = world.getRandom().nextInt(4) + 4;

        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable(pos.getX(), pos.getY(), pos.getZ());
        for (int y = 0; y <= centerHeight; ++y) {
            if (y > 0 && !world.getBlockState(blockpos$mutable).isAir()) break;

            world.setBlockState(blockpos$mutable, OmniBlocks.SAGUARO_CACTUS.get().getDefaultState(), 2);
            blockpos$mutable.move(Direction.UP);

            if (y == centerHeight - 1 && random.nextInt(3) == 0) world.setBlockState(blockpos$mutable, OmniBlocks.CACTUS_BLOOM.get().getDefaultState(), 2);
        }

        if (!hasArms) return true;

        int yTop = blockpos$mutable.getY();
        int armStart = world.getRandom().nextInt(centerHeight - 3) + 1;
        Direction[] directions = northSouth ? new Direction[]{Direction.NORTH, Direction.SOUTH} : new Direction[]{Direction.EAST, Direction.WEST};

        if (twoArms) {
            for (Direction d : directions) {
                generateArm(world, d, blockpos$mutable.getX(), pos.getY() + armStart, blockpos$mutable.getZ(), yTop);
                armStart = world.getRandom().nextInt(centerHeight - 3) + 1;
            }
        } else {
            generateArm(world, directions[random.nextInt(directions.length)], blockpos$mutable.getX(), pos.getY() + armStart, blockpos$mutable.getZ(), yTop);
        }

        if ((!isBig && random.nextInt(10) == 0) || (isBig && random.nextInt(50) == 0)) {
            BlockPos nextPos =  new BlockPos(pos.getX(), yTop, pos.getZ());
            if (world.getBlockState(nextPos).isAir()) generateCactus(world, random.nextBoolean(), nextPos, random, true);
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    private void generateArm(ISeedReader world, Direction direction, int centerX, int armY, int centerZ, int centerHeight) {
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable(centerX + direction.getXOffset(), armY, centerZ + direction.getZOffset());

        if (!world.getBlockState(blockpos$mutable).isAir()) return;

        BlockPos centerPos = blockpos$mutable.offset(direction.getOpposite());
        BlockState centerState = world.getBlockState(centerPos);
        if (!centerState.isIn(OmniBlocks.SAGUARO_CACTUS.get())) return;

        world.setBlockState(centerPos, centerState.with(SaguaroCactusBlock.FACING_PROPERTIES.get(direction), true), 2);
        world.setBlockState(blockpos$mutable, OmniBlocks.SAGUARO_CACTUS.get().getDefaultState().with(SaguaroCactusBlock.HORIZONTAL, true).with(SaguaroCactusBlock.HORIZONTAL_DIRECTION, direction.getOpposite()).with(SaguaroCactusBlock.FACING_PROPERTIES.get(direction.getOpposite()), true), 2);

        blockpos$mutable.move(Direction.UP);
        int armHeight = Math.max(1, (centerHeight - blockpos$mutable.getY()) + world.getRandom().nextInt(2) - 3);
        for(int i = 0; i <= armHeight; ++i) {
            if (!world.getBlockState(blockpos$mutable).isAir()) return;
            world.setBlockState(blockpos$mutable, OmniBlocks.SAGUARO_CACTUS.get().getDefaultState(), 2);
            blockpos$mutable.move(Direction.UP);

            if (i == armHeight - 1 && world.getRandom().nextInt(3) == 0) world.setBlockState(blockpos$mutable, OmniBlocks.CACTUS_BLOOM.get().getDefaultState(), 2);
        }
    }
}